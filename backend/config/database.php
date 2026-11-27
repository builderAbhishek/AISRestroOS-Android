<?php
/**
 * Local Test Database Configuration
 * Configured for isolated local MariaDB 11.8 testing on port 3308
 */

return [
    'driver'    => 'mysql',
    'host'      => '127.0.0.1',
    'port'      => 3308,
    'database'  => 'u328317103_restro_sync_test',
    'username'  => 'root',
    'password'  => '',
    'charset'   => 'utf8mb4',
    'collation' => 'utf8mb4_unicode_ci',
    'options'   => [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ],
];
