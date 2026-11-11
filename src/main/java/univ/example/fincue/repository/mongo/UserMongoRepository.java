package univ.example.fincue.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.UserDocument;

import java.util.Optional;

public interface UserMongoRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByAuthProviderAndAuthProviderId(String authProvider, String authProviderId);
}
