# AISRestroOS Android Application Complete Codebase Audit

**Package:** `com.ais.restroos`  
**Target Platform:** Android (Jetpack Compose, Room, Retrofit, WorkManager, Kotlin Coroutines & Flow)  
**Production API Base URL:** `https://restro.trakiyo.in/api/` (Retrofit client points to `https://restro.trakiyo.in/api/v1/`)  
**Audit Date:** November 2026  

---

## 1. Executive Summary & Architecture Overview

AISRestroOS is an Android Restaurant Point of Sale (POS) and operations management application designed for quick-service and full-service dining establishments. The client application utilizes a modern, reactive single-activity architecture:
- **Presentation Layer:** Jetpack Compose with Material 3 theming (`com.ais.restroos.core.theme`).
- **Navigation:** Jetpack Navigation Compose with a single centralized graph (`NavGraph.kt`) and type-safe sealed destinations (`Screen.kt`).
- **State Management:** MVVM pattern utilizing Kotlin `StateFlow` and `MutableStateFlow` in ViewModels, combining Room reactive streams and network operations.
- **Persistence Layer:** Android Jetpack Room (`AppDatabase`, `MenuItemDao`, `OrderDao`), Jetpack Preferences DataStore (`PreferencesManager`).
- **Networking:** Retrofit 2 with Gson converter and OkHttp interceptors with Bearer token authentication (`RetrofitClient.kt`, `RestroApiService.kt`).
- **Hardware Integration:** ESC/POS Bluetooth classic SPP thermal receipt printer (`BluetoothPrinterService.kt`, `EscPosPrinter.kt`) supporting 58mm and 80mm paper widths.
- **Background Operations:** WorkManager (`SyncWorker.kt`, `SyncManager.kt`) for scheduled periodic and opportunistic synchronization of orders and customers.

---

## 2. Discovered Modules & Screens

The application features 22 distinct screens and modal sheets:

| Route / Screen | File Location | ViewModel / Repository | Core Purpose |
| :--- | :--- | :--- | :--- |
| `auth` | `feature/auth/AuthScreen.kt` | `AuthViewModel` | Dual-mode login & restaurant registration; token storage in DataStore. |
| `home` | `feature/dashboard/DashboardScreen.kt` | `DashboardViewModel` | Executive overview: today/yesterday sales, live order metrics, branch switch dialog, printer status pill. |
| `items` | `feature/menu/MenuScreen.kt` | `MenuRepository` | Menu catalog browser with categories filter bar, search, and grouped dish listing. |
| `menu/add` | `feature/menu/AddMenuItemScreen.kt` | `AddMenuItemViewModel` | Form for new dish creation: code, name, category, price, tax mode, stock, favorite, image. |
| `orders_closed` | `feature/orders/closed/ClosedOrdersScreen.kt` | `ClosedOrdersViewModel` | Billed/settled invoices listing, filters (last 60 mins vs all time), bulk selection, reprint bill action. |
| `orders/on-hold` | `feature/orders/hold/OnHoldOrdersScreen.kt` | `OnHoldOrdersViewModel` | Active/held orders and KOT tickets; resume to POS cart, cancel/delete held orders. |
| `pos/new` | `feature/pos/AddOrderScreen.kt` | `AddOrderViewModel` | Primary POS terminal: visual food grid, category tabs, search, voice listening toggle, cart bottom bar. |
| `pos/edit/{orderId}`| `feature/pos/AddOrderScreen.kt` | `AddOrderViewModel` | Resumes an existing held order or closed bill back into active POS cart for modification. |
| `pos/cart` | `feature/pos/CartScreen.kt` | `AddOrderViewModel` | Detailed POS cart checkout: dine-in table picker, customer attachment sheet, discounts, service charges, tax calculation, hold/complete actions. |
| `pos/preferences` | `feature/pos/OrderPreferencesScreen.kt`| N/A (UI) | Quick configuration for POS behavior (default taxes, charges). |
| `reports` | `feature/reports/ReportsScreen.kt` | `OrderRepository` | Master navigation hub for analytics: Item reports, Order reports, Stock summary. |
| `reports/items` | `feature/reports/ItemReportsScreen.kt` | `OrderRepository` | Itemized sales performance with date presets ("Today", "Yesterday", "Last 7 Days", etc.) and customer filter. |
| `reports/orders` | `feature/reports/OrderReportsScreen.kt` | `OrderRepository` | Revenue overview, total completed orders, average ticket value, and order counts. |
| `reports/stock` | `feature/reports/StockSummaryScreen.kt` | `MenuRepository` | Active dishes stock audit, low-stock warnings, and dish stock status. |
| `more` | `feature/settings/MoreScreen.kt` | `AuthViewModel`, DataStore | Restaurant profile card, offline mode toggle switch, shortcuts to CRM, marketing, staff, printer, logout. |
| `more/customers` | `feature/settings/RegularCustomersScreen.kt`| `CustomerRepository` | Searchable regular and walk-in customer directory with direct call and history shortcuts. |
| `more/staff` | `feature/settings/ManageStaffScreen.kt` | N/A (UI) | Staff member list, role permissions (Cashier, Captain, Chef). |
| `more/support` | `feature/settings/SupportScreen.kt` | N/A (UI) | Help desk, contact numbers, ticket submission. |
| `more/whatsapp` | `feature/settings/WhatsAppManagementScreen.kt`| N/A (UI) | WhatsApp Business automation, automated receipt delivery configuration. |
| `more/google-profile`| `feature/settings/GoogleProfileScreen.kt` | N/A (UI) | Google Business Profile sync and reputation/review management. |
| `more/printer` | `feature/printer/PrinterSetupScreen.kt` | `PrinterViewModel` | Bluetooth classic device scanning, paired list, paper width (58mm/80mm), auto-cut, print test ticket. |

