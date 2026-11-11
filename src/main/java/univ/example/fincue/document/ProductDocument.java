package univ.example.fincue.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * products 컬렉션 (정기예금/적금/연금저축 – FSS API 동기화 데이터)
 * productType: DEPOSIT(정기예금), SAVING(적금), PENSION(연금저축)
 */
@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    /** DEPOSIT, SAVING, PENSION */
    private String productType;
    /** 금융회사 코드 (FSS) */
    private String finCoNo;
    /** 금융상품 코드 (FSS) - 옵션별로 동일 상품이면 동일 코드 */
    private String finPrdtCd;

    private String bankName;
    private String productName;
    private BigDecimal interestRate;   // 기본 금리 (연금은 avg_prft_rate 등)
    private BigDecimal intrRate2;      // 최고 우대금리 (정기예금/적금)
    private Integer period;            // 기간 (월), 연금은 납입기간(년) 등
    private BigDecimal maxAmount;
    private String url;
    private Instant crawledAt;
}
