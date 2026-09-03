package com.riceerp.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_STRING = "riceerpsystemsupersecretkeymustbeverylongatleast256bitsformetricsandsafety";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24; // 24h

    public static String generateToken(Long userId, String phoneNumber, String role, Long organizationId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("phoneNumber", phoneNumber)
                .claim("role", role != null ? role : "SALES") // fallback default
                .claim("organizationId", organizationId) // can be null if not selected yet
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    public static Long extractUserId(String token) {
        return Long.valueOf(
                Jwts.parserBuilder()
                        .setSigningKey(KEY)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }

    public static String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public static Long extractOrganizationId(String token) {
        Number orgId = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("organizationId", Number.class);
        return orgId != null ? orgId.longValue() : null;
    }
}
