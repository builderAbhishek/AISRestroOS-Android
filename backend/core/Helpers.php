<?php

namespace AISRestro\Core;

class Helpers
{
    /**
     * Send CORS headers
     */
    public static function setCorsHeaders(): void
    {
        $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
        $allowedOrigins = [
            'https://restro.trakiyo.in',
            'http://localhost',
            'http://127.0.0.1',
        ];
        $isAllowed = false;
        foreach ($allowedOrigins as $allowed) {
            if ($origin === $allowed || str_starts_with($origin, $allowed . ':')) {
                $isAllowed = true;
                break;
            }
        }

        if ($isAllowed) {
            header("Access-Control-Allow-Origin: {$origin}");
            header('Access-Control-Allow-Credentials: true');
        } else {
            header('Access-Control-Allow-Origin: *');
        }

        header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH');
        header('Access-Control-Allow-Headers: Authorization, Content-Type, X-Requested-With, X-Idempotency-Key, Accept');
        header('Access-Control-Max-Age: 86400');
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: SAMEORIGIN');
    }

    /**
     * Get base URL of current deployment
     */
    public static function getBaseUrl(): string
    {
        $scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
        $host   = $_SERVER['HTTP_HOST'] ?? 'restro.trakiyo.in';
        return "{$scheme}://{$host}/api";
    }

