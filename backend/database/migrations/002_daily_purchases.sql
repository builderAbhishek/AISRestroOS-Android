-- ============================================================================
-- AISRestroOS: Daily Purchases & Expense Management Migration
-- Target Database: u328317103_restro (Hostinger MariaDB / MySQL)
-- Multi-Tenant Safe: Isolated by restaurant_id
-- ============================================================================

CREATE TABLE IF NOT EXISTS `daily_purchases` (
  `id` VARCHAR(64) NOT NULL,
  `restaurant_id` VARCHAR(64) NOT NULL,
  `branch_id` VARCHAR(64) NULL,
  `item_name` VARCHAR(255) NOT NULL,
  `category` VARCHAR(100) NOT NULL DEFAULT 'Vegetables',
  `quantity` DECIMAL(10,2) NOT NULL DEFAULT 1.00,
  `unit` VARCHAR(50) NOT NULL DEFAULT 'kg',
  `vendor_name` VARCHAR(255) NULL,
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `payment_mode` VARCHAR(50) NOT NULL DEFAULT 'CASH',
  `expense_type` VARCHAR(50) NOT NULL DEFAULT 'PURCHASE',
  `purchase_date` DATE NOT NULL,
  `notes` TEXT NULL,
  `created_by_user_id` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_purchases_restro_date` (`restaurant_id`, `purchase_date`),
  INDEX `idx_purchases_vendor` (`restaurant_id`, `vendor_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
