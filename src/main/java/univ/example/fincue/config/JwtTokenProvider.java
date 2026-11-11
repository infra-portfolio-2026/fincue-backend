package univ.example.fincue.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long validityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:default-secret-key-for-development-only-change-in-production}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long validityMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMs = validityMs;
    }

    /** JWT 생성 (mongoUserId, name, provider 포함) */
    public String createToken(String mongoUserId, String name, String provider) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .subject(mongoUserId)
                .claim("name", name)
                .claim("provider", provider)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /** 토큰 검증 후 Claims 반환 (실패 시 null) */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException | io.jsonwebtoken.security.SignatureException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getValidityMs() {
        return validityMs;
    }
}
