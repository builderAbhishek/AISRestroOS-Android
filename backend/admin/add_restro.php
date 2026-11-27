<?php
/**
 * ============================================================================
 * AISRestroOS - Quick Restaurant Onboarding Tool (add_restro.php)
 * ============================================================================
 * Path on Server: /admin/add_restro.php  (https://restro.trakiyo.in/admin/add_restro.php)
 * 
 * Purpose:
 *   Allows administrators to easily create and initialize the 1st Restaurant
 *   (or subsequent restaurants: AISR/01, AISR/02, etc.) after database wipes.
 * 
 * Auto-creates:
 *   1. Owner Account in `users` (Role: owner, Bcrypt password)
 *   2. Restaurant Record in `restaurants` (ID: AISR/01)
 *   3. Main Branch in `branches` (MAIN-01, is_main = 1)
 *   4. Membership in `restaurant_memberships` (RESTAURANT_OWNER, active)
 *   5. Sequence Counters in `restaurant_settings` (INV-01, KOT-01 initialized to 0)
 *   6. Active Subscription in `subscriptions` (365 days active)
 * ============================================================================
 */

error_reporting(0);
ini_set('display_errors', '0');

// ----------------------------------------------------------------------------
// 1. DATABASE CONNECTION RESOLUTION (PHP 8.5 Safe Parser)
// ----------------------------------------------------------------------------
$dbHost = 'localhost';
$dbPort = '3306';
$dbName = 'u328317103_restro';
$dbUser = 'u328317103_restrouser';
$dbPass = ''; // Auto-loaded from config if present

// Search for database config in common locations without direct require to prevent PHP 8.5 notices
$configPaths = [
    __DIR__ . '/../api/config/database.php',
    __DIR__ . '/../config/database.php',
    __DIR__ . '/../backend/config/database.php',
    __DIR__ . '/config/database.php',
];

foreach ($configPaths as $path) {
    if (file_exists($path)) {
        $content = file_get_contents($path);
        if ($content !== false) {
            if (preg_match("/'host'\s*=>\s*['\"]([^'\"]+)['\"]/i", $content, $m))     $dbHost = $m[1];
            if (preg_match("/'port'\s*=>\s*(\d+)/i", $content, $m))                   $dbPort = $m[1];
            if (preg_match("/'database'\s*=>\s*['\"]([^'\"]+)['\"]/i", $content, $m)) $dbName = $m[1];
            if (preg_match("/'username'\s*=>\s*['\"]([^'\"]+)['\"]/i", $content, $m)) $dbUser = $m[1];
            if (preg_match("/'password'\s*=>\s*['\"]([^'\"]*)['\"]/i", $content, $m)) $dbPass = $m[1];
            break;
        }
    }
}

// Allow overriding via POST if connection fails
if (isset($_POST['override_db_pass']) && $_POST['override_db_pass'] !== '') {
    $dbPass = $_POST['override_db_pass'];
}

$pdo = null;
$dbError = null;

try {
    $dsn = "mysql:host={$dbHost};port={$dbPort};dbname={$dbName};charset=utf8mb4";
    $pdo = new PDO($dsn, $dbUser, $dbPass, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]);
    $pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
} catch (PDOException $e) {
    $dbError = "Database Connection Failed: " . htmlspecialchars($e->getMessage());
}

// ----------------------------------------------------------------------------
// 2. HELPER FUNCTIONS
// ----------------------------------------------------------------------------
function generateUuidV4(): string {
    $data = random_bytes(16);
    $data[6] = chr(ord($data[6]) & 0x0f | 0x40); // version 4
    $data[8] = chr(ord($data[8]) & 0x3f | 0x80); // variant RFC 4122
    return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
}

function calculateNextRestaurantId(PDO $pdo): string {
    try {
        $stmt = $pdo->query("SELECT id FROM restaurants");
        $rows = $stmt->fetchAll();
        $maxNum = 0;
        foreach ($rows as $row) {
            $id = (string)$row['id'];
            if (preg_match('/^AISR\/(\d+)$/i', $id, $matches)) {
                $num = (int)$matches[1];
                if ($num > $maxNum) {
                    $maxNum = $num;
                }
            }
        }
        $next = $maxNum + 1;
        return sprintf('AISR/%02d', $next);
    } catch (Exception $e) {
        return 'AISR/01';
    }
}

