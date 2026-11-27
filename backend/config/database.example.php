<?php
/**
 * Database Configuration Example
 * Copy this file to database.php and configure your Hostinger MySQL credentials.
 * DO NOT commit real database passwords or credentials to source control.
 */

return [
    'driver'    => 'mysql',
    'host'      => 'localhost',
    'port'      => 3306,
    'database'  => 'u328317103_restro',
    'username'  => 'u328317103_restrouser',
    'password'  => 'your_strong_database_password_here',
    'charset'   => 'utf8mb4',
    'collation' => 'utf8mb4_unicode_ci',
    'options'   => [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ],
];
