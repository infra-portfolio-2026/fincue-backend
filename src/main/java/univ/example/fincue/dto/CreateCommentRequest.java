package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "댓글 작성 요청")
public class CreateCommentRequest {

    @Schema(description = "댓글 내용", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
