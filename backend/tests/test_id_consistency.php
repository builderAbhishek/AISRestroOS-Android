<?php

/**
 * AISRestroOS UUID & Legacy ID Consistency Verification Suite
 * Tests every API write operation to ensure relational synchronization
 * between legacy integer IDs and companion UUIDs across:
 *   - orders & order_items
 *   - kot_tickets & kot_items
 *   - payments
 */

require_once dirname(__DIR__) . '/core/Database.php';
require_once dirname(__DIR__) . '/core/Request.php';
require_once dirname(__DIR__) . '/core/Response.php';
require_once dirname(__DIR__) . '/core/UUID.php';
require_once dirname(__DIR__) . '/core/Helpers.php';
require_once dirname(__DIR__) . '/core/Validator.php';
require_once dirname(__DIR__) . '/modules/OrderController.php';
require_once dirname(__DIR__) . '/modules/KotController.php';
require_once dirname(__DIR__) . '/modules/PaymentController.php';

use AISRestro\Core\Database;
use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\UUID;
use AISRestro\Core\Helpers;
use AISRestro\Modules\OrderController;
use AISRestro\Modules\KotController;
use AISRestro\Modules\PaymentController;

Response::$exitOnSend = false;

$port = isset($argv[1]) ? (int)$argv[1] : 3308;
$testDb = $argv[2] ?? 'u328317103_restro_consistency_test';
$restroId = '01a207f7-8342-7368-b5de-2019baf99da4';
$branchId = '01a207f7-8344-7360-8652-4ad52c3e6e38';

echo "====================================================================\n";
echo "AISRestroOS UUID & Legacy ID Relational Consistency Audit\n";
echo "Testing on MariaDB port {$port}, Database: {$testDb}\n";
echo "====================================================================\n\n";

function stripSqlComments(string $sql): string {
    $lines = explode("\n", $sql);
    $cleanLines = [];
    foreach ($lines as $line) {
        $trimmed = trim($line);
        if ($trimmed === '' || str_starts_with($trimmed, '--')) {
            continue;
        }
        $cleanLines[] = $line;
    }
    $clean = implode("\n", $cleanLines);
    $clean = preg_replace('/\/\*[^!].*?\*\//s', '', $clean);
    return trim($clean);
}

