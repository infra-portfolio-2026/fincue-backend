package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * userBadges 컬렉션 (획득 배지)
 * 관계: N:1 → users
 */
@Document(collection = "userBadges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadgeDocument {

    @Id
    private String id;

    @Indexed
    private String userId;  // users 참조
    private String badgeName;
    private Instant earnedAt;
}
