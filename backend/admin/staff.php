<?php
require_once __DIR__ . '/config.php';
requireAdminAuth();
require_once __DIR__ . '/layout.php';

$restaurants = $pdo ? getAllRestaurants($pdo) : [];
$selectedRestro = trim($_GET['restaurant_id'] ?? ($_POST['restaurant_id'] ?? ($restaurants[0]['id'] ?? '')));

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $pdo) {
    $action = $_POST['action'] ?? '';
    $restroId = trim($_POST['restaurant_id'] ?? '');

    if ($action === 'add_staff') {
        $name  = trim($_POST['name'] ?? '');
        $email = trim($_POST['email'] ?? '');
        $phone = trim($_POST['phone'] ?? '');
        $role  = strtoupper(trim($_POST['role'] ?? 'CASHIER'));
        $pass  = $_POST['password'] ?? '';

        if ($restroId === '' || $name === '' || $email === '' || strlen($pass) < 6) {
            setFlash('error', 'Name, email, and password (min 6 chars) are required.');
        } else {
            try {
                $pdo->beginTransaction();
                $uChk = $pdo->prepare("SELECT id FROM users WHERE email = ? LIMIT 1");
                $uChk->execute([$email]);
                $userId = $uChk->fetchColumn();
                if (!$userId) {
                    $hash = password_hash($pass, PASSWORD_BCRYPT);
                    $pdo->prepare("INSERT INTO users (name, email, phone, password, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                        ->execute([$name, $email, $phone ?: null, $hash, strtolower($role)]);
                    $userId = (int)$pdo->lastInsertId();
                }

                $br = $pdo->prepare("SELECT id FROM branches WHERE restaurant_id = ? LIMIT 1");
                $br->execute([$restroId]);
                $branchId = $br->fetchColumn() ?: null;

                $pdo->prepare("INSERT INTO restaurant_memberships (id, user_id, restaurant_id, branch_id, role, status, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'active', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    ->execute([generateUuidV4(), $userId, $restroId, $branchId, $role]);
                $pdo->commit();
                setFlash('success', "Staff '{$name}' added to {$restroId}.");
            } catch (Throwable $e) {
                if ($pdo->inTransaction()) $pdo->rollBack();
                setFlash('error', 'Failed to add staff: ' . $e->getMessage());
            }
        }
        header('Location: staff.php?restaurant_id=' . urlencode($restroId));
        exit;
    }

    if ($action === 'reset_password') {
        $userId = (int)($_POST['user_id'] ?? 0);
        $newPass = $_POST['new_password'] ?? '';
        if (strlen($newPass) < 6) {
            setFlash('error', 'Password must be at least 6 characters.');
        } else {
            $hash = password_hash($newPass, PASSWORD_BCRYPT);
            $pdo->prepare("UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")->execute([$hash, $userId]);
            setFlash('success', 'User password updated.');
        }
        header('Location: staff.php?restaurant_id=' . urlencode($restroId));
        exit;
    }

    if ($action === 'revoke') {
        $memId = trim($_POST['membership_id'] ?? '');
        $pdo->prepare("UPDATE restaurant_memberships SET is_active = 0, status = 'inactive' WHERE id = ? AND restaurant_id = ? AND role != 'RESTAURANT_OWNER'")
            ->execute([$memId, $restroId]);
        setFlash('success', 'Staff access revoked.');
        header('Location: staff.php?restaurant_id=' . urlencode($restroId));
        exit;
    }
}

$members = [];
if ($pdo && $selectedRestro !== '') {
    $stmt = $pdo->prepare(
        "SELECT rm.id AS membership_id, rm.role, rm.status, rm.created_at,
                u.id AS user_id, u.name, u.email, u.phone
         FROM restaurant_memberships rm
         JOIN users u ON u.id = rm.user_id
         WHERE rm.restaurant_id = ? AND COALESCE(rm.is_active, 1) = 1
         ORDER BY CASE WHEN rm.role = 'RESTAURANT_OWNER' THEN 0 ELSE 1 END, rm.created_at ASC"
    );
    $stmt->execute([$selectedRestro]);
    $members = $stmt->fetchAll();
}

renderAdminHeader('Staff & Users Access', 'staff', $dbError);
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
    <div class="text-xs font-semibold text-slate-500">
        Active logins in this restaurant: <span class="text-slate-900 font-bold"><?= count($members) ?></span>
    </div>
</div>

<!-- Add Staff Form -->
<div class="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
    <h2 class="text-sm font-bold text-slate-900 mb-3">Create Staff / Mobile Login for <?= e($selectedRestro) ?></h2>
    <form method="POST" class="grid grid-cols-1 md:grid-cols-6 gap-3 items-end">
        <input type="hidden" name="action" value="add_staff">
        <input type="hidden" name="restaurant_id" value="<?= e($selectedRestro) ?>">
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Name *</label>
            <input type="text" name="name" required placeholder="Staff Name" class="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Email *</label>
            <input type="email" name="email" required placeholder="staff@mail.com" class="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Phone</label>
            <input type="text" name="phone" placeholder="Mobile" class="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Role</label>
            <select name="role" class="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm bg-white">
                <option value="CASHIER">CASHIER</option>
                <option value="MANAGER">MANAGER</option>
                <option value="WAITER">WAITER</option>
                <option value="CHEF">CHEF</option>
            </select>
        </div>
        <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1">Password *</label>
            <input type="password" name="password" required minlength="6" placeholder="Min 6 chars" class="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm">
        </div>
        <button type="submit" class="py-2.5 px-4 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold shadow">
            + Add Staff
        </button>
    </form>
</div>

<!-- Staff List -->
<div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="overflow-x-auto">
        <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                    <th class="px-5 py-3">Name</th>
                    <th class="px-5 py-3">Email & Phone</th>
                    <th class="px-5 py-3">Role</th>
                    <th class="px-5 py-3">Reset Password</th>
                    <th class="px-5 py-3 text-right">Action</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
                <?php if (empty($members)): ?>
                    <tr><td colspan="5" class="px-5 py-8 text-center text-slate-400">No users found for this restaurant.</td></tr>
                <?php else: foreach ($members as $m): ?>
                    <tr class="hover:bg-slate-50">
                        <td class="px-5 py-3.5 font-semibold text-slate-900"><?= e($m['name']) ?></td>
                        <td class="px-5 py-3.5 text-xs text-slate-600">
                            <div><?= e($m['email']) ?></div>
                            <div class="text-slate-400"><?= e($m['phone']) ?></div>
                        </td>
                        <td class="px-5 py-3.5">
                            <span class="px-2.5 py-1 rounded-full text-xs font-bold <?= $m['role'] === 'RESTAURANT_OWNER' ? 'bg-orange-50 text-orange-700' : 'bg-blue-50 text-blue-700' ?>">
                                <?= e($m['role']) ?>
                            </span>
                        </td>
                        <td class="px-5 py-3.5">
                            <form method="POST" class="flex items-center gap-1.5">
                                <input type="hidden" name="action" value="reset_password">
                                <input type="hidden" name="restaurant_id" value="<?= e($selectedRestro) ?>">
                                <input type="hidden" name="user_id" value="<?= (int)$m['user_id'] ?>">
                                <input type="password" name="new_password" minlength="6" required placeholder="New password" class="px-2.5 py-1 rounded-lg border border-slate-200 text-xs w-32">
                                <button type="submit" class="px-2.5 py-1 rounded-lg bg-slate-800 text-white text-xs font-semibold">Reset</button>
                            </form>
                        </td>
                        <td class="px-5 py-3.5 text-right">
                            <?php if ($m['role'] !== 'RESTAURANT_OWNER'): ?>
                                <form method="POST" class="inline" onsubmit="return confirm('Revoke access for <?= e($m['name']) ?>?');">
                                    <input type="hidden" name="action" value="revoke">
                                    <input type="hidden" name="restaurant_id" value="<?= e($selectedRestro) ?>">
                                    <input type="hidden" name="membership_id" value="<?= e($m['membership_id']) ?>">
                                    <button type="submit" class="text-xs font-semibold text-red-600 hover:underline">Revoke</button>
                                </form>
                            <?php else: ?>
                                <span class="text-xs text-slate-400">Owner</span>
                            <?php endif; ?>
                        </td>
                    </tr>
                <?php endforeach; endif; ?>
            </tbody>
        </table>
    </div>
</div>

<?php renderAdminFooter(); ?>
