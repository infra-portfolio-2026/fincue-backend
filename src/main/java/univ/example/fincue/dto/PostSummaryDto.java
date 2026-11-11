package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "게시글 요약 (대표 TIP / 실시간 HOT)")
public class PostSummaryDto {

    @Schema(description = "게시글 ID")
    private String id;
    @Schema(description = "제목")
    private String title;
    @Schema(description = "해시태그 목록")
    private List<String> tags;
    @Schema(description = "작성일 (MM/dd)")
    private String date;
    @Schema(description = "좋아요 수")
    private int likeCount;
    @Schema(description = "댓글 수")
    private long commentCount;
    @Schema(description = "작성 시각")
    private Instant createdAt;
}