---

## 3. Data Models & Entity Structures

### 3.1 Local Room Entities (`AppDatabase.kt`)
1. **`MenuItemEntity`** (`tableName = "menu_items"`):
   - `id: String` (Primary Key, UUID string)
   - `name: String`
   - `category: String`
   - `price: Double`
   - `isAvailable: Boolean`
   - `isVeg: Boolean`
2. **`OrderEntity`** (`tableName = "orders"`):
   - `orderId: String` (Primary Key, e.g. "HOLD-1234", "INV-5678", or UUID)
   - `tableNumber: String`
   - `itemCount: Int`
   - `totalAmount: Double`
   - `status: String` ("ON_HOLD", "COMPLETED", "CANCELLED")
   - `itemsJson: String` (Serialized JSON string of ordered items)
   - `timestamp: Long`

### 3.2 In-Memory Domain Models
- **`POSMenuItem`**: `id`, `code`, `name`, `category`, `price`, `imageUrl`, `isAvailable`, `isVeg`, `isFavourite`.
- **`CartItem`**: `menuItem: POSMenuItem`, `quantity: Int`, `totalPrice: Double`.
- **`RestaurantTable`**: `number: String`, `isOccupied: Boolean`.
- **`Customer`**: `id: String`, `name: String`, `mobileNumber: String`, `isRegular: Boolean`, `createdAt: Long`.
- **`HeldOrder`**: `orderId`, `tableNumber`, `totalAmount`, `items: List<OrderItem>`, `status = OrderStatus.ON_HOLD`, `timestamp`.
- **`ClosedOrder`**: `orderId`, `invoiceNumber`, `tableNumber`, `totalAmount`, `items: List<OrderItem>`, `status = OrderStatus.COMPLETED`, `createdTimestamp`, `completedTimestamp`, `paymentMethod`, `isSelected`.
- **`DashboardOverview`**: `todaySales: Double`, `todayOrders: Int`, `yesterdaySales: Double`, `yesterdayOrders: Int`.
- **`RestaurantBranch`**: `id: String`, `name: String`, `subtitle: String`, `isSelected: Boolean`.
- **`PrinterDeviceInfo`**: `name: String`, `macAddress: String`, `isPaired: Boolean`, `isConnected: Boolean`.

