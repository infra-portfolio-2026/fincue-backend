package univ.example.fincue.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.CommentDocument;

import java.util.List;

public interface CommentMongoRepository extends MongoRepository<CommentDocument, String> {

    List<CommentDocument> findByPostIdOrderByCreatedAtAsc(String postId, Pageable pageable);

    long countByPostId(String postId);
}
