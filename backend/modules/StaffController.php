<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\Auth;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;

class StaffController
{
    private function resolvePlanLimits(string|int $restaurantId): array
    {
        $sub = Database::fetchOne(
            "SELECT COALESCE(s.plan_id, s.subscription_plan_id) AS plan_id, p.name AS plan_name
             FROM subscriptions s
             JOIN subscription_plans p ON (p.id = s.plan_id OR p.id = s.subscription_plan_id)
             WHERE s.restaurant_id = :restro AND (s.status = 'active' OR s.status = 'ACTIVE' OR s.status = 'TRIAL')
             ORDER BY COALESCE(s.expires_at, s.ends_at) DESC LIMIT 1",
            ['restro' => $restaurantId]
        );

        $planId = (string)($sub['plan_id'] ?? 'PLAN-BIZ-MOBILE');
        $planName = (string)($sub['plan_name'] ?? 'Business Mobile Plan');
        $lower = strtolower($planName . ' ' . $planId);

        $maxStaff = match (true) {
            str_contains($lower, 'enterprise') || str_contains($lower, 'pro') || str_contains($lower, 'unlimited') => 15,
            str_contains($lower, 'growth') || str_contains($lower, 'plus') || str_contains($lower, 'standard') => 5,
            default => 2, // Business Mobile / Starter plan: up to 2 mobile logins
        };

        return [
            'plan_id'   => $planId,
            'plan_name' => $planName,
            'max_staff' => $maxStaff,
        ];
    }

