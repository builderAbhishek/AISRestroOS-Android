<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;
use AISRestro\Core\Helpers;

class OrderController
{
    /**
     * GET /orders
     */
    public function index(Request $request): void
    {
        if (method_exists(Database::class, 'ensureOrderSoftDeleteColumn')) {
            Database::ensureOrderSoftDeleteColumn();
        }
        $restaurantId = $request->getAttribute('restaurant_id');
        $status = $request->query('status');

        $sql = "SELECT id, uuid, order_number, invoice_number, order_type, status, 
                       table_number, customer_name, customer_phone, subtotal, 
                       discount_amount, tax_amount, service_charge, grand_total, 
                       payment_method, completed_at, created_at 
                FROM orders 
                WHERE restaurant_id = :restro
                  AND COALESCE(is_deleted, 0) = 0";
        $params = ['restro' => $restaurantId];

        if (!empty($status) && strtoupper($status) !== 'ALL') {
            $sql .= " AND status = :status";
            $params['status'] = strtoupper($status);
        } else {
            $sql .= " AND status != 'CANCELLED'";
        }

        $sql .= " ORDER BY created_at DESC";

        $orders = Database::fetchAll($sql, $params);

        if (empty($orders)) {
            Response::success([], 'No orders found');
        }

        // Fetch all items for these orders
        $orderUuids = array_column($orders, 'uuid');
        $inClause = implode(',', array_fill(0, count($orderUuids), '?'));
        
        $itemRows = Database::fetchAll(
            "SELECT order_uuid, item_name, COALESCE(unit_price, price, 0.0) AS unit_price, quantity, COALESCE(total_price, total, 0.0) AS total_price 
             FROM order_items 
             WHERE order_uuid IN ({$inClause})",
            $orderUuids
        );

        $itemsGrouped = [];
        foreach ($itemRows as $item) {
            $itemsGrouped[$item['order_uuid']][] = [
                'item_name'  => (string)$item['item_name'],
                'unit_price' => (float)$item['unit_price'],
                'quantity'   => (int)$item['quantity'],
            ];
        }

        $result = array_map(function ($order) use ($itemsGrouped) {
            $uuid = (string)$order['uuid'];
            return [
                'uuid'            => $uuid,
                'order_number'    => (string)($order['invoice_number'] ?: $order['order_number']),
                'order_type'      => (string)($order['order_type'] ?: 'DINE_IN'),
                'status'          => (string)$order['status'],
                'table_number'    => $order['table_number'] ? (string)$order['table_number'] : null,
                'customer_name'   => $order['customer_name'] ? (string)$order['customer_name'] : null,
                'customer_phone'  => $order['customer_phone'] ? (string)$order['customer_phone'] : null,
                'subtotal'        => (float)$order['subtotal'],
                'discount_amount' => (float)$order['discount_amount'],
                'tax_amount'      => (float)$order['tax_amount'],
                'service_charge'  => (float)$order['service_charge'],
                'grand_total'     => (float)$order['grand_total'],
                'created_at'      => $order['created_at'] ? (string)$order['created_at'] : null,
                'completed_at'    => ($order['completed_at'] ?: $order['created_at']) ? (string)($order['completed_at'] ?: $order['created_at']) : null,
                'items'           => $itemsGrouped[$uuid] ?? [],
            ];
        }, $orders);

        Response::success($result, 'Orders retrieved successfully');
    }