### 3.3 Remote Network DTOs (`com.ais.restroos.data.remote.dtos`)
- **`ApiResponseWrapper<T>`**: `status: String` ("success" / "error"), `message: String`, `data: T?`, `errors: Map<String, List<String>>?`.
- **`RegisterRequest`**: `name`, `email`, `password`, `restaurant_name`, `phone?`.
- **`LoginRequest`**: `email`, `password`.
- **`AuthData`**: `token`, `user: UserDto(id, name, email, phone)`, `restaurant_id`, `branch_id`, `role`.
- **`CategorySyncDto`**: `id: String`, `name: String`, `display_order: Int`.
- **`MenuItemSyncDto`**: `id: String`, `code: String?`, `name: String`, `category: String`, `price: Double`, `tax_mode: String`, `tax_percentage: String`, `current_stock: Int`, `is_default_tax: Boolean`, `is_favourite: Boolean`, `image_url: String?`.
- **`OrderSyncDto`**: `uuid: String`, `order_number: String`, `order_type: String`, `status: String`, `table_number: String?`, `customer_name: String?`, `customer_phone: String?`, `subtotal: Double`, `discount_amount: Double`, `tax_amount: Double`, `service_charge: Double`, `grand_total: Double`, `items: List<OrderItemSyncDto>`.
- **`OrderItemSyncDto`**: `item_name: String`, `unit_price: Double`, `quantity: Int`.
- **`CustomerSyncDto`**: `id: String?`, `name: String`, `mobile_number: String`, `is_regular: Boolean`.
- **`SyncPushRequest`**: `orders: List<OrderSyncDto>`, `customers: List<CustomerSyncDto>`.
- **`SyncPushResult`**: `latest_cursor: Long`, `synced_uuids: List<String>`, `failed_uuids: List<String>`, `conflicts: List<String>`.
- **`SyncPullResult`**: `latest_cursor: Long`, `menu_items: List<MenuItemSyncDto>`, `categories: List<CategorySyncDto>`, `customers: List<CustomerSyncDto>`, `tombstones: List<TombstoneSyncDto>`.
- **`TombstoneSyncDto`**: `entity_type: String`, `entity_uuid: String`, `sync_seq: Long`.
- **`SalesReportSummaryDto`**: `total_revenue: Double`, `total_tax: Double`, `total_discount: Double`, `completed_orders_count: Int`, `average_order_value: Double`.
- **`ItemSalesReportDto`**: `item_name: String`, `total_quantity: Int`, `total_amount: Double`.

---

## 4. Hardware & Printing Subsystem

1. **Protocol:** ESC/POS binary command stream over standard Bluetooth Classic Serial Port Profile (SPP) UUID `00001101-0000-1000-8000-00805F9B34FB`.
2. **Supported Formatters:**
   - `buildTestReceipt()`: Diagnostic banner, connection status, paper width verification.
   - `buildOrderBillReceipt()`: Formatted customer tax invoice (restaurant header, invoice number, table label, date/time, itemized columns `Item (Qty) | Amount`, grand total, footer). Note that payment mode is excluded as per task specifications.
   - `buildKotReceipt()`: Kitchen Order Ticket with KOT Number, date/time, table number, itemized table `ITEM | QTY`, large kitchen copy banner.
3. **Numbering Safety:**
   - Bill and KOT sequences must be generated sequentially per restaurant.
   - Reprints MUST retain the identical existing invoice/KOT number and NOT generate a new sequence ID.

---

## 5. Offline & Synchronization Architecture

