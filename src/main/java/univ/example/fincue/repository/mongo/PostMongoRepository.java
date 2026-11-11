package univ.example.fincue.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.PostDocument;

import java.util.List;

public interface PostMongoRepository extends MongoRepository<PostDocument, String> {

    List<PostDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<PostDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 대표 TIP 목록 (첫화면 왼쪽) */
    List<PostDocument> findByIsTipTrueOrderByCreatedAtDesc(Pageable pageable);

    /** 커뮤니티: 카테고리별 목록 (절약팁, 질문, 자유) */
    List<PostDocument> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    long countByCategory(String category);

    long countAllBy();
}
