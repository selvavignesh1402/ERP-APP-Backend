-- Products with no GST have an empty rate. Do not change historical invoice totals.
ALTER TABLE products MODIFY COLUMN gst_rate DECIMAL(7,4) NULL;
UPDATE products SET gst_rate = NULL WHERE gst_rate = 0;
ALTER TABLE sales ADD COLUMN tax_type VARCHAR(20) NOT NULL DEFAULT 'INTRA_STATE';
ALTER TABLE sales_orders ADD COLUMN tax_type VARCHAR(20) NOT NULL DEFAULT 'INTRA_STATE';
-- Old invoices/orders used a blanket 5%; retain that legacy rate in their snapshots.
ALTER TABLE sales_items ADD COLUMN gst_rate DECIMAL(7,4) NULL DEFAULT 5;
ALTER TABLE sales_order_items ADD COLUMN gst_rate DECIMAL(7,4) NULL DEFAULT 5;
