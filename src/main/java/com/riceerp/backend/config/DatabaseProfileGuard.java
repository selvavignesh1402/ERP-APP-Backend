package com.riceerp.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/** Fail before datasource creation when deployment profiles could enable development writes. */
public class DatabaseProfileGuard implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 11; }
    @Override public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        Set<String> profiles = new HashSet<>(Arrays.asList(env.getActiveProfiles()));
        if (profiles.stream().noneMatch(Set.of("dev", "test", "production")::contains))
            throw new IllegalStateException("Select a database profile: dev for localhost development, test for tests, or production for deployment.");
        if (profiles.contains("demo") && !profiles.contains("dev"))
            throw new IllegalStateException("Demo data requires the dev profile.");
        if (!profiles.contains("production")) return;
        if (profiles.contains("dev") || profiles.contains("test") || profiles.contains("demo"))
            throw new IllegalStateException("Production cannot be combined with dev, test or demo profiles.");
        validateProductionOrigins(env.getProperty("app.cors.allowed-origins"));
        require(env, "spring.jpa.hibernate.ddl-auto", "validate");
        require(env, "spring.sql.init.mode", "never");
        require(env, "spring.jpa.show-sql", "false");
        require(env, "spring.jpa.defer-datasource-initialization", "false");
        require(env, "spring.flyway.enabled", "true");
        require(env, "spring.flyway.clean-disabled", "true");
        require(env, "spring.flyway.baseline-on-migrate", "false");
        require(env, "spring.flyway.validate-on-migrate", "true");
        require(env, "spring.flyway.locations", "classpath:db/migration");
        require(env, "logging.level.org.hibernate.SQL", "WARN");
        require(env, "logging.level.org.hibernate.orm.jdbc.bind", "WARN");
        for (String key : new String[]{"spring.datasource.username", "spring.datasource.password"}) {
            String value = env.getProperty(key);
            if (value == null || value.isBlank()) throw new IllegalStateException("Production requires " + key);
        }
        String url = env.getRequiredProperty("spring.datasource.url");
        // Permit exactly one transport setting, rejecting duplicate/conflicting TLS switches.
        String[] pieces = url.split("\\?", -1);
        if (!url.startsWith("jdbc:mysql://") || pieces.length != 2)
            throw new IllegalStateException("Production requires a MySQL URL with verified TLS.");
        boolean verified = false;
        for (String option : pieces[1].split("&")) {
            if (option.equals("sslMode=VERIFY_IDENTITY") && !verified) { verified = true; continue; }
            if (!option.equals("serverTimezone=UTC"))
                throw new IllegalStateException("Production database URL allows only sslMode=VERIFY_IDENTITY and serverTimezone=UTC; configure trust through the JVM trust store.");
        }
        if (!verified) throw new IllegalStateException("Production requires sslMode=VERIFY_IDENTITY.");
    }
    private static void require(ConfigurableEnvironment env, String key, String expected) {
        if (!expected.equalsIgnoreCase(env.getProperty(key, "")))
            throw new IllegalStateException("Production requires " + key + "=" + expected);
    }

    static void validateProductionOrigins(String origins) {
        if (origins == null || origins.isBlank())
            throw new IllegalStateException("Production requires explicit CORS_ALLOWED_ORIGINS.");
        for (String origin : origins.split(",", -1)) {
            try {
                var uri = java.net.URI.create(origin.trim());
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null ||
                        origin.contains("*") || uri.getUserInfo() != null || uri.getQuery() != null ||
                        uri.getFragment() != null || !uri.getPath().isEmpty() ||
                        uri.getPort() == 0 || uri.getPort() > 65535)
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Production CORS origins must be exact HTTPS origins without wildcards, credentials, paths, queries or fragments.");
            }
        }
    }
}
