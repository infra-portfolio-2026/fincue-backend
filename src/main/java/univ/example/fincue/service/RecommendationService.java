package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.ProductDocument;
import univ.example.fincue.document.SpendingDocument;
import univ.example.fincue.document.CardDocument;
import univ.example.fincue.dto.RecommendedProductDto;
import univ.example.fincue.repository.mongo.CardMongoRepository;
import univ.example.fincue.repository.mongo.ProductMongoRepository;
import univ.example.fincue.repository.mongo.SpendingMongoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 맞춤 상품 추천 서비스.
 * - 사용자의 최근 소비 패턴(카테고리 상위 1개)을 분석해 추천 이유 문구를 생성
 * - 실제 추천 로직은 적금 + 카드를 함께 섞어서 추천(각각 스코어로 정렬)
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ProductMongoRepository productMongoRepository;
    private final SpendingMongoRepository spendingMongoRepository;
    private final CardMongoRepository cardMongoRepository;

    /** 로그인 유저 기준 맞춤 상품 추천 */
    public List<RecommendedProductDto> getRecommendedProductsForUser(String userId, int limit) {
        String topCategory = findTopSpendingCategory(userId);
        return getMixedRecommendations(topCategory, limit);
    }

    /** 로그인 안 된 경우: 단순 상위 상품 추천 */
    public List<RecommendedProductDto> getRecommendedProductsForGuest(int limit) {
        return getMixedRecommendations(null, limit);
    }

    /**
     * 최근 3개월 소비에서 가장 많이 쓴 카테고리를 찾는다.
     * 데이터가 없으면 null 반환.
     */
    private String findTopSpendingCategory(String userId) {
        ZonedDateTime end = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime start = end.minusMonths(3).withDayOfMonth(1).toLocalDate()
                .atStartOfDay(ZoneId.systemDefault());

        Instant startInstant = start.toInstant();
        Instant endInstant = end.toInstant();

        List<SpendingDocument> list =
                spendingMongoRepository.findByUserIdAndSpentAtBetween(userId, startInstant, endInstant);

        if (list.isEmpty()) {
            return null;
        }

        Map<String, BigDecimal> byCategory = list.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategory() != null && !s.getCategory().isBlank() ? s.getCategory() : "기타",
                        Collectors.reducing(BigDecimal.ZERO, SpendingDocument::getAmount, BigDecimal::add)
                ));

        return byCategory.entrySet().stream()
                .max(Comparator.comparing(e -> e.getValue() != null ? e.getValue() : BigDecimal.ZERO))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<RecommendedProductDto> getMixedRecommendations(String topCategory, int limit) {
        int safeLimit = Math.max(limit, 1);
        int fetchSize = safeLimit * 3; // 스코어 계산을 위해 후보를 더 넉넉히 확보

        // 1) 적금 후보 수집 (productType = SAVING)
        List<ProductDocument> savingProducts = productMongoRepository.findByProductType("SAVING");
        savingProducts.sort(Comparator
                .comparing((ProductDocument p) -> p.getInterestRate() != null ? p.getInterestRate().doubleValue() : 0.0)
                .reversed());
        if (savingProducts.size() > fetchSize) savingProducts = savingProducts.subList(0, fetchSize);

        // 2) 카드 후보 수집
        // - orderBy가 필요한 기준이 없어서 우선 많은 카드 후보를 가져온 뒤 in-memory로 스코어 정렬
        List<CardDocument> cards = cardMongoRepository.findAll(PageRequest.of(0, fetchSize)).getContent();

        List<Candidate> combined = new ArrayList<>();
        for (ProductDocument p : savingProducts) {
            combined.add(Candidate.fromProduct(p, topCategory));
        }
        for (CardDocument c : cards) {
            combined.add(Candidate.fromCard(c, topCategory));
        }

        if (combined.isEmpty()) {
            return List.of();
        }

        // 3) 전체 스코어 정렬 후 top N 선택
        combined.sort(Comparator.comparingDouble((Candidate x) -> x.score).reversed());
        List<Candidate> selected = new ArrayList<>(combined.subList(0, Math.min(safeLimit, combined.size())));

        // 4) (가능할 때) 카드/적금이 모두 섞이도록 보정
        boolean hasProduct = selected.stream().anyMatch(x -> x.type == CandidateType.SAVING);
        boolean hasCard = selected.stream().anyMatch(x -> x.type == CandidateType.CARD);
        if (safeLimit >= 2 && hasProduct && !hasCard) {
            ensureTypePresent(selected, combined, CandidateType.CARD);
        } else if (safeLimit >= 2 && hasCard && !hasProduct) {
            ensureTypePresent(selected, combined, CandidateType.SAVING);
        }

        // 5) 최종 rank 재부여 (스코어 기준 내림차순)
        selected.sort(Comparator.comparingDouble((Candidate x) -> x.score).reversed());
        List<RecommendedProductDto> result = new ArrayList<>();
        int rank = 1;
        for (Candidate c : selected) {
            result.add(toRankedDto(c.baseDto, rank++, topCategory));
        }
        return result;
    }

    private void ensureTypePresent(List<Candidate> selected, List<Candidate> allSortedDesc, CandidateType wantedType) {
        // selected에서 가장 score가 낮은 후보 제거
        Candidate minSelected = selected.stream().min(Comparator.comparingDouble(x -> x.score)).orElse(null);
        if (minSelected == null) return;

        // selected에 없는 wantedType 후보 중 가장 score 높은 것을 찾아 교체
        Optional<Candidate> next = allSortedDesc.stream()
                .filter(c -> c.type == wantedType)
                .filter(c -> selected.stream().noneMatch(s -> Objects.equals(s.baseDto.getProductId(), c.baseDto.getProductId())
                        && Objects.equals(s.baseDto.getBankName(), c.baseDto.getBankName())))
                .findFirst();

        next.ifPresent(candidate -> {
            selected.remove(minSelected);
            selected.add(candidate);
        });
    }

    private RecommendedProductDto toRankedDto(RecommendedProductDto base, int rank, String topCategory) {
        return RecommendedProductDto.builder()
                .rank(rank)
                .productType(base.getProductType())
                .productId(base.getProductId())
                .bankName(base.getBankName())
                .productName(base.getProductName())
                .recommendationReason(base.getRecommendationReason())
                .benefits(base.getBenefits())
                .detailUrl(base.getDetailUrl())
                .mainSpendingCategory(topCategory)
                .build();
    }

    private enum CandidateType { SAVING, CARD }

    private static class Candidate {
        private final CandidateType type;
        private final double score;
        private final RecommendedProductDto baseDto;

        private Candidate(CandidateType type, double score, RecommendedProductDto baseDto) {
            this.type = type;
            this.score = score;
            this.baseDto = baseDto;
        }

        private static Candidate fromProduct(ProductDocument p, String topCategory) {
            double interest = p.getInterestRate() != null ? p.getInterestRate().doubleValue() : 0.0;
            double intrRate2 = p.getIntrRate2() != null ? p.getIntrRate2().doubleValue() : 0.0;

            double score = interest * 10.0 + intrRate2;

            String reason;
            if (topCategory != null) {
                reason = String.format("당신의 %s 지출이 많아, 더 나은 이자 혜택을 줄 수 있는 상품을 추천합니다.", topCategory);
            } else {
                reason = "높은 금리와 한도를 기준으로 추천된 인기 상품입니다.";
            }

            List<String> benefits = new ArrayList<>();
            if (p.getInterestRate() != null) {
                benefits.add(String.format("연 이자율 %.2f%%", p.getInterestRate()));
            }
            if (p.getPeriod() != null) {
                benefits.add(String.format("가입 기간 %d개월", p.getPeriod()));
            }
            if (p.getMaxAmount() != null) {
                benefits.add(String.format("최대 %,d원까지 적립 가능", p.getMaxAmount().longValue()));
            }

            RecommendedProductDto dto = RecommendedProductDto.builder()
                    .rank(0)
                    .productType(CandidateType.SAVING.name())
                    .productId(p.getId())
                    .bankName(p.getBankName())
                    .productName(p.getProductName())
                    .recommendationReason(reason)
                    .benefits(benefits)
                    .detailUrl(p.getUrl())
                    .mainSpendingCategory(topCategory)
                    .build();

            return new Candidate(CandidateType.SAVING, score, dto);
        }

        private static Candidate fromCard(CardDocument c, String topCategory) {
            int benefitCount = c.getBenefits() != null ? c.getBenefits().size() : 0;
            int fee = c.getAnnualFeeDomestic() != null ? c.getAnnualFeeDomestic() : Integer.MAX_VALUE;

            boolean matchesCategory = topCategory != null
                    && c.getBenefitCategories() != null
                    && c.getBenefitCategories().stream().anyMatch(x -> x != null && x.contains(topCategory));

            // - 혜택 개수가 많을수록 +, 연회비가 낮을수록 +
            double score = (benefitCount * 2.0) - (fee == Integer.MAX_VALUE ? 0.0 : fee / 100000.0);
            if (matchesCategory) score += 5.0;

            String reason;
            if (topCategory != null) {
                if (matchesCategory) {
                    reason = String.format("당신의 %s 지출이 많아, %s 관련 혜택이 포함된 카드를 추천합니다.", topCategory, topCategory);
                } else {
                    reason = String.format("당신의 %s 지출이 많아, 혜택 구성이 좋은 카드를 추천합니다.", topCategory);
                }
            } else {
                reason = "연회비 대비 혜택 구성을 기준으로 추천된 카드입니다.";
            }

            List<String> benefits = c.getBenefits() != null ? c.getBenefits() : List.of();

            RecommendedProductDto dto = RecommendedProductDto.builder()
                    .rank(0)
                    .productType(CandidateType.CARD.name())
                    .productId(c.getExternalId())
                    .bankName(c.getCardCompany())
                    .productName(c.getCardName())
                    .recommendationReason(reason)
                    .benefits(benefits)
                    .detailUrl(c.getDetailUrl())
                    .mainSpendingCategory(topCategory)
                    .build();

            return new Candidate(CandidateType.CARD, score, dto);
        }
    }
}

