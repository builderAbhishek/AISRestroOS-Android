<?php
require_once __DIR__ . '/config.php';
requireAdminAuth();
require_once __DIR__ . '/layout.php';

$stats = [
    'restaurants' => 0,
    'active_subs' => 0,
    'menu_items'  => 0,
    'categories'  => 0,
    'total_sales' => 0.0,
    'total_orders'=> 0,
];
$recentRestaurants = [];

if ($pdo) {
    try {
        $stats['restaurants'] = (int)$pdo->query("SELECT COUNT(*) FROM restaurants")->fetchColumn();
        $stats['active_subs'] = (int)$pdo->query("SELECT COUNT(*) FROM subscriptions WHERE UPPER(status) IN ('ACTIVE','TRIAL')")->fetchColumn();
        $stats['menu_items']  = (int)$pdo->query("SELECT COUNT(*) FROM menu_items")->fetchColumn();
        $stats['categories']  = (int)$pdo->query("SELECT COUNT(*) FROM categories")->fetchColumn();
        $ordRow = $pdo->query("SELECT COUNT(*) AS cnt, COALESCE(SUM(grand_total),0) AS rev FROM orders WHERE status = 'COMPLETED' AND COALESCE(is_deleted,0) = 0")->fetch();
        $stats['total_orders'] = (int)($ordRow['cnt'] ?? 0);
        $stats['total_sales']  = (float)($ordRow['rev'] ?? 0);

        $recentRestaurants = $pdo->query(
            "SELECT r.id, r.name, r.email, r.phone, r.is_active, r.created_at,
                    (SELECT p.name FROM subscriptions s JOIN subscription_plans p ON (p.id=s.plan_id OR p.id=s.subscription_plan_id) WHERE s.restaurant_id = r.id ORDER BY COALESCE(s.expires_at,s.ends_at) DESC LIMIT 1) AS plan_name,
                    (SELECT COUNT(*) FROM menu_items m WHERE m.restaurant_id = r.id) AS items_count
             FROM restaurants r
             ORDER BY r.created_at DESC
             LIMIT 8"
        )->fetchAll();
    } catch (Throwable $e) {
        $dbError = $e->getMessage();
    }
}

renderAdminHeader('Super Admin Dashboard', 'dashboard', $dbError);
?>

<!-- KPI Grid -->
<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
    <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Restaurants</div>
        <div class="text-3xl font-extrabold text-slate-900 mt-2"><?= $stats['restaurants'] ?></div>
        <a href="restaurants.php" class="text-xs font-semibold text-orange-600 hover:underline mt-2 inline-block">Manage Restaurants →</a>
    </div>

    <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Subscriptions</div>
        <div class="text-3xl font-extrabold text-emerald-600 mt-2"><?= $stats['active_subs'] ?></div>
        <a href="subscriptions.php" class="text-xs font-semibold text-orange-600 hover:underline mt-2 inline-block">Manage Plans →</a>
    </div>

    <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Menu Items & Categories</div>
        <div class="text-3xl font-extrabold text-slate-900 mt-2"><?= $stats['menu_items'] ?> <span class="text-sm font-medium text-slate-400">/ <?= $stats['categories'] ?> cats</span></div>
        <a href="menu.php" class="text-xs font-semibold text-orange-600 hover:underline mt-2 inline-block">Manage Menus →</a>
    </div>

    <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Platform Revenue</div>
        <div class="text-3xl font-extrabold text-blue-600 mt-2">₹<?= number_format($stats['total_sales'], 0) ?></div>
        <div class="text-xs text-slate-500 mt-2"><?= $stats['total_orders'] ?> completed orders</div>
    </div>
</div>

<!-- Quick Actions -->
<div class="bg-slate-900 rounded-2xl p-6 text-white flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
    <div>
        <h2 class="text-lg font-bold">Quick Onboarding & Management</h2>
        <p class="text-xs text-slate-400 mt-1">Onboard a new restaurant, push menu items, or update subscription plans in one click.</p>
    </div>
    <div class="flex flex-wrap gap-3">
        <a href="restaurants.php?new=1" class="px-4 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-xs font-bold shadow transition">+ Add Restaurant</a>
        <a href="menu.php?new=1" class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold border border-slate-700 transition">+ Add Menu Item</a>
        <a href="categories.php" class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold border border-slate-700 transition">+ Add Category</a>
        <a href="subscriptions.php" class="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold border border-slate-700 transition">Manage Plans</a>
    </div>
</div>

<!-- Recent Restaurants Table -->
<div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
        <h3 class="font-bold text-slate-900">Registered Restaurants</h3>
        <a href="restaurants.php" class="text-xs font-semibold text-orange-600 hover:underline">View All</a>
    </div>
    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                    <th class="px-6 py-3">ID</th>
                    <th class="px-6 py-3">Restaurant Name</th>
                    <th class="px-6 py-3">Owner Contact</th>
                    <th class="px-6 py-3">Plan</th>
                    <th class="px-6 py-3">Menu Items</th>
                    <th class="px-6 py-3">Status</th>
                    <th class="px-6 py-3 text-right">Actions</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php if (empty($recentRestaurants)): ?>
                    <tr><td colspan="7" class="px-6 py-8 text-center text-slate-400">No restaurants registered yet.</td></tr>
                <?php else: foreach ($recentRestaurants as $r): ?>
                    <tr class="hover:bg-slate-50">
                        <td class="px-6 py-3.5 font-mono text-xs font-bold text-slate-700"><?= e($r['id']) ?></td>
                        <td class="px-6 py-3.5 font-semibold text-slate-900"><?= e($r['name']) ?></td>
                        <td class="px-6 py-3.5 text-xs text-slate-600">
                            <div><?= e($r['email']) ?></div>
                            <div class="text-slate-400"><?= e($r['phone']) ?></div>
                        </td>
                        <td class="px-6 py-3.5">
                            <span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700">
                                <?= e($r['plan_name'] ?: 'No Plan') ?>
                            </span>
                        </td>
                        <td class="px-6 py-3.5 text-xs font-bold text-slate-700"><?= (int)$r['items_count'] ?></td>
                        <td class="px-6 py-3.5">
                            <?php if ((int)$r['is_active'] === 1): ?>
                                <span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700">Active</span>
                            <?php else: ?>
                                <span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-red-50 text-red-700">Inactive</span>
                            <?php endif; ?>
                        </td>
                        <td class="px-6 py-3.5 text-right space-x-2">
                            <a href="menu.php?restaurant_id=<?= urlencode($r['id']) ?>" class="text-xs font-semibold text-orange-600 hover:underline">Menu</a>
                            <a href="restaurants.php?edit=<?= urlencode($r['id']) ?>" class="text-xs font-semibold text-slate-600 hover:underline">Edit</a>
                        </td>
                    </tr>
                <?php endforeach; endif; ?>
            </tbody>
        </table>
    </div>
</div>

<?php renderAdminFooter(); ?>
