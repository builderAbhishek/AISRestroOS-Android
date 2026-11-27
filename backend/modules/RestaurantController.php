<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\Validator;

class RestaurantController
{
    /**
     * GET /restaurant/profile
     */
    public function profile(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $row = Database::fetchOne("SELECT * FROM restaurants WHERE id = :id LIMIT 1", ['id' => $restaurantId]);

        if (!$row) {
            Response::notFound('Restaurant profile not found');
            return;
        }

        // Fetch active plan summary for read-only display
        $sub = Database::fetchOne(
            "SELECT s.status, p.name AS plan_name
             FROM subscriptions s
             JOIN subscription_plans p ON (p.id = s.plan_id OR p.id = s.subscription_plan_id)
             WHERE s.restaurant_id = :restro AND (s.status = 'active' OR s.status = 'ACTIVE' OR s.status = 'TRIAL')
             ORDER BY COALESCE(s.expires_at, s.ends_at) DESC LIMIT 1",
            ['restro' => $restaurantId]
        );

        Response::success([
            'id'           => (string)$row['id'],
            'name'         => (string)($row['name'] ?? ''),
            'email'        => (string)($row['email'] ?? ''),
            'phone'        => (string)($row['phone'] ?? ''),
            'address'      => (string)($row['address'] ?? ''),
            'city'         => (string)($row['city'] ?? ''),
            'state'        => (string)($row['state'] ?? ''),
            'pincode'      => (string)($row['pincode'] ?? ''),
            'gst_number'   => (string)($row['gst_number'] ?? ($row['gstin'] ?? '')),
            'fssai_number' => (string)($row['fssai_number'] ?? ''),
            'currency'     => (string)($row['currency'] ?? 'INR'),
            'plan_name'    => (string)($sub['plan_name'] ?? 'Business Mobile Plan'),
            'plan_status'  => strtoupper((string)($sub['status'] ?? 'ACTIVE')),
        ], 'Restaurant profile retrieved');
    }

    /**
     * POST /restaurant/profile
     */
    public function updateProfile(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'name' => 'required|min:2',
        ]);

        if ($validator->fails()) {
            Response::error('Restaurant name is required', 422, $validator->errors());
            return;
        }

        // Inspect available columns on restaurants table so update never fails on missing optional columns
        $availableCols = [];
        try {
            $cols = Database::isSqlite()
                ? Database::fetchAll("PRAGMA table_info(restaurants)")
                : Database::fetchAll("SHOW COLUMNS FROM restaurants");
            foreach ($cols as $c) {
                $colName = $c['name'] ?? ($c['Field'] ?? '');
                if ($colName !== '') {
                    $availableCols[$colName] = true;
                }
            }
        } catch (\Throwable $e) {
            $availableCols = ['name' => true, 'email' => true, 'phone' => true];
        }

        $candidateFields = [
            'name'         => trim((string)($input['name'] ?? '')),
            'email'        => trim((string)($input['email'] ?? '')),
            'phone'        => trim((string)($input['phone'] ?? '')),
            'address'      => trim((string)($input['address'] ?? '')),
            'city'         => trim((string)($input['city'] ?? '')),
            'state'        => trim((string)($input['state'] ?? '')),
            'pincode'      => trim((string)($input['pincode'] ?? '')),
            'gst_number'   => trim((string)($input['gst_number'] ?? '')),
            'fssai_number' => trim((string)($input['fssai_number'] ?? '')),
        ];

        $sets = [];
        $params = ['id' => $restaurantId];
        foreach ($candidateFields as $col => $val) {
            if (isset($availableCols[$col])) {
                $sets[] = "{$col} = :{$col}";
                $params[$col] = $val;
            }
        }
        if (isset($availableCols['updated_at'])) {
            $sets[] = "updated_at = CURRENT_TIMESTAMP";
        }

        if (!empty($sets)) {
            $sql = "UPDATE restaurants SET " . implode(', ', $sets) . " WHERE id = :id";
            Database::execute($sql, $params);
        }

        $this->profile($request);
    }
}
