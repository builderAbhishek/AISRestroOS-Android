-- ============================================================================
-- AISRestroOS POST-MIGRATION VALIDATION SUITE (117 EXPLICIT CHECKS)
-- ============================================================================
-- Target Database: u328317103_restro
-- Target Environment: Hostinger MariaDB 11.8+ / MariaDB 10.5+
--
-- STRICT PASS/FAIL CRITERIA:
--  - Every check returns an explicit 'PASS' or 'FAIL'.
--  - Missing entities, columns, indexes, foreign keys, or sequence rows ALWAYS return 'FAIL'.
--  - Any orphan record, duplicate bill/KOT, or table count mismatch returns 'FAIL'.
--
-- TOTAL AUDITED CHECKS: 117 CHECKS ACROSS 8 SECTIONS
--   Section 1: Pre-existing Record Integrity (5 checks)
--   Section 2: Required Columns Presence (57 checks)
--   Section 3: Required Performance Indexes (23 checks)
--   Section 4: Required Foreign Keys (24 checks)
--   Section 5: Sequence Settings Row (1 check)
--   Section 6: Duplicate Number Checks (2 checks)
--   Section 7: Orphan Record Checks (4 checks)
--   Section 8: Exact Table Count (1 check)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- SECTION 1: PRODUCTION RECORD INTEGRITY (5 CHECKS)
-- Guaranteed to return FAIL if the record is missing or fields are corrupted
-- ----------------------------------------------------------------------------

-- Check 1.1: Verify User 1 (Abhishek Chaudhary) preserved
SELECT 
  'CHECK 1.1: Primary User Preservation' AS check_name,
  COALESCE(u.email, 'MISSING') AS actual_email,
  CASE 
    WHEN u.id IS NOT NULL 
     AND u.email = 'luckysingh955416@gmail.com' 
     AND u.password LIKE '$2y$%' 
     AND u.role = 'owner'
    THEN 'PASS' ELSE 'FAIL' 
  END AS status
FROM (SELECT 1 AS required_user_id) req
LEFT JOIN `users` u ON u.id = req.required_user_id;

-- Check 1.2: Verify Restaurant preservation and owner_id backfill
SELECT 
  'CHECK 1.2: Restaurant & Owner Link' AS check_name,
  COALESCE(r.id, 'MISSING') AS actual_id,
  CASE 
    WHEN r.id IS NOT NULL 
     AND r.owner_id = 1 
     AND r.currency = 'INR'
    THEN 'PASS' ELSE 'FAIL' 
  END AS status
FROM (SELECT '01a207f7-8342-7368-b5de-2019baf99da4' AS required_id) req
LEFT JOIN `restaurants` r ON r.id = req.required_id;

-- Check 1.3: Verify Branch preservation and is_main backfill
SELECT 
  'CHECK 1.3: Main Branch Flag' AS check_name,
  COALESCE(b.id, 'MISSING') AS actual_id,
  CASE 
    WHEN b.id IS NOT NULL 
     AND b.is_main = 1 
     AND b.is_main_branch = 1
    THEN 'PASS' ELSE 'FAIL' 
  END AS status
FROM (SELECT '01a207f7-8344-7360-8652-4ad52c3e6e38' AS required_id) req
LEFT JOIN `branches` b ON b.id = req.required_id;

-- Check 1.4: Verify Restaurant Membership preservation and status backfill
SELECT 
  'CHECK 1.4: Membership Status & Role' AS check_name,
  COALESCE(rm.id, 'MISSING') AS actual_id,
  CASE 
    WHEN rm.id IS NOT NULL 
     AND rm.user_id = 1 
     AND rm.status = 'active'
     AND rm.is_active = 1
    THEN 'PASS' ELSE 'FAIL' 
  END AS status
FROM (SELECT '01a207f7-8345-7568-9bbf-70bb22afffbd' AS required_id) req
LEFT JOIN `restaurant_memberships` rm ON rm.id = req.required_id;

