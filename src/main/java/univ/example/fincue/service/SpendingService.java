package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.IncomeDocument;
import univ.example.fincue.document.SpendingDocument;
import univ.example.fincue.dto.*;
import univ.example.fincue.repository.mongo.IncomeMongoRepository;
import univ.example.fincue.repository.mongo.SpendingMongoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpendingService {

    private final SpendingMongoRepository spendingMongoRepository;
    private final IncomeMongoRepository incomeMongoRepository;

    /** 소비 내역 추가 */
    public SpendingDocument addSpending(String userId, String spentDate, String category, BigDecimal amount) {
        // 날짜만 의미하므로 UTC 미드나잇으로 저장 -> DB에 입력 날짜 그대로 보이도록
        Instant spentAt = java.time.LocalDate.parse(spentDate)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant();
        SpendingDocument doc = SpendingDocument.builder()
                .userId(userId)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .category(category != null && !category.isBlank() ? category : "기타")
                .spentAt(spentAt)
                .build();
        return spendingMongoRepository.save(doc);
    }

    /** 이번달 소비분석: 카테고리별 금액 + 총액 */
    public MonthlySpendingSummaryDto getMonthlySummary(String userId, int year, int month) {
        ZonedDateTime start = LocalDate.of(year, month, 1).atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime end = start.plusMonths(1);
        Instant startInstant = start.toInstant();
        Instant endInstant = end.toInstant();

        List<SpendingDocument> list = spendingMongoRepository.findByUserIdAndSpentAtBetween(
                userId, startInstant, endInstant);

        Map<String, BigDecimal> byCategory = list.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategory() != null && !s.getCategory().isBlank() ? s.getCategory() : "기타",
                        Collectors.reducing(BigDecimal.ZERO, SpendingDocument::getAmount, BigDecimal::add)
                ));

        List<CategoryAmountDto> byCategoryList = byCategory.entrySet().stream()
                .map(e -> CategoryAmountDto.builder()
                        .category(e.getKey())
                        .amount(e.getValue())
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = byCategoryList.stream()
                .map(CategoryAmountDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonthlySpendingSummaryDto.builder()
                .year(year)
                .month(month)
                .byCategory(byCategoryList)
                .totalAmount(total)
                .build();
    }

    /** 소비 분석 대시보드 데이터 (카드 + 추이 + 전월 비교) */
    public SpendingAnalysisDto getSpendingAnalysis(String userId, int year, int month) {
        YearMonth base = YearMonth.of(year, month);
        YearMonth prev = base.minusMonths(1);

        List<SpendingDocument> currentList = findByUserAndMonth(userId, base);
        List<SpendingDocument> prevList = findByUserAndMonth(userId, prev);

        BigDecimal currentTotal = sumAmount(currentList);
        BigDecimal prevTotal = sumAmount(prevList);

        BigDecimal diffAmount = currentTotal.subtract(prevTotal);
        double diffRate = BigDecimal.ZERO.equals(prevTotal)
                ? 0.0
                : diffAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(prevTotal, 2, RoundingMode.HALF_UP)
                    .doubleValue();

        // 월별 추이 (최근 12개월)
        List<MonthlySpendingPointDto> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = base.minusMonths(i);
            List<SpendingDocument> list = findByUserAndMonth(userId, ym);
            BigDecimal total = sumAmount(list);
            trend.add(MonthlySpendingPointDto.builder()
                    .year(ym.getYear())
                    .month(ym.getMonthValue())
                    .totalAmount(total)
                    .build());
        }

        BigDecimal trendSum = trend.stream()
                .map(MonthlySpendingPointDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = trendSum.divide(BigDecimal.valueOf(trend.size()), 0, RoundingMode.HALF_UP);

        // 카테고리별 분포 (현재 달)
        Map<String, BigDecimal> currentByCategory = groupByCategory(currentList);
        List<CategoryAmountDto> categoryBreakdown = currentByCategory.entrySet().stream()
                .map(e -> CategoryAmountDto.builder()
                        .category(e.getKey())
                        .amount(e.getValue())
                        .build())
                .collect(Collectors.toList());

        // 전월 vs 이번달 카테고리 비교 (keySet()은 수정 불가 → mutable Set 사용)
        Map<String, BigDecimal> prevByCategory = groupByCategory(prevList);
        Set<String> allCategories = new HashSet<>(currentByCategory.keySet());
        allCategories.addAll(prevByCategory.keySet());

        List<CategoryCompareDto> categoryComparison = allCategories.stream()
                .map(cat -> {
                    BigDecimal prevAmt = prevByCategory.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal currAmt = currentByCategory.getOrDefault(cat, BigDecimal.ZERO);
                    return CategoryCompareDto.builder()
                            .category(cat)
                            .previousAmount(prevAmt)
                            .currentAmount(currAmt)
                            .diffAmount(currAmt.subtract(prevAmt))
                            .build();
                })
                .collect(Collectors.toList());

        return SpendingAnalysisDto.builder()
                .year(year)
                .month(month)
                .currentMonthTotal(currentTotal)
                .previousMonthTotal(prevTotal)
                .diffAmount(diffAmount)
                .diffRatePercent(diffRate)
                .averageMonthlySpending(avg)
                .monthlyTrend(trend)
                .categoryBreakdown(categoryBreakdown)
                .categoryComparison(categoryComparison)
                .build();
    }

    /** 월별 일자 기준 수입/지출 흐름 */
    public MonthlyCashflowDto getMonthlyCashflow(String userId, int year, int month) {
        ZonedDateTime start = LocalDate.of(year, month, 1).atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime end = start.plusMonths(1);
        Instant startInstant = start.toInstant();
        Instant endInstant = end.toInstant();

        List<SpendingDocument> spendings = spendingMongoRepository.findByUserIdAndSpentAtBetween(userId, startInstant, endInstant);
        List<IncomeDocument> incomes = incomeMongoRepository.findByUserIdAndReceivedAtBetween(userId, startInstant, endInstant);

        Map<LocalDate, BigDecimal> spendingByDate = spendings.stream()
                .filter(s -> s.getSpentAt() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getSpentAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, s -> s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));

        Map<LocalDate, BigDecimal> incomeByDate = incomes.stream()
                .filter(i -> i.getReceivedAt() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getReceivedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));

        Map<LocalDate, DailyCashflowPointDto> dailyMap = new LinkedHashMap<>();
        LocalDate cursor = LocalDate.of(year, month, 1);
        LocalDate monthEnd = YearMonth.of(year, month).atEndOfMonth();
        while (!cursor.isAfter(monthEnd)) {
            BigDecimal income = incomeByDate.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal spending = spendingByDate.getOrDefault(cursor, BigDecimal.ZERO);
            dailyMap.put(cursor, DailyCashflowPointDto.builder()
                    .date(cursor.toString())
                    .incomeAmount(income)
                    .spendingAmount(spending)
                    .netAmount(income.subtract(spending))
                    .build());
            cursor = cursor.plusDays(1);
        }

        BigDecimal totalIncome = dailyMap.values().stream()
                .map(DailyCashflowPointDto::getIncomeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpending = dailyMap.values().stream()
                .map(DailyCashflowPointDto::getSpendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonthlyCashflowDto.builder()
                .year(year)
                .month(month)
                .totalIncome(totalIncome)
                .totalSpending(totalSpending)
                .netAmount(totalIncome.subtract(totalSpending))
                .daily(new ArrayList<>(dailyMap.values()))
                .build();
    }

    /**
     * 특정 날짜 상세 (캘린더에서 날짜 클릭 시 사용).
     * - {@link #getMonthlyCashflow} 와 동일하게 {@link ZoneId#systemDefault()} 의 달력 하루(00:00~24:00)로 범위를 잡는다.
     *   (UTC 자정으로 자르면 cashflow 일자별 금액과 상세 조회 결과가 어긋날 수 있음)
     * - 카테고리별 합계는 금액 내림차순 정렬.
     */
    public DailyDetailDto getDailyDetail(String userId, LocalDate date) {
        ZoneId zone = ZoneId.systemDefault();
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<SpendingDocument> spendings = spendingMongoRepository
                .findByUserIdAndSpentAtBetween(userId, start, end);
        List<IncomeDocument> incomes = incomeMongoRepository
                .findByUserIdAndReceivedAtBetween(userId, start, end);

        BigDecimal totalSpending = sumAmount(spendings);
        BigDecimal totalIncome = sumIncomeAmount(incomes);

        List<CategoryAmountDto> spendingByCategory = toCategoryAmountSorted(groupByCategory(spendings));
        List<CategoryAmountDto> incomeByCategory = toCategoryAmountSorted(groupIncomeByCategory(incomes));

        spendings.sort(Comparator.comparing(
                SpendingDocument::getSpentAt, Comparator.nullsLast(Comparator.naturalOrder())));
        incomes.sort(Comparator.comparing(
                IncomeDocument::getReceivedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        return DailyDetailDto.builder()
                .date(date.toString())
                .totalIncome(totalIncome)
                .totalSpending(totalSpending)
                .netAmount(totalIncome.subtract(totalSpending))
                .spendingByCategory(spendingByCategory)
                .incomeByCategory(incomeByCategory)
                .spendings(spendings)
                .incomes(incomes)
                .build();
    }

    /* ---------- helpers ---------- */

    private List<SpendingDocument> findByUserAndMonth(String userId, YearMonth ym) {
        ZonedDateTime start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime end = start.plusMonths(1);
        Instant startInstant = start.toInstant();
        Instant endInstant = end.toInstant();
        return spendingMongoRepository.findByUserIdAndSpentAtBetween(userId, startInstant, endInstant);
    }

    private BigDecimal sumAmount(List<SpendingDocument> list) {
        return list.stream()
                .map(SpendingDocument::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> groupByCategory(List<SpendingDocument> list) {
        return list.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategory() != null && !s.getCategory().isBlank() ? s.getCategory() : "기타",
                        Collectors.reducing(BigDecimal.ZERO, SpendingDocument::getAmount, BigDecimal::add)
                ));
    }

    private BigDecimal sumIncomeAmount(List<IncomeDocument> list) {
        return list.stream()
                .map(IncomeDocument::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> groupIncomeByCategory(List<IncomeDocument> list) {
        return list.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getCategory() != null && !i.getCategory().isBlank() ? i.getCategory() : "기타",
                        Collectors.reducing(BigDecimal.ZERO,
                                i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));
    }

    private List<CategoryAmountDto> toCategoryAmountSorted(Map<String, BigDecimal> byCategory) {
        return byCategory.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> CategoryAmountDto.builder()
                        .category(e.getKey())
                        .amount(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
