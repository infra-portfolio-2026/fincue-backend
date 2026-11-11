package univ.example.fincue.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * incomes 컬렉션
 * 관계: N:1 -> users
 * 인덱스: userId, receivedAt
 */
@Document(collection = "incomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "수입 내역 (추가 시 반환)")
public class IncomeDocument {

    @Id
    @Schema(description = "수입 내역 ID")
    private String id;

    @Indexed
    @Schema(description = "유저 ID")
    private String userId;

    @Schema(description = "금액")
    private BigDecimal amount;

    @Schema(description = "카테고리")
    private String category;

    @Indexed
    @Schema(description = "수입 발생 시각")
    private Instant receivedAt;

    @Schema(description = "메모")
    private String memo;
}
