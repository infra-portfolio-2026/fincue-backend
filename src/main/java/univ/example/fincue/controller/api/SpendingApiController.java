package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.example.fincue.document.SpendingDocument;
import univ.example.fincue.dto.CreateSpendingRequest;
import univ.example.fincue.dto.DailyDetailDto;
import univ.example.fincue.dto.MonthlyCashflowDto;
import univ.example.fincue.dto.MonthlySpendingSummaryDto;
import univ.example.fincue.dto.SpendingAnalysisDto;
import univ.example.fincue.service.CurrentUserService;
import univ.example.fincue.service.SpendingService;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/spendings")
@RequiredArgsConstructor
@Tag(name = "소비", description = "소비 내역 추가, 월별 소비분석, 소비 분석 대시보드 (로그인 유저 기준)")
public class SpendingApiController {

    private final SpendingService spendingService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(
            summary = "소비 내역 추가",
            description = "로그인 필요. spentDate(yyyy-MM-dd), category, amount를 입력하여 소비 내역을 추가합니다. " +
                    "JWT를 Authorize에 등록 후 호출하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "소비 내역 추가 성공"),
            @ApiResponse(responseCode = "400", description = "날짜/금액 누락 또는 날짜 형식 오류"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<?> addSpending(@RequestBody CreateSpendingRequest request) {
        Optional<String> mongoUserId = currentUserService.getCurrentMongoUserId();
        if (mongoUserId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        if (request.getSpentDate() == null || request.getSpentDate().isBlank()) {
            return ResponseEntity.badRequest().body("소비한 날짜(spentDate)를 입력해주세요.");
        }
        if (request.getAmount() == null) {
            return ResponseEntity.badRequest().body("금액(amount)을 입력해주세요.");
        }
        try {
            SpendingDocument created = spendingService.addSpending(
                    mongoUserId.get(),
                    request.getSpentDate(),
                    request.getCategory(),
                    request.getAmount()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "이번달 소비분석", description = "로그인한 유저의 지정 연·월 카테고리별 소비와 총액. year/month 생략 시 이번 달.")
    public ResponseEntity<?> getMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Optional<String> mongoUserId = currentUserService.getCurrentMongoUserId();
        if (mongoUserId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        MonthlySpendingSummaryDto dto = spendingService.getMonthlySummary(mongoUserId.get(), y, m);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/analysis")
    @Operation(
            summary = "소비 분석 대시보드",
            description = "이번 달 총 소비, 전월 대비 증감, 월 평균 소비, 월별 소비 추이, " +
                    "카테고리별 분포, 전월 대비 카테고리 비교를 한 번에 반환합니다. " +
                    "year/month를 생략하면 현재 연·월 기준으로 계산합니다."
    )
    public ResponseEntity<?> getAnalysis(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        Optional<String> mongoUserId = currentUserService.getCurrentMongoUserId();
        if (mongoUserId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        SpendingAnalysisDto dto = spendingService.getSpendingAnalysis(mongoUserId.get(), y, m);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/daily-cashflow")
    @Operation(
            summary = "일자별 수입/지출",
            description = "로그인한 유저의 지정 연·월에 대해 날짜별 수입/지출/순금액을 반환합니다. " +
                    "year/month를 생략하면 이번 달 기준입니다."
    )
    public ResponseEntity<?> getDailyCashflow(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        Optional<String> mongoUserId = currentUserService.getCurrentMongoUserId();
        if (mongoUserId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        MonthlyCashflowDto dto = spendingService.getMonthlyCashflow(mongoUserId.get(), y, m);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/daily-detail")
    @Operation(
            summary = "특정 날짜 상세 (수입/지출)",
            description = "로그인한 유저의 지정 날짜(yyyy-MM-dd)에 대해 수입/지출 합계, " +
                    "카테고리별 합계, 거래 상세 목록을 한 번에 반환합니다. " +
                    "캘린더에서 날짜를 클릭했을 때 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "날짜 형식 오류 (yyyy-MM-dd)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<?> getDailyDetail(
            @RequestParam String date
    ) {
        Optional<String> mongoUserId = currentUserService.getCurrentMongoUserId();
        if (mongoUserId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        if (date == null || date.isBlank()) {
            return ResponseEntity.badRequest().body("date 파라미터가 필요합니다. (yyyy-MM-dd)");
        }
        LocalDate target;
        try {
            target = LocalDate.parse(date);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
        DailyDetailDto dto = spendingService.getDailyDetail(mongoUserId.get(), target);
        return ResponseEntity.ok(dto);
    }
}
