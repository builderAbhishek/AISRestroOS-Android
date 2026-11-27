<?php
/**
 * AISRestroOS Custom Core PHP REST API Front Controller
 * Environment: Hostinger PHP 8.x + MySQL
 * Base URL: https://restro.trakiyo.in/api/
 */

declare(strict_types=1);

// PSR-4 style autoloader for AISRestro namespace
spl_autoload_register(function ($class) {
    $prefix = 'AISRestro\\';
    $baseDir = __DIR__ . '/';

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

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Router;
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
use AISRestro\Modules\RestaurantController;
use AISRestro\Modules\StaffController;

// Enable CORS and security headers
Helpers::setCorsHeaders();

// Global Exception & Error Handler
set_exception_handler(function (\Throwable $e) {
    error_log("[Uncaught Exception] " . $e->getMessage() . " in " . $e->getFile() . ":" . $e->getLine());
    Response::error("An internal server error occurred.", 500);
});

set_error_handler(function ($errno, $errstr, $errfile, $errline) {
    if (!(error_reporting() & $errno)) {
        return false;
    }
    error_log("[PHP Error {$errno}] {$errstr} in {$errfile}:{$errline}");
    return true;
});

$request = new Request();
$router  = new Router();

// Static Image / Upload Serving (Fallback if Apache passes through or direct API request)
$reqPath = $request->getPath();
if (strpos($reqPath, 'uploads/') !== false) {
    Helpers::setCorsHeaders();
    $uploadRelative = substr($reqPath, strpos($reqPath, 'uploads/'));
    $uploadsRoot = realpath(__DIR__ . '/uploads');
    $candidatePath = $uploadsRoot !== false ? realpath(__DIR__ . '/' . $uploadRelative) : false;

    if ($uploadsRoot !== false && $candidatePath !== false && str_starts_with($candidatePath, $uploadsRoot . DIRECTORY_SEPARATOR) && is_file($candidatePath)) {
        $finfo = new \finfo(FILEINFO_MIME_TYPE);
        $mime = $finfo->file($candidatePath) ?: 'application/octet-stream';
        $allowedPublicMimes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
        if (!in_array($mime, $allowedPublicMimes, true)) {
            Response::error('Forbidden file type', 403);
            exit;
        }
        header("Content-Type: {$mime}");
        header("Content-Length: " . filesize($candidatePath));
        header("Cache-Control: public, max-age=604800");
        header("X-Content-Type-Options: nosniff");
        readfile($candidatePath);
        exit;
    } else {
        Response::error('Image file not found on server', 404);
        exit;
    }
}

// Middleware references
$authTenantMiddlewares = [
    AuthMiddleware::class,
    TenantMiddleware::class,
];

// --- 1. HEALTH & DIAGNOSTICS ---
$router->get('/health', function (Request $req) {
    Response::success([
        'status'  => 'online',
        'service' => 'AISRestroOS Core PHP REST API',
        'version' => '1.0.0',
        'runtime' => 'PHP ' . PHP_VERSION,
        'time'    => date('Y-m-d H:i:s'),
    ], 'AISRestroOS backend is healthy');
});

// --- 2. AUTHENTICATION MODULE ---
$router->post('/auth/register', [AuthController::class, 'register']);
$router->post('/auth/login',    [AuthController::class, 'login']);
$router->post('/auth/logout',   [AuthController::class, 'logout'], [AuthMiddleware::class]);
$router->get('/auth/me',        [AuthController::class, 'me'],     $authTenantMiddlewares);

// --- 3. CATEGORIES MODULE ---
$router->get('/categories',        [CategoryController::class, 'index'],   $authTenantMiddlewares);
$router->post('/categories',       [CategoryController::class, 'store'],   $authTenantMiddlewares);
$router->delete('/categories/{id}',[CategoryController::class, 'destroy'], $authTenantMiddlewares);

// --- 4. MENU ITEMS MODULE ---
$router->get('/menu-items',              [MenuController::class, 'index'],       $authTenantMiddlewares);
$router->post('/menu-items',             [MenuController::class, 'store'],       $authTenantMiddlewares);
$router->get('/menu-items/{id}',         [MenuController::class, 'show'],        $authTenantMiddlewares);
$router->delete('/menu-items/{id}',      [MenuController::class, 'destroy'],     $authTenantMiddlewares);
$router->post('/menu-items/upload-image', [MenuController::class, 'uploadImage'], $authTenantMiddlewares);

// --- 5. CUSTOMERS MODULE ---
$router->get('/customers',        [CustomerController::class, 'index'],   $authTenantMiddlewares);
$router->post('/customers',       [CustomerController::class, 'store'],   $authTenantMiddlewares);
$router->get('/customers/{id}',   [CustomerController::class, 'show'],    $authTenantMiddlewares);
$router->delete('/customers/{id}',[CustomerController::class, 'destroy'], $authTenantMiddlewares);

// --- 6. TABLES MODULE ---
$router->get('/tables',  [TableController::class, 'index'], $authTenantMiddlewares);
$router->post('/tables', [TableController::class, 'store'], $authTenantMiddlewares);

// --- 7. ORDERS & BILLING MODULE ---
$router->get('/orders',                 [OrderController::class, 'index'],         $authTenantMiddlewares);
$router->post('/orders',                [OrderController::class, 'store'],         $authTenantMiddlewares);
$router->delete('/orders',              [OrderController::class, 'clearAll'],        $authTenantMiddlewares);
$router->delete('/orders/item',         [OrderController::class, 'softDeleteOrder'], $authTenantMiddlewares);
$router->post('/orders/item/complete',  [OrderController::class, 'completeOrder'],   $authTenantMiddlewares);
$router->post('/orders/item/cancel',    [OrderController::class, 'cancelOrder'],     $authTenantMiddlewares);
$router->delete('/orders/{id}',         [OrderController::class, 'softDeleteOrder'], $authTenantMiddlewares);
$router->post('/orders/{id}/complete',  [OrderController::class, 'completeOrder'],   $authTenantMiddlewares);
$router->post('/orders/{id}/cancel',    [OrderController::class, 'cancelOrder'],     $authTenantMiddlewares);

// --- 8. KITCHEN ORDER TICKETS (KOT) MODULE ---
$router->get('/kot',              [KotController::class, 'index'],   $authTenantMiddlewares);
$router->post('/kot',             [KotController::class, 'store'],   $authTenantMiddlewares);
$router->post('/kot/item/reprint',[KotController::class, 'reprint'], $authTenantMiddlewares);
$router->post('/kot/{id}/reprint',[KotController::class, 'reprint'], $authTenantMiddlewares);

// --- 9. PAYMENTS MODULE ---
$router->get('/payments',  [PaymentController::class, 'index'], $authTenantMiddlewares);
$router->post('/payments', [PaymentController::class, 'store'], $authTenantMiddlewares);

// --- 10. INVENTORY & DAILY PURCHASES MODULE ---
$router->get('/inventory',                  [InventoryController::class, 'index'],          $authTenantMiddlewares);
$router->post('/inventory',                 [InventoryController::class, 'store'],          $authTenantMiddlewares);
$router->post('/inventory/movements',       [InventoryController::class, 'storeMovement'],  $authTenantMiddlewares);
$router->get('/inventory/purchases',        [InventoryController::class, 'listPurchases'],  $authTenantMiddlewares);
$router->post('/inventory/purchases',       [InventoryController::class, 'storePurchase'],  $authTenantMiddlewares);
$router->delete('/inventory/purchases/{id}',[InventoryController::class, 'deletePurchase'], $authTenantMiddlewares);
$router->get('/inventory/daily-summary',    [InventoryController::class, 'dailySummary'],   $authTenantMiddlewares);

// --- 11. REPORTS MODULE ---
$router->get('/reports/sales',    [ReportController::class, 'sales'],    $authTenantMiddlewares);
$router->get('/reports/itemized', [ReportController::class, 'itemized'], $authTenantMiddlewares);
$router->get('/reports/stock',    [ReportController::class, 'stock'],    $authTenantMiddlewares);

// --- 12. DASHBOARD OVERVIEW ---
$router->get('/branches/{branchId}/overview', [DashboardController::class, 'overview'], $authTenantMiddlewares);

// --- 13. DEVICES (PRINTERS) MODULE ---
$router->get('/devices',              [DeviceController::class, 'index'],               $authTenantMiddlewares);
$router->post('/devices',             [DeviceController::class, 'store'],               $authTenantMiddlewares);
$router->get('/printer-settings',     [DeviceController::class, 'receiptSettings'],     $authTenantMiddlewares);
$router->post('/printer-settings',    [DeviceController::class, 'saveReceiptSettings'], $authTenantMiddlewares);
$router->post('/printer-settings/qr', [DeviceController::class, 'uploadReceiptQr'],     $authTenantMiddlewares);

// --- 14. SUBSCRIPTIONS, RESTAURANT PROFILE & STAFF ACCESS MODULE ---
$router->get('/subscriptions',        [SubscriptionController::class, 'index'],         $authTenantMiddlewares);
$router->get('/subscription-plans',   [SubscriptionController::class, 'plans']);
$router->get('/restaurant/profile',   [RestaurantController::class,   'profile'],       $authTenantMiddlewares);
$router->post('/restaurant/profile',  [RestaurantController::class,   'updateProfile'], $authTenantMiddlewares);
$router->get('/staff',                [StaffController::class,        'index'],         $authTenantMiddlewares);
$router->post('/staff',               [StaffController::class,        'store'],         $authTenantMiddlewares);
$router->delete('/staff/{id}',        [StaffController::class,        'destroy'],       $authTenantMiddlewares);

// --- 15. OFFLINE REALTIME SYNC ENGINE ---
$router->post('/sync/push', [SyncController::class, 'push'], $authTenantMiddlewares);
$router->get('/sync/pull',  [SyncController::class, 'pull'], $authTenantMiddlewares);

// Dispatch incoming HTTP request
$router->dispatch($request);
