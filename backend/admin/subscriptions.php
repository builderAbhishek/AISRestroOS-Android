<?php
require_once __DIR__ . '/config.php';
requireAdminAuth();
require_once __DIR__ . '/layout.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $pdo) {
    $action = $_POST['action'] ?? '';

    if ($action === 'save_plan') {
        $id       = trim($_POST['id'] ?? '');
        $name     = trim($_POST['name'] ?? '');
        $price    = (float)($_POST['price'] ?? 0);
        $days     = max(1, (int)($_POST['duration_days'] ?? 30));
        $features = trim($_POST['features'] ?? '');
        $active   = isset($_POST['is_active']) ? 1 : 0;

        if ($name === '') {
            setFlash('error', 'Plan name is required.');
        } else {
            try {
                if ($id !== '') {
                    $pdo->prepare("UPDATE subscription_plans SET name = ?, price = ?, duration_days = ?, features = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
                        ->execute([$name, $price, $days, $features, $active, $id]);
                    setFlash('success', "Plan '{$name}' updated.");
                } else {
                    $newId = 'PLAN-' . strtoupper(substr(generateUuidV4(), 0, 6));
                    $pdo->prepare("INSERT INTO subscription_plans (id, name, price, duration_days, features, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        ->execute([$newId, $name, $price, $days, $features, $active]);
                    setFlash('success', "Plan '{$name}' created.");
                }
            } catch (Throwable $e) {
                setFlash('error', 'Failed to save plan: ' . $e->getMessage());
            }
        }
        header('Location: subscriptions.php');
        exit;
    }

    if ($action === 'delete_plan') {
        $id = trim($_POST['id'] ?? '');
        try {
            $pdo->prepare("DELETE FROM subscription_plans WHERE id = ?")->execute([$id]);
            setFlash('success', 'Plan deleted.');
        } catch (Throwable $e) {
            setFlash('error', 'Cannot delete plan in use.');
        }
        header('Location: subscriptions.php');
        exit;
    }

    if ($action === 'assign_sub') {
        $restroId = trim($_POST['restaurant_id'] ?? '');
        $planId   = trim($_POST['plan_id'] ?? '');
        $days     = max(1, (int)($_POST['duration_days'] ?? 365));
        $status   = trim($_POST['status'] ?? 'active');

        try {
            $starts  = date('Y-m-d H:i:s');
            $expires = date('Y-m-d H:i:s', strtotime("+{$days} days"));
            $pdo->prepare("UPDATE subscriptions SET status = 'expired' WHERE restaurant_id = ? AND UPPER(status) IN ('ACTIVE','TRIAL')")->execute([$restroId]);
            $pdo->prepare("INSERT INTO subscriptions (restaurant_id, plan_id, subscription_plan_id, status, starts_at, expires_at, ends_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                ->execute([$restroId, $planId, $planId, $status, $starts, $expires, $expires]);
            setFlash('success', "Subscription assigned to {$restroId} until " . substr($expires, 0, 10) . ".");
        } catch (Throwable $e) {
            setFlash('error', 'Failed to assign subscription: ' . $e->getMessage());
        }
        header('Location: subscriptions.php');
        exit;
    }

    if ($action === 'update_sub_status') {
        $subId  = (int)($_POST['sub_id'] ?? 0);
        $status = trim($_POST['status'] ?? 'active');
        $pdo->prepare("UPDATE subscriptions SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")->execute([$status, $subId]);
        setFlash('success', 'Subscription status updated.');
        header('Location: subscriptions.php');
        exit;
    }
}

$plans = $pdo ? $pdo->query("SELECT * FROM subscription_plans ORDER BY price ASC")->fetchAll() : [];
$restaurants = $pdo ? getAllRestaurants($pdo) : [];
$subs = $pdo ? $pdo->query(
    "SELECT s.*, r.name AS restaurant_name, p.name AS plan_name, p.price AS plan_price
     FROM subscriptions s
     LEFT JOIN restaurants r ON r.id = s.restaurant_id
     LEFT JOIN subscription_plans p ON (p.id = s.plan_id OR p.id = s.subscription_plan_id)
     ORDER BY s.created_at DESC"
)->fetchAll() : [];

$editPlan = null;
if (!empty($_GET['edit_plan']) && $pdo) {
    $st = $pdo->prepare("SELECT * FROM subscription_plans WHERE id = ? LIMIT 1");
    $st->execute([trim($_GET['edit_plan'])]);
    $editPlan = $st->fetch();
}

renderAdminHeader('Subscriptions & Plans', 'subscriptions', $dbError);
?>

<div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <!-- Assign Subscription to Restaurant -->
    <div class="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
        <h2 class="text-base font-bold text-slate-900 mb-4">Assign / Renew Restaurant Subscription</h2>
        <form method="POST" class="space-y-4">
            <input type="hidden" name="action" value="assign_sub">
            <div>
                <label class="block text-xs font-semibold text-slate-600 mb-1">Restaurant *</label>
                <select name="restaurant_id" required class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm bg-white">
                    <?php foreach ($restaurants as $r): ?>
                        <option value="<?= e($r['id']) ?>"><?= e($r['name']) ?> (<?= e($r['id']) ?>)</option>
                    <?php endforeach; ?>
                </select>
            </div>
            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Plan *</label>
                    <select name="plan_id" required class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm bg-white">
                        <?php foreach ($plans as $pl): ?>
                            <option value="<?= e($pl['id']) ?>"><?= e($pl['name']) ?> (₹<?= number_format((float)$pl['price'], 0) ?>)</option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Duration (Days)</label>
                    <input type="number" name="duration_days" value="365" min="1" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
                </div>
            </div>
            <div>
                <label class="block text-xs font-semibold text-slate-600 mb-1">Status</label>
                <select name="status" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm bg-white">
                    <option value="active">Active</option>
                    <option value="trial">Trial</option>
                    <option value="cancelled">Cancelled</option>
                </select>
            </div>
            <button type="submit" class="w-full py-2.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-sm font-bold shadow">
                Assign / Activate Plan
            </button>
        </form>
    </div>

    <!-- Create / Edit Subscription Plan -->
    <div class="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
        <div class="flex items-center justify-between mb-4">
            <h2 class="text-base font-bold text-slate-900"><?= $editPlan ? 'Edit Plan: ' . e($editPlan['name']) : 'Create New Subscription Plan' ?></h2>
            <?php if ($editPlan): ?>
                <a href="subscriptions.php" class="text-xs text-slate-500 font-semibold">✕ Cancel</a>
            <?php endif; ?>
        </div>
        <form method="POST" class="space-y-4">
            <input type="hidden" name="action" value="save_plan">
            <input type="hidden" name="id" value="<?= e($editPlan['id'] ?? '') ?>">
            <div>
                <label class="block text-xs font-semibold text-slate-600 mb-1">Plan Name *</label>
                <input type="text" name="name" required value="<?= e($editPlan['name'] ?? '') ?>" placeholder="e.g. Growth Plan (5 Staff)"
                       class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
            </div>
            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Price (₹)</label>
                    <input type="number" step="0.01" name="price" value="<?= e($editPlan['price'] ?? '499') ?>"
                           class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
                </div>
                <div>
                    <label class="block text-xs font-semibold text-slate-600 mb-1">Duration (Days)</label>
                    <input type="number" name="duration_days" value="<?= e($editPlan['duration_days'] ?? '30') ?>"
                           class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
                </div>
            </div>
            <div>
                <label class="block text-xs font-semibold text-slate-600 mb-1">Features (comma-separated)</label>
                <input type="text" name="features" value="<?= e($editPlan['features'] ?? 'Unlimited POS Orders, 5 Mobile Logins, Cloud Sync, Thermal Printing') ?>"
                       class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
            </div>
            <label class="inline-flex items-center gap-2 text-xs font-semibold text-slate-700">
                <input type="checkbox" name="is_active" <?= (!isset($editPlan) || (int)$editPlan['is_active'] === 1) ? 'checked' : '' ?> class="rounded">
                Active Plan
            </label>
            <button type="submit" class="w-full py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-sm font-bold shadow">
                <?= $editPlan ? 'Update Plan' : 'Create Plan' ?>
            </button>
        </form>
    </div>
</div>

<!-- Plans List -->
<div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="px-6 py-4 border-b border-slate-200 font-bold text-slate-900">Available Subscription Plans</div>
    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                    <th class="px-5 py-3">Plan</th>
                    <th class="px-5 py-3">Price</th>
                    <th class="px-5 py-3">Duration</th>
                    <th class="px-5 py-3">Features</th>
                    <th class="px-5 py-3 text-right">Actions</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php foreach ($plans as $pl): ?>
                    <tr class="hover:bg-slate-50">
                        <td class="px-5 py-3.5 font-semibold text-slate-900"><?= e($pl['name']) ?></td>
                        <td class="px-5 py-3.5 font-bold text-orange-600">₹<?= number_format((float)$pl['price'], 0) ?></td>
                        <td class="px-5 py-3.5 text-xs text-slate-600"><?= (int)$pl['duration_days'] ?> days</td>
                        <td class="px-5 py-3.5 text-xs text-slate-500 max-w-md truncate"><?= e($pl['features']) ?></td>
                        <td class="px-5 py-3.5 text-right space-x-2">
                            <a href="subscriptions.php?edit_plan=<?= urlencode($pl['id']) ?>" class="text-xs font-semibold text-blue-600 hover:underline">Edit</a>
                            <form method="POST" class="inline" onsubmit="return confirm('Delete plan <?= e($pl['name']) ?>?');">
                                <input type="hidden" name="action" value="delete_plan">
                                <input type="hidden" name="id" value="<?= e($pl['id']) ?>">
                                <button type="submit" class="text-xs font-semibold text-red-600 hover:underline">Delete</button>
                            </form>
                        </td>
                    </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
</div>

<!-- Restaurant Subscriptions Table -->
<div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="px-6 py-4 border-b border-slate-200 font-bold text-slate-900">Restaurant Subscriptions History</div>
    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                    <th class="px-5 py-3">Restaurant</th>
                    <th class="px-5 py-3">Plan</th>
                    <th class="px-5 py-3">Starts</th>
                    <th class="px-5 py-3">Expires</th>
                    <th class="px-5 py-3">Status</th>
                    <th class="px-5 py-3 text-right">Quick Action</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php foreach ($subs as $s): ?>
                    <tr class="hover:bg-slate-50">
                        <td class="px-5 py-3.5 font-semibold text-slate-900"><?= e($s['restaurant_name'] ?: $s['restaurant_id']) ?></td>
                        <td class="px-5 py-3.5 text-xs font-bold text-blue-700"><?= e($s['plan_name'] ?: $s['plan_id']) ?></td>
                        <td class="px-5 py-3.5 text-xs text-slate-500"><?= e(substr((string)$s['starts_at'], 0, 10)) ?></td>
                        <td class="px-5 py-3.5 text-xs font-semibold text-slate-700"><?= e(substr((string)($s['expires_at'] ?: $s['ends_at']), 0, 10)) ?></td>
                        <td class="px-5 py-3.5">
                            <span class="px-2.5 py-1 rounded-full text-xs font-semibold <?= strtolower($s['status']) === 'active' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600' ?>">
                                <?= e(strtoupper($s['status'])) ?>
                            </span>
                        </td>
                        <td class="px-5 py-3.5 text-right">
                            <form method="POST" class="inline">
                                <input type="hidden" name="action" value="update_sub_status">
                                <input type="hidden" name="sub_id" value="<?= (int)$s['id'] ?>">
                                <input type="hidden" name="status" value="<?= strtolower($s['status']) === 'active' ? 'cancelled' : 'active' ?>">
                                <button type="submit" class="text-xs font-semibold text-orange-600 hover:underline">
                                    <?= strtolower($s['status']) === 'active' ? 'Deactivate' : 'Activate' ?>
                                </button>
                            </form>
                        </td>
                    </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
</div>

<?php renderAdminFooter(); ?>
