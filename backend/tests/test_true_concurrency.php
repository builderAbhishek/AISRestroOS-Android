<?php

/**
 * AISRestroOS True Concurrency Test Harness
 * Spawns 50 parallel processes across separate database connections
 * for Bill Number and KOT Number allocations.
 */

$port = isset($argv[1]) ? (int)$argv[1] : 3308;
$testDb = $argv[2] ?? 'u328317103_restro_temp';
$restroId = '01a207f7-8342-7368-b5de-2019baf99da4';
$workerScript = __DIR__ . '/concurrency_worker.php';

echo "====================================================================\n";
echo "AISRestroOS True Concurrency Test (50 Parallel Processes)\n";
echo "Connecting to: 127.0.0.1:{$port} on database '{$testDb}'\n";
echo "====================================================================\n\n";

function runParallelAllocations(string $type, int $count, string $workerScript, string $restroId, int $port, string $testDb): array
{
    echo "--- Launching {$count} Genuinely Parallel Processes for '{$type}' ---\n";
    $tStart = microtime(true);

    $descriptors = [
        0 => ['pipe', 'r'],
        1 => ['pipe', 'w'],
        2 => ['pipe', 'w'],
    ];

    $processes = [];
    $pipes = [];

    // Launch all processes simultaneously
    for ($i = 1; $i <= $count; $i++) {
        $cmd = "php \"" . addslashes($workerScript) . "\" {$type} \"{$restroId}\" {$port} {$i} \"{$testDb}\"";
        $proc = proc_open($cmd, $descriptors, $pipes[$i]);
        if (!is_resource($proc)) {
            echo "[FATAL] Failed to spawn worker process {$i}\n";
            exit(1);
        }
        $processes[$i] = $proc;
    }

    echo "  [SPAWNED] All {$count} concurrent worker processes launched.\n";

    // Read outputs and wait for completion
    $results = [];
    for ($i = 1; $i <= $count; $i++) {
        $stdout = stream_get_contents($pipes[$i][1]);
        $stderr = stream_get_contents($pipes[$i][2]);
        fclose($pipes[$i][0]);
        fclose($pipes[$i][1]);
        fclose($pipes[$i][2]);
        $exitCode = proc_close($processes[$i]);

        if ($exitCode !== 0) {
            echo "  [WORKER ERROR] Worker {$i} exited with code {$exitCode}: {$stderr}\n";
        }

        $json = json_decode(trim($stdout), true);
        if ($json && isset($json['number'])) {
            $results[] = $json;
        } else {
            echo "  [PARSE ERROR] Worker {$i} invalid output: {$stdout} | stderr: {$stderr}\n";
        }
    }

    $tTotal = round((microtime(true) - $tStart) * 1000, 2);
    echo "  [COMPLETED] All {$count} worker processes finished in {$tTotal} ms.\n\n";

    return $results;
}

// 1. Reset sequence counters for clean concurrency test
$pdo = new PDO("mysql:host=127.0.0.1;port={$port};dbname={$testDb}", 'root', '', [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
]);

$pdo->prepare("
    UPDATE restaurant_settings 
    SET last_bill_number = 0, last_kot_number = 0, updated_at = NOW() 
    WHERE restaurant_id = ? AND key_name = 'sequence'
")->execute([$restroId]);
echo "[SETUP] Sequence counters reset to last_bill_number = 0, last_kot_number = 0.\n\n";

// 2. Test 50 Parallel Bill Allocations
$billResults = runParallelAllocations('bill', 50, $workerScript, $restroId, $port, $testDb);
$billNumbers = array_column($billResults, 'number');

echo "--- BILL NUMBER ALLOCATION AUDIT ---\n";
echo "  Total allocations requested: 50\n";
echo "  Total allocations received:  " . count($billNumbers) . "\n";
echo "  Total unique numbers:        " . count(array_unique($billNumbers)) . "\n";

// Check duplicates
$duplicates = array_filter(array_count_values($billNumbers), fn($c) => $c > 1);
if (count($duplicates) > 0) {
    echo "  [FAIL] Duplicates detected: " . json_encode($duplicates) . "\n";
    exit(1);
} else {
    echo "  [PASS] ZERO DUPLICATE BILL NUMBERS.\n";
}

// Check unbroken sequence (1..50)
$numericParts = array_map(function($num) {
    return (int)preg_replace('/[^0-9]/', '', $num);
}, $billNumbers);
sort($numericParts);

$expectedSequence = range(1, 50);
$missing = array_diff($expectedSequence, $numericParts);
if (count($missing) > 0) {
    echo "  [FAIL] Sequence gaps detected! Missing numbers: " . implode(', ', $missing) . "\n";
    exit(1);
} else {
    echo "  [PASS] UNBROKEN CONTIGUOUS SEQUENCE: INV-0001 through INV-0050 (Zero skipped numbers).\n";
}

echo "  Sample allocated bill numbers: " . implode(', ', array_slice($billNumbers, 0, 5)) . " ... " . end($billNumbers) . "\n\n";

// 3. Test 50 Parallel KOT Allocations
$kotResults = runParallelAllocations('kot', 50, $workerScript, $restroId, $port, $testDb);
$kotNumbers = array_column($kotResults, 'number');

echo "--- KOT NUMBER ALLOCATION AUDIT ---\n";
echo "  Total allocations requested: 50\n";
echo "  Total allocations received:  " . count($kotNumbers) . "\n";
echo "  Total unique numbers:        " . count(array_unique($kotNumbers)) . "\n";

$kotDuplicates = array_filter(array_count_values($kotNumbers), fn($c) => $c > 1);
if (count($kotDuplicates) > 0) {
    echo "  [FAIL] Duplicates detected: " . json_encode($kotDuplicates) . "\n";
    exit(1);
} else {
    echo "  [PASS] ZERO DUPLICATE KOT NUMBERS.\n";
}

$kotNumericParts = array_map(function($num) {
    return (int)preg_replace('/[^0-9]/', '', $num);
}, $kotNumbers);
sort($kotNumericParts);

$kotMissing = array_diff($expectedSequence, $kotNumericParts);
if (count($kotMissing) > 0) {
    echo "  [FAIL] Sequence gaps detected! Missing numbers: " . implode(', ', $kotMissing) . "\n";
    exit(1);
} else {
    echo "  [PASS] UNBROKEN CONTIGUOUS SEQUENCE: KOT-0001 through KOT-0050 (Zero skipped numbers).\n";
}

echo "  Sample allocated KOT numbers: " . implode(', ', array_slice($kotNumbers, 0, 5)) . " ... " . end($kotNumbers) . "\n\n";

echo "====================================================================\n";
echo "CONCURRENCY AUDIT RESULT: 100% SUCCESS (ZERO DUPLICATES, ZERO GAPS)\n";
echo "====================================================================\n";
