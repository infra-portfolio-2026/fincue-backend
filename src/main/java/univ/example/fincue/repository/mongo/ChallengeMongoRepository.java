package univ.example.fincue.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.ChallengeDocument;

import java.time.Instant;
import java.util.List;

public interface ChallengeMongoRepository extends MongoRepository<ChallengeDocument, String> {

    List<ChallengeDocument> findByStartDateBeforeAndEndDateAfter(Instant now, Instant now2);

    /** 유저가 생성한 나만의 도전 목록 */
    List<ChallengeDocument> findByCreatedByUserId(String userId);

    /** 시스템 템플릿 (인기 도전) - createdByUserId가 null */
    List<ChallengeDocument> findByCreatedByUserIdIsNull();
}
