<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\Validator;
use AISRestro\Core\UUID;

class PaymentController
{
    /**
     * GET /payments
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, uuid, COALESCE(order_uuid, '') AS order_uuid, amount, payment_method, 
                    COALESCE(transaction_reference, reference_number) AS transaction_reference, 
                    COALESCE(status, 'COMPLETED') AS status, created_at 
             FROM payments 
             WHERE restaurant_id = :restro 
             ORDER BY created_at DESC LIMIT 100",
            ['restro' => $restaurantId]
        );

        Response::success($rows, 'Payments retrieved successfully');
    }

    /**
     * POST /payments
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'order_uuid'     => 'required',
            'amount'         => 'required|numeric',
            'payment_method' => 'required',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $orderUuid = trim($input['order_uuid']);
        $amount = (float)$input['amount'];
        $method = strtoupper(trim($input['payment_method']));
        $txRef = !empty($input['transaction_reference']) ? trim($input['transaction_reference']) : null;
        $status = !empty($input['status']) ? strtoupper(trim($input['status'])) : 'COMPLETED';

        // Lookup order to link foreign keys
        $order = Database::fetchOne(
            "SELECT id, branch_id FROM orders WHERE uuid = :uuid AND restaurant_id = :restro LIMIT 1",
            ['uuid' => $orderUuid, 'restro' => $restaurantId]
        );
        if (!$order) {
            Response::error("Cannot record payment: Order not found with UUID '{$orderUuid}'", 404);
            return;
        }
        $orderId = (int)$order['id'];
        $payBranchId = !empty($order['branch_id']) ? $order['branch_id'] : $branchId;
        $paymentUuid = UUID::v4();

        Database::execute(
            "INSERT INTO payments (
                uuid, restaurant_id, branch_id, order_id, order_uuid, 
                amount, payment_method, reference_number, transaction_reference, 
                status, is_verified, paid_at, created_at, updated_at
             ) VALUES (
                :puuid, :restro, :branch, :oid, :ouuid, 
                :amt, :method, :ref, :ref2, 
                :status, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
             )",
            [
                'puuid'  => $paymentUuid,
                'restro' => $restaurantId,
                'branch' => $payBranchId,
                'oid'    => $orderId,
                'ouuid'  => $orderUuid,
                'amt'    => $amount,
                'method' => $method,
                'ref'    => $txRef,
                'ref2'   => $txRef,
                'status' => $status,
            ]
        );

        $id = (int)Database::lastInsertId();

        Response::success([
            'id'                    => $id,
            'uuid'                  => $paymentUuid,
            'order_uuid'            => $orderUuid,
            'amount'                => $amount,
            'payment_method'        => $method,
            'transaction_reference' => $txRef,
            'status'                => $status,
        ], 'Payment recorded successfully', 201);
    }
}
