<?php
require_once __DIR__ . '/config.php';
requireAdminAuth();
require_once __DIR__ . '/layout.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $pdo) {
    $action = $_POST['action'] ?? '';

    if ($action === 'create') {
        $restroName = trim($_POST['restaurant_name'] ?? '');
        $restroId   = trim($_POST['restaurant_id'] ?? '') ?: calculateNextRestaurantId($pdo);
        $ownerName  = trim($_POST['owner_name'] ?? '');
        $email      = trim($_POST['email'] ?? '');
        $phone      = trim($_POST['phone'] ?? '');
        $password   = $_POST['password'] ?? '';
        $planId     = trim($_POST['plan_id'] ?? '');
        $days       = max(1, (int)($_POST['duration_days'] ?? 365));

        if ($restroName === '' || $ownerName === '' || $email === '' || strlen($password) < 6) {
            setFlash('error', 'Please fill Restaurant Name, Owner Name, Email, and Password (min 6 chars).');
            header('Location: restaurants.php?new=1');
            exit;
        }

        try {
            $pdo->beginTransaction();

            $existingUser = $pdo->prepare("SELECT id FROM users WHERE email = ? LIMIT 1");
            $existingUser->execute([$email]);
            $uRow = $existingUser->fetch();

            if ($uRow) {
                $userId = (int)$uRow['id'];
            } else {
                $hash = password_hash($password, PASSWORD_BCRYPT);
                $stmt = $pdo->prepare("INSERT INTO users (name, email, phone, password, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'owner', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
                $stmt->execute([$ownerName, $email, $phone ?: null, $hash]);
                $userId = (int)$pdo->lastInsertId();
            }

            $slug = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $restroName))) . '-' . substr(generateUuidV4(), 0, 5);
            $pdo->prepare("INSERT INTO restaurants (id, name, slug, owner_id, email, phone, currency, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'INR', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                ->execute([$restroId, $restroName, $slug, $userId, $email, $phone ?: null]);

            $branchId = 'BR-' . strtoupper(substr(generateUuidV4(), 0, 8));
            $pdo->prepare("INSERT INTO branches (id, restaurant_id, name, code, is_main, is_main_branch, is_active, phone, created_at, updated_at) VALUES (?, ?, 'Main Branch', 'MAIN-01', 1, 1, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                ->execute([$branchId, $restroId, $phone ?: null]);

            $pdo->prepare("INSERT INTO restaurant_memberships (id, user_id, restaurant_id, branch_id, role, status, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, 'RESTAURANT_OWNER', 'active', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                ->execute([generateUuidV4(), $userId, $restroId, $branchId]);

            $pdo->prepare("INSERT INTO restaurant_settings (restaurant_id, key_name, key_value, bill_prefix, last_bill_number, kot_prefix, last_kot_number, created_at, updated_at) VALUES (?, 'sequence', 'Initialized', 'INV-', 0, 'KOT-', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                ->execute([$restroId]);

            // Default dining tables
            for ($t = 1; $t <= 6; $t++) {
                try {
                    $pdo->prepare("INSERT INTO dining_tables (id, restaurant_id, branch_id, table_number, number, capacity, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 4, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        ->execute([generateUuidV4(), $restroId, $branchId, "T{$t}", "T{$t}"]);
                } catch (Throwable $ignored) {}
            }

            if ($planId !== '') {
                $starts = date('Y-m-d H:i:s');
                $expires = date('Y-m-d H:i:s', strtotime("+{$days} days"));
                try {
                    $pdo->prepare("INSERT INTO subscriptions (restaurant_id, plan_id, subscription_plan_id, status, starts_at, expires_at, ends_at, created_at, updated_at) VALUES (?, ?, ?, 'active', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        ->execute([$restroId, $planId, $planId, $starts, $expires, $expires]);
                } catch (Throwable $ignored) {}
            }

            $pdo->commit();
            setFlash('success', "Restaurant '{$restroName}' ({$restroId}) created successfully!");
        } catch (Throwable $e) {
            if ($pdo->inTransaction()) $pdo->rollBack();
            setFlash('error', 'Failed to create restaurant: ' . $e->getMessage());
        }
        header('Location: restaurants.php');
        exit;
    }

    if ($action === 'update') {
        $id    = trim($_POST['id'] ?? '');
        $name  = trim($_POST['restaurant_name'] ?? '');
        $email = trim($_POST['email'] ?? '');
        $phone = trim($_POST['phone'] ?? '');
        $active = isset($_POST['is_active']) ? 1 : 0;

        try {
            $pdo->prepare("UPDATE restaurants SET name = ?, email = ?, phone = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
                ->execute([$name, $email, $phone, $active, $id]);
            setFlash('success', "Restaurant '{$name}' updated successfully!");
        } catch (Throwable $e) {
            setFlash('error', 'Update failed: ' . $e->getMessage());
        }
        header('Location: restaurants.php');
        exit;
    }

    if ($action === 'toggle_status') {
        $id = trim($_POST['id'] ?? '');
        $newStatus = (int)($_POST['new_status'] ?? 1);
        $pdo->prepare("UPDATE restaurants SET is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")->execute([$newStatus, $id]);
        setFlash('success', 'Restaurant status updated.');
        header('Location: restaurants.php');
        exit;
    }

    if ($action === 'delete') {
        $id = trim($_POST['id'] ?? '');
        try {
            $pdo->prepare("DELETE FROM restaurants WHERE id = ?")->execute([$id]);
            setFlash('success', "Restaurant {$id} deleted.");
        } catch (Throwable $e) {
            setFlash('error', 'Could not delete restaurant (may have linked orders). Try deactivating it instead.');
        }
        header('Location: restaurants.php');
        exit;
    }
}

$search = trim($_GET['q'] ?? '');
$showNew = isset($_GET['new']);
$editId = trim($_GET['edit'] ?? '');
$editRow = null;
$restaurants = [];
$plans = [];
$nextId = $pdo ? calculateNextRestaurantId($pdo) : 'AISR/01';

if ($pdo) {
    $plans = $pdo->query("SELECT id, name, duration_days FROM subscription_plans WHERE is_active = 1")->fetchAll();
    if ($editId !== '') {
        $st = $pdo->prepare("SELECT * FROM restaurants WHERE id = ? LIMIT 1");
        $st->execute([$editId]);
        $editRow = $st->fetch();
    }

    $sql = "SELECT r.*,
                   (SELECT p.name FROM subscriptions s JOIN subscription_plans p ON (p.id=s.plan_id OR p.id=s.subscription_plan_id) WHERE s.restaurant_id = r.id ORDER BY COALESCE(s.expires_at,s.ends_at) DESC LIMIT 1) AS plan_name,
                   (SELECT COUNT(*) FROM menu_items m WHERE m.restaurant_id = r.id) AS menu_count,
                   (SELECT COUNT(*) FROM restaurant_memberships rm WHERE rm.restaurant_id = r.id AND COALESCE(rm.is_active,1)=1) AS staff_count
            FROM restaurants r";
    $params = [];
    if ($search !== '') {
        $sql .= " WHERE r.name LIKE ? OR r.id LIKE ? OR r.email LIKE ? OR r.phone LIKE ?";
        $like = "%{$search}%";
        $params = [$like, $like, $like, $like];
    }
    $sql .= " ORDER BY r.created_at DESC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $restaurants = $stmt->fetchAll();
}

renderAdminHeader('Restaurants CRUD', 'restaurants', $dbError);
?>

<div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
    <form method="GET" class="flex gap-2 flex-1 max-w-md">
        <input type="text" name="q" value="<?= e($search) ?>" placeholder="Search by ID, name, email or phone..."
               class="w-full px-4 py-2 rounded-xl border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500">
        <button type="submit" class="px-4 py-2 rounded-xl bg-slate-900 text-white text-xs font-bold">Search</button>
    </form>
    <a href="restaurants.php?new=1" class="px-4 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold shadow text-center">
        + Add New Restaurant
    </a>
</div>

<?php if ($showNew): ?>
<div class="bg-white rounded-2xl border border-orange-200 p-6 shadow-sm">
    <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-bold text-slate-900">Onboard New Restaurant</h2>
        <a href="restaurants.php" class="text-xs text-slate-500 hover:text-slate-800 font-semibold">✕ Close</a>
    </div>
    <form method="POST" class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <input type="hidden" name="action" value="create">
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Restaurant ID</label>
            <input type="text" name="restaurant_id" value="<?= e($nextId) ?>" required class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm font-mono">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Restaurant Name *</label>
            <input type="text" name="restaurant_name" required placeholder="e.g. Royal Spice Kitchen" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Owner Full Name *</label>
            <input type="text" name="owner_name" required placeholder="Owner Name" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Owner Login Email *</label>
            <input type="email" name="email" required placeholder="owner@restaurant.com" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Phone Number</label>
            <input type="text" name="phone" placeholder="+91 9876543210" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Login Password *</label>
            <input type="password" name="password" required minlength="6" placeholder="Min 6 characters" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Assign Subscription Plan</label>
            <select name="plan_id" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm bg-white">
                <option value="">-- Select Plan --</option>
                <?php foreach ($plans as $pl): ?>
                    <option value="<?= e($pl['id']) ?>"><?= e($pl['name']) ?> (<?= (int)$pl['duration_days'] ?> days)</option>
                <?php endforeach; ?>
            </select>
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Subscription Validity (Days)</label>
            <input type="number" name="duration_days" value="365" min="1" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div class="flex items-end">
            <button type="submit" class="w-full py-2.5 px-4 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-sm font-bold shadow">
                Create Restaurant & Owner
            </button>
        </div>
    </form>
</div>
<?php endif; ?>

<?php if ($editRow): ?>
<div class="bg-white rounded-2xl border border-blue-200 p-6 shadow-sm">
    <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-bold text-slate-900">Edit Restaurant: <?= e($editRow['name']) ?> (<?= e($editRow['id']) ?>)</h2>
        <a href="restaurants.php" class="text-xs text-slate-500 hover:text-slate-800 font-semibold">✕ Cancel</a>
    </div>
    <form method="POST" class="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
        <input type="hidden" name="action" value="update">
        <input type="hidden" name="id" value="<?= e($editRow['id']) ?>">
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Restaurant Name</label>
            <input type="text" name="restaurant_name" value="<?= e($editRow['name']) ?>" required class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Email</label>
            <input type="email" name="email" value="<?= e($editRow['email']) ?>" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Phone</label>
            <input type="text" name="phone" value="<?= e($editRow['phone']) ?>" class="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div class="flex items-center gap-4">
            <label class="inline-flex items-center gap-2 text-xs font-semibold text-slate-700">
                <input type="checkbox" name="is_active" <?= ((int)$editRow['is_active'] === 1) ? 'checked' : '' ?> class="rounded">
                Active
            </label>
            <button type="submit" class="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold">
                Save Changes
            </button>
        </div>
    </form>
</div>
<?php endif; ?>

<div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                    <th class="px-5 py-3">ID</th>
                    <th class="px-5 py-3">Restaurant</th>
                    <th class="px-5 py-3">Contact</th>
                    <th class="px-5 py-3">Plan</th>
                    <th class="px-5 py-3">Menu / Staff</th>
                    <th class="px-5 py-3">Status</th>
                    <th class="px-5 py-3 text-right">Actions</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php if (empty($restaurants)): ?>
                    <tr><td colspan="7" class="px-5 py-8 text-center text-slate-400">No restaurants found.</td></tr>
                <?php else: foreach ($restaurants as $r): ?>
                    <tr class="hover:bg-slate-50">
                        <td class="px-5 py-3.5 font-mono text-xs font-bold text-slate-700"><?= e($r['id']) ?></td>
                        <td class="px-5 py-3.5 font-semibold text-slate-900"><?= e($r['name']) ?></td>
                        <td class="px-5 py-3.5 text-xs text-slate-600">
                            <div><?= e($r['email']) ?></div>
                            <div class="text-slate-400"><?= e($r['phone']) ?></div>
                        </td>
                        <td class="px-5 py-3.5">
                            <span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700">
                                <?= e($r['plan_name'] ?: 'None') ?>
                            </span>
                        </td>
                        <td class="px-5 py-3.5 text-xs text-slate-700">
                            <span class="font-bold"><?= (int)$r['menu_count'] ?></span> items •
                            <span class="font-bold"><?= (int)$r['staff_count'] ?></span> staff
                        </td>
                        <td class="px-5 py-3.5">
                            <form method="POST" class="inline">
                                <input type="hidden" name="action" value="toggle_status">
                                <input type="hidden" name="id" value="<?= e($r['id']) ?>">
                                <input type="hidden" name="new_status" value="<?= ((int)$r['is_active'] === 1) ? 0 : 1 ?>">
                                <button type="submit" class="px-2.5 py-1 rounded-full text-xs font-semibold <?= ((int)$r['is_active'] === 1) ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700' ?>">
                                    <?= ((int)$r['is_active'] === 1) ? 'Active' : 'Inactive' ?>
                                </button>
                            </form>
                        </td>
                        <td class="px-5 py-3.5 text-right whitespace-nowrap space-x-2">
                            <a href="categories.php?restaurant_id=<?= urlencode($r['id']) ?>" class="text-xs font-semibold text-purple-600 hover:underline">Categories</a>
                            <a href="menu.php?restaurant_id=<?= urlencode($r['id']) ?>" class="text-xs font-semibold text-orange-600 hover:underline">Menu</a>
                            <a href="restaurants.php?edit=<?= urlencode($r['id']) ?>" class="text-xs font-semibold text-blue-600 hover:underline">Edit</a>
                            <form method="POST" class="inline" onsubmit="return confirm('Delete restaurant <?= e($r['name']) ?>?');">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="id" value="<?= e($r['id']) ?>">
                                <button type="submit" class="text-xs font-semibold text-red-600 hover:underline">Delete</button>
                            </form>
                        </td>
                    </tr>
                <?php endforeach; endif; ?>
            </tbody>
        </table>
    </div>
</div>

<?php renderAdminFooter(); ?>
