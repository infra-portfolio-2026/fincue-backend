package univ.example.fincue.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.UserChallengeDocument;

import java.util.List;
import java.util.Optional;

public interface UserChallengeMongoRepository extends MongoRepository<UserChallengeDocument, String> {

    List<UserChallengeDocument> findByUserId(String userId);

    Optional<UserChallengeDocument> findByUserIdAndChallengeId(String userId, String challengeId);

    List<UserChallengeDocument> findByUserIdAndIsCompleted(String userId, Boolean isCompleted);

    void deleteByChallengeId(String challengeId);
}
