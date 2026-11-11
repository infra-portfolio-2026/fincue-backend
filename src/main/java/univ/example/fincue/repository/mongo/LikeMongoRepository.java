package univ.example.fincue.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.LikeDocument;

import java.util.List;
import java.util.Optional;

public interface LikeMongoRepository extends MongoRepository<LikeDocument, String> {

    Optional<LikeDocument> findByPostIdAndUserId(String postId, String userId);

    boolean existsByPostIdAndUserId(String postId, String userId);

    long countByPostId(String postId);

    List<LikeDocument> findByUserId(String userId);
}
