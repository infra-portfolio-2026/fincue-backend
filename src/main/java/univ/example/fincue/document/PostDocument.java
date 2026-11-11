package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * posts 컬렉션 (게시글)
 * 관계: N:1 → users / 1:N → comments, likes
 * 대표 TIP: isTip=true / 실시간 HOT: likeCount·최신순
 */
@Document(collection = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDocument {

    @Id
    private String id;

    @Indexed
    private String userId;  // users 참조
    private String title;
    private String content;
    private Instant createdAt;
    private Integer likeCount;   // 캐싱용 (likes 컬렉션과 동기화)
    private List<String> tags;   // 해시태그 (#저축 #재테크 등)
    private Boolean isTip;       // 대표 TIP 선정 여부
    /** 커뮤니티 카테고리: 절약팁, 질문, 자유 */
    private String category;
}
