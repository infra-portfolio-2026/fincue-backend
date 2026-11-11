package univ.example.fincue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCashflowPointDto {

    /** yyyy-MM-dd */
    private String date;

    /** 해당 일자의 총 수입 */
    private BigDecimal incomeAmount;

    /** 해당 일자의 총 지출 */
    private BigDecimal spendingAmount;

    /** 해당 일자의 순금액 (수입 - 지출) */
    private BigDecimal netAmount;
}
