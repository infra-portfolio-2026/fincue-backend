package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "챗봇 메시지 전송 응답")
public class ChatResponse {

    @Schema(description = "세션 ID (새로 생성됐다면 새 ID 반환)")
    private String sessionId;

    @Schema(description = "어시스턴트 응답 본문")
    private String reply;

    @Schema(description = "어시스턴트 메시지 ID")
    private String messageId;

    @Schema(description = "응답 생성 시각")
    private Instant createdAt;
}
