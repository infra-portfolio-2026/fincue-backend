package univ.example.fincue.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.CardDocument;

import java.util.List;
import java.util.Optional;

public interface CardMongoRepository extends MongoRepository<CardDocument, String> {

    Optional<CardDocument> findByExternalId(String externalId);

    List<CardDocument> findByCardCompany(String cardCompany);

    List<CardDocument> findAllByOrderByCardName(Pageable pageable);

    void deleteAllByExternalIdStartingWith(String prefix);
}
