package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.example.fincue.dto.*;
import univ.example.fincue.service.CommunityService;
import univ.example.fincue.service.CurrentUserService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "커뮤니티", description = "통계(전체/절약팁/활성회원/오늘의인기글), 게시글(절약팁·질문·자유 카테고리), 좋아요/북마크, 댓글")
public class CommunityApiController {

    private final CommunityService communityService;
    private final CurrentUserService currentUserService;

    @GetMapping("/stats")
    @Operation(summary = "커뮤니티 통계", description = "totalPosts(전체), tipCount(절약팁), activeMemberCount(게시글 작성 유저), todayPopularCount(당일 작성 + 좋아요 5개 이상)")
    public ResponseEntity<CommunityStatsDto> getStats() {
        return ResponseEntity.ok(communityService.getStats());
    }

    @GetMapping("/posts")
    @Operation(summary = "게시글 목록", description = "카테고리(절약팁/질문/자유)·검색어·페이지네이션. 로그인 시 좋아요/북마크 여부 포함.")
    public ResponseEntity<List<CommunityPostDto>> getPosts(
            @Parameter(description = "카테고리 필터: 절약팁, 질문, 자유 (비우면 전체)") @RequestParam(required = false) String category,
            @Parameter(description = "제목/내용 검색어") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        return ResponseEntity.ok(communityService.getPosts(category, keyword, page, size, userId));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "게시글 상세", description = "자세히 보기. 로그인 시 isLikedByMe, isBookmarkedByMe 포함.")
    public ResponseEntity<?> getPostDetail(
            @PathVariable String postId) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        return communityService.getPostDetail(postId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/posts")
    @Operation(summary = "새 글 작성", description = "로그인 필요. category는 절약팁/질문/자유만 허용, 미입력/유효하지 않으면 자유로 저장")
    public ResponseEntity<?> createPost(@RequestBody CreatePostRequest request) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body("제목을 입력해주세요.");
        }
        CommunityPostDto created = communityService.createPost(userId.get(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "좋아요 토글", description = "로그인 필요. 이미 좋아요면 취소, 아니면 추가. 반환: true=좋아요됨, false=취소됨")
    public ResponseEntity<?> toggleLike(@PathVariable String postId) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        boolean liked = communityService.toggleLike(postId, userId.get());
        return ResponseEntity.ok(new LikeToggleResponse(liked));
    }

    @PostMapping("/posts/{postId}/bookmark")
    @Operation(summary = "북마크 토글", description = "로그인 필요. 이미 북마크면 취소, 아니면 추가.")
    public ResponseEntity<?> toggleBookmark(@PathVariable String postId) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        boolean bookmarked = communityService.toggleBookmark(postId, userId.get());
        return ResponseEntity.ok(new BookmarkToggleResponse(bookmarked));
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글 목록", description = "페이지네이션")
    public ResponseEntity<List<CommunityCommentDto>> getComments(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(communityService.getComments(postId, page, size));
    }

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글 작성", description = "로그인 필요")
    public ResponseEntity<?> createComment(
            @PathVariable String postId,
            @RequestBody CreateCommentRequest request) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest().body("댓글 내용을 입력해주세요.");
        }
        CommunityCommentDto created = communityService.createComment(postId, userId.get(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class LikeToggleResponse {
        private final boolean liked;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class BookmarkToggleResponse {
        private final boolean bookmarked;
    }
}
