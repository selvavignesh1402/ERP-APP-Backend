-- Seed Suppliers (status maps to com.riceerp.backend.enums.Status)
INSERT INTO suppliers (id, supplier_name, phone, email, address, gst_number, rating, category, status, created_at)
VALUES
(1, 'Sri Balaji Rice Mills', '9876543210', 'balaji@ricemill.com', '12 Rice Mill Road, Nellore, Andhra Pradesh', '37AAAAA0000A1Z1', 4.8, 'BASMATI', 'ACTIVE', NOW()),
(2, 'Aditya Grain Traders', '8765432109', 'aditya@grains.com', '45 Grain Market, Ludhiana, Punjab', '03BBBBB1111B2Z2', 4.5, 'RAW_RICE', 'ACTIVE', NOW()),
(3, 'Shakthi Rice Industries', '9876543211', 'shakthi@ricemill.com', '78 Industrial Estate, Thanjavur, Tamil Nadu', '33CCCCC2222C3Z3', 4.7, 'ALL_RICE', 'ACTIVE', NOW()),
(4, 'Krishna Agro Foods', '9876543212', 'krishna@agro.in', '34 Agro Road, Mysore, Karnataka', '29DDDDD3333D4Z4', 4.6, 'ORGANIC', 'ACTIVE', NOW()),
(5, 'Ganesh Rice General', '9876543213', 'ganesh@rice.in', '56 Wholesale Market, Warangal, Telangana', '36EEEEE4444E5Z5', 4.3, 'RAW_RICE', 'ACTIVE', NOW()),
(6, 'Devi Rice Mills', '9876543214', 'devi@ricemill.in', '90 Rice Complex, Bardhaman, West Bengal', '19FFFFF5555F6Z6', 4.4, 'SPECIALITY', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE supplier_name=VALUES(supplier_name);

-- Seed Products (status maps to com.riceerp.backend.enums.Status)
INSERT INTO products (id, product_name, category, brand, unit, purchase_price, selling_price, stock, minimum_stock, gst_rate, hsn_code, status, created_at)
VALUES
-- Basmati Varieties (Premium)
(1, '1121 Basmati Rice', 'Basmati', 'Royal Grain', '25kg', 1800.0, 2400.0, 50.0, 10.0, 5.0, '10063010', 'ACTIVE', NOW()),
(2, 'Pusa Basmati Rice', 'Basmati', 'Golden Harvest', '25kg', 1400.0, 1900.0, 40.0, 8.0, 5.0, '10063010', 'ACTIVE', NOW()),
(3, 'Traditional Basmati Rice', 'Basmati', 'Royal Grain', '10kg', 900.0, 1200.0, 35.0, 10.0, 5.0, '10063010', 'ACTIVE', NOW()),
-- Raw Rice Varieties (Mass Market)
(4, 'Premium Sona Masoori Rice', 'Raw Rice', 'HMT', '25kg', 1200.0, 1550.0, 80.0, 15.0, 5.0, '10063020', 'ACTIVE', NOW()),
(5, 'Regular Sona Masoori Rice', 'Raw Rice', 'Bullet', '50kg', 2300.0, 2900.0, 60.0, 10.0, 5.0, '10063020', 'ACTIVE', NOW()),
(6, 'Ponni Raw Rice', 'Raw Rice', 'Shakthi', '25kg', 1050.0, 1350.0, 70.0, 12.0, 5.0, '10063090', 'ACTIVE', NOW()),
(7, 'Ponni Boiled Rice', 'Raw Rice', 'Shakthi', '25kg', 950.0, 1250.0, 90.0, 15.0, 5.0, '10063090', 'ACTIVE', NOW()),
(8, 'Idly Rice', 'Raw Rice', 'Ganesh', '25kg', 900.0, 1150.0, 45.0, 10.0, 5.0, '10063090', 'ACTIVE', NOW()),
-- Speciality Rice
(9, 'Kerala Matta Rice', 'Speciality', 'Devi', '25kg', 1400.0, 1750.0, 30.0, 8.0, 5.0, '10063090', 'ACTIVE', NOW()),
(10, 'Surti Kolam Rice', 'Speciality', 'Premium Fresh', '10kg', 600.0, 800.0, 55.0, 12.0, 5.0, '10063090', 'ACTIVE', NOW()),
-- Healthy/Organic
(11, 'Brown Rice', 'Organic', 'Krishna', '5kg', 325.0, 425.0, 40.0, 10.0, 5.0, '10062000', 'ACTIVE', NOW()),
(12, 'Organic Sona Masoori Rice', 'Organic', 'Krishna', '10kg', 600.0, 850.0, 35.0, 8.0, 5.0, '10063020', 'ACTIVE', NOW()),
-- Other
(13, 'Steamed Basmati Rice', 'Basmati', 'Golden Harvest', '25kg', 1600.0, 2150.0, 25.0, 6.0, 5.0, '10063010', 'ACTIVE', NOW()),
(14, 'Broken Rice (Industrial)', 'Other', 'Ganesh', '50kg', 1000.0, 1300.0, 100.0, 20.0, 5.0, '10064000', 'ACTIVE', NOW()),
(15, 'Basmati Brown Rice', 'Organic', 'Krishna', '5kg', 425.0, 550.0, 30.0, 8.0, 5.0, '10062000', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name);

-- Seed Price History (price_type maps to com.riceerp.backend.enums.PriceType)
INSERT INTO price_history (id, product_id, price_type, price, effective_from)
VALUES
(1, 1, 'PURCHASE', 1800.0, NOW()),
(2, 1, 'SELLING', 2400.0, NOW()),
(3, 2, 'PURCHASE', 1400.0, NOW()),
(4, 2, 'SELLING', 1900.0, NOW()),
(5, 3, 'PURCHASE', 900.0, NOW()),
(6, 3, 'SELLING', 1200.0, NOW()),
(7, 4, 'PURCHASE', 1200.0, NOW()),
(8, 4, 'SELLING', 1550.0, NOW()),
(9, 5, 'PURCHASE', 2300.0, NOW()),
(10, 5, 'SELLING', 2900.0, NOW()),
(11, 6, 'PURCHASE', 1050.0, NOW()),
(12, 6, 'SELLING', 1350.0, NOW()),
(13, 7, 'PURCHASE', 950.0, NOW()),
(14, 7, 'SELLING', 1250.0, NOW()),
(15, 8, 'PURCHASE', 900.0, NOW()),
(16, 8, 'SELLING', 1150.0, NOW()),
(17, 9, 'PURCHASE', 1400.0, NOW()),
(18, 9, 'SELLING', 1750.0, NOW()),
(19, 10, 'PURCHASE', 600.0, NOW()),
(20, 10, 'SELLING', 800.0, NOW()),
(21, 11, 'PURCHASE', 325.0, NOW()),
(22, 11, 'SELLING', 425.0, NOW()),
(23, 12, 'PURCHASE', 600.0, NOW()),
(24, 12, 'SELLING', 850.0, NOW()),
(25, 13, 'PURCHASE', 1600.0, NOW()),
(26, 13, 'SELLING', 2150.0, NOW()),
(27, 14, 'PURCHASE', 1000.0, NOW()),
(28, 14, 'SELLING', 1300.0, NOW()),
(29, 15, 'PURCHASE', 425.0, NOW()),
(30, 15, 'SELLING', 550.0, NOW())
ON DUPLICATE KEY UPDATE price=VALUES(price);

-- Seed Customers (status maps to com.riceerp.backend.enums.Status)
INSERT INTO customers (id, customer_name, phone, email, address, gst_number, credit_limit, credit_balance, status, created_at)
VALUES
(1, 'Anbu Rice Mart', '9871234560', 'anbu@ricemart.com', '12 Main Road, Salem, Tamil Nadu', '33AAAAA1111A1Z1', 100000.0, 15000.0, 'ACTIVE', NOW()),
(2, 'Murugan Stores', '9876501234', 'murugan@stores.in', '45 Bazaar Street, Erode, Tamil Nadu', '33BBBBB2222B2Z2', 75000.0, 5000.0, 'ACTIVE', NOW()),
(3, 'Lakshmi Traders', '9012345678', 'lakshmi@traders.in', '78 Market Lane, Coimbatore, Tamil Nadu', '33CCCCC3333C3Z3', 50000.0, 12000.0, 'ACTIVE', NOW()),
(4, 'Selvam Supermarket', '9023456789', 'selvam@supermarket.in', '100 Anna Nagar, Chennai, Tamil Nadu', '33DDDDD4444D4Z4', 150000.0, 0.0, 'ACTIVE', NOW()),
(5, 'Kannan Wholesale', '9034567890', 'kannan@wholesale.in', '22 Grain Market, Madurai, Tamil Nadu', '33EEEEE5555E5Z5', 200000.0, 45000.0, 'ACTIVE', NOW()),
(6, 'Tamil Traders', '9045678901', 'tamil@traders.in', '67 Big Street, Trichy, Tamil Nadu', '33FFFFF6666F6Z6', 80000.0, 8000.0, 'ACTIVE', NOW()),
(7, 'Sri Vinayaga Stores', '9056789012', 'vinayaga@stores.in', '89 Market Road, Bangalore, Karnataka', '29GGGGG7777G7Z7', 125000.0, 25000.0, 'ACTIVE', NOW()),
(8, 'Raja Rice Distributors', '9067890123', 'raja@distributors.in', '56 Wholesale Complex, Hyderabad, Telangana', '36HHHHH8888H8Z8', 250000.0, 60000.0, 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE customer_name=VALUES(customer_name);

-- Seed Sample Purchases (for testing purchase flow)
-- Note: `id` is manually specified so that the sample payments below can reference these purchase IDs.
-- In production, the application generates IDs automatically.
INSERT INTO purchase (id, supplier_id, invoice_number, purchase_date, total_amount, status, created_at)
VALUES
(1, 1, 'PUR-2024-001', DATE_SUB(NOW(), INTERVAL 10 DAY), 24000.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 3, 'PUR-2024-002', DATE_SUB(NOW(), INTERVAL 8 DAY), 18000.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 4, 'PUR-2024-003', DATE_SUB(NOW(), INTERVAL 5 DAY), 8500.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 2, 'PUR-2024-004', DATE_SUB(NOW(), INTERVAL 3 DAY), 29000.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 5, 'PUR-2024-005', DATE_SUB(NOW(), INTERVAL 1 DAY), 13000.0, 'DRAFT', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE invoice_number=VALUES(invoice_number);

-- Normalize any legacy purchase status values to valid PurchaseStatus enum values
UPDATE purchase SET status = 'DRAFT' WHERE status NOT IN
  ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'COMPLETED', 'CANCELLED');

-- Seed Purchase Items
INSERT INTO purchase_items (id, purchase_id, product_id, quantity, price)
VALUES
(1, 1, 1, 10, 1800.0),
(2, 1, 2, 5, 1400.0),
(3, 2, 6, 10, 1050.0),
(4, 2, 7, 6, 950.0),
(5, 3, 11, 20, 325.0),
(6, 3, 12, 5, 600.0),
(7, 4, 5, 10, 2300.0),
(8, 5, 14, 10, 1000.0),
(9, 5, 8, 3, 900.0)
ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);

-- Seed Sample Sales (for testing POS and dashboard)
INSERT INTO sales (id, bill_number, customer_id, customer_name, sale_date, payment_mode, total, discount, cgst, sgst, igst, grand_total, created_at)
VALUES
(1, 'SALE-2024-001', 1, 'Anbu Rice Mart', DATE_SUB(NOW(), INTERVAL 2 DAY), 'CREDIT', 11000.0, 500.0, 262.5, 262.5, 0.0, 11025.0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'SALE-2024-002', NULL, 'Cash Customer', DATE_SUB(NOW(), INTERVAL 1 DAY), 'CASH', 2400.0, 0.0, 60.0, 60.0, 0.0, 2520.0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 'SALE-2024-003', 4, 'Selvam Supermarket', DATE_SUB(NOW(), INTERVAL 1 DAY), 'UPI', 8700.0, 200.0, 212.5, 212.5, 0.0, 8925.0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 'SALE-2024-004', 3, 'Lakshmi Traders', NOW(), 'CREDIT', 5150.0, 0.0, 128.75, 128.75, 0.0, 5407.5, NOW()),
(5, 'SALE-2024-005', NULL, 'Cash Customer', NOW(), 'CASH', 1300.0, 0.0, 32.5, 32.5, 0.0, 1365.0, NOW())
ON DUPLICATE KEY UPDATE bill_number=VALUES(bill_number);

-- Seed Sale Items
INSERT INTO sales_items (id, sale_id, product_id, quantity, price)
VALUES
(1, 1, 1, 3, 2400.0),
(2, 1, 2, 2, 1900.0),
(3, 2, 3, 2, 1200.0),
(4, 3, 4, 5, 1550.0),
(5, 3, 10, 2, 800.0),
(6, 3, 6, 1, 1350.0),
(7, 4, 7, 3, 1250.0),
(8, 4, 8, 1, 1150.0),
(9, 4, 9, 1, 1750.0),
(10, 5, 14, 1, 1300.0)
ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);

-- Seed Payments (for testing payment ledger)
-- PaymentMode enum: CASH, UPI, CARD, CREDIT
-- ReferenceType enum: SALE, PURCHASE
INSERT INTO payments (id, reference_type, reference_id, amount, payment_mode, payment_date)
VALUES
(1, 'PURCHASE', 1, 24000.0, 'CASH', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 'PURCHASE', 2, 18000.0, 'UPI', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 'PURCHASE', 3, 8500.0, 'CASH', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 'PURCHASE', 4, 29000.0, 'UPI', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 'SALE', 1, 11025.0, 'CREDIT', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 'SALE', 2, 2520.0, 'CASH', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 'SALE', 3, 8925.0, 'UPI', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 'SALE', 4, 5407.5, 'CREDIT', NOW()),
(9, 'SALE', 5, 1365.0, 'CASH', NOW())
ON DUPLICATE KEY UPDATE amount=VALUES(amount);
