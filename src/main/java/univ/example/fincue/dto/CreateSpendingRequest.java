package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "소비 내역 추가 요청")
public class CreateSpendingRequest {

    @Schema(description = "소비한 날짜 (yyyy-MM-dd)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-02-21")
    private String spentDate;
    @Schema(description = "카테고리 (식비, 교통, 쇼핑, 문화/여가, 통신비, 기타 등)", requiredMode = Schema.RequiredMode.REQUIRED, example = "식비")
    private String category;
    @Schema(description = "금액", requiredMode = Schema.RequiredMode.REQUIRED, example = "15000")
    private BigDecimal amount;
}
