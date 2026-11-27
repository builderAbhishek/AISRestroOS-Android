<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;

class DeviceController
{
    /**
     * GET /devices
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, device_name, mac_address, device_type, paper_width, COALESCE(is_active, is_authorized, 1) AS is_active 
             FROM devices 
             WHERE restaurant_id = :restro 
             ORDER BY device_name ASC",
            ['restro' => $restaurantId]
        );

        Response::success($rows, 'Devices retrieved successfully');
    }

    /**
     * POST /devices
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'device_name' => 'required',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $id = !empty($input['id']) ? trim($input['id']) : 'DEV-' . strtoupper(substr(UUID::v4(), 0, 8));
        $name = trim($input['device_name']);
        $mac = !empty($input['mac_address']) ? trim($input['mac_address']) : null;
        $type = !empty($input['device_type']) ? trim($input['device_type']) : 'PRINTER';
        $width = !empty($input['paper_width']) ? trim($input['paper_width']) : '58mm';

        $existing = Database::fetchOne(
            "SELECT id FROM devices WHERE (id = :id OR mac_address = :mac) AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'mac' => $mac, 'restro' => $restaurantId]
        );

        $branchId = $request->getAttribute('branch_id');

        if ($existing) {
            Database::execute(
                "UPDATE devices SET device_name = :name, mac_address = :mac, paper_width = :w, is_active = 1, is_authorized = 1, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = :id AND restaurant_id = :restro",
                [
                    'name'   => $name,
                    'mac'    => $mac,
                    'w'      => $width,
                    'id'     => $existing['id'],
                    'restro' => $restaurantId,
                ]
            );
            $id = $existing['id'];
        } else {
            $deviceCode = substr($id, 0, 32);
            Database::execute(
                "INSERT INTO devices (id, restaurant_id, branch_id, device_code, device_name, mac_address, device_type, paper_width, is_authorized, is_active, created_at, updated_at) 
                 VALUES (:id, :restro, :branch, :code, :name, :mac, :type, :w, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                [
                    'id'     => $id,
                    'restro' => $restaurantId,
                    'branch' => $branchId,
                    'code'   => $deviceCode,
                    'name'   => $name,
                    'mac'    => $mac,
                    'type'   => $type,
                    'w'      => $width,
                ]
            );
        }

        Response::success([
            'id'          => $id,
            'device_name' => $name,
            'mac_address' => $mac,
            'paper_width' => $width,
        ], 'Device saved successfully', 201);
    }

    private function ensureReceiptSettingsTable(): void
    {
        try {
            Database::execute(
                "CREATE TABLE IF NOT EXISTS receipt_settings (
                    restaurant_id VARCHAR(64) PRIMARY KEY,
                    header_text TEXT NULL,
                    footer_text TEXT NULL,
                    qr_image_url TEXT NULL,
                    show_qr TINYINT(1) DEFAULT 1,
                    paper_width VARCHAR(10) DEFAULT '58mm',
                    font_size INT DEFAULT 18,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )"
            );
        } catch (\Throwable $e) {
            // Ignore if already exists
        }
    }

    /**
     * GET /printer-settings
     */
    public function receiptSettings(Request $request): void
    {
        $this->ensureReceiptSettingsTable();
        $restaurantId = $request->getAttribute('restaurant_id');

        $row = Database::fetchOne(
            "SELECT header_text, footer_text, qr_image_url, show_qr, paper_width, font_size
             FROM receipt_settings
             WHERE restaurant_id = :restro
             LIMIT 1",
            ['restro' => $restaurantId]
        );

        if (!$row) {
            $restro = Database::fetchOne(
                "SELECT name FROM restaurants WHERE id = :id LIMIT 1",
                ['id' => $restaurantId]
            );
            $defaultHeader = !empty($restro['name']) ? (string)$restro['name'] : 'RESTAURANT';
            $row = [
                'header_text'  => $defaultHeader,
                'footer_text'  => "Thank you! Visit Again\nPowered by AISRestroOS POS",
                'qr_image_url' => null,
                'show_qr'      => true,
                'paper_width'  => '58mm',
                'font_size'    => 18,
            ];
        } else {
            $row = [
                'header_text'  => (string)($row['header_text'] ?? ''),
                'footer_text'  => (string)($row['footer_text'] ?? ''),
                'qr_image_url' => !empty($row['qr_image_url']) ? (string)$row['qr_image_url'] : null,
                'show_qr'      => (int)($row['show_qr'] ?? 1) === 1,
                'paper_width'  => !empty($row['paper_width']) ? (string)$row['paper_width'] : '58mm',
                'font_size'    => max(14, min(24, (int)($row['font_size'] ?? 18))),
            ];
        }

        Response::success($row, 'Receipt settings retrieved');
    }

