package univ.example.fincue.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * userChallenges 컬렉션 (유저별 챌린지 참여 정보)
 * N:M을 분리한 조인 컬렉션
 * 유니크 인덱스: { userId: 1, challengeId: 1 }
 */
@Schema(description = "유저-챌린지 참여 정보")
@Document(collection = "userChallenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "userId_challengeId_unique", def = "{'userId': 1, 'challengeId': 1}", unique = true)
})
public class UserChallengeDocument {

    @Id
    private String id;

    private String userId;
    private String challengeId;
    private Double progressPercent;   // 0~100 진행률
    private Boolean isCompleted;      // 기간 종료 여부
    private BigDecimal savedAmount;   // 절약 금액 (목표 대비 성공 판정용)
    private Instant completedAt;      // 완료 시각
}