-- Check 1.5: Verify Personal Access Tokens preserved
SELECT 
  'CHECK 1.5: Personal Access Tokens' AS check_name,
  COUNT(t.id) AS token_count,
  CASE WHEN COUNT(t.id) >= 2 THEN 'PASS' ELSE 'FAIL' END AS status
FROM (SELECT 1 AS required_user_id) req
LEFT JOIN `personal_access_tokens` t ON t.tokenable_id = req.required_user_id;

-- ----------------------------------------------------------------------------
-- SECTION 2: REQUIRED COLUMNS PRESENCE (57 CHECKS)
-- Explicit derived list; missing columns produce explicit 'FAIL'
-- ----------------------------------------------------------------------------

SELECT 
  CONCAT('CHECK 2.', LPAD(@col_idx := @col_idx + 1, 2, '0'), ': Column [', e.table_name, '.', e.column_name, ']') AS check_name,
  e.table_name,
  e.column_name,
  CASE WHEN c.column_name IS NOT NULL THEN 'PASS' ELSE 'FAIL' END AS status
FROM (SELECT @col_idx := 0) init_var, (
  SELECT 'users' AS table_name, 'role' AS column_name UNION ALL
  SELECT 'restaurants', 'owner_id' UNION ALL
  SELECT 'restaurants', 'address' UNION ALL
  SELECT 'restaurants', 'currency' UNION ALL
  SELECT 'branches', 'is_main' UNION ALL
  SELECT 'branches', 'is_main_branch' UNION ALL
  SELECT 'restaurant_memberships', 'status' UNION ALL
  SELECT 'customers', 'email' UNION ALL
  SELECT 'menu_items', 'category_id' UNION ALL
  SELECT 'menu_items', 'is_veg' UNION ALL
  SELECT 'tables', 'is_occupied' UNION ALL
  SELECT 'orders', 'invoice_number' UNION ALL
  SELECT 'orders', 'payment_status' UNION ALL
  SELECT 'orders', 'payment_method' UNION ALL
  SELECT 'orders', 'completed_at' UNION ALL
  SELECT 'order_items', 'order_uuid' UNION ALL
  SELECT 'order_items', 'unit_price' UNION ALL
  SELECT 'order_items', 'total_price' UNION ALL
  SELECT 'order_items', 'price' UNION ALL
  SELECT 'order_items', 'total' UNION ALL
  SELECT 'kot_tickets', 'order_uuid' UNION ALL
  SELECT 'kot_tickets', 'kot_number' UNION ALL
  SELECT 'kot_tickets', 'ticket_number' UNION ALL
  SELECT 'kot_tickets', 'reprint_count' UNION ALL
  SELECT 'kot_items', 'kot_id' UNION ALL
  SELECT 'kot_items', 'kot_ticket_id' UNION ALL
  SELECT 'payments', 'order_uuid' UNION ALL
  SELECT 'payments', 'transaction_reference' UNION ALL
  SELECT 'payments', 'reference_number' UNION ALL
  SELECT 'payments', 'status' UNION ALL
  SELECT 'inventory_items', 'name' UNION ALL
  SELECT 'inventory_items', 'current_stock' UNION ALL
  SELECT 'inventory_items', 'unit' UNION ALL
  SELECT 'inventory_items', 'min_stock_alert' UNION ALL
  SELECT 'inventory_items', 'low_stock_alert' UNION ALL
  SELECT 'inventory_items', 'cost_per_unit' UNION ALL
  SELECT 'stock_movements', 'quantity' UNION ALL
  SELECT 'stock_movements', 'reason' UNION ALL
  SELECT 'stock_movements', 'reference_id' UNION ALL
  SELECT 'devices', 'device_type' UNION ALL
  SELECT 'devices', 'mac_address' UNION ALL
  SELECT 'devices', 'paper_width' UNION ALL
  SELECT 'devices', 'is_active' UNION ALL
  SELECT 'subscription_plans', 'features' UNION ALL
  SELECT 'subscription_plans', 'is_active' UNION ALL
  SELECT 'subscriptions', 'plan_id' UNION ALL
  SELECT 'subscriptions', 'expires_at' UNION ALL
  SELECT 'sync_tombstones', 'created_at' UNION ALL
  SELECT 'idempotency_keys', 'restaurant_id' UNION ALL
  SELECT 'idempotency_keys', 'key_value' UNION ALL
  SELECT 'idempotency_keys', 'resource_type' UNION ALL
  SELECT 'restaurant_settings', 'bill_prefix' UNION ALL
  SELECT 'restaurant_settings', 'last_bill_number' UNION ALL
  SELECT 'restaurant_settings', 'kot_prefix' UNION ALL
  SELECT 'restaurant_settings', 'last_kot_number' UNION ALL
  SELECT 'restaurant_settings', 'tax_percentage' UNION ALL
  SELECT 'restaurant_settings', 'service_charge_percentage'
) e
LEFT JOIN information_schema.columns c
  ON c.table_schema = DATABASE()
 AND c.table_name = e.table_name
 AND c.column_name = e.column_name
