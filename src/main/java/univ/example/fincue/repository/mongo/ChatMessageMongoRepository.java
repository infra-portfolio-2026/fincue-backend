package univ.example.fincue.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import univ.example.fincue.document.ChatMessageDocument;

import java.util.List;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

    /** 세션 내 메시지를 오래된 순으로 조회 (전체) */
    List<ChatMessageDocument> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /** 세션 내 최근 메시지 N개 (최신순). LLM 컨텍스트 조립에 사용. */
    List<ChatMessageDocument> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    void deleteBySessionId(String sessionId);
}
