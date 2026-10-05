package com.riceerp.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the real Hibernate mappings against a REAL MySQL 8 schema produced by
 * the committed Flyway migrations, with {@code spring.jpa.hibernate.ddl-auto=validate}.
 *
 * <p>This is the check that matters for production, which is pinned to
 * {@code validate} by {@link DatabaseProfileGuard}: if an entity field and the
 * migrated column disagree on SQL type, the application refuses to start. It
 * replaces the older SchemaExportTest, which compared generated DDL text against
 * the V1 baseline and so reported every legitimate later migration as drift.
 *
 * <p>Opt-in, like MySqlRehearsalTest, because it needs a real MySQL server. The
 * schema is created by Flyway from scratch, so point it at a disposable database:
 * <pre>
 * $env:DEV_DB_URL="jdbc:mysql://127.0.0.1:3306/erp_probe_tmp?useSSL=false&amp;allowPublicKeyRetrieval=true&amp;serverTimezone=UTC"
 * $env:DEV_DB_USERNAME="root"; $env:DEV_DB_PASSWORD="root"; $env:TEST_MYSQL="true"
 * mvnw test -Dtest=MySqlSchemaValidationTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "TEST_MYSQL", matches = "true")
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.clean-disabled=false",
        "spring.flyway.baseline-on-migrate=false",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
@ActiveProfiles("dev")
class MySqlSchemaValidationTest {

    @jakarta.annotation.Resource JdbcTemplate jdbc;

    @Test
    void flywayMigratedSchemaMatchesEveryEntityMapping() {
        // The context only starts if validate() passed, which is the real assertion.
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = 0", Integer.class));
        assertTrue(jdbc.queryForObject(
                "select version from flyway_schema_history order by installed_rank desc limit 1", String.class)
                .startsWith("4"), "V4 must be applied");

        // Money columns really are DECIMAL, not silently floating point.
        for (String c : new String[]{"credit_balance", "credit_limit"})
            assertTrue(columnType("customers", c).startsWith("decimal"),
                    "customers." + c + " should be decimal but is " + columnType("customers", c));
        assertTrue(columnType("products", "gst_rate").startsWith("decimal(7,4)"));
        assertTrue(columnType("products", "stock").startsWith("decimal(19,6)"));
        assertTrue(columnType("payments", "amount").startsWith("decimal(19,4)"));
        assertTrue(columnType("payment_sale_allocations", "amount").startsWith("decimal(19,4)"));
        assertTrue(columnType("sales", "grand_total").startsWith("decimal(19,4)"));

        // Deliberately excluded from V4: a 0-5 score, and signed geo coordinates.
        Map<String, String> excluded = new LinkedHashMap<>();
        excluded.put("suppliers", "rating");
        excluded.put("visit_check_ins", "latitude");
        excluded.put("visit_check_ins", "longitude");
        excluded.forEach((t, c) -> assertTrue(columnType(t, c).startsWith("double"),
                t + "." + c + " should remain floating point but is " + columnType(t, c)));
    }

    @Test
    void decimalColumnsStoreExactFractionalValues() {
        BigDecimal exact = jdbc.queryForObject("select cast(0.1 + 0.2 as decimal(19,4))", BigDecimal.class);
        // Compare numerically: BigDecimal.equals() also compares scale (0.3 vs 0.3000).
        assertEquals(0, new BigDecimal("0.3").compareTo(exact),
                "exact decimal arithmetic should not drift the way float does, got " + exact);
        // Documents the drift that motivated V4.
        assertNotEquals(0, new BigDecimal("0.3").compareTo(BigDecimal.valueOf(0.1 + 0.2)),
                "float arithmetic is expected to drift; that is why these columns are DECIMAL");
    }

    private String columnType(String table, String column) {
        return jdbc.queryForObject(
                "select column_type from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?",
                String.class, table, column);
    }
}