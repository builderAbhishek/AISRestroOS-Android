<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;

class TableController
{
    /**
     * GET /tables
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, table_number, capacity, COALESCE(is_occupied, CASE WHEN status = 'OCCUPIED' THEN 1 ELSE 0 END) AS is_occupied 
             FROM tables 
             WHERE restaurant_id = :restro 
             ORDER BY table_number ASC",
            ['restro' => $restaurantId]
        );

        $data = array_map(function ($row) {
            return [
                'id'         => (string)$row['id'],
                'number'     => (string)$row['table_number'],
                'capacity'   => (int)$row['capacity'],
                'isOccupied' => (bool)$row['is_occupied'],
            ];
        }, $rows);

        Response::success($data, 'Tables retrieved successfully');
    }

    /**
     * POST /tables
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'number' => 'required',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $tNum = trim($input['number']);
        $capacity = isset($input['capacity']) ? (int)$input['capacity'] : 4;
        $isOccupied = isset($input['isOccupied']) ? (int)(bool)$input['isOccupied'] : 0;
        $id = !empty($input['id']) ? trim($input['id']) : 'TBL-' . strtoupper(substr(UUID::v4(), 0, 8));

        $existing = Database::fetchOne(
            "SELECT id FROM tables WHERE table_number = :num AND restaurant_id = :restro LIMIT 1",
            ['num' => $tNum, 'restro' => $restaurantId]
        );

        $statusVal = $isOccupied ? 'OCCUPIED' : 'AVAILABLE';

        if ($existing) {
            Database::execute(
                "UPDATE tables SET capacity = :cap, status = :status, is_occupied = :occ, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = :id AND restaurant_id = :restro",
                [
                    'cap'    => $capacity,
                    'status' => $statusVal,
                    'occ'    => $isOccupied,
                    'id'     => $existing['id'],
                    'restro' => $restaurantId,
                ]
            );
            $id = $existing['id'];
        } else {
            Database::execute(
                "INSERT INTO tables (id, restaurant_id, branch_id, table_number, capacity, status, is_occupied, created_at, updated_at) 
                 VALUES (:id, :restro, :branch, :num, :cap, :status, :occ, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'     => $id,
                    'restro' => $restaurantId,
                    'branch' => $branchId,
                    'num'    => $tNum,
                    'cap'    => $capacity,
                    'status' => $statusVal,
                    'occ'    => $isOccupied,
                ]
            );
        }

        Response::success([
            'id'         => $id,
            'number'     => $tNum,
            'capacity'   => $capacity,
            'isOccupied' => (bool)$isOccupied,
        ], 'Table saved successfully', 201);
    }
}
