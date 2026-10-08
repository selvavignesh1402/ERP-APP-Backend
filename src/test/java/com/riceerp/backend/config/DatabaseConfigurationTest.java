package com.riceerp.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.flywaydb.core.Flyway;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigurationTest {
    @Test void legacyCustomerVersionsAreRepairedWithoutChangingBalances() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:customer_version_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table customers (id bigint primary key, credit_balance double, version bigint)");
        jdbc.update("insert into customers values (1, 24000, null), (2, 12504, 7)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__initialize_customer_versions.sql")).execute(ds);
        assertEquals(0L, jdbc.queryForObject("select version from customers where id=1", Long.class));
        assertEquals(7L, jdbc.queryForObject("select version from customers where id=2", Long.class));
        assertEquals(24000.0, jdbc.queryForObject("select credit_balance from customers where id=1", Double.class));
        jdbc.update("insert into customers (id, credit_balance) values (3, 0)");
        assertEquals(0L, jdbc.queryForObject("select version from customers where id=3", Long.class));
        assertThrows(Exception.class, () -> jdbc.update("update customers set version=null where id=1"));
        assertEquals(1, jdbc.update("update customers set credit_balance=25000, version=version+1 where id=1 and version=0"));
        assertEquals(0, jdbc.update("update customers set credit_balance=26000, version=version+1 where id=1 and version=0"));
    }
    MockEnvironment environment(String... profiles) {
        var env = new MockEnvironment(); env.setActiveProfiles(profiles);
        env.setProperty("DB_HOST", "db.example.test"); env.setProperty("DB_NAME", "erp");
        env.setProperty("DB_USERNAME", "erp_app"); env.setProperty("DB_PASSWORD", "test-only");
        env.setProperty("CORS_ALLOWED_ORIGINS", "https://erp.example.test");
        ConfigDataEnvironmentPostProcessor.applyTo(env); return env;
    }
    void validate(MockEnvironment env) { new DatabaseProfileGuard().postProcessEnvironment(env, null); }
    @ParameterizedTest @ValueSource(strings = {"", "*", "https://*.example.test", "http://localhost:8081",
            "exp://app", "https://erp.example.test/path", "https://user@erp.example.test", "https://erp.example.test,",
            "https://erp.example.test?x=1", "https://erp.example.test#x"})
    void productionRejectsUnsafeCorsOrigins(String origins) {
        var env = environment("production"); env.setProperty("app.cors.allowed-origins", origins);
        assertThrows(IllegalStateException.class, () -> validate(env));
    }
    @Test void productionAcceptsMultipleExactHttpsOrigins() {
        var env = environment("production");
        env.setProperty("app.cors.allowed-origins", "https://erp.example.test, https://admin.example.test:8443");
        validate(env);
    }
    @Test void developmentKeepsLocalhostButDoesNotSeed() {
        var env = environment("dev"); validate(env);
        assertTrue(env.getProperty("spring.datasource.url").startsWith("jdbc:mysql://localhost:3306/erp_db_v1"));
        assertEquals("update", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("never", env.getProperty("spring.sql.init.mode"));
    }
    @Test void demoRequiresExplicitDevelopmentOptIn() {
        var env = environment("dev", "demo"); validate(env);
        assertEquals("always", env.getProperty("spring.sql.init.mode"));
        assertEquals("classpath:seed/demo.sql", env.getProperty("spring.sql.init.data-locations"));
        assertThrows(IllegalStateException.class, () -> validate(environment("demo")));
        assertThrows(IllegalStateException.class, () -> validate(environment()));
        assertFalse(new ClassPathResource("data.sql").exists());
    }
    @Test void testProfileUsesIsolatedH2() {
        var env = environment("test"); validate(env);
        assertTrue(env.getProperty("spring.datasource.url").startsWith("jdbc:h2:mem:"));
        assertEquals("never", env.getProperty("spring.sql.init.mode"));
    }
    @Test void productionUsesVerifiedTlsAndVersionedMigrations() {
        var env = environment("production"); validate(env);
        assertEquals("jdbc:mysql://db.example.test:3306/erp?sslMode=VERIFY_IDENTITY&serverTimezone=UTC", env.getProperty("spring.datasource.url"));
        assertEquals("erp_app", env.getProperty("spring.datasource.username"));
    }
    @ParameterizedTest @ValueSource(strings = {"dev", "demo", "test"})
    void productionRejectsDevelopmentProfiles(String profile) {
        assertThrows(IllegalStateException.class, () -> validate(environment("production", profile)));
    }
    @ParameterizedTest @ValueSource(strings = {"spring.jpa.hibernate.ddl-auto=update", "spring.sql.init.mode=always",
            "spring.jpa.show-sql=true", "spring.flyway.enabled=false", "spring.flyway.clean-disabled=false",
            "spring.flyway.baseline-on-migrate=true", "spring.flyway.validate-on-migrate=false",
            "spring.datasource.url=jdbc:mysql://db/erp?sslMode=DISABLED", "spring.datasource.password="})
    void productionRejectsUnsafeOverrides(String override) {
        var env = environment("production"); var parts = override.split("=", 2); env.setProperty(parts[0], parts[1]);
        assertThrows(IllegalStateException.class, () -> validate(env));
    }
    @Test void baselineMigratesOnceAndReferenceSeedRerunsPreserveSavedNamesAndStock() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:migration_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var flyway = Flyway.configure().dataSource(ds).locations("classpath:db/migration").cleanDisabled(true).load();
        // V1 baseline, V2 invoice identity snapshots, V3 customer versions, V4 exact decimals.
        assertEquals(5, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        // Exercise the actual reference-data statements. Later demo transactions use MySQL DATE_SUB,
        // which H2 does not implement; the full demo script still needs a MySQL rehearsal.
        String script = new ClassPathResource("seed/demo.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String referenceData = script.substring(0, script.indexOf("INSERT INTO purchase "));
        var seed = new ResourceDatabasePopulator(new org.springframework.core.io.ByteArrayResource(
                referenceData.getBytes(java.nio.charset.StandardCharsets.UTF_8))); seed.execute(ds);
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("update organizations set name='Client renamed shop' where id=1");
        jdbc.update("update products set stock=7 where id=1");
        seed.execute(ds);
        assertEquals("Client renamed shop", jdbc.queryForObject("select name from organizations where id=1", String.class));
        assertEquals(7.0, jdbc.queryForObject("select stock from products where id=1", Double.class));
        assertThrows(Exception.class, flyway::clean);
    }
    @Test void existingUnversionedDatabaseIsNotSilentlyBaselined() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:legacy_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new JdbcTemplate(ds).execute("create table existing_business_data (id bigint primary key)");
        var flyway = Flyway.configure().dataSource(ds).locations("classpath:db/migration").baselineOnMigrate(false).load();
        assertThrows(Exception.class, flyway::migrate);
    }
}
