package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "수입 내역 추가 요청")
public class CreateIncomeRequest {

    @Schema(description = "수입 발생 날짜 (yyyy-MM-dd)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-04-06")
    private String receivedDate;

    @Schema(description = "카테고리 (월급, 용돈, 부수입, 환급, 기타 등)", requiredMode = Schema.RequiredMode.REQUIRED, example = "월급")
    private String category;

    @Schema(description = "금액", requiredMode = Schema.RequiredMode.REQUIRED, example = "3200000")
    private BigDecimal amount;
}
