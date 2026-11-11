package univ.example.fincue.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 전역 예외 처리 - 500 에러 원인 로깅.
 * HandlerExceptionResolver 사용 (RestControllerAdvice 대신) - springdoc ControllerAdviceBean 호환 이슈 회피
 */
@Component
public class GlobalExceptionHandler implements HandlerExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception ex) {
        log.error("요청 실패: {} {}", request.getMethod(), request.getRequestURI(), ex);

        String msg = escape(ex.getClass().getName() + ": " + (ex.getMessage() != null ? ex.getMessage() : ""));
        String html = """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><title>오류</title></head>
            <body style="font-family:sans-serif;padding:24px;">
            <h2>오류가 발생했습니다</h2>
            <pre style="background:#f5f5f5;padding:12px;overflow:auto;">{{MSG}}</pre>
            <p><a href="/">홈으로</a></p>
            </body></html>""".replace("{{MSG}}", msg);

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write(html);
        } catch (IOException e) {
            log.warn("예외 응답 출력 실패", e);
        }
        return new ModelAndView();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("%", "%%");
    }
}
