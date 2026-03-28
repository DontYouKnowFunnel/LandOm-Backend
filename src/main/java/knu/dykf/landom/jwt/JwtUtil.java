package knu.dykf.landom.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret.key}")
    private String secretKey;
    private Key key;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // Access Token 생성
    public String createAccessToken(String username) {
        // Access Token: 30분
        long ACCESS_TOKEN_TIME = 30 * 60 * 1000L;
        return createToken(username, ACCESS_TOKEN_TIME);
    }

    // Refresh Token 생성
    public String createRefreshToken(String username) {
        // Refresh Token: 7일
        long REFRESH_TOKEN_TIME = 7 * 24 * 60 * 60 * 1000L;
        return createToken(username, REFRESH_TOKEN_TIME);
    }

    private String createToken(String username, long expirationTime) {
        Date date = new Date();

        return Jwts.builder()
                .subject(username)
                .expiration(new Date(date.getTime() + expirationTime))
                .issuedAt(date)
                .signWith(key)
                .compact();
    }

    // 토큰에서 username 추출
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}