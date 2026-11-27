<?php
require_once __DIR__ . '/config.php';
requireAdminAuth();
require_once __DIR__ . '/layout.php';

$restaurants = $pdo ? getAllRestaurants($pdo) : [];
$selectedRestro = trim($_GET['restaurant_id'] ?? ($_POST['restaurant_id'] ?? ($restaurants[0]['id'] ?? '')));

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $pdo) {
    $action = $_POST['action'] ?? '';
    $restroId = trim($_POST['restaurant_id'] ?? '');

    if ($action === 'create') {
        $name = trim($_POST['name'] ?? '');
        $order = (int)($_POST['display_order'] ?? 0);
        if ($restroId === '' || $name === '') {
            setFlash('error', 'Select restaurant and enter category name.');
        } else {
            try {
                $br = $pdo->prepare("SELECT id FROM branches WHERE restaurant_id = ? LIMIT 1");
                $br->execute([$restroId]);
                $branchId = $br->fetchColumn() ?: null;

                $catId = 'CAT-' . strtoupper(substr(generateUuidV4(), 0, 8));
                $pdo->prepare("INSERT INTO categories (id, restaurant_id, branch_id, name, display_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    ->execute([$catId, $restroId, $branchId, $name, $order]);
                setFlash('success', "Category '{$name}' added to {$restroId}.");
            } catch (Throwable $e) {
                setFlash('error', 'Failed to add category: ' . $e->getMessage());
            }
        }
        header('Location: categories.php?restaurant_id=' . urlencode($restroId));
        exit;
    }

    if ($action === 'update') {
        $id = trim($_POST['id'] ?? '');
        $name = trim($_POST['name'] ?? '');
        $order = (int)($_POST['display_order'] ?? 0);
        $pdo->prepare("UPDATE categories SET name = ?, display_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND restaurant_id = ?")
            ->execute([$name, $order, $id, $restroId]);
        setFlash('success', "Category updated.");
        header('Location: categories.php?restaurant_id=' . urlencode($restroId));
        exit;
    }

    if ($action === 'delete') {
        $id = trim($_POST['id'] ?? '');
        $pdo->prepare("DELETE FROM categories WHERE id = ? AND restaurant_id = ?")->execute([$id, $restroId]);
        setFlash('success', 'Category deleted.');
        header('Location: categories.php?restaurant_id=' . urlencode($restroId));
        exit;
    }
}

$categories = [];
if ($pdo && $selectedRestro !== '') {
    $stmt = $pdo->prepare(
        "SELECT c.*, (SELECT COUNT(*) FROM menu_items m WHERE m.restaurant_id = c.restaurant_id AND m.category = c.name) AS item_count
         FROM categories c WHERE c.restaurant_id = ? ORDER BY c.display_order ASC, c.name ASC"
    );
    $stmt->execute([$selectedRestro]);
    $categories = $stmt->fetchAll();
}

renderAdminHeader('Categories CRUD', 'categories', $dbError);
?>

<div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
    <form method="GET" class="flex items-center gap-3">
        <label class="text-xs font-bold text-slate-600 uppercase">Restaurant:</label>
        <select name="restaurant_id" onchange="this.form.submit()" class="px-4 py-2 rounded-xl border border-slate-200 text-sm font-semibold bg-white">
            <?php foreach ($restaurants as $r): ?>
                <option value="<?= e($r['id']) ?>" <?= ($r['id'] === $selectedRestro) ? 'selected' : '' ?>>
                    <?= e($r['name']) ?> (<?= e($r['id']) ?>)
                </option>
            <?php endforeach; ?>
        </select>
    </form>

    <form method="POST" class="flex flex-wrap items-center gap-2">
        <input type="hidden" name="action" value="create">
        <input type="hidden" name="restaurant_id" value="<?= e($selectedRestro) ?>">
        <input type="text" name="name" required placeholder="New Category Name (e.g. Starters, Dosa)"
               class="px-3.5 py-2 rounded-xl border border-slate-200 text-sm w-64">
        <input type="number" name="display_order" value="0" placeholder="Order"
               class="px-3 py-2 rounded-xl border border-slate-200 text-sm w-20">
        <button type="submit" class="px-4 py-2 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold shadow">
            + Add Category
        </button>
    </form>
</div>

<div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                    <th class="px-6 py-3">Category ID</th>
                    <th class="px-6 py-3">Category Name</th>
                    <th class="px-6 py-3">Display Order</th>
                    <th class="px-6 py-3">Items Linked</th>
                    <th class="px-6 py-3 text-right">Actions</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php if (empty($categories)): ?>
                    <tr><td colspan="5" class="px-6 py-8 text-center text-slate-400">No categories found for this restaurant.</td></tr>
                <?php else: foreach ($categories as $cat): ?>
                    <tr class="hover:bg-slate-50">
                        <td class="px-6 py-3.5 font-mono text-xs text-slate-500"><?= e($cat['id']) ?></td>
                        <td class="px-6 py-3.5">
                            <form method="POST" class="flex items-center gap-2">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="restaurant_id" value="<?= e($selectedRestro) ?>">
                                <input type="hidden" name="id" value="<?= e($cat['id']) ?>">
                                <input type="text" name="name" value="<?= e($cat['name']) ?>" class="px-2.5 py-1.5 rounded-lg border border-slate-200 text-sm font-semibold">
                        </td>
                        <td class="px-6 py-3.5">
                                <input type="number" name="display_order" value="<?= (int)$cat['display_order'] ?>" class="px-2.5 py-1.5 rounded-lg border border-slate-200 text-sm w-20">
                        </td>
                        <td class="px-6 py-3.5 text-xs font-bold text-slate-700"><?= (int)$cat['item_count'] ?> items</td>
                        <td class="px-6 py-3.5 text-right space-x-2">
                                <button type="submit" class="px-3 py-1.5 rounded-lg bg-blue-50 text-blue-700 text-xs font-bold hover:bg-blue-100">Save</button>
                            </form>
                            <form method="POST" class="inline" onsubmit="return confirm('Delete category <?= e($cat['name']) ?>?');">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="restaurant_id" value="<?= e($selectedRestro) ?>">
                                <input type="hidden" name="id" value="<?= e($cat['id']) ?>">
                                <button type="submit" class="px-3 py-1.5 rounded-lg bg-red-50 text-red-700 text-xs font-bold hover:bg-red-100">Delete</button>
                            </form>
                        </td>
                    </tr>
                <?php endforeach; endif; ?>
            </tbody>
        </table>
    </div>
</div>

<?php renderAdminFooter(); ?>
