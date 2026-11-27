<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;

class CustomerController
{
    public static function ensureKhataTable(): void
    {
        static $checked = false;
        if ($checked) return;
        $checked = true;
        try {
            Database::execute(
                "CREATE TABLE IF NOT EXISTS customer_khata_entries (
                    id VARCHAR(64) PRIMARY KEY,
                    restaurant_id VARCHAR(64) NOT NULL,
                    customer_id VARCHAR(64) NULL,
                    customer_name VARCHAR(255) NOT NULL,
                    mobile_number VARCHAR(50) NOT NULL,
                    entry_type VARCHAR(20) NOT NULL,
                    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                    note VARCHAR(255) NULL,
                    order_uuid VARCHAR(64) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )"
            );
        } catch (\Throwable $e) {}
    }

    /**
     * GET /customers
     */
    public function index(Request $request): void
    {
        self::ensureKhataTable();
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, name, mobile_number, is_regular 
             FROM customers 
             WHERE restaurant_id = :restro 
             ORDER BY name ASC",
            ['restro' => $restaurantId]
        );

        $khataRows = Database::fetchAll(
            "SELECT mobile_number,
                    COALESCE(SUM(CASE WHEN entry_type = 'UDHAAR' THEN amount ELSE -amount END), 0) AS balance
             FROM customer_khata_entries
             WHERE restaurant_id = :restro
             GROUP BY mobile_number",
            ['restro' => $restaurantId]
        );
        $balanceByMobile = [];
        foreach ($khataRows as $kr) {
            $balanceByMobile[(string)$kr['mobile_number']] = max(0.0, (float)$kr['balance']);
        }

        $data = array_map(function ($row) use ($balanceByMobile) {
            $mob = (string)$row['mobile_number'];
            return [
                'id'             => (string)$row['id'],
                'name'           => (string)$row['name'],
                'mobile_number'  => $mob,
                'is_regular'     => (bool)$row['is_regular'],
                'udhaar_balance' => $balanceByMobile[$mob] ?? 0.0,
            ];
        }, $rows);

        Response::success($data, 'Customers retrieved successfully');
    }

    /**
     * POST /customers
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'name'          => 'required|min:1',
            'mobile_number' => 'required|min:10',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $id = !empty($input['id']) ? trim($input['id']) : 'CUST-' . strtoupper(substr(UUID::v4(), 0, 8));
        $name = trim($input['name']);
        $mobile = trim($input['mobile_number']);
        $isRegular = isset($input['is_regular']) ? (int)(bool)$input['is_regular'] : 0;

        // Check if customer with this mobile exists in this restaurant
        $existing = Database::fetchOne(
            "SELECT id FROM customers WHERE mobile_number = :mobile AND restaurant_id = :restro LIMIT 1",
            ['mobile' => $mobile, 'restro' => $restaurantId]
        );

        if ($existing) {
            $id = $existing['id'];
            Database::execute(
                "UPDATE customers SET name = :name, is_regular = :regular, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = :id AND restaurant_id = :restro",
                [
                    'name'    => $name,
                    'regular' => $isRegular,
                    'id'      => $id,
                    'restro'  => $restaurantId,
                ]
            );
        } else {
            Database::execute(
                "INSERT INTO customers (id, restaurant_id, name, mobile_number, is_regular, created_at, updated_at) 
                 VALUES (:id, :restro, :name, :mobile, :regular, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'      => $id,
                    'restro'  => $restaurantId,
                    'name'    => $name,
                    'mobile'  => $mobile,
                    'regular' => $isRegular,
                ]
            );
        }

        Response::success([
            'id'            => $id,
            'name'          => $name,
            'mobile_number' => $mobile,
            'is_regular'    => (bool)$isRegular,
        ], 'Customer saved successfully', 201);
    }

    /**
     * GET /customers/{id}
     */
    public function show(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->param('id');

        $row = Database::fetchOne(
            "SELECT id, name, mobile_number, is_regular FROM customers WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'restro' => $restaurantId]
        );

        if (!$row) {
            Response::notFound('Customer not found');
        }

        Response::success([
            'id'            => (string)$row['id'],
            'name'          => (string)$row['name'],
            'mobile_number' => (string)$row['mobile_number'],
            'is_regular'    => (bool)$row['is_regular'],
        ]);
    }

    /**
     * DELETE /customers/{id}
     */
    public function destroy(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->param('id');

        Database::execute(
            "INSERT INTO sync_tombstones (restaurant_id, entity_type, entity_uuid, sync_seq, created_at) 
             VALUES (:restro, 'customer', :uuid, UNIX_TIMESTAMP(), CURRENT_TIMESTAMP)",
            ['restro' => $restaurantId, 'uuid' => $id]
        );

        Database::execute(
            "DELETE FROM customers WHERE id = :id AND restaurant_id = :restro",
            ['id' => $id, 'restro' => $restaurantId]
        );

        Response::success(null, 'Customer deleted successfully');
    }

    /**
     * GET /customers/khata?mobile=...
     */
    public function khataLedger(Request $request): void
    {
        self::ensureKhataTable();
        $restaurantId = $request->getAttribute('restaurant_id');
        $mobile = trim((string)($request->query('mobile') ?? ''));

        if ($mobile !== '') {
            $rows = Database::fetchAll(
                "SELECT id, customer_id, customer_name, mobile_number, entry_type, amount, note, order_uuid, created_at
                 FROM customer_khata_entries
                 WHERE restaurant_id = :restro AND mobile_number = :mob
                 ORDER BY created_at DESC LIMIT 100",
                ['restro' => $restaurantId, 'mob' => $mobile]
            );
        } else {
            $rows = Database::fetchAll(
                "SELECT id, customer_id, customer_name, mobile_number, entry_type, amount, note, order_uuid, created_at
                 FROM customer_khata_entries
                 WHERE restaurant_id = :restro
                 ORDER BY created_at DESC LIMIT 100",
                ['restro' => $restaurantId]
            );
        }

        Response::success($rows, 'Khata ledger retrieved');
    }

    /**
     * POST /customers/khata
     */
    public function recordKhata(Request $request): void
    {
        self::ensureKhataTable();
        $restaurantId = $request->getAttribute('restaurant_id');
        $input = $request->input();

        $mobile = trim((string)($input['mobile_number'] ?? ''));
        $name = trim((string)($input['customer_name'] ?? 'Customer'));
        $type = strtoupper(trim((string)($input['entry_type'] ?? 'PAYMENT')));
        $amount = (float)($input['amount'] ?? 0);
        $note = trim((string)($input['note'] ?? ''));

        if ($mobile === '' || $amount <= 0 || !in_array($type, ['UDHAAR', 'PAYMENT'], true)) {
            Response::error('Valid mobile_number, entry_type (UDHAAR/PAYMENT), and amount > 0 are required.', 422);
            return;
        }

        // Ensure customer exists in customers table
        $existingCust = Database::fetchOne(
            "SELECT id, name FROM customers WHERE mobile_number = :mob AND restaurant_id = :restro LIMIT 1",
            ['mob' => $mobile, 'restro' => $restaurantId]
        );
        if (!$existingCust) {
            $custId = 'CUST-' . strtoupper(substr(UUID::v4(), 0, 8));
            Database::execute(
                "INSERT INTO customers (id, restaurant_id, name, mobile_number, is_regular, created_at, updated_at)
                 VALUES (:id, :restro, :name, :mob, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ['id' => $custId, 'restro' => $restaurantId, 'name' => $name, 'mob' => $mobile]
            );
        } else {
            $custId = (string)$existingCust['id'];
            $name = (string)$existingCust['name'];
        }

        $entryId = 'KH-' . strtoupper(substr(UUID::v4(), 0, 8));
        Database::execute(
            "INSERT INTO customer_khata_entries (id, restaurant_id, customer_id, customer_name, mobile_number, entry_type, amount, note, created_at)
             VALUES (:id, :restro, :cid, :cname, :mob, :etype, :amt, :note, CURRENT_TIMESTAMP)",
            [
                'id'     => $entryId,
                'restro' => $restaurantId,
                'cid'    => $custId,
                'cname'  => $name,
                'mob'    => $mobile,
                'etype'  => $type,
                'amt'    => $amount,
                'note'   => $note !== '' ? $note : ($type === 'PAYMENT' ? 'Udhaar Payment Received' : 'Manual Udhaar Entry'),
            ]
        );

        $balRow = Database::fetchOne(
            "SELECT COALESCE(SUM(CASE WHEN entry_type = 'UDHAAR' THEN amount ELSE -amount END), 0) AS balance
             FROM customer_khata_entries WHERE restaurant_id = :restro AND mobile_number = :mob",
            ['restro' => $restaurantId, 'mob' => $mobile]
        );
        $newBalance = max(0.0, (float)($balRow['balance'] ?? 0));

        Response::success([
            'entry_id'       => $entryId,
            'customer_id'    => $custId,
            'udhaar_balance' => $newBalance,
        ], 'Khata entry recorded', 201);
    }
}
