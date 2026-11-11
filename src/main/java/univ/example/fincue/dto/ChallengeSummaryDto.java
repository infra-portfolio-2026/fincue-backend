package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "나만의 챌린지 요약")
public class ChallengeSummaryDto {

    @Schema(description = "진행 중인 도전 개수")
    private long ongoingCount;
    @Schema(description = "성공한 도전 개수")
    private long successCount;
    @Schema(description = "총 절약 금액 (원)")
    private java.math.BigDecimal totalSavedAmount;
}
