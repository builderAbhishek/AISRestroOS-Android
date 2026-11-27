-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Nov 27, 2026 at 11:21 AM
-- Server version: 11.8.9-MariaDB-log
-- PHP Version: 7.2.34

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `u328317103_restro`
--

-- --------------------------------------------------------

--
-- Table structure for table `audit_logs`
--

CREATE TABLE `audit_logs` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `user_id` bigint(20) UNSIGNED DEFAULT NULL,
  `action` varchar(255) NOT NULL,
  `details` text DEFAULT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `audit_logs`
--

INSERT INTO `audit_logs` (`id`, `restaurant_id`, `user_id`, `action`, `details`, `ip_address`, `created_at`) VALUES
(1, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.167', '2026-11-24 07:41:13'),
(2, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '112.79.202.3', '2026-11-24 07:57:35'),
(3, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '2409:40e3:1028:8e69:3457:cff:fef8:d7ca', '2026-11-24 08:15:47'),
(4, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.167', '2026-11-24 17:28:42'),
(5, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.167', '2026-11-24 18:26:11'),
(6, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.167', '2026-11-24 19:51:39'),
(7, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.167', '2026-11-24 20:20:34'),
(8, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.20', '2026-11-25 04:58:52'),
(17, 'AISR/01', NULL, 'ASSIGN_SUBSCRIPTION', 'Assigned plan PLAN-072FC2E8 (ACTIVE)', '157.119.83.20', '2026-11-25 20:29:25'),
(18, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.20', '2026-11-25 20:34:40'),
(19, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '2402:3a80:11a7:2001:a1a:2e:bd29:ccb8', '2026-11-26 05:50:26'),
(20, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '2409:40e3:388:bc89:1c47:6bff:feb9:b0d1', '2026-11-27 04:38:01'),
(21, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '2409:40e3:388:bc89:1c47:6bff:feb9:b0d1', '2026-11-27 05:20:30'),
(22, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '2409:40e3:388:bc89:4c48:5d33:a9f2:8238', '2026-11-27 05:23:26'),
(23, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.2', '2026-11-27 07:02:13'),
(24, 'AISR/01', 1, 'LOGIN', 'User logged in from POS', '157.119.83.2', '2026-11-27 11:01:20');

-- --------------------------------------------------------

--
-- Table structure for table `branches`
--

CREATE TABLE `branches` (
  `id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `code` varchar(50) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `is_main_branch` tinyint(1) NOT NULL DEFAULT 0,
  `is_main` tinyint(1) NOT NULL DEFAULT 1,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `branches`
--

INSERT INTO `branches` (`id`, `restaurant_id`, `name`, `code`, `address`, `phone`, `is_main_branch`, `is_main`, `is_active`, `created_at`, `updated_at`) VALUES
('BR-E8A5036E', 'AISR/01', 'Main Branch', 'MAIN-01', 'Ganakul, Shohratgarh', '7379839937', 1, 1, 1, '2026-11-24 07:40:52', '2026-11-24 07:40:52');

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `display_order` int(11) NOT NULL DEFAULT 0,
  `sync_seq` bigint(20) UNSIGNED NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `categories`
--

INSERT INTO `categories` (`id`, `restaurant_id`, `branch_id`, `name`, `display_order`, `sync_seq`, `created_at`, `updated_at`) VALUES
('09ce172a-54e3-4bdb-8461-4450741ded69', 'AISR/01', NULL, 'Mutton & Egg Gravy', 11, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('129ddc77-f010-44d3-8575-4d2e25f251f7', 'AISR/01', NULL, 'Indian Breads', 13, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('28622d79-318f-4aa5-ab0f-feeb4ae41714', 'AISR/01', NULL, 'Pizza', 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 'AISR/01', NULL, 'Fried Rice', 16, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('40b2ed20-b917-464a-bdfd-6ad9162da253', 'AISR/01', NULL, 'Non-Veg Pizza', 2, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('44459c69-92e9-46d3-97f9-8f5396e75d51', 'AISR/01', NULL, 'Pizza Toppings', 3, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('65708565-70a7-4f17-9597-0adb9b3b4bb7', 'AISR/01', NULL, 'Soup', 17, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('72ebedcf-94fb-47d9-835e-0f0188ec3171', 'AISR/01', NULL, 'Biryani & Rice', 12, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('759db71b-e273-451e-9c0e-e79b1d4b9538', 'AISR/01', NULL, 'Shakes & Lassi', 5, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('7b1d9ffe-949e-471f-aeca-7193d6f1461a', 'AISR/01', NULL, 'Chowmein', 7, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('89d72b39-814e-4592-b22b-77915f420774', 'AISR/01', NULL, 'Chicken Snacks', 14, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 'AISR/01', NULL, 'Fried & Snacks', 9, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('990f597e-b935-44bf-ad27-308b868f1a3c', 'AISR/01', NULL, 'Indian Main Course', 18, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('9c67384e-d5cc-46f6-99f4-394d97201c54', 'AISR/01', NULL, 'Manchurian & Chilli', 15, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('a161bcf3-72a0-4984-b813-882728d1c898', 'AISR/01', NULL, 'Momos', 8, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('c15b0239-9f64-4d7d-9db4-fc0d5492785f', 'AISR/01', NULL, 'Raita', 19, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('d6b3e2f8-fbe6-4d95-91d3-a72d54d1c033', 'AISR/01', NULL, 'Coffee & Tea', 6, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('d8654fbb-5456-4e00-92bd-9b5c3666201e', 'AISR/01', NULL, 'Chicken Gravy', 10, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('edcf8c0f-ddaf-43e8-9779-6e6c87b99d6b', 'AISR/01', NULL, 'Special Dish', 20, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19'),
('f37d73fb-e337-4d42-be1a-470eb2de622d', 'AISR/01', NULL, 'Burger', 4, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19');

-- --------------------------------------------------------

--
-- Table structure for table `customers`
--

CREATE TABLE `customers` (
  `id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `mobile_number` varchar(50) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `is_regular` tinyint(1) NOT NULL DEFAULT 0,
  `sync_seq` bigint(20) UNSIGNED NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `daily_purchases`
--

CREATE TABLE `daily_purchases` (
  `id` varchar(64) NOT NULL,
  `restaurant_id` varchar(64) NOT NULL,
  `branch_id` varchar(64) DEFAULT NULL,
  `item_name` varchar(255) NOT NULL,
  `category` varchar(100) NOT NULL DEFAULT 'Vegetables',
  `quantity` decimal(10,2) NOT NULL DEFAULT 1.00,
  `unit` varchar(50) NOT NULL DEFAULT 'kg',
  `vendor_name` varchar(255) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `payment_mode` varchar(50) NOT NULL DEFAULT 'CASH',
  `expense_type` varchar(50) NOT NULL DEFAULT 'PURCHASE',
  `purchase_date` date NOT NULL,
  `notes` text DEFAULT NULL,
  `created_by_user_id` bigint(20) UNSIGNED DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `daily_purchases`
--

INSERT INTO `daily_purchases` (`id`, `restaurant_id`, `branch_id`, `item_name`, `category`, `quantity`, `unit`, `vendor_name`, `amount`, `payment_mode`, `expense_type`, `purchase_date`, `notes`, `created_by_user_id`, `created_at`, `updated_at`) VALUES
('PUR-0E63AE62', 'AISR/01', 'BR-E8A5036E', 'Sabji Mandi (Daily Vegetables)', 'Vegetables (Mandi)', 1.00, 'bill', 'Local Mandi (Vegetable Market)', 1450.00, 'CASH', 'EXPENSE', '2026-11-26', NULL, 1, '2026-11-26 05:53:22', '2026-11-26 05:53:22'),
('PUR-490B34E8', 'AISR/01', 'BR-E8A5036E', 'Sabji Mandi (Daily Vegetables)', 'Vegetables (Mandi)', 1.00, 'bill', 'Local Mandi (Vegetable Market)', 2508.00, 'CASH', 'EXPENSE', '2026-11-27', NULL, 1, '2026-11-27 04:01:50', '2026-11-27 04:01:50'),
('PUR-C00E22BE', 'AISR/01', 'BR-E8A5036E', 'other', 'Vegetables', 1.00, 'bill', 'Local Market / Store', 180.00, 'CASH', 'EXPENSE', '2026-11-26', NULL, 1, '2026-11-26 05:53:51', '2026-11-26 05:53:51');

-- --------------------------------------------------------

--
-- Table structure for table `devices`
--

CREATE TABLE `devices` (
  `id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `device_name` varchar(255) NOT NULL,
  `device_type` varchar(50) NOT NULL DEFAULT 'POS',
  `mac_address` varchar(50) DEFAULT NULL,
  `paper_width` varchar(20) NOT NULL DEFAULT '58mm',
  `device_code` varchar(32) DEFAULT NULL,
  `device_token` varchar(255) DEFAULT NULL,
  `is_authorized` tinyint(1) NOT NULL DEFAULT 1,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `last_synced_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `devices`
--

INSERT INTO `devices` (`id`, `restaurant_id`, `branch_id`, `device_name`, `device_type`, `mac_address`, `paper_width`, `device_code`, `device_token`, `is_authorized`, `is_active`, `last_synced_at`, `created_at`, `updated_at`) VALUES
('DEV-41154C18', 'AISR/01', 'BR-E8A5036E', 'table_221', 'PRINTER', '60:6E:41:15:4C:18', '58mm', 'DEV-41154C18', NULL, 1, 1, NULL, '2026-11-26 05:50:38', '2026-11-26 05:50:38');

-- --------------------------------------------------------

--
-- Table structure for table `failed_jobs`
--

CREATE TABLE `failed_jobs` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `connection` text NOT NULL,
  `queue` text NOT NULL,
  `payload` longtext NOT NULL,
  `exception` longtext NOT NULL,
  `failed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `idempotency_keys`
--

CREATE TABLE `idempotency_keys` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `restaurant_id` char(36) DEFAULT NULL,
  `idempotency_key` varchar(64) DEFAULT NULL,
  `key_value` varchar(255) DEFAULT NULL,
  `request_path` varchar(255) DEFAULT NULL,
  `resource_uuid` char(36) DEFAULT NULL,
  `resource_type` varchar(50) DEFAULT NULL,
  `response_payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`response_payload`)),
  `response_code` smallint(5) UNSIGNED NOT NULL DEFAULT 200,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `inventory_items`
--

CREATE TABLE `inventory_items` (
  `id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `item_name` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `quantity` decimal(10,2) NOT NULL DEFAULT 0.00,
  `current_stock` decimal(10,2) NOT NULL DEFAULT 0.00,
  `unit_of_measure` varchar(16) NOT NULL DEFAULT 'kg',
  `unit` varchar(50) NOT NULL DEFAULT 'kg',
  `min_stock_alert` decimal(10,2) NOT NULL DEFAULT 10.00,
  `low_stock_alert` decimal(10,2) NOT NULL DEFAULT 5.00,
  `cost_per_unit` decimal(10,2) NOT NULL DEFAULT 0.00,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `jobs`
--

CREATE TABLE `jobs` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `queue` varchar(255) NOT NULL,
  `payload` longtext NOT NULL,
  `attempts` tinyint(3) UNSIGNED NOT NULL,
  `reserved_at` int(10) UNSIGNED DEFAULT NULL,
  `available_at` int(10) UNSIGNED NOT NULL,
  `created_at` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `job_batches`
--

CREATE TABLE `job_batches` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `total_jobs` int(11) NOT NULL,
  `pending_jobs` int(11) NOT NULL,
  `failed_jobs` int(11) NOT NULL,
  `failed_job_ids` longtext NOT NULL,
  `options` mediumtext DEFAULT NULL,
  `cancelled_at` int(11) DEFAULT NULL,
  `created_at` int(11) NOT NULL,
  `finished_at` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `kot_items`
--

CREATE TABLE `kot_items` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `kot_ticket_id` bigint(20) UNSIGNED NOT NULL,
  `kot_id` bigint(20) UNSIGNED DEFAULT NULL,
  `item_name` varchar(255) NOT NULL,
  `quantity` int(11) NOT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `kot_items`
--

INSERT INTO `kot_items` (`id`, `kot_ticket_id`, `kot_id`, `item_name`, `quantity`, `notes`, `created_at`, `updated_at`) VALUES
(10, 2, 2, 'अंडा कड़ाही', 1, NULL, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(11, 2, 2, 'अंडा चाउमीन (Full)', 1, NULL, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(12, 2, 2, 'अंडा फ्राइड राइस शेजवान (Half)', 1, NULL, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(13, 2, 2, 'बूंदी रायता', 1, NULL, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(14, 2, 2, 'बनाना शेक', 3, NULL, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(15, 2, 2, 'मटन करी (Half (4 पीस))', 2, NULL, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(39, 1, 1, 'अंडा चाउमीन (Half)', 3, NULL, '2026-11-27 07:05:06', '2026-11-27 07:05:06'),
(40, 3, 3, 'Test Item', 1, NULL, '2026-11-27 07:06:22', '2026-11-27 07:06:22'),
(41, 4, 4, 'अंडा कड़ाही', 2, NULL, '2026-11-27 11:13:51', '2026-11-27 11:13:51'),
(42, 4, 4, 'अंडा करी', 1, NULL, '2026-11-27 11:13:51', '2026-11-27 11:13:51');

-- --------------------------------------------------------

--
-- Table structure for table `kot_tickets`
--

CREATE TABLE `kot_tickets` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `uuid` char(36) NOT NULL,
  `order_id` bigint(20) UNSIGNED NOT NULL,
  `order_uuid` char(36) DEFAULT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `ticket_number` varchar(50) DEFAULT NULL,
  `kot_number` varchar(50) DEFAULT NULL,
  `table_number` varchar(50) DEFAULT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'PENDING',
  `reprint_count` int(11) NOT NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `kot_tickets`
--

INSERT INTO `kot_tickets` (`id`, `uuid`, `order_id`, `order_uuid`, `restaurant_id`, `branch_id`, `ticket_number`, `kot_number`, `table_number`, `status`, `reprint_count`, `created_at`, `updated_at`) VALUES
(1, 'fe13d588-e928-4dd7-8d14-dbb4104f7dc8', 1, 'KOT-01', 'AISR/01', 'BR-E8A5036E', 'KOT-01', 'KOT-01', NULL, 'PENDING', 0, '2026-11-25 20:35:07', '2026-11-27 07:05:06'),
(2, 'dfb61039-6295-4ea8-ab17-5eba9107ec1e', 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', 'AISR/01', 'BR-E8A5036E', 'INV-0015', 'INV-0015', 'G8', 'COMPLETED', 0, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(3, 'b3579796-5b53-4436-9e00-d898dc7f960b', 3, '99999999-1111-4444-8888-111111111111', 'AISR/01', 'BR-E8A5036E', 'KOT-99', 'KOT-99', 'G2', 'PENDING', 0, '2026-11-27 05:43:04', '2026-11-27 07:06:22'),
(4, '240d3548-b7b9-4010-8f69-076c0259ad9e', 5, 'd0a95ec2-1317-4636-a4e0-d4e8e1ec8853', 'AISR/01', 'BR-E8A5036E', 'KOT-D0A95E', 'KOT-D0A95E', 'G1', 'PENDING', 0, '2026-11-27 11:13:51', '2026-11-27 11:13:51');

-- --------------------------------------------------------

--
-- Table structure for table `menu_items`
--

CREATE TABLE `menu_items` (
  `id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `code` varchar(32) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `category` varchar(255) NOT NULL DEFAULT 'General',
  `category_id` varchar(64) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `tax_mode` varchar(50) NOT NULL DEFAULT 'WITHOUT_TAX',
  `tax_percentage` varchar(50) NOT NULL DEFAULT 'None',
  `current_stock` int(11) NOT NULL DEFAULT 100,
  `is_default_tax` tinyint(1) NOT NULL DEFAULT 1,
  `is_favourite` tinyint(1) NOT NULL DEFAULT 0,
  `image_url` varchar(500) DEFAULT NULL,
  `is_available` tinyint(1) NOT NULL DEFAULT 1,
  `is_veg` tinyint(1) NOT NULL DEFAULT 1,
  `sync_seq` bigint(20) UNSIGNED NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  `variants_json` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `menu_items`
--

INSERT INTO `menu_items` (`id`, `restaurant_id`, `branch_id`, `code`, `name`, `category`, `category_id`, `price`, `tax_mode`, `tax_percentage`, `current_stock`, `is_default_tax`, `is_favourite`, `image_url`, `is_available`, `is_veg`, `sync_seq`, `created_at`, `updated_at`, `variants_json`) VALUES
('002dbe86-09bc-4fa5-8ee4-ea7441cd2919', 'AISR/01', NULL, '114', 'वेज पुलाव', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 210.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":210},{\"name\":\"Full\",\"price\":350}]'),
('003fa6ef-31ac-4509-bec2-09b5d6b63764', 'AISR/01', NULL, '14', 'पनीर टिक्का पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 369.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":369},{\"name\":\"Medium\",\"price\":559},{\"name\":\"Large\",\"price\":699}]'),
('00c038de-7ff3-4ab8-9411-c4b7cc17cd86', 'AISR/01', NULL, '129', 'चिकन कीमा पराठा', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 130.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('0381d4fc-37d8-426a-b9a5-a92eaa4a6db4', 'AISR/01', NULL, '128', 'पनीर पराठा', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('04e164a6-994b-494e-952f-0da38e084539', 'AISR/01', NULL, '149', 'वेज फ्राइड राइस', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":140},{\"name\":\"Full\",\"price\":260}]'),
('05b6d225-e546-49a4-8df2-cc7ab829cc8b', 'AISR/01', NULL, '139', 'चिकन मंचूरियन', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":140},{\"name\":\"Full\",\"price\":260}]'),
('05b72a74-21cf-4af8-873b-50b97e5a24b9', 'AISR/01', NULL, '127', 'लच्छा पराठा', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 70.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('06d70d10-c33b-4e8c-bc0b-7e9e912d5b10', 'AISR/01', NULL, '18', 'वेज क्लब पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 449.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":449},{\"name\":\"Medium\",\"price\":800},{\"name\":\"Large\",\"price\":1049}]'),
('076429db-8b96-4cb3-a8cb-dc4c27021a95', 'AISR/01', NULL, '142', 'चिकन चिल्ली', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 150.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":150},{\"name\":\"Full\",\"price\":280}]'),
('0abfd834-eb5a-400d-80cc-9aff238c3abc', 'AISR/01', NULL, '97', 'मटन करी', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 550.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":550},{\"name\":\"Full (8 पीस)\",\"price\":1000}]'),
('0c3d17e6-eb8d-418a-afb1-5d86130c4b18', 'AISR/01', NULL, '184', 'मशरूम हांडी', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 390.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('0f6724d3-02e6-4ebd-a970-811e4abdab97', 'AISR/01', NULL, '11', 'पनीर स्वीट कॉर्न पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 279.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":279},{\"name\":\"Medium\",\"price\":449},{\"name\":\"Large\",\"price\":529}]'),
('11593503-a7ac-4dbd-8d18-6dd526cea707', 'AISR/01', NULL, '06', 'वेज चीज पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 250.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":250},{\"name\":\"Medium\",\"price\":399},{\"name\":\"Large\",\"price\":499}]'),
('11b7ff43-ee27-4166-a80d-b8f4968a1da7', 'AISR/01', NULL, '150', 'चिकन फ्राइड राइस', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 180.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":180},{\"name\":\"Full\",\"price\":300}]'),
('1202f5dc-b841-4a30-af66-9309bf68e32c', 'AISR/01', NULL, '03', 'अनियन पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 190.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":190},{\"name\":\"Medium\",\"price\":290},{\"name\":\"Large\",\"price\":390}]'),
('12fecd87-20a1-457c-8ed4-93e81704ce09', 'AISR/01', NULL, '183', 'मशरूम कड़ाही', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 380.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('15ac9b4f-1055-445f-9cd6-123205ae01e9', 'AISR/01', NULL, '126', 'आलू पराठा', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('1a118c10-9e39-428f-8b45-df7eaf917324', 'AISR/01', NULL, '42', 'हॉट कॉफी', 'Coffee & Tea', 'd6b3e2f8-fbe6-4d95-91d3-a72d54d1c033', 50.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('1a6df5b8-056d-4f18-8126-36254bf8a1b2', 'AISR/01', NULL, '101', 'मटन मसाला', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 580.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":580},{\"name\":\"Full (8 पीस)\",\"price\":1030}]'),
('1b213910-8c4e-4843-afc3-512e11f8f1c0', 'AISR/01', NULL, '155', 'चिकन फ्राइड राइस शेजवान', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 190.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":190},{\"name\":\"Full\",\"price\":340}]'),
('1ba987da-7ea0-401a-9226-d31deec7bcce', 'AISR/01', NULL, '136', 'पथरी कलेजी फ्राई', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 220.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (5 पीस)\",\"price\":220},{\"name\":\"Full (10 पीस)\",\"price\":380}]'),
('1c8ff441-0dc4-48a7-a816-e33191b10f28', 'AISR/01', NULL, '120', 'तंदूरी बटर रोटी', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 40.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('1d6a48b3-028a-4267-b081-7a76d01ccd8c', 'AISR/01', NULL, '29', 'वेज पैटीज बर्गर', 'Burger', 'f37d73fb-e337-4d42-be1a-470eb2de622d', 60.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('1e53f1ec-e739-4df8-a697-2a8954566936', 'AISR/01', NULL, '190', 'मिक्स रायता', 'Raita', 'c15b0239-9f64-4d7d-9db4-fc0d5492785f', 130.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('1eeb76e7-12dd-4321-b596-973c9a3922a4', 'AISR/01', NULL, '09', 'पेरी पेरी पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 279.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":279},{\"name\":\"Medium\",\"price\":449},{\"name\":\"Large\",\"price\":529}]'),
('20b0b044-9e72-4236-999b-3aba89e6b340', 'AISR/01', NULL, '148', 'चिकन ड्रैगन', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 180.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":180},{\"name\":\"Full\",\"price\":340}]'),
('22e3fd84-bdc9-420d-bd2f-44191af53060', 'AISR/01', NULL, '141', 'पनीर चिल्ली', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 170.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":170},{\"name\":\"Full\",\"price\":320}]'),
('2402a1c8-088b-454e-8789-e12fd057dff6', 'AISR/01', NULL, '166', 'दाल मखनी', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 200.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('244ce369-8466-4444-803b-8abcc0457586', 'AISR/01', NULL, '195', 'बटर स्कॉच कोल्ड कॉफी', 'Special Dish', 'edcf8c0f-ddaf-43e8-9779-6e6c87b99d6b', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('25430405-ea63-413a-ac68-f178f917fe51', 'AISR/01', NULL, '103', 'मटन रारा', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 620.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":620},{\"name\":\"Full (8 पीस)\",\"price\":1100}]'),
('28ba0c29-237e-4ad4-ad8b-8691a0114a53', 'AISR/01', NULL, '124', 'पनीर नान', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('29d172f5-686a-4977-a273-6ea27c2a66b9', 'AISR/01', NULL, '41', 'मिल्क टी', 'Coffee & Tea', 'd6b3e2f8-fbe6-4d95-91d3-a72d54d1c033', 25.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('2cd27080-571b-4944-ab04-f165a540222c', 'AISR/01', NULL, '181', 'आलू गोभी मटर', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 220.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('2e022487-aa8b-4d9c-b674-086650c62d5a', 'AISR/01', NULL, '71', 'पनीर अफगानी मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 130.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":130},{\"name\":\"Full\",\"price\":250}]'),
('2e0c556e-9831-4468-9523-3a6c18b373cf', 'AISR/01', NULL, '157', 'वेज सूप', 'Soup', '65708565-70a7-4f17-9597-0adb9b3b4bb7', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('2f11b3f0-a135-4b28-bea1-3fcaf4e4903a', 'AISR/01', NULL, '19', 'क्लासिक पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 449.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":449},{\"name\":\"Medium\",\"price\":800},{\"name\":\"Large\",\"price\":1049}]'),
('300cfaf1-e818-47b3-a482-7d8ddfed931c', 'AISR/01', NULL, '61', 'अंडा हक्का चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":90},{\"name\":\"Full\",\"price\":170}]'),
('348ec33f-be66-4544-ba4a-10bae21990d2', 'AISR/01', NULL, '83', 'अंडा रोल', 'Fried & Snacks', '8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":200}]'),
('34f1e883-e749-4d43-8eff-62e184bc3a90', 'AISR/01', NULL, '16', 'चौधरी स्पेशल पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 389.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":389},{\"name\":\"Medium\",\"price\":589},{\"name\":\"Large\",\"price\":850}]'),
('351abfa8-ad92-452a-b05e-1b64c5e4e95c', 'AISR/01', NULL, '59', 'चिकन हॉट गार्लिक चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":220}]'),
('360735db-8f13-4ba1-afd0-4e2e85249870', 'AISR/01', NULL, '91', 'चिकन मसाला', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 430.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":430},{\"name\":\"Full (8 पीस)\",\"price\":790}]'),
('3946d9d8-9906-4eb1-b0a0-6db3c0e4f56c', 'AISR/01', NULL, '186', 'मशरूम मसाला', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 390.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('396cce76-526b-460b-b16b-d7d9d4d7ff26', 'AISR/01', NULL, '189', 'बूंदी रायता', 'Raita', 'c15b0239-9f64-4d7d-9db4-fc0d5492785f', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('3998c737-c878-4b3a-8db5-274eff9b75d6', 'AISR/01', NULL, '63', 'वेज मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 60.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":60},{\"name\":\"Full\",\"price\":120}]'),
('3a15ba61-1e3f-4451-8eb0-aa0c08d16465', 'AISR/01', NULL, '188', 'जीरा रायता', 'Raita', 'c15b0239-9f64-4d7d-9db4-fc0d5492785f', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('3c6bd203-14e7-4959-ba7e-b17523679876', 'AISR/01', NULL, '134', 'चिकन टिक्का', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 360.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (5 पीस)\",\"price\":360},{\"name\":\"Full (10 पीस)\",\"price\":620}]'),
('3c8ac2b2-e4f4-4cff-a20d-b777a42e885a', 'AISR/01', NULL, '116', 'मटन पुलाव', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 370.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":370},{\"name\":\"Full\",\"price\":600}]'),
('3fb58201-7d9b-4847-8d6f-76288ab68f70', 'AISR/01', NULL, '119', 'तंदूरी रोटी', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 35.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('3fb637b0-3a4d-4024-838d-e9b9a4c75c7c', 'AISR/01', NULL, '146', 'क्रिस्पी कॉर्न', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 150.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":150},{\"name\":\"Full\",\"price\":280}]'),
('40ca0238-793e-4d31-8e7d-211a897f0437', 'AISR/01', NULL, '162', 'चिकन सूप', 'Soup', '65708565-70a7-4f17-9597-0adb9b3b4bb7', 130.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('419653c5-4c5a-4abb-9a37-1fa8ad03f8a8', 'AISR/01', NULL, '138', 'वेज मंचूरियन', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":120},{\"name\":\"Full\",\"price\":230}]'),
('424fc698-ef78-445f-9f0a-d4c71dbf852c', 'AISR/01', NULL, '176', 'पालक पनीर', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 380.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('426b568e-dab3-408c-b1d0-4ef78976c409', 'AISR/01', NULL, '15', 'पनीर बुरजी पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 369.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":369},{\"name\":\"Medium\",\"price\":559},{\"name\":\"Large\",\"price\":699}]'),
('431ba494-6439-4231-948a-f29c90fbb213', 'AISR/01', NULL, '160', 'वेज स्वीट कॉर्न सूप', 'Soup', '65708565-70a7-4f17-9597-0adb9b3b4bb7', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('447cecd5-096d-4ff4-9ddb-fa0b75d1a050', 'AISR/01', NULL, '70', 'चिकन अफगानी मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 130.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":130},{\"name\":\"Full\",\"price\":250}]'),
('44d1468e-4c89-4c8a-b3fc-a2da72ac81cf', 'AISR/01', NULL, '37', 'बनाना शेक', 'Shakes & Lassi', '759db71b-e273-451e-9c0e-e79b1d4b9538', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('453ce193-7fbf-4715-b489-c12f1fa41954', 'AISR/01', NULL, '62', 'अंडा सिंगापुरी चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":100},{\"name\":\"Full\",\"price\":190}]'),
('46450ea1-b34a-42e0-a24b-824cc8e8dbc4', 'AISR/01', NULL, '90', 'चिकन दो प्याजा बोनलेस', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 450.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":450},{\"name\":\"Full (8 पीस)\",\"price\":830}]'),
('4665b517-5564-4244-b979-2709abff9e8a', 'AISR/01', NULL, '163', 'चिकन हॉट एंड सॉर सूप', 'Soup', '65708565-70a7-4f17-9597-0adb9b3b4bb7', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('4679b00e-1662-4d79-aeea-2390eb01b6d4', 'AISR/01', NULL, '94', 'बटर चिकन', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 430.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":430},{\"name\":\"Full (8 पीस)\",\"price\":790}]'),
('4771dfb7-5136-44aa-9924-4d659bf02a53', 'AISR/01', NULL, '76', 'चिकन झाल मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":160},{\"name\":\"Full\",\"price\":310}]'),
('498f5481-ed56-4374-9b65-0bb977c421f5', 'AISR/01', NULL, '89', 'चिकन दो प्याजा', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 430.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":430},{\"name\":\"Full (8 पीस)\",\"price\":820}]'),
('4a5cad38-9c2e-4893-abfd-1d64de16a5a0', 'AISR/01', NULL, '13', 'चिल्ली पनीर पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 369.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":369},{\"name\":\"Medium\",\"price\":559},{\"name\":\"Large\",\"price\":699}]'),
('4e7b1854-d39d-485a-90a0-28f73ce752c8', 'AISR/01', NULL, '22', 'चिकन तंदूरी टिक्का पिज्जा', 'Non-Veg Pizza', '40b2ed20-b917-464a-bdfd-6ad9162da253', 350.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":350},{\"name\":\"Medium\",\"price\":530},{\"name\":\"Large\",\"price\":670}]'),
('530a23d9-7384-4e15-b618-0caa2d7db446', 'AISR/01', NULL, '159', 'वेज हॉट एंड सॉर सूप', 'Soup', '65708565-70a7-4f17-9597-0adb9b3b4bb7', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('5500ed53-b51c-4a40-9538-b454086cc961', 'AISR/01', NULL, '39', 'बटरस्कोच शेक', 'Shakes & Lassi', '759db71b-e273-451e-9c0e-e79b1d4b9538', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('565255bc-19ce-4f79-b9c5-274449095abe', 'AISR/01', NULL, '60', 'अंडा चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 80.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":80},{\"name\":\"Full\",\"price\":150}]'),
('565a7336-59e5-49cc-9d7d-26cd486e5343', 'AISR/01', NULL, '98', 'मटन कोरमा', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 560.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":560},{\"name\":\"Full (8 पीस)\",\"price\":1010}]'),
('56c051c7-cba2-49cb-9764-e5a836caeeb0', 'AISR/01', NULL, '104', 'मटन कोल्हापुरी', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 580.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('576927d7-0c6e-43d2-bdbb-0925620f6cf7', 'AISR/01', NULL, '95', 'बटर चिकन बोनलेस', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 450.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":450},{\"name\":\"Full (8 पीस)\",\"price\":820}]'),
('5a177b37-7bd9-4d5a-8532-cc1e825ad2f2', 'AISR/01', NULL, '12', 'फार्म हाउस पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 299.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":299},{\"name\":\"Medium\",\"price\":469},{\"name\":\"Large\",\"price\":649}]'),
('5ab458bd-7c02-4466-bb55-6b10d95aa232', 'AISR/01', NULL, '17', 'चीज ब्लास्ट पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 389.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":389},{\"name\":\"Medium\",\"price\":589},{\"name\":\"Large\",\"price\":850}]'),
('5b634ac3-5249-4fa9-bec5-c8b7363a5c99', 'AISR/01', NULL, '67', 'चिकन फ्राई मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":90},{\"name\":\"Full\",\"price\":180}]'),
('5baf7372-0994-496f-bb1f-5080b4d362a8', 'AISR/01', NULL, '08', 'स्पाइसी गार्लिक पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 279.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":279},{\"name\":\"Medium\",\"price\":449},{\"name\":\"Large\",\"price\":529}]'),
('5e3cb3d5-6515-425b-bfd3-63c75c25c013', 'AISR/01', NULL, '23', 'चिकन स्मोकी पिज्जा', 'Non-Veg Pizza', '40b2ed20-b917-464a-bdfd-6ad9162da253', 350.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":350},{\"name\":\"Medium\",\"price\":530},{\"name\":\"Large\",\"price\":670}]'),
('5f87395a-c0aa-44a4-a301-da022d8afb8f', 'AISR/01', NULL, '53', 'अंडा शेजवान चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":90},{\"name\":\"Full\",\"price\":170}]'),
('5fe4cd54-70e0-4b10-9b83-df872e44bf30', 'AISR/01', NULL, '174', 'पनीर पसंदा', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 440.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('606ff5c9-9183-4926-a264-265254e8a6cc', 'AISR/01', NULL, '50', 'पनीर सिंगापुरी चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":120},{\"name\":\"Full\",\"price\":220}]'),
('608881b1-e25c-4062-9128-d443421f9afb', 'AISR/01', NULL, '154', 'पनीर फ्राइड राइस शेजवान', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 200.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":200},{\"name\":\"Full\",\"price\":380}]'),
('60d924ce-4858-487f-9362-e16ca0aca737', 'AISR/01', NULL, '144', 'हनी चिल्ली पोटैटो', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":190}]'),
('65b3e648-ce69-486a-bcba-c597f63b1a54', 'AISR/01', NULL, '118', 'प्लेन राइस', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":120},{\"name\":\"Full\",\"price\":200}]'),
('663a9b6e-57ed-4117-92d7-cdcfffd45566', 'AISR/01', NULL, '187', 'प्लेन दही', 'Raita', 'c15b0239-9f64-4d7d-9db4-fc0d5492785f', 70.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('68b44848-b639-4828-848f-73513eadcf6e', 'AISR/01', NULL, '87', 'हांडी चिकन', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 420.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":420},{\"name\":\"Full (8 पीस)\",\"price\":790}]'),
('68e8defb-baa5-4267-8720-4c29848da33f', 'AISR/01', NULL, '30', 'वेज चीज बर्गर', 'Burger', 'f37d73fb-e337-4d42-be1a-470eb2de622d', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('69cb2ab9-f3a4-46ba-835f-af649b3b5875', 'AISR/01', NULL, '05', 'मशरूम पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 230.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":230},{\"name\":\"Medium\",\"price\":399},{\"name\":\"Large\",\"price\":469}]'),
('6ab672c4-d516-47b3-af5d-21ae51164bbf', 'AISR/01', NULL, '46', 'वेज चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 60.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":60},{\"name\":\"Full\",\"price\":110}]'),
('6ad8aba0-43fb-425e-bc11-e07a1afdc47c', 'AISR/01', NULL, '57', 'चिकन सिंगापुरी चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":200}]'),
('6cfbbe3a-56c5-4739-8424-6960469db71b', 'AISR/01', NULL, '49', 'पनीर हक्का चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":200}]'),
('6fa0b973-337e-4dd2-95e9-f01fd9beeee6', 'AISR/01', NULL, '77', 'पनीर झाल मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":160},{\"name\":\"Full\",\"price\":310}]'),
('706486f2-2406-4841-be17-33badd27d073', 'AISR/01', NULL, '180', 'आलू गोभी', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 220.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('70f4f7d0-dfeb-4404-9274-db640eafff4b', 'AISR/01', NULL, '75', 'वेज झाल मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 130.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":130},{\"name\":\"Full\",\"price\":250}]'),
('72c22fec-a175-423d-b4cb-7b6fc4b2b78f', 'AISR/01', NULL, '35', 'स्ट्रॉबेरी शेक', 'Shakes & Lassi', '759db71b-e273-451e-9c0e-e79b1d4b9538', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('7447580f-3efb-492f-9005-8dd374278b18', 'AISR/01', NULL, '45', 'बटर स्कॉच कोल्ड कॉफी', 'Coffee & Tea', 'd6b3e2f8-fbe6-4d95-91d3-a72d54d1c033', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('7684ee51-5ab0-4d24-8603-47baa7b724ab', 'AISR/01', NULL, '147', 'चिकन 65', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 180.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":180},{\"name\":\"Full\",\"price\":340}]'),
('79e13d91-96f2-4092-98a9-5a3c5eab0164', 'AISR/01', NULL, '99', 'मटन कड़ाही', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 570.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":570},{\"name\":\"Full (8 पीस)\",\"price\":1020}]'),
('7a38505b-c9ce-4d8e-8515-477e32cb7707', 'AISR/01', NULL, '193', 'क्लासिक कोल्ड कॉफी', 'Special Dish', 'edcf8c0f-ddaf-43e8-9779-6e6c87b99d6b', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('7b6d31ed-871e-4efd-aa05-cbfe35e11be6', 'AISR/01', NULL, '48', 'वेज हक्का चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 80.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":80},{\"name\":\"Full\",\"price\":150}]'),
('7dd7cb25-0e45-413d-9a90-d78627fb9be8', 'AISR/01', NULL, '152', 'अंडा फ्राइड राइस', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 170.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":170},{\"name\":\"Full\",\"price\":280}]'),
('81ef049c-78ca-408c-87fe-a255e6e134af', 'AISR/01', NULL, '168', 'शाही पनीर', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 370.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('85b5ad63-85f4-4aea-aad6-ebaf1a4497d6', 'AISR/01', NULL, '68', 'पनीर फ्राई मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":100},{\"name\":\"Full\",\"price\":190}]'),
('85c92a00-cca4-4b4b-9f86-dddcdbb71ea4', 'AISR/01', NULL, '02', 'क्लासिक पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 190.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":190},{\"name\":\"Medium\",\"price\":290},{\"name\":\"Large\",\"price\":390}]'),
('89cc40cf-87fa-43fb-9a5c-b092fe675f44', 'AISR/01', NULL, '177', 'मलाई कोफ्ता', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 460.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('8a00a76d-abcd-4ac7-a50a-71b799e0618b', 'AISR/01', NULL, '192', 'चिकन दम बिरयानी', 'Special Dish', 'edcf8c0f-ddaf-43e8-9779-6e6c87b99d6b', 240.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Price\",\"price\":240}]'),
('8ce3ec07-b3a2-44fd-8e8c-cb6492ba92f1', 'AISR/01', NULL, '121', 'प्लेन नान', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 50.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('8e9af9ea-0a23-4441-aefa-d480cae4ecf4', 'AISR/01', NULL, '125', 'गार्लिक नान', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('8f1c997d-4484-4b32-86cf-00c449aac307', 'AISR/01', NULL, '131', 'चिकन फ्राई', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 270.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Quarter (4 पीस)\",\"price\":270},{\"name\":\"Half (8 पीस)\",\"price\":470},{\"name\":\"Full (16 पीस)\",\"price\":910}]'),
('90635965-2bf4-4d2f-a01e-34aec6997f1b', 'AISR/01', NULL, '140', 'पनीर मंचूरियन', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 170.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":170},{\"name\":\"Full\",\"price\":320}]'),
('91fedfa9-f6c5-43a3-8223-859a6f60a6a8', 'AISR/01', NULL, '82', 'पनीर स्प्रिंग रोल', 'Fried & Snacks', '8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":90},{\"name\":\"Full\",\"price\":170}]'),
('92103b49-0e9b-4789-8953-c7296eba88f1', 'AISR/01', NULL, '106', 'अंडा भुर्जी', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('92a5ccf2-3f7b-4e0b-a8fe-4f773be90641', 'AISR/01', NULL, '173', 'पनीर लबाबदार', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 450.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('9771e72e-b50d-4282-89e3-3dd8979f1d39', 'AISR/01', NULL, '156', 'अंडा फ्राइड राइस शेजवान', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 180.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":180},{\"name\":\"Full\",\"price\":300}]'),
('9a7ec19b-c661-49a4-b791-25e6d96af97a', 'AISR/01', NULL, '32', 'पनीर चीज बर्गर', 'Burger', 'f37d73fb-e337-4d42-be1a-470eb2de622d', 150.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('a1e58fd4-6cfb-42ee-bf64-f5d7638a0d5d', 'AISR/01', NULL, '164', 'दाल फ्राई', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 150.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('a2283f77-d8a9-4e3a-9303-dd54fb745c25', 'AISR/01', NULL, '112', 'चिकन पुलाव', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 250.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":250},{\"name\":\"Full\",\"price\":370}]'),
('a7141fe3-2758-46ca-8552-7108f8ad4839', 'AISR/01', NULL, '185', 'मशरूम दो प्याजा', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 400.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('a81a41f7-fedd-4f90-bc57-942f99711edb', 'AISR/01', NULL, '191', 'ड्रैगन पनीर', 'Special Dish', 'edcf8c0f-ddaf-43e8-9779-6e6c87b99d6b', 190.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Price\",\"price\":190}]'),
('a841436a-a7de-412b-be9b-6db4df7259e4', 'AISR/01', NULL, '28', 'चिकन', 'Pizza Toppings', '44459c69-92e9-46d3-97f9-8f5396e75d51', 60.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Price\",\"price\":60}]'),
('a8e42b6e-bf9c-44bb-a889-4b2ffa9aeca3', 'AISR/01', NULL, '158', 'वेज मंचो सूप', 'Soup', '65708565-70a7-4f17-9597-0adb9b3b4bb7', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('a9f15ebd-c569-41d9-8f08-06d53c0e9c8e', 'AISR/01', NULL, '07', 'पिज्जा इटालियन', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 250.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":250},{\"name\":\"Medium\",\"price\":399},{\"name\":\"Large\",\"price\":499}]'),
('ab0c118f-092f-450c-bb43-bfed4c6b77f0', 'AISR/01', NULL, '117', 'जीरा राइस', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":140},{\"name\":\"Full\",\"price\":240}]'),
('ac26688f-f52a-47b2-8fcb-13a82d6746cd', 'AISR/01', NULL, '93', 'चिकन रारा', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 530.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":530},{\"name\":\"Full (8 पीस)\",\"price\":1000}]'),
('adb1a204-3443-40b1-8af8-4729b66fb5a3', 'AISR/01', NULL, '135', 'चिकन मलाई टिक्का', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 360.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (5 पीस)\",\"price\":360},{\"name\":\"Full (10 पीस)\",\"price\":620}]'),
('aeb19502-1975-4534-9ec9-44b9907c20a4', 'AISR/01', NULL, '171', 'पनीर बटर मसाला', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 400.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('af10739c-56f5-405f-97a2-7f289f76c401', 'AISR/01', NULL, '161', 'टोमेटो सूप', 'Soup', '65708565-70a7-4f17-9597-0adb9b3b4bb7', 130.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('b0061b32-3d83-4078-be83-ec096571fe4c', 'AISR/01', NULL, '167', 'मटर पनीर', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 380.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('b0ee2778-4a72-488b-9e7f-04cee06b62bd', 'AISR/01', NULL, '110', 'चिकन दम बिरयानी', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 240.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":240},{\"name\":\"Full\",\"price\":370}]'),
('b2173b7f-ffd4-46ac-9344-60b44ff24f6a', 'AISR/01', NULL, '107', 'अंडा मसाला', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 250.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('b2588003-2091-427e-a676-eec1593460c6', 'AISR/01', NULL, '55', 'चिकन चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":90},{\"name\":\"Full\",\"price\":170}]'),
('b433b62f-9ac3-46a4-9ee9-41abec9e859a', 'AISR/01', NULL, '102', 'मटन दो प्याजा', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 570.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":570},{\"name\":\"Full (8 पीस)\",\"price\":1020}]'),
('b6a8d554-5218-49e3-b5a7-9e2bc6bbf227', 'AISR/01', NULL, '133', 'टंगड़ी कबाब', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 360.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (2 पीस)\",\"price\":360},{\"name\":\"Full (4 पीस)\",\"price\":620}]'),
('b6c9cc05-a31c-432f-b2d9-975b2d7d1aeb', 'AISR/01', NULL, '43', 'क्लासिक कोल्ड कॉफी', 'Coffee & Tea', 'd6b3e2f8-fbe6-4d95-91d3-a72d54d1c033', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('b75dd817-94d8-4c12-982d-518555fdfc82', 'AISR/01', NULL, '54', 'वेज हॉट गार्लिक चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":90},{\"name\":\"Full\",\"price\":170}]'),
('b80fac76-dbe8-4784-8ec3-42e7be7d9421', 'AISR/01', NULL, '86', 'कड़ाही चिकन बोनलेस', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 450.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":450},{\"name\":\"Full (8 पीस)\",\"price\":830}]'),
('b9bbe2db-6a92-41e2-b87c-b743ac5da6b3', 'AISR/01', NULL, '36', 'मैंगो शेक', 'Shakes & Lassi', '759db71b-e273-451e-9c0e-e79b1d4b9538', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('bb1a5588-b304-4c7f-b6ba-6b70fbddf4bc', 'AISR/01', NULL, '178', 'मिक्स वेज', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 270.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('bbf936b6-cf0d-4220-b936-779d0621fc7b', 'AISR/01', NULL, '21', 'सीख कबाब स्पेशल', 'Non-Veg Pizza', '40b2ed20-b917-464a-bdfd-6ad9162da253', 270.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":270},{\"name\":\"Medium\",\"price\":430},{\"name\":\"Large\",\"price\":550}]'),
('bd338845-259f-4d31-b9fb-b5cdb98b0a71', 'AISR/01', NULL, '115', 'मटन दम बिरयानी', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 370.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":370},{\"name\":\"Full\",\"price\":600}]'),
('bebd4e06-433b-4c3a-b883-023b5fea61eb', 'AISR/01', NULL, '26', 'वेजिटेबल', 'Pizza Toppings', '44459c69-92e9-46d3-97f9-8f5396e75d51', 30.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Price\",\"price\":30}]'),
('bf0a79e2-ec88-4e96-85b1-57d0bf73d8c8', 'AISR/01', NULL, '194', 'वनीला कोल्ड कॉफी', 'Special Dish', 'edcf8c0f-ddaf-43e8-9779-6e6c87b99d6b', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('bf5c1eae-8d1c-41a5-9fd2-9018d9dba2e6', 'AISR/01', NULL, '88', 'चिकन करी', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 410.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":410},{\"name\":\"Full (8 पीस)\",\"price\":780}]'),
('bf825d10-11c1-461a-a7c2-35884249f09b', 'AISR/01', NULL, '52', 'पनीर शेजवान चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":200}]'),
('bfdc42dd-ed82-4313-a513-846cf6a04063', 'AISR/01', NULL, '123', 'आलू नान', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('c0af4666-be50-4824-bf61-d91d3314f523', 'AISR/01', NULL, '96', 'चिकन लबाबदार', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 480.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":480},{\"name\":\"Full (8 पीस)\",\"price\":840}]'),
('c1f121ed-0a50-4d4b-92cf-9e7b148b7b99', 'AISR/01', NULL, '31', 'पनीर बर्गर', 'Burger', 'f37d73fb-e337-4d42-be1a-470eb2de622d', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('c39f450d-94eb-4265-91a6-820c0713c57b', 'AISR/01', NULL, '24', 'नॉन-वेज स्पेशल', 'Non-Veg Pizza', '40b2ed20-b917-464a-bdfd-6ad9162da253', 350.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":350},{\"name\":\"Medium\",\"price\":530},{\"name\":\"Large\",\"price\":670}]'),
('c412571e-356b-4124-8718-33a405aa17ca', 'AISR/01', NULL, '44', 'वनीला कोल्ड कॉफी', 'Coffee & Tea', 'd6b3e2f8-fbe6-4d95-91d3-a72d54d1c033', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('c416e803-5c38-4c51-a62e-03a9c937a962', 'AISR/01', NULL, '105', 'आमलेट', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('c5fe65a1-5373-48f5-a0a9-a7caac18c2e5', 'AISR/01', NULL, '179', 'आलू जीरा', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 210.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('c7debca5-bed0-4e31-bc51-402c86daefbf', 'AISR/01', NULL, '72', 'वेज तंदूरी मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":210}]'),
('c90244fe-4890-4023-933a-3f3f2a4d28a0', 'AISR/01', NULL, '151', 'पनीर फ्राइड राइस', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 190.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":190},{\"name\":\"Full\",\"price\":320}]'),
('cbfacdc2-8127-4000-b53d-157894bc313b', 'AISR/01', NULL, '143', 'चिल्ली पोटैटो', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":100},{\"name\":\"Full\",\"price\":180}]'),
('ce497097-263f-4042-ac55-768835f567c6', 'AISR/01', NULL, '58', 'चिकन शेजवान चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":200}]'),
('d14fb867-4aa2-49fd-9e2f-3a61c9334a54', 'AISR/01', NULL, '153', 'वेज फ्राइड राइस शेजवान', 'Fried Rice', '3809f02f-51dd-4b35-ac7c-b1f72c5499b4', 150.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":150},{\"name\":\"Full\",\"price\":280}]'),
('d2218c68-7997-46ab-9248-9a5dad18ce2a', 'AISR/01', NULL, '170', 'हांडी पनीर', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 380.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('d2686987-b4ce-4756-bf57-89845178fc81', 'AISR/01', NULL, '33', 'चिकन बर्गर', 'Burger', 'f37d73fb-e337-4d42-be1a-470eb2de622d', 80.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('d282b655-1511-4345-b9b6-f5f53e15413f', 'AISR/01', NULL, '169', 'कड़ाही पनीर', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 380.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('d3573c55-bd6a-4548-9fcf-fa054b35a855', 'AISR/01', NULL, '111', 'अंडा बिरयानी', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 230.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":230},{\"name\":\"Full\",\"price\":350}]'),
('d5480369-44d5-462b-898e-d5ede8471c33', 'AISR/01', NULL, '122', 'बटर नान', 'Indian Breads', '129ddc77-f010-44d3-8575-4d2e25f251f7', 60.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('d73feb19-3f0f-4439-a65a-f4d02a721783', 'AISR/01', NULL, '20', 'चिकन लवर', 'Non-Veg Pizza', '40b2ed20-b917-464a-bdfd-6ad9162da253', 270.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":270},{\"name\":\"Medium\",\"price\":450},{\"name\":\"Large\",\"price\":550}]'),
('d9f468c6-5070-4811-b589-cec17ae7452e', 'AISR/01', NULL, '74', 'पनीर तंदूरी मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":140},{\"name\":\"Full\",\"price\":260}]'),
('da26400c-624b-4bf8-afd3-329c10218565', 'AISR/01', NULL, '80', 'पापड़', 'Fried & Snacks', '8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 20.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('daf6e876-9e8f-4926-ad03-8dc4e40d5da6', 'AISR/01', NULL, '10', 'स्पाइसी अचारी पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 279.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":279},{\"name\":\"Medium\",\"price\":449},{\"name\":\"Large\",\"price\":529}]'),
('dfdd88b3-67a3-4773-9da6-f1a9297fa923', 'AISR/01', NULL, '137', 'अफगानी चिकन', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 370.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (5 पीस)\",\"price\":370},{\"name\":\"Full (10 पीस)\",\"price\":630}]'),
('e0483894-75ff-474b-a191-021a154c42d8', 'AISR/01', NULL, '113', 'वेज बिरयानी', 'Biryani & Rice', '72ebedcf-94fb-47d9-835e-0f0188ec3171', 210.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":210},{\"name\":\"Full\",\"price\":350}]'),
('e09d981e-b92e-4312-b420-8226b526ef89', 'AISR/01', NULL, '51', 'वेज शेजवान चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 70.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":70},{\"name\":\"Full\",\"price\":130}]'),
('e163fdcd-3129-467d-bd67-af6ade011aba', 'AISR/01', NULL, '01', 'स्वीट कॉर्न पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 190.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":190},{\"name\":\"Medium\",\"price\":290},{\"name\":\"Large\",\"price\":390}]'),
('e168056d-bb07-47e6-a696-87527274cb20', 'AISR/01', NULL, '69', 'वेज अफगानी मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 110.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":110},{\"name\":\"Full\",\"price\":220}]'),
('e649400d-5a3d-4755-8b93-23a116439918', 'AISR/01', NULL, '56', 'चिकन हक्का चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":100},{\"name\":\"Full\",\"price\":190}]'),
('e6fde553-b0e3-4d31-a434-9ed0160ea185', 'AISR/01', NULL, '34', 'स्वीट लस्सी', 'Shakes & Lassi', '759db71b-e273-451e-9c0e-e79b1d4b9538', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL);
INSERT INTO `menu_items` (`id`, `restaurant_id`, `branch_id`, `code`, `name`, `category`, `category_id`, `price`, `tax_mode`, `tax_percentage`, `current_stock`, `is_default_tax`, `is_favourite`, `image_url`, `is_available`, `is_veg`, `sync_seq`, `created_at`, `updated_at`, `variants_json`) VALUES
('e92827ca-d16c-47c3-bf0c-0d4436f368df', 'AISR/01', NULL, '38', 'वनीला शेक', 'Shakes & Lassi', '759db71b-e273-451e-9c0e-e79b1d4b9538', 160.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('e94b7ac0-a220-4854-b768-1eb80a59c885', 'AISR/01', NULL, '27', 'पनीर', 'Pizza Toppings', '44459c69-92e9-46d3-97f9-8f5396e75d51', 50.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Price\",\"price\":50}]'),
('e9b2b411-f193-4a8f-87c3-1bc2cddd3d91', 'AISR/01', NULL, '175', 'पनीर भुर्जी', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 380.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('eb6152e1-415e-4d65-8c3d-458ea98199eb', 'AISR/01', NULL, '172', 'पनीर दो प्याजा', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 400.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('ec463720-1713-47cc-8330-29f8aaa93447', 'AISR/01', NULL, '100', 'मटन हांडी', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 570.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('edf562fd-f01d-4453-9a24-83ea323c73f0', 'AISR/01', NULL, '25', 'चीज ओवरलोड', 'Pizza Toppings', '44459c69-92e9-46d3-97f9-8f5396e75d51', 70.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Price\",\"price\":70}]'),
('eeae36c3-15b2-468a-b49a-0f51ee5c61e4', 'AISR/01', NULL, '84', 'चिकन रोल', 'Fried & Snacks', '8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 150.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":150},{\"name\":\"Full\",\"price\":280}]'),
('eefaccb5-9e14-4980-acbf-ec9f49c1afad', 'AISR/01', NULL, '04', 'डबल चीज पिज्जा', 'Pizza', '28622d79-318f-4aa5-ab0f-feeb4ae41714', 250.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Regular\",\"price\":250},{\"name\":\"Medium\",\"price\":399},{\"name\":\"Large\",\"price\":499}]'),
('eefba873-6d96-444c-be95-3acc002238a4', 'AISR/01', NULL, '165', 'दाल तड़का', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 180.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('f0450776-252c-4fa4-a203-18b801026065', 'AISR/01', NULL, '81', 'वेज स्प्रिंग रोल', 'Fried & Snacks', '8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 60.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":60},{\"name\":\"Full\",\"price\":110}]'),
('f09e487c-1193-4e23-827f-bab54c2f3c17', 'AISR/01', NULL, '73', 'चिकन तंदूरी मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":140},{\"name\":\"Full\",\"price\":260}]'),
('f15b850a-4d00-4af3-bc80-6c68dd9b1b80', 'AISR/01', NULL, '78', 'फ्रेंच फ्राइज', 'Fried & Snacks', '8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 60.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":60},{\"name\":\"Full\",\"price\":110}]'),
('f1957009-cfe1-4080-8b16-4818846e30e2', 'AISR/01', NULL, '40', 'चॉकलेट शेक', 'Shakes & Lassi', '759db71b-e273-451e-9c0e-e79b1d4b9538', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('f26fbfa3-6196-4dfa-8529-9c2149835f86', 'AISR/01', NULL, '92', 'चिकन मसाला बोनलेस', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 450.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":450},{\"name\":\"Full (8 पीस)\",\"price\":830}]'),
('f2d3ef4e-ed47-4516-a90d-dda42da338ff', 'AISR/01', NULL, '79', 'पनीर पकौड़ा', 'Fried & Snacks', '8ea4d21a-39a0-426a-a2d1-5837ddf3ddaa', 140.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":140},{\"name\":\"Full\",\"price\":270}]'),
('f399b8e1-2b0d-4b50-bc3d-bace1e639343', 'AISR/01', NULL, '66', 'वेज फ्राई मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 80.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":80},{\"name\":\"Full\",\"price\":160}]'),
('f5df3465-d514-4ed2-9dd1-0387a9bbd981', 'AISR/01', NULL, '109', 'अंडा करी', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 230.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('f7984e24-041b-46c4-9158-1831f3837279', 'AISR/01', NULL, '182', 'मशरूम मटर', 'Indian Main Course', '990f597e-b935-44bf-ad27-308b868f1a3c', 380.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('f7fd24b3-feea-40cd-b09e-d3b4ee508618', 'AISR/01', NULL, '65', 'पनीर मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 90.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":90},{\"name\":\"Full\",\"price\":170}]'),
('fa40ea64-556d-4f98-8e44-1e2ff01f5f41', 'AISR/01', NULL, '132', 'चिकन लेग फ्राई', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 120.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Quarter (per piece)\",\"price\":120},{\"name\":\"Half (4 पीस)\",\"price\":450},{\"name\":\"Full (8 पीस)\",\"price\":850}]'),
('fc004496-f876-4a4d-a10f-52bbf5be7bc4', 'AISR/01', NULL, '145', 'मशरूम चिल्ली', 'Manchurian & Chilli', '9c67384e-d5cc-46f6-99f4-394d97201c54', 180.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":180},{\"name\":\"Full\",\"price\":340}]'),
('fc549f56-173d-41ba-906f-e2d51ec67aeb', 'AISR/01', NULL, '108', 'अंडा कड़ाही', 'Mutton & Egg Gravy', '09ce172a-54e3-4bdb-8461-4450741ded69', 240.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', NULL),
('fec4f31d-5ad4-466e-a9b7-3109ac4b6e0f', 'AISR/01', NULL, '64', 'चिकन मोमो', 'Momos', 'a161bcf3-72a0-4984-b813-882728d1c898', 80.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":80},{\"name\":\"Full\",\"price\":160}]'),
('ff32c883-64c7-406f-b620-f21e2afb2d2c', 'AISR/01', NULL, '85', 'कड़ाही चिकन', 'Chicken Gravy', 'd8654fbb-5456-4e00-92bd-9b5c3666201e', 430.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half (4 पीस)\",\"price\":430},{\"name\":\"Full (8 पीस)\",\"price\":790}]'),
('ffd9bcc7-e129-4095-a378-34863c9b4ccb', 'AISR/01', NULL, '47', 'पनीर चाउमीन', 'Chowmein', '7b1d9ffe-949e-471f-aeca-7193d6f1461a', 100.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 1, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Half\",\"price\":100},{\"name\":\"Full\",\"price\":190}]'),
('fffda5ec-199a-4469-a1d4-1b3fa7a2b136', 'AISR/01', NULL, '130', 'तंदूरी चिकन', 'Chicken Snacks', '89d72b39-814e-4592-b22b-77915f420774', 270.00, 'WITHOUT_TAX', 'None', 100, 1, 0, NULL, 1, 0, 1, '2026-11-25 20:32:19', '2026-11-25 20:32:19', '[{\"name\":\"Quarter (4 पीस)\",\"price\":270},{\"name\":\"Half (8 पीस)\",\"price\":470},{\"name\":\"Full (16 पीस)\",\"price\":910}]');

-- --------------------------------------------------------

--
-- Table structure for table `migrations`
--

CREATE TABLE `migrations` (
  `id` int(10) UNSIGNED NOT NULL,
  `migration` varchar(255) NOT NULL,
  `batch` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `uuid` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `customer_id` char(36) DEFAULT NULL,
  `order_number` varchar(50) NOT NULL,
  `invoice_number` varchar(50) DEFAULT NULL,
  `order_type` varchar(50) NOT NULL DEFAULT 'DINE_IN',
  `status` varchar(50) NOT NULL DEFAULT 'ON_HOLD',
  `payment_status` varchar(50) NOT NULL DEFAULT 'PENDING',
  `payment_method` varchar(50) DEFAULT NULL,
  `table_number` varchar(50) DEFAULT NULL,
  `customer_name` varchar(255) DEFAULT NULL,
  `customer_phone` varchar(50) DEFAULT NULL,
  `subtotal` decimal(10,2) NOT NULL DEFAULT 0.00,
  `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `tax_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `service_charge` decimal(10,2) NOT NULL DEFAULT 0.00,
  `grand_total` decimal(10,2) NOT NULL DEFAULT 0.00,
  `sync_seq` bigint(20) UNSIGNED NOT NULL DEFAULT 1,
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `orders`
--

INSERT INTO `orders` (`id`, `uuid`, `restaurant_id`, `branch_id`, `customer_id`, `order_number`, `invoice_number`, `order_type`, `status`, `payment_status`, `payment_method`, `table_number`, `customer_name`, `customer_phone`, `subtotal`, `discount_amount`, `tax_amount`, `service_charge`, `grand_total`, `sync_seq`, `completed_at`, `created_at`, `updated_at`, `is_deleted`) VALUES
(1, 'KOT-01', 'AISR/01', 'BR-E8A5036E', NULL, 'KOT-01', 'INV-0014', 'DINE_IN', 'ON_HOLD', 'PENDING', 'CASH', NULL, NULL, NULL, 240.00, 0.00, 0.00, 0.00, 240.00, 1, '2026-11-26 02:05:07', '2026-11-26 02:05:07', '2026-11-27 07:05:06', 1),
(2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', 'AISR/01', 'BR-E8A5036E', NULL, 'INV-0015', 'INV-0015', 'DINE_IN', 'COMPLETED', 'PAID', 'CASH', 'G8', NULL, NULL, 2080.00, 80.00, 0.00, 0.00, 2000.00, 1, '2026-11-26 02:08:36', '2026-11-26 02:08:36', '2026-11-25 20:38:37', 0),
(3, '99999999-1111-4444-8888-111111111111', 'AISR/01', 'BR-E8A5036E', NULL, 'KOT-99', NULL, 'DINE_IN', 'ON_HOLD', 'PENDING', 'CASH', 'G2', NULL, NULL, 240.00, 0.00, 0.00, 0.00, 240.00, 1, NULL, '2026-11-27 05:43:04', '2026-11-27 11:06:58', 1),
(4, '7525fcda-8428-41f0-bafd-c9eee9e2dcc0', 'AISR/01', 'BR-E8A5036E', NULL, 'ORD-7525FC', NULL, 'DINE_IN', 'ON_HOLD', 'PENDING', 'CASH', NULL, NULL, NULL, 0.00, 0.00, 0.00, 0.00, 0.00, 1, NULL, '2026-11-27 11:02:55', '2026-11-27 11:06:58', 1),
(5, 'd0a95ec2-1317-4636-a4e0-d4e8e1ec8853', 'AISR/01', 'BR-E8A5036E', NULL, 'KOT-D0A95E', NULL, 'DINE_IN', 'ON_HOLD', 'PENDING', 'CASH', 'G1', NULL, NULL, 710.00, 0.00, 0.00, 0.00, 710.00, 1, NULL, '2026-11-27 16:43:49', '2026-11-27 11:13:51', 0);

-- --------------------------------------------------------

--
-- Table structure for table `order_items`
--

CREATE TABLE `order_items` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `order_id` bigint(20) UNSIGNED NOT NULL,
  `order_uuid` char(36) DEFAULT NULL,
  `menu_item_id` char(36) DEFAULT NULL,
  `item_name` varchar(255) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `total` decimal(10,2) DEFAULT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `order_items`
--

INSERT INTO `order_items` (`id`, `order_id`, `order_uuid`, `menu_item_id`, `item_name`, `unit_price`, `price`, `quantity`, `total`, `total_price`, `created_at`, `updated_at`) VALUES
(10, 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', NULL, 'अंडा कड़ाही', 240.00, 240.00, 1, 240.00, 240.00, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(11, 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', NULL, 'अंडा चाउमीन (Full)', 150.00, 150.00, 1, 150.00, 150.00, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(12, 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', NULL, 'अंडा फ्राइड राइस शेजवान (Half)', 180.00, 180.00, 1, 180.00, 180.00, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(13, 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', NULL, 'बूंदी रायता', 110.00, 110.00, 1, 110.00, 110.00, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(14, 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', NULL, 'बनाना शेक', 100.00, 100.00, 3, 300.00, 300.00, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(15, 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', NULL, 'मटन करी (Half (4 पीस))', 550.00, 550.00, 2, 1100.00, 1100.00, '2026-11-25 20:38:37', '2026-11-25 20:38:37'),
(39, 1, 'KOT-01', NULL, 'अंडा चाउमीन (Half)', 80.00, 80.00, 3, 240.00, 240.00, '2026-11-27 07:05:06', '2026-11-27 07:05:06'),
(40, 3, '99999999-1111-4444-8888-111111111111', NULL, 'Test Item', 240.00, 240.00, 1, 240.00, 240.00, '2026-11-27 07:06:22', '2026-11-27 07:06:22'),
(41, 5, 'd0a95ec2-1317-4636-a4e0-d4e8e1ec8853', NULL, 'अंडा कड़ाही', 240.00, 240.00, 2, 480.00, 480.00, '2026-11-27 11:13:51', '2026-11-27 11:13:51'),
(42, 5, 'd0a95ec2-1317-4636-a4e0-d4e8e1ec8853', NULL, 'अंडा करी', 230.00, 230.00, 1, 230.00, 230.00, '2026-11-27 11:13:51', '2026-11-27 11:13:51');

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `uuid` char(36) NOT NULL,
  `order_id` bigint(20) UNSIGNED NOT NULL,
  `order_uuid` char(36) DEFAULT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `payment_method` varchar(50) NOT NULL DEFAULT 'CASH',
  `status` varchar(50) NOT NULL DEFAULT 'COMPLETED',
  `amount` decimal(10,2) NOT NULL,
  `reference_number` varchar(255) DEFAULT NULL,
  `transaction_reference` varchar(255) DEFAULT NULL,
  `is_verified` tinyint(1) NOT NULL DEFAULT 1,
  `paid_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `payments`
--

INSERT INTO `payments` (`id`, `uuid`, `order_id`, `order_uuid`, `restaurant_id`, `branch_id`, `payment_method`, `status`, `amount`, `reference_number`, `transaction_reference`, `is_verified`, `paid_at`, `created_at`, `updated_at`) VALUES
(17, 'e95f1104-6215-4906-86af-c464616af1bb', 1, 'KOT-01', 'AISR/01', 'BR-E8A5036E', 'CASH', 'COMPLETED', 230.00, NULL, NULL, 1, '2026-11-25 20:35:32', '2026-11-25 20:35:32', '2026-11-26 05:51:48'),
(18, '44b00a41-092e-4f73-b8e6-c29c4fdf0102', 2, '599d7b4f-1425-4c52-b7ab-daf14f2524f6', 'AISR/01', 'BR-E8A5036E', 'CASH', 'COMPLETED', 2000.00, NULL, NULL, 1, '2026-11-25 20:38:37', '2026-11-25 20:38:37', '2026-11-25 20:38:37');

-- --------------------------------------------------------

--
-- Table structure for table `personal_access_tokens`
--

CREATE TABLE `personal_access_tokens` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `tokenable_type` varchar(255) NOT NULL,
  `tokenable_id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `token` varchar(64) NOT NULL,
  `abilities` text DEFAULT NULL,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `personal_access_tokens`
--

INSERT INTO `personal_access_tokens` (`id`, `tokenable_type`, `tokenable_id`, `name`, `token`, `abilities`, `last_used_at`, `expires_at`, `created_at`, `updated_at`) VALUES
(1, 'App\\Models\\User', 1, 'ais_restro_pos', '11ede16627f9b6728788abda1f1e9a400fb25c357a65c345901636e0283c9536', '[\"*\"]', '2026-11-24 07:41:14', NULL, '2026-11-24 07:41:13', '2026-11-24 07:41:13'),
(2, 'App\\Models\\User', 1, 'ais_restro_pos', 'fd2f9491734ccc568be87103773f40cad2fdb7b408619e9cbd315a39cbcfacac', '[\"*\"]', '2026-11-24 08:10:56', NULL, '2026-11-24 07:57:35', '2026-11-24 07:57:35'),
(3, 'App\\Models\\User', 1, 'ais_restro_pos', '62110e66cbb8f228f345e883c1796aef56ba33800010a7cff71ede0fca3cb510', '[\"*\"]', '2026-11-24 15:36:25', NULL, '2026-11-24 08:15:47', '2026-11-24 08:15:47'),
(4, 'App\\Models\\User', 1, 'ais_restro_pos', 'd60d4869377b91bc2a00513cc11cfa1b3a78b2ca2feed942ffc6e0833f4030a4', '[\"*\"]', '2026-11-24 19:10:03', NULL, '2026-11-24 17:28:42', '2026-11-24 17:28:42'),
(6, 'App\\Models\\User', 1, 'ais_restro_pos', '76828d324ca6156b58a991e05c2c44d3d78186a0c0c856d4df72dedf4d3a0aa2', '[\"*\"]', '2026-11-24 20:17:54', NULL, '2026-11-24 19:51:39', '2026-11-24 19:51:39'),
(7, 'App\\Models\\User', 1, 'ais_restro_pos', '533a8c943e372548bdc50bcac4b08662cadf2e5abeff5664a534598c2149d818', '[\"*\"]', '2026-11-25 04:57:05', NULL, '2026-11-24 20:20:34', '2026-11-24 20:20:34'),
(11, 'App\\Models\\User', 2, 'ais_restro_pos', '36ce81b5b89ab40d90b43d524a292c5b827b83a46e5018f0dad078103909595c', '[\"*\"]', '2026-11-25 11:29:15', NULL, '2026-11-25 10:30:09', '2026-11-25 10:30:09'),
(12, 'App\\Models\\User', 2, 'ais_restro_pos', 'da68b6a8c23b4756ee8343f27403955454da3c1b7cf4996dcae05ba92e006404', '[\"*\"]', '2026-11-25 19:53:58', NULL, '2026-11-25 11:29:44', '2026-11-25 11:29:44'),
(13, 'App\\Models\\User', 2, 'ais_restro_pos', '499fe2c91d14986c0acb8ae94af564fd41799fa2989bcbd225468d5afb6ed74f', '[\"*\"]', '2026-11-27 05:17:48', NULL, '2026-11-25 12:13:06', '2026-11-25 12:13:06'),
(14, 'App\\Models\\User', 2, 'ais_restro_pos', '8fbf195a2ba019489b9d6d21475196ba7839851b86625a8aec6ddc976a06be55', '[\"*\"]', '2026-11-25 20:34:05', NULL, '2026-11-25 19:57:03', '2026-11-25 19:57:03'),
(15, 'App\\Models\\User', 1, 'ais_restro_pos', '703133f76b170501ab765323c603cf382f118ef6d403f4dbca2166337987f29e', '[\"*\"]', '2026-11-27 04:37:46', NULL, '2026-11-25 20:34:40', '2026-11-25 20:34:40'),
(16, 'App\\Models\\User', 1, 'ais_restro_pos', 'cacc5c2fccb4c6d60bab1586382d11b903e8320e75203d3ce9d6bdc6eb6eb9e0', '[\"*\"]', '2026-11-26 16:29:36', NULL, '2026-11-26 05:50:26', '2026-11-26 05:50:26'),
(17, 'App\\Models\\User', 1, 'ais_restro_pos', 'bae9609df188818b9c4d27e8b98b23859f2b7271d62be863ea8262496270d1f1', '[\"*\"]', '2026-11-27 05:19:34', NULL, '2026-11-27 04:38:01', '2026-11-27 04:38:01'),
(19, 'App\\Models\\User', 1, 'ais_restro_pos', 'de3d0d789a2796da38bd7d07e793b985bcd07294af2f93d627520e7cdf76c374', '[\"*\"]', '2026-11-27 11:06:58', NULL, '2026-11-27 05:23:26', '2026-11-27 05:23:26'),
(20, 'App\\Models\\User', 1, 'ais_restro_pos', '52e4bbfaea7d0b588151928fa72d47371ddd8b1d87d0ceb735e039f691d2d9c9', '[\"*\"]', '2026-11-27 10:59:41', NULL, '2026-11-27 07:02:13', '2026-11-27 07:02:13'),
(21, 'App\\Models\\User', 1, 'ais_restro_pos', '89217c8c4665636e81a5a8be47b3c7de3cc6101f931caf058158d398189b04c5', '[\"*\"]', '2026-11-27 11:19:44', NULL, '2026-11-27 11:01:20', '2026-11-27 11:01:20');

-- --------------------------------------------------------

--
-- Table structure for table `receipt_settings`
--

CREATE TABLE `receipt_settings` (
  `restaurant_id` varchar(64) NOT NULL,
  `header_text` text DEFAULT NULL,
  `footer_text` text DEFAULT NULL,
  `qr_image_url` text DEFAULT NULL,
  `show_qr` tinyint(1) DEFAULT 1,
  `paper_width` varchar(10) DEFAULT '58mm',
  `font_size` int(11) DEFAULT 18,
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `receipt_settings`
--

INSERT INTO `receipt_settings` (`restaurant_id`, `header_text`, `footer_text`, `qr_image_url`, `show_qr`, `paper_width`, `font_size`, `updated_at`) VALUES
('AISR/01', 'Chaudhary Cafe and Family Resturent', 'Thank you! Visit Again', NULL, 1, '58mm', 22, '2026-11-26 05:51:24'),
('RESTRO-42111585', 'Abhishek Chaudhary\'s Restaurant', 'Thank you! Visit Again', 'https://restro.trakiyo.in/api/uploads/qr/qr_RESTRO-42111585_1795617160.jpg', 0, '58mm', 21, '2026-11-25 19:58:30');

-- --------------------------------------------------------

--
-- Table structure for table `refunds`
--

CREATE TABLE `refunds` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `payment_id` bigint(20) UNSIGNED NOT NULL,
  `order_id` bigint(20) UNSIGNED NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `processed_by_user_id` bigint(20) UNSIGNED DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `restaurants`
--

CREATE TABLE `restaurants` (
  `id` char(36) NOT NULL,
  `owner_id` bigint(20) UNSIGNED DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `slug` varchar(255) NOT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `logo_url` varchar(255) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `currency` varchar(10) NOT NULL DEFAULT 'INR',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `trial_ends_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `restaurants`
--

INSERT INTO `restaurants` (`id`, `owner_id`, `name`, `slug`, `phone`, `email`, `logo_url`, `address`, `currency`, `is_active`, `trial_ends_at`, `created_at`, `updated_at`) VALUES
('AISR/01', 1, 'Chaudhary Cafe and Family Resturent', 'chaudhary-cafe-and-family-resturent-d0ccc', '7379839937', 'luckysingh955416@gmail.com', NULL, 'Ganakul, Shohratgarh', 'INR', 1, '2026-11-29 15:57:41', '2026-11-24 07:40:52', '2026-11-24 07:40:52');

-- --------------------------------------------------------

--
-- Table structure for table `restaurant_memberships`
--

CREATE TABLE `restaurant_memberships` (
  `id` char(36) NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `role` varchar(50) NOT NULL DEFAULT 'CASHIER',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `status` varchar(50) NOT NULL DEFAULT 'active',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `restaurant_memberships`
--

INSERT INTO `restaurant_memberships` (`id`, `user_id`, `restaurant_id`, `branch_id`, `role`, `is_active`, `status`, `created_at`, `updated_at`) VALUES
('17e1707c-22e9-4500-8ec0-819f904b9d95', 1, 'AISR/01', 'BR-E8A5036E', 'RESTAURANT_OWNER', 1, 'active', '2026-11-24 07:40:52', '2026-11-24 07:40:52');

-- --------------------------------------------------------

--
-- Table structure for table `restaurant_settings`
--

CREATE TABLE `restaurant_settings` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `key_name` varchar(64) DEFAULT 'general',
  `key_value` text DEFAULT NULL,
  `bill_prefix` varchar(20) NOT NULL DEFAULT 'INV-',
  `last_bill_number` int(11) NOT NULL DEFAULT 0,
  `kot_prefix` varchar(20) NOT NULL DEFAULT 'KOT-',
  `last_kot_number` int(11) NOT NULL DEFAULT 0,
  `tax_percentage` decimal(5,2) NOT NULL DEFAULT 0.00,
  `service_charge_percentage` decimal(5,2) NOT NULL DEFAULT 0.00,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `restaurant_settings`
--

INSERT INTO `restaurant_settings` (`id`, `restaurant_id`, `branch_id`, `key_name`, `key_value`, `bill_prefix`, `last_bill_number`, `kot_prefix`, `last_kot_number`, `tax_percentage`, `service_charge_percentage`, `created_at`, `updated_at`) VALUES
(1, 'AISR/01', NULL, 'sequence', 'Sequence counters initialized', 'INV-', 15, 'KOT-', 0, 0.00, 0.00, '2026-11-24 07:40:52', '2026-11-25 20:38:37');

-- --------------------------------------------------------

--
-- Table structure for table `stock_movements`
--

CREATE TABLE `stock_movements` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `inventory_item_id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `movement_type` varchar(50) NOT NULL DEFAULT 'IN',
  `quantity_change` decimal(10,2) NOT NULL,
  `quantity` decimal(10,2) NOT NULL DEFAULT 0.00,
  `notes` varchar(255) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `reference_id` varchar(64) DEFAULT NULL,
  `created_by_user_id` bigint(20) UNSIGNED DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `subscriptions`
--

CREATE TABLE `subscriptions` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `subscription_plan_id` char(36) NOT NULL,
  `plan_id` varchar(64) DEFAULT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'ACTIVE',
  `starts_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `ends_at` timestamp NULL DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `subscriptions`
--

INSERT INTO `subscriptions` (`id`, `restaurant_id`, `subscription_plan_id`, `plan_id`, `status`, `starts_at`, `ends_at`, `expires_at`, `created_at`, `updated_at`) VALUES
(2, 'AISR/01', 'PLAN-072FC2E8', 'PLAN-072FC2E8', 'ACTIVE', '2026-11-25 00:00:00', '2026-12-25 23:59:59', '2026-12-25 23:59:59', '2026-11-25 20:29:25', '2026-11-25 20:29:25');

-- --------------------------------------------------------

--
-- Table structure for table `subscription_plans`
--

CREATE TABLE `subscription_plans` (
  `id` char(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `features` text DEFAULT NULL,
  `duration_days` int(11) NOT NULL DEFAULT 30,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `max_branches` int(11) NOT NULL DEFAULT 1,
  `max_staff` int(11) NOT NULL DEFAULT 10,
  `max_devices` int(11) NOT NULL DEFAULT 5,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `subscription_plans`
--

INSERT INTO `subscription_plans` (`id`, `name`, `price`, `features`, `duration_days`, `is_active`, `max_branches`, `max_staff`, `max_devices`, `created_at`, `updated_at`) VALUES
('PLAN-072FC2E8', 'Starter', 299.00, 'Basic POS और billing की जरूरत वाले छोटे restaurants.\r\n- Billing & POS\r\n- Menu management\r\n- Order management\r\n- Basic sales reports\r\n- No billing device\r\n- 1 outlet\r\n- Basic support', 28, 1, 1, 1, 2, '2026-11-25 10:44:36', '2026-11-25 10:48:56'),
('PLAN-3059E533', 'GROWTH', 4999.00, 'Growing restaurants और cafes के लिए.\r\n- Starter के सभी features\r\n- Inventory management\r\n- KOT / Kitchen management\r\n- Table management\r\n- Expense management\r\n- Advanced sales reports\r\n- 1 billing devices\r\n- 1 outlet\r\n- Priority support', 365, 1, 1, 2, 1, '2026-11-25 10:51:54', '2026-11-25 10:54:06'),
('PLAN-87021994', 'BUSINESS PRO', 8999.00, 'High-volume restaurants और established businesses के लिए.\r\n- Growth के सभी features\r\n- Advanced analytics\r\n- Staff & role management\r\n- Profit and performance reports\r\n- 2 billing devices\r\n- Up to 3 outlets\r\n- Centralized reporting\r\n- Premium support', 365, 1, 2, 5, 3, '2026-11-25 10:53:57', '2026-11-25 10:53:57');

-- --------------------------------------------------------

--
-- Table structure for table `sync_tombstones`
--

CREATE TABLE `sync_tombstones` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `entity_type` varchar(32) NOT NULL,
  `entity_uuid` char(36) NOT NULL,
  `sync_seq` bigint(20) UNSIGNED NOT NULL,
  `deleted_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tables`
--

CREATE TABLE `tables` (
  `id` char(36) NOT NULL,
  `restaurant_id` char(36) NOT NULL,
  `branch_id` char(36) DEFAULT NULL,
  `table_number` varchar(50) NOT NULL,
  `capacity` int(11) NOT NULL DEFAULT 4,
  `status` varchar(50) NOT NULL DEFAULT 'AVAILABLE',
  `is_occupied` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tables`
--

INSERT INTO `tables` (`id`, `restaurant_id`, `branch_id`, `table_number`, `capacity`, `status`, `is_occupied`, `created_at`, `updated_at`) VALUES
('TBL-2848337A', 'AISR/01', NULL, 'G1', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-48CF3C94', 'AISR/01', NULL, 'G4', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-5C9CE126', 'AISR/01', NULL, 'G8', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-6D179B07', 'AISR/01', NULL, 'G9', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-73E9A6A5', 'AISR/01', NULL, 'G10', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-8B2C288D', 'AISR/01', NULL, 'G5', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-8DD20E35', 'AISR/01', NULL, 'G2', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-8EEDCACA', 'AISR/01', NULL, 'G3', 4, 'OCCUPIED', 1, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-94314CBA', 'AISR/01', NULL, 'G6', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-A1065481', 'AISR/01', NULL, 'G7', 4, 'AVAILABLE', 0, '2026-11-25 02:59:52', '2026-11-25 02:59:52'),
('TBL-E869046F', 'AISR/01', NULL, '11', 4, 'OCCUPIED', 1, '2026-11-25 02:59:52', '2026-11-25 02:59:52');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL DEFAULT 'owner',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `remember_token` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `name`, `email`, `phone`, `password`, `role`, `is_active`, `remember_token`, `created_at`, `updated_at`) VALUES
(1, 'Abhishek Chaudhary', 'luckysingh955416@gmail.com', '7379839937', '$2y$12$Belit2On3ECjBnZJybn4Y./DWGj0vtqEplToBfTZ/zjed.GQ.HBuS', 'owner', 1, NULL, '2026-11-24 07:40:52', '2026-11-24 07:40:52');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `audit_logs_restaurant_id_foreign` (`restaurant_id`),
  ADD KEY `idx_audit_restro` (`restaurant_id`);

--
-- Indexes for table `branches`
--
ALTER TABLE `branches`
  ADD PRIMARY KEY (`id`),
  ADD KEY `branches_restaurant_id_foreign` (`restaurant_id`),
  ADD KEY `idx_branches_restro` (`restaurant_id`);

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`id`),
  ADD KEY `categories_restaurant_id_sync_seq_index` (`restaurant_id`,`sync_seq`),
  ADD KEY `idx_cat_sync` (`restaurant_id`,`sync_seq`);

--
-- Indexes for table `customers`
--
ALTER TABLE `customers`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `customers_restaurant_id_mobile_number_unique` (`restaurant_id`,`mobile_number`),
  ADD KEY `customers_restaurant_id_sync_seq_index` (`restaurant_id`,`sync_seq`),
  ADD KEY `idx_cust_sync` (`restaurant_id`,`sync_seq`);

--
-- Indexes for table `daily_purchases`
--
ALTER TABLE `daily_purchases`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_purchases_restro_date` (`restaurant_id`,`purchase_date`),
  ADD KEY `idx_purchases_vendor` (`restaurant_id`,`vendor_name`);

--
-- Indexes for table `devices`
--
ALTER TABLE `devices`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `devices_device_code_unique` (`device_code`),
  ADD KEY `devices_restaurant_id_foreign` (`restaurant_id`),
  ADD KEY `idx_devices_restro` (`restaurant_id`);

--
-- Indexes for table `failed_jobs`
--
ALTER TABLE `failed_jobs`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `failed_jobs_uuid_unique` (`uuid`);

--
-- Indexes for table `idempotency_keys`
--
ALTER TABLE `idempotency_keys`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idempotency_keys_idempotency_key_unique` (`idempotency_key`),
  ADD KEY `idempotency_keys_idempotency_key_index` (`idempotency_key`),
  ADD KEY `idx_idemp_key` (`key_value`);

--
-- Indexes for table `inventory_items`
--
ALTER TABLE `inventory_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `inventory_items_restaurant_id_foreign` (`restaurant_id`),
  ADD KEY `idx_inventory_restro` (`restaurant_id`);

--
-- Indexes for table `jobs`
--
ALTER TABLE `jobs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `jobs_queue_index` (`queue`);

--
-- Indexes for table `job_batches`
--
ALTER TABLE `job_batches`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `kot_items`
--
ALTER TABLE `kot_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `kot_items_kot_ticket_id_foreign` (`kot_ticket_id`),
  ADD KEY `idx_kot_items_parent` (`kot_id`);

--
-- Indexes for table `kot_tickets`
--
ALTER TABLE `kot_tickets`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `kot_tickets_uuid_unique` (`uuid`),
  ADD KEY `kot_tickets_order_id_foreign` (`order_id`),
  ADD KEY `kot_tickets_restaurant_id_foreign` (`restaurant_id`),
  ADD KEY `idx_kot_order_uuid` (`order_uuid`);

--
-- Indexes for table `menu_items`
--
ALTER TABLE `menu_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `menu_items_restaurant_id_sync_seq_index` (`restaurant_id`,`sync_seq`),
  ADD KEY `menu_items_restaurant_id_category_index` (`restaurant_id`,`category`),
  ADD KEY `idx_menu_sync` (`restaurant_id`,`sync_seq`),
  ADD KEY `idx_menu_cat_id` (`category_id`);

--
-- Indexes for table `migrations`
--
ALTER TABLE `migrations`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `orders_uuid_unique` (`uuid`),
  ADD KEY `orders_restaurant_id_sync_seq_index` (`restaurant_id`,`sync_seq`),
  ADD KEY `orders_restaurant_id_order_number_index` (`restaurant_id`,`order_number`),
  ADD KEY `idx_restaurant_order_status` (`restaurant_id`,`status`),
  ADD KEY `idx_restaurant_order_date` (`restaurant_id`,`created_at`),
  ADD KEY `idx_orders_sync` (`restaurant_id`,`sync_seq`),
  ADD KEY `idx_orders_inv` (`restaurant_id`,`invoice_number`);

--
-- Indexes for table `order_items`
--
ALTER TABLE `order_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `order_items_order_id_foreign` (`order_id`),
  ADD KEY `idx_order_items_uuid` (`order_uuid`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `payments_uuid_unique` (`uuid`),
  ADD KEY `payments_order_id_foreign` (`order_id`),
  ADD KEY `idx_restaurant_payment_method` (`restaurant_id`,`payment_method`),
  ADD KEY `idx_restaurant_payment_date` (`restaurant_id`,`paid_at`),
  ADD KEY `idx_payments_order_uuid` (`order_uuid`);

--
-- Indexes for table `personal_access_tokens`
--
ALTER TABLE `personal_access_tokens`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `personal_access_tokens_token_unique` (`token`),
  ADD KEY `personal_access_tokens_tokenable_type_tokenable_id_index` (`tokenable_type`,`tokenable_id`);

--
-- Indexes for table `receipt_settings`
--
ALTER TABLE `receipt_settings`
  ADD PRIMARY KEY (`restaurant_id`);

--
-- Indexes for table `refunds`
--
ALTER TABLE `refunds`
  ADD PRIMARY KEY (`id`),
  ADD KEY `refunds_payment_id_foreign` (`payment_id`),
  ADD KEY `refunds_order_id_foreign` (`order_id`);

--
-- Indexes for table `restaurants`
--
ALTER TABLE `restaurants`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `restaurants_slug_unique` (`slug`),
  ADD KEY `idx_restro_owner` (`owner_id`);

--
-- Indexes for table `restaurant_memberships`
--
ALTER TABLE `restaurant_memberships`
  ADD PRIMARY KEY (`id`),
  ADD KEY `restaurant_memberships_user_id_foreign` (`user_id`),
  ADD KEY `restaurant_memberships_restaurant_id_foreign` (`restaurant_id`),
  ADD KEY `restaurant_memberships_branch_id_foreign` (`branch_id`),
  ADD KEY `idx_memberships_restro` (`restaurant_id`);

--
-- Indexes for table `restaurant_settings`
--
ALTER TABLE `restaurant_settings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `restaurant_settings_restaurant_id_branch_id_key_name_unique` (`restaurant_id`,`branch_id`,`key_name`),
  ADD KEY `idx_settings_restro` (`restaurant_id`);

--
-- Indexes for table `stock_movements`
--
ALTER TABLE `stock_movements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `stock_movements_inventory_item_id_foreign` (`inventory_item_id`),
  ADD KEY `idx_stock_item` (`inventory_item_id`);

--
-- Indexes for table `subscriptions`
--
ALTER TABLE `subscriptions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `subscriptions_restaurant_id_foreign` (`restaurant_id`),
  ADD KEY `subscriptions_subscription_plan_id_foreign` (`subscription_plan_id`),
  ADD KEY `idx_subs_restro` (`restaurant_id`);

--
-- Indexes for table `subscription_plans`
--
ALTER TABLE `subscription_plans`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `sync_tombstones`
--
ALTER TABLE `sync_tombstones`
  ADD PRIMARY KEY (`id`),
  ADD KEY `sync_tombstones_restaurant_id_sync_seq_index` (`restaurant_id`,`sync_seq`),
  ADD KEY `idx_tomb_sync` (`restaurant_id`,`sync_seq`);

--
-- Indexes for table `tables`
--
ALTER TABLE `tables`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `tables_restaurant_id_table_number_unique` (`restaurant_id`,`table_number`),
  ADD KEY `idx_tables_restro` (`restaurant_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `users_email_unique` (`email`),
  ADD KEY `idx_users_email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `audit_logs`
--
ALTER TABLE `audit_logs`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- AUTO_INCREMENT for table `failed_jobs`
--
ALTER TABLE `failed_jobs`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `idempotency_keys`
--
ALTER TABLE `idempotency_keys`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `jobs`
--
ALTER TABLE `jobs`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `kot_items`
--
ALTER TABLE `kot_items`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=43;

--
-- AUTO_INCREMENT for table `kot_tickets`
--
ALTER TABLE `kot_tickets`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `migrations`
--
ALTER TABLE `migrations`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `order_items`
--
ALTER TABLE `order_items`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=43;

--
-- AUTO_INCREMENT for table `payments`
--
ALTER TABLE `payments`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT for table `personal_access_tokens`
--
ALTER TABLE `personal_access_tokens`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=22;

--
-- AUTO_INCREMENT for table `refunds`
--
ALTER TABLE `refunds`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `restaurant_settings`
--
ALTER TABLE `restaurant_settings`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `stock_movements`
--
ALTER TABLE `stock_movements`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `subscriptions`
--
ALTER TABLE `subscriptions`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `sync_tombstones`
--
ALTER TABLE `sync_tombstones`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD CONSTRAINT `audit_logs_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `branches`
--
ALTER TABLE `branches`
  ADD CONSTRAINT `branches_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `categories`
--
ALTER TABLE `categories`
  ADD CONSTRAINT `categories_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `customers`
--
ALTER TABLE `customers`
  ADD CONSTRAINT `customers_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `devices`
--
ALTER TABLE `devices`
  ADD CONSTRAINT `devices_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `inventory_items`
--
ALTER TABLE `inventory_items`
  ADD CONSTRAINT `inventory_items_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `kot_items`
--
ALTER TABLE `kot_items`
  ADD CONSTRAINT `kot_items_kot_ticket_id_foreign` FOREIGN KEY (`kot_ticket_id`) REFERENCES `kot_tickets` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `kot_tickets`
--
ALTER TABLE `kot_tickets`
  ADD CONSTRAINT `kot_tickets_order_id_foreign` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `kot_tickets_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `menu_items`
--
ALTER TABLE `menu_items`
  ADD CONSTRAINT `menu_items_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `orders_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `order_items`
--
ALTER TABLE `order_items`
  ADD CONSTRAINT `order_items_order_id_foreign` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `payments`
--
ALTER TABLE `payments`
  ADD CONSTRAINT `payments_order_id_foreign` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `payments_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `refunds`
--
ALTER TABLE `refunds`
  ADD CONSTRAINT `refunds_order_id_foreign` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `refunds_payment_id_foreign` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `restaurant_memberships`
--
ALTER TABLE `restaurant_memberships`
  ADD CONSTRAINT `restaurant_memberships_branch_id_foreign` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `restaurant_memberships_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `restaurant_memberships_user_id_foreign` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `restaurant_settings`
--
ALTER TABLE `restaurant_settings`
  ADD CONSTRAINT `restaurant_settings_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `stock_movements`
--
ALTER TABLE `stock_movements`
  ADD CONSTRAINT `stock_movements_inventory_item_id_foreign` FOREIGN KEY (`inventory_item_id`) REFERENCES `inventory_items` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `subscriptions`
--
ALTER TABLE `subscriptions`
  ADD CONSTRAINT `subscriptions_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `subscriptions_subscription_plan_id_foreign` FOREIGN KEY (`subscription_plan_id`) REFERENCES `subscription_plans` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `tables`
--
ALTER TABLE `tables`
  ADD CONSTRAINT `tables_restaurant_id_foreign` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurants` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
