package backend.service;

import backend.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private static final String SECRET_KEY = "prep_secret_key_2026_super_secure_key";

    public SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        System.out.println("EMAIL = " + user.getEmail());
        System.out.println("ROLE = " + user.getRoleName());
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", "ROLE_" + user.getRoleName())
                .claim("fullName", user.getFullName())
                .claim("tokenVersion", user.getTokenVersion())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 phút
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user, boolean rememberMe) {
        long duration = rememberMe ? 30L * 24 * 60 * 60 * 1000 : 7L * 24 * 60 * 60 * 1000;
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("tokenVersion", user.getTokenVersion())
                .claim("type", "REFRESH_TOKEN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + duration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /*Reset Token --> Cap token tam thoi de nguoi dung su dung DUNG gmail*/
    public String generateResetToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "RESET_PASSWORD") /* 👈 QUAN TRỌNG */
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public Integer extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Integer.class);
    }

    public Integer extractTokenVersion(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("tokenVersion", Integer.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }
}
