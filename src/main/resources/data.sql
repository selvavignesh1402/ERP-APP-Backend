-- Seed Suppliers (status maps to com.riceerp.backend.enums.Status)
INSERT INTO suppliers (id, supplier_name, phone, email, address, gst_number, rating, category, status, created_at)
VALUES 
(1, 'Sri Balaji Rice Mills', '9876543210', 'balaji@ricemill.com', 'Andhra Pradesh, India', '37AAAAA0000A1Z1', 4.8, 'BASMATI', 'ACTIVE', NOW()),
(2, 'Aditya Grain Traders', '8765432109', 'aditya@grains.com', 'Punjab, India', '03BBBBB1111B2Z2', 4.5, 'RAW_RICE', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE supplier_name=VALUES(supplier_name);

-- Seed Products (status maps to com.riceerp.backend.enums.Status)
INSERT INTO products (id, product_name, category, brand, unit, purchase_price, selling_price, stock, minimum_stock, gst_rate, hsn_code, status, created_at)
VALUES 
(1, 'Premium Basmati Rice', 'Basmati', 'Royal Grain', '25kg', 1800.0, 2200.0, 50.0, 10.0, 5.0, '10063010', 'ACTIVE', NOW()),
(2, 'Sona Masoori Rice', 'Raw Rice', 'HMT', '25kg', 1100.0, 1400.0, 80.0, 15.0, 5.0, '10063020', 'ACTIVE', NOW()),
(3, 'Kolam Rice Premium', 'Raw Rice', 'Bullet', '25kg', 1250.0, 1600.0, 30.0, 8.0, 5.0, '10063090', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name);

-- Seed Price History (price_type maps to com.riceerp.backend.enums.PriceType)
INSERT INTO price_history (id, product_id, price_type, price, effective_from)
VALUES 
(1, 1, 'PURCHASE', 1800.0, NOW()),
(2, 1, 'SELLING', 2200.0, NOW()),
(3, 2, 'PURCHASE', 1100.0, NOW()),
(4, 2, 'SELLING', 1400.0, NOW()),
(5, 3, 'PURCHASE', 1250.0, NOW()),
(6, 3, 'SELLING', 1600.0, NOW())
ON DUPLICATE KEY UPDATE price=VALUES(price);
