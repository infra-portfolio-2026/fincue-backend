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
@Schema(description = "챗봇 세션 요약")
public class ChatSessionDto {

    @Schema(description = "세션 ID")
    private String id;

    @Schema(description = "세션 제목")
    private String title;

    @Schema(description = "생성 시각")
    private Instant createdAt;

    @Schema(description = "마지막 메시지 시각")
    private Instant lastMessageAt;
}
