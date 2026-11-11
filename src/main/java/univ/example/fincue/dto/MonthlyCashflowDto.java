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
public class MonthlyCashflowDto {

    private int year;
    private int month;

    /** 선택한 달 전체 수입 */
    private BigDecimal totalIncome;

    /** 선택한 달 전체 지출 */
    private BigDecimal totalSpending;

    /** 선택한 달 순금액 (수입 - 지출) */
    private BigDecimal netAmount;

    /** 선택한 달 일자별 수입/지출 */
    private List<DailyCashflowPointDto> daily;
}
