package com.riceerp.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in: only use against the disposable rehearsal databases described in DATABASE_SETUP.md. */
@EnabledIfSystemProperty(named = "test.mysql", matches = "true")
@SpringBootTest(properties = {"DB_HOST=localhost", "DB_PORT=3317", "DB_NAME=${test.mysql.database:erp_rehearsal}",
        "DB_USERNAME=erp_rehearsal", "DB_PASSWORD=rehearsal-only", "CORS_ALLOWED_ORIGINS=https://rehearsal.example.test"})
@ActiveProfiles("production")
class MySqlRehearsalTest {
    @Autowired DataSource datasource;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Test void productionContextMigratesValidatesAndPreservesRealMySqlSeedData() {
        assertEquals(3317, jdbc.queryForObject("select @@port", Integer.class));
        assertTrue(jdbc.queryForObject("select @@datadir", String.class).contains("mysql-rehearsal-20260928"));
        assertFalse(jdbc.queryForList("SHOW SESSION STATUS LIKE 'Ssl_cipher'").get(0).get("Value").toString().isBlank());
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        if (Boolean.getBoolean("test.mysql.restored")) {
            assertEquals("Renamed rehearsal shop", jdbc.queryForObject("select name from organizations where id=1", String.class));
            assertEquals(7.0, jdbc.queryForObject("select stock from products where id=1", Double.class));
            return;
        }
        var seed = new ResourceDatabasePopulator(new ClassPathResource("seed/demo.sql")); seed.execute(datasource);
        jdbc.update("update organizations set name='Renamed rehearsal shop' where id=1");
        jdbc.update("update products set stock=7 where id=1");
        seed.execute(datasource);
        assertEquals("Renamed rehearsal shop", jdbc.queryForObject("select name from organizations where id=1", String.class));
        assertEquals(7.0, jdbc.queryForObject("select stock from products where id=1", Double.class));
    }
}
