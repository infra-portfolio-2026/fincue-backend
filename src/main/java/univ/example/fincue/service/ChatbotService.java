package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.ChatMessageDocument;
import univ.example.fincue.document.ChatSessionDocument;
import univ.example.fincue.document.IncomeDocument;
import univ.example.fincue.document.SpendingDocument;
import univ.example.fincue.dto.ChatMessageDto;
import univ.example.fincue.dto.ChatResponse;
import univ.example.fincue.dto.ChatSessionDto;
import univ.example.fincue.dto.CommunityPostDto;
import univ.example.fincue.dto.CommunityStatsDto;
import univ.example.fincue.dto.OngoingChallengeDto;
import univ.example.fincue.dto.PopularChallengeDto;
import univ.example.fincue.dto.PostSummaryDto;
import univ.example.fincue.dto.RecommendedProductDto;
import univ.example.fincue.dto.SpendingAnalysisDto;
import univ.example.fincue.repository.mongo.ChatMessageMongoRepository;
import univ.example.fincue.repository.mongo.ChatSessionMongoRepository;
import univ.example.fincue.repository.mongo.IncomeMongoRepository;
import univ.example.fincue.repository.mongo.SpendingMongoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 챗봇 오케스트레이션 서비스.
 *
 * 답변 품질 전략:
 * 1) 시스템 프롬프트에 사용자의 이번달 소비·수입 요약과 월간 소비 분석을 항상 주입.
 * 2) 메시지 의도(추천/챌린지/커뮤니티)를 키워드로 감지해, 해당 의도일 때만
 *    {@link RecommendationService}, {@link ChallengeService} 등 결과를 추가 컨텍스트로 주입한다.
 * 3) "데이터에 있는 것만 인용·한국어만·금액 표기 그대로" 규칙으로 할루시네이션을 줄인다.
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    /** LLM에 전달할 최근 대화 메시지 최대 개수 (왕복 기준 N*2개 메시지) */
    private static final int HISTORY_LIMIT = 12;
    private static final int TITLE_MAX_LEN = 30;

    /** 추천/챌린지/커뮤니티 컨텍스트에 넣을 상위 N개 */
    private static final int RECOMMENDATION_TOP_N = 5;
    private static final int CHALLENGE_TOP_N = 5;
    private static final int COMMUNITY_TOP_N = 5;

    private final ChatSessionMongoRepository chatSessionRepo;
    private final ChatMessageMongoRepository chatMessageRepo;
    private final SpendingMongoRepository spendingMongoRepository;
    private final IncomeMongoRepository incomeMongoRepository;
    private final RecommendationService recommendationService;
    private final ChallengeService challengeService;
    private final SpendingService spendingService;
    private final PostService postService;
    private final CommunityService communityService;
    private final LlmClient llmClient;

    @Value("${chatbot.system-prompt:너는 사용자의 소비·수입 데이터를 바탕으로 친근하게 도와주는 한국어 금융 비서야. " +
            "답변은 3~6문장으로 간결하게.}")
    private String systemPromptBase;

    /* ============================ 세션 ============================ */

    /** 새 세션 생성. title 이 비어 있으면 "새 대화" 로 시작. */
    public ChatSessionDto createSession(String userId, String title) {
        Instant now = Instant.now();
        ChatSessionDocument session = ChatSessionDocument.builder()
                .userId(userId)
                .title(title != null && !title.isBlank() ? trimTitle(title) : "새 대화")
                .createdAt(now)
                .lastMessageAt(now)
                .build();
        ChatSessionDocument saved = chatSessionRepo.save(session);
        return toSessionDto(saved);
    }

    public List<ChatSessionDto> listSessions(String userId) {
        return chatSessionRepo.findByUserIdOrderByLastMessageAtDesc(userId).stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto> listMessages(String userId, String sessionId) {
        ChatSessionDocument session = requireOwnedSession(userId, sessionId);
        return chatMessageRepo.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    public void deleteSession(String userId, String sessionId) {
        ChatSessionDocument session = requireOwnedSession(userId, sessionId);
        chatMessageRepo.deleteBySessionId(session.getId());
        chatSessionRepo.deleteById(session.getId());
    }

    /* ============================ 메시지 ============================ */

    /**
     * 메시지 전송 → LLM 응답 생성 → 양쪽 모두 저장 후 응답 반환.
     * sessionId 가 비어 있으면 새 세션을 만든다.
     */
    public ChatResponse sendMessage(String userId, String sessionId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("message 가 비어 있습니다.");
        }

        ChatSessionDocument session = (sessionId != null && !sessionId.isBlank())
                ? requireOwnedSession(userId, sessionId)
                : openNewSession(userId, userMessage);

        Instant now = Instant.now();

        chatMessageRepo.save(ChatMessageDocument.builder()
                .sessionId(session.getId())
                .userId(userId)
                .role("USER")
                .content(userMessage)
                .createdAt(now)
                .build());

        List<Map<String, String>> llmMessages = LlmClient.newMessages();
        llmMessages.add(LlmClient.message(LlmClient.ROLE_SYSTEM, buildSystemPrompt(userId, userMessage)));

        List<ChatMessageDocument> recent = chatMessageRepo.findBySessionIdOrderByCreatedAtDesc(
                session.getId(), PageRequest.of(0, HISTORY_LIMIT));
        recent.sort(Comparator.comparing(ChatMessageDocument::getCreatedAt));
        for (ChatMessageDocument m : recent) {
            String role = "ASSISTANT".equals(m.getRole()) ? LlmClient.ROLE_ASSISTANT : LlmClient.ROLE_USER;
            llmMessages.add(LlmClient.message(role, m.getContent()));
        }

        String reply = llmClient.complete(llmMessages);

        Instant replyAt = Instant.now();
        ChatMessageDocument assistantMsg = chatMessageRepo.save(ChatMessageDocument.builder()
                .sessionId(session.getId())
                .userId(userId)
                .role("ASSISTANT")
                .content(reply)
                .createdAt(replyAt)
                .build());

        session.setLastMessageAt(replyAt);
        chatSessionRepo.save(session);

        return ChatResponse.builder()
                .sessionId(session.getId())
                .reply(reply)
                .messageId(assistantMsg.getId())
                .createdAt(replyAt)
                .build();
    }

    /* ============================ 내부 유틸 ============================ */

    private ChatSessionDocument requireOwnedSession(String userId, String sessionId) {
        ChatSessionDocument session = chatSessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 세션이 아닙니다.");
        }
        return session;
    }

    private ChatSessionDocument openNewSession(String userId, String firstUserMessage) {
        Instant now = Instant.now();
        ChatSessionDocument session = ChatSessionDocument.builder()
                .userId(userId)
                .title(trimTitle(firstUserMessage))
                .createdAt(now)
                .lastMessageAt(now)
                .build();
        return chatSessionRepo.save(session);
    }

    /* ============================ 컨텍스트 빌더 ============================ */

    /**
     * 시스템 프롬프트: 페르소나 + 엄격 규칙 + 사용자 컨텍스트(+ 의도별 추가 데이터).
     * 사용자 메시지에서 의도를 추론해 추천/챌린지/분석 데이터를 골라 붙인다.
     */
    private String buildSystemPrompt(String userId, String userMessage) {
        Set<Intent> intents = detectIntents(userMessage);

        StringBuilder sb = new StringBuilder();
        sb.append(systemPromptBase).append("\n\n");

        sb.append("[엄격 규칙]\n");
        sb.append("- 응답은 한국어로만 작성해. 한자(漢字)·일본어·중국어 표기(예: 外食, 取消)·히라가나·가타카나는 쓰지 마. " +
                "영어는 [데이터]에 적힌 고유명·URL만 그대로 인용할 수 있어.\n");
        sb.append("- 아래 [데이터]에 있는 정보만 사실로 말해. 없는 상품·챌린지·금액·비율을 지어내지 마.\n");
        sb.append("- 금액은 [데이터]에 적힌 표기(쉼표 포함 원 단위)를 그대로 복사해. " +
                "13만 원·130만 원처럼 읽기만 바꾸거나 자릿수를 바꾸는 행위는 금지야.\n");
        sb.append("- 첫 1~2문장은 반드시 [데이터]의 이번달 총지출·카테고리별 금액 등 숫자를 인용한 요약이어야 해. " +
                "데이터와 무관한 일반 절약 상식만 나열하지 마.\n");
        sb.append("- 어떤 카테고리를 줄이자고 할 때는, 그 카테고리가 [데이터]에 실제로 있고 금액이 얼마인지 먼저 말한 뒤에만 조언해.\n");
        sb.append("- 데이터가 '없음' 으로 표시되면 \"아직 데이터가 없어요\" 라고 솔직하게 답해.\n");
        sb.append("- 금리, 금액, 카테고리는 데이터에 적힌 값 그대로만 쓰고 반올림하지 마.\n");
        sb.append("- 상품/챌린지를 소개할 땐 항상 이름과 (간단한 이유)를 함께 말해.\n\n");

        sb.append("[데이터 - 사용자 이번달 소비 요약]\n");
        sb.append(buildSpendingContext(userId));

        sb.append("\n[데이터 - 사용자 이번달 수입 요약]\n");
        sb.append(buildIncomeContext(userId));

        sb.append("\n[데이터 - 소비 분석 (이번달 vs 지난달)]\n");
        sb.append(buildSpendingAnalysisContext(userId));

        if (intents.contains(Intent.RECOMMEND)) {
            sb.append("\n[데이터 - 맞춤 추천 상품 Top ").append(RECOMMENDATION_TOP_N).append("]\n");
            sb.append(buildRecommendationContext(userId));
        }

        if (intents.contains(Intent.CHALLENGE)) {
            sb.append("\n[데이터 - 내 진행 중 챌린지]\n");
            sb.append(buildOngoingChallengeContext(userId));
            sb.append("\n[데이터 - 인기 챌린지 Top ").append(CHALLENGE_TOP_N).append("]\n");
            sb.append(buildPopularChallengeContext());
        }

        if (intents.contains(Intent.COMMUNITY)) {
            sb.append("\n[데이터 - 커뮤니티 통계]\n");
            sb.append(buildCommunityStatsContext());
            sb.append("\n[데이터 - 대표 절약팁 Top 3 (최근 일주일, 좋아요순)]\n");
            sb.append(buildTipPostContext());
            sb.append("\n[데이터 - 실시간 HOT 게시글 Top 3 (최근 일주일, 좋아요순)]\n");
            sb.append(buildHotPostContext());
            sb.append("\n[데이터 - 최신 커뮤니티 게시글 Top ").append(COMMUNITY_TOP_N).append("]\n");
            sb.append(buildRecentCommunityPostContext(userId));
        }

        return sb.toString();
    }

    /** 메시지 안의 키워드로 의도 분류 (단순/빠름). 여러 의도가 동시에 잡힐 수 있다. */
    private Set<Intent> detectIntents(String message) {
        Set<Intent> out = EnumSet.noneOf(Intent.class);
        if (message == null) return out;
        String m = message;

        if (containsAny(m, "추천", "적금", "예금", "카드", "금융상품", "상품")) {
            out.add(Intent.RECOMMEND);
        }
        if (containsAny(m, "챌린지", "도전", "절약 챌린지", "참여")) {
            out.add(Intent.CHALLENGE);
        }
        if (containsAny(m, "커뮤니티", "게시글", "글", "팁", "절약팁", "인기글", "핫", "HOT", "hot", "질문", "후기")) {
            out.add(Intent.COMMUNITY);
        }
        return out;
    }

    private static boolean containsAny(String src, String... keywords) {
        if (src == null) return false;
        for (String k : keywords) {
            if (src.contains(k)) return true;
        }
        return false;
    }

    private String buildSpendingContext(String userId) {
        YearMonth ym = YearMonth.now(ZoneId.systemDefault());
        ZonedDateTime start = LocalDate.of(ym.getYear(), ym.getMonthValue(), 1)
                .atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime end = start.plusMonths(1);

        List<SpendingDocument> list = spendingMongoRepository.findByUserIdAndSpentAtBetween(
                userId, start.toInstant(), end.toInstant());

        if (list == null || list.isEmpty()) {
            return "- 이번달 소비 데이터: 없음\n";
        }

        Map<String, BigDecimal> byCategory = list.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategory() != null && !s.getCategory().isBlank() ? s.getCategory() : "기타",
                        Collectors.reducing(BigDecimal.ZERO,
                                s -> s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add)));

        BigDecimal total = byCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- 기준 월: %d년 %d월%n", ym.getYear(), ym.getMonthValue()));
        sb.append(String.format("- 이번달 총 지출: %,d원%n", total.longValue()));
        sb.append("- ※ 답변에 금액을 쓸 때는 위와 동일한 표기(예: 130,000원)를 그대로 사용. 만 원 단위로만 바꿔 말하지 마.\n");
        sb.append("- 카테고리별 지출:\n");

        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(byCategory.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<String, BigDecimal> e : sorted) {
            sb.append(String.format("  · %s: %,d원%n", e.getKey(), e.getValue().longValue()));
        }
        return sb.toString();
    }

    private String buildIncomeContext(String userId) {
        YearMonth ym = YearMonth.now(ZoneId.systemDefault());
        ZonedDateTime start = LocalDate.of(ym.getYear(), ym.getMonthValue(), 1)
                .atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime end = start.plusMonths(1);

        List<IncomeDocument> list = incomeMongoRepository.findByUserIdAndReceivedAtBetween(
                userId, start.toInstant(), end.toInstant());

        if (list == null || list.isEmpty()) {
            return "- 이번달 수입 데이터: 없음\n";
        }

        Map<String, BigDecimal> byCategory = list.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategory() != null && !s.getCategory().isBlank() ? s.getCategory() : "기타",
                        Collectors.reducing(BigDecimal.ZERO,
                                s -> s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add)));

        BigDecimal total = byCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- 기준 월: %d년 %d월%n", ym.getYear(), ym.getMonthValue()));
        sb.append(String.format("- 이번달 총 수입: %,d원%n", total.longValue()));
        sb.append("- 카테고리별 수입:\n");

        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(byCategory.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<String, BigDecimal> e : sorted) {
            sb.append(String.format("  · %s: %,d원%n", e.getKey(), e.getValue().longValue()));
        }
        return sb.toString();
    }

    private String buildSpendingAnalysisContext(String userId) {
        try {
            YearMonth ym = YearMonth.now(ZoneId.systemDefault());
            SpendingAnalysisDto a = spendingService.getSpendingAnalysis(userId, ym.getYear(), ym.getMonthValue());

            BigDecimal cur = nz(a.getCurrentMonthTotal());
            BigDecimal prev = nz(a.getPreviousMonthTotal());
            BigDecimal diff = nz(a.getDiffAmount());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("- 이번달 총지출: %,d원%n", cur.longValue()));
            sb.append(String.format("- 지난달 총지출: %,d원%n", prev.longValue()));
            sb.append(String.format("- 증감: %,d원 (%.1f%%)%n", diff.longValue(), a.getDiffRatePercent()));
            if (a.getAverageMonthlySpending() != null) {
                sb.append(String.format("- 최근 12개월 평균 지출: %,d원%n", a.getAverageMonthlySpending().longValue()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "- 소비 분석 데이터: 계산 실패\n";
        }
    }

    private String buildRecommendationContext(String userId) {
        List<RecommendedProductDto> list = recommendationService
                .getRecommendedProductsForUser(userId, RECOMMENDATION_TOP_N);

        if (list == null || list.isEmpty()) {
            return "- 추천 가능한 상품: 없음 (먼저 /api/sync/products, /api/sync/cards 동기화가 필요할 수 있어요)\n";
        }

        StringBuilder sb = new StringBuilder();
        for (RecommendedProductDto p : list) {
            sb.append(String.format("- [%d위][%s] %s / %s%n",
                    p.getRank(),
                    p.getProductType() != null ? p.getProductType() : "기타",
                    safe(p.getBankName()),
                    safe(p.getProductName())));
            if (p.getRecommendationReason() != null && !p.getRecommendationReason().isBlank()) {
                sb.append("    · 이유: ").append(p.getRecommendationReason()).append("\n");
            }
            if (p.getBenefits() != null && !p.getBenefits().isEmpty()) {
                String benefits = p.getBenefits().stream()
                        .limit(3)
                        .collect(Collectors.joining(", "));
                sb.append("    · 혜택: ").append(benefits).append("\n");
            }
            if (p.getDetailUrl() != null && !p.getDetailUrl().isBlank()) {
                sb.append("    · 상세: ").append(p.getDetailUrl()).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildOngoingChallengeContext(String userId) {
        List<OngoingChallengeDto> list = challengeService.getMyOngoingChallenges(userId);
        if (list == null || list.isEmpty()) {
            return "- 진행 중인 챌린지: 없음\n";
        }
        StringBuilder sb = new StringBuilder();
        for (OngoingChallengeDto c : list) {
            sb.append(String.format("- %s (%s): 진행률 %.1f%%, %,d원 / %,d원%n",
                    safe(c.getTitle()),
                    safe(c.getCategory()),
                    c.getProgressPercent(),
                    nz(c.getSavedAmount()).longValue(),
                    nz(c.getGoalAmount()).longValue()));
        }
        return sb.toString();
    }

    private String buildPopularChallengeContext() {
        try {
            List<PopularChallengeDto> list = challengeService.getPopularChallenges(CHALLENGE_TOP_N);
            if (list == null || list.isEmpty()) {
                return "- 인기 챌린지: 없음\n";
            }
            StringBuilder sb = new StringBuilder();
            int rank = 1;
            for (PopularChallengeDto c : list) {
                sb.append(String.format("- [%d위] %s (%s) · 목표 %,d원 · 참여자 %d명%n",
                        rank++,
                        safe(c.getTitle()),
                        safe(c.getCategory()),
                        nz(c.getGoalAmount()).longValue(),
                        c.getParticipantCount()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "- 인기 챌린지: 조회 실패\n";
        }
    }

    private String buildCommunityStatsContext() {
        try {
            CommunityStatsDto s = communityService.getStats();
            return String.format(
                    "- 전체 게시글: %d개%n- 절약팁 게시글: %d개%n- 활성 회원: %d명%n- 오늘의 인기글: %d개%n",
                    s.getTotalPosts(), s.getTipCount(), s.getActiveMemberCount(), s.getTodayPopularCount());
        } catch (Exception e) {
            return "- 커뮤니티 통계: 조회 실패\n";
        }
    }

    private String buildTipPostContext() {
        try {
            List<PostSummaryDto> list = postService.getTipPosts();
            if (list == null || list.isEmpty()) {
                return "- 대표 절약팁: 없음\n";
            }
            return list.stream()
                    .map(p -> String.format("- %s · 좋아요 %d · 댓글 %d · %s",
                            safe(p.getTitle()), p.getLikeCount(), p.getCommentCount(), safe(p.getDate())))
                    .collect(Collectors.joining("\n", "", "\n"));
        } catch (Exception e) {
            return "- 대표 절약팁: 조회 실패\n";
        }
    }

    private String buildHotPostContext() {
        try {
            List<PostSummaryDto> list = postService.getHotPosts();
            if (list == null || list.isEmpty()) {
                return "- HOT 게시글: 없음\n";
            }
            return list.stream()
                    .map(p -> String.format("- %s · 좋아요 %d · 댓글 %d · %s",
                            safe(p.getTitle()), p.getLikeCount(), p.getCommentCount(), safe(p.getDate())))
                    .collect(Collectors.joining("\n", "", "\n"));
        } catch (Exception e) {
            return "- HOT 게시글: 조회 실패\n";
        }
    }

    private String buildRecentCommunityPostContext(String userId) {
        try {
            List<CommunityPostDto> list = communityService.getPosts(null, null, 0, COMMUNITY_TOP_N, Optional.ofNullable(userId));
            if (list == null || list.isEmpty()) {
                return "- 최신 게시글: 없음\n";
            }
            StringBuilder sb = new StringBuilder();
            for (CommunityPostDto p : list) {
                sb.append(String.format("- [%s] %s · 좋아요 %d · 댓글 %d (by %s)%n",
                        safe(p.getCategory()),
                        safe(p.getTitle()),
                        p.getLikeCount(),
                        p.getCommentCount(),
                        safe(p.getAuthorName())));
            }
            return sb.toString();
        } catch (Exception e) {
            return "- 최신 게시글: 조회 실패\n";
        }
    }

    /* ============================ DTO 변환 / 유틸 ============================ */

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String trimTitle(String s) {
        String clean = s.replaceAll("\\s+", " ").trim();
        if (clean.length() <= TITLE_MAX_LEN) return clean;
        return clean.substring(0, TITLE_MAX_LEN) + "…";
    }

    private ChatSessionDto toSessionDto(ChatSessionDocument s) {
        return ChatSessionDto.builder()
                .id(s.getId())
                .title(s.getTitle())
                .createdAt(s.getCreatedAt())
                .lastMessageAt(s.getLastMessageAt())
                .build();
    }

    private ChatMessageDto toMessageDto(ChatMessageDocument m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private enum Intent { RECOMMEND, CHALLENGE, COMMUNITY }
}
