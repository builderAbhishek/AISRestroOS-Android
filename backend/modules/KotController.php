<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\Helpers;
use AISRestro\Core\UUID;

class KotController
{
    /**
     * GET /kot
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $tickets = Database::fetchAll(
            "SELECT id, COALESCE(kot_number, ticket_number) AS kot_number, COALESCE(order_uuid, '') AS order_uuid, table_number, status, reprint_count, created_at 
             FROM kot_tickets 
             WHERE restaurant_id = :restro 
             ORDER BY id DESC LIMIT 50",
            ['restro' => $restaurantId]
        );

        Response::success($tickets, 'KOT tickets retrieved successfully');
    }

    /**
     * POST /kot
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $orderUuid = !empty($input['order_uuid']) ? trim($input['order_uuid']) : null;
        $tableNumber = !empty($input['table_number']) ? trim($input['table_number']) : null;
        $items = isset($input['items']) && is_array($input['items']) ? $input['items'] : [];

        if (empty($orderUuid)) {
            Response::error('Order UUID is required for KOT ticket creation', 422);
            return;
        }

        Database::beginTransaction();

        try {
            // Check if KOT ticket already exists for this order
            $existing = Database::fetchOne(
                "SELECT * FROM kot_tickets WHERE (order_uuid = :uuid OR ticket_number = :uuid2) AND restaurant_id = :restro LIMIT 1",
                ['uuid' => $orderUuid, 'uuid2' => $orderUuid, 'restro' => $restaurantId]
            );

            if ($existing) {
                // If KOT already exists, DO NOT generate a new number; return existing ticket
                $kotItems = Database::fetchAll(
                    "SELECT item_name, quantity, notes FROM kot_items WHERE kot_id = :kid OR kot_ticket_id = :kid2",
                    ['kid' => $existing['id'], 'kid2' => $existing['id']]
                );

                Database::commit();

                Response::success([
                    'id'            => (int)$existing['id'],
                    'kot_number'    => (string)($existing['kot_number'] ?: $existing['ticket_number']),
                    'order_uuid'    => (string)($existing['order_uuid'] ?: $orderUuid),
                    'table_number'  => $existing['table_number'],
                    'status'        => $existing['status'],
                    'reprint_count' => (int)$existing['reprint_count'],
                    'items'         => $kotItems,
                    'is_existing'   => true,
                ], 'KOT ticket already generated. Retaining original KOT number.');
                return;
            }

            // Lookup existing order to satisfy foreign keys
            $order = Database::fetchOne(
                "SELECT id, branch_id FROM orders WHERE uuid = :uuid AND restaurant_id = :restro LIMIT 1",
                ['uuid' => $orderUuid, 'restro' => $restaurantId]
            );
            if (!$order) {
                Database::rollBack();
                Response::error("Cannot generate KOT: Order not found with UUID '{$orderUuid}'", 404);
                return;
            }

            // Allocate sequential KOT number & structured UUID (e.g. AISR01/KOT-01)
            $kotNumber = Helpers::getNextKotNumber($restaurantId);
            $orderId = (int)$order['id'];
            $orderBranchId = !empty($order['branch_id']) ? $order['branch_id'] : $branchId;
            $kotUuid = Helpers::getNextKotUuid($restaurantId, $kotNumber);

            Database::execute(
                "INSERT INTO kot_tickets (
                    uuid, order_id, order_uuid, ticket_number, kot_number, 
                    restaurant_id, branch_id, table_number, status, reprint_count, 
                    created_at, updated_at
                 ) VALUES (
                    :kuuid, :oid, :ouuid, :tnum, :knum, 
                    :restro, :branch, :tbl, 'SENT_TO_KITCHEN', 0, 
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                 )",
                [
                    'kuuid'   => $kotUuid,
                    'oid'     => $orderId,
                    'ouuid'   => $orderUuid,
                    'tnum'    => $kotNumber,
                    'knum'    => $kotNumber,
                    'restro'  => $restaurantId,
                    'branch'  => $orderBranchId,
                    'tbl'     => $tableNumber,
                ]
            );

            $kotId = (int)Database::lastInsertId();
            if ($kotId <= 0) {
                throw new \Exception("Cannot insert KOT items: Invalid parent KOT ID");
            }

            $savedItems = [];
            foreach ($items as $item) {
                $name = trim($item['item_name'] ?? '');
                $qty = (int)($item['quantity'] ?? 1);
                $notes = !empty($item['notes']) ? trim($item['notes']) : null;

                if (!empty($name)) {
                    Database::execute(
                        "INSERT INTO kot_items (kot_ticket_id, kot_id, item_name, quantity, notes, created_at, updated_at) 
                         VALUES (:ktid, :kid, :name, :qty, :notes, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        [
                            'ktid'  => $kotId,
                            'kid'   => $kotId,
                            'name'  => $name,
                            'qty'   => $qty,
                            'notes' => $notes,
                        ]
                    );

                    $savedItems[] = [
                        'item_name' => $name,
                        'quantity'  => $qty,
                        'notes'     => $notes,
                    ];
                }
            }

            Database::commit();

            Response::success([
                'id'            => $kotId,
                'kot_number'    => $kotNumber,
                'order_uuid'    => $orderUuid,
                'table_number'  => $tableNumber,
                'status'        => 'SENT_TO_KITCHEN',
                'reprint_count' => 0,
                'items'         => $savedItems,
            ], 'KOT ticket created successfully', 201);
        } catch (\Exception $e) {
            Database::rollBack();
            Response::error('Failed to create KOT ticket: ' . $e->getMessage(), 500);
        }
    }

    /**
     * POST /kot/{id}/reprint
     */
    public function reprint(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->query('id') ?: $request->param('id');

        $ticket = Database::fetchOne(
            "SELECT * FROM kot_tickets WHERE (id = :id OR kot_number = :kot_num OR order_uuid = :ord_uuid) AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'kot_num' => $id, 'ord_uuid' => $id, 'restro' => $restaurantId]
        );

        if (!$ticket) {
            Response::notFound('KOT ticket not found');
        }

        // Increment reprint count without creating a new sequence number
        Database::execute(
            "UPDATE kot_tickets SET reprint_count = reprint_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
            ['id' => $ticket['id']]
        );

        $items = Database::fetchAll(
            "SELECT item_name, quantity, notes FROM kot_items WHERE kot_id = :kid",
            ['kid' => $ticket['id']]
        );

        Response::success([
            'id'            => (int)$ticket['id'],
            'kot_number'    => (string)$ticket['kot_number'],
            'order_uuid'    => (string)$ticket['order_uuid'],
            'table_number'  => $ticket['table_number'],
            'reprint_count' => ((int)$ticket['reprint_count']) + 1,
            'items'         => $items,
        ], 'KOT ticket reprint verified. Original KOT number preserved.');
    }
}
