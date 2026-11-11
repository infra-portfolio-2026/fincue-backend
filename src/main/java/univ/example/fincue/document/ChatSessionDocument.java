package univ.example.fincue.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * chat_sessions 컬렉션 (챗봇 대화 세션)
 * 한 유저는 여러 세션을 가질 수 있음. 한 세션에 여러 메시지(ChatMessageDocument)가 속한다.
 */
@Document(collection = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "챗봇 대화 세션")
public class ChatSessionDocument {

    @Id
    @Schema(description = "세션 ID")
    private String id;

    @Indexed
    @Schema(description = "유저 ID")
    private String userId;

    @Schema(description = "세션 제목 (첫 질문 앞부분 또는 사용자 지정)")
    private String title;

    @Schema(description = "생성 시각")
    private Instant createdAt;

    @Schema(description = "마지막 메시지 시각")
    private Instant lastMessageAt;
}
