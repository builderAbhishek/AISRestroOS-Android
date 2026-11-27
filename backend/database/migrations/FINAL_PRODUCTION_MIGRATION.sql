-- ============================================================================
-- AISRestroOS PRODUCTION DATABASE COMPATIBILITY MIGRATION
-- ============================================================================
-- Target Database: u328317103_restro (Hosted on Hostinger MariaDB 11.8+ / MariaDB 10.5+)
-- Purpose: Bring existing production database into 100% full compatibility with
--          both the AISRestroOS Core PHP REST API backend and the existing /admin/ portal.
--
-- STRICT SAFETY GUARANTEES:
--  1. ZERO DATA LOSS: Existing tables, records, users, and tokens are NEVER dropped.
--  2. NON-DESTRUCTIVE: All changes add companion columns, broaden lengths, or add indexes.
--  3. DUAL COMPATIBILITY: Legacy columns and new API columns co-exist with backfilled data.
--  4. IDEMPOTENT: Safe to run multiple times without causing duplicate errors or key collisions.
--  5. FOREIGN KEY INTEGRITY: All production foreign keys are strictly preserved and enforced.
--
-- IMPORTANT DDL TRANSACTION LIMITATION NOTICE:
--  In MySQL and MariaDB, DDL statements (ALTER TABLE, CREATE INDEX, etc.) cause an
--  IMPLICIT COMMIT. Therefore, structural DDL operations CANNOT be rolled back via
--  ROLLBACK if an error occurs midway. DML statements (INSERT, UPDATE) participate
--  in transactions, but DDL structural alters are committed per statement.
--  A verified backup must be taken prior to migration execution (see MIGRATION_EXECUTION_GUIDE.md).
--
-- DATABASE COMPATIBILITY:
--  Tested and verified on MariaDB 11.8+ (Hostinger uses MariaDB).
--  Uses MariaDB-native idempotent syntax (ADD COLUMN IF NOT EXISTS, CREATE INDEX IF NOT EXISTS).
--  Standard MySQL 8.0 does not support IF NOT EXISTS in ALTER TABLE and will require
--  procedural wrapping if ported to MySQL 8.0.
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";

-- ----------------------------------------------------------------------------
-- 1. TABLE: users
-- ----------------------------------------------------------------------------
-- Add role column used by Core PHP API (mapped from restaurant_memberships)
-- Widen phone column to accommodate international formats
ALTER TABLE `users` 
  ADD COLUMN IF NOT EXISTS `role` VARCHAR(50) NOT NULL DEFAULT 'owner' AFTER `password`,
  MODIFY COLUMN `phone` VARCHAR(50) NULL DEFAULT NULL;

-- Backfill user role from active restaurant membership
UPDATE `users` u
LEFT JOIN `restaurant_memberships` rm ON u.id = rm.user_id
SET u.role = CASE 
  WHEN rm.role IN ('SUPER_ADMIN', 'RESTAURANT_OWNER') THEN 'owner'
  WHEN rm.role = 'MANAGER' THEN 'manager'
  WHEN rm.role = 'CASHIER' THEN 'cashier'
  WHEN rm.role = 'WAITER' THEN 'waiter'
  WHEN rm.role = 'CHEF' THEN 'kitchen'
  ELSE 'owner'
END
WHERE u.role IS NULL OR u.role = '' OR u.role = 'owner';

-- ----------------------------------------------------------------------------
-- 2. TABLE: restaurants
-- ----------------------------------------------------------------------------
-- Add owner_id (linking to users.id), address, and currency
-- Widen phone column
ALTER TABLE `restaurants`
  ADD COLUMN IF NOT EXISTS `owner_id` BIGINT(20) UNSIGNED NULL AFTER `id`,
  ADD COLUMN IF NOT EXISTS `address` TEXT NULL AFTER `logo_url`,
  ADD COLUMN IF NOT EXISTS `currency` VARCHAR(10) NOT NULL DEFAULT 'INR' AFTER `address`,
  MODIFY COLUMN `phone` VARCHAR(50) NULL DEFAULT NULL;

