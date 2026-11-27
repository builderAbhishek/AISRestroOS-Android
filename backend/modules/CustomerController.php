<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;

class CustomerController
{
    /**
     * GET /customers
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, name, mobile_number, is_regular 
             FROM customers 
             WHERE restaurant_id = :restro 
             ORDER BY name ASC",
            ['restro' => $restaurantId]
        );

        $data = array_map(function ($row) {
            return [
                'id'            => (string)$row['id'],
                'name'          => (string)$row['name'],
                'mobile_number' => (string)$row['mobile_number'],
                'is_regular'    => (bool)$row['is_regular'],
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
}
