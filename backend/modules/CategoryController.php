<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;

class CategoryController
{
    /**
     * GET /categories
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, name, display_order FROM categories 
             WHERE restaurant_id = :restro 
             ORDER BY display_order ASC, name ASC",
            ['restro' => $restaurantId]
        );

        $data = array_map(function ($row) {
            return [
                'id'            => (string)$row['id'],
                'name'          => (string)$row['name'],
                'display_order' => (int)$row['display_order'],
            ];
        }, $rows);

        Response::success($data, 'Categories retrieved successfully');
    }

    /**
     * POST /categories
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'name' => 'required|min:1',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $id = !empty($input['id']) ? trim($input['id']) : 'CAT-' . strtoupper(substr(UUID::v4(), 0, 8));
        $name = trim($input['name']);
        $displayOrder = isset($input['display_order']) ? (int)$input['display_order'] : 0;

        // Upsert category
        $existing = Database::fetchOne(
            "SELECT id FROM categories WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'restro' => $restaurantId]
        );

        if ($existing) {
            Database::execute(
                "UPDATE categories SET name = :name, display_order = :order, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = :id AND restaurant_id = :restro",
                [
                    'name'   => $name,
                    'order'  => $displayOrder,
                    'id'     => $id,
                    'restro' => $restaurantId,
                ]
            );
        } else {
            Database::execute(
                "INSERT INTO categories (id, restaurant_id, branch_id, name, display_order, created_at, updated_at) 
                 VALUES (:id, :restro, :branch, :name, :order, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'     => $id,
                    'restro' => $restaurantId,
                    'branch' => $branchId,
                    'name'   => $name,
                    'order'  => $displayOrder,
                ]
            );
        }

        Response::success([
            'id'            => $id,
            'name'          => $name,
            'display_order' => $displayOrder,
        ], 'Category saved successfully', 201);
    }

    /**
     * DELETE /categories/{id}
     */
    public function destroy(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->param('id');

        if (!$id) {
            Response::error('Category ID is required', 400);
        }

        // Record in sync tombstones for delta sync clients
        Database::execute(
            "INSERT INTO sync_tombstones (restaurant_id, entity_type, entity_uuid, sync_seq, created_at) 
             VALUES (:restro, 'category', :uuid, UNIX_TIMESTAMP(), CURRENT_TIMESTAMP)",
            ['restro' => $restaurantId, 'uuid' => $id]
        );

        Database::execute(
            "DELETE FROM categories WHERE id = :id AND restaurant_id = :restro",
            ['id' => $id, 'restro' => $restaurantId]
        );

        Response::success(null, 'Category deleted successfully');
    }
}
