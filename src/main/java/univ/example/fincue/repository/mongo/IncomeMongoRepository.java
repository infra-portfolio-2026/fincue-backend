package univ.example.fincue.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.IncomeDocument;

import java.time.Instant;
import java.util.List;

public interface IncomeMongoRepository extends MongoRepository<IncomeDocument, String> {

    List<IncomeDocument> findByUserIdOrderByReceivedAtDesc(String userId, Pageable pageable);

    List<IncomeDocument> findByUserIdAndReceivedAtBetween(String userId, Instant start, Instant end);
}
