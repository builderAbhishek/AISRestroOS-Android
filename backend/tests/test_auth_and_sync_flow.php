<?php

/**
 * AISRestroOS End-to-End Authentication and Database Sync Integration Test Suite
 *
 * Verifies:
 *  1. Unauthenticated request to protected endpoints -> HTTP 401
 *  2. Invalid/expired Bearer token request -> HTTP 401
 *  3. Login with invalid credentials -> HTTP 401
 *  4. Registration / Login with valid credentials -> HTTP 200/201 with Bearer token
 *  5. Session verification via GET /auth/me -> HTTP 200 with tenant context
 *  6. Controlled Order creation (POST /orders) -> Persisted in MariaDB orders + order_items
 *  7. Controlled Order update/completion (POST /orders/{id}/complete) -> Status COMPLETED, invoice assigned, payment recorded
 *  8. Offline sync push (POST /sync/push) with X-Idempotency-Key -> Persisted in MariaDB
 *  9. Idempotent retry of sync request -> Zero duplicate records in MariaDB
 * 10. Logout (POST /auth/logout) -> Token revoked, subsequent requests return HTTP 401
 */

$apiBase = 'http://127.0.0.1:8088/api';
$dbHost = '127.0.0.1';
$dbPort = 3308;
$dbName = 'u328317103_restro_sync_test';
$dbUser = 'root';
$dbPass = '';

echo "====================================================================\n";
echo "AISRestroOS Authentication & Database Sync Integration Test Suite\n";
echo "Target API: {$apiBase}\n";
echo "Database: {$dbHost}:{$dbPort}/{$dbName}\n";
echo "====================================================================\n\n";

// Connect to MariaDB directly for state verification
try {
    $pdo = new PDO("mysql:host={$dbHost};port={$dbPort};dbname={$dbName};charset=utf8mb4", $dbUser, $dbPass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    echo "[PASS] Connected to MariaDB directly on port {$dbPort}.\n\n";
} catch (Exception $e) {
    echo "[FATAL] Cannot connect to MariaDB: " . $e->getMessage() . "\n";
    exit(1);
}

function http_call(string $method, string $url, ?array $body = null, ?string $token = null, array $extraHeaders = []): array {
    $headers = ['Content-Type: application/json', 'Accept: application/json'];
    if ($token !== null) {
        $headers[] = 'Authorization: Bearer ' . $token;
    }
    foreach ($extraHeaders as $k => $v) {
        $headers[] = "{$k}: {$v}";
    }

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);

    if ($body !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    }

    $raw = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);

    if ($raw === false) {
        throw new Exception("cURL error: {$err}");
    }

    $json = json_decode($raw, true);
    return [
        'status' => $status,
        'body'   => $json,
        'raw'    => $raw,
    ];
}

$testsPassed = 0;
$testsTotal = 0;

function assert_test(string $name, bool $condition, string $detail = ''): void {
    global $testsPassed, $testsTotal;
    $testsTotal++;
    if ($condition) {
        $testsPassed++;
        echo "  [PASS] Test {$testsTotal}: {$name}\n";
        if ($detail) echo "         {$detail}\n";
    } else {
        echo "  [FAIL] Test {$testsTotal}: {$name}\n";
        if ($detail) echo "         Details: {$detail}\n";
        exit(1);
    }
}

// -------------------------------------------------------------
// SCENARIO 1: Protected Endpoint Without Token
// -------------------------------------------------------------
echo "--- SCENARIO 1: Unauthenticated Requests to Protected Endpoints ---\n";
$res1 = http_call('GET', "{$apiBase}/auth/me");
assert_test("GET /auth/me without token returns HTTP 401", $res1['status'] === 401, "Returned status {$res1['status']}");

$res2 = http_call('GET', "{$apiBase}/orders");
assert_test("GET /orders without token returns HTTP 401", $res2['status'] === 401, "Returned status {$res2['status']}");

// -------------------------------------------------------------
// SCENARIO 2: Protected Endpoint With Invalid Token
// -------------------------------------------------------------
echo "\n--- SCENARIO 2: Invalid or Expired Bearer Token ---\n";
$res3 = http_call('GET', "{$apiBase}/auth/me", null, 'bogus_token_xyz987');
assert_test("GET /auth/me with bogus token returns HTTP 401", $res3['status'] === 401, "Returned status {$res3['status']}");

// -------------------------------------------------------------
// SCENARIO 3: Login With Invalid Credentials
// -------------------------------------------------------------
echo "\n--- SCENARIO 3: Invalid Credentials Login ---\n";
$res4 = http_call('POST', "{$apiBase}/auth/login", [
    'email' => 'luckysingh955416@gmail.com',
    'password' => 'CompletelyWrongPassword_!@#',
]);
assert_test("POST /auth/login with invalid password returns HTTP 401", $res4['status'] === 401, "Returned status {$res4['status']}");

