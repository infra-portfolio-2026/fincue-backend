package univ.example.fincue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 맞춤 상품 추천 카드 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedProductDto {

    /** 추천 순위 (1, 2, 3...) */
    private int rank;

    /** 상품 구분: CARD(카드) / SAVING(적금) */
    private String productType;

    private String productId;
    private String bankName;
    private String productName;

    /** 추천 이유 문구 */
    private String recommendationReason;

    /** 주요 혜택 텍스트 리스트 */
    private List<String> benefits;

    /** 상세 보기 URL (상품 페이지) */
    private String detailUrl;

    /** 추천에 사용된 주요 소비 카테고리 (없으면 null) */
    private String mainSpendingCategory;
}

