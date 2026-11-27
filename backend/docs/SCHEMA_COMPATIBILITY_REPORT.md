# AISRestroOS – Production Database Schema Compatibility Report

**Document Version:** 1.0.0  
**Target Environment:** Hostinger Managed Cloud  
**Engine:** MariaDB 11.8+ / MySQL 8.x (`u328317103_restro`)  
**Dual Consumer Architecture:** AISRestroOS Core PHP REST API (`/api/`) + Existing Web Portal (`/admin/`)  
**Data Safety Standard:** Strict Zero Data Loss / Non-Destructive In-Place Upgrade  

---

## 1. Executive Summary

This report delivers a comprehensive structural and functional comparison between:
1. **Existing Production Database Export (`u328317103_restro.sql`)**: 28 tables supporting existing production records and the Laravel-based `/admin/` application.
2. **Proposed Baseline Schema (`001_schema_baseline.sql`)**: 23 tables proposed for the new standalone Core PHP backend.
3. **AISRestroOS Core PHP Backend Source Code (`/backend/`)**: 14 REST API controllers, authentication engine, SQLite/MySQL PDO abstraction layer, and POS sync engine.

### Core Verdict
A standard destructive schema replacement (`DROP TABLE` or replacing tables) **would have caused catastrophic production failure**, deleting User `1` (`Abhishek Chaudhary`), restaurant `'01a207f7-8342-7368-b5de-2019baf99da4'` (`Siddharth Family Resturent`), active device tokens, and breaking the existing `/admin/` portal.

Instead, a **Dual-Compatible Non-Destructive In-Place Migration** (`FINAL_PRODUCTION_MIGRATION.sql`) was designed, implemented, and verified. Companion columns, nullable foreign keys, and intelligent SQL fallback expressions (`COALESCE`) ensure that both systems function concurrently on the same database instance without collisions or data loss.

---

## 2. Global Architectural Realities & Resolution

### 2.1 UUID Primary Keys (`CHAR(36)`) vs Auto-Increment
* **Production Reality:** Business entities in `u328317103_restro` (`restaurants`, `branches`, `restaurant_memberships`, `categories`, `menu_items`, `customers`, `tables`, `inventory_items`, `subscription_plans`) use `CHAR(36)` UUID strings as primary keys.
* **Proposed Baseline Flaw:** Baseline attempted to redefine several of these as integer `BIGINT UNSIGNED AUTO_INCREMENT` (e.g. `restaurant_memberships`).
* **Resolution:** All existing primary keys remain `CHAR(36)` UUIDs. Core PHP controllers (`UUID::v4()`) generate compatible RFC 4122 v4 UUIDs for all entity inserts, ensuring 100% relational integrity.

### 2.2 The Fatal `orders.sync_seq` Double Auto-Increment
* **Proposed Baseline Flaw:** Baseline declared both `id BIGINT AUTO_INCREMENT` and `sync_seq BIGINT AUTO_INCREMENT UNIQUE` on the `orders` table. In MySQL and MariaDB, table definition `1075: Incorrect table definition; there can be only one auto column and it must be defined as a key` immediately crashes execution.
* **Production Reality:** In `u328317103_restro`, `id` is the single `BIGINT AUTO_INCREMENT` primary key, and `sync_seq` is `BIGINT UNSIGNED NOT NULL DEFAULT 1`.
* **Resolution:** `orders.sync_seq` is maintained as a normal indexed sequence column. Sequence ordering and delta cursors in `SyncController` increment or filter on `sync_seq` and `id` without illegal DDL attributes.

### 2.3 Order Item & KOT Dual Keys (`BIGINT` vs `UUID`)
* **Production Reality:** `order_items`, `kot_tickets`, and `payments` link to `orders` via `order_id BIGINT UNSIGNED` with foreign key constraints `ON DELETE CASCADE`.
* **Proposed Baseline Flaw:** Baseline assumed line items link exclusively via `order_uuid VARCHAR(64)`.
* **Resolution:** Dual-column compatibility:
  - Tables contain both `order_id` (foreign key) and `order_uuid` (indexed UUID).
  - Backend writes populate both: `$orderId` (from `lastInsertId` or existing row) and `$orderUuid`.
  - Backend reads query using `COALESCE` and `ON (o.uuid = oi.order_uuid OR o.id = oi.order_id)`.
  - Legacy `/admin/` queries via `order_id` remain 100% operational.

