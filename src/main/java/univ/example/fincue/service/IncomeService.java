package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import univ.example.fincue.document.IncomeDocument;
import univ.example.fincue.repository.mongo.IncomeMongoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeMongoRepository incomeMongoRepository;

    /** 수입 내역 추가 */
    public IncomeDocument addIncome(String userId, String receivedDate, String category, BigDecimal amount) {
        // 날짜만 의미하므로 UTC 미드나잇으로 저장 -> DB에 입력 날짜 그대로 보이도록
        Instant receivedAt = LocalDate.parse(receivedDate)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        IncomeDocument doc = IncomeDocument.builder()
                .userId(userId)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .category(category != null && !category.isBlank() ? category : "기타")
                .receivedAt(receivedAt)
                .build();

        return incomeMongoRepository.save(doc);
    }
}