    /**
     * GET /staff
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $planInfo = $this->resolvePlanLimits($restaurantId);

        $rows = Database::fetchAll(
            "SELECT rm.id, rm.user_id, rm.role, rm.status, rm.is_active, rm.created_at,
                    u.name, u.email, u.phone
             FROM restaurant_memberships rm
             JOIN users u ON u.id = rm.user_id
             WHERE rm.restaurant_id = :restro AND COALESCE(rm.is_active, 1) = 1
             ORDER BY CASE WHEN rm.role = 'RESTAURANT_OWNER' THEN 0 ELSE 1 END, rm.created_at ASC",
            ['restro' => $restaurantId]
        );

        $staff = array_map(function ($r) {
            return [
                'id'         => (string)$r['id'],
                'user_id'    => (int)$r['user_id'],
                'name'       => (string)$r['name'],
                'email'      => (string)($r['email'] ?? ''),
                'phone'      => (string)($r['phone'] ?? ''),
                'role'       => (string)$r['role'],
                'status'     => strtoupper((string)($r['status'] ?? 'ACTIVE')),
                'is_owner'   => strtoupper((string)$r['role']) === 'RESTAURANT_OWNER',
                'created_at' => (string)($r['created_at'] ?? ''),
            ];
        }, $rows);

        Response::success([
            'plan_id'    => $planInfo['plan_id'],
            'plan_name'  => $planInfo['plan_name'],
            'max_staff'  => $planInfo['max_staff'],
            'used_staff' => count($staff),
            'staff'      => $staff,
        ], 'Staff list retrieved');
    }

    /**
     * POST /staff
     */
    public function store(Request $request): void
    {
        if (strtolower((string)$request->getAttribute('role')) !== 'owner') {
            Response::error('Only the restaurant owner can add staff members.', 403);
            return;
        }
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'name'     => 'required|min:2',
            'email'    => 'required|email',
            'password' => 'required|min:6',
            'role'     => 'required',
        ]);

        if ($validator->fails()) {
            Response::error('Please fill name, valid email, role and password (min 6 chars)', 422, $validator->errors());
            return;
        }

        $planInfo = $this->resolvePlanLimits($restaurantId);
        $countRow = Database::fetchOne(
            "SELECT COUNT(*) AS cnt FROM restaurant_memberships WHERE restaurant_id = :restro AND COALESCE(is_active, 1) = 1",
            ['restro' => $restaurantId]
        );
        $usedStaff = (int)($countRow['cnt'] ?? 0);

        if ($usedStaff >= $planInfo['max_staff']) {
            Response::error(
                "Staff/device limit reached for {$planInfo['plan_name']} (Max {$planInfo['max_staff']} logins). Upgrade your plan to add more staff.",
                403
            );
            return;
        }

        $name = trim((string)$input['name']);
        $email = trim((string)$input['email']);
        $phone = !empty($input['phone']) ? trim((string)$input['phone']) : null;
        $role = strtoupper(trim((string)$input['role']));
        if ($role === 'RESTAURANT_OWNER') {
            $role = 'MANAGER';
        }

        Database::beginTransaction();
        try {
            $existingUser = Database::fetchOne("SELECT id FROM users WHERE email = :email LIMIT 1", ['email' => $email]);
            if ($existingUser) {
                $userId = (int)$existingUser['id'];
                $alreadyMember = Database::fetchOne(
                    "SELECT id FROM restaurant_memberships WHERE user_id = :uid AND restaurant_id = :restro AND COALESCE(is_active, 1) = 1 LIMIT 1",
                    ['uid' => $userId, 'restro' => $restaurantId]
                );
                if ($alreadyMember) {
                    Database::rollBack();
                    Response::error('This staff email already has access to your restaurant', 422);
                    return;
                }
            } else {
                $hashedPassword = Auth::hashPassword((string)$input['password']);
                Database::execute(
                    "INSERT INTO users (name, email, phone, password, role, created_at, updated_at)
                     VALUES (:name, :email, :phone, :password, :role, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    [
                        'name'     => $name,
                        'email'    => $email,
                        'phone'    => $phone,
                        'password' => $hashedPassword,
                        'role'     => strtolower($role),
                    ]
                );
                $userId = (int)Database::lastInsertId();
            }

            $membershipId = UUID::v4();
            Database::execute(
                "INSERT INTO restaurant_memberships (id, user_id, restaurant_id, branch_id, role, status, is_active, created_at, updated_at)
                 VALUES (:id, :uid, :restro, :branch, :role, 'active', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'     => $membershipId,
                    'uid'    => $userId,
                    'restro' => $restaurantId,
                    'branch' => $branchId,
                    'role'   => $role,
                ]
            );

            Database::commit();
            Response::success([
                'id'       => $membershipId,
                'user_id'  => $userId,
                'name'     => $name,
                'email'    => $email,
                'phone'    => $phone ?? '',
                'role'     => $role,
                'status'   => 'ACTIVE',
                'is_owner' => false,
            ], 'Staff access created successfully', 201);
        } catch (\Throwable $e) {
            Database::rollBack();
            error_log("[Staff Store Error] " . $e->getMessage());
            Response::error('Failed to add staff member. Please try again.', 500);
        }
    }

    /**
     * DELETE /staff/{id}
     */
    public function destroy(Request $request): void
    {
        if (strtolower((string)$request->getAttribute('role')) !== 'owner') {
            Response::error('Only the restaurant owner can remove staff members.', 403);
            return;
        }
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->param('id') ?: $request->query('id');

        if (empty($id)) {
            Response::error('Staff membership ID is required', 400);
            return;
        }

        $member = Database::fetchOne(
            "SELECT id, user_id, role FROM restaurant_memberships WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'restro' => $restaurantId]
        );

        if (!$member) {
            Response::notFound('Staff member not found');
            return;
        }

        if (strtoupper((string)$member['role']) === 'RESTAURANT_OWNER') {
            Response::error('Owner account cannot be removed', 403);
            return;
        }

        Database::execute(
            "UPDATE restaurant_memberships SET is_active = 0, status = 'inactive', updated_at = CURRENT_TIMESTAMP WHERE id = :id AND restaurant_id = :restro",
            ['id' => $id, 'restro' => $restaurantId]
        );

        // Revoke active bearer tokens if user has no remaining active memberships
        if (!empty($member['user_id'])) {
            $otherActive = Database::fetchOne(
                "SELECT id FROM restaurant_memberships WHERE user_id = :uid AND COALESCE(is_active, 1) = 1 LIMIT 1",
                ['uid' => $member['user_id']]
            );
            if (!$otherActive) {
                Database::execute(
                    "DELETE FROM personal_access_tokens WHERE tokenable_id = :uid",
                    ['uid' => $member['user_id']]
                );
            }
        }

        Response::success(['id' => $id], 'Staff access revoked');
    }
}
