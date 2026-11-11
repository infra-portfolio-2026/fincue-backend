package univ.example.fincue.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.SpendingDocument;

import java.time.Instant;
import java.util.List;

public interface SpendingMongoRepository extends MongoRepository<SpendingDocument, String> {

    List<SpendingDocument> findByUserIdOrderBySpentAtDesc(String userId, Pageable pageable);

    List<SpendingDocument> findByUserIdAndSpentAtBetween(String userId, Instant start, Instant end);
}
