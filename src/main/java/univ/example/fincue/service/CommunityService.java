package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.*;
import univ.example.fincue.dto.*;
import univ.example.fincue.repository.mongo.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final String CATEGORY_TIP = "절약팁";
    private static final String CATEGORY_QUESTION = "질문";
    private static final String CATEGORY_FREE = "자유";
    private static final java.util.Set<String> VALID_CATEGORIES = java.util.Set.of(CATEGORY_TIP, CATEGORY_QUESTION, CATEGORY_FREE);
    private static final int CONTENT_SNIPPET_LENGTH = 200;

    private final PostMongoRepository postMongoRepository;
    private final CommentMongoRepository commentMongoRepository;
    private final LikeMongoRepository likeMongoRepository;
    private final BookmarkMongoRepository bookmarkMongoRepository;
    private final UserMongoRepository userMongoRepository;
    private final MongoTemplate mongoTemplate;

    /** 커뮤니티 통계: 전체게시글, 절약팁, 활성회원, 오늘의 인기글 수 */
    public CommunityStatsDto getStats() {
        long totalPosts = postMongoRepository.countAllBy();
        long tipCount = postMongoRepository.countByCategory(CATEGORY_TIP);
        if (totalPosts == 0) {
            return CommunityStatsDto.builder()
                    .totalPosts(0)
                    .tipCount(0)
                    .activeMemberCount(0)
                    .todayPopularCount(0)
                    .build();
        }
        List<String> userIds = mongoTemplate.getCollection("posts").distinct("userId", String.class).into(new ArrayList<>());
        long activeMemberCount = userIds.size();
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        // 오늘의 인기글: 당일 작성 + 좋아요 5개 이상
        Query todayPopularQuery = new Query(
                Criteria.where("createdAt").gte(todayStart)
                        .and("likeCount").gte(5)
        );
        long todayPopularCount = mongoTemplate.count(todayPopularQuery, PostDocument.class);
        return CommunityStatsDto.builder()
                .totalPosts(totalPosts)
                .tipCount(tipCount)
                .activeMemberCount(activeMemberCount)
                .todayPopularCount(todayPopularCount)
                .build();
    }

    /** 커뮤니티 게시글 목록 (카테고리/검색/페이지) */
    public List<CommunityPostDto> getPosts(String category, String keyword, int page, int size, Optional<String> currentUserId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<PostDocument> list;
        if (keyword != null && !keyword.isBlank()) {
            Query q = new Query();
            q.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(keyword, "i"),
                    Criteria.where("content").regex(keyword, "i")
            ));
            if (category != null && !category.isBlank()) {
                q.addCriteria(Criteria.where("category").is(category));
            }
            q.with(pageable);
            list = mongoTemplate.find(q, PostDocument.class);
        } else if (category != null && !category.isBlank()) {
            list = postMongoRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        } else {
            list = postMongoRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return list.stream()
                .map(p -> toCommunityPostDto(p, true, currentUserId))
                .collect(Collectors.toList());
    }

    /** 게시글 상세 (자세히 보기) */
    public Optional<CommunityPostDto> getPostDetail(String postId, Optional<String> currentUserId) {
        return postMongoRepository.findById(postId)
                .map(p -> toCommunityPostDto(p, false, currentUserId));
    }

    /** 새 글 작성 (로그인 유저). 카테고리: 절약팁, 질문, 자유만 허용 */
    public CommunityPostDto createPost(String userId, CreatePostRequest request) {
        String category = request.getCategory();
        if (category == null || category.isBlank()) {
            category = CATEGORY_FREE;
        } else if (!VALID_CATEGORIES.contains(category)) {
            category = CATEGORY_FREE;  // 유효하지 않으면 자유로
        }
        PostDocument doc = PostDocument.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .category(category)
                .tags(request.getTags() != null ? request.getTags() : List.of())
                .createdAt(Instant.now())
                .likeCount(0)
                .isTip(false)
                .build();
        doc = postMongoRepository.save(doc);
        return toCommunityPostDto(doc, false, Optional.of(userId));
    }

    /** 좋아요 토글 */
    public boolean toggleLike(String postId, String userId) {
        Optional<LikeDocument> existing = likeMongoRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            likeMongoRepository.delete(existing.get());
            updatePostLikeCount(postId, -1);
            return false;
        }
        likeMongoRepository.save(LikeDocument.builder().postId(postId).userId(userId).build());
        updatePostLikeCount(postId, 1);
        return true;
    }

    /** 북마크 토글 */
    public boolean toggleBookmark(String postId, String userId) {
        Optional<BookmarkDocument> existing = bookmarkMongoRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            bookmarkMongoRepository.delete(existing.get());
            return false;
        }
        bookmarkMongoRepository.save(BookmarkDocument.builder().postId(postId).userId(userId).build());
        return true;
    }

    /** 댓글 목록 */
    public List<CommunityCommentDto> getComments(String postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        List<CommentDocument> list = commentMongoRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
        return list.stream().map(this::toCommentDto).collect(Collectors.toList());
    }

    /** 댓글 작성 */
    public CommunityCommentDto createComment(String postId, String userId, String content) {
        CommentDocument doc = CommentDocument.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .createdAt(Instant.now())
                .build();
        doc = commentMongoRepository.save(doc);
        return toCommentDto(doc);
    }

    private void updatePostLikeCount(String postId, int delta) {
        postMongoRepository.findById(postId).ifPresent(p -> {
            int next = (p.getLikeCount() != null ? p.getLikeCount() : 0) + delta;
            p.setLikeCount(Math.max(0, next));
            postMongoRepository.save(p);
        });
    }

    private CommunityPostDto toCommunityPostDto(PostDocument p, boolean snippet, Optional<String> currentUserId) {
        String authorName = userMongoRepository.findById(p.getUserId())
                .map(UserDocument::getName).orElse("알 수 없음");
        long commentCount = commentMongoRepository.countByPostId(p.getId());
        long bookmarkCount = bookmarkMongoRepository.countByPostId(p.getId());
        String content = p.getContent();
        if (snippet && content != null && content.length() > CONTENT_SNIPPET_LENGTH) {
            content = content.substring(0, CONTENT_SNIPPET_LENGTH) + "...";
        }
        Boolean isLiked = currentUserId.map(uid -> likeMongoRepository.existsByPostIdAndUserId(p.getId(), uid)).orElse(null);
        Boolean isBookmarked = currentUserId.map(uid -> bookmarkMongoRepository.existsByPostIdAndUserId(p.getId(), uid)).orElse(null);
        return CommunityPostDto.builder()
                .id(p.getId())
                .authorId(p.getUserId())
                .authorName(authorName)
                .title(p.getTitle())
                .content(content)
                .category(p.getCategory() != null ? p.getCategory() : "자유")
                .tags(p.getTags() != null ? p.getTags() : List.of())
                .likeCount(p.getLikeCount() != null ? p.getLikeCount() : 0)
                .commentCount(commentCount)
                .bookmarkCount(bookmarkCount)
                .createdAt(p.getCreatedAt())
                .isLikedByMe(isLiked)
                .isBookmarkedByMe(isBookmarked)
                .build();
    }

    private CommunityCommentDto toCommentDto(CommentDocument c) {
        String authorName = userMongoRepository.findById(c.getUserId())
                .map(UserDocument::getName).orElse("알 수 없음");
        return CommunityCommentDto.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .userId(c.getUserId())
                .authorName(authorName)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
