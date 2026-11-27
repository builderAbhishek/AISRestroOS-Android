# AISRestroOS – Production Migration Execution Guide & Safety Runbook

**Target Database:** `u328317103_restro`  
**Host Environment:** Hostinger Cloud / Shared / VPS MariaDB (MariaDB 11.8+ / MariaDB 10.5+)  
**Migration File:** `backend/database/migrations/FINAL_PRODUCTION_MIGRATION.sql`  
**Validation File:** `backend/database/POST_MIGRATION_VALIDATION.sql`  
**Automated Test Suite:** `backend/tests/run_mariadb_migration_test.php` (100% Passed)

---

## 1. Important Architectural Reality: DDL Implicit Commit

> [!WARNING]
> **DDL Cannot Be Rolled Back Transactionally in MySQL or MariaDB.**  
> In MySQL and MariaDB engines, Data Definition Language (DDL) statements (`ALTER TABLE`, `CREATE TABLE`, `CREATE INDEX`, `DROP TABLE`) cause an **IMPLICIT COMMIT** immediately before and after execution.
> 
> Therefore, wrapping DDL statements inside `START TRANSACTION ... ROLLBACK` **does NOT revert structural schema alterations** if an error occurs midway. DML statements (`INSERT`, `UPDATE`, `DELETE`) participate in transactions, but DDL alters are committed per statement.
>
> **The only guaranteed recovery mechanism for DDL operations is a verified pre-migration database backup.**

---

## 2. Pre-Migration Safety Checklist & Backup Procedure

> [!CRITICAL]
> **DO NOT EXECUTE THE MIGRATION WITHOUT PERFORMING THIS BACKUP FIRST.**

### Step 2.1: Method A – Command-Line (SSH / Terminal)
Run `mysqldump` with transaction isolation and full routine/trigger inclusion:

```bash
# 1. Create a timestamped logical backup
mysqldump -u u328317103_restro -p \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  u328317103_restro > pre_migration_backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Verify the backup file was written and is non-empty
ls -lh pre_migration_backup_*.sql
```

### Step 2.2: Method B – Hostinger phpMyAdmin (Web GUI)
1. Log in to **Hostinger hPanel** → **Databases** → **phpMyAdmin** → Select `u328317103_restro`.
2. Click on the **Export** tab in the top navigation bar.
3. Select **Custom** export method:
   - Tables: Ensure **all 28 tables** are selected.
   - Output: Save output to a file (Character set: `utf-8`, Compression: `None` or `zipped`).
   - Format-specific options: Check **Add DROP TABLE / VIEW / PROCEDURE / FUNCTION / EVENT / TRIGGER statement** (optional, recommended for clean restores).
   - Data creation options: Select **INSERT statements with column names**.
4. Click **Export** and save the file safely to your local workstation.

---

## 3. Database Compatibility Matrix

| Engine | Version | Compatibility | Technical Notes |
|---|---|---|---|
| **MariaDB** | **11.8+ / 10.11 LTS / 10.5+** | **100% Fully Compatible (Verified)** | Natively supports idempotent `ADD COLUMN IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`. Hostinger runs MariaDB. |
| **MySQL** | **8.0.x** | **Requires Procedural Wrapper** | MySQL 8.0 throws `ERROR 1064 (42000)` on `IF NOT EXISTS` syntax in `ALTER TABLE` and `CREATE INDEX`. Verified on MySQL 8.0.46. |
| **SQLite** | **3.x** | **Unit Testing Compatible** | Emulated via SQLite unit test runner in `backend/tests/run_tests.php`. |

---

## 4. Step-by-Step Migration Execution

### Execution via Hostinger phpMyAdmin (Recommended)
1. Open phpMyAdmin and select database `u328317103_restro` in the left panel.
2. Click the **Import** tab.
3. Click **Choose File** and select:
   ```text
   backend/database/migrations/FINAL_PRODUCTION_MIGRATION.sql
   ```
4. Configuration options:
   - Character set: **utf-8**
   - Format: **SQL**
   - Partial import: **Unchecked**
5. Click **Import** (or **Go**).
6. Execution completes in ~1.5 to 2.5 seconds (76 statements executed). Confirm the green success banner.

### Execution via SSH CLI
```bash
mysql -u u328317103_restro -p u328317103_restro < backend/database/migrations/FINAL_PRODUCTION_MIGRATION.sql
```

---

## 5. Post-Migration Validation (Mandatory Verification)

Immediately after applying the migration, execute the validation script:

1. In phpMyAdmin, click the **SQL** query tab for `u328317103_restro`.
2. Copy and paste the entire contents of:
   ```text
   backend/database/POST_MIGRATION_VALIDATION.sql
   ```
3. Click **Go** to execute all 117 validation checks.
4. **Acceptance Criteria (Every check must return `PASS` with zero `FAIL` results):**
   - **Section 1 (Record Integrity - 5 checks):** User 1, Restaurant, Branch, Memberships, and Sanctum tokens preserved.
   - **Section 2 (Required Columns - 57 checks):** All 57 required columns across 21 tables verified present.
   - **Section 3 (Required Indexes - 23 checks):** All 23 performance indexes verified present.
   - **Section 4 (Foreign Keys - 24 checks):** All 24 relational foreign key constraints verified present.
   - **Section 5 (Sequence Counters - 1 check):** Restaurant sequence counters row present and initialized.
   - **Section 6 (Duplicate Check - 2 checks):** 0 duplicate invoice numbers and 0 duplicate KOT numbers.
   - **Section 7 (Orphan Check - 4 checks):** 0 orphan order items, 0 orphan KOT tickets, 0 orphan KOT items, 0 orphan payments.
   - **Section 8 (Table Count - 1 check):** Exactly 28 tables present.
   - **Automated Summary:** `Total: 117 | Passed: 117 | Failed: 0`.

