<?php
require_once __DIR__ . '/config.php';

if (!empty($_SESSION['super_admin_logged_in'])) {
    header('Location: index.php');
    exit;
}

$error = null;
$lockoutUntil = $_SESSION['admin_lockout_until'] ?? 0;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (time() < $lockoutUntil) {
        $waitSecs = $lockoutUntil - time();
        $error = "Too many failed attempts. Please try again in {$waitSecs} seconds.";
    } else {
        $email = trim($_POST['email'] ?? '');
        $pass  = $_POST['password'] ?? '';
        if (hash_equals(SUPER_ADMIN_EMAIL, $email) && hash_equals(SUPER_ADMIN_PASS, $pass)) {
            unset($_SESSION['admin_failed_attempts'], $_SESSION['admin_lockout_until']);
            session_regenerate_id(true);
            $_SESSION['super_admin_logged_in'] = true;
            $_SESSION['super_admin_email'] = $email;
            header('Location: index.php');
            exit;
        } else {
            $attempts = ($_SESSION['admin_failed_attempts'] ?? 0) + 1;
            $_SESSION['admin_failed_attempts'] = $attempts;
            if ($attempts >= 5) {
                $_SESSION['admin_lockout_until'] = time() + 300;
                $error = 'Too many failed attempts. Account locked for 5 minutes.';
            } else {
                $error = 'Invalid Super Admin email or password.';
            }
        }
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Super Admin Login — AISRestroOS</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-slate-900 min-h-screen flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-2xl overflow-hidden">
        <div class="bg-slate-950 px-8 py-7 text-center border-b border-slate-800">
            <div class="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-orange-600 text-white font-extrabold text-lg mb-3 shadow-lg">
                AIS
            </div>
            <h1 class="text-xl font-bold text-white">AISRestro<span class="text-orange-500">OS</span> Super Admin</h1>
            <p class="text-xs text-slate-400 mt-1">Central Control Panel for Restaurants & Subscriptions</p>
        </div>

        <form method="POST" class="p-8 space-y-5">
            <?php if ($error): ?>
                <div class="p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-semibold">
                    <?= e($error) ?>
                </div>
            <?php endif; ?>

            <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1.5">Super Admin Email</label>
                <input type="email" name="email" required value="<?= e($_POST['email'] ?? '') ?>"
                       placeholder="admin@aisrestro.com"
                       class="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500">
            </div>

            <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1.5">Password</label>
                <input type="password" name="password" required placeholder="••••••••"
                       class="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-orange-500">
            </div>

            <button type="submit"
                    class="w-full py-3 rounded-xl bg-orange-600 hover:bg-orange-700 text-white font-bold text-sm shadow transition">
                Sign In to Super Admin
            </button>
        </form>
    </div>
</body>
</html>