$message = null;
$messageType = null;
$createdData = null;

// ----------------------------------------------------------------------------
// 3. PROCESS FORM SUBMISSION
// ----------------------------------------------------------------------------
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'add_restaurant') {
    if (!$pdo) {
        $message = "Cannot proceed: Database is not connected.";
        $messageType = "danger";
    } else {
        $restroName  = trim($_POST['restaurant_name'] ?? '');
        $restroId    = trim($_POST['restaurant_id'] ?? '');
        $phone       = trim($_POST['phone'] ?? '');
        $email       = trim($_POST['email'] ?? '');
        $address     = trim($_POST['address'] ?? '');
        $ownerName   = trim($_POST['owner_name'] ?? '');
        $loginPass   = trim($_POST['password'] ?? '');
        $billPrefix  = trim($_POST['bill_prefix'] ?? 'INV-');
        $kotPrefix   = trim($_POST['kot_prefix'] ?? 'KOT-');
        $taxPercent  = floatval($_POST['tax_percentage'] ?? 0.00);

        if (empty($restroName) || empty($restroId) || empty($ownerName) || empty($email) || empty($loginPass)) {
            $message = "Please fill in all required fields (Restaurant Name, Restaurant ID, Owner Name, Email, Password).";
            $messageType = "danger";
        } elseif (strlen($loginPass) < 6) {
            $message = "Password must be at least 6 characters long.";
            $messageType = "danger";
        } else {
            try {
                $pdo->beginTransaction();

                // 1. Check if Restaurant ID already exists
                $chkRestro = $pdo->prepare("SELECT id FROM restaurants WHERE id = :id LIMIT 1");
                $chkRestro->execute(['id' => $restroId]);
                if ($chkRestro->fetch()) {
                    throw new Exception("Restaurant ID '{$restroId}' is already registered in database! Please choose another ID.");
                }

                // 2. Check or create User
                $chkUser = $pdo->prepare("SELECT id FROM users WHERE email = :email LIMIT 1");
                $chkUser->execute(['email' => $email]);
                $existingUser = $chkUser->fetch();

                if ($existingUser) {
                    $userId = (int)$existingUser['id'];
                    // Update password and phone if existing user
                    $updateUser = $pdo->prepare("UPDATE users SET name = :name, phone = :phone, password = :pass, updated_at = NOW() WHERE id = :id");
                    $updateUser->execute([
                        'name'  => $ownerName,
                        'phone' => $phone ?: null,
                        'pass'  => password_hash($loginPass, PASSWORD_BCRYPT),
                        'id'    => $userId,
                    ]);
                } else {
                    $insertUser = $pdo->prepare(
                        "INSERT INTO users (name, email, phone, password, role, created_at, updated_at) 
                         VALUES (:name, :email, :phone, :password, 'owner', NOW(), NOW())"
                    );
                    $insertUser->execute([
                        'name'     => $ownerName,
                        'email'    => $email,
                        'phone'    => $phone ?: null,
                        'password' => password_hash($loginPass, PASSWORD_BCRYPT),
                    ]);
                    $userId = (int)$pdo->lastInsertId();
                }

                // 3. Create Restaurant
                $slug = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $restroName))) . '-' . substr(bin2hex(random_bytes(3)), 0, 5);
                $insertRestro = $pdo->prepare(
                    "INSERT INTO restaurants (id, name, slug, owner_id, email, phone, address, currency, is_active, created_at, updated_at) 
                     VALUES (:id, :name, :slug, :owner_id, :email, :phone, :address, 'INR', 1, NOW(), NOW())"
                );
                $insertRestro->execute([
                    'id'       => $restroId,
                    'name'     => $restroName,
                    'slug'     => $slug,
                    'owner_id' => $userId,
                    'email'    => $email,
                    'phone'    => $phone ?: null,
                    'address'  => $address ?: null,
                ]);

                // 4. Create Main Branch
                $branchId = 'BR-' . strtoupper(substr(bin2hex(random_bytes(4)), 0, 8));
                $insertBranch = $pdo->prepare(
                    "INSERT INTO branches (id, restaurant_id, name, code, address, phone, is_main, is_main_branch, is_active, created_at, updated_at) 
                     VALUES (:id, :restro, 'Main Branch', 'MAIN-01', :address, :phone, 1, 1, 1, NOW(), NOW())"
                );
                $insertBranch->execute([
                    'id'      => $branchId,
                    'restro'  => $restroId,
                    'address' => $address ?: null,
                    'phone'   => $phone ?: null,
                ]);

                // 5. Create Restaurant Membership
                $membershipId = generateUuidV4();
                $insertMem = $pdo->prepare(
                    "INSERT INTO restaurant_memberships (id, user_id, restaurant_id, branch_id, role, status, is_active, created_at, updated_at) 
                     VALUES (:id, :uid, :restro, :branch, 'RESTAURANT_OWNER', 'active', 1, NOW(), NOW())"
                );
                $insertMem->execute([
                    'id'     => $membershipId,
                    'uid'    => $userId,
                    'restro' => $restroId,
                    'branch' => $branchId,
                ]);

                // 6. Initialize Sequence Settings (INV-01, KOT-01 starts cleanly at 0)
                $insertSettings = $pdo->prepare(
                    "INSERT INTO restaurant_settings (restaurant_id, branch_id, key_name, key_value, bill_prefix, last_bill_number, kot_prefix, last_kot_number, tax_percentage, service_charge_percentage, created_at, updated_at) 
                     VALUES (:restro, NULL, 'sequence', 'Sequence counters initialized', :bill_prefix, 0, :kot_prefix, 0, :tax, 0.00, NOW(), NOW())"
                );
                $insertSettings->execute([
                    'restro'      => $restroId,
                    'bill_prefix' => $billPrefix ?: 'INV-',
                    'kot_prefix'  => $kotPrefix ?: 'KOT-',
                    'tax'         => $taxPercent,
                ]);

                // 7. Initialize Subscription (365 Days Active)
                try {
                    $insertSub = $pdo->prepare(
                        "INSERT INTO subscriptions (restaurant_id, plan_id, status, starts_at, expires_at, created_at, updated_at) 
                         VALUES (:restro, 'PLAN-BIZ-MOBILE', 'active', NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), NOW(), NOW())"
                    );
                    $insertSub->execute(['restro' => $restroId]);
                } catch (Exception $subEx) {
                    // Ignore if subscriptions table does not exist or has different schema
                }

                $pdo->commit();

                $message = "🎉 Restaurant '{$restroName}' ({$restroId}) has been successfully created and initialized!";
                $messageType = "success";
                $createdData = [
                    'restaurant_id'   => $restroId,
                    'restaurant_name' => $restroName,
                    'owner_name'      => $ownerName,
                    'login_email'     => $email,
                    'login_password'  => $loginPass,
                    'bill_format'     => "{$billPrefix}01, {$billPrefix}02...",
                    'kot_format'      => "{$kotPrefix}01, {$kotPrefix}02...",
                ];

            } catch (Exception $e) {
                if ($pdo->inTransaction()) {
                    $pdo->rollBack();
                }
                $message = "Error creating restaurant: " . htmlspecialchars($e->getMessage());
                $messageType = "danger";
            }
        }
    }
}

