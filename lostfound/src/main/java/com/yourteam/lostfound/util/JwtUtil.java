package com.yourteam.lostfound.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Configurable 256-bit secret key for HMAC-SHA256 (minimum 32 characters)
    private static final String DEFAULT_SECRET = "campus_lost_and_found_dev_secret_jwt_key_2026_min32chars!";
    private static java.security.Key KEY = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

    @org.springframework.beans.factory.annotation.Value("${jwt.secret:${JWT_SECRET:campus_lost_and_found_dev_secret_jwt_key_2026_min32chars!}}")
    public void setSecret(String secret) {
        if (secret != null && !secret.isBlank()) {
            KEY = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public static String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public static String extractRole(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    public static boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private static Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}