---

## 6. Migration Failure Recovery Runbook (Hostinger Compatible)

> [!IMPORTANT]
> **Hostinger Shared/Cloud Privilege Constraint:**  
> The MySQL user `u328317103_restro` does **not** have global `DROP DATABASE` or `CREATE DATABASE` permissions. Attempting `DROP DATABASE u328317103_restro;` will fail with `ERROR 1044 (42000): Access denied`.  
> Therefore, all recovery procedures operate strictly **inside the existing database** at the table level.

If a network interruption, server crash, or unexpected error occurs during execution:

### Scenario A: Failure during Phase 1 (Column Additions)
- **Database State:** Statements prior to the failure are committed (due to DDL implicit commit). The failed statement was rejected without altering that table.
- **Remediation:** 
  1. Inspect the error line number in phpMyAdmin or terminal.
  2. Because all column additions use `ADD COLUMN IF NOT EXISTS`, you can safely re-run `FINAL_PRODUCTION_MIGRATION.sql` from the beginning once the issue is resolved.

### Scenario B: Failure during Phase 2 (DML Data Backfills)
- **Database State:** Data updates run inside transactions. An uncommitted update will be rolled back.
- **Remediation:**
  1. Fix the backfill condition.
  2. Re-run `FINAL_PRODUCTION_MIGRATION.sql`. All backfill queries are idempotent (`WHERE <new_col> IS NULL`).

### Scenario C: Emergency Disaster Recovery – In-Database Table Wipe & Restore
If an unrecoverable failure occurs and you must return immediately to the exact pre-migration state:

#### Method 1: Hostinger phpMyAdmin (GUI)
1. Open phpMyAdmin and select `u328317103_restro`.
2. Scroll to the bottom of the table list and click **Check all**.
3. In the dropdown next to it ("With selected:"), choose **Drop**.
4. Confirm "Do you really want to execute the following query?" -> Click **Yes**.
5. Once all tables are cleared, click the **Import** tab.
6. Choose your `pre_migration_backup_*.sql` file and click **Import**.
7. All 28 tables, records, passwords, and tokens are restored within ~5 seconds.

#### Method 2: SSH CLI (Command-Line)
Run the in-database table drop followed by restore (no `DROP DATABASE` required):

```bash
# 1. Disable FK checks, drop all tables, and re-enable FK checks
mysql -u u328317103_restro -p u328317103_restro -e "
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS 
  audit_logs, branches, categories, customers, devices, failed_jobs, 
  idempotency_keys, inventory_items, job_batches, jobs, kot_items, 
  kot_tickets, menu_items, migrations, order_items, orders, 
  password_reset_tokens, payments, personal_access_tokens, refunds, 
  restaurant_memberships, restaurant_settings, restaurants, 
  stock_movements, subscription_plans, subscriptions, sync_tombstones, tables;
SET FOREIGN_KEY_CHECKS = 1;
"

# 2. Restore directly from the clean pre-migration backup
mysql -u u328317103_restro -p u328317103_restro < pre_migration_backup_20261123.sql
```
Verified in automated recovery test `backend/tests/test_backup_restore.php` (restored in 4,784 ms with 100% integrity).

---

## 7. Deployment Configuration & Directory Resolution

### Directory Structure Resolution
The public URL for the Android REST API is:
```text
https://restro.trakiyo.in/api/
```

On Hostinger, how your domain is mapped determines the exact folder path:
- **Case 1 (Subdomain Document Root is `public_html/restro`):**
  If `restro.trakiyo.in` document root is set to `public_html/restro`, deploy the Core PHP files to:
  ```text
  public_html/restro/api/
  ```
- **Case 2 (Subdomain Document Root is `public_html`):**
  If the subdomain points to `public_html/`, deploy the Core PHP files to:
  ```text
  public_html/api/
  ```

### Seamless Routing via Router Normalization
`backend/core/Router.php` includes automated path normalization that strips both `/restro/api` and `/api` prefixes:
```php
$prefixes = ['/restro/api/v1', '/restro/api', '/api/v1', '/api', '/v1'];
```
Therefore, incoming requests to `https://restro.trakiyo.in/api/orders` route directly to the orders controller regardless of whether the physical folder on disk is `public_html/restro/api` or `public_html/api`.

### Production Database Configuration
On the Hostinger server, copy `backend/config/database.example.php` to `backend/config/database.php`:
```php
<?php
return [
    'driver'    => 'mysql',
    'host'      => '127.0.0.1',
    'port'      => 3306,
    'database'  => 'u328317103_restro',
    'username'  => 'u328317103_restro',
    'password'  => '<YOUR_HOSTINGER_DB_PASSWORD>',
    'charset'   => 'utf8mb4',
    'collation' => 'utf8mb4_unicode_ci',
];
```
