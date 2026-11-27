<?php
/**
 * AISRestroOS Super Admin Panel - Tailwind CSS Layout Shell
 */

function renderAdminHeader(string $pageTitle, string $activeNav, ?string $dbError = null): void
{
    $flash = getFlash();
    $navItems = [
        ['key' => 'dashboard',     'label' => 'Dashboard',          'href' => 'index.php',         'icon' => 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6'],
        ['key' => 'restaurants',   'label' => 'Restaurants',        'href' => 'restaurants.php',   'icon' => 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4'],
        ['key' => 'categories',    'label' => 'Categories',         'href' => 'categories.php',    'icon' => 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z'],
        ['key' => 'menu',          'label' => 'Menu Items',         'href' => 'menu.php',          'icon' => 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253'],
        ['key' => 'subscriptions', 'label' => 'Plans & Subscriptions','href' => 'subscriptions.php', 'icon' => 'M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z'],
        ['key' => 'staff',         'label' => 'Staff & Users',      'href' => 'staff.php',         'icon' => 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z'],
        ['key' => 'orders',        'label' => 'Platform Orders',    'href' => 'orders.php',        'icon' => 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2'],
    ];
    ?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= e($pageTitle) ?> — AISRestroOS Super Admin</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        navy: '#0F172A',
                        slateDark: '#1E293B',
                        saffron: '#EA580C',
                    }
                }
            }
        }
    </script>
</head>
<body class="bg-slate-50 text-slate-900 min-h-screen flex flex-col lg:flex-row">

    <!-- Sidebar -->
    <aside id="adminSidebar" class="bg-navy text-white w-full lg:w-64 lg:min-h-screen flex-shrink-0 flex flex-col justify-between border-r border-slate-800">
        <div>
            <div class="px-5 py-5 border-b border-slate-800 flex items-center justify-between">
                <div class="flex items-center gap-3">
                    <div class="w-9 h-9 rounded-xl bg-saffron flex items-center justify-center font-extrabold text-white text-sm shadow">
                        AIS
                    </div>
                    <div>
                        <div class="font-bold text-base tracking-tight">AISRestro<span class="text-orange-400">OS</span></div>
                        <div class="text-[11px] text-slate-400">Super Admin Panel</div>
                    </div>
                </div>
                <button onclick="document.getElementById('mobileNav').classList.toggle('hidden')" class="lg:hidden p-1.5 rounded-lg bg-slate-800 text-slate-300">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/></svg>
                </button>
            </div>

            <nav id="mobileNav" class="hidden lg:block px-3 py-4 space-y-1">
                <?php foreach ($navItems as $item):
                    $isActive = ($activeNav === $item['key']);
                    $cls = $isActive
                        ? 'bg-saffron text-white font-semibold shadow-sm'
                        : 'text-slate-300 hover:bg-slate-800 hover:text-white';
                ?>
                    <a href="<?= e($item['href']) ?>" class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm transition <?= $cls ?>">
                        <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="<?= e($item['icon']) ?>"/>
                        </svg>
                        <span><?= e($item['label']) ?></span>
                    </a>
                <?php endforeach; ?>
            </nav>
        </div>

        <div class="hidden lg:block p-4 border-t border-slate-800">
            <a href="logout.php" class="flex items-center justify-center gap-2 w-full py-2.5 px-3 rounded-xl bg-slate-800 hover:bg-red-600/90 text-slate-200 hover:text-white text-xs font-semibold transition">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>
                Logout Super Admin
            </a>
        </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col min-w-0">
        <!-- Top Bar -->
        <header class="bg-white border-b border-slate-200 px-6 py-4 flex items-center justify-between sticky top-0 z-20">
            <div>
                <h1 class="text-xl font-bold text-slate-900"><?= e($pageTitle) ?></h1>
                <p class="text-xs text-slate-500">Manage restaurants, menus, categories, subscriptions & platform health</p>
            </div>
            <div class="flex items-center gap-3">
                <span class="hidden sm:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                    DB Connected
                </span>
                <a href="logout.php" class="lg:hidden px-3 py-1.5 rounded-lg bg-slate-100 text-slate-700 text-xs font-semibold">Logout</a>
            </div>
        </header>

        <div class="p-6 flex-1 space-y-6 max-w-7xl w-full mx-auto">
            <?php if ($dbError): ?>
                <div class="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm font-medium">
                    Database Connection Error: <?= e($dbError) ?>
                </div>
            <?php endif; ?>

            <?php if ($flash):
                $isOk = ($flash['type'] ?? '') === 'success';
                $boxCls = $isOk ? 'bg-emerald-50 border-emerald-200 text-emerald-800' : 'bg-red-50 border-red-200 text-red-800';
            ?>
                <div class="p-4 rounded-xl border text-sm font-medium flex items-center justify-between <?= $boxCls ?>">
                    <span><?= e($flash['message'] ?? '') ?></span>
                    <button onclick="this.parentElement.remove()" class="text-xs opacity-60 hover:opacity-100 font-bold ml-4">✕</button>
                </div>
            <?php endif; ?>
<?php
}

function renderAdminFooter(): void
{
    ?>
        </div>
        <footer class="px-6 py-4 border-t border-slate-200 bg-white text-xs text-slate-400 flex items-center justify-between">
            <span>AISRestroOS Super Admin • Built with Tailwind CSS & PHP PDO</span>
            <span>v1.1</span>
        </footer>
    </main>
</body>
</html>
<?php
}
