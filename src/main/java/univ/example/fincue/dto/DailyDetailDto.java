package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import univ.example.fincue.document.IncomeDocument;
import univ.example.fincue.document.SpendingDocument;

import java.math.BigDecimal;
import java.util.List;

/**
 * 특정 날짜 상세: 카테고리별 합계 + 거래 상세 목록 (수입/지출 모두).
 * 캘린더에서 날짜를 클릭했을 때 모달/패널을 채우기 위한 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "특정 날짜의 수입/지출 상세")
public class DailyDetailDto {

    @Schema(description = "기준 날짜 (yyyy-MM-dd)", example = "2026-05-13")
    private String date;

    @Schema(description = "그날 총 수입")
    private BigDecimal totalIncome;

    @Schema(description = "그날 총 지출")
    private BigDecimal totalSpending;

    @Schema(description = "그날 순금액 (수입 - 지출)")
    private BigDecimal netAmount;

    @Schema(description = "지출 카테고리별 합계 (금액 내림차순)")
    private List<CategoryAmountDto> spendingByCategory;

    @Schema(description = "수입 카테고리별 합계 (금액 내림차순)")
    private List<CategoryAmountDto> incomeByCategory;

    @Schema(description = "그날의 지출 상세 목록")
    private List<SpendingDocument> spendings;

    @Schema(description = "그날의 수입 상세 목록")
    private List<IncomeDocument> incomes;
}
