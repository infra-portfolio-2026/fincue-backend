package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import univ.example.fincue.service.CardGorillaSyncService;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "카드고릴라 동기화", description = "카드고릴라 내부 API 호출 → MongoDB cards 컬렉션 저장. 소비 분석 기반 카드 추천에 사용.")
public class CardSyncApiController {

    private final CardGorillaSyncService cardGorillaSyncService;

    @PostMapping("/cards")
    @Operation(
            summary = "카드 목록 DB 저장",
            description = "카드고릴라 API를 호출해 신용카드 목록을 가져와 MongoDB cards 컬렉션에 저장합니다. 인증 불필요. 실행 후 서버 로그에서 건수 확인.",
            security = {}
    )
    public ResponseEntity<Map<String, String>> syncCards() {
        cardGorillaSyncService.syncAll();
        return ResponseEntity.ok(Map.of("message", "카드 동기화 요청 완료. 로그에서 결과 확인."));
    }
}