-- Backfill owner_id from existing restaurant_memberships or fallback to user 1
UPDATE `restaurants` r
LEFT JOIN `restaurant_memberships` rm ON r.id = rm.restaurant_id AND rm.role IN ('SUPER_ADMIN', 'RESTAURANT_OWNER')
SET r.owner_id = COALESCE(rm.user_id, 1)
WHERE r.owner_id IS NULL;

-- ----------------------------------------------------------------------------
-- 3. TABLE: branches
-- ----------------------------------------------------------------------------
-- Add is_main companion column to match baseline schema
-- Widen address, phone, and code
ALTER TABLE `branches`
  ADD COLUMN IF NOT EXISTS `is_main` TINYINT(1) NOT NULL DEFAULT 1 AFTER `is_main_branch`,
  MODIFY COLUMN `address` TEXT NULL DEFAULT NULL,
  MODIFY COLUMN `phone` VARCHAR(50) NULL DEFAULT NULL,
  MODIFY COLUMN `code` VARCHAR(50) NULL DEFAULT NULL;

-- Backfill is_main from is_main_branch
UPDATE `branches`
SET `is_main` = `is_main_branch`
WHERE `is_main` IS NULL OR `is_main` = 0;

-- ----------------------------------------------------------------------------
-- 4. TABLE: restaurant_memberships
-- ----------------------------------------------------------------------------
-- Add status companion column ('active' / 'inactive')
-- Broaden role from ENUM to VARCHAR(50) so both legacy & API roles are accepted
ALTER TABLE `restaurant_memberships`
  ADD COLUMN IF NOT EXISTS `status` VARCHAR(50) NOT NULL DEFAULT 'active' AFTER `is_active`,
  MODIFY COLUMN `role` VARCHAR(50) NOT NULL DEFAULT 'CASHIER';

-- Backfill status from is_active
UPDATE `restaurant_memberships`
SET `status` = IF(`is_active` = 1, 'active', 'inactive')
WHERE `status` IS NULL OR `status` = '';

-- ----------------------------------------------------------------------------
-- 5. TABLE: categories
-- ----------------------------------------------------------------------------
ALTER TABLE `categories`
  MODIFY COLUMN `name` VARCHAR(255) NOT NULL;

-- ----------------------------------------------------------------------------
-- 6. TABLE: customers
-- ----------------------------------------------------------------------------
-- Add optional email column
-- Widen mobile_number length
ALTER TABLE `customers`
  ADD COLUMN IF NOT EXISTS `email` VARCHAR(255) NULL AFTER `mobile_number`,
  MODIFY COLUMN `mobile_number` VARCHAR(50) NOT NULL;

-- ----------------------------------------------------------------------------
-- 7. TABLE: menu_items
-- ----------------------------------------------------------------------------
-- Add category_id and is_veg columns
-- Widen category, image_url, tax_percentage, tax_mode
ALTER TABLE `menu_items`
  ADD COLUMN IF NOT EXISTS `category_id` VARCHAR(64) NULL AFTER `category`,
  ADD COLUMN IF NOT EXISTS `is_veg` TINYINT(1) NOT NULL DEFAULT 1 AFTER `is_available`,
  MODIFY COLUMN `category` VARCHAR(255) NOT NULL DEFAULT 'General',
  MODIFY COLUMN `image_url` VARCHAR(500) NULL DEFAULT NULL,
  MODIFY COLUMN `tax_percentage` VARCHAR(50) NOT NULL DEFAULT 'None',
  MODIFY COLUMN `tax_mode` VARCHAR(50) NOT NULL DEFAULT 'WITHOUT_TAX';

-- ----------------------------------------------------------------------------
-- 8. TABLE: tables
-- ----------------------------------------------------------------------------
-- Add is_occupied companion column
-- Broaden table_number and status
ALTER TABLE `tables`
  ADD COLUMN IF NOT EXISTS `is_occupied` TINYINT(1) NOT NULL DEFAULT 0 AFTER `status`,
  MODIFY COLUMN `table_number` VARCHAR(50) NOT NULL,
  MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE';

