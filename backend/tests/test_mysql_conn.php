<?php
try {
    $pdo = new PDO('mysql:host=127.0.0.1;port=3307', 'root', '');
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $ver = $pdo->query("SELECT VERSION()")->fetchColumn();
    echo "SUCCESS: Connected to isolated test server on port 3307! Version: {$ver}\n";
} catch (Exception $e) {
    echo "FAILED: " . $e->getMessage() . "\n";
}
