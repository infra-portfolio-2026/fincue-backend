package univ.example.fincue.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.ProductDocument;

import java.util.List;

public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {

    List<ProductDocument> findByBankName(String bankName);

    List<ProductDocument> findAllByOrderByInterestRateDesc(Pageable pageable);

    List<ProductDocument> findByPeriodBetween(int minPeriod, int maxPeriod, Pageable pageable);

    List<ProductDocument> findByProductType(String productType);

    void deleteByProductType(String productType);
}
