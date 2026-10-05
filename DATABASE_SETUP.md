# Database startup and migrations

Use Java 17 or later and the Maven wrapper. No profile is selected automatically: startup refuses to guess which database mode is intended. No live database was changed while implementing this configuration.

## Local development

In PowerShell, start your existing localhost MySQL database, then run from this directory:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
./mvnw.cmd spring-boot:run
```

The dev profile keeps the existing localhost:3306/erp_db_v1 database and local root/root defaults. Override DEV_DB_URL, DEV_DB_USERNAME and DEV_DB_PASSWORD for your local installation. Localhost remains correct for a backend and database running on the same computer. Hibernate schema updates are allowed only for development; this profile must never target production data. SQL output is off by default; DEV_SHOW_SQL=true enables it locally.

Normal dev startup does not insert demo data. For a disposable demo database only, select `dev,demo` instead of `dev` and point DEV_DB_URL at that database. The seed has fixed IDs, so do not apply it to a client database. Repeated demo initialization preserves existing row names, balances and quantities, but can insert missing sample rows. Switch back to dev after loading the demo. Master-admin creation remains the separate opt-in bootstrap documented in the existing project workflow; demo seeding does not supply account passwords.

The test profile lives under src/test/resources and uses an in-memory H2 database. Run `./mvnw.cmd clean test` for automated checks. These tests do not connect to localhost MySQL.

## Production

Select only `production` and supply DB_HOST, DB_PORT (default 3306), DB_NAME, DB_USERNAME and DB_PASSWORD through the deployment secret/configuration system. No deployment credentials have defaults. Provision the MySQL 8 database before startup. The JDBC URL requires sslMode=VERIFY_IDENTITY: configure the server certificate chain in the JVM trust store and use a hostname matching that certificate. Production rejects combinations with dev, demo or test and rejects schema-update, demo-init and unsafe migration overrides.

Flyway runs the committed versioned migrations before Hibernate validates the schema. V1 creates the current schema for a new, empty database; it contains no business/demo rows. Hibernate does not repair or create production tables. SQL and parameter logging are suppressed. Flyway clean and automatic baselining are disabled. A nonempty database with no migration history fails startup instead of being silently adopted. The migration account needs DDL privileges; if using the runtime account for application-managed migrations, account for that privilege in deployment. Separate migration credentials can be supplied through Spring's Flyway configuration where your deployment uses a dedicated migration account.

Never edit V1 after it has been applied. Commit future changes as V2, V3 and later migrations, test them on a restored copy, then deploy. MySQL DDL can commit partially; a failed migration requires investigation and a restore/recovery plan, not blind retries or automatic repair. V4 converts the money and quantity columns from `float(53)` to `DECIMAL`; on InnoDB this rebuilds each affected table, so run it in a maintenance window. MySqlSchemaValidationTest boots the application against a real MySQL 8 schema at `ddl-auto=validate` and runs in CI via a MySQL service container.

## Existing databases and restore rehearsal

Do not enable baseline-on-migrate or switch an existing client database directly to production. First make a backup using your MySQL backup tooling and restore it into a separate, access-controlled rehearsal database. Use prompted credentials or a protected client option file, never a password in a command line. A typical logical backup uses mysqldump with single-transaction, routines, triggers and events enabled; ensure the backup contains the intended schema and that no schema changes occur during the snapshot. Protect and retain the backup according to the deployment's requirements.

Compare the restored database with V1, including column types, nullability, enum values, keys, indexes and foreign keys. Legacy databases may differ from current entity mappings; prepare and review a specific migration for those differences. Do not discard rows or relax constraints simply to make validation pass. Only after the restored schema is confirmed equivalent to V1 should an operator explicitly baseline that database at version 1 with the matching Flyway tooling. Keep the application's automatic baseline option disabled. Repeat the verified process for the real deployment during its maintenance window.

Validate the rehearsal by comparing row counts and key business totals (invoice values, allocations, customer balances and stock), checking organization isolation and logging in through the application. Record backup time, restore duration, schema version and verification results. Test a subsequent restart to ensure migrations do not rerun and saved shop names remain unchanged. Approve deployment only after the MySQL-specific migration, TLS and restore checks pass. The default H2 suite exercises service behaviour only; it does not establish production MySQL compatibility. Run `mvnw test -Dtest=MySqlSchemaValidationTest` with `TEST_MYSQL=true` and `DEV_DB_URL` pointing at a disposable MySQL 8 schema to verify migrations and entity mappings together.
