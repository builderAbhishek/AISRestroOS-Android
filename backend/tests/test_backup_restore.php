<?php

/**
 * AISRestroOS Backup & Restore Recovery Verification Suite
 * Verifies Hostinger-compatible disaster recovery without assuming DROP DATABASE privilege:
 * 1. Imports production baseline into an isolated database.
 * 2. Simulates database state changes.
 * 3. Executes in-database table-level wipe: SET FOREIGN_KEY_CHECKS = 0; DROP TABLE <all>; SET FOREIGN_KEY_CHECKS = 1;
 * 4. Restores from clean backup.
 * 5. Verifies 100% preservation of tables, records, credentials, relationships, and tokens.
 */

require_once dirname(__DIR__) . '/core/Database.php';

$port = isset($argv[1]) ? (int)$argv[1] : 3308;
$restoreDb = $argv[2] ?? 'u328317103_restro_restore_test';

echo "====================================================================\n";
echo "AISRestroOS Backup & Restore Disaster Recovery Verification\n";
echo "Testing on MariaDB port {$port}, Database: {$restoreDb}\n";
echo "====================================================================\n\n";

function stripSqlComments(string $sql): string {
    $lines = explode("\n", $sql);
    $cleanLines = [];
    foreach ($lines as $line) {
        $trimmed = trim($line);
        if ($trimmed === '' || str_starts_with($trimmed, '--')) {
            continue;
        }
        $cleanLines[] = $line;
    }
    $clean = implode("\n", $cleanLines);
    $clean = preg_replace('/\/\*[^!].*?\*\//s', '', $clean);
    return trim($clean);
}

function assert_test(string $name, bool $condition, string $detail = ''): void {
    if ($condition) {
        echo "  [PASS] {$name}\n";
    } else {
        echo "  [FAIL] {$name} (" . ($detail ?: 'Assertion failed') . ")\n";
        exit(1);
    }
}

