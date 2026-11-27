<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\Auth;
use AISRestro\Core\Validator;
use AISRestro\Core\UUID;
use AISRestro\Core\Helpers;

class AuthController
{
    /**
     * POST /auth/register
     */
    public function register(Request $request): void
    {
        $input = $request->input();

        $validator = Validator::make($input, [
            'name'            => 'required|min:2',
            'email'           => 'required|email',
            'password'        => 'required|min:6',
            'restaurant_name' => 'required|min:2',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
            return;
        }

        $email = trim($input['email']);

        // Check if email already registered
        $existing = Database::fetchOne("SELECT id FROM users WHERE email = :email LIMIT 1", ['email' => $email]);
        if ($existing) {
            Response::error('This email address is already registered. Please login.', 422, [
                'email' => ['The email has already been taken.'],
            ]);
            return;
        }

        Database::beginTransaction();

        try {
            $hashedPassword = Auth::hashPassword($input['password']);
            $phone = !empty($input['phone']) ? trim($input['phone']) : null;

            Database::execute(
                "INSERT INTO users (name, email, phone, password, role, created_at, updated_at) 
                 VALUES (:name, :email, :phone, :password, 'owner', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'name'     => trim($input['name']),
                    'email'    => $email,
                    'phone'    => $phone,
                    'password' => $hashedPassword,
                ]
            );

            $userId = (int)Database::lastInsertId();

            $restaurantId = Helpers::getNextRestaurantId();
            $branchId     = 'BR-' . strtoupper(substr(UUID::v4(), 0, 8));
            $membershipId = UUID::v4();
            $restroName   = trim($input['restaurant_name']);
            $slug         = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $restroName))) . '-' . substr(UUID::v4(), 0, 5);

            Database::execute(
                "INSERT INTO restaurants (id, name, slug, owner_id, email, phone, currency, is_active, created_at, updated_at) 
                 VALUES (:id, :name, :slug, :owner, :email, :phone, 'INR', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'    => $restaurantId,
                    'name'  => $restroName,
                    'slug'  => $slug,
                    'owner' => $userId,
                    'email' => $email,
                    'phone' => $phone,
                ]
            );

            Database::execute(
                "INSERT INTO branches (id, restaurant_id, name, code, is_main, is_main_branch, is_active, phone, created_at, updated_at) 
                 VALUES (:id, :restro, 'Main Branch', 'MAIN-01', 1, 1, 1, :phone, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'     => $branchId,
                    'restro' => $restaurantId,
                    'phone'  => $phone,
                ]
            );

            Database::execute(
                "INSERT INTO restaurant_memberships (id, user_id, restaurant_id, branch_id, role, status, is_active, created_at, updated_at) 
                 VALUES (:id, :uid, :restro, :branch, 'RESTAURANT_OWNER', 'active', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'     => $membershipId,
                    'uid'    => $userId,
                    'restro' => $restaurantId,
                    'branch' => $branchId,
                ]
            );

            // Initialize restaurant sequence settings starting at 0
            Database::execute(
                "INSERT INTO restaurant_settings (restaurant_id, key_name, key_value, bill_prefix, last_bill_number, kot_prefix, last_kot_number, created_at, updated_at) 
                 VALUES (:restro, 'sequence', 'Sequence counters initialized', 'INV-', 0, 'KOT-', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ['restro' => $restaurantId]
            );

            $token = Auth::createToken($userId, 'ais_restro_pos');

            Database::commit();

            Helpers::logAudit($restaurantId, $userId, 'REGISTER', "Registered restaurant: {$restroName}");

            Response::success([
                'token'         => $token,
                'user'          => [
                    'id'    => $userId,
                    'name'  => trim($input['name']),
                    'email' => $email,
                    'phone' => $phone,
                ],
                'restaurant_id' => $restaurantId,
                'branch_id'     => $branchId,
                'role'          => 'owner',
            ], 'Restaurant registered successfully', 201);
        } catch (\Exception $e) {
            Database::rollBack();
            error_log("[Register Error] " . $e->getMessage());
            Response::error('Failed to register account. Please try again.', 500);
        }
    }

    /**
     * POST /auth/login
     */
    public function login(Request $request): void
    {
        $input = $request->input();

        $validator = Validator::make($input, [
            'email'    => 'required|email',
            'password' => 'required',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
            return;
        }

        $email = trim($input['email']);
        $password = $input['password'];

        $user = Database::fetchOne(
            "SELECT id, name, email, phone, password, role FROM users WHERE email = :email LIMIT 1",
            ['email' => $email]
        );

        if (!$user || !Auth::verifyPassword($password, $user['password'])) {
            Response::error('Invalid email or password.', 401);
            return;
        }

        $userId = (int)$user['id'];

        // Fetch membership (supports both status='active' and is_active=1, uppercase & lowercase roles)
        $membership = Database::fetchOne(
            "SELECT restaurant_id, branch_id, role FROM restaurant_memberships 
             WHERE user_id = :uid AND (status = 'active' OR is_active = 1) 
             ORDER BY created_at ASC LIMIT 1",
            ['uid' => $userId]
        );

        $restaurantId = $membership['restaurant_id'] ?? null;
        $branchId     = $membership['branch_id'] ?? null;
        $rawRole      = !empty($membership['role']) ? strtolower($membership['role']) : ($user['role'] ?? 'owner');
        $role         = in_array($rawRole, ['restaurant_owner', 'super_admin']) ? 'owner' : $rawRole;

        if (!$restaurantId) {
            $owned = Database::fetchOne(
                "SELECT id FROM restaurants WHERE owner_id = :owner LIMIT 1",
                ['owner' => $userId]
            );
            if ($owned) {
                $restaurantId = $owned['id'];
            }
        }

        $restaurantName = null;
        if ($restaurantId) {
            $restroRow = Database::fetchOne("SELECT name FROM restaurants WHERE id = :id LIMIT 1", ['id' => $restaurantId]);
            $restaurantName = $restroRow['name'] ?? null;
        }

        $token = Auth::createToken($userId, 'ais_restro_pos');

        Helpers::logAudit($restaurantId, $userId, 'LOGIN', "User logged in from POS");

        Response::success([
            'token'           => $token,
            'user'            => [
                'id'    => $userId,
                'name'  => $user['name'],
                'email' => $user['email'],
                'phone' => $user['phone'],
            ],
            'restaurant_id'   => $restaurantId,
            'restaurant_name' => $restaurantName,
            'branch_id'       => $branchId,
            'role'            => $role,
        ], 'Logged in successfully');
    }

    /**
     * POST /auth/logout
     */
    public function logout(Request $request): void
    {
        $token = $request->getBearerToken();
        if ($token) {
            Auth::revokeToken($token);
        }
        Response::success(null, 'Logged out successfully');
    }

    /**
     * GET /auth/me
     */
    public function me(Request $request): void
    {
        $user = $request->getAttribute('user');
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $role = $request->getAttribute('role');

        $restaurantName = null;
        if ($restaurantId) {
            $restroRow = Database::fetchOne("SELECT name FROM restaurants WHERE id = :id LIMIT 1", ['id' => $restaurantId]);
            $restaurantName = $restroRow['name'] ?? null;
        }

        Response::success([
            'token'           => $request->getBearerToken(),
            'user'            => [
                'id'    => (int)$user['id'],
                'name'  => $user['name'],
                'email' => $user['email'],
                'phone' => $user['phone'],
            ],
            'restaurant_id'   => $restaurantId,
            'restaurant_name' => $restaurantName,
            'branch_id'       => $branchId,
            'role'            => $role,
        ], 'Authenticated user retrieved');
    }
}
