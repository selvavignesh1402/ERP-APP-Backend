-- Exact decimal storage for monetary and fractional-quantity columns.
--
-- V1 declared every numeric business value as float(53). Binary floating point
-- cannot represent values such as 0.1 or 1234.56 exactly, so persisted totals
-- drifted and the services had to round defensively (Math.round(x*100)/100).
--
-- These columns hold exact decimal values, so DECIMAL storage removes the
-- rounding error at rest. The Java fields deliberately stay `double`: they are
-- annotated @JdbcTypeCode(SqlTypes.DECIMAL) so Hibernate binds and extracts
-- them as DECIMAL. See DECIMAL_STORAGE.md for why precision/scale are not
-- declared on the entity fields (specifying `scale` on a floating-point
-- mapping is rejected: "scale has no meaning for SQL floating point types").
--
-- Precision choices:
--   money     DECIMAL(19,4)  15 integer digits + 4 decimal (paise-safe, and
--                           leaves headroom for intermediate tax arithmetic)
--   quantity  DECIMAL(19,6)  fractional stock is weighed in kg, not whole units
--   rate      DECIMAL(7,4)   GST is a percentage
--
-- Deliberately NOT converted:
--   suppliers.rating           a 0-5 score, not money
--   visit_check_ins.latitude   geographic coordinates, and signed
--   visit_check_ins.longitude  (negative for western/southern hemispheres)
--
-- Operational note: float(53) -> DECIMAL is a table rebuild on InnoDB for most
-- of these columns. On a large client database run this during a maintenance
-- window; the statements are one per table so a single failure is attributable.

-- ---------- money: DECIMAL(19,4) ----------

alter table customers modify column credit_balance decimal(19,4) not null;
alter table customers modify column credit_limit decimal(19,4) not null;

alter table delivery_items modify column unit_price decimal(19,4) not null;

alter table goods_receipt_items modify column unit_price decimal(19,4) not null;

alter table payment_sale_allocations modify column amount decimal(19,4) not null;

alter table payments modify column amount decimal(19,4) not null;
alter table payments modify column opening_balance_amount decimal(19,4) null;

alter table price_history modify column price decimal(19,4) not null;

alter table products modify column purchase_price decimal(19,4) not null;
alter table products modify column selling_price decimal(19,4) not null;

alter table purchase modify column total_amount decimal(19,4) not null;

alter table purchase_items modify column price decimal(19,4) not null;

alter table reconciliation_results modify column amount_matched decimal(19,4) not null;
alter table reconciliation_results modify column amount_on_invoice decimal(19,4) not null;
alter table reconciliation_results modify column amount_on_purchase decimal(19,4) not null;

alter table sales modify column cgst decimal(19,4) not null;
alter table sales modify column discount decimal(19,4) not null;
alter table sales modify column grand_total decimal(19,4) not null;
alter table sales modify column igst decimal(19,4) not null;
alter table sales modify column sgst decimal(19,4) not null;
alter table sales modify column total decimal(19,4) not null;

alter table sales_items modify column price decimal(19,4) not null;

alter table sales_order_items modify column total_price decimal(19,4) not null;
alter table sales_order_items modify column unit_price decimal(19,4) not null;

alter table sales_orders modify column discount decimal(19,4) not null;
alter table sales_orders modify column grand_total decimal(19,4) not null;
alter table sales_orders modify column subtotal decimal(19,4) not null;
alter table sales_orders modify column tax_amount decimal(19,4) not null;

alter table supplier_invoice_items modify column total_amount decimal(19,4) not null;
alter table supplier_invoice_items modify column unit_price decimal(19,4) not null;

alter table supplier_invoices modify column total_amount decimal(19,4) not null;

alter table supplier_products modify column purchase_price decimal(19,4) not null;

-- ---------- quantity: DECIMAL(19,6) ----------

alter table goods_receipt_items modify column ordered_qty decimal(19,6) not null;
alter table goods_receipt_items modify column received_qty decimal(19,6) not null;

alter table products modify column stock decimal(19,6) not null;
alter table products modify column minimum_stock decimal(19,6) not null;

alter table purchase_items modify column quantity decimal(19,6) not null;

alter table purchase_returns modify column quantity_returned decimal(19,6) not null;

alter table sales_items modify column quantity decimal(19,6) not null;

alter table stock_adjustments modify column quantity_change decimal(19,6) not null;

alter table stock_movements modify column quantity decimal(19,6) not null;

alter table supplier_invoice_items modify column quantity decimal(19,6) not null;

alter table supplier_products modify column min_order_qty decimal(19,6) null;

-- ---------- rate: DECIMAL(7,4) ----------

alter table products modify column gst_rate decimal(7,4) not null;