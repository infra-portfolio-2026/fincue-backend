package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * users 컬렉션
 * 관계: 1:N → spendings, posts, comments, userChallenges, userBadges
 * OAuth 연동: authProvider + authProviderId 로 로그인 유저 조회
 */
@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "auth_provider_id", def = "{'authProvider': 1, 'authProviderId': 1}", unique = true)
})
public class UserDocument {

    @Id
    private String id;

    private String name;
    private Instant createdAt;

    /** OAuth provider (google, kakao 등) - 로그인 유저 조회용 */
    private String authProvider;
    /** OAuth provider에서 준 subject id - 로그인 유저 조회용 */
    private String authProviderId;
}
