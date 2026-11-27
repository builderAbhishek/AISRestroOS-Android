<?php
require_once __DIR__ . '/includes/auth.php';
$pageTitle = 'Menu & Categories';

/**
 * Ensure menu_items.variants_json column exists for portion/size pricing.
 */
function ensure_variants_column(PDO $pdo): void {
    static $checked = false;
    if ($checked) {
        return;
    }
    $checked = true;
    try {
        $col = $pdo->query("SHOW COLUMNS FROM menu_items LIKE 'variants_json'")->fetch();
        if (!$col) {
            $pdo->exec("ALTER TABLE menu_items ADD COLUMN variants_json TEXT NULL AFTER price");
        }
    } catch (Throwable $e) {
        // Ignore if permissions/engine already handled it
    }
}

ensure_variants_column($pdo);

/**
 * Kisi restaurant ke liye category dhoondta hai; agar nahi milti to auto-create karke ID return karta hai.
 */
function ensure_category(PDO $pdo, string $restaurantId, string $categoryName): string {
    $cleanName = trim($categoryName);
    if ($cleanName === '') {
        $cleanName = 'General';
    }

    $stmt = $pdo->prepare("SELECT id FROM categories WHERE restaurant_id = ? AND LOWER(TRIM(name)) = LOWER(?) LIMIT 1");
    $stmt->execute([$restaurantId, $cleanName]);
    $existingId = $stmt->fetchColumn();

    if ($existingId) {
        return (string)$existingId;
    }

    $maxStmt = $pdo->prepare("SELECT COALESCE(MAX(display_order), 0) + 1 FROM categories WHERE restaurant_id = ?");
    $maxStmt->execute([$restaurantId]);
    $nextOrder = (int)$maxStmt->fetchColumn();

    $newCatId = uuid_v4();
    $ins = $pdo->prepare("INSERT INTO categories (id, restaurant_id, name, display_order, sync_seq, created_at, updated_at) VALUES (?, ?, ?, ?, 1, NOW(), NOW())");
    $ins->execute([$newCatId, $restaurantId, $cleanName, $nextOrder]);

    return $newCatId;
}

/**
 * Parses variant string from CSV or JSON into normalized [['name' => ..., 'price' => ...], ...].
 * Supports:
 * - JSON: [{"name":"Half","price":90},{"name":"Full","price":160}]
 * - Pipe/semicolon separated: "Half:90|Full:160" or "Small=149;Medium=249;Large=349"
 */
function parse_variants_string(string $raw): array {
    $raw = trim($raw);
    if ($raw === '') {
        return [];
    }

    if ($raw[0] === '[') {
        $decoded = json_decode($raw, true);
        if (is_array($decoded)) {
            $clean = [];
            foreach ($decoded as $v) {
                $vName = trim((string)($v['name'] ?? ''));
                $vPrice = (float)($v['price'] ?? 0);
                if ($vName !== '' && $vPrice > 0) {
                    $clean[] = ['name' => $vName, 'price' => $vPrice];
                }
            }
            return $clean;
        }
    }

    $parts = preg_split('/[|;]+/', $raw);
    $clean = [];
    foreach ($parts as $part) {
        $part = trim($part);
        if ($part === '') {
            continue;
        }
        $pair = preg_split('/[:=]/', $part, 2);
        if (count($pair) === 2) {
            $vName = trim($pair[0]);
            $vPrice = (float)preg_replace('/[^0-9.]/', '', $pair[1]);
            if ($vName !== '' && $vPrice > 0) {
                $clean[] = ['name' => $vName, 'price' => $vPrice];
            }
        }
    }
    return $clean;
}

/**
 * CSV row ko flexible column names ke hisab se parse karta hai.
 */
