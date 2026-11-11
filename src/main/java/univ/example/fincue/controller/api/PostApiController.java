package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import univ.example.fincue.dto.PostSummaryDto;
import univ.example.fincue.service.PostService;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "게시글", description = "대표 TIP(절약팁)·실시간 HOT(전체) - 일주일 이내 좋아요 상위 3개")
public class PostApiController {

    private final PostService postService;

    @GetMapping("/tips")
    @Operation(summary = "대표 TIP 목록", description = "절약팁 카테고리 중 일주일 이내 좋아요 상위 3개 (파라미터 없음)")
    public ResponseEntity<List<PostSummaryDto>> getTips() {
        return ResponseEntity.ok(postService.getTipPosts());
    }

    @GetMapping("/hot")
    @Operation(summary = "실시간 HOT 게시글", description = "전체 게시글 중 일주일 이내 좋아요 상위 3개 (파라미터 없음)")
    public ResponseEntity<List<PostSummaryDto>> getHot() {
        return ResponseEntity.ok(postService.getHotPosts());
    }
}