1. **Offline Mode:** The user can toggle "Use Offline" in `MoreScreen.kt`. When offline or network is intermittent, `OrderRepository` and `MenuRepository` operate against in-memory state and Room local caches.
2. **WorkManager Periodic Sync:** `SyncWorker` fires every 15 minutes when connected to network:
   - Collects pending held orders.
   - Converts to `OrderSyncDto`.
   - Sends an idempotent POST to `/sync/push` with header `X-Idempotency-Key: UUID`.
3. **Delta Sync Protocol:**
   - `/sync/pull?cursor={cursor}` returns items modified after `cursor`, along with deleted records via `tombstones`.
   - `/sync/push` applies changes in an atomic transaction and responds with `latest_cursor`, `synced_uuids`, and `failed_uuids`.

---

## 6. Required Backend REST APIs

The backend must provide endpoints that directly service the Retrofit definitions, plus the extended POS operations:

| HTTP Method | API Route | Handled DTO / Body | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | None | API health status. |
| `POST`| `/auth/register` | `RegisterRequest` | Registers new user and restaurant; issues token. |
| `POST`| `/auth/login` | `LoginRequest` | Authenticates email & password; issues Bearer token. |
| `POST`| `/auth/logout` | Bearer Token | Revokes active personal access token. |
| `GET` | `/auth/me` | Bearer Token | Returns current user, membership, restaurant, and branch. |
| `GET` | `/categories` | Bearer Token | Lists restaurant categories. |
| `POST`| `/categories` | Category payload | Creates a new category. |
| `GET` | `/menu-items` | Bearer Token | Lists all active menu items for the restaurant. |
| `POST`| `/menu-items` | `MenuItemSyncDto` | Creates or updates a menu item. |
| `POST`| `/menu-items/upload-image` | Multipart `image` | Stores dish photo, returns public URL and relative path. |
| `GET` | `/customers` | Bearer Token | Fetches registered regular and walk-in customers. |
| `POST`| `/customers` | `CustomerSyncDto` | Upserts customer record by mobile number. |
| `GET` | `/orders` | Query `status` | Fetches orders by status (`ON_HOLD`, `COMPLETED`, `ALL`). |
| `POST`| `/orders` | `OrderSyncDto` | Records a new order (held or completed) with items. |
| `GET` | `/branches/{branchId}/overview` | Path `branchId` | Today & yesterday sales and order count metrics. |
| `GET` | `/reports/sales` | Bearer Token | Total revenue, tax, discounts, order counts, average order value. |
| `GET` | `/reports/itemized` | Bearer Token | Item-level sales aggregation (`item_name`, `total_quantity`, `total_amount`). |
| `POST`| `/sync/push` | `SyncPushRequest` + Header | Idempotent bulk sync push for offline orders/customers. |
| `GET` | `/sync/pull` | Query `cursor` | Incremental delta pull of menu items, categories, customers, tombstones. |
| `GET` | `/tables` | Bearer Token | List tables and occupancy status. |
| `POST`| `/kot` | KOT payload | Generates sequential KOT ticket for kitchen. |
| `POST`| `/kot/{id}/reprint` | Path `id` | Marks KOT reprint without renumbering. |
| `GET` | `/subscriptions` | Bearer Token | Current subscription plan status. |
| `GET` | `/devices` | Bearer Token | Registered Bluetooth/network printers for the restaurant. |

---

## 7. Security & Tenancy Invariants

1. **Strict Restaurant Scoping:** Every authenticated request resolves `user_id` -> `restaurant_memberships` -> `restaurant_id` & `branch_id`. Any query executed MUST append `WHERE restaurant_id = :restaurant_id`.
2. **Never Trust Client Restaurant ID:** Any `restaurant_id` passed in the JSON payload or query parameters must be ignored or strictly validated against the token's authenticated tenancy.
3. **Password Security:** Use PHP `password_hash($pwd, PASSWORD_BCRYPT)` and `password_verify()` to ensure seamless parity with existing Laravel users.
4. **Token Security:** Store SHA-256 token hash in `personal_access_tokens.token` matching Laravel Sanctum conventions; never return raw database hashes.
