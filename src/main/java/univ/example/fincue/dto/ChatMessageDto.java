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
@Schema(description = "챗봇 메시지 (한 줄)")
public class ChatMessageDto {

    @Schema(description = "메시지 ID")
    private String id;

    @Schema(description = "역할 (USER / ASSISTANT)")
    private String role;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "생성 시각")
    private Instant createdAt;
}
