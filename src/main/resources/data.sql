-- Seed Organization
INSERT INTO organizations (id, name, created_at)
VALUES 
(1, 'Default Retailer', NOW()),
(2, 'Sam''s Shop', NOW()),
(3, 'Royal Bakery & Confectionery', NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Seed Suppliers (status maps to com.riceerp.backend.enums.Status)
INSERT INTO suppliers (organization_id, id, supplier_name, phone, email, address, gst_number, rating, category, status, created_at)
VALUES
(1, 1, 'Sri Balaji Rice Mills', '9876543210', 'balaji@ricemill.com', '12 Rice Mill Road, Nellore, Andhra Pradesh', '37AAAAA0000A1Z1', 4.8, 'BASMATI', 'ACTIVE', NOW()),
(1, 2, 'Aditya Grain Traders', '8765432109', 'aditya@grains.com', '45 Grain Market, Ludhiana, Punjab', '03BBBBB1111B2Z2', 4.5, 'RAW_RICE', 'ACTIVE', NOW()),
(1, 3, 'Shakthi Rice Industries', '9876543211', 'shakthi@ricemill.com', '78 Industrial Estate, Thanjavur, Tamil Nadu', '33CCCCC2222C3Z3', 4.7, 'ALL_RICE', 'ACTIVE', NOW()),
(1, 4, 'Krishna Agro Foods', '9876543212', 'krishna@agro.in', '34 Agro Road, Mysore, Karnataka', '29DDDDD3333D4Z4', 4.6, 'ORGANIC', 'ACTIVE', NOW()),
(1, 5, 'Ganesh Rice General', '9876543213', 'ganesh@rice.in', '56 Wholesale Market, Warangal, Telangana', '36EEEEE4444E5Z5', 4.3, 'RAW_RICE', 'ACTIVE', NOW()),
(1, 6, 'Devi Rice Mills', '9876543214', 'devi@ricemill.in', '90 Rice Complex, Bardhaman, West Bengal', '19FFFFF5555F6Z6', 4.4, 'SPECIALITY', 'ACTIVE', NOW()),
-- Org 2 Suppliers (Grocery & FMCG)
(2, 7, 'Fresh Produce Farm Suppliers', '9123456780', 'fresh@farms.com', '12 Veg Wholesale Market, Salem, Tamil Nadu', '33AAABBB1111A1Z1', 4.9, 'SPECIALITY', 'ACTIVE', NOW()),
(2, 8, 'Heritage Foods & Dairy Ltd', '9123456781', 'sales@heritage.in', '88 Dairy Tech Park, Chennai, Tamil Nadu', '33AAABBB2222A1Z2', 4.7, 'ORGANIC', 'ACTIVE', NOW()),
(2, 9, 'Sunrise Spices & Provisions', '9123456782', 'info@sunrisespices.com', '45 Spice Bazaar, Madurai, Tamil Nadu', '33AAABBB3333A1Z3', 4.6, 'ALL_RICE', 'ACTIVE', NOW()),
-- Org 3 Suppliers (Bakery & Confectionery Ingredients)
(3, 10, 'BakeMaster Ingredients Ltd', '9123456790', 'bakemaster@bakery.com', '77 Food Tech Zone, Coimbatore, Tamil Nadu', '33AAABBB4444A1Z4', 4.9, 'SPECIALITY', 'ACTIVE', NOW()),
(3, 11, 'Cocoa & Dairy Supplies', '9123456791', 'cocoa@dairy.com', '12 Chocolate Estate, Ooty, Tamil Nadu', '33AAABBB5555A1Z5', 4.8, 'ORGANIC', 'ACTIVE', NOW()),
(3, 12, 'Golden Pack Packaging Co', '9123456792', 'pack@golden.com', '99 Industrial Hub, Hosur, Tamil Nadu', '33AAABBB6666A1Z6', 4.5, 'ALL_RICE', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE id = id;

-- Seed Products (status maps to com.riceerp.backend.enums.Status)
INSERT INTO products (organization_id, id, product_name, category, brand, unit, purchase_price, selling_price, stock, minimum_stock, gst_rate, hsn_code, status, version, created_at)
VALUES
-- Org 1 Products (Rice Wholesale)
(1, 1, '1121 Basmati Rice', 'Basmati', 'Royal Grain', '25kg', 1800.0, 2400.0, 50.0, 10.0, 5.0, '10063010', 'ACTIVE', 0, NOW()),
(1, 2, 'Pusa Basmati Rice', 'Basmati', 'Golden Harvest', '25kg', 1400.0, 1900.0, 40.0, 8.0, 5.0, '10063010', 'ACTIVE', 0, NOW()),
(1, 3, 'Traditional Basmati Rice', 'Basmati', 'Royal Grain', '10kg', 900.0, 1200.0, 35.0, 10.0, 5.0, '10063010', 'ACTIVE', 0, NOW()),
(1, 4, 'Premium Sona Masoori Rice', 'Raw Rice', 'HMT', '25kg', 1200.0, 1550.0, 80.0, 15.0, 5.0, '10063020', 'ACTIVE', 0, NOW()),
(1, 5, 'Regular Sona Masoori Rice', 'Raw Rice', 'Bullet', '50kg', 2300.0, 2900.0, 60.0, 10.0, 5.0, '10063020', 'ACTIVE', 0, NOW()),
(1, 6, 'Ponni Raw Rice', 'Raw Rice', 'Shakthi', '25kg', 1050.0, 1350.0, 70.0, 12.0, 5.0, '10063090', 'ACTIVE', 0, NOW()),
(1, 7, 'Ponni Boiled Rice', 'Raw Rice', 'Shakthi', '25kg', 950.0, 1250.0, 90.0, 15.0, 5.0, '10063090', 'ACTIVE', 0, NOW()),
(1, 8, 'Idly Rice', 'Raw Rice', 'Ganesh', '25kg', 900.0, 1150.0, 45.0, 10.0, 5.0, '10063090', 'ACTIVE', 0, NOW()),
(1, 9, 'Kerala Matta Rice', 'Speciality', 'Devi', '25kg', 1400.0, 1750.0, 30.0, 8.0, 5.0, '10063090', 'ACTIVE', 0, NOW()),
(1, 10, 'Surti Kolam Rice', 'Speciality', 'Premium Fresh', '10kg', 600.0, 800.0, 55.0, 12.0, 5.0, '10063090', 'ACTIVE', 0, NOW()),
(1, 11, 'Brown Rice', 'Organic', 'Krishna', '5kg', 325.0, 425.0, 40.0, 10.0, 5.0, '10062000', 'ACTIVE', 0, NOW()),
(1, 12, 'Organic Sona Masoori Rice', 'Organic', 'Krishna', '10kg', 600.0, 850.0, 35.0, 8.0, 5.0, '10063020', 'ACTIVE', 0, NOW()),
(1, 13, 'Steamed Basmati Rice', 'Basmati', 'Golden Harvest', '25kg', 1600.0, 2150.0, 25.0, 6.0, 5.0, '10063010', 'ACTIVE', 0, NOW()),
(1, 14, 'Broken Rice (Industrial)', 'Other', 'Ganesh', '50kg', 1000.0, 1300.0, 100.0, 20.0, 5.0, '10064000', 'ACTIVE', 0, NOW()),
(1, 15, 'Basmati Brown Rice', 'Organic', 'Krishna', '5kg', 425.0, 550.0, 30.0, 8.0, 5.0, '10062000', 'ACTIVE', 0, NOW()),
-- Org 2 Products (General Grocery & Provisions Variety)
(2, 101, 'Aashirvaad Whole Wheat Atta', 'Flour', 'Aashirvaad', '10kg', 420.0, 480.0, 40.0, 10.0, 5.0, '11010000', 'ACTIVE', 0, NOW()),
(2, 102, 'Fortune Sunflower Oil', 'Edible Oil', 'Fortune', '5L', 650.0, 720.0, 30.0, 8.0, 5.0, '15121910', 'ACTIVE', 0, NOW()),
(2, 103, 'Toor Dal (Arhar Dal)', 'Pulses', 'Tata Sampann', '1kg', 140.0, 165.0, 60.0, 15.0, 0.0, '07136000', 'ACTIVE', 0, NOW()),
(2, 104, 'Moong Dal Yellow', 'Pulses', 'Tata Sampann', '1kg', 110.0, 130.0, 50.0, 10.0, 0.0, '07133100', 'ACTIVE', 0, NOW()),
(2, 105, 'White Refined Sugar', 'Essentials', 'Madhur', '5kg', 210.0, 245.0, 80.0, 20.0, 5.0, '17019990', 'ACTIVE', 0, NOW()),
(2, 106, 'Red Label Tea', 'Beverages', 'Brooke Bond', '500g', 240.0, 280.0, 35.0, 10.0, 5.0, '09023020', 'ACTIVE', 0, NOW()),
(2, 107, 'Tata Salt Vacuum Evaporated', 'Essentials', 'Tata', '1kg', 22.0, 28.0, 150.0, 30.0, 0.0, '25010010', 'ACTIVE', 0, NOW()),
(2, 108, 'Everest Turmeric Powder', 'Spices', 'Everest', '200g', 52.0, 65.0, 45.0, 12.0, 5.0, '09103030', 'ACTIVE', 0, NOW()),
(2, 109, 'Everest Red Chilli Powder', 'Spices', 'Everest', '200g', 68.0, 85.0, 40.0, 10.0, 5.0, '09042211', 'ACTIVE', 0, NOW()),
(2, 110, 'Amul Butter Pasteurized', 'Dairy', 'Amul', '500g', 240.0, 275.0, 25.0, 8.0, 12.0, '04051000', 'ACTIVE', 0, NOW()),
-- Org 3 Products (Bakery & Confectionery Ingredients Variety)
(3, 201, 'Maida Fine Cake Flour 10kg', 'Flour', 'BakeMaster', '10kg', 380.0, 450.0, 50.0, 10.0, 5.0, '11010000', 'ACTIVE', 0, NOW()),
(3, 202, 'Pure Cocoa Powder 1kg', 'Baking', 'Hershey', '1kg', 550.0, 680.0, 30.0, 8.0, 18.0, '18050000', 'ACTIVE', 0, NOW()),
(3, 203, 'Dark Chocolate Compound 500g', 'Baking', 'Morde', '500g', 180.0, 230.0, 40.0, 10.0, 18.0, '18069010', 'ACTIVE', 0, NOW()),
(3, 204, 'Whipped Cream Liquid 1L', 'Dairy', 'Richs', '1L', 160.0, 210.0, 25.0, 8.0, 12.0, '04029990', 'ACTIVE', 0, NOW()),
(3, 205, 'Vanilla Essence Extract 500ml', 'Flavors', 'Bush', '500ml', 120.0, 160.0, 35.0, 10.0, 18.0, '33021010', 'ACTIVE', 0, NOW()),
(3, 206, 'Icing Sugar Powder 5kg', 'Essentials', 'Madhur', '5kg', 260.0, 310.0, 45.0, 10.0, 5.0, '17019990', 'ACTIVE', 0, NOW()),
(3, 207, 'Unsalted Baking Butter 1kg', 'Dairy', 'Amul', '1kg', 480.0, 560.0, 20.0, 5.0, 12.0, '04051000', 'ACTIVE', 0, NOW()),
(3, 208, 'Dry Active Yeast 500g', 'Baking', 'Prime', '500g', 150.0, 190.0, 60.0, 15.0, 18.0, '21021010', 'ACTIVE', 0, NOW()),
(3, 209, 'Baking Soda & Powder Combo', 'Baking', 'Weikfield', '500g', 80.0, 110.0, 50.0, 10.0, 18.0, '28363000', 'ACTIVE', 0, NOW()),
(3, 210, 'Choco Chips Dark 1kg', 'Confectionery', 'Morde', '1kg', 320.0, 410.0, 30.0, 8.0, 18.0, '18069020', 'ACTIVE', 0, NOW())
ON DUPLICATE KEY UPDATE id = id;

-- Seed Price History (price_type maps to com.riceerp.backend.enums.PriceType)
INSERT INTO price_history (organization_id, id, product_id, price_type, price, effective_from)
VALUES
-- Org 1 Price History
(1, 1, 1, 'PURCHASE', 1800.0, NOW()),
(1, 2, 1, 'SELLING', 2400.0, NOW()),
(1, 3, 2, 'PURCHASE', 1400.0, NOW()),
(1, 4, 2, 'SELLING', 1900.0, NOW()),
(1, 5, 3, 'PURCHASE', 900.0, NOW()),
(1, 6, 3, 'SELLING', 1200.0, NOW()),
(1, 7, 4, 'PURCHASE', 1200.0, NOW()),
(1, 8, 4, 'SELLING', 1550.0, NOW()),
(1, 9, 5, 'PURCHASE', 2300.0, NOW()),
(1, 10, 5, 'SELLING', 2900.0, NOW()),
(1, 11, 6, 'PURCHASE', 1050.0, NOW()),
(1, 12, 6, 'SELLING', 1350.0, NOW()),
(1, 13, 7, 'PURCHASE', 950.0, NOW()),
(1, 14, 7, 'SELLING', 1250.0, NOW()),
(1, 15, 8, 'PURCHASE', 900.0, NOW()),
(1, 16, 8, 'SELLING', 1150.0, NOW()),
(1, 17, 9, 'PURCHASE', 1400.0, NOW()),
(1, 18, 9, 'SELLING', 1750.0, NOW()),
(1, 19, 10, 'PURCHASE', 600.0, NOW()),
(1, 20, 10, 'SELLING', 800.0, NOW()),
(1, 21, 11, 'PURCHASE', 325.0, NOW()),
(1, 22, 11, 'SELLING', 425.0, NOW()),
(1, 23, 12, 'PURCHASE', 600.0, NOW()),
(1, 24, 12, 'SELLING', 850.0, NOW()),
(1, 25, 13, 'PURCHASE', 1600.0, NOW()),
(1, 26, 13, 'SELLING', 2150.0, NOW()),
(1, 27, 14, 'PURCHASE', 1000.0, NOW()),
(1, 28, 14, 'SELLING', 1300.0, NOW()),
(1, 29, 15, 'PURCHASE', 425.0, NOW()),
(1, 30, 15, 'SELLING', 550.0, NOW()),

-- Org 2 Price History
(2, 101, 101, 'PURCHASE', 420.0, NOW()),
(2, 102, 101, 'SELLING', 480.0, NOW()),
(2, 103, 102, 'PURCHASE', 650.0, NOW()),
(2, 104, 102, 'SELLING', 720.0, NOW()),
(2, 105, 103, 'PURCHASE', 140.0, NOW()),
(2, 106, 103, 'SELLING', 165.0, NOW()),
(2, 107, 104, 'PURCHASE', 110.0, NOW()),
(2, 108, 104, 'SELLING', 130.0, NOW()),
(2, 109, 105, 'PURCHASE', 210.0, NOW()),
(2, 110, 105, 'SELLING', 245.0, NOW())
ON DUPLICATE KEY UPDATE id = id;

-- Seed Customers (status maps to com.riceerp.backend.enums.Status)
INSERT INTO customers (organization_id, id, customer_name, phone, email, address, gst_number, credit_limit, credit_balance, status, created_at)
VALUES
-- Org 1 Customers
(1, 1, 'Anbu Rice Mart', '9871234560', 'anbu@ricemart.com', '12 Main Road, Salem, Tamil Nadu', '33AAAAA1111A1Z1', 100000.0, 15000.0, 'ACTIVE', NOW()),
(1, 2, 'Murugan Stores', '9876501234', 'murugan@stores.in', '45 Bazaar Street, Erode, Tamil Nadu', '33BBBBB2222B2Z2', 75000.0, 5000.0, 'ACTIVE', NOW()),
(1, 3, 'Lakshmi Traders', '9012345678', 'lakshmi@traders.in', '78 Market Lane, Coimbatore, Tamil Nadu', '33CCCCC3333C3Z3', 50000.0, 12000.0, 'ACTIVE', NOW()),
(1, 4, 'Selvam Supermarket', '9023456789', 'selvam@supermarket.in', '100 Anna Nagar, Chennai, Tamil Nadu', '33DDDDD4444D4Z4', 150000.0, 0.0, 'ACTIVE', NOW()),
(1, 5, 'Kannan Wholesale', '9034567890', 'kannan@wholesale.in', '22 Grain Market, Madurai, Tamil Nadu', '33EEEEE5555E5Z5', 200000.0, 45000.0, 'ACTIVE', NOW()),
(1, 6, 'Tamil Traders', '9045678901', 'tamil@traders.in', '67 Big Street, Trichy, Tamil Nadu', '33FFFFF6666F6Z6', 80000.0, 8000.0, 'ACTIVE', NOW()),
(1, 7, 'Sri Vinayaga Stores', '9056789012', 'vinayaga@stores.in', '89 Market Road, Bangalore, Karnataka', '29GGGGG7777G7Z7', 125000.0, 25000.0, 'ACTIVE', NOW()),
(1, 8, 'Raja Rice Distributors', '9067890123', 'raja@distributors.in', '56 Wholesale Complex, Hyderabad, Telangana', '36HHHHH8888H8Z8', 250000.0, 60000.0, 'ACTIVE', NOW()),

-- Org 2 Customers
(2, 101, 'Karthik General Store', '9789012345', 'karthik@store.in', '15 Gandhi Road, Salem, Tamil Nadu', '33CCCC1111A1Z1', 80000.0, 12000.0, 'ACTIVE', NOW()),
(2, 102, 'Green Leaf Restaurant', '9789012346', 'greenleaf@rest.com', '88 Cross Street, Salem, Tamil Nadu', '33CCCC2222A1Z2', 120000.0, 24000.0, 'ACTIVE', NOW()),
(2, 103, 'Saravana Bakery & Sweets', '9789012347', 'saravana@sweets.in', '44 Bus Stand Complex, Salem, Tamil Nadu', '33CCCC3333A1Z3', 50000.0, 0.0, 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE id = id;

-- Seed Sample Purchases (for testing purchase flow)
INSERT INTO purchase (organization_id, id, supplier_id, invoice_number, purchase_date, total_amount, status, created_at)
VALUES
-- Org 1 Purchases
(1, 1, 1, 'PUR-2024-001', DATE_SUB(NOW(), INTERVAL 10 DAY), 24000.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1, 2, 3, 'PUR-2024-002', DATE_SUB(NOW(), INTERVAL 8 DAY), 18000.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1, 3, 4, 'PUR-2024-003', DATE_SUB(NOW(), INTERVAL 5 DAY), 8500.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 4, 2, 'PUR-2024-004', DATE_SUB(NOW(), INTERVAL 3 DAY), 29000.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 5, 5, 'PUR-2024-005', DATE_SUB(NOW(), INTERVAL 1 DAY), 13000.0, 'DRAFT', DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Org 2 Purchases
(2, 101, 7, 'PUR-ORG2-001', DATE_SUB(NOW(), INTERVAL 7 DAY), 18500.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 102, 8, 'PUR-ORG2-002', DATE_SUB(NOW(), INTERVAL 3 DAY), 12400.0, 'RECEIVED', DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE id = id;

-- Normalize any legacy purchase status values to valid PurchaseStatus enum values
UPDATE purchase SET status = 'DRAFT' WHERE status NOT IN
  ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'COMPLETED', 'CANCELLED');

-- Seed Purchase Items
INSERT INTO purchase_items (organization_id, id, purchase_id, product_id, quantity, price)
VALUES
-- Org 1 Purchase Items
(1, 1, 1, 1, 10, 1800.0),
(1, 2, 1, 2, 5, 1400.0),
(1, 3, 2, 6, 10, 1050.0),
(1, 4, 2, 7, 6, 950.0),
(1, 5, 3, 11, 20, 325.0),
(1, 6, 3, 12, 5, 600.0),
(1, 7, 4, 5, 10, 2300.0),
(1, 8, 5, 14, 10, 1000.0),
(1, 9, 5, 8, 3, 900.0),

-- Org 2 Purchase Items
(2, 101, 101, 101, 20, 420.0),
(2, 102, 101, 102, 10, 650.0),
(2, 103, 102, 103, 50, 140.0),
(2, 104, 102, 105, 20, 210.0)
ON DUPLICATE KEY UPDATE id = id;

-- Seed Sample Sales (for testing POS and dashboard)
INSERT INTO sales (organization_id, id, bill_number, customer_id, customer_name, sale_date, payment_mode, total, discount, cgst, sgst, igst, grand_total, created_at)
VALUES
-- Org 1 Sales
(1, 1, 'SALE-2024-001', 1, 'Anbu Rice Mart', DATE_SUB(NOW(), INTERVAL 2 DAY), 'CREDIT', 11000.0, 500.0, 262.5, 262.5, 0.0, 11025.0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 2, 'SALE-2024-002', NULL, 'Cash Customer', DATE_SUB(NOW(), INTERVAL 1 DAY), 'CASH', 2400.0, 0.0, 60.0, 60.0, 0.0, 2520.0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 3, 'SALE-2024-003', 4, 'Selvam Supermarket', DATE_SUB(NOW(), INTERVAL 1 DAY), 'UPI', 8700.0, 200.0, 212.5, 212.5, 0.0, 8925.0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 4, 'SALE-2024-004', 3, 'Lakshmi Traders', NOW(), 'CREDIT', 5150.0, 0.0, 128.75, 128.75, 0.0, 5407.5, NOW()),
(1, 5, 'SALE-2024-005', NULL, 'Cash Customer', NOW(), 'CASH', 1300.0, 0.0, 32.5, 32.5, 0.0, 1365.0, NOW()),

-- Org 2 Sales
(2, 101, 'SALE-ORG2-001', 101, 'Karthik General Store', DATE_SUB(NOW(), INTERVAL 2 DAY), 'CREDIT', 4500.0, 100.0, 105.0, 105.0, 0.0, 4610.0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 102, 'SALE-ORG2-002', NULL, 'Cash Customer', DATE_SUB(NOW(), INTERVAL 1 DAY), 'UPI', 1250.0, 0.0, 31.25, 31.25, 0.0, 1312.5, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE id = id;

-- Seed Sale Items
INSERT INTO sales_items (organization_id, id, sale_id, product_id, quantity, price)
VALUES
-- Org 1 Sale Items
(1, 1, 1, 1, 3, 2400.0),
(1, 2, 1, 2, 2, 1900.0),
(1, 3, 2, 3, 2, 1200.0),
(1, 4, 3, 4, 5, 1550.0),
(1, 5, 3, 10, 2, 800.0),
(1, 6, 3, 6, 1, 1350.0),
(1, 7, 4, 7, 3, 1250.0),
(1, 8, 4, 8, 1, 1150.0),
(1, 9, 4, 9, 1, 1750.0),
(1, 10, 5, 14, 1, 1300.0),

-- Org 2 Sale Items
(2, 101, 101, 101, 5, 480.0),
(2, 102, 101, 102, 2, 720.0),
(2, 103, 102, 103, 4, 165.0),
(2, 104, 102, 106, 2, 280.0)
ON DUPLICATE KEY UPDATE id = id;

-- Seed Payments (for testing payment ledger)
INSERT INTO payments (organization_id, id, reference_type, reference_id, amount, payment_mode, payment_date)
VALUES
-- Org 1 Payments
(1, 1, 'PURCHASE', 1, 24000.0, 'CASH', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1, 2, 'PURCHASE', 2, 18000.0, 'UPI', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1, 3, 'PURCHASE', 3, 8500.0, 'CASH', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 4, 'PURCHASE', 4, 29000.0, 'UPI', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 5, 'SALE', 1, 11025.0, 'CREDIT', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 6, 'SALE', 2, 2520.0, 'CASH', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 7, 'SALE', 3, 8925.0, 'UPI', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 8, 'SALE', 4, 5407.5, 'CREDIT', NOW()),
(1, 9, 'SALE', 5, 1365.0, 'CASH', NOW()),

-- Org 2 Payments
(2, 101, 'PURCHASE', 101, 18500.0, 'UPI', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 102, 'SALE', 101, 4610.0, 'CREDIT', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 103, 'SALE', 102, 1312.5, 'UPI', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE id = id;

