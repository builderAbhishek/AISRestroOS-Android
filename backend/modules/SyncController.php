<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\Helpers;

class SyncController
{
    /**
     * POST /sync/push
     * Accepts SyncPushRequest + X-Idempotency-Key header
     */
    public function push(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $idempotencyKey = $request->getHeader('x-idempotency-key');

        // Check Idempotency Key cache
        if (!empty($idempotencyKey)) {
            $cached = Database::fetchOne(
                "SELECT response_payload FROM idempotency_keys WHERE (key_value = :key OR idempotency_key = :key2) LIMIT 1",
                ['key' => $idempotencyKey, 'key2' => $idempotencyKey]
            );

            if ($cached) {
                $payload = json_decode($cached['response_payload'], true);
                if (is_array($payload)) {
                    Response::rawJson($payload);
                }
            }
        }

        $input = $request->input();
        $orders = $input['orders'] ?? [];
        $customers = $input['customers'] ?? [];

        $syncedUuids = [];
        $failedUuids = [];
        $conflicts = [];

        Database::beginTransaction();

        try {
            // 1. Process Orders
            foreach ($orders as $order) {
                $uuid = trim($order['uuid'] ?? '');
                if (empty($uuid)) {
                    continue;
                }

                try {
                    $orderNumber = !empty($order['order_number']) ? trim($order['order_number']) : 'ORD-' . substr($uuid, 0, 6);
                    $orderType   = !empty($order['order_type']) ? strtoupper(trim($order['order_type'])) : 'DINE_IN';
                    $status      = !empty($order['status']) ? strtoupper(trim($order['status'])) : 'ON_HOLD';
                    $tableNumber = !empty($order['table_number']) ? trim($order['table_number']) : null;
                    $custName    = !empty($order['customer_name']) ? trim($order['customer_name']) : null;
                    $custPhone   = !empty($order['customer_phone']) ? trim($order['customer_phone']) : null;
                    $subtotal    = (float)($order['subtotal'] ?? 0.0);
                    $disc        = (float)($order['discount_amount'] ?? 0.0);
                    $tax         = (float)($order['tax_amount'] ?? 0.0);
                    $sc          = (float)($order['service_charge'] ?? 0.0);
                    $grand       = (float)($order['grand_total'] ?? ($subtotal - $disc + $tax + $sc));
                    $items       = $order['items'] ?? [];

                    $existing = Database::fetchOne(
                        "SELECT id, invoice_number, status FROM orders WHERE uuid = :uuid AND restaurant_id = :restro LIMIT 1",
                        ['uuid' => $uuid, 'restro' => $restaurantId]
                    );

                    $invoiceNumber = $existing['invoice_number'] ?? null;
                    if ($status === 'COMPLETED' && empty($invoiceNumber)) {
                        $invoiceNumber = Helpers::getNextBillNumber($restaurantId);
                    }

                    if ($existing) {
                        Database::execute(
                            "UPDATE orders SET 
                                order_number = :ord_num, invoice_number = :inv_num, order_type = :ord_type, 
                                status = :status, table_number = :tbl, customer_name = :cname, 
                                customer_phone = :cphone, subtotal = :subtotal, discount_amount = :disc, 
                                tax_amount = :tax, service_charge = :sc, grand_total = :grand, 
                                updated_at = CURRENT_TIMESTAMP 
                             WHERE id = :id AND restaurant_id = :restro",
                            [
                                'ord_num'  => $orderNumber,
                                'inv_num'  => $invoiceNumber,
                                'ord_type' => $orderType,
                                'status'   => $status,
                                'tbl'      => $tableNumber,
                                'cname'    => $custName,
                                'cphone'   => $custPhone,
                                'subtotal' => $subtotal,
                                'disc'     => $disc,
                                'tax'      => $tax,
                                'sc'       => $sc,
                                'grand'    => $grand,
                                'id'       => $existing['id'],
                                'restro'   => $restaurantId,
                            ]
                        );

                        Database::execute("DELETE FROM order_items WHERE order_uuid = :uuid OR order_id = :oid", ['uuid' => $uuid, 'oid' => $existing['id']]);
                        $orderId = (int)$existing['id'];
                    } else {
                        Database::execute(
                            "INSERT INTO orders (
                                uuid, restaurant_id, branch_id, order_number, invoice_number, 
                                order_type, status, table_number, customer_name, customer_phone, 
                                subtotal, discount_amount, tax_amount, service_charge, grand_total, 
                                created_at, updated_at
                             ) VALUES (
                                :uuid, :restro, :branch, :ord_num, :inv_num, 
                                :ord_type, :status, :tbl, :cname, :cphone, 
                                :subtotal, :disc, :tax, :sc, :grand, 
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                             )",
                            [
                                'uuid'     => $uuid,
                                'restro'   => $restaurantId,
                                'branch'   => $branchId,
                                'ord_num'  => $orderNumber,
                                'inv_num'  => $invoiceNumber,
                                'ord_type' => $orderType,
                                'status'   => $status,
                                'tbl'      => $tableNumber,
                                'cname'    => $custName,
                                'cphone'   => $custPhone,
                                'subtotal' => $subtotal,
                                'disc'     => $disc,
                                'tax'      => $tax,
                                'sc'       => $sc,
                                'grand'    => $grand,
                            ]
                        );
                        $orderId = (int)Database::lastInsertId();
                    }

                    // Insert line items with dual-column compatibility
                    foreach ($items as $item) {
                        $itemName = trim($item['item_name'] ?? '');
                        $unitPrice = (float)($item['unit_price'] ?? ($item['price'] ?? 0.0));
                        $qty = (int)($item['quantity'] ?? 1);
                        if (!empty($itemName)) {
                            Database::execute(
                                "INSERT INTO order_items (order_id, order_uuid, item_name, price, unit_price, quantity, total, total_price, created_at, updated_at) 
                                 VALUES (:oid, :uuid, :name, :price, :uprice, :qty, :total, :ttotal, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                                [
                                    'oid'    => $orderId ?: null,
                                    'uuid'   => $uuid,
                                    'name'   => $itemName,
                                    'price'  => $unitPrice,
                                    'uprice' => $unitPrice,
                                    'qty'    => $qty,
                                    'total'  => $unitPrice * $qty,
                                    'ttotal' => $unitPrice * $qty,
                                ]
                            );
                        }
                    }

                    $syncedUuids[] = $uuid;
                } catch (\Exception $ex) {
                    error_log("[Sync Order Fail] {$uuid}: " . $ex->getMessage());
                    $failedUuids[] = $uuid;
                }
            }

            // 2. Process Customers
            foreach ($customers as $cust) {
                $cId = $cust['id'] ?? null;
                $name = trim($cust['name'] ?? '');
                $mobile = trim($cust['mobile_number'] ?? '');
                $isReg = isset($cust['is_regular']) ? (int)(bool)$cust['is_regular'] : 0;

                if (!empty($mobile) && !empty($name)) {
                    $cExisting = Database::fetchOne(
                        "SELECT id FROM customers WHERE mobile_number = :mobile AND restaurant_id = :restro LIMIT 1",
                        ['mobile' => $mobile, 'restro' => $restaurantId]
                    );

                    if ($cExisting) {
                        Database::execute(
                            "UPDATE customers SET name = :name, is_regular = :reg, updated_at = CURRENT_TIMESTAMP 
                             WHERE id = :id AND restaurant_id = :restro",
                            ['name' => $name, 'reg' => $isReg, 'id' => $cExisting['id'], 'restro' => $restaurantId]
                        );
                    } else {
                        $newCId = $cId ?: 'CUST-' . strtoupper(substr(uniqid(), -6));
                        Database::execute(
                            "INSERT INTO customers (id, restaurant_id, name, mobile_number, is_regular, created_at, updated_at) 
                             VALUES (:id, :restro, :name, :mobile, :reg, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            ['id' => $newCId, 'restro' => $restaurantId, 'name' => $name, 'mobile' => $mobile, 'reg' => $isReg]
                        );
                    }
                }
            }

            $latestCursor = time();

            Database::commit();

            $resultData = [
                'latest_cursor' => $latestCursor,
                'synced_uuids'  => $syncedUuids,
                'failed_uuids'  => $failedUuids,
                'conflicts'     => $conflicts,
            ];

            $responseWrapper = [
                'status'  => 'success',
                'message' => 'Sync push processed successfully',
                'data'    => $resultData,
            ];

            // Cache response if idempotency key present
            if (!empty($idempotencyKey)) {
                Database::execute(
                    "INSERT INTO idempotency_keys (
                        restaurant_id, idempotency_key, key_value, request_path, 
                        resource_type, response_code, response_payload, created_at
                     ) VALUES (
                        :restro, :key, :key2, '/sync/push', 
                        'sync_push', 200, :payload, CURRENT_TIMESTAMP
                     )",
                    [
                        'restro'  => $restaurantId,
                        'key'     => $idempotencyKey,
                        'key2'    => $idempotencyKey,
                        'payload' => json_encode($responseWrapper, JSON_UNESCAPED_UNICODE),
                    ]
                );
            }

            Response::rawJson($responseWrapper);
        } catch (\Exception $e) {
            Database::rollBack();
            Response::error('Sync push failure: ' . $e->getMessage(), 500);
        }
    }

