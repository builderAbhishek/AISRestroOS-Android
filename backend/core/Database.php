<?php

namespace AISRestro\Core;

use PDO;
use PDOException;
use Exception;

class Database
{
    private static ?PDO $instance = null;
    private static array $config = [];
    private static bool $softDeleteChecked = false;

    /**
     * Set explicit config (useful for testing or custom runtime injection)
     */
    public static function setConfig(array $config): void
    {
        self::$config = $config;
        self::$instance = null;
    }

    /**
     * Get active PDO instance
     */
    public static function getConnection(): PDO
    {
        if (self::$instance !== null) {
            return self::$instance;
        }

        if (empty(self::$config)) {
            $configPath = dirname(__DIR__) . '/config/database.php';
            if (file_exists($configPath)) {
                self::$config = require $configPath;
            } else {
                $examplePath = dirname(__DIR__) . '/config/database.example.php';
                if (file_exists($examplePath)) {
                    self::$config = require $examplePath;
                }
            }
        }

        $driver = self::$config['driver'] ?? 'mysql';

        try {
            if ($driver === 'sqlite') {
                $dsn = 'sqlite:' . (self::$config['database'] ?? ':memory:');
                $pdo = new PDO($dsn, null, null, [
                    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                ]);
                $pdo->exec('PRAGMA foreign_keys = ON;');
                
                // Emulate MySQL functions in SQLite for seamless testing
                $pdo->sqliteCreateFunction('UNIX_TIMESTAMP', function ($date = null) {
                    return $date ? strtotime((string)$date) : time();
                }, -1);
                $pdo->sqliteCreateFunction('CURDATE', function () {
                    return date('Y-m-d');
                }, 0);
                $pdo->sqliteCreateFunction('NOW', function () {
                    return date('Y-m-d H:i:s');
                }, 0);

                self::$instance = $pdo;
                return self::$instance;
            }

            $host     = self::$config['host'] ?? '127.0.0.1';
            $port     = self::$config['port'] ?? 3306;
            $database = self::$config['database'] ?? '';
            $username = self::$config['username'] ?? '';
            $password = self::$config['password'] ?? '';
            $charset  = self::$config['charset'] ?? 'utf8mb4';

            $dsn = "mysql:host={$host};port={$port};dbname={$database};charset={$charset}";

            $options = self::$config['options'] ?? [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ];

            self::$instance = new PDO($dsn, $username, $password, $options);
            return self::$instance;
        } catch (PDOException $e) {
            error_log("[Database Connection Error] " . $e->getMessage());
            throw new Exception("Database connection failure. Please verify database credentials.");
        }
    }

    /**
     * Check if currently running on SQLite
     */
    public static function isSqlite(): bool
    {
        return (self::$config['driver'] ?? 'mysql') === 'sqlite';
    }

    /**
     * Returns " FOR UPDATE" on MySQL, or empty string on SQLite
     */
    public static function forUpdate(): string
    {
        return self::isSqlite() ? '' : ' FOR UPDATE';
    }

    /**
     * Execute a prepared query and return PDOStatement
     */
    public static function query(string $sql, array $params = []): \PDOStatement
    {
        $db = self::getConnection();
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return $stmt;
    }

    /**
     * Fetch all rows
     */
    public static function fetchAll(string $sql, array $params = []): array
    {
        return self::query($sql, $params)->fetchAll();
    }

    /**
     * Fetch single row
     */
    public static function fetchOne(string $sql, array $params = []): ?array
    {
        $result = self::query($sql, $params)->fetch();
        return $result ?: null;
    }

    /**
     * Execute an INSERT/UPDATE/DELETE query and return affected rows
     */
    public static function execute(string $sql, array $params = []): int
    {
        $stmt = self::query($sql, $params);
        return $stmt->rowCount();
    }

    /**
     * Get last insert ID
     */
    public static function lastInsertId(): string
    {
        return self::getConnection()->lastInsertId();
    }

    /**
     * Ensure `is_deleted` column exists on `orders` table (0 = active, 1 = soft-deleted)
     */
    public static function ensureOrderSoftDeleteColumn(): void
    {
        if (self::$softDeleteChecked) {
            return;
        }
        self::$softDeleteChecked = true;
        try {
            if (self::isSqlite()) {
                $cols = self::fetchAll("PRAGMA table_info(orders)");
                $hasCol = false;
                foreach ($cols as $c) {
                    if (($c['name'] ?? '') === 'is_deleted') {
                        $hasCol = true;
                        break;
                    }
                }
                if (!$hasCol) {
                    self::execute("ALTER TABLE orders ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
                }
            } else {
                $col = self::fetchOne("SHOW COLUMNS FROM orders LIKE 'is_deleted'");
                if (!$col) {
                    self::execute("ALTER TABLE orders ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0");
                }
            }
        } catch (\Throwable $e) {
            // Ignore if already exists or insufficient DDL permission
        }
    }

    /**
     * Begin transaction
     */
    public static function beginTransaction(): bool
    {
        $db = self::getConnection();
        return $db->inTransaction() ? true : $db->beginTransaction();
    }

    /**
     * Commit transaction
     */
    public static function commit(): bool
    {
        $db = self::getConnection();
        return $db->inTransaction() ? $db->commit() : true;
    }

    /**
     * Rollback transaction
     */
    public static function rollBack(): bool
    {
        $db = self::getConnection();
        return $db->inTransaction() ? $db->rollBack() : true;
    }
}