### 2.4 Restaurant Settings: Key-Value vs Fixed Columns
* **Production Reality:** Existing table stores flexible key-value rows (`key_name VARCHAR(64)`, `key_value TEXT`) with a unique constraint on `(restaurant_id, branch_id, key_name)`.
* **Proposed Baseline Flaw:** Baseline assumed `restaurant_settings` had fixed sequence columns: `bill_prefix`, `last_bill_number`, `kot_prefix`, `last_kot_number`.
* **Resolution:** In-place column expansion:
  - Sequence and tax columns were added directly to `restaurant_settings`.
  - `key_name` was made nullable with default `'general'`.
  - A dedicated sequence row with `key_name = 'sequence'` is initialized for each restaurant.
  - Transactions allocate next bill/KOT numbers via `FOR UPDATE` row locks, supporting both models simultaneously.

---

## 3. Table-by-Table Deep Comparison Matrix

| # | Table Name | Status in Prod | Status in Baseline | Differences & Migration Strategy |
|---|---|---|---|---|
| 1 | `users` | Exists (1 record) | Exists | Prod lacks `role`. Added `role VARCHAR(50) DEFAULT 'owner'`. Backfilled User 1 (`Abhishek Chaudhary`) to `'owner'`. Phone widened to `VARCHAR(50)`. Password hash `$2y$12$...` preserved. |
| 2 | `restaurants` | Exists (1 record) | Exists | Prod lacks `owner_id`, `address`, `currency`. Added columns; backfilled `owner_id = 1` and `currency = 'INR'`. Existing slug `siddharth-family-resturent-IoCPP` preserved. |
| 3 | `branches` | Exists (1 record) | Exists | Prod has `is_main_branch`. Added `is_main TINYINT(1)`. Backfilled `is_main = is_main_branch` (1). Widened `address` to `TEXT`, `phone` to `VARCHAR(50)`. |
| 4 | `restaurant_memberships` | Exists (1 record) | Exists | Prod primary key is `CHAR(36)`, has `is_active` (1) and `role ENUM(...)`. Added `status VARCHAR(50) DEFAULT 'active'`. Widened `role` to `VARCHAR(50)`. Preserved UUID PK `'01a207f7-8345-7568-9bbf-70bb22afffbd'`. |
| 5 | `categories` | Exists (0 records) | Exists | Schema matches. Widened `name` to `VARCHAR(255)`. Added index `idx_cat_sync (restaurant_id, sync_seq)`. |
| 6 | `customers` | Exists (0 records) | Exists | Prod lacked `email`. Added `email VARCHAR(255) NULL AFTER mobile_number`. Widened `mobile_number` to `VARCHAR(50)`. Added index `idx_cust_sync`. |
| 7 | `menu_items` | Exists (0 records) | Exists | Prod lacked `category_id` and `is_veg`. Added `category_id VARCHAR(64) NULL` and `is_veg TINYINT(1) DEFAULT 1`. Widened `image_url` to `VARCHAR(500)`. |
| 8 | `tables` | Exists (0 records) | Exists | Prod uses `status ENUM(...)`. Added companion `is_occupied TINYINT(1) DEFAULT 0`. Widened `table_number` to `VARCHAR(50)` and `status` to `VARCHAR(50)`. |
| 9 | `orders` | Exists (0 records) | Exists | Prod lacked `invoice_number`, `payment_status`, `payment_method`, `completed_at`. Added columns. Prevented duplicate auto-increment crash on `sync_seq`. Widened all string fields. |
| 10 | `order_items` | Exists (0 records) | Exists | Prod has `order_id BIGINT`, `price`, `total`. Baseline had `order_uuid`, `unit_price`, `total_price`. Added `order_uuid`, `unit_price`, `total_price`. Made `order_id` nullable while retaining FK. |
| 11 | `kot_tickets` | Exists (0 records) | Exists | Prod has `order_id`, `ticket_number`, `uuid`. Baseline had `order_uuid`, `kot_number`, `reprint_count`. Added companion columns. Backend writes populate both number and ID pairs. |
| 12 | `kot_items` | Exists (0 records) | Exists | Prod has `kot_ticket_id BIGINT`. Baseline had `kot_id`. Added `kot_id BIGINT NULL`. Backend inserts populate both `kot_ticket_id` and `kot_id`. |
| 13 | `payments` | Exists (0 records) | Exists | Prod has `uuid`, `order_id`, `reference_number`, `is_verified`. Baseline had `order_uuid`, `transaction_reference`, `status`. Added companion columns. Populated both on write. |
| 14 | `inventory_items` | Exists (0 records) | Exists | Prod has `item_name`, `quantity`, `unit_of_measure`, `low_stock_alert`. Baseline had `name`, `current_stock`, `unit`, `min_stock_alert`, `cost_per_unit`. Added companion columns with bi-directional sync. |
| 15 | `stock_movements` | Exists (0 records) | Exists | Prod has `quantity_change`, `notes`, `created_by_user_id`. Baseline had `quantity`, `reason`, `reference_id`. Added companion columns. Populated both on write. |
| 16 | `devices` | Exists (0 records) | Exists | Prod has `device_code`, `device_token`, `is_authorized`. Baseline had `device_type`, `mac_address`, `paper_width`, `is_active`. Added companion columns. Backfilled `is_active = is_authorized`. |
| 17 | `subscription_plans` | Exists (0 records) | Exists | Prod has quotas (`max_devices`, etc.). Baseline had `features` and `is_active`. Added `features TEXT NULL` and `is_active TINYINT(1) DEFAULT 1`. |
| 18 | `subscriptions` | Exists (0 records) | Exists | Prod has `subscription_plan_id`, `ends_at`. Baseline had `plan_id`, `expires_at`. Added companion columns. Backfilled `plan_id = subscription_plan_id` and `expires_at = ends_at`. |
| 19 | `sync_tombstones` | Exists (0 records) | Exists | Prod has `deleted_at`. Baseline had `created_at`. Added `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`. Backfilled `created_at = deleted_at`. |
| 20 | `idempotency_keys` | Exists (0 records) | Exists | Prod has `idempotency_key`, `request_path`, `response_code`. Baseline had `key_value`, `resource_type`. Added companion columns. Made `request_path` and `restaurant_id` nullable. |
| 21 | `restaurant_settings` | Exists (0 records) | Exists | Prod has key-value structure (`key_name`, `key_value`). Added sequence columns (`bill_prefix`, `last_bill_number`, etc.). Initialized sequence row for existing restaurant. |
| 22 | `audit_logs` | Exists (0 records) | Exists | Widened `action` to `VARCHAR(255)` and `ip_address` to `VARCHAR(50)`. Added index `idx_audit_restro`. |
| 23 | `personal_access_tokens` | Exists (2 records) | Exists | Preserved without modifications. Tokens for User 1 remain 100% active and authenticatable. |
| 24 | `refunds` | Exists (0 records) | Missing in Base | **PRESERVED AS-IS.** Required by `/admin/` portal. |
| 25 | `jobs` | Exists (0 records) | Missing in Base | **PRESERVED AS-IS.** Required by Laravel queue worker. |
| 26 | `failed_jobs` | Exists (0 records) | Missing in Base | **PRESERVED AS-IS.** Required by Laravel error tracking. |
| 27 | `job_batches` | Exists (0 records) | Missing in Base | **PRESERVED AS-IS.** Required by Laravel background jobs. |
| 28 | `migrations` | Exists (15 records) | Missing in Base | **PRESERVED AS-IS.** Tracks Laravel migration batch history. |

