package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "댓글")
public class CommunityCommentDto {

    @Schema(description = "댓글 ID")
    private String id;
    @Schema(description = "게시글 ID")
    private String postId;
    @Schema(description = "작성자 ID")
    private String userId;
    @Schema(description = "작성자 이름")
    private String authorName;
    @Schema(description = "댓글 내용")
    private String content;
    @Schema(description = "작성 시각")
    private Instant createdAt;
}
