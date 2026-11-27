# AISRestroOS Custom Core PHP REST API Backend

A custom, high-performance Core PHP REST API backend built specifically for the **AISRestroOS** Android POS application.

---

## 1. Technical Specifications

- **Runtime:** PHP 8.x (Compatible with Hostinger Shared / Cloud hosting)
- **Database Engine:** MySQL / MariaDB (`u328317103_restro`) with PDO prepared statements
- **Architecture:** Zero-framework, modular Core PHP with RESTful JSON endpoints
- **Authentication:** Bearer token authentication matching Laravel Sanctum conventions (`personal_access_tokens` table)
- **Multi-Tenancy:** Automated tenant resolution from authenticated user memberships (`restaurant_memberships`)

---

## 2. Directory Structure

```
backend/
├── index.php                    # Front controller & route dispatcher
├── .htaccess                    # URL rewrite and authorization header forwarder
├── README.md                    # Project overview & quickstart
├── config/
│   └── database.example.php     # Sample database configuration
├── core/
│   ├── Database.php             # Singleton PDO wrapper & transaction manager
│   ├── Request.php              # HTTP request, headers, body & files parser
│   ├── Response.php             # JSON response wrapper (ApiResponseWrapper<T>)
│   ├── Router.php               # Dynamic route dispatcher & prefix normalizer
│   ├── Auth.php                 # Bearer token validation & password hashing
│   ├── Validator.php            # Structured input validation
│   ├── UUID.php                 # RFC 4122 v4 UUID generator
│   └── Helpers.php              # Sequence generator & audit logging
├── middleware/
│   ├── AuthMiddleware.php       # Bearer token guard
│   └── TenantMiddleware.php     # Restaurant tenancy isolation guard
├── modules/
│   ├── AuthController.php       # Registration, login, logout, me
│   ├── MenuController.php       # Dish catalog & image upload
│   ├── CategoryController.php   # Category CRUD
│   ├── CustomerController.php   # Regular & walk-in customers
│   ├── TableController.php      # Table occupancy management
│   ├── OrderController.php      # Order lifecycle, hold, complete, billing
│   ├── KotController.php        # KOT sequential ticketing & reprint
│   ├── PaymentController.php    # Tender recording
│   ├── InventoryController.php  # Stock tracking & movements
│   ├── ReportController.php     # Sales & itemized analytics
│   ├── DashboardController.php  # Overview metrics for dashboard
│   ├── DeviceController.php     # Printer & terminal registry
│   ├── SubscriptionController.php # Business mobile plans
│   └── SyncController.php       # Real-time incremental delta sync
├── uploads/
│   ├── .htaccess                # Disables script execution in uploads
│   └── menu/                    # Stored dish images
├── database/
│   ├── SCHEMA.md                # Full schema documentation
│   └── migrations/
│       └── 001_schema_baseline.sql # Non-destructive MySQL migration script
├── docs/
│   ├── APP_AUDIT.md             # Complete Android app audit
│   ├── API_DOCUMENTATION.md     # Detailed API endpoint reference
│   └── DEPLOYMENT.md            # Hostinger deployment guide
└── tests/
    └── run_tests.php            # Automated local test runner
```

---

## 3. Local Development & Testing

To run the automated PHP test suite locally:

```bash
cd "F:\Android Apps\AISRestroOS\backend"
php tests/run_tests.php
```

To run a local PHP built-in server:

```bash
cd "F:\Android Apps\AISRestroOS\backend"
php -S 127.0.0.1:8000 index.php
```
