package univ.example.fincue.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import univ.example.fincue.config.JwtPrincipal;
import univ.example.fincue.config.JwtTokenProvider;
import univ.example.fincue.document.UserDocument;
import univ.example.fincue.dto.KakaoTokenRequest;
import univ.example.fincue.repository.mongo.UserMongoRepository;
import univ.example.fincue.service.AuthService;
import univ.example.fincue.service.KakaoAuthService;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증", description = "OAuth2(구글·카카오·네이버) 및 카카오 access_token으로 JWT 발급 / 인증")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserMongoRepository userMongoRepository;
    private final KakaoAuthService kakaoAuthService;
    private final AuthService authService;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "홈")
    public String home() {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>AI 금융 관리 - 로그인</title>
            <style>
            *{box-sizing:border-box;}
            body{font-family:'Segoe UI',sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#1a1a2e 0%%,#16213e 100%%);color:#fff;}
            .card{background:rgba(255,255,255,.08);border-radius:16px;padding:48px;text-align:center;max-width:420px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,.3);}
            h1{font-size:1.5rem;margin-bottom:8px;}
            p{color:#aaa;margin-bottom:32px;font-size:14px;}
            .btn{display:block;width:100%;padding:14px 24px;margin:12px 0;border:none;border-radius:12px;font-size:16px;font-weight:600;cursor:pointer;text-decoration:none;text-align:center;transition:transform .15s,box-shadow .15s;}
            .btn:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(0,0,0,.3);}
            .btn-google{background:#fff;color:#333;}
            .btn-kakao{background:#FEE500;color:#191919;}
            .btn-naver{background:#03C75A;color:#fff;}
            .btn-swagger{background:#4CAF50;color:#fff;margin-top:24px;border:1px solid rgba(255,255,255,.2);}
            </style></head>
            <body>
            <div class="card">
            <h1>AI 금융 관리 API</h1>
            <p>OAuth2(구글·카카오·네이버) 또는 Swagger에서 카카오 토큰으로 JWT를 발급받아 API를 사용하세요.</p>
            <a href="/oauth2/authorization/google" class="btn btn-google">Google 로그인</a>
            <a href="/oauth2/authorization/kakao" class="btn btn-kakao">Kakao 로그인</a>
            <a href="/oauth2/authorization/naver" class="btn btn-naver">Naver 로그인</a>
            <a href="/swagger-ui.html" class="btn btn-kakao">Swagger에서 카카오 토큰 → JWT 발급 테스트</a>
            <a href="/swagger-ui.html" class="btn btn-swagger">Swagger UI 열기 (문서 & API 테스트)</a>
            <a href="/demo.html" class="btn btn-home" style="margin-top:12px;background:rgba(255,255,255,.15);">API 체험 페이지</a>
            </div>
            </body></html>""";
    }

    /**
     * OAuth2 로그인 성공 후 리다이렉트되는 콜백 페이지.
     * token 쿼리 파라미터로 JWT를 받아 화면에 표시. (프론트엔드 없을 때 사용)
     */
    @GetMapping(value = "/auth/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String authCallback(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"><title>로그인 콜백</title></head>
                <body><h2>토큰을 받지 못했습니다.</h2><p>다시 로그인해 주세요.</p>
                <a href="/">홈으로</a> | <a href="/oauth2/authorization/google">Google 로그인</a> | <a href="/oauth2/authorization/kakao">Kakao 로그인</a> | <a href="/oauth2/authorization/naver">Naver 로그인</a>
                </body></html>""";
        }
        String escaped = token.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>로그인 성공 - JWT 토큰</title>
            <style>
            *{box-sizing:border-box;}
            body{font-family:'Segoe UI',sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#1a1a2e 0%%,#16213e 100%%);color:#fff;padding:20px;}
            .card{background:rgba(255,255,255,.08);border-radius:16px;padding:32px;max-width:560px;width:100%;box-shadow:0 8px 32px rgba(0,0,0,.3);}
            h2{margin-top:0;color:#4CAF50;}
            .token{background:rgba(0,0,0,.3);padding:12px;word-break:break-all;font-size:11px;border-radius:8px;margin:12px 0;}
            .btn{display:inline-block;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600;margin:8px 8px 8px 0;border:none;cursor:pointer;font-size:14px;transition:transform .15s;}
            .btn:hover{transform:translateY(-2px);}
            .btn-copy{background:#2196F3;color:#fff;}
            .btn-swagger{background:#4CAF50;color:#fff;}
            .btn-home{background:rgba(255,255,255,.2);color:#fff;}
            p{color:#ccc;font-size:14px;}
            </style></head>
            <body>
            <div class="card">
            <h2>로그인 성공</h2>
            <p>JWT 토큰이 발급되었습니다. 토큰을 복사한 뒤 Swagger UI에서 Authorize로 등록하세요.</p>
            <div id="tokenVal" class="token">{{TOKEN}}</div>
            <button class="btn btn-copy" onclick="var t=document.getElementById('tokenVal');navigator.clipboard.writeText(t.textContent);this.textContent='복사 완료!';setTimeout(function(){this.textContent='토큰 복사';}.bind(this),1500)">토큰 복사</button>
            <a href="/swagger-ui.html" class="btn btn-swagger">Swagger UI로 이동</a>
            <a href="/" class="btn btn-home">홈으로</a>
            </div>
            </body></html>""".replace("{{TOKEN}}", escaped);
    }

    /**
     * 프론트엔드에서 카카오 로그인 성공 후 받은 액세스 토큰을 보내면 JWT 발급.
     * 이후 API 호출 시 Authorization: Bearer {jwt} 헤더로 인증.
     */
    @PostMapping("/auth/kakao/token")
    @Operation(
            summary = "카카오 토큰으로 JWT 발급",
            description = "프론트엔드에서 카카오 로그인 성공 후 받은 access_token을 보내면 검증 후 JWT를 발급합니다. " +
                    "테스트용: accessToken에 'test'를 넣으면 테스트 유저로 로그인됩니다. " +
                    "발급받은 JWT를 Swagger 'Authorize' 또는 API 요청 시 Authorization: Bearer {jwt} 헤더로 사용하세요.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT 발급 성공"),
            @ApiResponse(responseCode = "400", description = "토큰 누락 또는 형식 오류"),
            @ApiResponse(responseCode = "401", description = "카카오 토큰 검증 실패")
    })
    public ResponseEntity<?> exchangeKakaoTokenForJwt(@RequestBody KakaoTokenRequest request) {
        if (request == null || request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("카카오 액세스 토큰(accessToken)이 필요합니다."));
        }
        String accessToken = request.getAccessToken().trim();
        UserDocument user;
        if ("test".equalsIgnoreCase(accessToken)) {
            // 테스트용: accessToken="test" 이면 테스트 유저로 로그인 (Swagger 등 더미 테스트용)
            user = authService.processKakaoLogin("test_user_id", "테스트유저");
        } else {
            try {
                KakaoAuthService.KakaoUserInfo userInfo = kakaoAuthService.getUserInfo(accessToken);
                user = authService.processKakaoLogin(userInfo.providerId(), userInfo.name());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
            }
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getName(), "kakao");
        return ResponseEntity.ok(new TokenResponse(token, "Bearer", jwtTokenProvider.getValidityMs()));
    }

    /**
     * JWT로 인증된 현재 유저의 이름 반환. 파라미터 없음, Authorization: Bearer {jwt} 만 필요.
     */
    @GetMapping("/auth/me")
    @Operation(
            summary = "현재 유저 이름",
            description = "Authorization 헤더의 JWT로 인증된 유저의 이름을 반환합니다. 파라미터 없이 Bearer 토큰만 보내면 됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 이름 반환"),
            @ApiResponse(responseCode = "401", description = "JWT 없음 또는 유효하지 않음")
    })
    public ResponseEntity<?> getCurrentUserName(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return ResponseEntity.status(401).body(new ErrorResponse("로그인이 필요합니다. JWT를 Authorization: Bearer {token} 으로 보내세요."));
        }
        String name = principal.getName();
        return ResponseEntity.ok(new UserNameResponse(name != null ? name : ""));
    }

    /**
     * OAuth2 세션 로그인 상태일 때 JWT를 발급.
     * 프론트에서 세션 기반 로그인 후 이 엔드포인트를 호출해 토큰을 받을 수 있음.
     */
    @GetMapping("/auth/token")
    @Operation(summary = "JWT 토큰 발급", description = "OAuth2 세션 로그인 시 JWT 발급. 프론트에서 토큰을 받아 Bearer 헤더로 API 호출")
    public ResponseEntity<?> getToken(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return ResponseEntity.status(401).body(new ErrorResponse("로그인이 필요합니다."));
        }
        String principalName = oauth2User.getName();
        String provider = null;
        for (String regId : new String[]{"google", "kakao", "naver"}) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(regId, principalName);
            if (client != null) {
                provider = regId;
                break;
            }
        }
        if (provider == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("유효한 OAuth2 클라이언트를 찾을 수 없습니다."));
        }
        Optional<UserDocument> userOpt = userMongoRepository.findByAuthProviderAndAuthProviderId(provider, principalName);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(new ErrorResponse("MongoDB 유저 정보를 찾을 수 없습니다."));
        }
        UserDocument user = userOpt.get();
        String token = jwtTokenProvider.createToken(user.getId(), user.getName(), provider);
        return ResponseEntity.ok(new TokenResponse(token, "Bearer", jwtTokenProvider.getValidityMs()));
    }

    @Getter
    @AllArgsConstructor
    public static class TokenResponse {
        private final String accessToken;
        private final String tokenType;
        private final long expiresInMs;
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private final String message;
    }

    @Getter
    @AllArgsConstructor
    public static class UserNameResponse {
        private final String name;
    }
}