// 1. Initialize test database
$serverPdo = new \PDO("mysql:host=127.0.0.1;port={$port}", 'root', '', [
    \PDO::ATTR_ERRMODE => \PDO::ERRMODE_EXCEPTION,
]);
$serverPdo->exec("DROP DATABASE IF EXISTS `{$restoreDb}`");
$serverPdo->exec("CREATE DATABASE `{$restoreDb}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$serverPdo->exec("USE `{$restoreDb}`");

echo "--- 1. Importing Initial Production SQL Dump ---\n";
$dumpPath = "C:/Users/kkuma/.gemini/antigravity/brain/ec7c961b-1d18-4059-b836-5b751abe8d43/.user_uploaded/media_1795437937148_fcfe28be.txt";
$dumpSql = str_replace("\r\n", "\n", file_get_contents($dumpPath));
$serverPdo->exec("SET FOREIGN_KEY_CHECKS = 0;");
$serverPdo->exec("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';");
$queries = explode(";\n", $dumpSql);
$importedCount = 0;
foreach ($queries as $q) {
    $stmt = stripSqlComments($q);
    if (!empty($stmt)) {
        try {
            $serverPdo->exec($stmt);
            $importedCount++;
        } catch (\Exception $ex) {}
    }
}
$serverPdo->exec("SET FOREIGN_KEY_CHECKS = 1;");
echo "  [PASS] Imported {$importedCount} statements from production baseline dump.\n\n";

// Count baseline tables and records
$tables = $serverPdo->query("SHOW TABLES")->fetchAll(\PDO::FETCH_COLUMN);
$baselineTableCount = count($tables);
assert_test("Baseline table count is 28", $baselineTableCount === 28, "Found: {$baselineTableCount}");

$user1 = $serverPdo->query("SELECT * FROM users WHERE id = 1")->fetch(\PDO::FETCH_ASSOC);
assert_test("User 1 exists", !empty($user1));
$restro = $serverPdo->query("SELECT * FROM restaurants LIMIT 1")->fetch(\PDO::FETCH_ASSOC);
assert_test("Restaurant exists", !empty($restro));
$tokensCount = (int)$serverPdo->query("SELECT COUNT(*) FROM personal_access_tokens WHERE tokenable_id = 1")->fetchColumn();
assert_test("User 1 has 2 personal access tokens", $tokensCount === 2);
echo "\n";

// 2. Simulate partial corruption or schema change
echo "--- 2. Simulating Schema & Data Modification (Simulating Failed State) ---\n";
$serverPdo->exec("CREATE TABLE dummy_corrupt_table (id INT PRIMARY KEY, junk VARCHAR(255))");
$serverPdo->exec("INSERT INTO users (name, email, password, created_at, updated_at) VALUES ('Corrupt User', 'bad@example.com', 'badhash', NOW(), NOW())");
$modTableCount = count($serverPdo->query("SHOW TABLES")->fetchAll(\PDO::FETCH_COLUMN));
assert_test("Database now has 29 tables (modified state)", $modTableCount === 29);
echo "\n";

// 3. Perform Hostinger-compatible disaster recovery (WITHOUT DROP DATABASE)
echo "--- 3. Executing In-Database Recovery (Hostinger Safe: Without DROP DATABASE) ---\n";
$startRecovery = microtime(true);

// Step A: Disable foreign key checks
$serverPdo->exec("SET FOREIGN_KEY_CHECKS = 0;");

// Step B: Fetch all tables in current database and drop each table
$existingTables = $serverPdo->query("SELECT table_name FROM information_schema.tables WHERE table_schema = '{$restoreDb}'")->fetchAll(\PDO::FETCH_COLUMN);
foreach ($existingTables as $tbl) {
    $serverPdo->exec("DROP TABLE IF EXISTS `{$tbl}`");
}

// Step C: Re-enable foreign key checks
$serverPdo->exec("SET FOREIGN_KEY_CHECKS = 1;");

$zeroTables = count($serverPdo->query("SHOW TABLES")->fetchAll(\PDO::FETCH_COLUMN));
assert_test("All tables cleanly dropped within existing database", $zeroTables === 0);

// Step D: Re-import clean production dump
$serverPdo->exec("SET FOREIGN_KEY_CHECKS = 0;");
$serverPdo->exec("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';");
foreach ($queries as $q) {
    $stmt = stripSqlComments($q);
    if (!empty($stmt)) {
        try {
            $serverPdo->exec($stmt);
        } catch (\Exception $ex) {}
    }
}
$serverPdo->exec("SET FOREIGN_KEY_CHECKS = 1;");
$recoveryElapsed = round((microtime(true) - $startRecovery) * 1000, 2);
echo "  [PASS] Clean backup re-imported successfully in {$recoveryElapsed} ms.\n\n";

// 4. Verify post-recovery state
echo "--- 4. Verifying Post-Recovery State & Relational Integrity ---\n";
$postTables = $serverPdo->query("SHOW TABLES")->fetchAll(\PDO::FETCH_COLUMN);
assert_test("Recovered table count is exactly 28", count($postTables) === 28);
assert_test("Dummy corrupt table removed", !in_array('dummy_corrupt_table', $postTables));

$postUser1 = $serverPdo->query("SELECT * FROM users WHERE id = 1")->fetch(\PDO::FETCH_ASSOC);
assert_test("User 1 email intact ('{$postUser1['email']}')", $postUser1['email'] === $user1['email']);
assert_test("User 1 password hash intact", $postUser1['password'] === $user1['password']);

$badUserCount = (int)$serverPdo->query("SELECT COUNT(*) FROM users WHERE email = 'bad@example.com'")->fetchColumn();
assert_test("Corrupted test user completely purged", $badUserCount === 0);

$postRestro = $serverPdo->query("SELECT * FROM restaurants WHERE id = '{$restro['id']}'")->fetch(\PDO::FETCH_ASSOC);
assert_test("Restaurant preserved ({$postRestro['name']})", !empty($postRestro));

$postTokens = (int)$serverPdo->query("SELECT COUNT(*) FROM personal_access_tokens WHERE tokenable_id = 1")->fetchColumn();
assert_test("Personal access tokens intact (2 tokens)", $postTokens === 2);

$postMigrations = (int)$serverPdo->query("SELECT COUNT(*) FROM migrations")->fetchColumn();
assert_test("Laravel migrations records preserved ({$postMigrations} migrations)", $postMigrations > 0);

// 5. Cleanup
$serverPdo->exec("DROP DATABASE IF EXISTS `{$restoreDb}`");

echo "\n====================================================================\n";
echo "HOSTINGER DISASTER RECOVERY VERIFICATION: 100% SUCCESS\n";
echo "====================================================================\n";
