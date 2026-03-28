package knu.dykf.landom.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    // Access Token: 30분
    private final long ACCESS_TOKEN_TIME = 30 * 60 * 1000L;
    // Refresh Token: 7일
    private final long REFRESH_TOKEN_TIME = 7 * 24 * 60 * 60 * 1000L;

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
        return createToken(username, ACCESS_TOKEN_TIME);
    }

    // Refresh Token 생성
    public String createRefreshToken(String username) {
        return createToken(username, REFRESH_TOKEN_TIME);
    }

    private String createToken(String username, long expirationTime) {
        Date date = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(date.getTime() + expirationTime))
                .setIssuedAt(date)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}