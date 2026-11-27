# 🍽️ AISRestroOS — Complete Restaurant Operating System & Smart POS

> **Run Your Entire Restaurant from Your Pocket.**  
> An offline-first Android Point-of-Sale (POS), Kitchen Order Ticket (KOT), Thermal Billing, Digital QR Menu, Udhaar Khata, Daily Hisaab, Inventory & Multi-Branch Cloud Platform built for modern restaurants, cafés, dhabas, cloud kitchens, and QSRs.

---

## 📋 Table of Contents

1. [Executive Summary & Sales Pitch](#-executive-summary--sales-pitch)
2. [How AISRestroOS Makes Restaurant Management Effortless](#-how-aisrestroos-makes-restaurant-management-effortless)
3. [Key Features & Functional Modules](#-key-features--functional-modules)
4. [Day-to-Day Usage Guide](#-day-to-day-usage-guide)
5. [System Architecture & Tech Stack](#-system-architecture--tech-stack)
6. [Project Structure](#-project-structure)
7. [Installation & Developer Setup](#-installation--developer-setup)
8. [Testing & Quality Assurance](#-testing--quality-assurance)

---

## 💼 Executive Summary & Sales Pitch

Traditional restaurant POS systems are expensive, require bulky desktop hardware, fail when the internet drops, and force owners to use 4–5 different apps (one for billing, a notebook for *Udhaar*, another register for *Daily Hisaab*, and spreadsheets for stock).

**AISRestroOS** replaces all of them with a single, fast, mobile-first operating system that works on any Android smartphone or tablet and pairs seamlessly with affordable **58mm / 80mm Bluetooth thermal printers**.

### Why Restaurants Buy AISRestroOS (Value Proposition)

| Restaurant Pain Point | How AISRestroOS Solves It | Business Impact |
| :--- | :--- | :--- |
| **Expensive POS Hardware** | Runs on any Android phone/tablet (`Android 7.0+`) + standard Bluetooth printer. | **Zero heavy capex** — start in minutes. |
| **Internet Dropouts During Rush Hour** | **Offline-first architecture** with Room/SQLite + `WorkManager` auto-sync. | **100% uptime** — billing & KOT never stop. |
| **Kitchen Confusion & Missed Items** | **Incremental KOT Delta Printing** only prints newly added items on table updates. | Faster kitchen prep, **zero duplicate dishes**. |
| **Unbilled Credit (*Udhaar*) Leakage** | Built-in **Udhaar Khata** with customer ledger & 1-tap WhatsApp reminders. | Faster cash recovery, **zero forgotten dues**. |
| **No Clarity on Real Daily Profit** | **Daily Hisaab** tracks raw material purchases & daily expenses alongside sales. | Know your **true net margin every night**. |
| **Printed Menu Reprinting Costs** | Instant **Digital QR Menu** generator with print & share support. | Update prices anytime with **zero printing cost**. |
| **Regional Language Billing Issues** | Bitmap-based **ESC/POS Thermal Printing** supports **Hindi / Unicode** text. | Clear, readable bills for every staff & guest. |

### Ideal Target Customers
- **Dine-In Restaurants & Family Dhabas** (Table management, Hold/Resume orders, KOT printing)
- **Cafés, Bakeries & QSRs** (Fast takeaway billing, item variants like Half/Full, QR Menu)
- **Cloud Kitchens & Takeaway Counters** (Order tracking, stock summary, daily expense tracking)
- **Multi-Branch Food Businesses** (Centralized PHP/MySQL backend, Super Admin panel, SaaS subscription control)

---

## ⚡ How AISRestroOS Makes Restaurant Management Effortless

Instead of juggling paper KOT pads, calculators, and manual registers, a restaurant's entire daily lifecycle runs on autopilot:

1. **Morning Opening (2 Minutes):**
   - Open the app → **Auto-connects** to your saved Bluetooth thermal printer silently in the background.
   - Check **Stock Summary** to verify available items and mark out-of-stock dishes in one tap.
2. **Peak Hour Order Taking (Zero Chaos):**
   - Select **Dine-In** (pick an occupied or free table) or **Takeaway**.
   - Search or filter menu items by category/favourites, choose variants (e.g., *Half / Full*), and tap to add to cart.
   - Put the order **On Hold** for running tables and automatically fire a **KOT** to the kitchen.
   - If the guest orders extra items later, reopen the held order and update it — **only the newly added delta items print on the new KOT**.
3. **Fast Checkout & Billing:**
   - Apply discounts, service charges, or taxes; attach a regular customer; choose payment mode (**Cash, UPI, Card, or Udhaar**).
   - Print a crisp **58mm or 80mm Tax Invoice** (with custom header, GSTIN, Hindi/Unicode item names, and QR code) or preview it live on screen.
4. **Nightly Closing & Reconciliation (5 Minutes):**
   - Log milk, vegetables, gas, or staff expenses in **Daily Hisaab** (with smart auto-suggestions).
   - Check **Insights & Reports** for Today vs. Yesterday sales, top-selling items, and net cash in hand.
   - Send **WhatsApp payment reminders** to customers with pending balances in **Udhaar Khata**.

---

## 🚀 Key Features & Functional Modules

### 1. ⚡ Point of Sale (POS) & Order Lifecycle
- **Order Types:** Supports **Dine-In** (with visual Table Selection & Occupancy Status), **Takeaway**, and **Delivery**.
- **Menu Browsing:** Toggle between **Image Grid View** and **Compact List View** (`OrderPreferencesScreen`); filter by Category, *All*, or *Favourite*; instant search.
- **Variants & Pricing:** Supports item variants (`MenuItemVariant`), quantity steppers, percentage/flat discounts, service charges, and tax calculation.
- **Hold & Resume Orders:** Keep multiple active table orders **On Hold** (`OnHoldOrdersScreen`), edit them anytime (`EditOrderPOS`), and convert to **Closed Orders** (`ClosedOrdersScreen`) upon payment.
- **Bill Receipt & Live Preview:** Full on-screen thermal bill preview (`BillReceiptScreen`) with one-tap print/reprint.

### 2. 🖨️ Smart Bluetooth ESC/POS Thermal Printing
- **Hardware Support:** Works with standard **58mm (384px)** and **80mm (576px)** Bluetooth thermal printers (`BluetoothPrinterService`, `EscPosPrinter`).
- **Unicode & Hindi Support:** Renders receipts as high-contrast bitmaps so **Hindi / Devanagari** (e.g., `पनीर टिक्का (Half)`) and special characters print cleanly without garbled fonts.
- **Incremental KOT Delta Printing:** Tracks previously printed quantities on held orders and prints only the newly added items when an order is updated.
- **Customizable Receipts (`ReceiptSettingsScreen`):** Configure custom multi-line restaurant headers (Name, Address, GSTIN), footer notes, paper width, font size, and optional QR code embedding.
- **Auto-Connect & Status Monitoring:** Live printer status indicator (`ONLINE` / `OFFLINE`) right on the Dashboard with 1-tap refresh.

### 3. 📱 Digital QR Menu
- **Instant QR Generation (`DigitalQrMenuScreen`):** Generates a restaurant-specific QR code using `ZXing` pointing to your live online menu URL.
- **Print & Share:** Print the QR standee directly on your thermal printer or share it via WhatsApp/social media.

### 4. 📒 Udhaar Khata (Digital Credit Ledger) & Customer CRM
- **Regular Customers (`RegularCustomersScreen`):** Store customer names, phone numbers, and visit/order history.
- **Udhaar Khata (`UdhaarKhataScreen`):**
  - Track total pending market receivables at a glance.
  - Filter by customers with due balances or search by name/phone.
  - Record **Credit Given (Udhaar)** or **Payment Received (Settle)** with notes.
  - **1-Tap WhatsApp Reminder:** Automatically formats a polite payment reminder message with the pending balance and restaurant name and opens WhatsApp directly.

### 5. 💰 Daily Hisaab (Purchase & Expense Tracker)
- **Daily Expense Logging (`DailyHisaabScreen`):** Record daily purchases (groceries, dairy, gas cylinders, packaging, wages) by date (Today, Yesterday, or Calendar picker).
- **Smart Suggestions (`HisaabSuggestionsManager`):** Learns and suggests frequently purchased items for faster data entry.
- **True Profitability:** Combines with sales data so owners can see real cash flow instead of just gross revenue.

### 6. 📊 Reports, Stock & Business Insights
- **Live Dashboard (`DashboardScreen`):** Real-time **Today vs. Yesterday** sales and order count comparison, quick actions, and branch selector.
- **Insights (`InsightsScreen`):** Visual breakdown of revenue trends, order channels (Dine-In vs. Takeaway), and top-performing items.
- **Detailed Reports:**
  - **Item Reports (`ItemReportsScreen`):** Quantity sold and revenue per dish.
  - **Order Reports (`OrderReportsScreen`):** Filterable order history across Closed, On-Hold, and All orders.
  - **Expense Reports (`ExpenseReportScreen`):** Date-wise purchase and expense summaries.
  - **Stock Summary (`StockSummaryScreen`):** Real-time inventory status, stock codes, and availability filtering.

### 7. 👥 Staff, Multi-Branch & SaaS Subscription Management
- **Manage Staff (`ManageStaffScreen`):** Create, view, and remove staff accounts with role-based access under your restaurant tenancy.
- **Restaurant Profile & Branches (`RestaurantProfileScreen`, `BranchSelectorDialog`):** Manage restaurant details and switch branches cleanly.
- **Subscription & Plan Control (`PlanDetailsScreen`, `SubscriptionExpiredScreen`):** Built-in plan tracking (`Trial`, `Active`, `Expired`) synced with the backend.
- **Web Super Admin Portal (`backend/admin/`):** Dedicated PHP admin dashboard for platform operators to onboard restaurants, manage categories/menus, monitor platform-wide revenue, and control subscriptions.

### 8. 🔄 Offline-First Sync & Reliability
- **Local Persistence:** Powered by **Room / SQLite** (`AppDatabase`, `LocalDatabaseHelper`) and **DataStore Preferences** (`PreferencesManager`).
- **Background Delta Sync (`SyncManager`, `SyncWorker`):** Automatically schedules periodic sync every **15 minutes** (when network is available) plus immediate sync triggers after orders or updates, ensuring local and cloud databases stay consistent.

---

## 📖 Day-to-Day Usage Guide

### Step 1: Login & Profile Setup
1. Launch **AISRestroOS** and sign in with your restaurant/staff credentials (`AuthScreen`).
2. Go to **More → Restaurant Profile** to verify your restaurant name, address, and contact details.

### Step 2: Configure Printer & Receipt
1. Pair your Bluetooth thermal printer in Android Bluetooth settings.
2. Open **More → Printer Setup** (`PrinterSetupScreen`), select your device, and run a **Test Print**.
3. Open **More → Receipt Settings** to choose **58mm** or **80mm**, set your header/GSTIN, and attach a payment/feedback QR code.

### Step 3: Build Your Menu
1. Go to the **Items** tab (`MenuScreen`) and tap **+ Add Menu Item** (`AddMenuItemScreen`).
2. Enter dish name, category, price, optional variants (e.g., Half/Full), and upload/compress a dish photo (`ImageCompressor`).

### Step 4: Take & Manage Orders
1. Tap **New Order** (`AddOrderPOS`) from the Dashboard or Quick Actions.
2. Choose **Dine-In** (select a table) or **Takeaway**, add items to the cart, and tap **Cart** (`CartScreen`).
3. Choose **Hold Order** (fires KOT to kitchen and keeps table open) or **Complete / Pay** (closes order and prints customer bill).
4. View active tables under **Orders → On Hold** and completed bills under **Orders → Closed**.

### Step 5: Track Khata, Hisaab & Reports
- Use **Reports → Daily Hisaab** every evening to log raw material & petty cash expenses.
- Use **Reports → Udhaar Khata** to track credit customers and send WhatsApp reminders.
- Use **Reports → Insights / Item Reports / Stock Summary** to review business performance.

---

## 🏗️ System Architecture & Tech Stack

### Android Application (`app/`)
- **Language:** Kotlin (JVM Target 17)
- **UI Framework:** Jetpack Compose + Material 3 + Material Icons Extended
- **Navigation:** Jetpack Navigation Compose (`NavGraph`, sealed `Screen` routes)
- **Architecture:** MVVM + Repository Pattern (`StateFlow` / `Coroutines`)
- **Local Storage:** Room Database + SQLite + Jetpack DataStore Preferences
- **Background Sync:** AndroidX WorkManager (`SyncWorker`)
- **Networking:** Retrofit 2 + Gson + OkHttp Logging Interceptor
- **Image Loading:** Coil Compose (`coil-compose`)
- **QR Generation:** Google ZXing Core (`com.google.zxing:core:3.5.3`)
- **Printing:** Custom ESC/POS Bluetooth Socket Service + Android `StaticLayout` / `Canvas` Bitmap Renderer
- **SDK Targets:** `minSdk = 24` (Android 7.0), `targetSdk = 35`, `compileSdk = 35`

### Cloud Backend & Admin Portal (`backend/`)
- **Runtime:** Core PHP 8.x (zero-framework, Hostinger/Shared-hosting & Cloud ready)
- **Database:** MySQL / MariaDB via PDO prepared statements (`u328317103_restro`)
- **Authentication:** Bearer Token (`personal_access_tokens`) + `AuthMiddleware`
- **Multi-Tenancy:** Automated restaurant isolation via `TenantMiddleware` (`restaurant_memberships`)
- **REST Modules:** `Auth`, `Menu`, `Category`, `Customer`, `Table`, `Order`, `Kot`, `Payment`, `Inventory`, `Report`, `Dashboard`, `Device`, `Subscription`, `Sync`
- **Web Admin Panel:** `backend/admin/` (Tailwind-styled PHP dashboard for Super Admins)

---

## 📂 Project Structure

```text
AISRestroOS/
├── app/                                      # Android Application (Kotlin + Jetpack Compose)
│   └── src/main/java/com/ais/restroos/
│       ├── MainActivity.kt                   # App entry point & permission/sync bootstrap
│       ├── core/
│       │   ├── auth/                         # AuthEventManager (session & token expiry handling)
│       │   ├── components/                   # Reusable UI (BottomNav, BusinessOverview, TableDialog, etc.)
│       │   ├── navigation/                   # Screen routes & NavGraph
│       │   ├── notification/                 # Local notifications
│       │   ├── permission/                   # Bluetooth & Notification runtime permissions
│       │   ├── theme/                        # Material 3 colors, typography & theme
│       │   └── utils/                        # ImageCompressor & helpers
│       ├── data/
│       │   ├── local/                        # Room AppDatabase, DAOs, Entities, PreferencesManager, HisaabSuggestions
│       │   ├── printer/                      # BluetoothPrinterService & EscPosPrinter (58mm/80mm + Unicode)
│       │   ├── remote/                       # RestroApiService, RetrofitClient & DTOs
│       │   ├── repository/                   # Menu, Order, Customer, Purchase & ReceiptSettings repositories
│       │   └── sync/                         # SyncManager & WorkManager SyncWorker
│       ├── domain/model/                     # Domain models (POS, Order, Customer, Dashboard)
│       └── feature/
│           ├── auth/                         # Login & Registration
│           ├── dashboard/                    # Home Dashboard & Live Metrics
│           ├── menu/                         # Menu Catalog & Add/Edit Dish
│           ├── orders/                       # All, On-Hold & Closed Orders
│           ├── pos/                          # POS Cart, Preferences, Customer Sheet & Bill Receipt
│           ├── printer/                      # Bluetooth Printer Discovery & Setup
│           ├── reports/                      # Daily Hisaab, Udhaar Khata, Insights, Item/Order/Expense/Stock Reports
│           └── settings/                     # More, Staff, Digital QR Menu, Receipt Settings, Plan & Support
│
└── backend/                                  # Core PHP 8.x REST API & Super Admin Panel
    ├── index.php                             # Front controller & API router
    ├── admin/                                # Super Admin Web Dashboard (restaurants, menus, plans, orders)
    ├── config/                               # Database config & example
    ├── core/                                 # PDO Database, Auth, Request, Response, Router, Validator, UUID
    ├── middleware/                           # AuthMiddleware & TenantMiddleware
    ├── modules/                              # 16 REST API Controllers (Order, KOT, Sync, Report, etc.)
    ├── database/                             # Schema docs, migrations & validation SQL
    ├── docs/                                 # API Documentation, Deployment Guide & Audit Reports
    └── tests/                                # PHP & Python automated test and schema verification suite
```

---

## 🛠️ Installation & Developer Setup

### 1. Android App Setup
1. Open `AISRestroOS` in **Android Studio** (Ladybug or newer recommended) with **JDK 17**.
2. Configure the backend base URL in `RetrofitClient.kt` if pointing to your own server or local machine.
3. Build and install via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
   Or build a minified release APK (`isMinifyEnabled = true`, `isShrinkResources = true`):
   ```bash
   ./gradlew assembleRelease
   ```

### 2. Backend API Setup (Local / Production)
1. Copy `backend/config/database.example.php` to `backend/config/database.php` and update your MySQL/MariaDB credentials.
2. Import the baseline schema from `backend/database/migrations/001_schema_baseline.sql` (and `002_daily_purchases.sql` / `FINAL_PRODUCTION_MIGRATION.sql` as applicable).
3. Start a local PHP development server:
   ```bash
   cd backend
   php -S 127.0.0.1:8000 index.php
   ```
4. For production deployment (e.g., Hostinger), refer to `backend/docs/DEPLOYMENT.md` and `backend/docs/API_DOCUMENTATION.md`.

---

## 🧪 Testing & Quality Assurance

Run the backend automated test suite locally:
```bash
cd backend
php tests/run_tests.php
```
Additional test scripts in `backend/tests/` cover:
- Authentication & incremental sync flow (`test_auth_and_sync_flow.php`)
- True concurrency & ID consistency (`test_true_concurrency.php`, `test_id_consistency.php`)
- Backup/restore & database migration verification (`test_backup_restore.php`, `verify_migration_sql.py`)

Run Android unit tests:
```bash
./gradlew test
```

---

## 📄 License & Ownership

**AISRestroOS** — Proprietary Restaurant Operating System.  
All rights reserved.
