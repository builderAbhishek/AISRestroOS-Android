-- ============================================================================
-- AISRestroOS: RESET ALL ORDERS & INITIALIZE SEQUENCES FOR NEW RESTAURANT
-- Target Database: u328317103_restro (Hostinger phpMyAdmin / MariaDB)
-- Description:
--   Deletes all old test orders, order items, kitchen tickets, and payments.
--   Resets the sequence counters to 0 so the first new order will start at:
--     - Bill / Invoice Number: "INV-01", "INV-02", "INV-03"...
--     - KOT Number:           "KOT-01", "KOT-02", "KOT-03"...
-- ============================================================================

-- Disable foreign key checks temporarily for clean cascade deletion
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Wipe all order line items
TRUNCATE TABLE order_items;

-- 2. Wipe all kitchen order ticket items
TRUNCATE TABLE kot_items;

-- 3. Wipe all kitchen order tickets
TRUNCATE TABLE kot_tickets;

-- 4. Wipe all payments
TRUNCATE TABLE payments;

-- 5. Wipe all orders
TRUNCATE TABLE orders;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- 6. Reset restaurant sequence counters so next number is 1 (INV-01 / KOT-01)
UPDATE restaurant_settings 
SET last_bill_number = 0, 
    last_kot_number = 0, 
    bill_prefix = 'INV-',
    kot_prefix = 'KOT-',
    updated_at = CURRENT_TIMESTAMP 
WHERE key_name = 'sequence' OR id > 0;

-- Done! Verify counts:
SELECT 'orders' AS tbl, COUNT(*) AS remaining_count FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'kot_tickets', COUNT(*) FROM kot_tickets
UNION ALL
SELECT 'restaurant_settings_sequences', COUNT(*) FROM restaurant_settings WHERE last_bill_number = 0;
