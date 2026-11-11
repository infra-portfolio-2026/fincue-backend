package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.ChallengeDocument;
import univ.example.fincue.document.UserChallengeDocument;
import univ.example.fincue.dto.*;
import univ.example.fincue.repository.mongo.ChallengeMongoRepository;
import univ.example.fincue.repository.mongo.UserChallengeMongoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeMongoRepository challengeMongoRepository;
    private final UserChallengeMongoRepository userChallengeMongoRepository;
    private final MongoTemplate mongoTemplate;

    /** 나만의 챌린지 요약: 진행 중, 성공, 총 절약 금액 */
    public ChallengeSummaryDto getSummary(String userId) {
        List<UserChallengeDocument> mine = userChallengeMongoRepository.findByUserId(userId);
        long ongoing = 0;
        long success = 0;
        BigDecimal totalSaved = BigDecimal.ZERO;

        Instant now = Instant.now();
        for (UserChallengeDocument uc : mine) {
            ChallengeDocument c = challengeMongoRepository.findById(uc.getChallengeId()).orElse(null);
            if (c == null) continue;

            boolean isOngoing = c.getEndDate() != null && c.getEndDate().isAfter(now);
            if (isOngoing) {
                ongoing++;
            } else {
                if (uc.getIsCompleted() != null && uc.getIsCompleted()) {
                    BigDecimal goal = c.getGoalAmount() != null ? c.getGoalAmount() : BigDecimal.ZERO;
                    BigDecimal saved = uc.getSavedAmount() != null ? uc.getSavedAmount() : BigDecimal.ZERO;
                    if (saved.compareTo(goal) >= 0) success++;
                }
            }
            if (uc.getSavedAmount() != null) {
                totalSaved = totalSaved.add(uc.getSavedAmount());
            }
        }
        return ChallengeSummaryDto.builder()
                .ongoingCount(ongoing)
                .successCount(success)
                .totalSavedAmount(totalSaved)
                .build();
    }

    /** 진행 중인 도전 목록 */
    public List<OngoingChallengeDto> getMyOngoingChallenges(String userId) {
        List<UserChallengeDocument> mine = userChallengeMongoRepository.findByUserId(userId);
        Instant now = Instant.now();
        List<OngoingChallengeDto> result = new ArrayList<>();

        for (UserChallengeDocument uc : mine) {
            ChallengeDocument c = challengeMongoRepository.findById(uc.getChallengeId()).orElse(null);
            if (c == null || c.getEndDate() == null || !c.getEndDate().isAfter(now)) continue;

            BigDecimal goal = c.getGoalAmount() != null ? c.getGoalAmount() : BigDecimal.ZERO;
            BigDecimal saved = uc.getSavedAmount() != null ? uc.getSavedAmount() : BigDecimal.ZERO;
            double progress = goal.compareTo(BigDecimal.ZERO) > 0
                    ? saved.multiply(BigDecimal.valueOf(100)).divide(goal, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            BigDecimal remaining = goal.subtract(saved).max(BigDecimal.ZERO);

            result.add(OngoingChallengeDto.builder()
                    .challengeId(c.getId())
                    .title(c.getTitle())
                    .category(c.getCategory() != null ? c.getCategory() : "기타")
                    .progressPercent(Math.min(100.0, progress))
                    .savedAmount(saved)
                    .goalAmount(goal)
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .remainingAmount(remaining)
                    .build());
        }
        result.sort(Comparator.comparing(OngoingChallengeDto::getEndDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /** 완료된 도전 목록 */
    public List<CompletedChallengeDto> getMyCompletedChallenges(String userId) {
        List<UserChallengeDocument> mine = userChallengeMongoRepository.findByUserId(userId);
        Instant now = Instant.now();
        List<CompletedChallengeDto> result = new ArrayList<>();

        for (UserChallengeDocument uc : mine) {
            ChallengeDocument c = challengeMongoRepository.findById(uc.getChallengeId()).orElse(null);
            if (c == null || c.getEndDate() == null || !c.getEndDate().isBefore(now)) continue;

            BigDecimal goal = c.getGoalAmount() != null ? c.getGoalAmount() : BigDecimal.ZERO;
            BigDecimal saved = uc.getSavedAmount() != null ? uc.getSavedAmount() : BigDecimal.ZERO;
            boolean success = saved.compareTo(goal) >= 0;

            result.add(CompletedChallengeDto.builder()
                    .challengeId(c.getId())
                    .title(c.getTitle())
                    .category(c.getCategory() != null ? c.getCategory() : "기타")
                    .success(success)
                    .endDate(c.getEndDate())
                    .savedAmount(saved)
                    .build());
        }
        result.sort(Comparator.comparing(CompletedChallengeDto::getEndDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    /** 새 도전 만들기 */
    public ChallengeDocument createChallenge(String userId, CreateChallengeRequest request) {
        Instant start = LocalDate.parse(request.getStartDate()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = LocalDate.parse(request.getEndDate()).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        ChallengeDocument challenge = ChallengeDocument.builder()
                .title(request.getTitle())
                .category(request.getCategory() != null ? request.getCategory() : "기타")
                .goalAmount(request.getGoalAmount() != null ? request.getGoalAmount() : BigDecimal.ZERO)
                .startDate(start)
                .endDate(end)
                .createdByUserId(userId)
                .build();
        challenge = challengeMongoRepository.save(challenge);

        UserChallengeDocument uc = UserChallengeDocument.builder()
                .userId(userId)
                .challengeId(challenge.getId())
                .progressPercent(0.0)
                .isCompleted(false)
                .savedAmount(BigDecimal.ZERO)
                .build();
        userChallengeMongoRepository.save(uc);

        return challenge;
    }

    /** 도전 수정 (진행 중인 것만) */
    public ChallengeDocument updateChallenge(String userId, String challengeId, UpdateChallengeRequest request) {
        ChallengeDocument c = challengeMongoRepository.findById(challengeId).orElseThrow(() -> new IllegalArgumentException("도전을 찾을 수 없습니다."));
        if (c.getCreatedByUserId() == null || !c.getCreatedByUserId().equals(userId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        Instant now = Instant.now();
        if (c.getEndDate() != null && !c.getEndDate().isAfter(now)) {
            throw new IllegalArgumentException("완료된 도전은 수정할 수 없습니다.");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) c.setTitle(request.getTitle());
        if (request.getCategory() != null) c.setCategory(request.getCategory());
        if (request.getGoalAmount() != null) c.setGoalAmount(request.getGoalAmount());
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            c.setStartDate(LocalDate.parse(request.getStartDate()).atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            c.setEndDate(LocalDate.parse(request.getEndDate()).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        }
        return challengeMongoRepository.save(c);
    }

    /** 도전 삭제 (생성자만 가능) */
    public void deleteChallenge(String userId, String challengeId) {
        ChallengeDocument c = challengeMongoRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("도전을 찾을 수 없습니다."));
        if (c.getCreatedByUserId() == null || !c.getCreatedByUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        userChallengeMongoRepository.deleteByChallengeId(challengeId);
        challengeMongoRepository.delete(c);
    }

    /** 사람들이 많이 한 도전 (참여자 수 순) */
    @SuppressWarnings("unchecked")
    public List<PopularChallengeDto> getPopularChallenges(int limit) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("challengeId").count().as("participantCount"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "participantCount"),
                Aggregation.limit(limit)
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "userChallenges", Map.class);
        List<Map> grouped = results.getMappedResults();

        List<PopularChallengeDto> list = new ArrayList<>();
        for (Map m : grouped) {
            String challengeId = (String) m.get("_id");
            long participantCount = ((Number) m.getOrDefault("participantCount", 0)).longValue();
            ChallengeDocument c = challengeMongoRepository.findById(challengeId).orElse(null);
            if (c == null) continue;

            BigDecimal goal = c.getGoalAmount() != null ? c.getGoalAmount() : BigDecimal.ZERO;
            double achievementRate = 0.0; // TODO: 전체 참여자 평균 달성률 계산 가능
            list.add(PopularChallengeDto.builder()
                    .challengeId(c.getId())
                    .title(c.getTitle())
                    .category(c.getCategory() != null ? c.getCategory() : "기타")
                    .goalAmount(goal)
                    .achievementRate(achievementRate)
                    .participantCount(participantCount)
                    .build());
        }
        return list;
    }

    /** 도전 함께하기 (인기 도전 참여) */
    public UserChallengeDocument joinChallenge(String userId, String challengeId) {
        challengeMongoRepository.findById(challengeId).orElseThrow(() -> new IllegalArgumentException("도전을 찾을 수 없습니다."));
        if (userChallengeMongoRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent()) {
            throw new IllegalArgumentException("이미 참여 중인 도전입니다.");
        }
        UserChallengeDocument uc = UserChallengeDocument.builder()
                .userId(userId)
                .challengeId(challengeId)
                .progressPercent(0.0)
                .isCompleted(false)
                .savedAmount(BigDecimal.ZERO)
                .build();
        return userChallengeMongoRepository.save(uc);
    }
}
