package com.riceerp.backend.security;

import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.enums.PlatformRole;
import io.jsonwebtoken.Claims;
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

    public static String generateToken(Long userId, String phoneNumber, PlatformRole platformRole, Long organizationId, OrgRole orgRole) {
        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("phoneNumber", phoneNumber)
                .claim("platformRole", platformRole != null ? platformRole.name() : "USER")
                .claim("organizationId", organizationId)
                .claim("orgRole", orgRole != null ? orgRole.name() : null)
                // Legacy claim for backward compatibility
                .claim("role", orgRole != null ? orgRole.name() : (platformRole != null ? platformRole.name() : "USER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(KEY, SignatureAlgorithm.HS256);

        return builder.compact();
    }

    // Backward-compatible overload
    public static String generateToken(Long userId, String phoneNumber, String role, Long organizationId) {
        OrgRole orgRole = null;
        PlatformRole platformRole = PlatformRole.USER;

        if (role != null) {
            try {
                orgRole = OrgRole.valueOf(role);
            } catch (IllegalArgumentException ignored) {
                try {
                    platformRole = PlatformRole.valueOf(role);
                } catch (IllegalArgumentException ignored2) {}
            }
        }

        return generateToken(userId, phoneNumber, platformRole, organizationId, orgRole);
    }

    public static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static Long extractUserId(String token) {
        return Long.valueOf(extractAllClaims(token).getSubject());
    }

    public static String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public static String extractPlatformRole(String token) {
        String pRole = extractAllClaims(token).get("platformRole", String.class);
        if (pRole != null) return pRole;
        // Fallback for legacy tokens
        String role = extractRole(token);
        if ("MASTER_ADMIN".equals(role)) return "MASTER_ADMIN";
        return "USER";
    }

    public static String extractOrgRole(String token) {
        String oRole = extractAllClaims(token).get("orgRole", String.class);
        if (oRole != null) return oRole;
        // Fallback for legacy tokens
        String role = extractRole(token);
        if (role != null && !"MASTER_ADMIN".equals(role) && !"USER".equals(role)) {
            return role;
        }
        return null;
    }

    public static Long extractOrganizationId(String token) {
        Number orgId = extractAllClaims(token).get("organizationId", Number.class);
        return orgId != null ? orgId.longValue() : null;
    }
}
