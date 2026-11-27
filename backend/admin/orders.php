<?php
require_once __DIR__ . '/config.php';
requireAdminAuth();
require_once __DIR__ . '/layout.php';

$restaurants = $pdo ? getAllRestaurants($pdo) : [];
$selectedRestro = trim($_GET['restaurant_id'] ?? ($restaurants[0]['id'] ?? ''));
$showDeleted = isset($_GET['show_deleted']) ? (int)$_GET['show_deleted'] : 1;

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $pdo) {
    $action = $_POST['action'] ?? '';
    $uuid   = trim($_POST['uuid'] ?? '');
    $rId    = trim($_POST['restaurant_id'] ?? '');
    if ($action === 'restore' && $uuid !== '') {
        $pdo->prepare("UPDATE orders SET is_deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE uuid = ? AND restaurant_id = ?")
            ->execute([$uuid, $rId]);
        setFlash('success', "Order {$uuid} restored to active reports.");
    } elseif ($action === 'soft_delete' && $uuid !== '') {
        $pdo->prepare("UPDATE orders SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE uuid = ? AND restaurant_id = ?")
            ->execute([$uuid, $rId]);
        setFlash('success', "Order {$uuid} marked as deleted (is_deleted = 1).");
    }
    header('Location: orders.php?restaurant_id=' . urlencode($rId));
    exit;
}

$orders = [];
if ($pdo && $selectedRestro !== '') {
    $sql = "SELECT uuid, order_number, invoice_number, order_type, status, table_number, grand_total, payment_method, COALESCE(is_deleted, 0) AS is_deleted, created_at
            FROM orders WHERE restaurant_id = ?";
    if ($showDeleted === 0) {
        $sql .= " AND COALESCE(is_deleted, 0) = 0";
    }
    $sql .= " ORDER BY created_at DESC LIMIT 100";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$selectedRestro]);
    $orders = $stmt->fetchAll();
}

renderAdminHeader('Platform Orders & Soft-Delete Audit', 'orders', $dbError);
?>

<div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-wrap items-center justify-between gap-4">
    <form method="GET" class="flex flex-wrap items-center gap-3">
        <label class="text-xs font-bold text-slate-600 uppercase">Restaurant:</label>
        <select name="restaurant_id" onchange="this.form.submit()" class="px-4 py-2 rounded-xl border border-slate-200 text-sm font-semibold bg-white">
            <?php foreach ($restaurants as $r): ?>
                <option value="<?= e($r['id']) ?>" <?= ($r['id'] === $selectedRestro) ? 'selected' : '' ?>>
                    <?= e($r['name']) ?> (<?= e($r['id']) ?>)
                </option>
            <?php endforeach; ?>
        </select>

        <select name="show_deleted" onchange="this.form.submit()" class="px-3.5 py-2 rounded-xl border border-slate-200 text-sm bg-white">
            <option value="1" <?= $showDeleted === 1 ? 'selected' : '' ?>>All (Including Soft-Deleted)</option>
            <option value="0" <?= $showDeleted === 0 ? 'selected' : '' ?>>Active Only (is_deleted = 0)</option>
        </select>
    </form>
    <div class="text-xs text-slate-500">Showing latest <?= count($orders) ?> orders</div>
</div>

<div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                    <th class="px-5 py-3">Order / Invoice</th>
                    <th class="px-5 py-3">Type / Table</th>
                    <th class="px-5 py-3">Amount</th>
                    <th class="px-5 py-3">Status</th>
                    <th class="px-5 py-3">Visibility</th>
                    <th class="px-5 py-3">Created</th>
                    <th class="px-5 py-3 text-right">Action</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php if (empty($orders)): ?>
                    <tr><td colspan="7" class="px-5 py-8 text-center text-slate-400">No orders found.</td></tr>
                <?php else: foreach ($orders as $o): $isDel = (int)$o['is_deleted'] === 1; ?>
                    <tr class="hover:bg-slate-50 <?= $isDel ? 'bg-red-50/40' : '' ?>">
                        <td class="px-5 py-3.5 font-mono text-xs font-bold text-slate-800">
                            <?= e($o['invoice_number'] ?: $o['order_number']) ?>
                            <div class="text-[11px] text-slate-400 font-normal"><?= e($o['uuid']) ?></div>
                        </td>
                        <td class="px-5 py-3.5 text-xs text-slate-700">
                            <?= e($o['order_type']) ?> <?= $o['table_number'] ? '• Table ' . e($o['table_number']) : '' ?>
                        </td>
                        <td class="px-5 py-3.5 font-bold text-slate-900">₹<?= number_format((float)$o['grand_total'], 2) ?></td>
                        <td class="px-5 py-3.5 text-xs font-semibold"><?= e($o['status']) ?></td>
                        <td class="px-5 py-3.5">
                            <?php if ($isDel): ?>
                                <span class="px-2.5 py-1 rounded-full text-xs font-bold bg-red-100 text-red-700">Deleted (1)</span>
                            <?php else: ?>
                                <span class="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700">Active (0)</span>
                            <?php endif; ?>
                        </td>
                        <td class="px-5 py-3.5 text-xs text-slate-500"><?= e($o['created_at']) ?></td>
                        <td class="px-5 py-3.5 text-right">
                            <form method="POST" class="inline">
                                <input type="hidden" name="restaurant_id" value="<?= e($selectedRestro) ?>">
                                <input type="hidden" name="uuid" value="<?= e($o['uuid']) ?>">
                                <?php if ($isDel): ?>
                                    <input type="hidden" name="action" value="restore">
                                    <button type="submit" class="text-xs font-bold text-emerald-600 hover:underline">Restore</button>
                                <?php else: ?>
                                    <input type="hidden" name="action" value="soft_delete">
                                    <button type="submit" class="text-xs font-bold text-red-600 hover:underline">Soft Delete</button>
                                <?php endif; ?>
                            </form>
                        </td>
                    </tr>
                <?php endforeach; endif; ?>
            </tbody>
        </table>
    </div>
</div>

<?php renderAdminFooter(); ?>
