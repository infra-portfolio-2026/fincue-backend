package univ.example.fincue.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * chat_messages 컬렉션 (챗봇 단일 메시지)
 * role: USER (사용자 발화) / ASSISTANT (LLM 응답) / SYSTEM (시스템 프롬프트, 보통 저장 안 함)
 */
@Document(collection = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "챗봇 메시지")
public class ChatMessageDocument {

    @Id
    @Schema(description = "메시지 ID")
    private String id;

    @Indexed
    @Schema(description = "세션 ID")
    private String sessionId;

    @Indexed
    @Schema(description = "유저 ID (조회 편의)")
    private String userId;

    @Schema(description = "역할 (USER / ASSISTANT)")
    private String role;

    @Schema(description = "메시지 본문")
    private String content;

    @Indexed
    @Schema(description = "생성 시각")
    private Instant createdAt;
}
