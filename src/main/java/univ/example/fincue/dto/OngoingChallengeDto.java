package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "진행 중인 도전")
public class OngoingChallengeDto {

    @Schema(description = "도전 ID")
    private String challengeId;
    @Schema(description = "제목")
    private String title;
    @Schema(description = "카테고리")
    private String category;
    @Schema(description = "진행률 (%)")
    private double progressPercent;
    @Schema(description = "현재 절약 금액")
    private BigDecimal savedAmount;
    @Schema(description = "목표 금액")
    private BigDecimal goalAmount;
    @Schema(description = "시작일")
    private Instant startDate;
    @Schema(description = "종료일")
    private Instant endDate;
    @Schema(description = "목표까지 남은 금액")
    private BigDecimal remainingAmount;
}
