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
     * GET /public/menu?r=AISR/01 - Public Mobile-First Digital QR Menu for Customers
     */
    public function publicWebMenu(Request $request): void
    {
        $this->ensureVariantsColumn();
        $restaurantId = trim((string)($request->query('r') ?? ''));
        if ($restaurantId === '') {
            http_response_code(400);
            echo "<h1>Restaurant ID missing</h1>";
            exit;
        }

        $restaurant = Database::fetchOne(
            "SELECT id, name, phone, address FROM restaurants WHERE id = :id OR REPLACE(id, '/', '') = :clean LIMIT 1",
            ['id' => $restaurantId, 'clean' => preg_replace('/[^A-Za-z0-9]/', '', $restaurantId)]
        );

        if (!$restaurant) {
            http_response_code(404);
            echo "<h1>Menu not found</h1>";
            exit;
        }

        $restroIdReal = (string)$restaurant['id'];
        $categories = Database::fetchAll(
            "SELECT name FROM categories WHERE restaurant_id = :restro ORDER BY display_order ASC, name ASC",
            ['restro' => $restroIdReal]
        );
        $items = Database::fetchAll(
            "SELECT name, category, price, image_url, is_veg, variants_json
             FROM menu_items
             WHERE restaurant_id = :restro AND COALESCE(is_available, 1) = 1
             ORDER BY category ASC, name ASC",
            ['restro' => $restroIdReal]
        );

        $restroName = htmlspecialchars((string)$restaurant['name'], ENT_QUOTES, 'UTF-8');
        $restroAddr = htmlspecialchars((string)($restaurant['address'] ?? ''), ENT_QUOTES, 'UTF-8');
        $restroPhone = htmlspecialchars((string)($restaurant['phone'] ?? ''), ENT_QUOTES, 'UTF-8');

        header('Content-Type: text/html; charset=UTF-8');
        echo '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">';
        echo "<title>{$restroName} — Digital Menu</title>";
        echo '<style>
            *{box-sizing:border-box;margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif}
            body{background:#f8fafc;color:#0f172a;padding-bottom:40px}
            .hdr{background:linear-gradient(135deg,#0f172a,#1e293b);color:#fff;padding:20px 16px;border-radius:0 0 22px 22px;text-align:center}
            .hdr h1{font-size:21px;font-weight:800;margin-bottom:4px}
            .hdr p{font-size:12px;color:#cbd5e1}
            .srch-wrap{padding:14px 16px 8px;position:sticky;top:0;background:#f8fafc;z-index:10}
            .srch{width:100%;padding:11px 14px;border:1px solid #cbd5e1;border-radius:12px;font-size:14px;outline:none;background:#fff}
            .cats{display:flex;gap:8px;overflow-x:auto;padding:8px 16px 12px;scrollbar-width:none}
            .cat{padding:7px 14px;border-radius:20px;background:#fff;border:1px solid #e2e8f0;font-size:12.5px;font-weight:600;white-space:nowrap;cursor:pointer;color:#475569}
            .cat.active{background:#f97316;color:#fff;border-color:#f97316}
            .list{padding:0 16px;display:flex;flex-direction:column;gap:10px;max-width:640px;margin:0 auto}
            .card{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:12px;display:flex;justify-content:space-between;align-items:center;gap:12px;box-shadow:0 1px 2px rgba(0,0,0,0.03)}
            .veg{display:inline-block;width:11px;height:11px;border:1.5px solid #16a34a;border-radius:3px;position:relative;margin-right:6px;vertical-align:middle}
            .veg::after{content:"";width:5px;height:5px;background:#16a34a;border-radius:50%;position:absolute;top:1.5px;left:1.5px}
            .nonveg{border-color:#dc2626}.nonveg::after{background:#dc2626}
            .title{font-size:15px;font-weight:700;color:#0f172a}
            .cat-lbl{font-size:11.5px;color:#64748b;margin-top:2px}
            .vars{display:flex;flex-wrap:wrap;gap:6px;margin-top:6px}
            .vchip{background:#fff7ed;color:#c2410c;border:1px solid #fed7aa;padding:3px 8px;border-radius:8px;font-size:11.5px;font-weight:700}
            .price{font-size:16px;font-weight:800;color:#ea580c;white-space:nowrap}
            .thumb{width:64px;height:64px;border-radius:10px;object-fit:cover;flex-shrink:0}
            .ftr{text-align:center;font-size:11px;color:#94a3b8;margin-top:28px}
        </style></head><body>';
        echo "<div class=\"hdr\"><h1>{$restroName}</h1>";
        if ($restroAddr !== '' || $restroPhone !== '') {
            echo "<p>{$restroAddr}" . ($restroPhone !== '' ? " • {$restroPhone}" : '') . "</p>";
        }
        echo '</div>';
        echo '<div class="srch-wrap"><input id="q" class="srch" type="search" placeholder="Search dishes..." oninput="filterMenu()"></div>';
        echo '<div class="cats"><button class="cat active" onclick="setCat(\'ALL\',this)">All</button>';
        foreach ($categories as $c) {
            $cn = htmlspecialchars((string)$c['name'], ENT_QUOTES, 'UTF-8');
            echo "<button class=\"cat\" onclick=\"setCat('{$cn}',this)\">{$cn}</button>";
        }
        echo '</div><div class="list" id="menuList">';
        foreach ($items as $it) {
            $name = htmlspecialchars((string)$it['name'], ENT_QUOTES, 'UTF-8');
            $cat = htmlspecialchars((string)($it['category'] ?: 'General'), ENT_QUOTES, 'UTF-8');
            $price = (float)$it['price'];
            $priceStr = ($price == (int)$price) ? '₹' . (int)$price : '₹' . number_format($price, 2);
            $isVeg = (int)($it['is_veg'] ?? 1) === 1;
            $img = !empty($it['image_url']) ? htmlspecialchars((string)$it['image_url'], ENT_QUOTES, 'UTF-8') : '';
            $vars = !empty($it['variants_json']) ? json_decode((string)$it['variants_json'], true) : [];
            echo "<div class=\"card\" data-name=\"" . strtolower($name) . "\" data-cat=\"{$cat}\">";
            echo '<div style="flex:1">';
            echo '<div class="title"><span class="veg ' . ($isVeg ? '' : 'nonveg') . '"></span>' . $name . '</div>';
            echo "<div class=\"cat-lbl\">{$cat}</div>";
            if (is_array($vars) && count($vars) > 0) {
                echo '<div class="vars">';
                foreach ($vars as $v) {
                    $vn = htmlspecialchars((string)($v['name'] ?? ''), ENT_QUOTES, 'UTF-8');
                    $vp = (float)($v['price'] ?? 0);
                    $vpStr = ($vp == (int)$vp) ? '₹' . (int)$vp : '₹' . number_format($vp, 2);
                    echo "<span class=\"vchip\">{$vn}: {$vpStr}</span>";
                }
                echo '</div>';
            }
            echo '</div>';
            if ($img !== '') {
                echo "<img class=\"thumb\" src=\"{$img}\" alt=\"{$name}\" loading=\"lazy\">";
            }
            if (!is_array($vars) || count($vars) === 0) {
                echo "<div class=\"price\">{$priceStr}</div>";
            }
            echo '</div>';
        }
        echo '</div><div class="ftr">Digital Menu • Powered by AISRestroOS</div>';
        echo '<script>
            let activeCat="ALL";
            function setCat(c,el){activeCat=c;document.querySelectorAll(".cat").forEach(b=>b.classList.remove("active"));el.classList.add("active");filterMenu();}
            function filterMenu(){
                const q=document.getElementById("q").value.trim().toLowerCase();
                document.querySelectorAll(".card").forEach(card=>{
                    const mCat=(activeCat==="ALL"||card.dataset.cat===activeCat);
                    const mQ=(!q||card.dataset.name.includes(q)||card.dataset.cat.toLowerCase().includes(q));
                    card.style.display=(mCat&&mQ)?"flex":"none";
                });
            }
        </script></body></html>';
        exit;
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
