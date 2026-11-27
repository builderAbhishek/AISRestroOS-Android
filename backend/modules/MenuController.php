<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;
use AISRestro\Core\Helpers;

class MenuController
{
    private function ensureVariantsColumn(): void
    {
        static $checked = false;
        if ($checked) return;
        $checked = true;
        try {
            if (Database::isSqlite()) {
                $cols = Database::fetchAll("PRAGMA table_info(menu_items)");
                $has = false;
                foreach ($cols as $c) {
                    if (($c['name'] ?? '') === 'variants_json') { $has = true; break; }
                }
                if (!$has) Database::execute("ALTER TABLE menu_items ADD COLUMN variants_json TEXT NULL");
            } else {
                $col = Database::fetchOne("SHOW COLUMNS FROM menu_items LIKE 'variants_json'");
                if (!$col) Database::execute("ALTER TABLE menu_items ADD COLUMN variants_json TEXT NULL");
            }
        } catch (\Throwable $e) {}
    }

    /**
     * GET /menu-items
     */
    public function index(Request $request): void
    {
        $this->ensureVariantsColumn();
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, code, name, category, price, tax_mode, tax_percentage, 
                    current_stock, is_default_tax, is_favourite, is_available, is_veg, image_url, variants_json 
             FROM menu_items 
             WHERE restaurant_id = :restro 
             ORDER BY name ASC",
            ['restro' => $restaurantId]
        );

        $data = array_map(function ($row) {
            $variants = [];
            if (!empty($row['variants_json'])) {
                $decoded = json_decode((string)$row['variants_json'], true);
                if (is_array($decoded)) {
                    foreach ($decoded as $v) {
                        if (!empty($v['name']) && isset($v['price'])) {
                            $variants[] = [
                                'name'  => (string)$v['name'],
                                'price' => (float)$v['price'],
                            ];
                        }
                    }
                }
            }
            return [
                'id'             => (string)$row['id'],
                'code'           => $row['code'] !== null ? (string)$row['code'] : null,
                'name'           => (string)$row['name'],
                'category'       => (string)$row['category'],
                'price'          => (float)$row['price'],
                'tax_mode'       => (string)($row['tax_mode'] ?: 'WITHOUT_TAX'),
                'tax_percentage' => (string)($row['tax_percentage'] ?: 'None'),
                'current_stock'  => (int)$row['current_stock'],
                'is_default_tax' => (bool)$row['is_default_tax'],
                'is_favourite'   => (bool)$row['is_favourite'],
                'image_url'      => $row['image_url'] ?: null,
                'variants'       => $variants,
            ];
        }, $rows);

