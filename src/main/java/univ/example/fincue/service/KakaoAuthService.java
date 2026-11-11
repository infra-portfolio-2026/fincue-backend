package univ.example.fincue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


/**
 * 카카오 액세스 토큰으로 사용자 정보 조회 (프론트엔드에서 받은 카카오 토큰 검증)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthService {

    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;

    /**
     * 카카오 액세스 토큰으로 사용자 정보 조회. 유효하지 않으면 예외 발생.
     *
     * @return [providerId (String), name (String)]
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("카카오 액세스 토큰이 필요합니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                KAKAO_USER_INFO_URI,
                HttpMethod.GET,
                entity,
                String.class
        );

        if (response.getStatusCode() != HttpStatusCode.valueOf(200) || response.getBody() == null) {
            throw new IllegalArgumentException("카카오 사용자 정보 조회 실패: 유효하지 않은 토큰입니다.");
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String providerId = root.path("id").asText();
            String name = extractName(root);
            return new KakaoUserInfo(providerId, name);
        } catch (Exception e) {
            log.warn("카카오 응답 파싱 실패: {}", e.getMessage());
            throw new IllegalArgumentException("카카오 사용자 정보를 파싱할 수 없습니다.", e);
        }
    }

    private String extractName(JsonNode root) {
        // properties.nickname
        JsonNode props = root.path("properties");
        if (!props.isMissingNode()) {
            JsonNode nick = props.path("nickname");
            if (!nick.isMissingNode() && !nick.isNull()) {
                String s = nick.asText();
                if (s != null && !s.isBlank()) return s;
            }
        }
        // kakao_account.profile.nickname
        JsonNode kakaoAccount = root.path("kakao_account");
        if (!kakaoAccount.isMissingNode()) {
            JsonNode profile = kakaoAccount.path("profile");
            if (!profile.isMissingNode()) {
                JsonNode nick = profile.path("nickname");
                if (!nick.isMissingNode() && !nick.isNull()) {
                    String s = nick.asText();
                    if (s != null && !s.isBlank()) return s;
                }
            }
            JsonNode emailNode = kakaoAccount.path("email");
            if (!emailNode.isMissingNode() && !emailNode.isNull()) {
                String email = emailNode.asText();
                if (email != null && !email.isBlank()) return email;
            }
        }
        return root.path("id").asText();
    }

    public record KakaoUserInfo(String providerId, String name) {}
}
