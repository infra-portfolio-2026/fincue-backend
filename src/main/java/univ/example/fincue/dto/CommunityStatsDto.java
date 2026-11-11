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
@Schema(description = "커뮤니티 통계 응답")
public class CommunityStatsDto {

    @Schema(description = "전체 게시글 수")
    private long totalPosts;
    @Schema(description = "절약팁 카테고리 게시글 수")
    private long tipCount;
    @Schema(description = "활성 회원 수 (게시글 작성한 적 있는 유저)")
    private long activeMemberCount;
    @Schema(description = "오늘의 인기글 수 (당일 작성 + 좋아요 5개 이상)")
    private long todayPopularCount;
}
