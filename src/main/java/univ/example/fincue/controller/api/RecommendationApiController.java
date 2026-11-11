package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import univ.example.fincue.dto.RecommendedProductDto;
import univ.example.fincue.service.CurrentUserService;
import univ.example.fincue.service.RecommendationService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "맞춤 상품", description = "맞춤 상품(적금/카드 등) 추천 API")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/products")
    @Operation(
            summary = "맞춤 상품 추천 목록",
            description = "로그인 유저의 최근 소비 패턴(카테고리 상위 1개 기반)으로 추천 이유를 생성하고, " +
                    "적금(SAVING) + 카드 정보를 함께 섞어서 추천합니다. 비로그인 시에는 스코어 기준 상위 N개를 반환합니다."
    )
    public ResponseEntity<List<RecommendedProductDto>> getRecommendedProducts(
            @RequestParam(defaultValue = "5") int limit
    ) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        List<RecommendedProductDto> list = userId
                .map(id -> recommendationService.getRecommendedProductsForUser(id, limit))
                .orElseGet(() -> recommendationService.getRecommendedProductsForGuest(limit));
        return ResponseEntity.ok(list);
    }
}

