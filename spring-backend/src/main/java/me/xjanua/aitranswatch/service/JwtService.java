package me.xjanua.aitranswatch.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import me.xjanua.aitranswatch.model.User;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long accessTokenValidityInSeconds;

    public JwtService(@Value("${jwt.base64-secret}") String base64Secret,
            @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds) {
        byte[] secret = Base64.getDecoder().decode(base64Secret);
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT_BASE64_SECRET phải giải mã thành ít nhất 32 bytes cho HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(role -> role.getCode()).sorted().toList();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId().toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenValidityInSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("uid", String.class));
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInSeconds;
    }
}
