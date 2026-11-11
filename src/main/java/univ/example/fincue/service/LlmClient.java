package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI(또는 OpenAI 호환) Chat Completions API 호출 래퍼.
 * - api.key, base-url, model은 application.properties로 외부화
 * - 키가 비어 있으면 즉시 안내 메시지를 반환해 개발 환경에서도 동작 (실패 대신 graceful fallback)
 */
@Component
@RequiredArgsConstructor
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final RestTemplate restTemplate;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.api.temperature:0.4}")
    private double temperature;

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    /**
     * 한 번의 채팅 완료 요청.
     *
     * @param messages role/content 쌍 리스트 (system → 과거 user/assistant → 이번 user)
     * @return 어시스턴트 응답 본문
     */
    public String complete(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("openai.api.key 가 설정되지 않아 챗봇 fallback 응답을 반환합니다.");
            return fallbackReply(messages);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/chat/completions", entity, Map.class);
            return extractContent(response);
        } catch (Exception ex) {
            log.error("LLM 호출 실패: {}", ex.getMessage(), ex);
            return "죄송해요, 지금 응답을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
        }
    }

    private String extractContent(Map<String, Object> response) {
        if (response == null) return "";
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return "";
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return "";
        Object messageObj = firstMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) return "";
        Object content = messageMap.get("content");
        return content != null ? content.toString().trim() : "";
    }

    /** API 키가 없을 때 최소 동작을 보장하는 단순 응답. */
    private String fallbackReply(List<Map<String, String>> messages) {
        String lastUser = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, String> m = messages.get(i);
            if (ROLE_USER.equals(m.get("role"))) {
                lastUser = m.getOrDefault("content", "");
                break;
            }
        }
        return "(데모 응답: OpenAI API 키가 설정되지 않았습니다.)\n질문: \"" + lastUser + "\"\n" +
                "application.properties 에 openai.api.key 를 설정하면 실제 LLM 응답을 받을 수 있습니다.";
    }

    /** role/content Map 헬퍼 (서비스에서 쓰기 편하라고). */
    public static Map<String, String> message(String role, String content) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content != null ? content : "");
        return m;
    }

    /** 빈 리스트로 시작해서 system 메시지를 추가하는 헬퍼. */
    public static List<Map<String, String>> newMessages() {
        return new ArrayList<>();
    }
}
