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
@Schema(description = "커뮤니티 게시글 (목록/상세)")
public class CommunityPostDto {

    @Schema(description = "게시글 ID")
    private String id;
    @Schema(description = "작성자 ID")
    private String authorId;
    @Schema(description = "작성자 이름")
    private String authorName;
    @Schema(description = "제목")
    private String title;
    @Schema(description = "내용 (목록: 요약 200자, 상세: 전체)")
    private String content;
    @Schema(description = "카테고리 (절약팁, 질문, 자유)")
    private String category;
    @Schema(description = "해시태그 목록")
    private List<String> tags;
    @Schema(description = "좋아요 수")
    private int likeCount;
    @Schema(description = "댓글 수")
    private long commentCount;
    @Schema(description = "북마크 수")
    private long bookmarkCount;
    @Schema(description = "작성 시각")
    private Instant createdAt;
    @Schema(description = "현재 유저 좋아요 여부 (로그인 시)")
    private Boolean isLikedByMe;
    @Schema(description = "현재 유저 북마크 여부 (로그인 시)")
    private Boolean isBookmarkedByMe;
}
