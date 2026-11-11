package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * bookmarks 컬렉션 (게시글 북마크)
 * 유니크 인덱스: { postId: 1, userId: 1 }
 */
@Document(collection = "bookmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "postId_userId_unique", def = "{'postId': 1, 'userId': 1}", unique = true)
})
public class BookmarkDocument {

    @Id
    private String id;

    private String postId;
    private String userId;
}
