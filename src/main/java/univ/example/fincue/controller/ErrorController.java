package univ.example.fincue.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * /error 핸들러 - 500 등 발생 시 실제 예외 내용 표시 (원인 파악용)
 */
@Hidden
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping("/error")
    public ResponseEntity<String> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object ex = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = status != null ? (Integer) status : 500;
        String message = "";
        if (ex instanceof Throwable t) {
            log.error("에러 발생: {} {}", request.getMethod(), request.getRequestURI(), t);
            message = t.getClass().getName() + ": " + t.getMessage();
            Throwable cause = t.getCause();
            while (cause != null) {
                message += "\n  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage();
                cause = cause.getCause();
            }
        }

        String html = """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><title>오류</title></head>
            <body style="font-family:sans-serif;padding:24px;max-width:800px;">
            <h2>오류가 발생했습니다 (HTTP {{CODE}})</h2>
            <pre style="background:#f5f5f5;padding:12px;overflow:auto;font-size:12px;">{{MSG}}</pre>
            <p>IntelliJ 콘솔에서 전체 스택 트레이스를 확인하세요.</p>
            <p><a href="/">홈으로</a> | <a href="/swagger-ui.html">Swagger UI</a></p>
            </body></html>""".replace("{{CODE}}", String.valueOf(statusCode)).replace("{{MSG}}", escape(message));

        return ResponseEntity.status(statusCode)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private static String escape(String s) {
        if (s == null || s.isEmpty()) return "(예외 정보 없음)";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("%", "%%");
    }
}