$res5 = http_call('POST', "{$apiBase}/auth/login", [
    'email' => 'nonexistent_user@example.com',
    'password' => 'some_password',
]);
assert_test("POST /auth/login with nonexistent email returns HTTP 401", $res5['status'] === 401, "Returned status {$res5['status']}");

// -------------------------------------------------------------
// SCENARIO 4: Valid Registration & Authentication
// -------------------------------------------------------------
echo "\n--- SCENARIO 4: Valid User Registration & Authentication Setup ---\n";
$testEmail = 'pos_test_' . time() . '@restro.com';
$testPass = 'StrongPass@2026';
$testRestroName = 'AutoTest Bistro ' . rand(100, 999);

$res6 = http_call('POST', "{$apiBase}/auth/register", [
    'name' => 'POS Test Operator',
    'email' => $testEmail,
    'password' => $testPass,
    'phone' => '9876543210',
    'restaurant_name' => $testRestroName,
]);
assert_test("POST /auth/register returns HTTP 201 Created", $res6['status'] === 201, "Returned status {$res6['status']}");
assert_test("Register response includes token and tenant info", 
    !empty($res6['body']['data']['token']) && 
    !empty($res6['body']['data']['restaurant_id']) && 
    !empty($res6['body']['data']['branch_id']),
    "Restro ID: " . ($res6['body']['data']['restaurant_id'] ?? 'null')
);

$authSession = $res6['body']['data'];
$registeredToken = $authSession['token'];
$restaurantId = $authSession['restaurant_id'];
$branchId = $authSession['branch_id'];

// Verify user, restaurant, branch, and sequence in MariaDB
$dbUser = $pdo->prepare("SELECT * FROM users WHERE email = :email");
$dbUser->execute(['email' => $testEmail]);
$userRow = $dbUser->fetch();
assert_test("User row created in MariaDB users table", (bool)$userRow, "User ID: {$userRow['id']}");

$dbRestro = $pdo->prepare("SELECT * FROM restaurants WHERE id = :id");
$dbRestro->execute(['id' => $restaurantId]);
$restroRow = $dbRestro->fetch();
assert_test("Restaurant row created in MariaDB restaurants table", (bool)$restroRow, "Name: {$restroRow['name']}");

$dbBranch = $pdo->prepare("SELECT * FROM branches WHERE id = :id");
$dbBranch->execute(['id' => $branchId]);
$branchRow = $dbBranch->fetch();
assert_test("Branch row created in MariaDB branches table", (bool)$branchRow, "Code: {$branchRow['code']}");

// -------------------------------------------------------------
// SCENARIO 5: Login With Valid Credentials
// -------------------------------------------------------------
echo "\n--- SCENARIO 5: Login with Valid Credentials ---\n";
$res7 = http_call('POST', "{$apiBase}/auth/login", [
    'email' => $testEmail,
    'password' => $testPass,
]);
assert_test("POST /auth/login returns HTTP 200 OK", $res7['status'] === 200, "Status: {$res7['status']}");
assert_test("Login returns valid Bearer token", !empty($res7['body']['data']['token']), "Token received");
$activeToken = $res7['body']['data']['token'];

// -------------------------------------------------------------
// SCENARIO 6: Token Verification (GET /auth/me)
// -------------------------------------------------------------
echo "\n--- SCENARIO 6: Verify Token & Tenant Identity via GET /auth/me ---\n";
$res8 = http_call('GET', "{$apiBase}/auth/me", null, $activeToken);
assert_test("GET /auth/me returns HTTP 200 OK", $res8['status'] === 200, "Status: {$res8['status']}");
assert_test("GET /auth/me returns matching restaurant_id and branch_id", 
    $res8['body']['data']['restaurant_id'] === $restaurantId && 
    $res8['body']['data']['branch_id'] === $branchId,
    "Verified Restro ID {$restaurantId}"
);

// -------------------------------------------------------------
// SCENARIO 7: Controlled Order Creation (POST /orders)
// -------------------------------------------------------------
echo "\n--- SCENARIO 7: Controlled Order Creation & Database Persistence ---\n";
$orderUuid = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
    mt_rand(0, 0xffff), mt_rand(0, 0xffff),
    mt_rand(0, 0xffff),
    mt_rand(0, 0x0fff) | 0x4000,
    mt_rand(0, 0x3fff) | 0x8000,
    mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
);