    /**
     * Allocate the next bill sequence number for a restaurant in a transaction-safe manner
     */
    public static function getNextBillNumber(string $restaurantId): string
    {
        // 1. Ensure sequence row exists for restaurant
        $settings = Database::fetchOne(
            "SELECT id, bill_prefix, last_bill_number FROM restaurant_settings WHERE restaurant_id = :restro_id ORDER BY (key_name = 'sequence') DESC, id ASC LIMIT 1",
            ['restro_id' => $restaurantId]
        );

        if (!$settings) {
            Database::execute(
                "INSERT INTO restaurant_settings (restaurant_id, key_name, key_value, bill_prefix, last_bill_number, kot_prefix, last_kot_number, created_at, updated_at) 
                 VALUES (:restro_id, 'sequence', 'Sequence counters initialized', 'INV-', 1, 'KOT-', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ['restro_id' => $restaurantId]
            );
            return "INV-01";
        }

        $settingsId = $settings['id'];
        $prefix = $settings['bill_prefix'] ?: 'INV-';

        // 2. Perform atomic increment
        if (Database::isSqlite()) {
            $nextNum = ((int)$settings['last_bill_number']) + 1;
            Database::execute(
                "UPDATE restaurant_settings SET last_bill_number = :num, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                ['num' => $nextNum, 'id' => $settingsId]
            );
        } else {
            // MySQL 8 / MariaDB 11.8: Atomic sequence increment using LAST_INSERT_ID()
            Database::execute(
                "UPDATE restaurant_settings SET last_bill_number = LAST_INSERT_ID(last_bill_number + 1), updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                ['id' => $settingsId]
            );
            $row = Database::fetchOne("SELECT LAST_INSERT_ID() AS num");
            $nextNum = (int)$row['num'];
        }

        return sprintf('%s%02d', $prefix, $nextNum);
    }

    /**
     * Allocate the next unique restaurant ID in AISR/01, AISR/02 format
     */
    public static function getNextRestaurantId(): string
    {
        try {
            $existingRows = Database::fetchAll("SELECT id FROM restaurants");
            $maxNum = 0;
            foreach ($existingRows as $row) {
                $id = (string)$row['id'];
                if (preg_match('/^AISR\/(\d+)$/i', $id, $matches)) {
                    $num = (int)$matches[1];
                    if ($num > $maxNum) {
                        $maxNum = $num;
                    }
                }
            }
            $nextNum = $maxNum + 1;
            while (true) {
                $candidate = sprintf('AISR/%02d', $nextNum);
                $exists = Database::fetchOne("SELECT id FROM restaurants WHERE id = :id LIMIT 1", ['id' => $candidate]);
                if (!$exists) {
                    return $candidate;
                }
                $nextNum++;
            }
        } catch (\Exception $e) {
            return 'AISR/01';
        }
    }

    /**
     * Allocate the next KOT sequence number for a restaurant in a transaction-safe manner
     */
    public static function getNextKotNumber(string $restaurantId): string
    {
        $settings = Database::fetchOne(
            "SELECT id, kot_prefix, last_kot_number FROM restaurant_settings WHERE restaurant_id = :restro_id ORDER BY (key_name = 'sequence') DESC, id ASC LIMIT 1",
            ['restro_id' => $restaurantId]
        );

        if (!$settings) {
            Database::execute(
                "INSERT INTO restaurant_settings (restaurant_id, key_name, key_value, bill_prefix, last_bill_number, kot_prefix, last_kot_number, created_at, updated_at) 
                 VALUES (:restro_id, 'sequence', 'Sequence counters initialized', 'INV-', 0, 'KOT-', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ['restro_id' => $restaurantId]
            );
            return "KOT-01";
        }

        $settingsId = $settings['id'];
        $prefix = $settings['kot_prefix'] ?: 'KOT-';

        if (Database::isSqlite()) {
            $nextNum = ((int)$settings['last_kot_number']) + 1;
            Database::execute(
                "UPDATE restaurant_settings SET last_kot_number = :num, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                ['num' => $nextNum, 'id' => $settingsId]
            );
        } else {
            // MySQL 8 / MariaDB 11.8: Atomic sequence increment using LAST_INSERT_ID()
            Database::execute(
                "UPDATE restaurant_settings SET last_kot_number = LAST_INSERT_ID(last_kot_number + 1), updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                ['id' => $settingsId]
            );
            $row = Database::fetchOne("SELECT LAST_INSERT_ID() AS num");
            $nextNum = (int)$row['num'];
        }

        return sprintf('%s%02d', $prefix, $nextNum);
    }

    /**
     * Clean restaurant ID for structured UUID prefix (e.g. "AISR/01" -> "AISR01")
     */
    public static function cleanRestaurantPrefix(string $restaurantId): string
    {
        $clean = preg_replace('/[^A-Za-z0-9]/', '', $restaurantId);
        return $clean !== '' ? strtoupper($clean) : 'AISR01';
    }

    /**
     * Allocate the next structured Order UUID (e.g. "AISR01/ORD-01")
     */
    public static function getNextOrderUuid(string $restaurantId): string
    {
        $cleanRestro = self::cleanRestaurantPrefix($restaurantId);

        $settings = Database::fetchOne(
            "SELECT id, last_bill_number FROM restaurant_settings WHERE restaurant_id = :restro_id AND key_name = 'order_seq' LIMIT 1",
            ['restro_id' => $restaurantId]
        );

        if (!$settings) {
            // Seed from existing structured orders or total count so we never collide
            $existingOrders = Database::fetchAll(
                "SELECT uuid FROM orders WHERE restaurant_id = :restro_id",
                ['restro_id' => $restaurantId]
            );
            $maxNum = 0;
            foreach ($existingOrders as $row) {
                if (preg_match('/\/ORD-(\d+)$/i', (string)$row['uuid'], $m)) {
                    $maxNum = max($maxNum, (int)$m[1]);
                }
            }
            $nextNum = $maxNum + 1;
            Database::execute(
                "INSERT INTO restaurant_settings (restaurant_id, key_name, key_value, bill_prefix, last_bill_number, kot_prefix, last_kot_number, created_at, updated_at) 
                 VALUES (:restro_id, 'order_seq', 'Order UUID sequence counter', 'ORD-', :num, 'KOT-', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ['restro_id' => $restaurantId, 'num' => $nextNum]
            );
        } else {
            $settingsId = $settings['id'];
            if (Database::isSqlite()) {
                $nextNum = ((int)$settings['last_bill_number']) + 1;
                Database::execute(
                    "UPDATE restaurant_settings SET last_bill_number = :num, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                    ['num' => $nextNum, 'id' => $settingsId]
                );
            } else {
                Database::execute(
                    "UPDATE restaurant_settings SET last_bill_number = LAST_INSERT_ID(last_bill_number + 1), updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                    ['id' => $settingsId]
                );
                $row = Database::fetchOne("SELECT LAST_INSERT_ID() AS num");
                $nextNum = (int)$row['num'];
            }
        }

        while (true) {
            $candidate = sprintf('%s/ORD-%02d', $cleanRestro, $nextNum);
            $exists = Database::fetchOne("SELECT id FROM orders WHERE uuid = :uuid LIMIT 1", ['uuid' => $candidate]);
            if (!$exists) {
                return $candidate;
            }
            $nextNum++;
        }
    }

    /**
     * Allocate the next structured KOT UUID (e.g. "AISR01/KOT-01")
     */
    public static function getNextKotUuid(string $restaurantId, ?string $kotNumber = null): string
    {
        $cleanRestro = self::cleanRestaurantPrefix($restaurantId);
        $currentKot = $kotNumber ?: self::getNextKotNumber($restaurantId);

        while (true) {
            $candidate = "{$cleanRestro}/{$currentKot}";
            $exists = Database::fetchOne("SELECT id FROM kot_tickets WHERE uuid = :uuid LIMIT 1", ['uuid' => $candidate]);
            if (!$exists) {
                return $candidate;
            }
            $currentKot = self::getNextKotNumber($restaurantId);
        }
    }

    /**
     * Audit log helper
     */
    public static function logAudit(?string $restaurantId, ?int $userId, string $action, ?string $details = null): void
    {
        try {
            $ip = $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';
            Database::execute(
                "INSERT INTO audit_logs (restaurant_id, user_id, action, details, ip_address, created_at) 
                 VALUES (:restro, :user, :act, :det, :ip, CURRENT_TIMESTAMP)",
                [
                    'restro' => $restaurantId,
                    'user'   => $userId,
                    'act'    => $action,
                    'det'    => $details,
                    'ip'     => $ip,
                ]
            );
        } catch (\Exception $e) {
            error_log("[Audit Log Failure] " . $e->getMessage());
        }
    }
}
