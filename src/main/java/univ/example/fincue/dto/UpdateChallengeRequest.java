package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "도전 수정 요청")
public class UpdateChallengeRequest {

    @Schema(description = "도전 제목", example = "커피값 절약 챌린지")
    private String title;
    @Schema(description = "카테고리", example = "식비")
    private String category;
    @Schema(description = "목표 금액 (원)", example = "150000")
    private BigDecimal goalAmount;
    @Schema(description = "시작일 (yyyy-MM-dd)", example = "2025-12-01")
    private String startDate;
    @Schema(description = "종료일 (yyyy-MM-dd)", example = "2025-12-31")
    private String endDate;
}