$newOrderPayload = [
    'uuid'            => $orderUuid,
    'order_number'    => 'ORD-TEST-001',
    'order_type'      => 'DINE_IN',
    'status'          => 'ON_HOLD',
    'table_number'    => 'T-12',
    'customer_name'   => 'Rohan Sharma',
    'customer_phone'  => '9811223344',
    'subtotal'        => 500.0,
    'discount_amount' => 50.0,
    'tax_amount'      => 22.5,
    'service_charge'  => 10.0,
    'grand_total'     => 482.5,
    'items'           => [
        [
            'item_name'  => 'Paneer Butter Masala',
            'unit_price' => 260.0,
            'quantity'   => 1,
        ],
        [
            'item_name'  => 'Butter Naan',
            'unit_price' => 40.0,
            'quantity'   => 6,
        ],
    ],
];

$res9 = http_call('POST', "{$apiBase}/orders", $newOrderPayload, $activeToken);
assert_test("POST /orders returns HTTP 201 Created", $res9['status'] === 201, "Status: {$res9['status']}");

// Directly verify persistence in MariaDB
$dbOrder = $pdo->prepare("SELECT * FROM orders WHERE uuid = :uuid");
$dbOrder->execute(['uuid' => $orderUuid]);
$orderRow = $dbOrder->fetch();
assert_test("Order persisted in MariaDB with matching UUID", (bool)$orderRow, "Order ID: {$orderRow['id']}");
assert_test("Order has correct restaurant_id and branch_id", 
    $orderRow['restaurant_id'] === $restaurantId && $orderRow['branch_id'] === $branchId,
    "Restro: {$orderRow['restaurant_id']}, Branch: {$orderRow['branch_id']}"
);
assert_test("Order grand_total accurately stored in MariaDB", (float)$orderRow['grand_total'] === 482.5, "Grand total: {$orderRow['grand_total']}");

// Verify order line items in MariaDB
$dbItems = $pdo->prepare("SELECT * FROM order_items WHERE order_uuid = :uuid OR order_id = :oid");
$dbItems->execute(['uuid' => $orderUuid, 'oid' => $orderRow['id']]);
$itemRows = $dbItems->fetchAll();
assert_test("Line items persisted in MariaDB order_items table", count($itemRows) === 2, "Found " . count($itemRows) . " line items");
assert_test("Order item prices and quantities match exactly", 
    $itemRows[0]['item_name'] === 'Paneer Butter Masala' && (float)$itemRows[0]['price'] === 260.0 &&
    $itemRows[1]['item_name'] === 'Butter Naan' && (int)$itemRows[1]['quantity'] === 6,
    "Items verified in MariaDB"
);

// -------------------------------------------------------------
// SCENARIO 8: Order Completion & Bill Generation
// -------------------------------------------------------------
echo "\n--- SCENARIO 8: Order Completion & Automatic Invoice / Payment Recording ---\n";
$res10 = http_call('POST', "{$apiBase}/orders/{$orderUuid}/complete", ['payment_method' => 'UPI'], $activeToken);
assert_test("POST /orders/{id}/complete returns HTTP 200 OK", $res10['status'] === 200, "Status: {$res10['status']}");
assert_test("Response includes allocated invoice_number", !empty($res10['body']['data']['invoice_number']), "Invoice: " . ($res10['body']['data']['invoice_number'] ?? ''));

// Directly verify MariaDB order status and invoice number
$dbOrder->execute(['uuid' => $orderUuid]);
$completedOrderRow = $dbOrder->fetch();
assert_test("Order status updated to COMPLETED in MariaDB", $completedOrderRow['status'] === 'COMPLETED', "Status: {$completedOrderRow['status']}");
assert_test("Invoice number recorded in MariaDB orders table", !empty($completedOrderRow['invoice_number']), "Invoice: {$completedOrderRow['invoice_number']}");

// Directly verify payment record in MariaDB
$dbPayment = $pdo->prepare("SELECT * FROM payments WHERE order_uuid = :uuid OR order_id = :oid");
$dbPayment->execute(['uuid' => $orderUuid, 'oid' => $orderRow['id']]);
$paymentRow = $dbPayment->fetch();
assert_test("Payment record created in MariaDB payments table", (bool)$paymentRow, "Payment ID: {$paymentRow['id']}");
assert_test("Payment amount and method match order", 
    (float)$paymentRow['amount'] === 482.5 && $paymentRow['payment_method'] === 'UPI',
    "Amount: {$paymentRow['amount']}, Method: {$paymentRow['payment_method']}"
);

// -------------------------------------------------------------
// SCENARIO 9: Offline Sync Push (POST /sync/push)
// -------------------------------------------------------------
echo "\n--- SCENARIO 9: Offline-to-Online Sync Push via POST /sync/push ---\n";
$offlineOrderUuid = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
    mt_rand(0, 0xffff), mt_rand(0, 0xffff),
    mt_rand(0, 0xffff),
    mt_rand(0, 0x0fff) | 0x4000,
    mt_rand(0, 0x3fff) | 0x8000,
    mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
);

