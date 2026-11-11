package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.PostDocument;
import univ.example.fincue.dto.PostSummaryDto;
import univ.example.fincue.repository.mongo.CommentMongoRepository;
import univ.example.fincue.repository.mongo.PostMongoRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final String CATEGORY_TIP = "절약팁";
    private static final int TOP_LIMIT = 3;
    private static final java.time.format.DateTimeFormatter DATE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("MM/dd").withZone(ZoneId.systemDefault());

    private final PostMongoRepository postMongoRepository;
    private final CommentMongoRepository commentMongoRepository;
    private final MongoTemplate mongoTemplate;

    /** 대표 TIP: 절약팁 카테고리 중 일주일 이내 좋아요 상위 3개 */
    public List<PostSummaryDto> getTipPosts() {
        Instant weekAgo = LocalDate.now().minus(7, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Query q = new Query(
                Criteria.where("category").is(CATEGORY_TIP)
                        .and("createdAt").gte(weekAgo)
        );
        q.with(Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt")));
        q.limit(TOP_LIMIT);
        List<PostDocument> list = mongoTemplate.find(q, PostDocument.class);
        return toSummaryList(list);
    }

    /** 실시간 HOT: 전체 중 일주일 이내 좋아요 상위 3개 */
    public List<PostSummaryDto> getHotPosts() {
        Instant weekAgo = LocalDate.now().minus(7, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Query q = new Query(Criteria.where("createdAt").gte(weekAgo));
        q.with(Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt")));
        q.limit(TOP_LIMIT);
        List<PostDocument> list = mongoTemplate.find(q, PostDocument.class);
        return toSummaryList(list);
    }

    private List<PostSummaryDto> toSummaryList(List<PostDocument> list) {
        return list.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private PostSummaryDto toSummary(PostDocument doc) {
        long commentCount = commentMongoRepository.countByPostId(doc.getId());
        String date = doc.getCreatedAt() != null
                ? DATE_FORMAT.format(doc.getCreatedAt())
                : "";
        return PostSummaryDto.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .tags(doc.getTags() != null ? doc.getTags() : List.of())
                .date(date)
                .likeCount(doc.getLikeCount() != null ? doc.getLikeCount() : 0)
                .commentCount(commentCount)
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
