# Hostinger Deployment Guide for AISRestroOS Backend

This document describes the step-by-step deployment procedure for hosting the Core PHP REST API backend on Hostinger shared/cloud hosting for domain `restro.trakiyo.in`.

---

## 1. Environment & Architecture Overview

- **Production Domain:** `https://restro.trakiyo.in`
- **Target API Directory:** `public_html/restro/api/`
- **Public API Base URL:** `https://restro.trakiyo.in/api/` (Retrofit also supports `/api/v1/`)
- **Existing Admin Directory:** `public_html/restro/admin/` (**MUST BE PRESERVED - DO NOT OVERWRITE**)
- **MySQL Database:** `u328317103_restro` on Hostinger MySQL Server

---

## 2. Pre-Deployment Preparation

### 2.1 Prepare `config/database.php`
On your local machine or in Hostinger File Manager, create `config/database.php` based on `config/database.example.php`:

```php
<?php
return [
    'driver'    => 'mysql',
    'host'      => 'localhost',
    'port'      => 3306,
    'database'  => 'u328317103_restro',
    'username'  => 'u328317103_YOUR_USER',
    'password'  => 'YOUR_SECURE_HOSTINGER_PASSWORD',
    'charset'   => 'utf8mb4',
    'collation' => 'utf8mb4_unicode_ci',
    'options'   => [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ],
];
```

> **IMPORTANT:** Never commit real database credentials to Git or public repositories. Keep production credentials secured with file permissions `0600` or `0640`.

---

## 3. Database Migration / Baseline Execution

1. Log into your **Hostinger hPanel** -> **Databases** -> **phpMyAdmin** for database `u328317103_restro`.
2. Open the SQL tab.
3. Import or execute the contents of:
   `backend/database/migrations/001_schema_baseline.sql`
4. The migration script uses `CREATE TABLE IF NOT EXISTS` and does not drop any existing tables or records. Existing restaurants, users, orders, and tokens will remain completely intact.

---

## 4. File Upload Checklist

Upload the contents of `F:\Android Apps\AISRestroOS\backend` into Hostinger directory `public_html/restro/api/`.

### Files and Directories to Upload:
- [x] `index.php` (Front controller)
- [x] `.htaccess` (Rewrite rules and HTTP Authorization header forwarder)
- [x] `config/` (contains `database.php` with real credentials)
- [x] `core/` (Database, Router, Request, Response, Auth, Validator, UUID, Helpers)
- [x] `middleware/` (AuthMiddleware, TenantMiddleware)
- [x] `modules/` (all 14 Controller modules)
- [x] `uploads/` (contains `.htaccess` and `menu/` directory)

### Files to EXCLUDE from Upload:
- [ ] `tests/` (Development test suite)
- [ ] `database/` (Documentation and migration files)
- [ ] `docs/` (Architecture and audit docs)
- [ ] `.git` / `.gitignore`
- [ ] Any `.env` or local temporary logs

---

## 5. File Permissions & Security

Inside `public_html/restro/api/`:
1. Directories: Set to `0755` (rwxr-xr-x)
2. PHP Files: Set to `0644` (rw-r--r--)
3. Configuration file (`config/database.php`): Set to `0600` or `0640`
4. Upload directory (`uploads/menu/`): Ensure write permissions for the web server user (`0755` or `0775`)
5. Verify `uploads/.htaccess` exists to disallow executing any uploaded scripts.

---

## 6. Verification & Post-Deployment Testing

### 6.1 Test System Health
Execute via cURL or browser:
```bash
curl -i https://restro.trakiyo.in/api/health
```
Expected HTTP 200 response:
```json
{
  "status": "success",
  "message": "AISRestroOS backend is healthy",
  "data": {
    "status": "online",
    "service": "AISRestroOS Core PHP REST API",
    "version": "1.0.0",
    "runtime": "PHP 8.x"
  }
}
```

### 6.2 Test Authentication
```bash
curl -X POST https://restro.trakiyo.in/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"luckysingh955416@gmail.com","password":"YOUR_PASSWORD"}'
```
Expected HTTP 200 response with Bearer token.

### 6.3 Verify Admin Panel Preservation
Navigate to `https://restro.trakiyo.in/admin/` in your browser. Verify the admin interface loads without interference from the `/api/` rewrite rules.

### 6.4 Verify Android App Connectivity
Open the AISRestroOS Android app on your device or emulator:
1. Log into your account or register a new branch.
2. Verify menu items load from the remote MySQL database.
3. Test creating a held order and printing a KOT.
4. Complete an order and verify sequential bill numbering (INV-0001, etc.).
5. Check dashboard overview metrics update immediately.