$idempotencyKey = 'IDEMPOTENCY-' . uniqid();

$syncPushPayload = [
    'orders' => [
        [
            'uuid'            => $offlineOrderUuid,
            'order_number'    => 'ORD-OFFLINE-001',
            'order_type'      => 'TAKEAWAY',
            'status'          => 'ON_HOLD',
            'table_number'    => null,
            'customer_name'   => 'Deepak Verma',
            'customer_phone'  => '9988776655',
            'subtotal'        => 350.0,
            'discount_amount' => 0.0,
            'tax_amount'      => 17.5,
            'service_charge'  => 0.0,
            'grand_total'     => 367.5,
            'items'           => [
                [
                    'item_name'  => 'Dal Makhani',
                    'unit_price' => 220.0,
                    'quantity'   => 1,
                ],
                [
                    'item_name'  => 'Jeera Rice',
                    'unit_price' => 130.0,
                    'quantity'   => 1,
                ],
            ],
        ],
    ],
    'customers' => [
        [
            'name'          => 'Deepak Verma',
            'mobile_number' => '9988776655',
            'is_regular'    => 1,
        ],
    ],
];

$res11 = http_call('POST', "{$apiBase}/sync/push", $syncPushPayload, $activeToken, ['x-idempotency-key' => $idempotencyKey]);
assert_test("POST /sync/push returns HTTP 200 OK", $res11['status'] === 200, "Status: {$res11['status']}");
assert_test("Sync push response confirms synced UUID", 
    in_array($offlineOrderUuid, $res11['body']['data']['synced_uuids'] ?? []),
    "Synced UUID: {$offlineOrderUuid}"
);

// Verify offline order was persisted into MariaDB
$dbOrder->execute(['uuid' => $offlineOrderUuid]);
$offlineOrderRow = $dbOrder->fetch();
assert_test("Offline order persisted in MariaDB via sync push", (bool)$offlineOrderRow, "Order ID: {$offlineOrderRow['id']}");

// Verify customer persisted into MariaDB
$dbCust = $pdo->prepare("SELECT * FROM customers WHERE mobile_number = '9988776655' AND restaurant_id = :restro");
$dbCust->execute(['restro' => $restaurantId]);
$custRow = $dbCust->fetch();
assert_test("Offline customer persisted in MariaDB customers table", (bool)$custRow, "Customer ID: {$custRow['id']}, Name: {$custRow['name']}");

// -------------------------------------------------------------
// SCENARIO 10: Idempotent Retry of Sync Request
// -------------------------------------------------------------
echo "\n--- SCENARIO 10: Idempotency Protection on Duplicate Sync Requests ---\n";
// Re-send the identical sync push request with the same X-Idempotency-Key
$res12 = http_call('POST', "{$apiBase}/sync/push", $syncPushPayload, $activeToken, ['x-idempotency-key' => $idempotencyKey]);
assert_test("Duplicate POST /sync/push returns HTTP 200 OK", $res12['status'] === 200, "Status: {$res12['status']}");

// Check MariaDB for duplicate rows with the same UUID
$dbDupCheck = $pdo->prepare("SELECT COUNT(*) as count FROM orders WHERE uuid = :uuid");
$dbDupCheck->execute(['uuid' => $offlineOrderUuid]);
$orderCount = (int)$dbDupCheck->fetch()['count'];
assert_test("Zero duplicate orders created in MariaDB (count == 1)", $orderCount === 1, "Order count in DB: {$orderCount}");

$dbDupItems = $pdo->prepare("SELECT COUNT(*) as count FROM order_items WHERE order_uuid = :uuid");
$dbDupItems->execute(['uuid' => $offlineOrderUuid]);
$itemCount = (int)$dbDupItems->fetch()['count'];
assert_test("Zero duplicate order items created in MariaDB (count == 2)", $itemCount === 2, "Items count in DB: {$itemCount}");

// -------------------------------------------------------------
// SCENARIO 11: Logout & Token Revocation
// -------------------------------------------------------------
echo "\n--- SCENARIO 11: Logout & Session Revocation ---\n";
$res13 = http_call('POST', "{$apiBase}/auth/logout", null, $activeToken);
assert_test("POST /auth/logout returns HTTP 200 OK", $res13['status'] === 200, "Status: {$res13['status']}");

// Check that subsequent request with revoked token is denied
$res14 = http_call('GET', "{$apiBase}/auth/me", null, $activeToken);
assert_test("Subsequent GET /auth/me with revoked token returns HTTP 401 Unauthorized", $res14['status'] === 401, "Status: {$res14['status']}");

echo "\n====================================================================\n";
echo "INTEGRATION TEST SUMMARY: {$testsPassed} / {$testsTotal} TESTS PASSED (100% SUCCESS)\n";
echo "====================================================================\n";