ORDER BY e.table_name, e.column_name;

-- ----------------------------------------------------------------------------
-- SECTION 3: REQUIRED PERFORMANCE INDEXES (23 CHECKS)
-- Explicit derived list; missing indexes produce explicit 'FAIL'
-- ----------------------------------------------------------------------------

SELECT 
  CONCAT('CHECK 3.', LPAD(@idx_count := @idx_count + 1, 2, '0'), ': Index [', e.table_name, '.', e.index_name, ']') AS check_name,
  e.table_name,
  e.index_name,
  CASE WHEN s.index_name IS NOT NULL THEN 'PASS' ELSE 'FAIL' END AS status
FROM (SELECT @idx_count := 0) init_var, (
  SELECT 'users' AS table_name, 'idx_users_email' AS index_name UNION ALL
  SELECT 'restaurants', 'idx_restro_owner' UNION ALL
  SELECT 'branches', 'idx_branches_restro' UNION ALL
  SELECT 'restaurant_memberships', 'idx_memberships_restro' UNION ALL
  SELECT 'categories', 'idx_cat_sync' UNION ALL
  SELECT 'customers', 'idx_cust_sync' UNION ALL
  SELECT 'menu_items', 'idx_menu_sync' UNION ALL
  SELECT 'menu_items', 'idx_menu_cat_id' UNION ALL
  SELECT 'tables', 'idx_tables_restro' UNION ALL
  SELECT 'orders', 'idx_orders_sync' UNION ALL
  SELECT 'orders', 'idx_orders_inv' UNION ALL
  SELECT 'order_items', 'idx_order_items_uuid' UNION ALL
  SELECT 'kot_tickets', 'idx_kot_order_uuid' UNION ALL
  SELECT 'kot_items', 'idx_kot_items_parent' UNION ALL
  SELECT 'payments', 'idx_payments_order_uuid' UNION ALL
  SELECT 'inventory_items', 'idx_inventory_restro' UNION ALL
  SELECT 'stock_movements', 'idx_stock_item' UNION ALL
  SELECT 'devices', 'idx_devices_restro' UNION ALL
  SELECT 'subscriptions', 'idx_subs_restro' UNION ALL
  SELECT 'sync_tombstones', 'idx_tomb_sync' UNION ALL
  SELECT 'idempotency_keys', 'idx_idemp_key' UNION ALL
  SELECT 'restaurant_settings', 'idx_settings_restro' UNION ALL
  SELECT 'audit_logs', 'idx_audit_restro'
) e
LEFT JOIN (
  SELECT DISTINCT table_name, index_name
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
) s ON s.table_name = e.table_name AND s.index_name = e.index_name
ORDER BY e.table_name, e.index_name;

