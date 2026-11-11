package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "프론트엔드 카카오 로그인 성공 시 받은 액세스 토큰")
public class KakaoTokenRequest {

    @Schema(description = "카카오에서 발급받은 액세스 토큰 (access_token)", requiredMode = Schema.RequiredMode.REQUIRED, example = "카카오_로그인_성공_후_받은_access_token")
    private String accessToken;
}