    /**
     * POST /orders
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $uuid = !empty($input['uuid']) ? trim($input['uuid']) : UUID::v4();
        $orderNumber = !empty($input['order_number']) ? trim($input['order_number']) : 'ORD-' . strtoupper(substr($uuid, 0, 6));
        $orderType = !empty($input['order_type']) ? strtoupper(trim($input['order_type'])) : 'DINE_IN';
        $status = !empty($input['status']) ? strtoupper(trim($input['status'])) : 'ON_HOLD';
        $tableNumber = !empty($input['table_number']) ? trim($input['table_number']) : null;
        $customerName = !empty($input['customer_name']) ? trim($input['customer_name']) : null;
        $customerPhone = !empty($input['customer_phone']) ? trim($input['customer_phone']) : null;
        $subtotal = isset($input['subtotal']) ? (float)$input['subtotal'] : 0.0;
        $discountAmount = isset($input['discount_amount']) ? (float)$input['discount_amount'] : 0.0;
        $taxAmount = isset($input['tax_amount']) ? (float)$input['tax_amount'] : 0.0;
        $serviceCharge = isset($input['service_charge']) ? (float)$input['service_charge'] : 0.0;
        $grandTotal = isset($input['grand_total']) ? (float)$input['grand_total'] : ($subtotal - $discountAmount + $taxAmount + $serviceCharge);
        $paymentMethod = !empty($input['payment_method']) ? strtoupper(trim((string)$input['payment_method'])) : 'CASH';
        $items = isset($input['items']) && is_array($input['items']) ? $input['items'] : [];
        $completedAtInput = !empty($input['completed_at']) ? trim($input['completed_at']) : null;
        $createdAtInput = !empty($input['created_at']) ? trim($input['created_at']) : null;

        Database::beginTransaction();

        try {
            $cleanRestro = Helpers::cleanRestaurantPrefix($restaurantId);

            // Check if active (non-deleted) order already exists for this restaurant
            $existing = Database::fetchOne(
                "SELECT id, uuid, invoice_number, status, completed_at, created_at FROM orders WHERE uuid = :uuid AND restaurant_id = :restro AND COALESCE(is_deleted, 0) = 0 LIMIT 1",
                ['uuid' => $uuid, 'restro' => $restaurantId]
            );

            // For brand-new orders, assign structured sequential UUID (e.g. AISR01/ORD-01)
            if (!$existing) {
                if (str_starts_with($uuid, $cleanRestro . '/ORD-')) {
                    $globalCollision = Database::fetchOne(
                        "SELECT id FROM orders WHERE uuid = :uuid LIMIT 1",
                        ['uuid' => $uuid]
                    );
                    if ($globalCollision) {
                        $uuid = Helpers::getNextOrderUuid($restaurantId);
                    }
                } else {
                    $uuid = Helpers::getNextOrderUuid($restaurantId);
                }

                // Allocate restaurant-scoped sequential KOT / Bill number starting from -01
                if ($status === 'ON_HOLD') {
                    $orderNumber = Helpers::getNextKotNumber($restaurantId);
                }
            } else {
                $uuid = (string)$existing['uuid'];
            }

            $invoiceNumber = $existing['invoice_number'] ?? null;

            // If order is completed and lacks invoice number, allocate one sequentially
            if ($status === 'COMPLETED' && empty($invoiceNumber)) {
                $invoiceNumber = Helpers::getNextBillNumber($restaurantId);
                $orderNumber = $invoiceNumber;
            }

            if ($existing) {
                $compAt = ($status === 'COMPLETED') ? ($completedAtInput ?: ($existing['completed_at'] ?: date('Y-m-d H:i:s'))) : $existing['completed_at'];
                $paymentStatus = ($status === 'COMPLETED') ? 'PAID' : 'PENDING';
                Database::execute(
                    "UPDATE orders SET 
                        order_number = :ord_num, invoice_number = :inv_num, order_type = :ord_type, 
                        status = :status, payment_status = :pay_status, payment_method = :pay_method,
                        table_number = :tbl, customer_name = :cname, 
                        customer_phone = :cphone, subtotal = :subtotal, discount_amount = :disc, 
                        tax_amount = :tax, service_charge = :sc, grand_total = :grand, 
                        completed_at = :comp_at, 
                        updated_at = CURRENT_TIMESTAMP 
                     WHERE id = :id AND restaurant_id = :restro",
                    [
                        'ord_num'    => $orderNumber,
                        'inv_num'    => $invoiceNumber,
                        'ord_type'   => $orderType,
                        'status'     => $status,
                        'pay_status' => $paymentStatus,
                        'pay_method' => $paymentMethod,
                        'tbl'        => $tableNumber,
                        'cname'      => $customerName,
                        'cphone'     => $customerPhone,
                        'subtotal'   => $subtotal,
                        'disc'       => $discountAmount,
                        'tax'        => $taxAmount,
                        'sc'         => $serviceCharge,
                        'grand'      => $grandTotal,
                        'comp_at'    => $compAt,
                        'id'         => $existing['id'],
                        'restro'     => $restaurantId,
                    ]
                );

                // Delete existing items to replace with fresh snapshot
                Database::execute("DELETE FROM order_items WHERE order_uuid = :uuid OR order_id = :oid", ['uuid' => $uuid, 'oid' => $existing['id']]);
                $orderId = (int)$existing['id'];
            } else {
                $compAt = ($status === 'COMPLETED') ? ($completedAtInput ?: date('Y-m-d H:i:s')) : null;
                $createdAt = $createdAtInput ?: date('Y-m-d H:i:s');
                $paymentStatus = ($status === 'COMPLETED') ? 'PAID' : 'PENDING';
                Database::execute(
                    "INSERT INTO orders (
                        uuid, restaurant_id, branch_id, order_number, invoice_number, 
                        order_type, status, payment_status, payment_method, table_number, customer_name, customer_phone, 
                        subtotal, discount_amount, tax_amount, service_charge, grand_total, 
                        completed_at, created_at, updated_at
                     ) VALUES (
                        :uuid, :restro, :branch, :ord_num, :inv_num, 
                        :ord_type, :status, :pay_status, :pay_method, :tbl, :cname, :cphone, 
                        :subtotal, :disc, :tax, :sc, :grand, 
                        :comp_at, :created_at, CURRENT_TIMESTAMP
                     )",
                    [
                        'uuid'       => $uuid,
                        'restro'     => $restaurantId,
                        'branch'     => $branchId,
                        'ord_num'    => $orderNumber,
                        'inv_num'    => $invoiceNumber,
                        'ord_type'   => $orderType,
                        'status'     => $status,
                        'pay_status' => $paymentStatus,
                        'pay_method' => $paymentMethod,
                        'tbl'        => $tableNumber,
                        'cname'      => $customerName,
                        'cphone'     => $customerPhone,
                        'subtotal'   => $subtotal,
                        'disc'       => $discountAmount,
                        'tax'        => $taxAmount,
                        'sc'         => $serviceCharge,
                        'grand'      => $grandTotal,
                        'comp_at'    => $compAt,
                        'created_at' => $createdAt,
                    ]
                );
                $orderId = (int)Database::lastInsertId();
            }

            if ($orderId <= 0) {
                throw new \Exception("Cannot insert order line items: Failed to obtain valid parent order_id");
            }

            // Insert line items with dual-column compatibility (order_id & order_uuid, price & unit_price, total & total_price)
            $savedItems = [];
            foreach ($items as $item) {
                $itemName = trim($item['item_name'] ?? '');
                $unitPrice = (float)($item['unit_price'] ?? ($item['price'] ?? 0.0));
                $quantity = (int)($item['quantity'] ?? 1);
                $totalPrice = $unitPrice * $quantity;

                if (!empty($itemName)) {
                    Database::execute(
                        "INSERT INTO order_items (order_id, order_uuid, item_name, price, unit_price, quantity, total, total_price, created_at, updated_at) 
                         VALUES (:oid, :uuid, :name, :price, :uprice, :qty, :total, :ttotal, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        [
                            'oid'    => $orderId,
                            'uuid'   => $uuid,
                            'name'   => $itemName,
                            'price'  => $unitPrice,
                            'uprice' => $unitPrice,
                            'qty'    => $quantity,
                            'total'  => $totalPrice,
                            'ttotal' => $totalPrice,
                        ]
                    );

                    $savedItems[] = [
                        'item_name'  => $itemName,
                        'unit_price' => $unitPrice,
                        'quantity'   => $quantity,
                    ];
                }
            }

            // Sync KOT Ticket & KOT Items tables so kitchen tickets are always recorded
            if (!empty($savedItems)) {
                $kotStatus = ($status === 'COMPLETED') ? 'COMPLETED' : 'PENDING';
                $existingKot = Database::fetchOne(
                    "SELECT id FROM kot_tickets WHERE order_id = :oid AND restaurant_id = :restro LIMIT 1",
                    ['oid' => $orderId, 'restro' => $restaurantId]
                );
                if ($existingKot) {
                    $kotTicketId = (int)$existingKot['id'];
                    Database::execute(
                        "UPDATE kot_tickets SET status = :st, table_number = :tbl, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                        ['st' => $kotStatus, 'tbl' => $tableNumber, 'id' => $kotTicketId]
                    );
                    Database::execute("DELETE FROM kot_items WHERE kot_ticket_id = :ktid", ['ktid' => $kotTicketId]);
                } else {
                    $kotTicketNumber = ($status === 'ON_HOLD') ? $orderNumber : Helpers::getNextKotNumber($restaurantId);
                    $kotUuid = Helpers::getNextKotUuid($restaurantId, $kotTicketNumber);
                    Database::execute(
                        "INSERT INTO kot_tickets (uuid, order_id, order_uuid, restaurant_id, branch_id, ticket_number, kot_number, table_number, status, reprint_count, created_at, updated_at)
                         VALUES (:kuuid, :oid, :ouuid, :restro, :branch, :tnum, :knum, :tbl, :st, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        [
                            'kuuid'  => $kotUuid,
                            'oid'    => $orderId,
                            'ouuid'  => $uuid,
                            'restro' => $restaurantId,
                            'branch' => $branchId,
                            'tnum'   => $kotTicketNumber,
                            'knum'   => $kotTicketNumber,
                            'tbl'    => $tableNumber,
                            'st'     => $kotStatus,
                        ]
                    );
                    $kotTicketId = (int)Database::lastInsertId();
                }

                if ($kotTicketId > 0) {
                    foreach ($savedItems as $si) {
                        Database::execute(
                            "INSERT INTO kot_items (kot_ticket_id, kot_id, item_name, quantity, created_at, updated_at)
                             VALUES (:ktid, :kid, :name, :qty, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                            [
                                'ktid' => $kotTicketId,
                                'kid'  => $kotTicketId,
                                'name' => $si['item_name'],
                                'qty'  => $si['quantity'],
                            ]
                        );
                    }
                }
            }

            // Record Payment transaction when order is COMPLETED
            if ($status === 'COMPLETED') {
                $isUdhaar = strtoupper($paymentMethod) === 'UDHAAR';
                $payRecStatus = $isUdhaar ? 'PENDING' : 'COMPLETED';
                $existingPay = Database::fetchOne(
                    "SELECT id FROM payments WHERE order_id = :oid AND restaurant_id = :restro LIMIT 1",
                    ['oid' => $orderId, 'restro' => $restaurantId]
                );
                if ($existingPay) {
                    Database::execute(
                        "UPDATE payments SET amount = :amt, payment_method = :pm, status = :pst, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                        ['amt' => $grandTotal, 'pm' => $paymentMethod, 'pst' => $payRecStatus, 'id' => $existingPay['id']]
                    );
                } else {
                    Database::execute(
                        "INSERT INTO payments (uuid, restaurant_id, branch_id, order_id, order_uuid, amount, payment_method, status, is_verified, paid_at, created_at, updated_at)
                         VALUES (:puuid, :restro, :branch, :oid, :ouuid, :amt, :pm, :pst, :ver, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        [
                            'puuid'  => UUID::v4(),
                            'restro' => $restaurantId,
                            'branch' => $branchId,
                            'oid'    => $orderId,
                            'ouuid'  => $uuid,
                            'amt'    => $grandTotal,
                            'pm'     => $paymentMethod,
                            'pst'    => $payRecStatus,
                            'ver'    => $isUdhaar ? 0 : 1,
                        ]
                    );
                }

                // Auto-record Udhaar Khata entry when paid via UDHAAR
                if ($isUdhaar && !empty($customerPhone)) {
                    CustomerController::ensureKhataTable();
                    $existingKhata = Database::fetchOne(
                        "SELECT id FROM customer_khata_entries WHERE order_uuid = :ouuid AND restaurant_id = :restro LIMIT 1",
                        ['ouuid' => $uuid, 'restro' => $restaurantId]
                    );
                    if (!$existingKhata) {
                        $custRow = Database::fetchOne(
                            "SELECT id, name FROM customers WHERE mobile_number = :mob AND restaurant_id = :restro LIMIT 1",
                            ['mob' => $customerPhone, 'restro' => $restaurantId]
                        );
                        $custId = $custRow['id'] ?? ('CUST-' . strtoupper(substr(UUID::v4(), 0, 8)));
                        $custNameFinal = !empty($customerName) ? $customerName : ($custRow['name'] ?? 'Customer');
                        if (!$custRow) {
                            Database::execute(
                                "INSERT INTO customers (id, restaurant_id, name, mobile_number, is_regular, created_at, updated_at)
                                 VALUES (:id, :restro, :name, :mob, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                                ['id' => $custId, 'restro' => $restaurantId, 'name' => $custNameFinal, 'mob' => $customerPhone]
                            );
                        }
                        Database::execute(
                            "INSERT INTO customer_khata_entries (id, restaurant_id, customer_id, customer_name, mobile_number, entry_type, amount, note, order_uuid, created_at)
                             VALUES (:id, :restro, :cid, :cname, :mob, 'UDHAAR', :amt, :note, :ouuid, CURRENT_TIMESTAMP)",
                            [
                                'id'     => 'KH-' . strtoupper(substr(UUID::v4(), 0, 8)),
                                'restro' => $restaurantId,
                                'cid'    => $custId,
                                'cname'  => $custNameFinal,
                                'mob'    => $customerPhone,
                                'amt'    => $grandTotal,
                                'note'   => 'Bill ' . ($invoiceNumber ?: $orderNumber),
                                'ouuid'  => $uuid,
                            ]
                        );
                    }
                }
            }

            Database::commit();

            Response::success([
                'uuid'            => $uuid,
                'order_number'    => $invoiceNumber ?: $orderNumber,
                'order_type'      => $orderType,
                'status'          => $status,
                'table_number'    => $tableNumber,
                'customer_name'   => $customerName,
                'customer_phone'  => $customerPhone,
                'subtotal'        => $subtotal,
                'discount_amount' => $discountAmount,
                'tax_amount'      => $taxAmount,
                'service_charge'  => $serviceCharge,
                'grand_total'     => $grandTotal,
                'items'           => $savedItems,
            ], 'Order saved successfully', 201);
        } catch (\Exception $e) {
            Database::rollBack();
            error_log("[Order Store Error] " . $e->getMessage());
            Response::error('Failed to save order: ' . $e->getMessage(), 500);
        }
    }

    /**
     * POST /orders/{id}/complete
     */
    public function completeOrder(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->query('id') ?: $request->param('id');
        $input = $request->input();

        Database::beginTransaction();

        try {
            $forUpdate = Database::forUpdate();
            $order = Database::fetchOne(
                "SELECT * FROM orders WHERE (uuid = :id OR order_number = :ord_num) AND restaurant_id = :restro LIMIT 1{$forUpdate}",
                ['id' => $id, 'ord_num' => $id, 'restro' => $restaurantId]
            );

            if (!$order) {
                Database::rollBack();
                Response::notFound('Order not found');
            }

            // Allocate bill number if not already allocated
            $invoiceNumber = $order['invoice_number'];
            if (empty($invoiceNumber)) {
                $invoiceNumber = Helpers::getNextBillNumber($restaurantId);
            }

            $paymentMethod = !empty($input['payment_method']) ? trim($input['payment_method']) : ($order['payment_method'] ?: 'CASH');

            Database::execute(
                "UPDATE orders SET 
                    status = 'COMPLETED', invoice_number = :inv, payment_method = :pay, 
                    payment_status = 'PAID', completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = :id",
                [
                    'inv' => $invoiceNumber,
                    'pay' => $paymentMethod,
                    'id'  => $order['id'],
                ]
            );

            // Record payment transaction with dual compatibility (uuid, order_id, order_uuid, branch_id)
            $paymentUuid = UUID::v4();
            Database::execute(
                "INSERT INTO payments (
                    uuid, restaurant_id, branch_id, order_id, order_uuid, 
                    amount, payment_method, status, is_verified, paid_at, created_at, updated_at
                 ) VALUES (
                    :puuid, :restro, :branch, :oid, :ouuid, 
                    :amount, :method, 'COMPLETED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                 )",
                [
                    'puuid'  => $paymentUuid,
                    'restro' => $restaurantId,
                    'branch' => $order['branch_id'] ?? null,
                    'oid'    => $order['id'] ?? null,
                    'ouuid'  => $order['uuid'],
                    'amount' => $order['grand_total'],
                    'method' => $paymentMethod,
                ]
            );

            Database::commit();

            Response::success([
                'uuid'           => $order['uuid'],
                'invoice_number' => $invoiceNumber,
                'status'         => 'COMPLETED',
                'grand_total'    => (float)$order['grand_total'],
            ], 'Order completed and billed successfully');
        } catch (\Exception $e) {
            Database::rollBack();
            Response::error('Failed to complete order: ' . $e->getMessage(), 500);
        }
    }

