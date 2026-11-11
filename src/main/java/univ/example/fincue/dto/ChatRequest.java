package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 메시지 전송 요청")
public class ChatRequest {

    @Schema(description = "기존 세션 ID. 비어 있으면 새 세션을 자동 생성합니다.", example = "65f5...")
    private String sessionId;

    @Schema(description = "사용자 질문 내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "이번 달 내 식비 얼마나 썼어?")
    private String message;
}
