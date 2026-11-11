package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * likes 컬렉션 (좋아요)
 * 관계: N:1 → posts, users
 * 유니크 인덱스: { postId: 1, userId: 1 }
 */
@Document(collection = "likes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "postId_userId_unique", def = "{'postId': 1, 'userId': 1}", unique = true)
})
public class LikeDocument {

    @Id
    private String id;

    private String postId;   // posts 참조
    private String userId;   // users 참조
}