    /**
     * POST /orders/{id}/cancel
     */
    public function cancelOrder(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->query('id') ?: $request->param('id');

        Database::execute(
            "UPDATE orders SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP 
             WHERE (uuid = :id OR order_number = :ord_num) AND restaurant_id = :restro",
            ['id' => $id, 'ord_num' => $id, 'restro' => $restaurantId]
        );

        Response::success(null, 'Order cancelled successfully');
    }

    /**
     * DELETE /orders/{id} - Soft delete a single order (sets is_deleted = 1, keeps row in DB)
     */
    public function softDeleteOrder(Request $request): void
    {
        if (method_exists(Database::class, 'ensureOrderSoftDeleteColumn')) {
            Database::ensureOrderSoftDeleteColumn();
        }
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->query('id') ?: $request->param('id') ?: $request->getAttribute('id');

        if (empty($id)) {
            Response::error('Order ID is required', 400);
        }

        $updated = Database::execute(
            "UPDATE orders 
             SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP 
             WHERE (uuid = :id OR order_number = :ord_num OR invoice_number = :inv_num) 
               AND restaurant_id = :restro",
            ['id' => $id, 'ord_num' => $id, 'inv_num' => $id, 'restro' => $restaurantId]
        );

        if ($updated === 0) {
            Response::notFound('Order not found');
        }

        Response::success(['id' => $id, 'is_deleted' => 1], 'Order deleted successfully');
    }

