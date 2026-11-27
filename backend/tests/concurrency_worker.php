<?php

/**
 * Worker script for concurrency testing.
 * Runs in its own separate CLI process with its own separate PDO connection.
 */

require_once dirname(__DIR__) . '/core/Database.php';
require_once dirname(__DIR__) . '/core/Helpers.php';

use AISRestro\Core\Database;
use AISRestro\Core\Helpers;

$type     = $argv[1] ?? 'bill'; // 'bill' or 'kot'
$restroId = $argv[2] ?? '01a207f7-8342-7368-b5de-2019baf99da4';
$port     = isset($argv[3]) ? (int)$argv[3] : 3308;
$workerId = isset($argv[4]) ? (int)$argv[4] : 1;
$database = $argv[5] ?? 'u328317103_restro_temp';

// Configure separate connection for this process
Database::setConfig([
    'driver'   => 'mysql',
    'host'     => '127.0.0.1',
    'port'     => $port,
    'database' => $database,
    'username' => 'root',
    'password' => '',
]);

$tStart = microtime(true);

if ($type === 'bill') {
    $num = Helpers::getNextBillNumber($restroId);
} else {
    $num = Helpers::getNextKotNumber($restroId);
}

$tDuration = round((microtime(true) - $tStart) * 1000, 2);

echo json_encode([
    'worker_id'   => $workerId,
    'type'        => $type,
    'number'      => $num,
    'duration_ms' => $tDuration,
    'pid'         => getmypid(),
]);
echo "\n";
exit(0);
