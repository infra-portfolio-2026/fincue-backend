package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * cards 컬렉션 (카드고릴라 API 동기화 데이터)
 * 소비 분석 기반 카드 추천에 사용
 */
@Document(collection = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDocument {

    @Id
    private String id;

    /** 카드고릴라 등 외부 식별자 (중복 저장 방지) */
    @Indexed(unique = true)
    private String externalId;

    private String cardName;
    private String cardCompany;

    /** 혜택 요약 (예: 최대 75만원 혜택) */
    private String benefitSummary;

    /** 혜택 목록 (예: 1,000원당 1마일 기본적립, 인천공항 라운지 무료이용) */
    private List<String> benefits;

    /** 혜택 분야 (카드고릴라 분야와 매칭: 주유, 교통, 푸드, 쇼핑, 여행/숙박 등) */
    private List<String> benefitCategories;

    /** 국내전용 연회비 (원) */
    private Integer annualFeeDomestic;

    /** 해외겸용 연회비 (원) */
    private Integer annualFeeOverseas;

    /** 연회비 텍스트 (원본 그대로 저장용) */
    private String annualFeeText;

    /** 전월실적 조건 (예: 전월실적 없음) */
    private String previousMonthCondition;

    /** 상세 페이지 URL */
    private String detailUrl;

    private Instant crawledAt;
}