// 1. Initialize isolated database
$serverPdo = new \PDO("mysql:host=127.0.0.1;port={$port}", 'root', '', [
    \PDO::ATTR_ERRMODE => \PDO::ERRMODE_EXCEPTION,
]);
$serverPdo->exec("DROP DATABASE IF EXISTS `{$testDb}`");
$serverPdo->exec("CREATE DATABASE `{$testDb}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$serverPdo->exec("USE `{$testDb}`");

Database::setConfig([
    'driver'   => 'mysql',
    'host'     => '127.0.0.1',
    'port'     => $port,
    'database' => $testDb,
    'username' => 'root',
    'password' => '',
]);

$pdo = Database::getConnection();

// Import production dump
$dumpPath = "C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437937148_fcfe28be.txt";
$dumpSql = str_replace("\r\n", "\n", file_get_contents($dumpPath));
$pdo->exec("SET FOREIGN_KEY_CHECKS = 0;");
$pdo->exec("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';");
$queries = explode(";\n", $dumpSql);
foreach ($queries as $q) {
    $stmt = stripSqlComments($q);
    if (!empty($stmt)) {
        try {
            $pdo->exec($stmt);
        } catch (\Exception $ex) {}
    }
}
$pdo->exec("SET FOREIGN_KEY_CHECKS = 1;");

// Apply migration
$migSqlPath = dirname(__DIR__) . '/database/migrations/FINAL_PRODUCTION_MIGRATION.sql';
$migSql = str_replace("\r\n", "\n", file_get_contents($migSqlPath));
$migQueries = explode(";\n", $migSql);
foreach ($migQueries as $q) {
    $stmt = stripSqlComments($q);
    if (!empty($stmt)) {
        try {
            $pdo->exec($stmt);
        } catch (\Exception $ex) {
            echo "[MIGRATION STMT ERROR] " . $ex->getMessage() . "\n";
        }
    }
}

function assert_test(string $name, bool $condition, string $detail = ''): void {
    if ($condition) {
        echo "  [PASS] {$name}\n";
    } else {
        echo "  [FAIL] {$name} (" . ($detail ?: 'Assertion failed') . ")\n";
        exit(1);
    }
}

// ----------------------------------------------------------------------------
// 1. Test Order & Order Items Dual Consistency
// ----------------------------------------------------------------------------
echo "--- 1. Testing Order & Order Items Dual Write Consistency ---\n";
$orderUuid = UUID::v4();
$orderPayload = [
    'uuid'            => $orderUuid,
    'order_type'      => 'DINE_IN',
    'table_number'    => 'T-10',
    'customer_name'   => 'Deepak Verma',
    'customer_phone'  => '9812345678',
    'subtotal'        => 250.00,
    'discount_amount' => 0.00,
    'tax_amount'      => 12.50,
    'service_charge'  => 0.00,
    'grand_total'     => 262.50,
    'items'           => [
        ['item_name' => 'Paneer Tikka', 'unit_price' => 150.00, 'quantity' => 1],
        ['item_name' => 'Butter Naan', 'unit_price' => 50.00, 'quantity' => 2],
    ],
];

Response::reset();
$req = Request::createMock('POST', '/orders', $orderPayload, ['restaurant_id' => $restroId, 'branch_id' => $branchId]);
(new OrderController())->store($req);

assert_test("Order created with HTTP 201", Response::getLastStatusCode() === 201);

// Direct DB inspection
$dbOrder = $pdo->query("SELECT * FROM orders WHERE uuid = '{$orderUuid}'")->fetch();
assert_test("Order exists in DB with positive integer ID", (int)$dbOrder['id'] > 0);
assert_test("Order UUID matches client payload", $dbOrder['uuid'] === $orderUuid);

$dbItems = $pdo->query("SELECT * FROM order_items WHERE order_id = {$dbOrder['id']}")->fetchAll();
assert_test("Order line items count matches (2 items)", count($dbItems) === 2);

foreach ($dbItems as $item) {
    assert_test(
        "Item '{$item['item_name']}': order_id matches parent integer ID ({$dbOrder['id']})",
        (int)$item['order_id'] === (int)$dbOrder['id']
    );
    assert_test(
        "Item '{$item['item_name']}': order_uuid matches parent UUID ({$dbOrder['uuid']})",
        $item['order_uuid'] === $dbOrder['uuid']
    );
    assert_test(
        "Item '{$item['item_name']}': price equals unit_price ({$item['unit_price']})",
        (float)$item['price'] === (float)$item['unit_price']
    );
    assert_test(
        "Item '{$item['item_name']}': total equals total_price ({$item['total_price']})",
        (float)$item['total'] === (float)$item['total_price']
    );
}
echo "\n";

// ----------------------------------------------------------------------------
// 2. Test KOT Ticket & KOT Items Dual Consistency
// ----------------------------------------------------------------------------
echo "--- 2. Testing KOT Ticket & Items Dual Write Consistency ---\n";
$kotPayload = [
    'order_uuid'   => $orderUuid,
    'table_number' => 'T-10',
    'items'        => [
        ['item_name' => 'Paneer Tikka', 'quantity' => 1, 'notes' => 'Spicy'],
        ['item_name' => 'Butter Naan', 'quantity' => 2, 'notes' => 'Crispy'],
    ],
];

Response::reset();
$reqKot = Request::createMock('POST', '/kot', $kotPayload, ['restaurant_id' => $restroId, 'branch_id' => $branchId]);
(new KotController())->store($reqKot);

assert_test("KOT created with HTTP 201", Response::getLastStatusCode() === 201);

$dbKot = $pdo->query("SELECT * FROM kot_tickets WHERE order_uuid = '{$orderUuid}'")->fetch();
assert_test("KOT ticket linked to parent order_id integer ({$dbOrder['id']})", (int)$dbKot['order_id'] === (int)$dbOrder['id']);
assert_test("KOT ticket linked to parent order_uuid ({$dbOrder['uuid']})", $dbKot['order_uuid'] === $dbOrder['uuid']);
assert_test("KOT ticket_number matches kot_number ({$dbKot['kot_number']})", $dbKot['ticket_number'] === $dbKot['kot_number']);

$dbKotItems = $pdo->query("SELECT * FROM kot_items WHERE kot_ticket_id = {$dbKot['id']}")->fetchAll();
assert_test("KOT items count matches (2 items)", count($dbKotItems) === 2);

foreach ($dbKotItems as $kItem) {
    assert_test(
        "KOT item '{$kItem['item_name']}': kot_ticket_id matches parent integer ID ({$dbKot['id']})",
        (int)$kItem['kot_ticket_id'] === (int)$dbKot['id']
    );
    assert_test(
        "KOT item '{$kItem['item_name']}': kot_id matches kot_ticket_id ({$dbKot['id']})",
        (int)$kItem['kot_id'] === (int)$dbKot['id']
    );
}
echo "\n";

// ----------------------------------------------------------------------------
// 3. Test Payment Dual Consistency
// ----------------------------------------------------------------------------
echo "--- 3. Testing Payment Dual Write Consistency ---\n";
$payPayload = [
    'order_uuid'            => $orderUuid,
    'amount'                => 262.50,
    'payment_method'        => 'UPI',
    'transaction_reference' => 'UPI-TXN-123456789',
    'status'                => 'COMPLETED',
];

Response::reset();
$reqPay = Request::createMock('POST', '/payments', $payPayload, ['restaurant_id' => $restroId, 'branch_id' => $branchId]);
(new PaymentController())->store($reqPay);

assert_test("Payment created with HTTP 201", Response::getLastStatusCode() === 201);

$dbPay = $pdo->query("SELECT * FROM payments WHERE order_uuid = '{$orderUuid}'")->fetch();
assert_test("Payment linked to parent order_id integer ({$dbOrder['id']})", (int)$dbPay['order_id'] === (int)$dbOrder['id']);
assert_test("Payment linked to parent order_uuid ({$dbOrder['uuid']})", $dbPay['order_uuid'] === $dbOrder['uuid']);
assert_test(
    "Payment reference_number matches transaction_reference ({$dbPay['transaction_reference']})",
    $dbPay['reference_number'] === $dbPay['transaction_reference']
);
echo "\n";

// ----------------------------------------------------------------------------
// 4. Test Missing Parent & Invalid UUID Handling (Orphan Prevention)
// ----------------------------------------------------------------------------
echo "--- 4. Testing Invalid UUID & Missing Parent Error Handling ---\n";
$bogusUuid = '00000000-0000-0000-0000-000000000999';

// KOT with non-existent order
Response::reset();
$bogusKotReq = Request::createMock('POST', '/kot', ['order_uuid' => $bogusUuid, 'table_number' => 'T-99', 'items' => [['item_name' => 'Ghost Item', 'quantity' => 1]]], ['restaurant_id' => $restroId, 'branch_id' => $branchId]);
(new KotController())->store($bogusKotReq);

assert_test("KOT with invalid order UUID returned HTTP 404", Response::getLastStatusCode() === 404);
$ghostKotCount = (int)$pdo->query("SELECT COUNT(*) FROM kot_tickets WHERE order_uuid = '{$bogusUuid}'")->fetchColumn();
assert_test("Zero KOT tickets created for invalid UUID", $ghostKotCount === 0);

// Payment with non-existent order
Response::reset();
$bogusPayReq = Request::createMock('POST', '/payments', ['order_uuid' => $bogusUuid, 'amount' => 100.00, 'payment_method' => 'CASH'], ['restaurant_id' => $restroId, 'branch_id' => $branchId]);
(new PaymentController())->store($bogusPayReq);

assert_test("Payment with invalid order UUID returned HTTP 404", Response::getLastStatusCode() === 404);
$ghostPayCount = (int)$pdo->query("SELECT COUNT(*) FROM payments WHERE order_uuid = '{$bogusUuid}'")->fetchColumn();
assert_test("Zero payments created for invalid UUID", $ghostPayCount === 0);
echo "\n";

// ----------------------------------------------------------------------------
// 5. Direct Database Foreign Key Violation Enforcement
// ----------------------------------------------------------------------------
echo "--- 5. Testing Database Engine Relational Foreign Key Integrity ---\n";
$invalidFkTests = [
    [
        'label' => 'Order Item with non-existent order_id (999999)',
        'sql'   => "INSERT INTO order_items (order_id, order_uuid, item_name, price, unit_price, quantity, total, total_price) VALUES (999999, '{$orderUuid}', 'Illegal Item', 10, 10, 1, 10, 10)",
    ],
    [
        'label' => 'KOT Ticket with non-existent order_id (999999)',
        'sql'   => "INSERT INTO kot_tickets (uuid, order_id, order_uuid, ticket_number, kot_number, restaurant_id, branch_id, table_number, status) VALUES ('bad-kuuid', 999999, '{$orderUuid}', 'KOT-BAD', 'KOT-BAD', '{$restroId}', '{$branchId}', 'T-1', 'PENDING')",
    ],
    [
        'label' => 'KOT Item with non-existent kot_ticket_id (999999)',
        'sql'   => "INSERT INTO kot_items (kot_ticket_id, kot_id, item_name, quantity) VALUES (999999, 999999, 'Illegal KOT Item', 1)",
    ],
    [
        'label' => 'Payment with non-existent order_id (999999)',
        'sql'   => "INSERT INTO payments (uuid, restaurant_id, branch_id, order_id, order_uuid, amount, payment_method, is_verified) VALUES ('bad-puuid', '{$restroId}', '{$branchId}', 999999, '{$orderUuid}', 50, 'CASH', 1)",
    ],
];

foreach ($invalidFkTests as $test) {
    try {
        $pdo->exec($test['sql']);
        assert_test($test['label'], false, "Engine failed to block orphan insert!");
    } catch (\PDOException $e) {
        assert_test("Engine strictly blocked: " . $test['label'], true);
    }
}

// Cleanup
$serverPdo->exec("DROP DATABASE IF EXISTS `{$testDb}`");

echo "\n====================================================================\n";
echo "UUID & LEGACY ID CONSISTENCY RESULT: 100% SYNCHRONIZED & PROTECTED\n";
echo "====================================================================\n";
