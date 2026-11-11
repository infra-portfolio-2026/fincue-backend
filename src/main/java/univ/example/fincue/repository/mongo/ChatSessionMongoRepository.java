package univ.example.fincue.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.ChatSessionDocument;

import java.util.List;

public interface ChatSessionMongoRepository extends MongoRepository<ChatSessionDocument, String> {

    List<ChatSessionDocument> findByUserIdOrderByLastMessageAtDesc(String userId);
}
