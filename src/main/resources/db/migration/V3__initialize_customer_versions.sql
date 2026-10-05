-- Legacy customers predate optimistic locking. Hibernate cannot update a
-- customer with a null version when a credit invoice changes its balance.
UPDATE customers SET version = 0 WHERE version IS NULL;
ALTER TABLE customers MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
