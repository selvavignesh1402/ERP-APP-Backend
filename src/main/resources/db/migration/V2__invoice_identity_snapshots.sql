-- Preserve invoice identity when product/customer/shop master data changes.
-- Existing invoices have no trustworthy historical snapshots; leave them NULL.
-- The UI labels those records as legacy rather than inventing historical values.
ALTER TABLE sales ADD COLUMN shop_name varchar(255) NULL;
ALTER TABLE sales ADD COLUMN customer_phone varchar(255) NULL;
ALTER TABLE sales ADD COLUMN customer_address varchar(255) NULL;
ALTER TABLE sales_items ADD COLUMN product_name varchar(255) NULL;
ALTER TABLE sales_items ADD COLUMN unit varchar(255) NULL;