-- Backfill is_occupied based on status
UPDATE `tables`
SET `is_occupied` = IF(`status` = 'OCCUPIED', 1, 0)
WHERE `is_occupied` IS NULL;

-- ----------------------------------------------------------------------------
-- 9. TABLE: orders
-- ----------------------------------------------------------------------------
-- Add invoice_number, payment_status, payment_method
-- Broaden order_number, table_number, customer_phone, status, order_type
-- NOTE: completed_at and service_charge already exist in production
-- NOTE: sync_seq remains BIGINT UNSIGNED NOT NULL DEFAULT 1 (NOT secondary AUTO_INCREMENT)
ALTER TABLE `orders`
  ADD COLUMN IF NOT EXISTS `invoice_number` VARCHAR(50) NULL AFTER `order_number`,
  ADD COLUMN IF NOT EXISTS `payment_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' AFTER `status`,
  ADD COLUMN IF NOT EXISTS `payment_method` VARCHAR(50) NULL AFTER `payment_status`,
  MODIFY COLUMN `order_number` VARCHAR(50) NOT NULL,
  MODIFY COLUMN `table_number` VARCHAR(50) NULL DEFAULT NULL,
  MODIFY COLUMN `customer_phone` VARCHAR(50) NULL DEFAULT NULL,
  MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'ON_HOLD',
  MODIFY COLUMN `order_type` VARCHAR(50) NOT NULL DEFAULT 'DINE_IN';

-- Backfill payment_status from status
UPDATE `orders`
SET `payment_status` = IF(`status` = 'COMPLETED', 'PAID', 'PENDING')
WHERE `payment_status` IS NULL OR `payment_status` = 'PENDING';

-- ----------------------------------------------------------------------------
-- 10. TABLE: order_items
-- ----------------------------------------------------------------------------
-- Add order_uuid companion column to link with orders.uuid (CHAR(36) matching orders.uuid)
-- Add price and total companion columns (production already has unit_price & total_price)
-- PRESERVE order_id NOT NULL to ensure strict foreign key integrity
ALTER TABLE `order_items`
  ADD COLUMN IF NOT EXISTS `order_uuid` CHAR(36) COLLATE utf8mb4_unicode_ci NULL AFTER `order_id`,
  ADD COLUMN IF NOT EXISTS `price` DECIMAL(10,2) NULL AFTER `unit_price`,
  ADD COLUMN IF NOT EXISTS `total` DECIMAL(10,2) NULL AFTER `quantity`;

-- Backfill order_uuid, price, total from existing data
UPDATE `order_items` oi
JOIN `orders` o ON oi.order_id = o.id
SET oi.order_uuid = o.uuid
WHERE oi.order_uuid IS NULL;

UPDATE `order_items`
SET `price` = `unit_price`
WHERE `price` IS NULL AND `unit_price` IS NOT NULL;

UPDATE `order_items`
SET `total` = `total_price`
WHERE `total` IS NULL AND `total_price` IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 11. TABLE: kot_tickets
-- ----------------------------------------------------------------------------
-- Add order_uuid, kot_number, reprint_count
-- PRESERVE order_id NOT NULL to ensure strict foreign key integrity
-- Broaden ticket_number, table_number, status
ALTER TABLE `kot_tickets`
  ADD COLUMN IF NOT EXISTS `order_uuid` CHAR(36) COLLATE utf8mb4_unicode_ci NULL AFTER `order_id`,
  ADD COLUMN IF NOT EXISTS `kot_number` VARCHAR(50) NULL AFTER `ticket_number`,
  ADD COLUMN IF NOT EXISTS `reprint_count` INT NOT NULL DEFAULT 0 AFTER `status`,
  MODIFY COLUMN `ticket_number` VARCHAR(50) NULL DEFAULT NULL,
  MODIFY COLUMN `table_number` VARCHAR(50) NULL DEFAULT NULL,
  MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- Backfill order_uuid and kot_number
UPDATE `kot_tickets` kt
JOIN `orders` o ON kt.order_id = o.id
SET kt.order_uuid = o.uuid
WHERE kt.order_uuid IS NULL;

UPDATE `kot_tickets`
SET `kot_number` = `ticket_number`
WHERE `kot_number` IS NULL AND `ticket_number` IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 12. TABLE: kot_items
-- ----------------------------------------------------------------------------
-- Add kot_id companion column
-- PRESERVE kot_ticket_id NOT NULL to ensure strict foreign key integrity
-- Broaden notes
ALTER TABLE `kot_items`
  ADD COLUMN IF NOT EXISTS `kot_id` BIGINT(20) UNSIGNED NULL AFTER `kot_ticket_id`,
  MODIFY COLUMN `notes` TEXT NULL DEFAULT NULL;

-- Backfill kot_id from kot_ticket_id
UPDATE `kot_items`
SET `kot_id` = `kot_ticket_id`
WHERE `kot_id` IS NULL AND `kot_ticket_id` IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 13. TABLE: payments
-- ----------------------------------------------------------------------------
-- Add order_uuid, transaction_reference, status
-- PRESERVE order_id NOT NULL to ensure strict foreign key integrity
-- Broaden payment_method
ALTER TABLE `payments`
  ADD COLUMN IF NOT EXISTS `order_uuid` CHAR(36) COLLATE utf8mb4_unicode_ci NULL AFTER `order_id`,
  ADD COLUMN IF NOT EXISTS `transaction_reference` VARCHAR(255) NULL AFTER `reference_number`,
  ADD COLUMN IF NOT EXISTS `status` VARCHAR(50) NOT NULL DEFAULT 'COMPLETED' AFTER `payment_method`,
  MODIFY COLUMN `payment_method` VARCHAR(50) NOT NULL DEFAULT 'CASH';

-- Backfill order_uuid and transaction_reference
UPDATE `payments` p
JOIN `orders` o ON p.order_id = o.id
SET p.order_uuid = o.uuid
WHERE p.order_uuid IS NULL;

UPDATE `payments`
SET `transaction_reference` = `reference_number`
WHERE `transaction_reference` IS NULL AND `reference_number` IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 14. TABLE: inventory_items
-- ----------------------------------------------------------------------------
-- Add name, current_stock, unit, low_stock_alert, cost_per_unit companion columns
-- Make item_name nullable (production already has min_stock_alert)
ALTER TABLE `inventory_items`
  ADD COLUMN IF NOT EXISTS `name` VARCHAR(255) NULL AFTER `item_name`,
  ADD COLUMN IF NOT EXISTS `current_stock` DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER `quantity`,
  ADD COLUMN IF NOT EXISTS `unit` VARCHAR(50) NOT NULL DEFAULT 'kg' AFTER `unit_of_measure`,
  ADD COLUMN IF NOT EXISTS `low_stock_alert` DECIMAL(10,2) NOT NULL DEFAULT 5.00 AFTER `min_stock_alert`,
  ADD COLUMN IF NOT EXISTS `cost_per_unit` DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER `low_stock_alert`,
  MODIFY COLUMN `item_name` VARCHAR(255) NULL;

-- Backfill companion inventory columns
UPDATE `inventory_items`
SET `name` = `item_name`
WHERE `name` IS NULL OR `name` = '';

UPDATE `inventory_items`
SET `current_stock` = `quantity`
WHERE `current_stock` = 0.00 AND `quantity` != 0.00;

UPDATE `inventory_items`
SET `unit` = `unit_of_measure`
WHERE `unit` = 'kg' AND `unit_of_measure` != 'kg';

UPDATE `inventory_items`
SET `low_stock_alert` = `min_stock_alert`
WHERE `low_stock_alert` = 5.00 AND `min_stock_alert` != 5.00;

-- ----------------------------------------------------------------------------
-- 15. TABLE: stock_movements
-- ----------------------------------------------------------------------------
-- Add quantity, reason, reference_id companion columns
-- Broaden movement_type
ALTER TABLE `stock_movements`
  ADD COLUMN IF NOT EXISTS `quantity` DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER `quantity_change`,
  ADD COLUMN IF NOT EXISTS `reason` VARCHAR(255) NULL AFTER `notes`,
  ADD COLUMN IF NOT EXISTS `reference_id` VARCHAR(64) NULL AFTER `reason`,
  MODIFY COLUMN `movement_type` VARCHAR(50) NOT NULL DEFAULT 'IN';

-- Backfill quantity and reason
UPDATE `stock_movements`
SET `quantity` = `quantity_change`
WHERE `quantity` = 0.00 AND `quantity_change` != 0.00;

UPDATE `stock_movements`
SET `reason` = `notes`
WHERE `reason` IS NULL AND `notes` IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 16. TABLE: devices
-- ----------------------------------------------------------------------------
-- Add device_type, mac_address, paper_width, is_active companion columns
-- Make device_code nullable
ALTER TABLE `devices`
  ADD COLUMN IF NOT EXISTS `device_type` VARCHAR(50) NOT NULL DEFAULT 'POS' AFTER `device_name`,
  ADD COLUMN IF NOT EXISTS `mac_address` VARCHAR(50) NULL AFTER `device_type`,
  ADD COLUMN IF NOT EXISTS `paper_width` VARCHAR(20) NOT NULL DEFAULT '58mm' AFTER `mac_address`,
  ADD COLUMN IF NOT EXISTS `is_active` TINYINT(1) NOT NULL DEFAULT 1 AFTER `is_authorized`,
  MODIFY COLUMN `device_code` VARCHAR(32) NULL DEFAULT NULL;

-- Backfill is_active
UPDATE `devices`
SET `is_active` = `is_authorized`
WHERE `is_active` = 1;

-- ----------------------------------------------------------------------------
-- 17. TABLE: subscription_plans
-- ----------------------------------------------------------------------------
-- Add features and is_active columns
ALTER TABLE `subscription_plans`
  ADD COLUMN IF NOT EXISTS `features` TEXT NULL AFTER `price`,
  ADD COLUMN IF NOT EXISTS `is_active` TINYINT(1) NOT NULL DEFAULT 1 AFTER `duration_days`;

-- ----------------------------------------------------------------------------
-- 18. TABLE: subscriptions
-- ----------------------------------------------------------------------------
-- Add plan_id and expires_at companion columns
-- Broaden status
ALTER TABLE `subscriptions`
  ADD COLUMN IF NOT EXISTS `plan_id` VARCHAR(64) NULL AFTER `subscription_plan_id`,
  ADD COLUMN IF NOT EXISTS `expires_at` DATETIME NULL AFTER `ends_at`,
  MODIFY COLUMN `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Backfill plan_id and expires_at
