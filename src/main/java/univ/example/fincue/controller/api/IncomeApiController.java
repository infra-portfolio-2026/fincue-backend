package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import univ.example.fincue.document.IncomeDocument;
import univ.example.fincue.dto.CreateIncomeRequest;
import univ.example.fincue.service.CurrentUserService;
import univ.example.fincue.service.IncomeService;

import java.util.Optional;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@Tag(name = "수입", description = "수입 내역 추가 (로그인 유저 기준)")
public class IncomeApiController {

    private final IncomeService incomeService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(
            summary = "수입 내역 추가",
            description = "로그인 필요. receivedDate(yyyy-MM-dd), category, amount를 입력하여 수입 내역을 추가합니다. " +
                    "JWT를 Authorize에 등록 후 호출하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "수입 내역 추가 성공"),
            @ApiResponse(responseCode = "400", description = "날짜/금액 누락 또는 날짜 형식 오류"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<?> addIncome(@RequestBody CreateIncomeRequest request) {
        Optional<String> mongoUserId = currentUserService.getCurrentMongoUserId();
        if (mongoUserId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        if (request.getReceivedDate() == null || request.getReceivedDate().isBlank()) {
            return ResponseEntity.badRequest().body("수입 날짜(receivedDate)를 입력해주세요.");
        }
        if (request.getAmount() == null) {
            return ResponseEntity.badRequest().body("금액(amount)을 입력해주세요.");
        }

        try {
            IncomeDocument created = incomeService.addIncome(
                    mongoUserId.get(),
                    request.getReceivedDate(),
                    request.getCategory(),
                    request.getAmount()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }
}
