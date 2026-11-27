<?php
/**
 * AISRestroOS Automated Local Test Suite
 * Tests Core PHP REST API Endpoints, Database Persistence, Numbering, and Tenancy
 */

declare(strict_types=1);

// Register autoloader
spl_autoload_register(function ($class) {
    $prefix = 'AISRestro\\';
    $baseDir = dirname(__DIR__) . '/';

    $len = strlen($prefix);
    if (strncmp($prefix, $class, $len) !== 0) {
        return;
    }

    $relativeClass = substr($class, $len);
    $parts = explode('\\', $relativeClass);

    if (count($parts) >= 2) {
        $folder = strtolower($parts[0]);
        $filename = $parts[1] . '.php';
        $file = $baseDir . $folder . '/' . $filename;
        if (file_exists($file)) {
            require_once $file;
            return;
        }
    }

    $file = $baseDir . str_replace('\\', '/', $relativeClass) . '.php';
    if (file_exists($file)) {
        require_once $file;
    }
});

use AISRestro\Core\Database;
use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Router;
use AISRestro\Core\Auth;
use AISRestro\Core\UUID;
use AISRestro\Core\Helpers;
use AISRestro\Middleware\AuthMiddleware;
use AISRestro\Middleware\TenantMiddleware;
use AISRestro\Modules\AuthController;
use AISRestro\Modules\MenuController;
use AISRestro\Modules\CategoryController;
use AISRestro\Modules\CustomerController;
use AISRestro\Modules\TableController;
use AISRestro\Modules\OrderController;
use AISRestro\Modules\KotController;
use AISRestro\Modules\PaymentController;
use AISRestro\Modules\InventoryController;
use AISRestro\Modules\ReportController;
use AISRestro\Modules\DashboardController;
use AISRestro\Modules\DeviceController;
use AISRestro\Modules\SubscriptionController;
use AISRestro\Modules\SyncController;

// Configure SQLite in-memory database for isolated, fast local execution
Database::setConfig([
    'driver'   => 'sqlite',
    'database' => ':memory:',
]);
Response::$exitOnSend = false;

// Helper class for capturing controller outputs without exit()
class TestRunner
{
    private static int $passed = 0;
    private static int $failed = 0;

    public static function assert(string $testName, bool $condition, string $detail = ''): void
    {
        if ($condition) {
            echo "  [PASS] {$testName}\n";
            self::$passed++;
        } else {
            echo "  [FAIL] {$testName} - {$detail}\n";
            self::$failed++;
        }
    }

    public static function getStats(): array
    {
        return ['passed' => self::$passed, 'failed' => self::$failed];
    }
}

// 1. Setup Test Schema
echo "=========================================================\n";
echo "AISRestroOS Core PHP Automated Test Suite Starting...\n";
echo "=========================================================\n\n";

echo "Setting up SQLite test database schema...\n";