---

## 4. Data Preservation Audit

The migration was verified against existing production records in `u328317103_restro.sql`:
* **User 1:**
  - Name: `Abhishek Chaudhary`
  - Email: `luckysingh955416@gmail.com`
  - Password Hash: `$2y$12$sQsDHJMw7wO5DwhowDBsqOWYLMbMyGKc9kr.fLGv5E6sl9N88UESu` (Untouched, bcrypt verified)
  - Role: Mapped from `RESTAURANT_OWNER` to `'owner'`.
* **Restaurant:**
  - ID: `'01a207f7-8342-7368-b5de-2019baf99da4'`
  - Name: `Siddharth Family Resturent`
  - Owner ID: Backfilled to `1`.
  - Currency: Backfilled to `'INR'`.
* **Branch:**
  - ID: `'01a207f7-8344-7360-8652-4ad52c3e6e38'`
  - Code: `'MAIN-01'`
  - `is_main` and `is_main_branch`: Both set to `1`.
* **Personal Access Tokens:**
  - Token IDs 1 & 2 for user 1 remain valid for POS device authentications.

---

## 5. Migration Execution Safety

* **Transaction Boundary:** Wrapped in `START TRANSACTION; ... COMMIT;` with `SET FOREIGN_KEY_CHECKS = 0;` and restoration to `1`.
* **Idempotency:** Utilizes `ADD COLUMN IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS` native to MariaDB 11.8+, allowing safe re-execution.
* **No Table Locks or Drops:** Operates via metadata modifications and non-blocking in-place updates.
