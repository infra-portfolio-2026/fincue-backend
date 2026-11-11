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
@Schema(description = "완료된 도전")
public class CompletedChallengeDto {

    @Schema(description = "도전 ID")
    private String challengeId;
    @Schema(description = "제목")
    private String title;
    @Schema(description = "카테고리")
    private String category;
    @Schema(description = "성공 여부 (true=성공, false=실패)")
    private boolean success;
    @Schema(description = "종료일")
    private Instant endDate;
    @Schema(description = "절약 금액")
    private BigDecimal savedAmount;
}