function map_csv_row(array $row, array $headerMap): array {
    $getVal = function (array $keys, $default = '') use ($row, $headerMap) {
        foreach ($keys as $k) {
            if (isset($headerMap[$k]) && isset($row[$headerMap[$k]])) {
                return trim((string)$row[$headerMap[$k]]);
            }
        }
        return $default;
    };

    $name        = $getVal(['name', 'item_name', 'dish_name', 'dish', 'title']);
    $code        = $getVal(['code', 'item_code', 'sku']);
    $category    = $getVal(['category', 'category_name', 'cat'], 'General');
    $priceRaw    = $getVal(['price', 'rate', 'amount'], '0');
    $variantsRaw = $getVal(['variants', 'variant', 'portions', 'sizes', 'variants_json'], '');
    $stockRaw    = $getVal(['stock', 'current_stock', 'qty', 'quantity'], '100');
    $vegRaw      = strtolower($getVal(['is_veg', 'veg', 'type', 'food_type'], '1'));
    $availRaw    = strtolower($getVal(['is_available', 'available', 'status'], '1'));

    $variants = parse_variants_string($variantsRaw);
    $price = (float)preg_replace('/[^0-9.]/', '', $priceRaw);
    if (!empty($variants) && $price <= 0) {
        $price = min(array_column($variants, 'price'));
    }

    $stock = (int)preg_replace('/[^0-9-]/', '', $stockRaw);
    if ($stockRaw === '') {
        $stock = 100;
    }

    if (in_array($vegRaw, ['0', 'no', 'false', 'non-veg', 'nonveg', 'non veg'], true)) {
        $isVeg = 0;
    } else {
        $isVeg = 1;
    }

    if (in_array($availRaw, ['0', 'no', 'false', 'unavailable', 'inactive'], true)) {
        $isAvail = 0;
    } else {
        $isAvail = 1;
    }

    return [
        'name'          => $name,
        'code'          => $code !== '' ? $code : null,
        'category'      => $category !== '' ? $category : 'General',
        'price'         => $price,
        'variants_json' => !empty($variants) ? json_encode($variants, JSON_UNESCAPED_UNICODE) : null,
        'stock'         => $stock,
        'is_veg'        => $isVeg,
        'is_available'  => $isAvail,
    ];
}

