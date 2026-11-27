<?php
$passwords = ['', 'root', 'admin', 'password', '123456', 'root123', 'root@123', 'mysql', 'secret'];
foreach ($passwords as $pwd) {
    try {
        $pdo = new PDO('mysql:host=127.0.0.1;port=3306', 'root', $pwd);
        echo "SUCCESS! Password is: " . ($pwd === '' ? '(empty)' : $pwd) . "\n";
        exit(0);
    } catch (Exception $e) {
        // try next
    }
}
echo "No common password matched.\n";
