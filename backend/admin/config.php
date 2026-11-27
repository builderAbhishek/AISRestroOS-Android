<?php
/**
 * AISRestroOS Super Admin Panel - Core Configuration & Database Bootstrap
 */

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

error_reporting(E_ALL & ~E_DEPRECATED & ~E_NOTICE);
ini_set('display_errors', '0');

// Super Admin Credentials (Change these after uploading to Hostinger)
define('SUPER_ADMIN_EMAIL', 'admin@aisrestro.com');
define('SUPER_ADMIN_PASS', 'AISAdmin@2026');

// Resolve Database Config safely across common Hostinger directory layouts
$dbHost = 'localhost';
$dbPort = '3306';
$dbName = 'u328317103_restro';
$dbUser = 'u328317103_restrouser';
$dbPass = '';

$configPaths = [
    __DIR__ . '/../api/config/database.php',
    __DIR__ . '/../config/database.php',
    __DIR__ . '/../../config/database.php',
    __DIR__ . '/config/database.php',
];

foreach ($configPaths as $path) {
    if (file_exists($path)) {
        $content = file_get_contents($path);
        if ($content !== false) {
            if (preg_match("/'host'\s*=>\s*['\"]([^'\"]+)['\"]/i", $content, $m))     $dbHost = $m[1];
            if (preg_match("/'port'\s*=>\s*(\d+)/i", $content, $m))                   $dbPort = $m[1];
            if (preg_match("/'database'\s*=>\s*['\"]([^'\"]+)['\"]/i", $content, $m)) $dbName = $m[1];
            if (preg_match("/'username'\s*=>\s*['\"]([^'\"]+)['\"]/i", $content, $m)) $dbUser = $m[1];
            if (preg_match("/'password'\s*=>\s*['\"]([^'\"]*)['\"]/i", $content, $m)) $dbPass = $m[1];
            break;
        }
    }
}

$pdo = null;
$dbError = null;

try {
    $dsn = "mysql:host={$dbHost};port={$dbPort};dbname={$dbName};charset=utf8mb4";
    $pdo = new PDO($dsn, $dbUser, $dbPass, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]);
    $pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
} catch (PDOException $e) {
    $dbError = $e->getMessage();
}

function e(?string $str): string
{
    return htmlspecialchars((string)$str, ENT_QUOTES, 'UTF-8');
}

function generateUuidV4(): string
{
    $data = random_bytes(16);
    $data[6] = chr(ord($data[6]) & 0x0f | 0x40);
    $data[8] = chr(ord($data[8]) & 0x3f | 0x80);
    return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
}

function calculateNextRestaurantId(PDO $pdo): string
{
    try {
        $rows = $pdo->query("SELECT id FROM restaurants")->fetchAll();
        $maxNum = 0;
        foreach ($rows as $row) {
            $id = (string)$row['id'];
            if (preg_match('/^AISR\/(\d+)$/i', $id, $matches)) {
                $num = (int)$matches[1];
                if ($num > $maxNum) {
                    $maxNum = $num;
                }
            }
        }
        return sprintf('AISR/%02d', $maxNum + 1);
    } catch (Throwable $e) {
        return 'AISR/01';
    }
}

function requireAdminAuth(): void
{
    if (empty($_SESSION['super_admin_logged_in'])) {
        header('Location: login.php');
        exit;
    }
}

function setFlash(string $type, string $message): void
{
    $_SESSION['admin_flash'] = ['type' => $type, 'message' => $message];
}

function getFlash(): ?array
{
    if (!empty($_SESSION['admin_flash'])) {
        $flash = $_SESSION['admin_flash'];
        unset($_SESSION['admin_flash']);
        return $flash;
    }
    return null;
}

function getAllRestaurants(PDO $pdo): array
{
    try {
        return $pdo->query("SELECT id, name, email, phone, is_active FROM restaurants ORDER BY created_at DESC")->fetchAll();
    } catch (Throwable $e) {
        return [];
    }
}
