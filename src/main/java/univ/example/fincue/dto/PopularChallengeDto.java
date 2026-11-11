package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사람들이 많이 한 도전 (인기 도전)")
public class PopularChallengeDto {

    @Schema(description = "도전 ID")
    private String challengeId;
    @Schema(description = "제목")
    private String title;
    @Schema(description = "카테고리")
    private String category;
    @Schema(description = "목표 금액")
    private BigDecimal goalAmount;
    @Schema(description = "달성 비율 (%)")
    private double achievementRate;
    @Schema(description = "참여 인원 수")
    private long participantCount;
}
