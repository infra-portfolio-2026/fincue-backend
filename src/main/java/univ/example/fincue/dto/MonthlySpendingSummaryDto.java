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
public class MonthlySpendingSummaryDto {

    private int year;
    private int month;
    private List<CategoryAmountDto> byCategory;
    private BigDecimal totalAmount;
}
