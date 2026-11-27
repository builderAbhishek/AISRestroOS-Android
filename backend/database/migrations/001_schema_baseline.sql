-- AISRestroOS MySQL Baseline Schema Migration
-- Database: u328317103_restro
-- Engine: InnoDB, Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- Safe execution: All statements use CREATE TABLE IF NOT EXISTS

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `phone` VARCHAR(50) NULL,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(50) NOT NULL DEFAULT 'owner',
  `remember_token` VARCHAR(100) NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Personal Access Tokens (Laravel Sanctum compatible)
CREATE TABLE IF NOT EXISTS `personal_access_tokens` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tokenable_type` VARCHAR(255) NOT NULL DEFAULT 'App\\Models\\User',
  `tokenable_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL DEFAULT 'auth_token',
  `token` VARCHAR(64) NOT NULL UNIQUE,
  `abilities` TEXT NULL,
  `last_used_at` TIMESTAMP NULL,
  `expires_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_pat_token` (`token`),
  INDEX `idx_pat_tokenable` (`tokenable_type`, `tokenable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Restaurants Table
CREATE TABLE IF NOT EXISTS `restaurants` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `slug` VARCHAR(255) NULL,
  `owner_id` BIGINT UNSIGNED NOT NULL,
  `phone` VARCHAR(50) NULL,
  `email` VARCHAR(255) NULL,
  `address` TEXT NULL,
  `currency` VARCHAR(10) NOT NULL DEFAULT 'INR',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_restro_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Branches Table
CREATE TABLE IF NOT EXISTS `branches` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `code` VARCHAR(50) NULL,
  `address` TEXT NULL,
  `phone` VARCHAR(50) NULL,
  `is_main` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_branches_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Restaurant Memberships
CREATE TABLE IF NOT EXISTS `restaurant_memberships` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `role` VARCHAR(50) NOT NULL DEFAULT 'owner',
  `status` VARCHAR(50) NOT NULL DEFAULT 'active',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_restro` (`user_id`, `restaurant_id`),
  INDEX `idx_memberships_user` (`user_id`),
  INDEX `idx_memberships_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Categories Table
CREATE TABLE IF NOT EXISTS `categories` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `name` VARCHAR(255) NOT NULL,
  `display_order` INT NOT NULL DEFAULT 0,
  `sync_seq` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_cat_restro` (`restaurant_id`),
  INDEX `idx_cat_sync` (`restaurant_id`, `sync_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Menu Items Table
CREATE TABLE IF NOT EXISTS `menu_items` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `category_id` VARCHAR(64) NULL,
  `code` VARCHAR(50) NULL,
  `name` VARCHAR(255) NOT NULL,
  `category` VARCHAR(255) NOT NULL DEFAULT 'General',
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `tax_mode` VARCHAR(50) NOT NULL DEFAULT 'WITHOUT_TAX',
  `tax_percentage` VARCHAR(50) NOT NULL DEFAULT 'None',
  `current_stock` INT NOT NULL DEFAULT 100,
  `is_default_tax` TINYINT(1) NOT NULL DEFAULT 1,
  `is_favourite` TINYINT(1) NOT NULL DEFAULT 0,
  `is_available` TINYINT(1) NOT NULL DEFAULT 1,
  `is_veg` TINYINT(1) NOT NULL DEFAULT 1,
  `image_url` VARCHAR(500) NULL,
  `sync_seq` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_menu_restro` (`restaurant_id`),
  INDEX `idx_menu_category` (`restaurant_id`, `category`),
  INDEX `idx_menu_sync` (`restaurant_id`, `sync_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Customers Table
CREATE TABLE IF NOT EXISTS `customers` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `mobile_number` VARCHAR(20) NOT NULL,
  `email` VARCHAR(255) NULL,
  `is_regular` TINYINT(1) NOT NULL DEFAULT 0,
  `sync_seq` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_cust_restro_mobile` (`restaurant_id`, `mobile_number`),
  INDEX `idx_cust_sync` (`restaurant_id`, `sync_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Tables Table
CREATE TABLE IF NOT EXISTS `tables` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `table_number` VARCHAR(50) NOT NULL,
  `capacity` INT NOT NULL DEFAULT 4,
  `is_occupied` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_restro_table` (`restaurant_id`, `table_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Orders Table
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(64) NOT NULL UNIQUE,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `order_number` VARCHAR(50) NOT NULL,
  `invoice_number` VARCHAR(50) NULL,
  `order_type` VARCHAR(50) NOT NULL DEFAULT 'DINE_IN',
  `status` VARCHAR(50) NOT NULL DEFAULT 'ON_HOLD',
  `table_number` VARCHAR(50) NULL,
  `customer_id` VARCHAR(64) NULL,
  `customer_name` VARCHAR(255) NULL,
  `customer_phone` VARCHAR(50) NULL,
  `subtotal` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `tax_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `service_charge` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `grand_total` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `payment_method` VARCHAR(50) NULL,
  `payment_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  `sync_seq` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
  `completed_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_orders_uuid` (`uuid`),
  INDEX `idx_orders_restro_status` (`restaurant_id`, `status`),
  INDEX `idx_orders_restro_created` (`restaurant_id`, `created_at`),
  INDEX `idx_orders_sync` (`restaurant_id`, `sync_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Order Items Table
CREATE TABLE IF NOT EXISTS `order_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_uuid` VARCHAR(64) NOT NULL,
  `menu_item_id` VARCHAR(64) NULL,
  `item_name` VARCHAR(255) NOT NULL,
  `unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `quantity` INT NOT NULL DEFAULT 1,
  `total_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_order_items_uuid` (`order_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. KOT Tickets & Items
CREATE TABLE IF NOT EXISTS `kot_tickets` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `kot_number` VARCHAR(50) NOT NULL,
  `order_uuid` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `table_number` VARCHAR(50) NULL,
  `status` VARCHAR(50) NOT NULL DEFAULT 'SENT_TO_KITCHEN',
  `reprint_count` INT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_kot_order` (`order_uuid`),
  INDEX `idx_kot_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `kot_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `kot_id` BIGINT UNSIGNED NOT NULL,
  `item_name` VARCHAR(255) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `notes` TEXT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_kot_items_parent` (`kot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Payments Table
CREATE TABLE IF NOT EXISTS `payments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `order_uuid` VARCHAR(64) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `payment_method` VARCHAR(50) NOT NULL,
  `transaction_reference` VARCHAR(255) NULL,
  `status` VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_payments_order` (`order_uuid`),
  INDEX `idx_payments_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Inventory Items & Stock Movements
CREATE TABLE IF NOT EXISTS `inventory_items` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `name` VARCHAR(255) NOT NULL,
  `unit` VARCHAR(50) NOT NULL DEFAULT 'kg',
  `current_stock` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `min_stock_alert` DECIMAL(10,2) NOT NULL DEFAULT 5.00,
  `cost_per_unit` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_inventory_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `stock_movements` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventory_item_id` VARCHAR(64) NOT NULL,
  `movement_type` VARCHAR(50) NOT NULL,
  `quantity` DECIMAL(10,2) NOT NULL,
  `reason` VARCHAR(255) NULL,
  `reference_id` VARCHAR(64) NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_stock_item` (`inventory_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Devices Table (Bluetooth Thermal Printers)
CREATE TABLE IF NOT EXISTS `devices` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `device_name` VARCHAR(255) NOT NULL,
  `mac_address` VARCHAR(50) NULL,
  `device_type` VARCHAR(50) NOT NULL DEFAULT 'PRINTER',
  `paper_width` VARCHAR(20) NOT NULL DEFAULT '58mm',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_devices_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Subscription Plans & Subscriptions
CREATE TABLE IF NOT EXISTS `subscription_plans` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `duration_days` INT NOT NULL DEFAULT 30,
  `features` TEXT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `subscriptions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `plan_id` VARCHAR(64) NOT NULL,
  `status` VARCHAR(50) NOT NULL DEFAULT 'active',
  `starts_at` DATETIME NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_subs_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Restaurant Settings (Per-restaurant sequence numbering)
CREATE TABLE IF NOT EXISTS `restaurant_settings` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `restaurant_id` VARCHAR(64) NOT NULL UNIQUE,
  `bill_prefix` VARCHAR(20) NOT NULL DEFAULT 'INV-',
  `last_bill_number` INT NOT NULL DEFAULT 0,
  `kot_prefix` VARCHAR(20) NOT NULL DEFAULT 'KOT-',
  `last_kot_number` INT NOT NULL DEFAULT 0,
  `tax_percentage` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  `service_charge_percentage` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_settings_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Idempotency Keys, Tombstones, Audit Logs
CREATE TABLE IF NOT EXISTS `idempotency_keys` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `key_value` VARCHAR(255) NOT NULL UNIQUE,
  `resource_type` VARCHAR(50) NOT NULL,
  `response_payload` LONGTEXT NOT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_idemp_key` (`key_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sync_tombstones` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `entity_type` VARCHAR(50) NOT NULL,
  `entity_uuid` VARCHAR(64) NOT NULL,
  `sync_seq` BIGINT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_tomb_sync` (`restaurant_id`, `sync_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `restaurant_id` VARCHAR(64) NULL,
  `user_id` BIGINT UNSIGNED NULL,
  `action` VARCHAR(255) NOT NULL,
  `details` TEXT NULL,
  `ip_address` VARCHAR(50) NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_audit_restro` (`restaurant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