// Sample CSV Download
if (isset($_GET['sample_csv'])) {
    header('Content-Type: text/csv; charset=utf-8');
    header('Content-Disposition: attachment; filename="menu_import_sample.csv"');
    $out = fopen('php://output', 'w');
    fputcsv($out, ['name', 'code', 'category', 'price', 'variants', 'stock', 'is_veg', 'is_available']);
    fputcsv($out, ['Paneer Tikka', 'PT01', 'Starters', '220', '', '50', '1', '1']);
    fputcsv($out, ['Veg Chowmein', 'VC02', 'Chinese', '90', 'Half:90|Full:160', '50', '1', '1']);
    fputcsv($out, ['Margherita Pizza', 'MP03', 'Pizza', '149', 'Small:149|Medium:249|Large:349', '40', '1', '1']);
    fputcsv($out, ['Paneer Pakoda', 'PP04', 'Snacks', '110', 'Half (4 pcs):110|Full (8 pcs):200', '40', '1', '1']);
    fputcsv($out, ['Chicken Biryani', 'CB05', 'Main Course', '280', '', '40', '0', '1']);
    fclose($out);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    verify_csrf('menu.php');
    $entity = $_POST['entity'] ?? 'menu_item';
    $mode   = $_POST['mode'] ?? 'create';

    try {
        if ($entity === 'import_csv') {
            $restroId = trim($_POST['restaurant_id'] ?? '');
            if ($restroId === '') {
                throw new RuntimeException('Kripya restaurant select karein.');
            }
            if (empty($_FILES['csv_file']['tmp_name']) || !is_uploaded_file($_FILES['csv_file']['tmp_name'])) {
                throw new RuntimeException('Kripya valid CSV / Excel export file upload karein.');
            }

            $ext = strtolower(pathinfo($_FILES['csv_file']['name'] ?? '', PATHINFO_EXTENSION));
            if ($ext === 'xlsx' || $ext === 'xls') {
                throw new RuntimeException('.xls/.xlsx binary file directly PHP me bina library ke parse nahi hoti. Excel me "Save As -> CSV UTF-8 (.csv)" karke upload karein.');
            }

            $handle = fopen($_FILES['csv_file']['tmp_name'], 'r');
            if (!$handle) {
                throw new RuntimeException('Uploaded file open nahi ho pa rahi.');
            }

            // Delimiter auto-detect (comma vs semicolon vs tab)
            $firstLine = fgets($handle);
            if ($firstLine === false) {
                fclose($handle);
                throw new RuntimeException('File khali hai.');
            }
            $firstLine = preg_replace('/^\xEF\xBB\xBF/', '', $firstLine); // strip UTF-8 BOM
            $delimiter = ',';
            if (substr_count($firstLine, ';') > substr_count($firstLine, ',')) {
                $delimiter = ';';
            } elseif (substr_count($firstLine, "\t") > substr_count($firstLine, ',')) {
                $delimiter = "\t";
            }
            rewind($handle);

            $rowIndex = 0;
            $headerMap = [];
            $importedCount = 0;
            $skippedCount = 0;

            $pdo->beginTransaction();

            $insertStmt = $pdo->prepare("
                INSERT INTO menu_items
                (id, restaurant_id, code, name, category, category_id, price, variants_json, current_stock, is_available, is_veg, sync_seq, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(), NOW())
            ");

            while (($row = fgetcsv($handle, 0, $delimiter)) !== false) {
                if (count($row) === 1 && trim((string)$row[0]) === '') {
                    continue;
                }
                if ($rowIndex === 0) {
                    $row[0] = preg_replace('/^\xEF\xBB\xBF/', '', (string)$row[0]);
                    $normalizedCols = array_map(fn($c) => strtolower(trim((string)$c)), $row);
                    if (in_array('name', $normalizedCols, true) || in_array('item_name', $normalizedCols, true) || in_array('dish_name', $normalizedCols, true)) {
                        foreach ($normalizedCols as $idx => $colName) {
                            if ($colName !== '') {
                                $headerMap[$colName] = $idx;
                            }
                        }
                        $rowIndex++;
                        continue;
                    } else {
                        // Positional fallback: name, code, category, price, variants, stock, is_veg, is_available
                        $headerMap = [
                            'name'         => 0,
                            'code'         => 1,
                            'category'     => 2,
                            'price'        => 3,
                            'variants'     => 4,
                            'stock'        => 5,
                            'is_veg'       => 6,
                            'is_available' => 7,
                        ];
                    }
                }

                $item = map_csv_row($row, $headerMap);
                if ($item['name'] === '') {
                    $skippedCount++;
                    $rowIndex++;
                    continue;
                }

                $catId = ensure_category($pdo, $restroId, $item['category']);

                $insertStmt->execute([
                    uuid_v4(),
                    $restroId,
                    $item['code'],
                    $item['name'],
                    $item['category'],
                    $catId,
                    $item['price'],
                    $item['variants_json'],
                    $item['stock'],
                    $item['is_available'],
                    $item['is_veg'],
                ]);
                $importedCount++;
                $rowIndex++;
            }

            fclose($handle);
            $pdo->commit();

            flash('success', "{$importedCount} menu items (variants ke saath) import ho gaye" . ($skippedCount ? " ({$skippedCount} blank rows skip)" : '') . ". Missing categories auto-add ho gayi hain.");
        } elseif ($entity === 'category') {
            if ($mode === 'delete') {
                $pdo->prepare("DELETE FROM categories WHERE id = ?")->execute([$_POST['id'] ?? '']);
                flash('success', 'Category delete ho gayi.');
            } else {
                $restroId = $_POST['restaurant_id'] ?? '';
                $name     = trim($_POST['name'] ?? '');
                $order    = (int)($_POST['display_order'] ?? 0);
                if ($mode === 'create') {
                    $pdo->prepare("INSERT INTO categories (id, restaurant_id, name, display_order, sync_seq, created_at, updated_at) VALUES (?, ?, ?, ?, 1, NOW(), NOW())")
                        ->execute([uuid_v4(), $restroId, $name, $order]);
                    flash('success', "Category '{$name}' add ho gayi.");
                } else {
                    $pdo->prepare("UPDATE categories SET restaurant_id = ?, name = ?, display_order = ?, updated_at = NOW() WHERE id = ?")
                        ->execute([$restroId, $name, $order, $_POST['id'] ?? '']);
                    flash('success', "Category '{$name}' update ho gayi.");
                }
            }
        } else {
            if ($mode === 'delete') {
                $pdo->prepare("DELETE FROM menu_items WHERE id = ?")->execute([$_POST['id'] ?? '']);
                flash('success', 'Menu item delete ho gaya.');
            } else {
                $restroId = $_POST['restaurant_id'] ?? '';
                $code     = trim($_POST['code'] ?? '');
                $name     = trim($_POST['name'] ?? '');
                $category = trim($_POST['category'] ?? 'General') ?: 'General';
                $price    = (float)($_POST['price'] ?? 0);
                $stock    = (int)($_POST['current_stock'] ?? 100);
                $isVeg    = isset($_POST['is_veg']) ? 1 : 0;
                $isAvail  = isset($_POST['is_available']) ? 1 : 0;

                // Handle Variants Mode
                $pricingMode = $_POST['pricing_mode'] ?? 'single';
                $variantsJson = null;
                if ($pricingMode === 'variants') {
                    $vNames  = $_POST['variant_name'] ?? [];
                    $vPrices = $_POST['variant_price'] ?? [];
                    $parsedVariants = [];
                    foreach ($vNames as $idx => $vName) {
                        $vn = trim((string)$vName);
                        $vp = (float)($vPrices[$idx] ?? 0);
                        if ($vn !== '' && $vp > 0) {
                            $parsedVariants[] = ['name' => $vn, 'price' => $vp];
                        }
                    }
                    if (!empty($parsedVariants)) {
                        $variantsJson = json_encode($parsedVariants, JSON_UNESCAPED_UNICODE);
                        $price = min(array_column($parsedVariants, 'price'));
                    }
                }

                // Category na ho to auto-add karein
                $catId = ensure_category($pdo, $restroId, $category);

                if ($mode === 'create') {
                    $pdo->prepare("INSERT INTO menu_items (id, restaurant_id, code, name, category, category_id, price, variants_json, current_stock, is_available, is_veg, sync_seq, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, NOW(), NOW())")
                        ->execute([uuid_v4(), $restroId, $code ?: null, $name, $category, $catId, $price, $variantsJson, $stock, $isAvail, $isVeg]);
                    flash('success', "Dish '{$name}' add ho gayi (Category '{$category}' ready hai).");
                } else {
                    $pdo->prepare("UPDATE menu_items SET restaurant_id = ?, code = ?, name = ?, category = ?, category_id = ?, price = ?, variants_json = ?, current_stock = ?, is_available = ?, is_veg = ?, updated_at = NOW() WHERE id = ?")
                        ->execute([$restroId, $code ?: null, $name, $category, $catId, $price, $variantsJson, $stock, $isAvail, $isVeg, $_POST['id'] ?? '']);
                    flash('success', "Dish '{$name}' update ho gayi (Category '{$category}' ready hai).");
                }
            }
        }
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        flash('error', 'Error: ' . $e->getMessage());
    }

    header('Location: menu.php');
    exit;
}

$categories  = $pdo->query("SELECT c.*, r.name AS restaurant_name FROM categories c LEFT JOIN restaurants r ON c.restaurant_id = r.id ORDER BY c.display_order, c.name")->fetchAll();
$items       = $pdo->query("SELECT m.*, r.name AS restaurant_name FROM menu_items m LEFT JOIN restaurants r ON m.restaurant_id = r.id ORDER BY m.created_at DESC")->fetchAll();
$restaurants = $pdo->query("SELECT id, name FROM restaurants ORDER BY name")->fetchAll();

require_once __DIR__ . '/includes/header.php';
?>

<div class="flex flex-wrap justify-between items-center gap-3 mb-6">
    <p class="text-sm text-slate-500">Categories, Menu Items aur Portions/Variants manage karein. Nayi category automatically add ho jaati hai.</p>
    <div class="flex flex-wrap gap-2">
        <button onclick="openImportModal()" class="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-semibold">↑ Import CSV / Excel</button>
        <button onclick="openCatModal()" class="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-900 text-white text-sm font-semibold">+ Add Category</button>
        <button onclick="openItemModal()" class="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold">+ Add Menu Item</button>
    </div>
</div>

<!-- Categories Section -->
<div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden mb-8">
    <div class="px-5 py-3.5 border-b border-slate-200 font-bold text-slate-800 text-sm">Categories (<?= count($categories) ?>)</div>
    <div class="overflow-x-auto">
        <table class="w-full text-sm text-left">
            <thead class="bg-slate-50 text-slate-500 text-xs uppercase">
                <tr>
                    <th class="px-5 py-2.5">Restaurant</th>
                    <th class="px-5 py-2.5">Category Name</th>
                    <th class="px-5 py-2.5">Order</th>
                    <th class="px-5 py-2.5 text-right">Actions</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php if (!$categories): ?>
                    <tr><td colspan="4" class="px-5 py-4 text-center text-slate-400 text-xs">Abhi koi category nahi hai.</td></tr>
                <?php endif; ?>
                <?php foreach ($categories as $c): ?>
                <tr>
                    <td class="px-5 py-2.5 text-slate-600"><?= h($c['restaurant_name'] ?? $c['restaurant_id']) ?></td>
                    <td class="px-5 py-2.5 font-semibold"><?= h($c['name']) ?></td>
                    <td class="px-5 py-2.5"><?= (int)$c['display_order'] ?></td>
                    <td class="px-5 py-2.5 text-right space-x-2">
                        <button onclick='openCatModal(<?= json_encode($c, JSON_HEX_APOS | JSON_HEX_QUOT) ?>)' class="px-2.5 py-1 rounded bg-amber-50 text-amber-700 border border-amber-200 text-xs font-semibold">Edit</button>
                        <form method="POST" class="inline" onsubmit="return confirm('Delete category?');">
                            <?= csrf_field() ?>
                            <input type="hidden" name="entity" value="category">
                            <input type="hidden" name="mode" value="delete">
                            <input type="hidden" name="id" value="<?= h($c['id']) ?>">
                            <button class="px-2.5 py-1 rounded bg-rose-50 text-rose-700 border border-rose-200 text-xs font-semibold">Delete</button>
                        </form>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
</div>

<!-- Menu Items Section -->
<div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="px-5 py-3.5 border-b border-slate-200 font-bold text-slate-800 text-sm">Menu Items (<?= count($items) ?>)</div>
    <div class="overflow-x-auto">
        <table class="w-full text-sm text-left">
            <thead class="bg-slate-50 text-slate-500 text-xs uppercase">
                <tr>
                    <th class="px-5 py-3">Item</th>
                    <th class="px-5 py-3">Restaurant</th>
                    <th class="px-5 py-3">Category</th>
                    <th class="px-5 py-3">Price / Variants</th>
                    <th class="px-5 py-3">Type</th>
                    <th class="px-5 py-3 text-right">Actions</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php if (!$items): ?>
                    <tr><td colspan="6" class="px-5 py-6 text-center text-slate-400 text-xs">Abhi koi menu item nahi hai. '+ Add Menu Item' ya 'Import CSV / Excel' use karein.</td></tr>
                <?php endif; ?>
                <?php foreach ($items as $m): ?>
                <?php
                    $itemVariants = [];
                    if (!empty($m['variants_json'])) {
                        $decoded = json_decode((string)$m['variants_json'], true);
                        if (is_array($decoded)) {
                            $itemVariants = $decoded;
                        }
                    }
                ?>
                <tr>
                    <td class="px-5 py-3 font-bold text-slate-800"><?= h($m['name']) ?> <span class="text-xs font-mono text-slate-400"><?= h($m['code'] ?? '') ?></span></td>
                    <td class="px-5 py-3 text-slate-600"><?= h($m['restaurant_name'] ?? $m['restaurant_id']) ?></td>
                    <td class="px-5 py-3"><?= h($m['category']) ?></td>
                    <td class="px-5 py-3">
                        <?php if (!empty($itemVariants)): ?>
                            <div class="font-semibold text-indigo-700 text-xs mb-1">From ₹<?= number_format((float)$m['price'], 2) ?> (<?= count($itemVariants) ?> sizes)</div>
                            <div class="flex flex-wrap gap-1">
                                <?php foreach ($itemVariants as $v): ?>
                                    <span class="px-1.5 py-0.5 rounded bg-slate-100 text-slate-700 text-[11px] font-medium"><?= h($v['name'] ?? '') ?>: ₹<?= number_format((float)($v['price'] ?? 0), 0) ?></span>
                                <?php endforeach; ?>
                            </div>
                        <?php else: ?>
                            <span class="font-semibold text-emerald-700">₹<?= number_format((float)$m['price'], 2) ?></span>
                        <?php endif; ?>
                    </td>
                    <td class="px-5 py-3">
                        <span class="px-2 py-0.5 rounded text-xs font-semibold <?= $m['is_veg'] ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700' ?>">
                            <?= $m['is_veg'] ? 'VEG' : 'NON-VEG' ?>
                        </span>
                    </td>
                    <td class="px-5 py-3 text-right space-x-2">
                        <button onclick='openItemModal(<?= json_encode($m, JSON_HEX_APOS | JSON_HEX_QUOT) ?>)' class="px-2.5 py-1 rounded bg-amber-50 text-amber-700 border border-amber-200 text-xs font-semibold">Edit</button>
                        <form method="POST" class="inline" onsubmit="return confirm('Delete item?');">
                            <?= csrf_field() ?>
                            <input type="hidden" name="entity" value="menu_item">
                            <input type="hidden" name="mode" value="delete">
                            <input type="hidden" name="id" value="<?= h($m['id']) ?>">
                            <button class="px-2.5 py-1 rounded bg-rose-50 text-rose-700 border border-rose-200 text-xs font-semibold">Delete</button>
                        </form>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
</div>

<!-- Import CSV / Excel Modal -->
<div id="importModal" class="fixed inset-0 z-50 bg-slate-900/60 hidden items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden">
        <div class="px-6 py-4 border-b flex justify-between items-center">
            <h3 class="text-lg font-bold text-slate-800">Import Menu Items (CSV / Excel Export)</h3>
            <button onclick="closeModal('importModal')" class="text-slate-400">✕</button>
        </div>
        <form method="POST" enctype="multipart/form-data" class="p-6 space-y-4">
            <?= csrf_field() ?>
            <input type="hidden" name="entity" value="import_csv">

            <div>
                <label class="block text-xs font-semibold text-slate-600 mb-1">Restaurant *</label>
                <select name="restaurant_id" required class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                    <?php foreach ($restaurants as $r): ?>
                        <option value="<?= h($r['id']) ?>"><?= h($r['name']) ?></option>
                    <?php endforeach; ?>
                </select>
            </div>

            <div>
                <div class="flex items-center justify-between mb-1">
                    <label class="block text-xs font-semibold text-slate-600">Select CSV File *</label>
                    <a href="menu.php?sample_csv=1" class="text-xs font-semibold text-indigo-600 hover:underline">Download Sample CSV (with Variants)</a>
                </div>
                <input type="file" name="csv_file" accept=".csv,text/csv" required
                       class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm file:mr-3 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100">
            </div>

            <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 space-y-1.5">
                <div class="font-semibold text-slate-800">Columns:</div>
                <div><code class="bg-white px-1.5 py-0.5 border rounded">name, code, category, price, variants, stock, is_veg, is_available</code></div>
                <div class="text-[11px] text-slate-600 pt-1">
                    <strong>Variants format:</strong> <code class="bg-white px-1 border rounded">Half:90|Full:160</code> ya <code class="bg-white px-1 border rounded">Small:149|Medium:249|Large:349</code> ya <code class="bg-white px-1 border rounded">Half (4 pcs):110|Full (8 pcs):200</code>. Single price item ke liye <strong>variants</strong> blank chhod dein.
                </div>
                <div class="text-[11px] text-slate-500">Excel file ko <strong>File → Save As → CSV UTF-8 (.csv)</strong> karke upload karein. Missing category auto-add ho jayegi.</div>
            </div>

            <div class="pt-3 border-t flex justify-end gap-2">
                <button type="button" onclick="closeModal('importModal')" class="px-4 py-2 rounded-lg border text-sm">Cancel</button>
                <button type="submit" class="px-5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-semibold">Import Items</button>
            </div>
        </form>
    </div>
</div>

<!-- Category Modal -->
<div id="catModal" class="fixed inset-0 z-50 bg-slate-900/60 hidden items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden">
        <div class="px-6 py-4 border-b flex justify-between items-center">
            <h3 id="catModalTitle" class="text-lg font-bold">Add Category</h3>
            <button onclick="closeModal('catModal')" class="text-slate-400">✕</button>
        </div>
        <form method="POST" class="p-6 space-y-4">
            <?= csrf_field() ?>
            <input type="hidden" name="entity" value="category">
            <input type="hidden" name="mode" id="catMode" value="create">
            <input type="hidden" name="id" id="catId">
            <div>
                <label class="block text-xs font-semibold text-slate-600 mb-1">Restaurant *</label>
                <select name="restaurant_id" id="catRestro" required class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                    <?php foreach ($restaurants as $r): ?>
                        <option value="<?= h($r['id']) ?>"><?= h($r['name']) ?></option>
                    <?php endforeach; ?>
                </select>
            </div>
            <div class="grid grid-cols-3 gap-3">
                <div class="col-span-2">
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Category Name *</label>
                    <input type="text" name="name" id="catName" required placeholder="Chinese / Starters" class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                </div>
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Order</label>
                    <input type="number" name="display_order" id="catOrder" value="1" class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                </div>
            </div>
            <div class="pt-3 border-t flex justify-end gap-2">
                <button type="button" onclick="closeModal('catModal')" class="px-4 py-2 rounded-lg border text-sm">Cancel</button>
                <button type="submit" class="px-5 py-2 rounded-lg bg-slate-800 text-white text-sm font-semibold">Save Category</button>
            </div>
        </form>
    </div>
</div>

<!-- Menu Item Modal -->
<div id="itemModal" class="fixed inset-0 z-50 bg-slate-900/60 hidden items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden">
        <div class="px-6 py-4 border-b flex justify-between items-center">
            <h3 id="itemModalTitle" class="text-lg font-bold">Add Menu Item</h3>
            <button onclick="closeModal('itemModal')" class="text-slate-400">✕</button>
        </div>
        <form method="POST" class="p-6 space-y-4 overflow-y-auto">
            <?= csrf_field() ?>
            <input type="hidden" name="entity" value="menu_item">
            <input type="hidden" name="mode" id="itemMode" value="create">
            <input type="hidden" name="id" id="itemId">
            <input type="hidden" name="pricing_mode" id="pricingMode" value="single">

            <div>
                <label class="block text-xs font-semibold text-slate-600 mb-1">Restaurant *</label>
                <select name="restaurant_id" id="itemRestro" required class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                    <?php foreach ($restaurants as $r): ?>
                        <option value="<?= h($r['id']) ?>"><?= h($r['name']) ?></option>
                    <?php endforeach; ?>
                </select>
            </div>

            <div class="grid grid-cols-3 gap-3">
                <div class="col-span-2">
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Dish Name *</label>
                    <input type="text" name="name" id="itemName" required placeholder="Paneer Tikka" class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                </div>
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Short Code</label>
                    <input type="text" name="code" id="itemCode" placeholder="PT01" class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                </div>
            </div>

            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Category (Auto-add)</label>
                    <input type="text" name="category" id="itemCat" list="existingCategoriesList" value="General" class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                    <datalist id="existingCategoriesList">
                        <?php foreach ($categories as $c): ?>
                            <option value="<?= h($c['name']) ?>">
                        <?php endforeach; ?>
                    </datalist>
                </div>
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Stock</label>
                    <input type="number" name="current_stock" id="itemStock" value="100" class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm">
                </div>
            </div>

            <!-- Pricing Mode Toggle -->
            <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-3">
                <div class="flex items-center justify-between">
                    <span class="text-xs font-bold text-slate-700">Pricing &amp; Portions</span>
                    <div class="inline-flex rounded-lg bg-slate-200 p-0.5 text-xs font-semibold">
                        <button type="button" id="btnModeSingle" onclick="setPricingMode('single')" class="px-3 py-1 rounded-md bg-indigo-600 text-white">Single Price</button>
                        <button type="button" id="btnModeVariants" onclick="setPricingMode('variants')" class="px-3 py-1 rounded-md text-slate-600">Variants / Portions</button>
                    </div>
                </div>

                <!-- Single Price Box -->
                <div id="singlePriceBox">
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Price (₹) *</label>
                    <input type="number" step="0.01" name="price" id="itemPrice" placeholder="0.00" class="w-full px-3 py-2 rounded-lg border border-slate-300 text-sm bg-white">
                </div>

                <!-- Variants Box -->
                <div id="variantsBox" class="hidden space-y-2.5">
                    <div class="flex flex-wrap gap-1.5">
                        <button type="button" onclick="applyPreset(['Half','Full'])" class="px-2.5 py-1 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200 text-[11px] font-semibold">Half / Full</button>
                        <button type="button" onclick="applyPreset(['Small','Medium','Large'])" class="px-2.5 py-1 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200 text-[11px] font-semibold">Small / Medium / Large</button>
                        <button type="button" onclick="applyPreset(['Half (4 pcs)','Full (8 pcs)'])" class="px-2.5 py-1 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200 text-[11px] font-semibold">Half (4 pcs) / Full (8 pcs)</button>
                    </div>
                    <div id="variantRows" class="space-y-2"></div>
                    <button type="button" onclick="addVariantRow('', '')" class="w-full py-1.5 rounded-lg border border-dashed border-indigo-300 text-indigo-700 text-xs font-semibold hover:bg-indigo-50">+ Add Another Variant</button>
                </div>
            </div>

            <div class="flex gap-6">
                <label class="inline-flex items-center gap-2 text-sm"><input type="checkbox" name="is_veg" id="itemVeg" checked> Pure Veg</label>
                <label class="inline-flex items-center gap-2 text-sm"><input type="checkbox" name="is_available" id="itemAvail" checked> Available</label>
            </div>
            <div class="pt-3 border-t flex justify-end gap-2">
                <button type="button" onclick="closeModal('itemModal')" class="px-4 py-2 rounded-lg border text-sm">Cancel</button>
                <button type="submit" class="px-5 py-2 rounded-lg bg-indigo-600 text-white text-sm font-semibold">Save Dish</button>
            </div>
        </form>
    </div>
</div>

<script>
function openImportModal() {
    openModal('importModal');
}
function openCatModal(d = null) {
    document.getElementById('catModalTitle').innerText = d ? 'Edit Category' : 'Add Category';
    document.getElementById('catMode').value = d ? 'update' : 'create';
    document.getElementById('catId').value = d ? d.id : '';
    if (d) document.getElementById('catRestro').value = d.restaurant_id;
    document.getElementById('catName').value = d ? d.name : '';
    document.getElementById('catOrder').value = d ? d.display_order : 1;
    openModal('catModal');
}

function setPricingMode(mode) {
    document.getElementById('pricingMode').value = mode;
    const singleBox = document.getElementById('singlePriceBox');
    const variantsBox = document.getElementById('variantsBox');
    const btnSingle = document.getElementById('btnModeSingle');
    const btnVariants = document.getElementById('btnModeVariants');
    const priceInput = document.getElementById('itemPrice');

    if (mode === 'variants') {
        singleBox.classList.add('hidden');
        variantsBox.classList.remove('hidden');
        priceInput.required = false;
        btnVariants.className = 'px-3 py-1 rounded-md bg-indigo-600 text-white';
        btnSingle.className = 'px-3 py-1 rounded-md text-slate-600';
        if (document.getElementById('variantRows').children.length === 0) {
            addVariantRow('Half', '');
            addVariantRow('Full', priceInput.value || '');
        }
    } else {
        singleBox.classList.remove('hidden');
        variantsBox.classList.add('hidden');
        priceInput.required = true;
        btnSingle.className = 'px-3 py-1 rounded-md bg-indigo-600 text-white';
        btnVariants.className = 'px-3 py-1 rounded-md text-slate-600';
    }
}

function addVariantRow(name = '', price = '') {
    const container = document.getElementById('variantRows');
    const row = document.createElement('div');
    row.className = 'grid grid-cols-12 gap-2 items-center';
    row.innerHTML = `
        <input type="text" name="variant_name[]" value="${String(name).replace(/"/g, '&quot;')}" placeholder="e.g. Half / Small" class="col-span-6 px-3 py-1.5 rounded-lg border border-slate-300 text-xs bg-white">
        <input type="number" step="0.01" name="variant_price[]" value="${String(price).replace(/"/g, '&quot;')}" placeholder="Price (₹)" class="col-span-4 px-3 py-1.5 rounded-lg border border-slate-300 text-xs bg-white">
        <button type="button" onclick="this.parentElement.remove()" class="col-span-2 py-1.5 rounded-lg bg-rose-50 text-rose-600 border border-rose-200 text-xs font-bold">✕</button>
    `;
    container.appendChild(row);
}

function applyPreset(names) {
    const existingPrices = Array.from(document.querySelectorAll('input[name="variant_price[]"]')).map(el => el.value);
    const container = document.getElementById('variantRows');
    container.innerHTML = '';
    names.forEach((n, idx) => addVariantRow(n, existingPrices[idx] || ''));
}

function openItemModal(d = null) {
    document.getElementById('itemModalTitle').innerText = d ? 'Edit Menu Item' : 'Add Menu Item';
    document.getElementById('itemMode').value = d ? 'update' : 'create';
    document.getElementById('itemId').value = d ? d.id : '';
    if (d) document.getElementById('itemRestro').value = d.restaurant_id;
    document.getElementById('itemName').value = d ? d.name : '';
    document.getElementById('itemCode').value = d ? (d.code || '') : '';
    document.getElementById('itemCat').value = d ? d.category : 'General';
    document.getElementById('itemPrice').value = d ? d.price : '';
    document.getElementById('itemStock').value = d ? d.current_stock : 100;
    document.getElementById('itemVeg').checked = d ? Number(d.is_veg) === 1 : true;
    document.getElementById('itemAvail').checked = d ? Number(d.is_available) === 1 : true;

    const container = document.getElementById('variantRows');
    container.innerHTML = '';
    let parsedVariants = [];
    if (d && d.variants_json) {
        try {
            parsedVariants = JSON.parse(d.variants_json) || [];
        } catch (e) {
            parsedVariants = [];
        }
    }

    if (Array.isArray(parsedVariants) && parsedVariants.length > 0) {
        parsedVariants.forEach(v => addVariantRow(v.name || '', v.price || ''));
        setPricingMode('variants');
    } else {
        setPricingMode('single');
    }

    openModal('itemModal');
}
</script>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