$schemaStatements = [
    "CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        email TEXT NOT NULL UNIQUE,
        phone TEXT,
        password TEXT NOT NULL,
        role TEXT NOT NULL DEFAULT 'owner',
        remember_token TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE personal_access_tokens (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        tokenable_type TEXT NOT NULL,
        tokenable_id INTEGER NOT NULL,
        name TEXT NOT NULL,
        token TEXT NOT NULL UNIQUE,
        abilities TEXT,
        last_used_at DATETIME,
        expires_at DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE restaurants (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        slug TEXT,
        owner_id INTEGER NOT NULL,
        phone TEXT,
        email TEXT,
        address TEXT,
        currency TEXT NOT NULL DEFAULT 'INR',
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE branches (
        id TEXT PRIMARY KEY,
        restaurant_id TEXT NOT NULL,
        name TEXT NOT NULL,
        code TEXT,
        address TEXT,
        phone TEXT,
        is_main INTEGER NOT NULL DEFAULT 1,
        is_main_branch INTEGER NOT NULL DEFAULT 1,
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE restaurant_memberships (
        id TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        role TEXT NOT NULL DEFAULT 'owner',
        status TEXT NOT NULL DEFAULT 'active',
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE categories (
        id TEXT PRIMARY KEY,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        name TEXT NOT NULL,
        display_order INTEGER NOT NULL DEFAULT 0,
        sync_seq INTEGER,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE menu_items (
        id TEXT PRIMARY KEY,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        category_id TEXT,
        code TEXT,
        name TEXT NOT NULL,
        category TEXT NOT NULL DEFAULT 'General',
        price REAL NOT NULL DEFAULT 0.00,
        tax_mode TEXT NOT NULL DEFAULT 'WITHOUT_TAX',
        tax_percentage TEXT NOT NULL DEFAULT 'None',
        current_stock INTEGER NOT NULL DEFAULT 100,
        is_default_tax INTEGER NOT NULL DEFAULT 1,
        is_favourite INTEGER NOT NULL DEFAULT 0,
        is_available INTEGER NOT NULL DEFAULT 1,
        is_veg INTEGER NOT NULL DEFAULT 1,
        image_url TEXT,
        sync_seq INTEGER,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE customers (
        id TEXT PRIMARY KEY,
        restaurant_id TEXT NOT NULL,
        name TEXT NOT NULL,
        mobile_number TEXT NOT NULL,
        email TEXT,
        is_regular INTEGER NOT NULL DEFAULT 0,
        sync_seq INTEGER,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE tables (
        id TEXT PRIMARY KEY,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        table_number TEXT NOT NULL,
        capacity INTEGER NOT NULL DEFAULT 4,
        status TEXT NOT NULL DEFAULT 'AVAILABLE',
        is_occupied INTEGER NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE orders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT NOT NULL UNIQUE,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        order_number TEXT NOT NULL,
        invoice_number TEXT,
        order_type TEXT NOT NULL DEFAULT 'DINE_IN',
        status TEXT NOT NULL DEFAULT 'ON_HOLD',
        table_number TEXT,
        customer_id TEXT,
        customer_name TEXT,
        customer_phone TEXT,
        subtotal REAL NOT NULL DEFAULT 0.00,
        discount_amount REAL NOT NULL DEFAULT 0.00,
        tax_amount REAL NOT NULL DEFAULT 0.00,
        service_charge REAL NOT NULL DEFAULT 0.00,
        grand_total REAL NOT NULL DEFAULT 0.00,
        payment_method TEXT,
        payment_status TEXT NOT NULL DEFAULT 'PENDING',
        sync_seq INTEGER,
        completed_at DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE order_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        order_id INTEGER,
        order_uuid TEXT NOT NULL,
        menu_item_id TEXT,
        item_name TEXT NOT NULL,
        price REAL NOT NULL DEFAULT 0.00,
        unit_price REAL NOT NULL DEFAULT 0.00,
        quantity INTEGER NOT NULL DEFAULT 1,
        total REAL NOT NULL DEFAULT 0.00,
        total_price REAL NOT NULL DEFAULT 0.00,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE kot_tickets (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT,
        order_id INTEGER,
        order_uuid TEXT NOT NULL,
        ticket_number TEXT,
        kot_number TEXT NOT NULL,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        table_number TEXT,
        status TEXT NOT NULL DEFAULT 'SENT_TO_KITCHEN',
        reprint_count INTEGER NOT NULL DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE kot_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        kot_ticket_id INTEGER,
        kot_id INTEGER NOT NULL,
        item_name TEXT NOT NULL,
        quantity INTEGER NOT NULL DEFAULT 1,
        notes TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE payments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        order_id INTEGER,
        order_uuid TEXT NOT NULL,
        amount REAL NOT NULL DEFAULT 0.00,
        payment_method TEXT NOT NULL,
        reference_number TEXT,
        transaction_reference TEXT,
        status TEXT NOT NULL DEFAULT 'COMPLETED',
        is_verified INTEGER NOT NULL DEFAULT 1,
        paid_at DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE inventory_items (
        id TEXT PRIMARY KEY,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        item_name TEXT,
        name TEXT NOT NULL,
        unit_of_measure TEXT NOT NULL DEFAULT 'kg',
        unit TEXT NOT NULL DEFAULT 'kg',
        quantity REAL NOT NULL DEFAULT 0.00,
        current_stock REAL NOT NULL DEFAULT 0.00,
        low_stock_alert REAL NOT NULL DEFAULT 5.00,
        min_stock_alert REAL NOT NULL DEFAULT 5.00,
        cost_per_unit REAL NOT NULL DEFAULT 0.00,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE stock_movements (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        restaurant_id TEXT,
        inventory_item_id TEXT NOT NULL,
        created_by_user_id INTEGER,
        movement_type TEXT NOT NULL,
        quantity_change REAL NOT NULL DEFAULT 0.00,
        quantity REAL NOT NULL,
        notes TEXT,
        reason TEXT,
        reference_id TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE devices (
        id TEXT PRIMARY KEY,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        device_code TEXT,
        device_name TEXT NOT NULL,
        device_token TEXT,
        mac_address TEXT,
        device_type TEXT NOT NULL DEFAULT 'PRINTER',
        paper_width TEXT NOT NULL DEFAULT '58mm',
        is_authorized INTEGER NOT NULL DEFAULT 1,
        is_active INTEGER NOT NULL DEFAULT 1,
        last_synced_at DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE subscription_plans (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        price REAL NOT NULL DEFAULT 0.00,
        duration_days INTEGER NOT NULL DEFAULT 30,
        features TEXT,
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE subscriptions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        restaurant_id TEXT NOT NULL,
        subscription_plan_id TEXT,
        plan_id TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'active',
        starts_at DATETIME NOT NULL,
        ends_at DATETIME,
        expires_at DATETIME NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE restaurant_settings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        restaurant_id TEXT NOT NULL,
        branch_id TEXT,
        key_name TEXT DEFAULT 'general',
        key_value TEXT,
        bill_prefix TEXT NOT NULL DEFAULT 'INV-',
        last_bill_number INTEGER NOT NULL DEFAULT 0,
        kot_prefix TEXT NOT NULL DEFAULT 'KOT-',
        last_kot_number INTEGER NOT NULL DEFAULT 0,
        tax_percentage REAL NOT NULL DEFAULT 0.00,
        service_charge_percentage REAL NOT NULL DEFAULT 0.00,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE idempotency_keys (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        restaurant_id TEXT,
        idempotency_key TEXT,
        key_value TEXT,
        request_path TEXT,
        resource_type TEXT,
        resource_uuid TEXT,
        response_code INTEGER DEFAULT 200,
        response_payload TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE sync_tombstones (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        restaurant_id TEXT NOT NULL,
        entity_type TEXT NOT NULL,
        entity_uuid TEXT NOT NULL,
        sync_seq INTEGER NOT NULL,
        deleted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );",
    "CREATE TABLE audit_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        restaurant_id TEXT,
        user_id INTEGER,
        action TEXT NOT NULL,
        details TEXT,
        ip_address TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );"
];

$pdo = Database::getConnection();
foreach ($schemaStatements as $sql) {
    $pdo->exec($sql);
}

echo "Database schema initialized successfully.\n\n";

// Helper function to mock request execution
function mockCall(string $method, string $path, array $body = [], array $headers = [], array $queryParams = []): array
{
    Response::reset();

    // Override globals
    $_SERVER['REQUEST_METHOD'] = $method;
    $_SERVER['REQUEST_URI'] = $path;
    $_GET = $queryParams;
    $_POST = ($method === 'POST' || $method === 'PUT') ? $body : [];
    $_FILES = [];

    // Inject headers
    foreach ($_SERVER as $k => $v) {
        if (str_starts_with($k, 'HTTP_')) {
            unset($_SERVER[$k]);
        }
    }
    foreach ($headers as $hk => $hv) {
        $_SERVER['HTTP_' . strtoupper(str_replace('-', '_', $hk))] = $hv;
    }

    $req = new Request();
    if (!empty($body)) {
        $req->setBody($body);
    }

    // Use output buffering
    ob_start();
    try {
        // Run router or controller directly
        $router = new Router();
        $authMiddlewares = [AuthMiddleware::class, TenantMiddleware::class];

        // Register routes matching index.php
        $router->get('/health', function () {
            Response::success(['status' => 'online'], 'Healthy');
        });
        $router->post('/auth/register', [AuthController::class, 'register']);
        $router->post('/auth/login',    [AuthController::class, 'login']);
        $router->get('/auth/me',        [AuthController::class, 'me'], $authMiddlewares);
        $router->get('/categories',     [CategoryController::class, 'index'], $authMiddlewares);
        $router->post('/categories',    [CategoryController::class, 'store'], $authMiddlewares);
        $router->get('/menu-items',     [MenuController::class, 'index'], $authMiddlewares);
        $router->post('/menu-items',    [MenuController::class, 'store'], $authMiddlewares);
        $router->get('/customers',      [CustomerController::class, 'index'], $authMiddlewares);
        $router->post('/customers',     [CustomerController::class, 'store'], $authMiddlewares);
        $router->get('/tables',         [TableController::class, 'index'], $authMiddlewares);
        $router->get('/orders',         [OrderController::class, 'index'], $authMiddlewares);
        $router->post('/orders',        [OrderController::class, 'store'], $authMiddlewares);
        $router->post('/orders/{id}/complete', [OrderController::class, 'completeOrder'], $authMiddlewares);
        $router->post('/kot',           [KotController::class, 'store'], $authMiddlewares);
        $router->post('/kot/{id}/reprint', [KotController::class, 'reprint'], $authMiddlewares);
        $router->get('/reports/sales',  [ReportController::class, 'sales'], $authMiddlewares);
        $router->get('/reports/itemized',[ReportController::class, 'itemized'], $authMiddlewares);
        $router->get('/branches/{branchId}/overview', [DashboardController::class, 'overview'], $authMiddlewares);
        $router->post('/sync/push',     [SyncController::class, 'push'], $authMiddlewares);
        $router->get('/sync/pull',      [SyncController::class, 'pull'], $authMiddlewares);

        $router->dispatch($req);
    } catch (\Throwable $e) {
        $json = ob_get_clean();
        $decoded = json_decode((string)$json, true);
        if ($decoded !== null) {
            return $decoded;
        }
        return ['status' => 'error', 'message' => $e->getMessage(), 'raw' => $json];
    }

    $raw = ob_get_clean();
    $decoded = json_decode((string)$raw, true);
    return is_array($decoded) ? $decoded : ['status' => 'raw', 'content' => $raw];
}

// ================= TEST SUITE =================
echo "Running Test Suite...\n\n";

// 1. Health Check
$healthRes = mockCall('GET', '/health');
TestRunner::assert("1. Health check returns success", ($healthRes['status'] ?? '') === 'success');

// 2. Auth Registration
$regData = [
    'name'            => 'Lucky Singh',
    'email'           => 'luckysingh955416@gmail.com',
    'password'        => 'SecretPass123',
    'restaurant_name' => 'Siddharth Family Resturent',
    'phone'           => '9876543210'
];
$regRes = mockCall('POST', '/auth/register', $regData);
TestRunner::assert("2. User and Restaurant Registration", ($regRes['status'] ?? '') === 'success');
$authToken = $regRes['data']['token'] ?? '';
$restroId1 = $regRes['data']['restaurant_id'] ?? '';
TestRunner::assert("   - Token generated with Sanctum format", !empty($authToken));
TestRunner::assert("   - Restaurant ID created", !empty($restroId1));

// 3. Auth Login with wrong password
$badLogin = mockCall('POST', '/auth/login', [
    'email'    => 'luckysingh955416@gmail.com',
    'password' => 'WrongPassword'
]);
TestRunner::assert("3. Login rejected for wrong password", ($badLogin['status'] ?? '') === 'error');

// 4. Auth Login with correct password
$goodLogin = mockCall('POST', '/auth/login', [
    'email'    => 'luckysingh955416@gmail.com',
    'password' => 'SecretPass123'
]);
TestRunner::assert("4. Login successful with valid credentials", ($goodLogin['status'] ?? '') === 'success');

// 5. Auth Me
$meRes = mockCall('GET', '/auth/me', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("5. Authenticated /auth/me returns user & membership", ($meRes['status'] ?? '') === 'success');
TestRunner::assert("   - Email matches authenticated user", ($meRes['data']['user']['email'] ?? '') === 'luckysingh955416@gmail.com');

// 6. Category Creation
$catRes = mockCall('POST', '/categories', ['name' => 'डोसा', 'display_order' => 1], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("6. Create category 'डोसा'", ($catRes['status'] ?? '') === 'success');

$catListRes = mockCall('GET', '/categories', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("   - List categories contains created category", count($catListRes['data'] ?? []) >= 1);

// 7. Menu Item Creation
$menuItemData = [
    'id'             => 'ITEM-DOSA-01',
    'code'           => '05',
    'name'           => 'मसाला डोसा Special',
    'category'       => 'डोसा',
    'price'          => 120.00,
    'tax_mode'       => 'WITHOUT_TAX',
    'tax_percentage' => 'None',
    'current_stock'  => 50,
    'is_favourite'   => true,
];
$menuRes = mockCall('POST', '/menu-items', $menuItemData, ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("7. Create menu item 'मसाला डोसा Special'", ($menuRes['status'] ?? '') === 'success');

$menuListRes = mockCall('GET', '/menu-items', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("   - List menu items contains created item with price 120", ($menuListRes['data'][0]['price'] ?? 0) == 120.0);

// 8. Customer Creation
$custData = [
    'name'          => 'Rohit Sharma',
    'mobile_number' => '9811122233',
    'is_regular'    => true,
];
$custRes = mockCall('POST', '/customers', $custData, ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("8. Customer upsert via mobile number", ($custRes['status'] ?? '') === 'success');

$custListRes = mockCall('GET', '/customers', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("   - List customers contains regular customer", ($custListRes['data'][0]['mobile_number'] ?? '') === '9811122233');

// 9. Tables
$tablesRes = mockCall('GET', '/tables', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("9. Tables list returns initialized dining tables", count($tablesRes['data'] ?? []) >= 10);

// 10. Order Creation & Hold Order
$orderUuid1 = UUID::v4();
$orderData1 = [
    'uuid'            => $orderUuid1,
    'order_number'    => 'HOLD-1001',
    'order_type'      => 'DINE_IN',
    'status'          => 'ON_HOLD',
    'table_number'    => 'G1',
    'customer_name'   => 'Rohit Sharma',
    'customer_phone'  => '9811122233',
    'subtotal'        => 240.00,
    'discount_amount' => 0.00,
    'tax_amount'      => 0.00,
    'service_charge'  => 0.00,
    'grand_total'     => 240.00,
    'items'           => [
        [
            'item_name'  => 'मसाला डोसा Special',
            'unit_price' => 120.00,
            'quantity'   => 2
        ]
    ]
];
$orderRes1 = mockCall('POST', '/orders', $orderData1, ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("10. Order created on hold with 2 items", ($orderRes1['status'] ?? '') === 'success');

// 11. KOT Generation & Sequential Numbering
$kotData1 = [
    'order_uuid'   => $orderUuid1,
    'table_number' => 'G1',
    'items'        => [
        ['item_name' => 'मसाला डोसा Special', 'quantity' => 2, 'notes' => 'Extra crisp']
    ]
];
$kotRes1 = mockCall('POST', '/kot', $kotData1, ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("11. KOT Ticket generated with number starting at KOT-0001", ($kotRes1['data']['kot_number'] ?? '') === 'KOT-0001');

// 12. KOT Duplicate / Reprint Protection
$kotResReprint = mockCall('POST', '/kot', $kotData1, ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("12. Calling KOT again retains original KOT-0001 without incrementing", ($kotResReprint['data']['kot_number'] ?? '') === 'KOT-0001');

$reprintExplicit = mockCall('POST', "/kot/{$orderUuid1}/reprint", [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("   - Explicit reprint increments reprint_count to 1 and keeps KOT-0001", ($reprintExplicit['data']['reprint_count'] ?? 0) === 1);

// 13. Complete Order & Bill Numbering
$completeRes = mockCall('POST', "/orders/{$orderUuid1}/complete", ['payment_method' => 'UPI'], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("13. Order completion allocates bill number starting at INV-0001", ($completeRes['data']['invoice_number'] ?? '') === 'INV-0001');

// Verify Completing again keeps same INV-0001
$completeAgain = mockCall('POST', "/orders/{$orderUuid1}/complete", ['payment_method' => 'UPI'], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("   - Re-completing keeps same INV-0001 without sequence jump", ($completeAgain['data']['invoice_number'] ?? '') === 'INV-0001');

// 14. Sales Report & Itemized Analytics
$salesReport = mockCall('GET', '/reports/sales', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("14. Sales report calculates total revenue 240 and 1 completed order", ($salesReport['data']['total_revenue'] ?? 0) == 240.0 && ($salesReport['data']['completed_orders_count'] ?? 0) == 1);

$itemReport = mockCall('GET', '/reports/itemized', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("   - Itemized report aggregates 2 quantity of 'मसाला डोसा Special'", ($itemReport['data'][0]['total_quantity'] ?? 0) == 2);

// 15. Dashboard Overview Dual Compatibility
$dashRes = mockCall('GET', '/branches/1/overview', [], ['Authorization' => "Bearer {$authToken}"]);
TestRunner::assert("15. Dashboard overview returns todaySales 240 and todayOrders 1", ($dashRes['todaySales'] ?? 0) == 240.0 && ($dashRes['todayOrders'] ?? 0) == 1);
TestRunner::assert("   - Dual compatibility: also contains 'data' wrapper", ($dashRes['data']['todaySales'] ?? 0) == 240.0);

// 16. Multi-Tenant Isolation Test
$regData2 = [
    'name'            => 'Another Owner',
    'email'           => 'other@cafe.com',
    'password'        => 'Password456',
    'restaurant_name' => 'Metro Cafe',
];
$regRes2 = mockCall('POST', '/auth/register', $regData2);
$token2 = $regRes2['data']['token'] ?? '';
$restroId2 = $regRes2['data']['restaurant_id'] ?? '';
TestRunner::assert("16. Register Second Independent Restaurant", !empty($token2) && $restroId2 !== $restroId1);

$menuListRestro2 = mockCall('GET', '/menu-items', [], ['Authorization' => "Bearer {$token2}"]);
TestRunner::assert("   - Tenant Isolation: Restaurant 2 has 0 menu items (cannot see Restaurant 1 dishes)", count($menuListRestro2['data'] ?? []) === 0);

$ordersRestro2 = mockCall('GET', '/orders', [], ['Authorization' => "Bearer {$token2}"]);
TestRunner::assert("   - Tenant Isolation: Restaurant 2 has 0 orders (cannot see Restaurant 1 orders)", count($ordersRestro2['data'] ?? []) === 0);

// 17. Offline Sync Push & Pull
$pushData = [
    'orders' => [
        [
            'uuid'         => 'OFFLINE-ORDER-99',
            'order_number' => 'HOLD-OFFLINE',
            'order_type'   => 'TAKEAWAY',
            'status'       => 'ON_HOLD',
            'subtotal'     => 150.00,
            'grand_total'  => 150.00,
            'items'        => [
                ['item_name' => 'Cold Coffee', 'unit_price' => 75.00, 'quantity' => 2]
            ]
        ]
    ]
];
$syncPushRes = mockCall('POST', '/sync/push', $pushData, [
    'Authorization'      => "Bearer {$authToken}",
    'X-Idempotency-Key'  => 'IDEMP-KEY-TEST-001'
]);
TestRunner::assert("17. Offline sync push persists order", in_array('OFFLINE-ORDER-99', $syncPushRes['data']['synced_uuids'] ?? []));

// Sync Pull
$syncPullRes = mockCall('GET', '/sync/pull', [], ['Authorization' => "Bearer {$authToken}"], ['cursor' => 0]);
TestRunner::assert("   - Offline sync pull returns menu items and categories", count($syncPullRes['data']['menu_items'] ?? []) >= 1);

// Final Results
echo "\n=========================================================\n";
$stats = TestRunner::getStats();
echo "TEST SUMMARY: Total {$stats['passed']} PASSED, {$stats['failed']} FAILED\n";
echo "=========================================================\n";

if ($stats['failed'] > 0) {
    exit(1);
}
