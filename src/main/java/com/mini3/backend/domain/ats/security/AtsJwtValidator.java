package com.mini3.backend.domain.ats.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * management-back 의 {@code JwtProvider} 와 동일한 HS256·기본 시크릿으로 토큰만 검증한다.
 * management-back 소스는 수정하지 않고, 여기서만 동작을 맞춘다.
 */
@Component
public class AtsJwtValidator {

    private final Key key;

    public AtsJwtValidator(
            @Value("${jwt.secret:mysecretkeymysecretkeymysecretkey123456}") String secret
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
