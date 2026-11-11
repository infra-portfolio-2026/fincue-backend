package univ.example.fincue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "새 게시글 작성 요청")
public class CreatePostRequest {

    @Schema(description = "제목", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(description = "내용")
    private String content;
    @Schema(description = "카테고리 (절약팁, 질문, 자유)", allowableValues = {"절약팁", "질문", "자유"}, example = "자유")
    private String category;
    @Schema(description = "해시태그 목록")
    private List<String> tags;
}
