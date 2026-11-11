package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import univ.example.fincue.service.FssProductSyncService;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "FSS Open API 동기화", description = "금융감독원 Open API 호출 → MongoDB 저장. 여기서 실행하면 DB에 정기예금/적금/연금저축 데이터가 저장됩니다.")
public class ProductSyncApiController {

    private final FssProductSyncService fssProductSyncService;

    @PostMapping("/products")
    @Operation(
            summary = "FSS 금융상품 DB 저장",
            description = "FSS Open API를 호출해 정기예금·적금·연금저축 각 최대 300건씩 가져와 MongoDB products 컬렉션에 저장합니다. 인증 불필요. 실행 후 서버 로그에서 건수 확인.",
            security = {}
    )
    public ResponseEntity<Map<String, String>> syncProducts() {
        fssProductSyncService.syncAll();
        return ResponseEntity.ok(Map.of("message", "동기화 요청 완료. 로그에서 결과 확인."));
    }
}