    /**
     * POST /printer-settings
     */
    public function saveReceiptSettings(Request $request): void
    {
        $this->ensureReceiptSettingsTable();
        $restaurantId = $request->getAttribute('restaurant_id');
        $input = $request->input();

        $headerText = trim((string)($input['header_text'] ?? ''));
        $footerText = trim((string)($input['footer_text'] ?? ''));
        $qrUrl      = isset($input['qr_image_url']) && trim((string)$input['qr_image_url']) !== ''
            ? trim((string)$input['qr_image_url'])
            : null;
        $showQr     = isset($input['show_qr']) ? ((bool)$input['show_qr'] ? 1 : 0) : 1;
        $paperWidth = in_array(($input['paper_width'] ?? '58mm'), ['58mm', '80mm'], true)
            ? (string)$input['paper_width']
            : '58mm';
        $fontSize   = max(14, min(24, (int)($input['font_size'] ?? 18)));

        $existing = Database::fetchOne(
            "SELECT restaurant_id FROM receipt_settings WHERE restaurant_id = :restro LIMIT 1",
            ['restro' => $restaurantId]
        );

        if ($existing) {
            Database::execute(
                "UPDATE receipt_settings
                 SET header_text = :header, footer_text = :footer, qr_image_url = :qr,
                     show_qr = :show_qr, paper_width = :width, font_size = :font_size,
                     updated_at = CURRENT_TIMESTAMP
                 WHERE restaurant_id = :restro",
                [
                    'header'    => $headerText,
                    'footer'    => $footerText,
                    'qr'        => $qrUrl,
                    'show_qr'   => $showQr,
                    'width'     => $paperWidth,
                    'font_size' => $fontSize,
                    'restro'    => $restaurantId,
                ]
            );
        } else {
            Database::execute(
                "INSERT INTO receipt_settings (restaurant_id, header_text, footer_text, qr_image_url, show_qr, paper_width, font_size)
                 VALUES (:restro, :header, :footer, :qr, :show_qr, :width, :font_size)",
                [
                    'restro'    => $restaurantId,
                    'header'    => $headerText,
                    'footer'    => $footerText,
                    'qr'        => $qrUrl,
                    'show_qr'   => $showQr,
                    'width'     => $paperWidth,
                    'font_size' => $fontSize,
                ]
            );
        }

        Response::success([
            'header_text'  => $headerText,
            'footer_text'  => $footerText,
            'qr_image_url' => $qrUrl,
            'show_qr'      => $showQr === 1,
            'paper_width'  => $paperWidth,
            'font_size'    => $fontSize,
        ], 'Receipt settings saved');
    }

    /**
     * POST /printer-settings/qr
     */
    public function uploadReceiptQr(Request $request): void
    {
        $this->ensureReceiptSettingsTable();
        $restaurantId = $request->getAttribute('restaurant_id');

        if (empty($_FILES['image']['tmp_name']) || !is_uploaded_file($_FILES['image']['tmp_name'])) {
            Response::error('No QR image uploaded', 422);
        }

        if (($_FILES['image']['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
            Response::error('File upload error', 400);
        }

        if (($_FILES['image']['size'] ?? 0) > 5 * 1024 * 1024) {
            Response::error('QR image exceeds maximum allowed size of 5MB', 422);
        }

        $finfo = new \finfo(FILEINFO_MIME_TYPE);
        $mime = $finfo->file($_FILES['image']['tmp_name']);
        $allowedMimes = [
            'image/jpeg' => 'jpg',
            'image/png'  => 'png',
            'image/webp' => 'webp',
        ];
        if (!isset($allowedMimes[$mime])) {
            Response::error('Invalid file format. Only JPEG, PNG, and WEBP images are permitted.', 422);
        }
        $ext = $allowedMimes[$mime];

        $uploadDir = dirname(__DIR__) . '/uploads/qr';
        if (!is_dir($uploadDir)) {
            @mkdir($uploadDir, 0755, true);
        }

        // Delete previous QR file if local
        $oldRow = Database::fetchOne(
            "SELECT qr_image_url FROM receipt_settings WHERE restaurant_id = :restro LIMIT 1",
            ['restro' => $restaurantId]
        );
        if (!empty($oldRow['qr_image_url'])) {
            $oldPath = parse_url((string)$oldRow['qr_image_url'], PHP_URL_PATH);
            if ($oldPath && str_contains($oldPath, '/uploads/qr/')) {
                $fileName = basename($oldPath);
                $qrRoot = realpath($uploadDir);
                $fullOld = $qrRoot !== false ? realpath($uploadDir . '/' . $fileName) : false;
                if ($qrRoot !== false && $fullOld !== false && str_starts_with($fullOld, $qrRoot . DIRECTORY_SEPARATOR) && is_file($fullOld)) {
                    @unlink($fullOld);
                }
            }
        }

        $safeRestro = preg_replace('/[^a-zA-Z0-9_-]/', '', (string)$restaurantId);
        $newFileName = 'qr_' . $safeRestro . '_' . time() . '.' . $ext;
        $destPath = $uploadDir . '/' . $newFileName;

        if (!move_uploaded_file($_FILES['image']['tmp_name'], $destPath)) {
            Response::error('Failed to save uploaded QR image', 500);
        }

        $scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
        $host = $_SERVER['HTTP_HOST'] ?? 'restro.trakiyo.in';
        $scriptDir = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '/api/index.php')), '/');
        $publicUrl = "{$scheme}://{$host}{$scriptDir}/uploads/qr/{$newFileName}";

        Response::success(['url' => $publicUrl], 'QR image uploaded successfully');
    }
}
