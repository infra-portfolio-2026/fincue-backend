package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import univ.example.fincue.config.JwtPrincipal;
import univ.example.fincue.document.UserDocument;
import univ.example.fincue.repository.mongo.UserMongoRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserMongoRepository userMongoRepository;

    /**
     * 현재 로그인한 유저의 MongoDB user id (ObjectId 문자열).
     * JWT Bearer 토큰 또는 OAuth2 세션 기반 인증 지원.
     */
    public Optional<String> getCurrentMongoUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        // JWT 인증
        if (auth.getPrincipal() instanceof JwtPrincipal jwtPrincipal) {
            return Optional.ofNullable(jwtPrincipal.mongoUserId());
        }
        // OAuth2 세션 인증
        if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
            String principalName = oauth2User.getName();
            if (principalName == null || principalName.isBlank()) {
                return Optional.empty();
            }
            for (String registrationId : new String[]{"google", "kakao", "naver"}) {
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, principalName);
                if (client != null) {
                    return userMongoRepository.findByAuthProviderAndAuthProviderId(registrationId, principalName)
                            .map(UserDocument::getId);
                }
            }
        }
        return Optional.empty();
    }
}