    /**
     * DELETE /orders - Clear all orders and reset sequences to 0
     */
    public function clearAll(Request $request): void
    {
        if (strtolower((string)$request->getAttribute('role')) !== 'owner') {
            Response::error('Only the restaurant owner can clear all orders.', 403);
            return;
        }
        $restaurantId = $request->getAttribute('restaurant_id');

        Database::beginTransaction();
        try {
            // Delete order items
            Database::execute(
                "DELETE oi FROM order_items oi 
                 JOIN orders o ON (oi.order_id = o.id OR oi.order_uuid = o.uuid) 
                 WHERE o.restaurant_id = :restro",
                ['restro' => $restaurantId]
            );

            // Delete kot items
            Database::execute(
                "DELETE ki FROM kot_items ki 
                 JOIN kot_tickets kt ON (ki.kot_id = kt.id OR ki.kot_ticket_id = kt.id) 
                 WHERE kt.restaurant_id = :restro",
                ['restro' => $restaurantId]
            );

            // Delete kot tickets
            Database::execute("DELETE FROM kot_tickets WHERE restaurant_id = :restro", ['restro' => $restaurantId]);

            // Delete payments
            Database::execute("DELETE FROM payments WHERE restaurant_id = :restro", ['restro' => $restaurantId]);

            // Delete orders
            Database::execute("DELETE FROM orders WHERE restaurant_id = :restro", ['restro' => $restaurantId]);

            // Reset sequence counters so the next order starts at 01
            Database::execute(
                "UPDATE restaurant_settings 
                 SET last_bill_number = 0, last_kot_number = 0, updated_at = CURRENT_TIMESTAMP 
                 WHERE restaurant_id = :restro",
                ['restro' => $restaurantId]
            );

            Database::commit();

            Response::success(null, 'All orders and KOTs cleared, sequences reset to 0');
        } catch (\Exception $e) {
            Database::rollBack();
            error_log("[ClearAll Orders Error] " . $e->getMessage());
            Response::error('Failed to clear orders. Please try again.', 500);
        }
    }
}
