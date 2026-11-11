package univ.example.fincue.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * spendings 컬렉션
 * 관계: N:1 → users
 * 인덱스: userId, spentAt
 */
@Document(collection = "spendings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "소비 내역 (추가 시 반환)")
public class SpendingDocument {

    @Id
    @Schema(description = "소비 내역 ID")
    private String id;

    @Indexed
    @Schema(description = "유저 ID")
    private String userId;

    @Schema(description = "금액")
    private BigDecimal amount;

    @Schema(description = "카테고리")
    private String category;

    @Indexed
    @Schema(description = "소비 시각")
    private Instant spentAt;

    @Schema(description = "메모")
    private String memo;
}
