package com.example.socialnetwork.security;

import com.example.socialnetwork.config.JwtProperties;
import com.example.socialnetwork.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTtlMinutes() * 60L);
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim(TOKEN_TYPE_CLAIM, "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(accessKey())
                .compact();
    }

    public RefreshTokenPayload generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTtlDays() * 24L * 60L * 60L);
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(TOKEN_TYPE_CLAIM, "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(refreshKey())
                .compact();
        return new RefreshTokenPayload(token, tokenId, toLocalDateTime(expiresAt));
    }

    public JwtClaims parseAccessToken(String token) {
        Jws<Claims> claims = Jwts.parser()
                .verifyWith(accessKey())
                .build()
                .parseSignedClaims(token);
        validateType(claims.getPayload(), "access");
        return new JwtClaims(
                Long.parseLong(claims.getPayload().getSubject()),
                claims.getPayload().get("email", String.class),
                claims.getPayload().getId(),
                toLocalDateTime(claims.getPayload().getExpiration().toInstant())
        );
    }

    public JwtClaims parseRefreshToken(String token) {
        Jws<Claims> claims = Jwts.parser()
                .verifyWith(refreshKey())
                .build()
                .parseSignedClaims(token);
        validateType(claims.getPayload(), "refresh");
        return new JwtClaims(
                Long.parseLong(claims.getPayload().getSubject()),
                claims.getPayload().get("email", String.class),
                claims.getPayload().getId(),
                toLocalDateTime(claims.getPayload().getExpiration().toInstant())
        );
    }

    private void validateType(Claims claims, String expectedType) {
        String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.equals(actualType)) {
            throw new IllegalArgumentException("Invalid token type");
        }
    }

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public record JwtClaims(Long userId, String email, String tokenId, LocalDateTime expiresAt) {
    }

    public record RefreshTokenPayload(String token, String tokenId, LocalDateTime expiresAt) {
    }
}