-- ----------------------------------------------------------------------------
-- SECTION 4: REQUIRED FOREIGN KEYS (24 CHECKS)
-- Complete list of all 24 production relational foreign keys
-- Missing foreign keys produce explicit 'FAIL'
-- ----------------------------------------------------------------------------

SELECT 
  CONCAT('CHECK 4.', LPAD(@fk_count := @fk_count + 1, 2, '0'), ': FK [', e.table_name, '.', e.constraint_name, ']') AS check_name,
  e.table_name,
  e.constraint_name,
  CASE WHEN k.constraint_name IS NOT NULL THEN 'PASS' ELSE 'FAIL' END AS status
FROM (SELECT @fk_count := 0) init_var, (
  SELECT 'audit_logs' AS table_name, 'audit_logs_restaurant_id_foreign' AS constraint_name UNION ALL
  SELECT 'branches', 'branches_restaurant_id_foreign' UNION ALL
  SELECT 'categories', 'categories_restaurant_id_foreign' UNION ALL
  SELECT 'customers', 'customers_restaurant_id_foreign' UNION ALL
  SELECT 'devices', 'devices_restaurant_id_foreign' UNION ALL
  SELECT 'inventory_items', 'inventory_items_restaurant_id_foreign' UNION ALL
  SELECT 'kot_items', 'kot_items_kot_ticket_id_foreign' UNION ALL
  SELECT 'kot_tickets', 'kot_tickets_order_id_foreign' UNION ALL
  SELECT 'kot_tickets', 'kot_tickets_restaurant_id_foreign' UNION ALL
  SELECT 'menu_items', 'menu_items_restaurant_id_foreign' UNION ALL
  SELECT 'orders', 'orders_restaurant_id_foreign' UNION ALL
  SELECT 'order_items', 'order_items_order_id_foreign' UNION ALL
  SELECT 'payments', 'payments_order_id_foreign' UNION ALL
  SELECT 'payments', 'payments_restaurant_id_foreign' UNION ALL
  SELECT 'refunds', 'refunds_order_id_foreign' UNION ALL
  SELECT 'refunds', 'refunds_payment_id_foreign' UNION ALL
  SELECT 'restaurant_memberships', 'restaurant_memberships_branch_id_foreign' UNION ALL
  SELECT 'restaurant_memberships', 'restaurant_memberships_restaurant_id_foreign' UNION ALL
  SELECT 'restaurant_memberships', 'restaurant_memberships_user_id_foreign' UNION ALL
  SELECT 'restaurant_settings', 'restaurant_settings_restaurant_id_foreign' UNION ALL
  SELECT 'stock_movements', 'stock_movements_inventory_item_id_foreign' UNION ALL
  SELECT 'subscriptions', 'subscriptions_restaurant_id_foreign' UNION ALL
  SELECT 'subscriptions', 'subscriptions_subscription_plan_id_foreign' UNION ALL
  SELECT 'tables', 'tables_restaurant_id_foreign'
) e
LEFT JOIN (
  SELECT DISTINCT table_name, constraint_name
  FROM information_schema.key_column_usage
  WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL
) k ON k.table_name = e.table_name AND k.constraint_name = e.constraint_name
ORDER BY e.table_name, e.constraint_name;

-- ----------------------------------------------------------------------------
-- SECTION 5: SEQUENCE SETTINGS ROW PRESENCE (1 CHECK)
-- Guaranteed to return FAIL if the sequence row is missing
-- ----------------------------------------------------------------------------

SELECT 
  'CHECK 5.1: Sequence Counters Initialized' AS check_name,
  COALESCE(rs.restaurant_id, 'MISSING') AS restaurant_id,
  rs.bill_prefix, rs.last_bill_number, rs.kot_prefix, rs.last_kot_number,
  CASE 
    WHEN rs.id IS NOT NULL 
     AND rs.bill_prefix IS NOT NULL 
     AND rs.last_bill_number >= 0 
     AND rs.kot_prefix IS NOT NULL 
     AND rs.last_kot_number >= 0
    THEN 'PASS' ELSE 'FAIL' 
  END AS status