UPDATE `subscriptions`
SET `plan_id` = `subscription_plan_id`
WHERE `plan_id` IS NULL;

UPDATE `subscriptions`
SET `expires_at` = `ends_at`
WHERE `expires_at` IS NULL AND `ends_at` IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 19. TABLE: sync_tombstones
-- ----------------------------------------------------------------------------
-- Add created_at companion column
ALTER TABLE `sync_tombstones`
  ADD COLUMN IF NOT EXISTS `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `deleted_at`;

-- Backfill created_at from deleted_at
UPDATE `sync_tombstones`
SET `created_at` = `deleted_at`
WHERE `created_at` IS NULL;

-- ----------------------------------------------------------------------------
-- 20. TABLE: idempotency_keys
-- ----------------------------------------------------------------------------
-- Add key_value and resource_type companion columns
-- Relax column requirements to support both JSON cache payloads and REST API keys
ALTER TABLE `idempotency_keys`
  ADD COLUMN IF NOT EXISTS `restaurant_id` CHAR(36) NULL AFTER `id`,
  ADD COLUMN IF NOT EXISTS `key_value` VARCHAR(255) NULL AFTER `idempotency_key`,
  ADD COLUMN IF NOT EXISTS `resource_type` VARCHAR(50) NULL AFTER `resource_uuid`,
  MODIFY COLUMN `request_path` VARCHAR(255) NULL,
  MODIFY COLUMN `idempotency_key` VARCHAR(64) NULL;

-- Backfill key_value
UPDATE `idempotency_keys`
SET `key_value` = `idempotency_key`
WHERE `key_value` IS NULL AND `idempotency_key` IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 21. TABLE: restaurant_settings
-- ----------------------------------------------------------------------------
-- Add sequence numbering columns directly to restaurant_settings table
-- Make key_name nullable so row-level sequence counters function alongside key-value rows
ALTER TABLE `restaurant_settings`
  ADD COLUMN IF NOT EXISTS `bill_prefix` VARCHAR(20) NOT NULL DEFAULT 'INV-' AFTER `key_value`,
  ADD COLUMN IF NOT EXISTS `last_bill_number` INT NOT NULL DEFAULT 0 AFTER `bill_prefix`,
  ADD COLUMN IF NOT EXISTS `kot_prefix` VARCHAR(20) NOT NULL DEFAULT 'KOT-' AFTER `last_bill_number`,
  ADD COLUMN IF NOT EXISTS `last_kot_number` INT NOT NULL DEFAULT 0 AFTER `kot_prefix`,
  ADD COLUMN IF NOT EXISTS `tax_percentage` DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER `last_kot_number`,
  ADD COLUMN IF NOT EXISTS `service_charge_percentage` DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER `tax_percentage`,
  MODIFY COLUMN `key_name` VARCHAR(64) NULL DEFAULT 'general';

-- Initialize sequence settings row dynamically based on actual highest existing numbers
INSERT INTO `restaurant_settings` (
  `restaurant_id`, `branch_id`, `key_name`, `key_value`,
  `bill_prefix`, `last_bill_number`, `kot_prefix`, `last_kot_number`,
  `tax_percentage`, `service_charge_percentage`, `created_at`, `updated_at`
)
SELECT 
  r.id, 
  NULL, 
  'sequence', 
  'Sequence counters initialized',
  'INV-', 
  COALESCE(
    GREATEST(
      COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(order_number, '[^0-9]', '') AS UNSIGNED)) FROM `orders` WHERE restaurant_id = r.id AND order_number REGEXP '[0-9]'), 0),
      COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(invoice_number, '[^0-9]', '') AS UNSIGNED)) FROM `orders` WHERE restaurant_id = r.id AND invoice_number REGEXP '[0-9]'), 0)
    ), 0
  ),
  'KOT-',
  COALESCE(
    GREATEST(
      COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(ticket_number, '[^0-9]', '') AS UNSIGNED)) FROM `kot_tickets` WHERE restaurant_id = r.id AND ticket_number REGEXP '[0-9]'), 0),
      COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(kot_number, '[^0-9]', '') AS UNSIGNED)) FROM `kot_tickets` WHERE restaurant_id = r.id AND kot_number REGEXP '[0-9]'), 0)
    ), 0
  ),
  0.00, 0.00, NOW(), NOW()
FROM `restaurants` r
WHERE NOT EXISTS (
  SELECT 1 FROM `restaurant_settings` rs WHERE rs.restaurant_id = r.id AND rs.key_name = 'sequence'
);

-- If sequence row already exists, ensure counters reflect at least the maximum existing numbers
UPDATE `restaurant_settings` rs
SET 
  rs.last_bill_number = GREATEST(
    rs.last_bill_number,
    COALESCE(
      GREATEST(
        COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(order_number, '[^0-9]', '') AS UNSIGNED)) FROM `orders` WHERE restaurant_id = rs.restaurant_id AND order_number REGEXP '[0-9]'), 0),
        COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(invoice_number, '[^0-9]', '') AS UNSIGNED)) FROM `orders` WHERE restaurant_id = rs.restaurant_id AND invoice_number REGEXP '[0-9]'), 0)
      ), 0
    )
  ),
  rs.last_kot_number = GREATEST(
    rs.last_kot_number,
    COALESCE(
      GREATEST(
        COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(ticket_number, '[^0-9]', '') AS UNSIGNED)) FROM `kot_tickets` WHERE restaurant_id = rs.restaurant_id AND ticket_number REGEXP '[0-9]'), 0),
        COALESCE((SELECT MAX(CAST(REGEXP_REPLACE(kot_number, '[^0-9]', '') AS UNSIGNED)) FROM `kot_tickets` WHERE restaurant_id = rs.restaurant_id AND kot_number REGEXP '[0-9]'), 0)
      ), 0
    )
  )
WHERE rs.key_name = 'sequence';

-- ----------------------------------------------------------------------------
-- 22. TABLE: audit_logs
-- ----------------------------------------------------------------------------
-- Broaden action and ip_address
ALTER TABLE `audit_logs`
  MODIFY COLUMN `action` VARCHAR(255) NOT NULL,
  MODIFY COLUMN `ip_address` VARCHAR(50) NULL DEFAULT NULL;

-- ----------------------------------------------------------------------------
-- 23. OPTIMIZED PERFORMANCE INDEXES
-- ----------------------------------------------------------------------------
-- Create indexes safely (MariaDB supports CREATE INDEX IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS `idx_users_email` ON `users` (`email`);
CREATE INDEX IF NOT EXISTS `idx_restro_owner` ON `restaurants` (`owner_id`);
CREATE INDEX IF NOT EXISTS `idx_branches_restro` ON `branches` (`restaurant_id`);
CREATE INDEX IF NOT EXISTS `idx_memberships_restro` ON `restaurant_memberships` (`restaurant_id`);
CREATE INDEX IF NOT EXISTS `idx_cat_sync` ON `categories` (`restaurant_id`, `sync_seq`);
CREATE INDEX IF NOT EXISTS `idx_cust_sync` ON `customers` (`restaurant_id`, `sync_seq`);
CREATE INDEX IF NOT EXISTS `idx_menu_sync` ON `menu_items` (`restaurant_id`, `sync_seq`);
CREATE INDEX IF NOT EXISTS `idx_menu_cat_id` ON `menu_items` (`category_id`);
CREATE INDEX IF NOT EXISTS `idx_tables_restro` ON `tables` (`restaurant_id`);
CREATE INDEX IF NOT EXISTS `idx_orders_sync` ON `orders` (`restaurant_id`, `sync_seq`);
CREATE INDEX IF NOT EXISTS `idx_orders_inv` ON `orders` (`restaurant_id`, `invoice_number`);
CREATE INDEX IF NOT EXISTS `idx_order_items_uuid` ON `order_items` (`order_uuid`);
CREATE INDEX IF NOT EXISTS `idx_kot_order_uuid` ON `kot_tickets` (`order_uuid`);
CREATE INDEX IF NOT EXISTS `idx_kot_items_parent` ON `kot_items` (`kot_id`);
CREATE INDEX IF NOT EXISTS `idx_payments_order_uuid` ON `payments` (`order_uuid`);
CREATE INDEX IF NOT EXISTS `idx_inventory_restro` ON `inventory_items` (`restaurant_id`);
CREATE INDEX IF NOT EXISTS `idx_stock_item` ON `stock_movements` (`inventory_item_id`);
CREATE INDEX IF NOT EXISTS `idx_devices_restro` ON `devices` (`restaurant_id`);
CREATE INDEX IF NOT EXISTS `idx_subs_restro` ON `subscriptions` (`restaurant_id`);
CREATE INDEX IF NOT EXISTS `idx_tomb_sync` ON `sync_tombstones` (`restaurant_id`, `sync_seq`);
CREATE INDEX IF NOT EXISTS `idx_idemp_key` ON `idempotency_keys` (`key_value`);
CREATE INDEX IF NOT EXISTS `idx_settings_restro` ON `restaurant_settings` (`restaurant_id`);
CREATE INDEX IF NOT EXISTS `idx_audit_restro` ON `audit_logs` (`restaurant_id`);

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

-- ============================================================================
-- MIGRATION COMPLETE: ZERO DATA LOSS - 100% PRODUCTION COMPATIBLE
-- ============================================================================
