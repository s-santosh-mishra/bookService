package com.servicehub.serv.security;

import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMillis;

    private static final long USER_WORKER_EXPIRATION =
            7L * 24 * 60 * 60 * 1000;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMillis
    ) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirationMillis = expirationMillis;
    }


    // Generate JWT

    public String generateToken(Credentials credentials) {

        Date issuedAt = new Date();

        long tokenExpiration = expirationMillis;

        if (credentials.getRole() == UserRole.USER ||
                credentials.getRole() == UserRole.WORKER) {

            tokenExpiration = USER_WORKER_EXPIRATION;
        }

        Date expiration =
                new Date(
                        issuedAt.getTime() + tokenExpiration
                );

        return Jwts.builder()
                .subject(credentials.getUserId().toString())
                .claim("email", credentials.getEmail())
                .claim("role", credentials.getRole().name())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }


    // Parse and Validate JWT

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    // Extract User ID

    public String extractUserId(String token) {

        return extractAllClaims(token)
                .getSubject();
    }


    // Extract Role

    public String extractRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }
}