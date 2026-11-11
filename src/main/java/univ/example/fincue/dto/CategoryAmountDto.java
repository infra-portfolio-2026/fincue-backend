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
public class CategoryAmountDto {

    private String category;   // 식비, 교통, 쇼핑, 문화/여가, 통신비, 기타 등
    private BigDecimal amount;
}