        Response::success($data, 'Menu items retrieved successfully');
    }

    /**
     * POST /menu-items
     */
    public function store(Request $request): void
    {
        $this->ensureVariantsColumn();
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'name'     => 'required|min:1',
            'price'    => 'required|numeric',
            'category' => 'required',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $id = !empty($input['id']) ? trim($input['id']) : UUID::v4();
        $code = !empty($input['code']) ? trim($input['code']) : null;
        $name = trim($input['name']);
        $category = trim($input['category']);
        $price = (float)$input['price'];
        $taxMode = !empty($input['tax_mode']) ? trim($input['tax_mode']) : 'WITHOUT_TAX';
        $taxPercentage = !empty($input['tax_percentage']) ? trim($input['tax_percentage']) : 'None';
        $currentStock = isset($input['current_stock']) ? (int)$input['current_stock'] : 100;
        $isDefaultTax = isset($input['is_default_tax']) ? (int)(bool)$input['is_default_tax'] : 1;
        $isFavourite = isset($input['is_favourite']) ? (int)(bool)$input['is_favourite'] : 0;
        $imageUrl = !empty($input['image_url']) ? trim($input['image_url']) : null;

        $cleanVariants = [];
        if (!empty($input['variants']) && is_array($input['variants'])) {
            foreach ($input['variants'] as $v) {
                $vName = trim((string)($v['name'] ?? ''));
                $vPrice = isset($v['price']) ? (float)$v['price'] : 0.0;
                if ($vName !== '' && $vPrice > 0) {
                    $cleanVariants[] = ['name' => $vName, 'price' => $vPrice];
                }
            }
        }
        $variantsJson = !empty($cleanVariants) ? json_encode($cleanVariants, JSON_UNESCAPED_UNICODE) : null;
        if (!empty($cleanVariants) && $price <= 0) {
            $price = (float)$cleanVariants[0]['price'];
        }

        // Upsert menu item
        $existing = Database::fetchOne(
            "SELECT id, image_url FROM menu_items WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'restro' => $restaurantId]
        );

        if ($existing) {
            if (!empty($existing['image_url']) && $existing['image_url'] !== $imageUrl) {
                $this->deleteOldImageFile($existing['image_url']);
            }
            Database::execute(
                "UPDATE menu_items SET 
                    code = :code, name = :name, category = :category, price = :price, 
                    tax_mode = :tax_mode, tax_percentage = :tax_pct, current_stock = :stock, 
                    is_default_tax = :def_tax, is_favourite = :fav, image_url = :img, 
                    variants_json = :variants_json, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = :id AND restaurant_id = :restro",
                [
                    'code'          => $code,
                    'name'          => $name,
                    'category'      => $category,
                    'price'         => $price,
                    'tax_mode'      => $taxMode,
                    'tax_pct'       => $taxPercentage,
                    'stock'         => $currentStock,
                    'def_tax'       => $isDefaultTax,
                    'fav'           => $isFavourite,
                    'img'           => $imageUrl,
                    'variants_json' => $variantsJson,
                    'id'            => $id,
                    'restro'        => $restaurantId,
                ]
            );
        } else {
            Database::execute(
                "INSERT INTO menu_items (
                    id, restaurant_id, branch_id, code, name, category, price, 
                    tax_mode, tax_percentage, current_stock, is_default_tax, is_favourite, 
                    image_url, variants_json, created_at, updated_at
                 ) VALUES (
                    :id, :restro, :branch, :code, :name, :category, :price, 
                    :tax_mode, :tax_pct, :stock, :def_tax, :fav, 
                    :img, :variants_json, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                 )",
                [
                    'id'            => $id,
                    'restro'        => $restaurantId,
                    'branch'        => $branchId,
                    'code'          => $code,
                    'name'          => $name,
                    'category'      => $category,
                    'price'         => $price,
                    'tax_mode'      => $taxMode,
                    'tax_pct'       => $taxPercentage,
                    'stock'         => $currentStock,
                    'def_tax'       => $isDefaultTax,
                    'fav'           => $isFavourite,
                    'img'           => $imageUrl,
                    'variants_json' => $variantsJson,
                ]
            );
        }

        Response::success([
            'id'             => $id,
            'code'           => $code,
            'name'           => $name,
            'category'       => $category,
            'price'          => $price,
            'tax_mode'       => $taxMode,
            'tax_percentage' => $taxPercentage,
            'current_stock'  => $currentStock,
            'is_default_tax' => (bool)$isDefaultTax,
            'is_favourite'   => (bool)$isFavourite,
            'image_url'      => $imageUrl,
        ], 'Menu item saved successfully', 201);
    }

    /**
     * GET /menu-items/{id}
     */
    public function show(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->param('id');

        $row = Database::fetchOne(
            "SELECT * FROM menu_items WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'restro' => $restaurantId]
        );

        if (!$row) {
            Response::notFound('Menu item not found');
        }

        Response::success([
            'id'             => (string)$row['id'],
            'code'           => $row['code'],
            'name'           => (string)$row['name'],
            'category'       => (string)$row['category'],
            'price'          => (float)$row['price'],
            'tax_mode'       => (string)$row['tax_mode'],
            'tax_percentage' => (string)$row['tax_percentage'],
            'current_stock'  => (int)$row['current_stock'],
            'is_default_tax' => (bool)$row['is_default_tax'],
            'is_favourite'   => (bool)$row['is_favourite'],
            'image_url'      => $row['image_url'],
        ]);
    }

    /**
     * DELETE /menu-items/{id}
     */
    public function destroy(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->param('id');

        // Delete associated image file from disk if present
        $existing = Database::fetchOne(
            "SELECT image_url FROM menu_items WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'restro' => $restaurantId]
        );
        if ($existing && !empty($existing['image_url'])) {
            $this->deleteOldImageFile($existing['image_url']);
        }

        // Record tombstone for offline sync
        Database::execute(
            "INSERT INTO sync_tombstones (restaurant_id, entity_type, entity_uuid, sync_seq, created_at) 
             VALUES (:restro, 'menu_item', :uuid, UNIX_TIMESTAMP(), CURRENT_TIMESTAMP)",
            ['restro' => $restaurantId, 'uuid' => $id]
        );

        Database::execute(
            "DELETE FROM menu_items WHERE id = :id AND restaurant_id = :restro",
            ['id' => $id, 'restro' => $restaurantId]
        );

        Response::success(null, 'Menu item deleted successfully');
    }

    /**
     * POST /menu-items/upload-image
     */
    public function uploadImage(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id') ?: (!empty($_POST['restaurant_id']) ? trim($_POST['restaurant_id']) : 'AISR/01');
        $itemId = !empty($_POST['item_id']) ? trim($_POST['item_id']) : (!empty($_GET['item_id']) ? trim($_GET['item_id']) : 'item_' . bin2hex(random_bytes(6)));

        // Look up previous image file for this item (owned by this restaurant) and delete it from storage
        $existing = Database::fetchOne(
            "SELECT image_url FROM menu_items WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $itemId, 'restro' => $restaurantId]
        );
        $oldImageUrl = $existing['image_url'] ?? null;
        if (!empty($oldImageUrl)) {
            $this->deleteOldImageFile($oldImageUrl);
        }

        $file = $request->file('image');

        if (!$file || empty($file['tmp_name'])) {
            Response::error('No image file uploaded in field "image"', 422);
        }

        if ($file['error'] !== UPLOAD_ERR_OK) {
            Response::error('File upload error code: ' . $file['error'], 400);
        }

        // Validate file size (max 8MB)
        $maxBytes = 8 * 1024 * 1024;
        if ($file['size'] > $maxBytes) {
            Response::error('File size exceeds maximum allowed size of 8MB', 422);
        }

        // Validate MIME type
        $finfo = new \finfo(FILEINFO_MIME_TYPE);
        $mime = $finfo->file($file['tmp_name']);
        $allowedMimes = [
            'image/jpeg' => 'jpg',
            'image/png'  => 'png',
            'image/webp' => 'webp',
            'image/gif'  => 'gif',
        ];

        if (!isset($allowedMimes[$mime])) {
            Response::error('Invalid file format. Only JPEG, PNG, WEBP, and GIF images are permitted.', 422);
        }

        $extension = $allowedMimes[$mime];

        // Sanitize restaurant ID for directory naming (e.g. AISR/01 -> AISR_01)
        $cleanRestroId = preg_replace('/[^A-Za-z0-9_-]/', '_', $restaurantId);
        $cleanItemId   = preg_replace('/[^A-Za-z0-9_-]/', '_', $itemId);

        // Store in structured folder: uploads/restaurants/{restaurant_id}/items/
        $uploadDir = dirname(__DIR__) . "/uploads/restaurants/{$cleanRestroId}/items/";
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }

        // Filename with restaurant ID and item ID
        $filename = "{$cleanRestroId}_{$cleanItemId}_" . time() . '.' . $extension;
        $targetPath = $uploadDir . $filename;

        if (!move_uploaded_file($file['tmp_name'], $targetPath)) {
            Response::error('Failed to save uploaded file onto storage.', 500);
        }

        $baseUrl = Helpers::getBaseUrl();
        $publicUrl = "{$baseUrl}/uploads/restaurants/{$cleanRestroId}/items/{$filename}";
        $relativePath = "uploads/restaurants/{$cleanRestroId}/items/{$filename}";

        // If item exists in menu_items, update its image_url directly
        try {
            Database::execute(
                "UPDATE menu_items SET image_url = :img, updated_at = CURRENT_TIMESTAMP WHERE id = :id AND restaurant_id = :restro",
                [
                    'img'    => $publicUrl,
                    'id'     => $itemId,
                    'restro' => $restaurantId,
                ]
            );
        } catch (\Exception $e) {
            // Non-blocking if item is not saved yet
        }

        Response::success([
            'url'           => $publicUrl,
            'path'          => $relativePath,
            'restaurant_id' => $restaurantId,
            'item_id'       => $itemId,
            'filename'      => $filename,
        ], 'Image uploaded successfully', 201);
    }

    /**
     * Delete old image file from storage if it exists
     */
    private function deleteOldImageFile(?string $imageUrl): void
    {
        if (empty($imageUrl)) {
            return;
        }
        try {
            $parsedPath = parse_url($imageUrl, PHP_URL_PATH);
            if (!$parsedPath) {
                return;
            }
            $pos = strpos($parsedPath, 'uploads/restaurants/');
            if ($pos !== false) {
                $relative = substr($parsedPath, $pos);
                $uploadsRoot = realpath(dirname(__DIR__) . '/uploads/restaurants');
                $filePath = $uploadsRoot !== false ? realpath(dirname(__DIR__) . '/' . $relative) : false;
                if ($uploadsRoot !== false && $filePath !== false && str_starts_with($filePath, $uploadsRoot . DIRECTORY_SEPARATOR) && is_file($filePath)) {
                    @unlink($filePath);
                }
            }
        } catch (\Throwable $e) {
            error_log("[Delete Old Image Warning] " . $e->getMessage());
        }
    }
}
