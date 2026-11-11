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
public class CategoryCompareDto {

    private String category;
    private BigDecimal previousAmount;
    private BigDecimal currentAmount;
    private BigDecimal diffAmount;    // current - previous
}

