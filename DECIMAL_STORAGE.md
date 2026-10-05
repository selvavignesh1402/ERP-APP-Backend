# Exact decimal storage (V4)

V1 declared every monetary and fractional-quantity column as `float(53)`. Binary
floating point cannot represent values such as `0.1` or `1234.56` exactly, so stored
totals drifted and the services compensated with `Math.round(x * 100.0) / 100.0`.

V4 converts those columns to `DECIMAL`. Persisted values are now exact, and the
compensating rounding is no longer load-bearing for storage.

## Column types

| Group | Type | Applies to |
|---|---|---|
| Money | `DECIMAL(19,4)` | invoice totals, tax, discounts, prices, credit balances, payment allocations |
| Quantity | `DECIMAL(19,6)` | stock, fractional quantities, goods receipt and movement quantities |
| Rate | `DECIMAL(7,4)` | `products.gst_rate` |

Deliberately left as `float(53)`:

- `suppliers.rating` — a 0–5 score, not money
- `visit_check_ins.latitude`, `visit_check_ins.longitude` — geographic coordinates, and
  they must stay signed for the southern and western hemispheres

## Why the Java fields are still `double`

Converting the fields to `BigDecimal` is the ideal end state, but it touches roughly a
hundred call sites across thirty services and most of the test suite. The storage
problem does not require that, so the fields stay `double` and are annotated instead:

```java
@JdbcTypeCode(SqlTypes.DECIMAL)
@Column(name = "grand_total", nullable = false, columnDefinition = "decimal(19,4)")
private double grandTotal = 0.0;
```

Both annotations are required, for different reasons:

- `@JdbcTypeCode(SqlTypes.DECIMAL)` changes the SQL type Hibernate binds and extracts.
  Without it, `ddl-auto=validate` in production **refuses to start**, because
  `Dialect.equivalentTypes` treats `DOUBLE` and `DECIMAL` as different types. The
  observed failure is:
  `Schema-validation: wrong column type encountered in column [amount] ... found
  [decimal (Types#DECIMAL)], but expecting [float(53) (Types#FLOAT)]`
- `columnDefinition` controls generated DDL, so `dev` (which runs `ddl-auto=update`)
  produces exactly the V4 types instead of `decimal(53,2)`.

Precision and scale deliberately live in `columnDefinition`, **not** in
`@Column(precision = ..., scale = ...)`. Hibernate rejects a scale on a
floating-point mapping at metadata build time:

```
scale has no meaning for SQL floating point types
```

## Verifying a mapping change

`MySqlSchemaValidationTest` applies every migration to a real MySQL 8 schema and then
starts the application with `ddl-auto=validate`, which is the configuration
`DatabaseProfileGuard` pins in production. It replaces the older `SchemaExportTest`,
which compared generated DDL text against the V1 baseline and so reported every
legitimate later migration as drift.

```powershell
$env:DEV_DB_URL="jdbc:mysql://127.0.0.1:3306/erp_probe_tmp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DEV_DB_USERNAME="root"; $env:DEV_DB_PASSWORD="root"; $env:TEST_MYSQL="true"
mvnw test -Dtest=MySqlSchemaValidationTest
```

Point it at a disposable schema: the test migrates it from scratch. CI runs the same
test against a MySQL 8 service container.

## Remaining known imprecision

Converting the columns fixes what is *stored*. Arithmetic in the service layer is
still performed in `double` and can still drift before it is persisted, so the
existing `BigDecimal` conversions and rounding remain in place. Moving the fields to
`BigDecimal` is the follow-up that removes that, and is best done once the in-flight
work on these services is committed.