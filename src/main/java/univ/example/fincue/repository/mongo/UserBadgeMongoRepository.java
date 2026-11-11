package univ.example.fincue.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.UserBadgeDocument;

import java.util.List;

public interface UserBadgeMongoRepository extends MongoRepository<UserBadgeDocument, String> {

    List<UserBadgeDocument> findByUserIdOrderByEarnedAtDesc(String userId);
}
