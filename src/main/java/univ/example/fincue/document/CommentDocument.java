package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * comments 컬렉션 (댓글)
 * 관계: N:1 → posts, users
 */
@Document(collection = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDocument {

    @Id
    private String id;

    @Indexed
    private String postId;   // posts 참조
    private String userId;  // users 참조
    private String content;
    private Instant createdAt;
}