FROM (SELECT '01a207f7-8342-7368-b5de-2019baf99da4' AS required_restro_id) req
LEFT JOIN `restaurant_settings` rs 
  ON rs.restaurant_id = req.required_restro_id AND rs.key_name = 'sequence';

-- ----------------------------------------------------------------------------
-- SECTION 6: DUPLICATE BILL & KOT NUMBER CHECKS (2 CHECKS)
-- ----------------------------------------------------------------------------

SELECT 
  'CHECK 6.1: Duplicate Invoice Numbers' AS check_name, 
  COUNT(*) AS duplicate_count,
  CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END AS status
FROM (
  SELECT invoice_number, COUNT(*) AS cnt 
  FROM `orders` 
  WHERE invoice_number IS NOT NULL AND invoice_number != '' 
  GROUP BY restaurant_id, invoice_number 
  HAVING cnt > 1
) dupes;

SELECT 
  'CHECK 6.2: Duplicate KOT Numbers' AS check_name, 
  COUNT(*) AS duplicate_count,
  CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END AS status
FROM (
  SELECT kot_number, COUNT(*) AS cnt 
  FROM `kot_tickets` 
  WHERE kot_number IS NOT NULL AND kot_number != '' 
  GROUP BY restaurant_id, kot_number 
  HAVING cnt > 1
) dupes;

-- ----------------------------------------------------------------------------
-- SECTION 7: ORPHAN RECORD CHECKS (4 CHECKS)
-- ----------------------------------------------------------------------------

SELECT 
  'CHECK 7.1: Orphan Order Items (Missing Order)' AS check_name,
  COUNT(oi.id) AS orphan_count,
  CASE WHEN COUNT(oi.id) = 0 THEN 'PASS' ELSE 'FAIL' END AS status
FROM `order_items` oi
LEFT JOIN `orders` o ON oi.order_id = o.id
WHERE o.id IS NULL;

SELECT 
  'CHECK 7.2: Orphan KOT Tickets (Missing Order)' AS check_name,
  COUNT(kt.id) AS orphan_count,
  CASE WHEN COUNT(kt.id) = 0 THEN 'PASS' ELSE 'FAIL' END AS status
FROM `kot_tickets` kt
LEFT JOIN `orders` o ON kt.order_id = o.id
WHERE o.id IS NULL;

SELECT 
  'CHECK 7.3: Orphan KOT Items (Missing KOT Ticket)' AS check_name,
  COUNT(ki.id) AS orphan_count,
  CASE WHEN COUNT(ki.id) = 0 THEN 'PASS' ELSE 'FAIL' END AS status
FROM `kot_items` ki
LEFT JOIN `kot_tickets` kt ON ki.kot_ticket_id = kt.id
WHERE kt.id IS NULL;

SELECT 
  'CHECK 7.4: Orphan Payments (Missing Order)' AS check_name,
  COUNT(p.id) AS orphan_count,
  CASE WHEN COUNT(p.id) = 0 THEN 'PASS' ELSE 'FAIL' END AS status
FROM `payments` p
LEFT JOIN `orders` o ON p.order_id = o.id
WHERE o.id IS NULL;

-- ----------------------------------------------------------------------------
-- SECTION 8: TOTAL PRODUCTION TABLE COUNT (1 CHECK)
-- ----------------------------------------------------------------------------

SELECT 
  'CHECK 8.1: Exact Production Table Count' AS check_name,
  COUNT(DISTINCT table_name) AS actual_count,
  28 AS expected_count,
  CASE WHEN COUNT(DISTINCT table_name) = 28 THEN 'PASS' ELSE 'FAIL' END AS status
FROM information_schema.tables
WHERE table_schema = DATABASE();
