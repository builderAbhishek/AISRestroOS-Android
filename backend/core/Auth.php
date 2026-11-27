<?php

namespace AISRestro\Core;

class Auth
{
    /**
     * Validate a bearer token against personal_access_tokens table
     * Supports both Laravel Sanctum format (e.g. "4|abc123...") and plain string tokens
     */
    public static function validateToken(string $tokenString): ?array
    {
        $tokenId = null;
        $plainToken = $tokenString;

        if (str_contains($tokenString, '|')) {
            [$idPart, $plainToken] = explode('|', $tokenString, 2);
            $tokenId = is_numeric($idPart) ? (int)$idPart : null;
        }

        $hashedToken = hash('sha256', $plainToken);

        if ($tokenId !== null) {
            $tokenRow = Database::fetchOne(
                "SELECT * FROM personal_access_tokens WHERE id = :id LIMIT 1",
                ['id' => $tokenId]
            );

            if (!$tokenRow) {
                return null;
            }

            // Verify sha256 hash or exact match
            if (!hash_equals($tokenRow['token'], $hashedToken) && !hash_equals($tokenRow['token'], $plainToken)) {
                return null;
            }
        } else {
            $tokenRow = Database::fetchOne(
                "SELECT * FROM personal_access_tokens WHERE token = :hash OR token = :plain LIMIT 1",
                ['hash' => $hashedToken, 'plain' => $plainToken]
            );

            if (!$tokenRow) {
                return null;
            }
        }

        // Check expiration
        if (!empty($tokenRow['expires_at']) && strtotime($tokenRow['expires_at']) < time()) {
            return null;
        }

        // Update last_used_at
        Database::execute(
            "UPDATE personal_access_tokens SET last_used_at = CURRENT_TIMESTAMP WHERE id = :id",
            ['id' => $tokenRow['id']]
        );

        // Fetch User
        $user = Database::fetchOne(
            "SELECT id, name, email, phone, role FROM users WHERE id = :id LIMIT 1",
            ['id' => $tokenRow['tokenable_id']]
        );

        if (!$user) {
            return null;
        }

        // Fetch User's Active Restaurant Membership
        $membership = Database::fetchOne(
            "SELECT restaurant_id, branch_id, role FROM restaurant_memberships 
             WHERE user_id = :user_id AND (status = 'active' OR is_active = 1) 
             ORDER BY created_at ASC LIMIT 1",
            ['user_id' => $user['id']]
        );

        $restaurantId = $membership['restaurant_id'] ?? null;
        $branchId     = $membership['branch_id'] ?? null;
        $rawRole      = !empty($membership['role']) ? strtolower($membership['role']) : ($user['role'] ?? 'owner');
        $role         = in_array($rawRole, ['restaurant_owner', 'super_admin']) ? 'owner' : $rawRole;

        // If no membership found yet, check if there's any restaurant owned by user
        if (!$restaurantId) {
            $ownedRestro = Database::fetchOne(
                "SELECT id FROM restaurants WHERE owner_id = :owner_id LIMIT 1",
                ['owner_id' => $user['id']]
            );
            if ($ownedRestro) {
                $restaurantId = $ownedRestro['id'];
            }
        }

        return [
            'user'          => $user,
            'restaurant_id' => $restaurantId,
            'branch_id'     => $branchId,
            'role'          => $role,
            'token_id'      => $tokenRow['id'],
        ];
    }

    /**
     * Create personal access token compatible with Laravel Sanctum
     */
    public static function createToken(int $userId, string $tokenName = 'auth_token'): string
    {
        $plainTextToken = bin2hex(random_bytes(32)); // 64-char random hex
        $hashedToken = hash('sha256', $plainTextToken);

        Database::execute(
            "INSERT INTO personal_access_tokens (tokenable_type, tokenable_id, name, token, abilities, created_at, updated_at) 
             VALUES (:type, :id, :name, :token, :abilities, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            [
                'type'      => 'App\\Models\\User',
                'id'        => $userId,
                'name'      => $tokenName,
                'token'     => $hashedToken,
                'abilities' => '["*"]',
            ]
        );

        $tokenId = Database::lastInsertId();
        return "{$tokenId}|{$plainTextToken}";
    }

    /**
     * Revoke a token
     */
    public static function revokeToken(string $tokenString): bool
    {
        if (str_contains($tokenString, '|')) {
            [$idPart, $plainToken] = explode('|', $tokenString, 2);
            $tokenId = is_numeric($idPart) ? (int)$idPart : null;
            if ($tokenId !== null) {
                Database::execute("DELETE FROM personal_access_tokens WHERE id = :id", ['id' => $tokenId]);
                return true;
            }
        }

        $hashedToken = hash('sha256', $tokenString);
        Database::execute(
            "DELETE FROM personal_access_tokens WHERE token = :hash OR token = :plain",
            ['hash' => $hashedToken, 'plain' => $tokenString]
        );
        return true;
    }

    /**
     * Password hashing compatible with Laravel
     */
    public static function hashPassword(string $password): string
    {
        return password_hash($password, PASSWORD_BCRYPT);
    }

    /**
     * Password verification
     */
    public static function verifyPassword(string $password, string $hash): bool
    {
        return password_verify($password, $hash);
    }
}
