package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "새 도전 만들기 요청")
public class CreateChallengeRequest {

    @Schema(description = "도전 제목", requiredMode = Schema.RequiredMode.REQUIRED, example = "커피값 절약 챌린지")
    private String title;
    @Schema(description = "카테고리 (식비, 교통, 쇼핑 등)", requiredMode = Schema.RequiredMode.REQUIRED, example = "식비")
    private String category;
    @Schema(description = "목표 금액 (원)", requiredMode = Schema.RequiredMode.REQUIRED, example = "150000")
    private BigDecimal goalAmount;
    @Schema(description = "시작일 (yyyy-MM-dd)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-12-01")
    private String startDate;
    @Schema(description = "종료일 (yyyy-MM-dd)", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-12-31")
    private String endDate;
}
