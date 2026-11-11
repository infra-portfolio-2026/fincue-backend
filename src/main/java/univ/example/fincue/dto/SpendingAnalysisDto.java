package univ.example.fincue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingAnalysisDto {

    private int year;
    private int month;

    /** 이번 달 총 소비 */
    private BigDecimal currentMonthTotal;

    /** 전월 총 소비 */
    private BigDecimal previousMonthTotal;

    /** 전월 대비 증감액 (이번 달 - 전월) */
    private BigDecimal diffAmount;

    /** 전월 대비 증감률 (%). 전월이 0이면 0.0 */
    private Double diffRatePercent;

    /** 최근 12개월 월 평균 소비 */
    private BigDecimal averageMonthlySpending;

    /** 월별 소비 추이 (최근 12개월) */
    private List<MonthlySpendingPointDto> monthlyTrend;

    /** 선택한 달의 카테고리별 소비 */
    private List<CategoryAmountDto> categoryBreakdown;

    /** 전월 대비 카테고리별 비교 */
    private List<CategoryCompareDto> categoryComparison;
}

