<?php

$dbName = $argv[1] ?? 'u328317103_restro_concurrency_test';
$port = isset($argv[2]) ? (int)$argv[2] : 3308;

$pdo = new PDO("mysql:host=127.0.0.1;port={$port}", "root", "", [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$pdo->exec("DROP DATABASE IF EXISTS `{$dbName}`");
$pdo->exec("CREATE DATABASE `{$dbName}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$pdo->exec("USE `{$dbName}`");

$dumpPath = "C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437937148_fcfe28be.txt";
$dumpSql = str_replace("\r\n", "\n", file_get_contents($dumpPath));
$pdo->exec("SET FOREIGN_KEY_CHECKS = 0;");
$pdo->exec("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';");

function stripSqlComments(string $sql): string {
    $lines = explode("\n", $sql);
    $cleanLines = [];
    foreach ($lines as $line) {
        $trimmed = trim($line);
        if ($trimmed === "" || str_starts_with($trimmed, "--")) continue;
        $cleanLines[] = $line;
    }
    $clean = implode("\n", $cleanLines);
    return trim(preg_replace("/\/\*[^!].*?\*\//s", "", $clean));
}

foreach (explode(";\n", $dumpSql) as $q) {
    $stmt = stripSqlComments($q);
    if (!empty($stmt)) {
        try { $pdo->exec($stmt); } catch (Exception $e) {}
    }
}
$pdo->exec("SET FOREIGN_KEY_CHECKS = 1;");

$migSql = str_replace("\r\n", "\n", file_get_contents(__DIR__ . "/../database/migrations/FINAL_PRODUCTION_MIGRATION.sql"));
foreach (explode(";\n", $migSql) as $mq) {
    $stmt = stripSqlComments($mq);
    if (!empty($stmt)) {
        try { 
            $pdo->exec($stmt); 
        } catch (Exception $e) { 
            echo "Migration error: " . $e->getMessage() . "\n"; 
            exit(1); 
        }
    }
}

echo "Database {$dbName} prepared successfully.\n";