// Next suggested Restaurant ID
$suggestedRestroId = $pdo ? calculateNextRestaurantId($pdo) : 'AISR/01';

// Fetch all existing restaurants
$existingRestaurants = [];
if ($pdo) {
    try {
        $stmt = $pdo->query(
            "SELECT r.id, r.name, r.email, r.phone, r.is_active, r.created_at, u.name as owner_name, u.email as owner_email,
                    rs.bill_prefix, rs.last_bill_number, rs.kot_prefix, rs.last_kot_number
             FROM restaurants r
             LEFT JOIN users u ON r.owner_id = u.id
             LEFT JOIN restaurant_settings rs ON rs.restaurant_id = r.id AND rs.key_name = 'sequence'
             ORDER BY r.created_at DESC"
        );
        $existingRestaurants = $stmt->fetchAll();
    } catch (Exception $e) {
        // Table might not exist or empty
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AISRestroOS - Quick Add Restaurant</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        :root {
            --ais-navy: #0f172a;
            --ais-orange: #ea580c;
            --ais-orange-hover: #c2410c;
            --ais-light: #f8fafc;
        }
        body {
            background-color: #f1f5f9;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            color: #1e293b;
        }
        .header-bar {
            background: linear-gradient(135deg, var(--ais-navy) 0%, #1e293b 100%);
            color: white;
            padding: 1.25rem 0;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
        }
        .card-custom {
            border: none;
            border-radius: 1rem;
            box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.07), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
            background: white;
        }
        .btn-ais {
            background-color: var(--ais-orange);
            color: white;
            font-weight: 600;
            border: none;
            padding: 0.75rem 1.5rem;
            border-radius: 0.5rem;
            transition: all 0.2s;
        }
        .btn-ais:hover {
            background-color: var(--ais-orange-hover);
            color: white;
            box-shadow: 0 4px 12px rgba(234, 88, 12, 0.3);
        }
        .badge-id {
            background-color: #ffedd5;
            color: #9a3412;
            font-weight: 700;
            font-size: 0.95rem;
            padding: 0.35rem 0.65rem;
            border-radius: 0.375rem;
            border: 1px solid #fed7aa;
        }
        .field-group-title {
            font-size: 0.85rem;
            text-transform: uppercase;
            font-weight: 700;
            letter-spacing: 0.05em;
            color: #64748b;
            margin-bottom: 0.75rem;
            border-bottom: 1px solid #e2e8f0;
            padding-bottom: 0.35rem;
        }
        .copy-pill {
            cursor: pointer;
            user-select: all;
            background: #e2e8f0;
            padding: 2px 6px;
            border-radius: 4px;
            font-family: monospace;
        }
    </style>
</head>
<body>

    <!-- Header -->
    <header class="header-bar mb-4">
        <div class="container d-flex align-items-center justify-content-between">
            <div class="d-flex align-items-center gap-3">
                <span class="fs-2 text-warning"><i class="bi bi-shop"></i></span>
                <div>
                    <h4 class="mb-0 fw-bold">AISRestroOS <span class="badge bg-warning text-dark fs-6 align-middle">Admin Setup</span></h4>
                    <small class="text-white-50">Quick Restaurant Onboarding & Account Initializer</small>
                </div>
            </div>
            <div>
                <span class="badge bg-dark border border-secondary text-light px-3 py-2">
                    <i class="bi bi-database me-1"></i> <?= htmlspecialchars($dbName) ?>
                </span>
            </div>
        </div>
    </header>

    <div class="container pb-5">

        <!-- DB Connection Error Alert if any -->
        <?php if ($dbError): ?>
            <div class="alert alert-danger shadow-sm mb-4">
                <h5 class="fw-bold"><i class="bi bi-exclamation-triangle-fill me-2"></i> Database Connection Error</h5>
                <p class="mb-2"><?= $dbError ?></p>
                <form method="POST" class="row g-2 align-items-center mt-2">
                    <div class="col-auto">
                        <label class="col-form-label fw-bold">Enter Database Password manually:</label>
                    </div>
                    <div class="col-auto">
                        <input type="password" name="override_db_pass" class="form-control" placeholder="Hostinger MySQL Password" required>
                    </div>
                    <div class="col-auto">
                        <button type="submit" class="btn btn-primary">Connect Again</button>
                    </div>
                </form>
            </div>
        <?php endif; ?>

        <!-- Success or Alert Messages -->
        <?php if ($message): ?>
            <div class="alert alert-<?= $messageType ?> alert-dismissible fade show shadow-sm mb-4" role="alert">
                <div class="d-flex align-items-center">
                    <i class="bi <?= $messageType === 'success' ? 'bi-check-circle-fill text-success fs-3 me-3' : 'bi-x-circle-fill text-danger fs-3 me-3' ?>"></i>
                    <div>
                        <div class="fw-bold fs-5"><?= $message ?></div>
                        <?php if ($createdData): ?>
                            <div class="mt-3 p-3 bg-light rounded border text-dark">
                                <h6 class="fw-bold text-success mb-2"><i class="bi bi-phone me-1"></i> AISRestroOS Android App Login Credentials:</h6>
                                <div class="row g-2">
                                    <div class="col-md-4">
                                        <strong>Unique Restaurant ID:</strong> 
                                        <span class="badge-id ms-1"><?= htmlspecialchars($createdData['restaurant_id']) ?></span>
                                    </div>
                                    <div class="col-md-4">
                                        <strong>Login Email:</strong> 
                                        <span class="copy-pill ms-1"><?= htmlspecialchars($createdData['login_email']) ?></span>
                                    </div>
                                    <div class="col-md-4">
                                        <strong>Login Password:</strong> 
                                        <span class="copy-pill ms-1"><?= htmlspecialchars($createdData['login_password']) ?></span>
                                    </div>
                                </div>
                                <div class="mt-2 text-muted small">
                                    <i class="bi bi-info-circle me-1"></i> Ab phone me AISRestroOS app open karke is Email aur Password se login karein.
                                </div>
                            </div>
                        <?php endif; ?>
                    </div>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        <?php endif; ?>

        <div class="row g-4">
            <!-- Add Restaurant Form Card -->
            <div class="col-lg-7">
                <div class="card card-custom p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-0 text-dark">
                            <i class="bi bi-plus-circle-fill text-warning me-2"></i> Add New Restaurant
                        </h5>
                        <span class="badge bg-secondary-subtle text-secondary border">Step 1</span>
                    </div>
                    <p class="text-muted small mb-4">
                        Data clean hone ke baad naya restaurant add karein. Yeh tool automatically Restaurant record, Owner account, Main branch aur Sequence numbering (INV-01 / KOT-01) set kar dega.
                    </p>

                    <form method="POST" action="">
                        <input type="hidden" name="action" value="add_restaurant">

                        <!-- Section: Restaurant Details -->
                        <div class="field-group-title">
                            <i class="bi bi-building me-1"></i> 1. Restaurant Information
                        </div>
                        <div class="row g-3 mb-4">
                            <div class="col-md-7">
                                <label class="form-label fw-semibold">Restaurant Name <span class="text-danger">*</span></label>
                                <input type="text" name="restaurant_name" class="form-control" placeholder="e.g. Trakiyo Royal Restaurant" required value="<?= htmlspecialchars($_POST['restaurant_name'] ?? '') ?>">
                            </div>
                            <div class="col-md-5">
                                <label class="form-label fw-semibold">
                                    Restaurant ID <span class="text-danger">*</span>
                                </label>
                                <input type="text" name="restaurant_id" class="form-control font-monospace fw-bold" value="<?= htmlspecialchars($_POST['restaurant_id'] ?? $suggestedRestroId) ?>" required>
                                <div class="form-text small">Next sequence: <code>AISR/01</code>, <code>AISR/02</code>...</div>
                            </div>

                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Mobile / Phone Number</label>
                                <input type="text" name="phone" class="form-control" placeholder="e.g. 9876543210" value="<?= htmlspecialchars($_POST['phone'] ?? '') ?>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Address / City</label>
                                <input type="text" name="address" class="form-control" placeholder="e.g. Civil Lines, Main Market" value="<?= htmlspecialchars($_POST['address'] ?? '') ?>">
                            </div>
                        </div>

                        <!-- Section: Owner Account -->
                        <div class="field-group-title">
                            <i class="bi bi-person-badge me-1"></i> 2. Owner & POS App Login Details
                        </div>
                        <div class="row g-3 mb-4">
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Owner Full Name <span class="text-danger">*</span></label>
                                <input type="text" name="owner_name" class="form-control" placeholder="e.g. Abhishek Chaudhary" required value="<?= htmlspecialchars($_POST['owner_name'] ?? '') ?>">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label fw-semibold">Login Email <span class="text-danger">*</span></label>
                                <input type="email" name="email" class="form-control" placeholder="e.g. admin@trakiyo.in" required value="<?= htmlspecialchars($_POST['email'] ?? '') ?>">
                                <div class="form-text small">Is email se app me login hoga.</div>
                            </div>
                            <div class="col-md-12">
                                <label class="form-label fw-semibold">Login Password <span class="text-danger">*</span></label>
                                <input type="text" name="password" class="form-control font-monospace" placeholder="e.g. admin@1234" required value="<?= htmlspecialchars($_POST['password'] ?? '123456') ?>">
                                <div class="form-text small">At least 6 characters. App me yahi password enter karein.</div>
                            </div>
                        </div>

                        <!-- Section: Billing & Numbering -->
                        <div class="field-group-title">
                            <i class="bi bi-receipt me-1"></i> 3. Sequence & Billing Preferences
                        </div>
                        <div class="row g-3 mb-4">
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Bill Prefix</label>
                                <input type="text" name="bill_prefix" class="form-control font-monospace" value="INV-">
                                <div class="form-text small">Order format: INV-01, INV-02</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">KOT Prefix</label>
                                <input type="text" name="kot_prefix" class="form-control font-monospace" value="KOT-">
                                <div class="form-text small">KOT format: KOT-01, KOT-02</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Default Tax / GST %</label>
                                <input type="number" step="0.01" name="tax_percentage" class="form-control" value="0.00">
                                <div class="form-text small">e.g. 5.00 for 5% GST</div>
                            </div>
                        </div>

                        <div class="d-grid mt-4">
                            <button type="submit" class="btn btn-ais btn-lg shadow">
                                <i class="bi bi-check2-circle me-2"></i> Create Restaurant & Initialize
                            </button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Existing Restaurants & Help Card -->
            <div class="col-lg-5">
                <!-- Existing Restaurants List -->
                <div class="card card-custom p-4 mb-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h6 class="fw-bold mb-0 text-dark">
                            <i class="bi bi-list-stars text-primary me-2"></i> Existing Restaurants
                        </h6>
                        <span class="badge bg-primary rounded-pill"><?= count($existingRestaurants) ?></span>
                    </div>

                    <?php if (empty($existingRestaurants)): ?>
                        <div class="text-center py-4 text-muted">
                            <i class="bi bi-inbox fs-1 d-block mb-2 text-black-50"></i>
                            <p class="mb-0 fw-semibold">No restaurants found in database.</p>
                            <small>Wipe complete! Add your 1st restaurant using the form on the left.</small>
                        </div>
                    <?php else: ?>
                        <div class="list-group list-group-flush">
                            <?php foreach ($existingRestaurants as $res): ?>
                                <div class="list-group-item px-0 py-3">
                                    <div class="d-flex justify-content-between align-items-start mb-1">
                                        <h6 class="fw-bold mb-0 text-dark"><?= htmlspecialchars($res['name']) ?></h6>
                                        <span class="badge-id"><?= htmlspecialchars($res['id']) ?></span>
                                    </div>
                                    <div class="text-muted small mb-1">
                                        <i class="bi bi-person me-1"></i> Owner: <strong><?= htmlspecialchars($res['owner_name'] ?? 'N/A') ?></strong> (<?= htmlspecialchars($res['owner_email'] ?? $res['email'] ?? '') ?>)
                                    </div>
                                    <?php if (!empty($res['phone'])): ?>
                                        <div class="text-muted small mb-1">
                                            <i class="bi bi-telephone me-1"></i> <?= htmlspecialchars($res['phone']) ?>
                                        </div>
                                    <?php endif; ?>
                                    <div class="d-flex gap-2 mt-2">
                                        <span class="badge bg-light text-dark border">
                                            Next Bill: <?= htmlspecialchars($res['bill_prefix'] ?? 'INV-') ?><?= sprintf('%02d', ((int)($res['last_bill_number'] ?? 0)) + 1) ?>
                                        </span>
                                        <span class="badge bg-light text-dark border">
                                            Next KOT: <?= htmlspecialchars($res['kot_prefix'] ?? 'KOT-') ?><?= sprintf('%02d', ((int)($res['last_kot_number'] ?? 0)) + 1) ?>
                                        </span>
                                    </div>
                                </div>
                            <?php endforeach; ?>
                        </div>
                    <?php endif; ?>
                </div>

                <!-- Info & Guidelines Box -->
                <div class="card card-custom p-4 bg-light">
                    <h6 class="fw-bold text-dark mb-2">
                        <i class="bi bi-lightbulb-fill text-warning me-1"></i> Instructions
                    </h6>
                    <ul class="text-muted small ps-3 mb-0" style="line-height: 1.8;">
                        <li><strong>Restaurant ID:</strong> Ye unique format <code>AISR/01</code>, <code>AISR/02</code> me rahega.</li>
                        <li><strong>Bill & KOT Counter:</strong> Automatically <code>0</code> se start hoga jisse pahla order <code>INV-01</code> aur pahla KOT <code>KOT-01</code> generate hoga.</li>
                        <li><strong>Phone Data Reset:</strong> Database clean hone ke baad phone me app ka data clear karein (Settings &gt; Apps &gt; AISRestroOS &gt; Clear Storage).</li>
                        <li><strong>Login in App:</strong> App open karke upar banaye gaye <strong>Email</strong> aur <strong>Password</strong> se direct login karein.</li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
