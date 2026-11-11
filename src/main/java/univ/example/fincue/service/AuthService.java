package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.UserDocument;
import univ.example.fincue.repository.mongo.UserMongoRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMongoRepository userMongoRepository;

    /** OAuth 로그인 후 MongoDB 유저 생성/유지 후 유저 반환 (JWT 발급용) */
    public UserDocument processOAuthPostLogin(String provider, OAuth2User oAuth2User) {
        String providerId = oAuth2User.getName();
        String name = oAuth2User.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = oAuth2User.getAttribute("nickname");
        }
        if (name == null || name.isBlank()) {
            String email = oAuth2User.getAttribute("email");
            name = email != null ? email : providerId;
        }
        return syncMongoUser(provider, providerId, name);
    }

    /** 카카오 액세스 토큰으로 얻은 사용자 정보로 MongoDB 유저 동기화 후 반환 (JWT 발급용) */
    public UserDocument processKakaoLogin(String providerId, String name) {
        return syncMongoUser("kakao", providerId, name);
    }

    private UserDocument syncMongoUser(String provider, String providerId, String name) {
        return userMongoRepository.findByAuthProviderAndAuthProviderId(provider, providerId)
                .orElseGet(() -> {
                    UserDocument doc = UserDocument.builder()
                            .name(name)
                            .createdAt(Instant.now())
                            .authProvider(provider)
                            .authProviderId(providerId)
                            .build();
                    return userMongoRepository.save(doc);
                });
    }
}
