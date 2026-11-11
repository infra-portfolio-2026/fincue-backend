package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import univ.example.fincue.dto.ChatMessageDto;
import univ.example.fincue.dto.ChatRequest;
import univ.example.fincue.dto.ChatResponse;
import univ.example.fincue.dto.ChatSessionDto;
import univ.example.fincue.service.ChatbotService;
import univ.example.fincue.service.CurrentUserService;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "챗봇", description = "사용자 소비 데이터를 기반으로 답하는 금융 비서 챗봇")
public class ChatbotApiController {

    private final ChatbotService chatbotService;
    private final CurrentUserService currentUserService;

    @PostMapping("/sessions")
    @Operation(summary = "새 세션 생성", description = "비어 있는 새 대화 세션을 만든다. title은 선택값.")
    public ResponseEntity<ChatSessionDto> createSession(
            @RequestParam(required = false) String title
    ) {
        String userId = requireUserId();
        return ResponseEntity.ok(chatbotService.createSession(userId, title));
    }

    @GetMapping("/sessions")
    @Operation(summary = "내 세션 목록", description = "최근 메시지 시각 내림차순으로 정렬된 세션 리스트.")
    public ResponseEntity<List<ChatSessionDto>> listSessions() {
        String userId = requireUserId();
        return ResponseEntity.ok(chatbotService.listSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "세션 메시지 목록", description = "오래된 순으로 정렬된 한 세션의 전체 메시지.")
    public ResponseEntity<List<ChatMessageDto>> listMessages(@PathVariable String sessionId) {
        String userId = requireUserId();
        return ResponseEntity.ok(chatbotService.listMessages(userId, sessionId));
    }

    @PostMapping("/messages")
    @Operation(
            summary = "메시지 전송",
            description = "사용자 메시지를 보내고 챗봇 응답을 받는다. " +
                    "sessionId가 비어 있으면 새 세션이 자동 생성된다. " +
                    "응답에는 (생성된) 세션 ID가 포함된다."
    )
    public ResponseEntity<ChatResponse> send(@RequestBody ChatRequest request) {
        String userId = requireUserId();
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "message 가 비어 있습니다.");
        }
        return ResponseEntity.ok(
                chatbotService.sendMessage(userId, request.getSessionId(), request.getMessage()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "세션 삭제", description = "세션과 그 안의 모든 메시지를 삭제한다.")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        String userId = requireUserId();
        chatbotService.deleteSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    private String requireUserId() {
        return currentUserService.getCurrentMongoUserId()
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "로그인이 필요합니다."));
    }
}
