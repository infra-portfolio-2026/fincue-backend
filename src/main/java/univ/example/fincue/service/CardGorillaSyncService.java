package univ.example.fincue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import univ.example.fincue.document.CardDocument;
import univ.example.fincue.repository.mongo.CardMongoRepository;

import java.time.Instant;
import java.util.*;

/**
 * 카드고릴라 내부 API 호출 후 MongoDB cards 컬렉션에 저장.
 * API: https://api.card-gorilla.com:8080/v1/cards?p=1&perPage=50 등
 * 응답 구조에 맞춰 JSON 필드를 추출해 CardDocument로 매핑.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardGorillaSyncService {

    private static final int PER_PAGE = 50;
    private static final int MAX_PAGES = 20;
    /** 카드 상세 페이지 베이스 (card/bridge/{idx}) */
    private static final String CARD_DETAIL_BASE = "https://www.card-gorilla.com/card/bridge/";
    /** 카드고릴라 카드 상세 API (request_pc/request_m 추출용) */
    private static final String CARD_DETAIL_API_BASE = "https://api.card-gorilla.com:8080/v1/cards/";
    /** 카드사 공식 사이트(카드사 정보 페이지) fallback */
    private static final Map<String, String> CARD_COMPANY_URLS = Map.ofEntries(
            Map.entry("신한카드", "https://www.shinhancard.com"),
            Map.entry("삼성카드", "https://www.samsungcard.com"),
            Map.entry("KB국민카드", "https://card.kbcard.com"),
            Map.entry("국민카드", "https://card.kbcard.com"),
            Map.entry("현대카드", "https://www.hyundaicard.com"),
            Map.entry("롯데카드", "https://www.lottecard.co.kr"),
            Map.entry("우리카드", "https://pc.wooricard.com"),
            Map.entry("하나카드", "https://www.hanacard.co.kr"),
            Map.entry("NH농협카드", "https://card.nonghyup.com"),
            Map.entry("농협카드", "https://card.nonghyup.com"),
            Map.entry("BC카드", "https://www.bccard.com"),
            Map.entry("IBK기업은행", "https://www.ibk.co.kr"),
            Map.entry("기업은행", "https://www.ibk.co.kr"),
            Map.entry("씨티카드", "https://www.citibank.co.kr"),
            Map.entry("카카오뱅크", "https://www.kakaobank.com"),
            Map.entry("케이뱅크", "https://www.kbanknow.com"),
            Map.entry("토스뱅크", "https://www.tossbank.com")
    );

    private final RestTemplate restTemplate;
    private final CardMongoRepository cardMongoRepository;
    private final ObjectMapper objectMapper;

    @Value("${cardgorilla.api.base-url:https://api.card-gorilla.com:8080/v1}")
    private String baseUrl;

    public void syncAll() {
        List<CardDocument> all = new ArrayList<>();
        for (int p = 1; p <= MAX_PAGES; p++) {
            String url = baseUrl + "/cards?p=" + p + "&perPage=" + PER_PAGE;
            try {
                ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
                String body = res.getBody();
                if (body == null || body.isBlank()) break;

                JsonNode root = objectMapper.readTree(body);
                List<CardDocument> page = parseCardsFromResponse(root);
                if (page.isEmpty()) break;
                all.addAll(page);
                if (page.size() < PER_PAGE) break;
            } catch (Exception e) {
                log.warn("카드고릴라 API 호출 실패 p={}: {}", p, e.getMessage());
                break;
            }
        }

        if (all.isEmpty()) {
            log.warn("카드고릴라 동기화: 수집된 카드가 없습니다. API URL/응답 구조를 확인하세요.");
            return;
        }

        int saved = 0;
        for (CardDocument doc : all) {
            try {
                cardMongoRepository.findByExternalId(doc.getExternalId())
                        .ifPresentOrElse(
                                existing -> {
                                    doc.setId(existing.getId());
                                    cardMongoRepository.save(doc);
                                },
                                () -> cardMongoRepository.save(doc)
                        );
                saved++;
            } catch (Exception e) {
                log.debug("카드 저장 스킵 {}: {}", doc.getExternalId(), e.getMessage());
            }
        }
        log.info("카드고릴라 동기화 완료: {}건 저장", saved);
    }

    /**
     * 응답에서 카드 배열을 찾아 CardDocument 리스트로 변환.
     * data, cards, items, list 등 다양한 키 시도.
     */
    private List<CardDocument> parseCardsFromResponse(JsonNode root) {
        JsonNode array = root.get("data");
        if (array == null) array = root.get("cards");
        if (array == null) array = root.get("items");
        if (array == null) array = root.get("list");
        if (array == null) array = root.isArray() ? root : null;

        if (array == null || !array.isArray()) {
            return Collections.emptyList();
        }

        // API 응답 구조 확인용: 첫 항목의 필드명 로그 (필드 매핑이 안 될 때 참고)
        if (array.size() > 0 && log.isDebugEnabled()) {
            JsonNode first = array.get(0);
            List<String> keys = new ArrayList<>();
            first.fieldNames().forEachRemaining(keys::add);
            log.debug("카드고릴라 API 첫 항목 필드: {}", keys);
        }

        List<CardDocument> list = new ArrayList<>();
        Instant now = Instant.now();
        int index = 0;
        for (JsonNode item : array) {
            try {
                CardDocument doc = mapNodeToCard(item, now, index++);
                if (doc != null && doc.getCardName() != null && !doc.getCardName().isBlank()) {
                    list.add(doc);
                }
            } catch (Exception e) {
                log.trace("카드 항목 파싱 스킵: {}", e.getMessage());
            }
        }
        return list;
    }

    private CardDocument mapNodeToCard(JsonNode node, Instant crawledAt, int index) {
        // 중첩 객체 시도: card, product, data 등으로 한 단계 감싸진 경우
        JsonNode target = node;
        if (node.has("card")) target = node.get("card");
        else if (node.has("product")) target = node.get("product");
        else if (node.has("data")) target = node.get("data");
        else if (node.has("item")) target = node.get("item");

        // 카드고릴라 API: cid("0049"), idx(49), name, corp_txt, annual_fee_basic, pre_month_money, search_benefit, top_benefit, event
        String externalId = text(target, "cid", "id", "cardId", "card_id").orElse(null);
        if (externalId == null) externalId = number(target, "idx", "seq", "no").map(String::valueOf).orElse("cg_" + index + "_" + System.currentTimeMillis());

        String cardName = text(target, "name", "cardName", "card_name", "title", "prdtNm", "productName", "korName").orElse("");
        if (cardName.isBlank()) cardName = textFromPath(target, "brand", "name");
        if (cardName.isBlank()) cardName = textFromPath(target, "product", "name");

        String cardCompany = text(target, "corp_txt", "company", "cardCompany", "card_company", "issuer", "brandName", "issuerName", "companyName", "korCoNm", "finCoNm").orElse("");
        if (cardCompany.isBlank()) cardCompany = textFromPath(target, "corp", "name");
        if (cardCompany.isBlank()) cardCompany = textFromPath(target, "brand", "name");
        if (cardCompany.isBlank()) cardCompany = textFromPath(target, "issuer", "name");
        if (cardCompany.isBlank()) cardCompany = textFromPath(target, "company", "name");

        // 혜택 요약: event.card_detail_text(최대 75만원 혜택), pr_container(HTML 가능)
        String benefitSummary = textFromPath(target, "event", "card_detail_text");
        if (benefitSummary == null || benefitSummary.isBlank()) benefitSummary = text(target, "benefitSummary", "benefit_summary", "promo", "promotion", "promoText", "mainBenefit", "highlight", "summary").orElse(null);
        if (benefitSummary == null) benefitSummary = textFromPath(target, "promotion", "text");
        if (benefitSummary == null) benefitSummary = textFromPath(target, "promo", "description");
        if (benefitSummary != null && benefitSummary.contains("<")) benefitSummary = stripHtml(benefitSummary);

        String annualFeeText = text(target, "annual_fee_basic", "annualFeeText", "annual_fee_text", "annualFee", "annual_fee", "feeText", "fee_desc", "feeDescription").orElse(null);
        if (annualFeeText == null) annualFeeText = textFromPath(target, "fee", "text");
        if (annualFeeText == null) annualFeeText = textFromPath(target, "annualFee", "display");

        Integer annualFeeDomestic = number(target, "annualFeeDomestic", "annual_fee_domestic", "feeDomestic", "domesticFee", "annualFee", "annual_fee").orElse(null);
        if (annualFeeDomestic == null) annualFeeDomestic = numberFromPath(target, "fee", "domestic").orElse(null);
        Integer annualFeeOverseas = number(target, "annualFeeOverseas", "annual_fee_overseas", "feeOverseas", "overseasFee").orElse(null);
        if (annualFeeOverseas == null) annualFeeOverseas = numberFromPath(target, "fee", "overseas").orElse(null);
        if (annualFeeText != null && (annualFeeDomestic == null || annualFeeOverseas == null)) {
            int[] parsed = parseFeeFromText(annualFeeText);
            if (annualFeeDomestic == null && parsed[0] >= 0) annualFeeDomestic = parsed[0];
            if (annualFeeOverseas == null && parsed[1] >= 0) annualFeeOverseas = parsed[1];
        }

        // 전월실적: pre_month_money(숫자) → "전월실적 없음" / "전월실적 N원 이상"
        String previousMonth = text(target, "previousMonthCondition", "previous_month_condition", "preCondition", "pre_condition", "condition", "minUsage", "실적").orElse(null);
        if (previousMonth == null) previousMonth = textFromPath(target, "condition", "text");
        if (previousMonth == null) {
            Integer preMoney = number(target, "pre_month_money", "preMonthMoney", "min_usage").orElse(null);
            if (preMoney != null) previousMonth = preMoney == 0 ? "전월실적 없음" : "전월실적 " + String.format("%,d", preMoney) + "원 이상";
        }

        Integer cardIdx = number(target, "idx", "id", "no").orElse(null);

        String detailUrl = text(target, "detailUrl", "detail_url", "url", "link", "href", "detailLink", "card_url", "cardUrl").orElse(null);
        detailUrl = nullIfBlank(detailUrl);
        if (detailUrl == null) detailUrl = nullIfBlank(textFromPath(target, "link", "url"));
        if (detailUrl == null) detailUrl = nullIfBlank(textFromPath(target, "corp", "url"));
        if (detailUrl == null) detailUrl = nullIfBlank(textFromPath(target, "company", "url"));
        if (detailUrl == null) detailUrl = nullIfBlank(textFromPath(target, "issuer", "url"));
        // 카드고릴라 상세 API의 "카드사 바로가기" 버튼 URL(request_pc/request_m) 우선 사용
        if (detailUrl == null && cardIdx != null) detailUrl = nullIfBlank(fetchIssuerLandingUrl(cardIdx));
        if (detailUrl == null) detailUrl = nullIfBlank(resolveCompanyInfoUrl(cardCompany));
        if (detailUrl == null) {
            if (cardIdx != null) detailUrl = CARD_DETAIL_BASE + cardIdx;
        }

        List<String> benefits = collectBenefits(target);
        if (benefits.isEmpty()) benefits = collectTopBenefit(target);

        List<String> benefitCategories = collectSearchBenefitTitles(target);
        if (benefitCategories.isEmpty()) benefitCategories = stringListFromNode(target, "benefitCategories", "benefit_categories", "categories", "category", "search_benefit_names", "benefitAreas");

        return CardDocument.builder()
                .externalId(externalId)
                .cardName(cardName)
                .cardCompany(cardCompany)
                .benefitSummary(benefitSummary != null && !benefitSummary.isBlank() ? benefitSummary : null)
                .benefits(benefits.isEmpty() ? null : benefits)
                .benefitCategories(benefitCategories.isEmpty() ? null : benefitCategories)
                .annualFeeDomestic(annualFeeDomestic)
                .annualFeeOverseas(annualFeeOverseas)
                .annualFeeText(annualFeeText)
                .previousMonthCondition(previousMonth)
                .detailUrl(detailUrl)
                .crawledAt(crawledAt)
                .build();
    }

    /** 카드고릴라 search_benefit 배열에서 title만 수집 (혜택 분야: 공항라운지/PP, 교통, 기타 등) */
    private static List<String> collectSearchBenefitTitles(JsonNode node) {
        List<String> list = new ArrayList<>();
        JsonNode arr = node.get("search_benefit");
        if (arr == null || !arr.isArray()) return list;
        for (JsonNode item : arr) {
            String title = item.has("title") && item.get("title").isTextual() ? item.get("title").asText().trim() : null;
            if (title != null && !title.isBlank()) list.add(title);
        }
        return list;
    }

    /** 카드고릴라 top_benefit 배열에서 혜택 문구 수집 (title + tags) */
    private static List<String> collectTopBenefit(JsonNode node) {
        List<String> list = new ArrayList<>();
        JsonNode arr = node.get("top_benefit");
        if (arr == null || !arr.isArray()) return list;
        for (JsonNode item : arr) {
            String title = item.has("title") && item.get("title").isTextual() ? item.get("title").asText().trim() : "";
            List<String> tags = new ArrayList<>();
            if (item.has("tags") && item.get("tags").isArray())
                item.get("tags").forEach(t -> { if (t.isTextual()) tags.add(t.asText().trim()); });
            String line = title;
            if (!tags.isEmpty()) line = line + " " + String.join(", ", tags);
            if (!line.isBlank()) list.add(line);
        }
        return list;
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    /** 중첩 경로에서 문자열 추출 (예: node.brand.name) */
    private static String textFromPath(JsonNode node, String first, String second) {
        JsonNode n = node.get(first);
        if (n == null) return "";
        n = n.get(second);
        if (n == null || !n.isTextual()) return "";
        String s = n.asText();
        return s != null ? s.trim() : "";
    }

    private static Optional<Integer> numberFromPath(JsonNode node, String first, String second) {
        JsonNode n = node.get(first);
        if (n == null) return Optional.empty();
        n = n.get(second);
        if (n == null) return Optional.empty();
        if (n.isNumber()) return Optional.of(n.asInt());
        if (n.isTextual()) try {
            return Optional.of(Integer.parseInt(n.asText().replaceAll("[^0-9]", "")));
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    /** 혜택 목록 추출 - 배열/객체 여러 형태 지원 */
    private static List<String> collectBenefits(JsonNode node) {
        List<String> benefits = new ArrayList<>();
        String[] arrayKeys = { "benefits", "benefit", "benefitList", "benefit_list", "descriptions", "featureList", "features", "benefitItems" };
        for (String key : arrayKeys) {
            JsonNode arr = node.get(key);
            if (arr == null || !arr.isArray()) continue;
            for (JsonNode b : arr) {
                if (b.isTextual()) {
                    String s = b.asText().trim();
                    if (!s.isBlank()) benefits.add(s);
                } else if (b.isObject()) {
                    String s = text(b, "text", "description", "content", "name", "label", "title", "desc", "summary").orElse(null);
                    if (s != null && !s.isBlank()) benefits.add(s);
                }
            }
            if (!benefits.isEmpty()) return benefits;
        }
        return benefits;
    }

    private static Optional<String> text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode n = node.get(key);
            if (n != null && n.isTextual()) {
                String s = n.asText();
                if (s != null && !s.isBlank()) return Optional.of(s.trim());
            }
            if (n != null && n.isNumber()) return Optional.of(String.valueOf(n.asInt()));
        }
        return Optional.empty();
    }

    private static Optional<Integer> number(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode n = node.get(key);
            if (n != null && n.isNumber()) return Optional.of(n.asInt());
            if (n != null && n.isTextual()) {
                try {
                    return Optional.of(Integer.parseInt(n.asText().replaceAll("[^0-9]", "")));
                } catch (NumberFormatException ignored) { }
            }
        }
        return Optional.empty();
    }

    /** "국내전용 47,000원 / 해외겸용 49,000원" 형태에서 [국내, 해외] 금액 추출. 없으면 -1 */
    private static int[] parseFeeFromText(String feeText) {
        int domestic = -1, overseas = -1;
        if (feeText == null || feeText.isBlank()) return new int[] { domestic, overseas };
        String numOnly = feeText.replaceAll("[^0-9\\s/]", " ").trim();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("[0-9]+");
        java.util.regex.Matcher m = p.matcher(numOnly);
        if (m.find()) domestic = Integer.parseInt(m.group());
        if (m.find()) overseas = Integer.parseInt(m.group());
        return new int[] { domestic, overseas };
    }

    private static List<String> stringListFromNode(JsonNode node, String... keys) {
        List<String> list = new ArrayList<>();
        for (String key : keys) {
            JsonNode n = node.get(key);
            if (n == null) continue;
            if (n.isArray()) {
                n.forEach(e -> {
                    if (e.isTextual()) list.add(e.asText().trim());
                    else if (e.isNumber()) list.add(String.valueOf(e.asInt()));
                });
                if (!list.isEmpty()) return list;
            }
            if (n.isTextual()) {
                String s = n.asText().trim();
                if (!s.isBlank()) return List.of(s);
            }
        }
        return list;
    }

    private static String resolveCompanyInfoUrl(String cardCompany) {
        if (cardCompany == null || cardCompany.isBlank()) return null;
        String normalized = cardCompany.replace("(주)", "").replace("주식회사", "").trim();
        String mapped = CARD_COMPANY_URLS.get(normalized);
        if (mapped != null) return mapped;

        for (Map.Entry<String, String> e : CARD_COMPANY_URLS.entrySet()) {
            if (normalized.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private static String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * 카드고릴라 상세 API에서 카드사 바로가기 URL 추출.
     * 우선순위: request_pc -> request_m -> event.event_url
     */
    private String fetchIssuerLandingUrl(int cardIdx) {
        try {
            String url = CARD_DETAIL_API_BASE + cardIdx;
            ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
            String body = res.getBody();
            if (body == null || body.isBlank()) return null;

            JsonNode root = objectMapper.readTree(body);
            String requestPc = nullIfBlank(text(root, "request_pc", "requestPc").orElse(null));
            if (requestPc != null) return requestPc;

            String requestM = nullIfBlank(text(root, "request_m", "requestM").orElse(null));
            if (requestM != null) return requestM;

            return nullIfBlank(textFromPath(root, "event", "event_url"));
        } catch (Exception e) {
            log.debug("카드 상세 URL 조회 실패 idx={}: {}", cardIdx, e.getMessage());
            return null;
        }
    }
}