    /**
     * GET /sync/pull?cursor={cursor}
     */
    public function pull(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $cursor = (int)$request->query('cursor', 0);

        // Fetch modified menu items
        $menuSql = "SELECT id, code, name, category, price, tax_mode, tax_percentage, 
                           current_stock, is_default_tax, is_favourite, image_url 
                    FROM menu_items 
                    WHERE restaurant_id = :restro";
        $menuParams = ['restro' => $restaurantId];
        if ($cursor > 0) {
            $menuSql .= " AND (sync_seq > :cursor OR UNIX_TIMESTAMP(updated_at) > :cursor)";
            $menuParams['cursor'] = $cursor;
        }
        $menuSql .= " ORDER BY name ASC";

        $menuRows = Database::fetchAll($menuSql, $menuParams);

        $menuItems = array_map(function ($row) {
            return [
                'id'             => (string)$row['id'],
                'code'           => $row['code'] ?: null,
                'name'           => (string)$row['name'],
                'category'       => (string)$row['category'],
                'price'          => (float)$row['price'],
                'tax_mode'       => (string)$row['tax_mode'],
                'tax_percentage' => (string)$row['tax_percentage'],
                'current_stock'  => (int)$row['current_stock'],
                'is_default_tax' => (bool)$row['is_default_tax'],
                'is_favourite'   => (bool)$row['is_favourite'],
                'image_url'      => $row['image_url'] ?: null,
            ];
        }, $menuRows);

        // Fetch modified categories
        $catSql = "SELECT id, name, display_order 
                   FROM categories 
                   WHERE restaurant_id = :restro";
        $catParams = ['restro' => $restaurantId];
        if ($cursor > 0) {
            $catSql .= " AND (sync_seq > :cursor OR UNIX_TIMESTAMP(updated_at) > :cursor)";
            $catParams['cursor'] = $cursor;
        }
        $catSql .= " ORDER BY display_order ASC";

        $catRows = Database::fetchAll($catSql, $catParams);

        $categories = array_map(function ($row) {
            return [
                'id'            => (string)$row['id'],
                'name'          => (string)$row['name'],
                'display_order' => (int)$row['display_order'],
            ];
        }, $catRows);

        // Fetch modified customers
        $custSql = "SELECT id, name, mobile_number, is_regular 
                    FROM customers 
                    WHERE restaurant_id = :restro";
        $custParams = ['restro' => $restaurantId];
        if ($cursor > 0) {
            $custSql .= " AND (sync_seq > :cursor OR UNIX_TIMESTAMP(updated_at) > :cursor)";
            $custParams['cursor'] = $cursor;
        }
        $custSql .= " ORDER BY name ASC";

        $custRows = Database::fetchAll($custSql, $custParams);

        $customers = array_map(function ($row) {
            return [
                'id'            => (string)$row['id'],
                'name'          => (string)$row['name'],
                'mobile_number' => (string)$row['mobile_number'],
                'is_regular'    => (bool)$row['is_regular'],
            ];
        }, $custRows);

        // Fetch tombstones
        $tombRows = Database::fetchAll(
            "SELECT entity_type, entity_uuid, sync_seq 
             FROM sync_tombstones 
             WHERE restaurant_id = :restro AND sync_seq > :cursor 
             ORDER BY sync_seq ASC",
            ['restro' => $restaurantId, 'cursor' => $cursor]
        );

        $tombstones = array_map(function ($row) {
            return [
                'entity_type' => (string)$row['entity_type'],
                'entity_uuid' => (string)$row['entity_uuid'],
                'sync_seq'    => (int)$row['sync_seq'],
            ];
        }, $tombRows);

        $latestCursor = time();

        Response::success([
            'latest_cursor' => $latestCursor,
            'menu_items'    => $menuItems,
            'categories'    => $categories,
            'customers'     => $customers,
            'tombstones'    => $tombstones,
        ], 'Sync pull retrieved successfully');
    }
}
