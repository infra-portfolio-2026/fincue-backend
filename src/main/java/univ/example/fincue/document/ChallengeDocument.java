package univ.example.fincue.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "챌린지 (도전)")
@Document(collection = "challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeDocument {

    @Id
    private String id;

    private String title;
    private String category;     // 식비, 교통, 쇼핑 등
    private BigDecimal goalAmount;
    private Instant startDate;
    private Instant endDate;
    private String description;
    private String badge;
    /** 유저가 생성한 도전인 경우 작성자 ID, null이면 시스템 템플릿(인기 도전) */
    private String createdByUserId;
}
