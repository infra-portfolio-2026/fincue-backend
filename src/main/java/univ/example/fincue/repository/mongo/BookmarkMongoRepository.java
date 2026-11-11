package univ.example.fincue.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.BookmarkDocument;

import java.util.List;
import java.util.Optional;

public interface BookmarkMongoRepository extends MongoRepository<BookmarkDocument, String> {

    Optional<BookmarkDocument> findByPostIdAndUserId(String postId, String userId);

    boolean existsByPostIdAndUserId(String postId, String userId);

    long countByPostId(String postId);

    List<BookmarkDocument> findByUserId(String userId);
}
