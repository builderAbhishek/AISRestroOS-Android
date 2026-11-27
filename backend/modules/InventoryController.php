<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;
use AISRestro\Core\Validator;

class InventoryController
{
    /**
     * GET /inventory
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT id, COALESCE(name, item_name) AS name, COALESCE(unit, unit_of_measure, 'kg') AS unit, 
                    COALESCE(current_stock, quantity, 0.0) AS current_stock, 
                    COALESCE(min_stock_alert, low_stock_alert, 5.0) AS min_stock_alert, 
                    cost_per_unit, updated_at 
             FROM inventory_items 
             WHERE restaurant_id = :restro 
             ORDER BY name ASC",
            ['restro' => $restaurantId]
        );

        $data = array_map(function ($row) {
            return [
                'id'              => (string)$row['id'],
                'name'            => (string)$row['name'],
                'unit'            => (string)$row['unit'],
                'current_stock'   => (float)$row['current_stock'],
                'min_stock_alert' => (float)$row['min_stock_alert'],
                'cost_per_unit'   => (float)$row['cost_per_unit'],
                'is_low_stock'    => ((float)$row['current_stock'] <= (float)$row['min_stock_alert']),
            ];
        }, $rows);

        Response::success($data, 'Inventory items retrieved successfully');
    }

    /**
     * POST /inventory
     */
    public function store(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->getAttribute('branch_id');
        $input = $request->input();

        $validator = Validator::make($input, [
            'name' => 'required|min:1',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $id = !empty($input['id']) ? trim($input['id']) : 'INV-' . strtoupper(substr(UUID::v4(), 0, 8));
        $name = trim($input['name']);
        $unit = !empty($input['unit']) ? trim($input['unit']) : 'kg';
        $currentStock = isset($input['current_stock']) ? (float)$input['current_stock'] : 0.0;
        $minStock = isset($input['min_stock_alert']) ? (float)$input['min_stock_alert'] : 5.0;
        $cost = isset($input['cost_per_unit']) ? (float)$input['cost_per_unit'] : 0.0;

        $existing = Database::fetchOne(
            "SELECT id FROM inventory_items WHERE id = :id AND restaurant_id = :restro LIMIT 1",
            ['id' => $id, 'restro' => $restaurantId]
        );

        if ($existing) {
            Database::execute(
                "UPDATE inventory_items SET 
                    name = :name, item_name = :iname, unit = :unit, unit_of_measure = :uom, 
                    current_stock = :stock, quantity = :qty, 
                    min_stock_alert = :alert, low_stock_alert = :lalert, 
                    cost_per_unit = :cost, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = :id AND restaurant_id = :restro",
                [
                    'name'   => $name,
                    'iname'  => $name,
                    'unit'   => $unit,
                    'uom'    => $unit,
                    'stock'  => $currentStock,
                    'qty'    => $currentStock,
                    'alert'  => $minStock,
                    'lalert' => $minStock,
                    'cost'   => $cost,
                    'id'     => $id,
                    'restro' => $restaurantId,
                ]
            );
        } else {
            Database::execute(
                "INSERT INTO inventory_items (
                    id, restaurant_id, branch_id, item_name, name, quantity, current_stock, 
                    unit_of_measure, unit, low_stock_alert, min_stock_alert, cost_per_unit, 
                    created_at, updated_at
                 ) VALUES (
                    :id, :restro, :branch, :name, :name2, :stock, :stock2, 
                    :unit, :unit2, :alert, :alert2, :cost, 
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                 )",
                [
                    'id'      => $id,
                    'restro'  => $restaurantId,
                    'branch'  => $branchId,
                    'name'    => $name,
                    'name2'   => $name,
                    'stock'   => $currentStock,
                    'stock2'  => $currentStock,
                    'unit'    => $unit,
                    'unit2'   => $unit,
                    'alert'   => $minStock,
                    'alert2'  => $minStock,
                    'cost'    => $cost,
                ]
            );
        }

        Response::success([
            'id'              => $id,
            'name'            => $name,
            'unit'            => $unit,
            'current_stock'   => $currentStock,
            'min_stock_alert' => $minStock,
            'cost_per_unit'   => $cost,
        ], 'Inventory item saved successfully', 201);
    }

    /**
     * POST /inventory/movements
     */
    public function storeMovement(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $user = $request->getAttribute('user');
        $userId = $user ? $user['id'] : null;
        $input = $request->input();

        $validator = Validator::make($input, [
            'inventory_item_id' => 'required',
            'movement_type'     => 'required',
            'quantity'          => 'required|numeric',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $itemId = trim($input['inventory_item_id']);
        $type = strtoupper(trim($input['movement_type'])); // 'IN', 'OUT', 'WASTAGE'
        $qty = abs((float)$input['quantity']);
        $reason = !empty($input['reason']) ? trim($input['reason']) : null;

        Database::beginTransaction();

        try {
            $forUpdate = Database::forUpdate();
            $item = Database::fetchOne(
                "SELECT id, COALESCE(current_stock, quantity, 0.0) AS current_stock 
                 FROM inventory_items 
                 WHERE id = :id AND restaurant_id = :restro LIMIT 1{$forUpdate}",
                ['id' => $itemId, 'restro' => $restaurantId]
            );

            if (!$item) {
                Database::rollBack();
                Response::notFound('Inventory item not found');
            }

            $current = (float)$item['current_stock'];
            $newStock = ($type === 'IN') ? ($current + $qty) : max(0.0, $current - $qty);
            $qtyChange = ($type === 'IN') ? $qty : -$qty;

            Database::execute(
                "UPDATE inventory_items SET current_stock = :stock, quantity = :qty, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                ['stock' => $newStock, 'qty' => $newStock, 'id' => $itemId]
            );

            Database::execute(
                "INSERT INTO stock_movements (
                    restaurant_id, inventory_item_id, created_by_user_id, 
                    movement_type, quantity, quantity_change, notes, reason, created_at
                 ) VALUES (
                    :restro, :item, :uid, 
                    :type, :qty, :qchange, :notes, :reason, CURRENT_TIMESTAMP
                 )",
                [
                    'restro'  => $restaurantId,
                    'item'    => $itemId,
                    'uid'     => $userId,
                    'type'    => $type,
                    'qty'     => $qty,
                    'qchange' => $qtyChange,
                    'notes'   => $reason,
                    'reason'  => $reason,
                ]
            );

            Database::commit();

            Response::success([
                'inventory_item_id' => $itemId,
                'movement_type'     => $type,
                'quantity'          => $qty,
                'new_stock'         => $newStock,
            ], 'Stock movement recorded successfully', 201);
        } catch (\Exception $e) {
            Database::rollBack();
            Response::error('Failed to record stock movement: ' . $e->getMessage(), 500);
        }
    }

    /**
     * GET /inventory/purchases
     */
    public function listPurchases(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $date = $request->query('date');
        $fromDate = $request->query('from_date');
        $toDate = $request->query('to_date');
        $vendor = $request->query('vendor');

        $query = "SELECT id, restaurant_id, branch_id, item_name, category, quantity, unit, 
                         vendor_name, amount, payment_mode, expense_type, purchase_date, notes, created_at 
                  FROM daily_purchases 
                  WHERE restaurant_id = :restro";
        $params = ['restro' => $restaurantId];

        if (!empty($date)) {
            $query .= " AND purchase_date = :pdate";
            $params['pdate'] = $date;
        } elseif (!empty($fromDate) && !empty($toDate)) {
            $query .= " AND purchase_date BETWEEN :fdate AND :tdate";
            $params['fdate'] = $fromDate;
            $params['tdate'] = $toDate;
        }

        if (!empty($vendor)) {
            $query .= " AND vendor_name LIKE :vendor";
            $params['vendor'] = "%" . trim($vendor) . "%";
        }

        $query .= " ORDER BY purchase_date DESC, created_at DESC";

        try {
            $rows = Database::fetchAll($query, $params);
            $data = array_map(function ($row) {
                return [
                    'id'            => (string)$row['id'],
                    'item_name'     => (string)$row['item_name'],
                    'category'      => (string)$row['category'],
                    'quantity'      => (float)$row['quantity'],
                    'unit'          => (string)$row['unit'],
                    'vendor_name'   => (string)($row['vendor_name'] ?? 'Local Market'),
                    'amount'        => (float)$row['amount'],
                    'payment_mode'  => (string)$row['payment_mode'],
                    'expense_type'  => (string)$row['expense_type'],
                    'purchase_date' => (string)$row['purchase_date'],
                    'notes'         => (string)($row['notes'] ?? ''),
                    'created_at'    => (string)$row['created_at'],
                ];
            }, $rows);

            Response::success($data, 'Purchases retrieved successfully');
        } catch (\Exception $e) {
            // In case table not yet created on server, return empty list gracefully
            Response::success([], 'Purchases list retrieved (empty)');
        }
    }

    /**
     * POST /inventory/purchases
     */
    public function storePurchase(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId     = $request->getAttribute('branch_id');
        $user         = $request->getAttribute('user');
        $userId       = $user ? $user['id'] : null;
        $input        = $request->input();

        $validator = Validator::make($input, [
            'item_name' => 'required|min:1',
            'amount'    => 'required|numeric',
        ]);

        if ($validator->fails()) {
            Response::error('Validation failed', 422, $validator->errors());
        }

        $id           = !empty($input['id']) ? trim($input['id']) : 'PUR-' . strtoupper(substr(UUID::v4(), 0, 8));
        $itemName     = trim($input['item_name']);
        $category     = !empty($input['category']) ? trim($input['category']) : 'Vegetables';
        $quantity     = isset($input['quantity']) ? (float)$input['quantity'] : 1.0;
        $unit         = !empty($input['unit']) ? trim($input['unit']) : 'kg';
        $vendorName   = !empty($input['vendor_name']) ? trim($input['vendor_name']) : 'Local Market';
        $amount       = (float)$input['amount'];
        $paymentMode  = !empty($input['payment_mode']) ? strtoupper(trim($input['payment_mode'])) : 'CASH';
        $expenseType  = !empty($input['expense_type']) ? strtoupper(trim($input['expense_type'])) : 'PURCHASE';
        $purchaseDate = !empty($input['purchase_date']) ? trim($input['purchase_date']) : date('Y-m-d');
        $notes        = !empty($input['notes']) ? trim($input['notes']) : null;

        try {
            Database::execute(
                "INSERT INTO daily_purchases (
                    id, restaurant_id, branch_id, item_name, category, quantity, unit,
                    vendor_name, amount, payment_mode, expense_type, purchase_date, notes,
                    created_by_user_id, created_at, updated_at
                 ) VALUES (
                    :id, :restro, :branch, :item, :cat, :qty, :unit,
                    :vendor, :amt, :mode, :etype, :pdate, :notes,
                    :uid, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                 )",
                [
                    'id'     => $id,
                    'restro' => $restaurantId,
                    'branch' => $branchId,
                    'item'   => $itemName,
                    'cat'    => $category,
                    'qty'    => $quantity,
                    'unit'   => $unit,
                    'vendor' => $vendorName,
                    'amt'    => $amount,
                    'mode'   => $paymentMode,
                    'etype'  => $expenseType,
                    'pdate'  => $purchaseDate,
                    'notes'  => $notes,
                    'uid'    => $userId,
                ]
            );

            Response::success([
                'id'            => $id,
                'item_name'     => $itemName,
                'category'      => $category,
                'quantity'      => $quantity,
                'unit'          => $unit,
                'vendor_name'   => $vendorName,
                'amount'        => $amount,
                'payment_mode'  => $paymentMode,
                'expense_type'  => $expenseType,
                'purchase_date' => $purchaseDate,
                'notes'         => $notes ?: '',
            ], 'Purchase recorded successfully', 201);
        } catch (\Exception $e) {
            Response::error('Failed to save purchase: ' . $e->getMessage(), 500);
        }
    }

    /**
     * DELETE /inventory/purchases/{id}
     */
    public function deletePurchase(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $id = $request->param('id') ?: $request->getAttribute('id') ?: $request->query('id');

        if (empty($id)) {
            Response::error('Purchase ID is required', 400);
        }

        try {
            Database::execute(
                "DELETE FROM daily_purchases WHERE id = :id AND restaurant_id = :restro",
                ['id' => $id, 'restro' => $restaurantId]
            );

            Response::success(['id' => $id], 'Purchase record deleted successfully');
        } catch (\Exception $e) {
            Response::error('Failed to delete purchase record: ' . $e->getMessage(), 500);
        }
    }

    /**
     * GET /inventory/daily-summary
     */
    public function dailySummary(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');
        $date = $request->query('date') ?: date('Y-m-d');

        try {
            if (method_exists(Database::class, 'ensureOrderSoftDeleteColumn')) {
                Database::ensureOrderSoftDeleteColumn();
            }
            // 1. Total Completed Sales for this Restaurant on requested KOT date
            $salesRow = Database::fetchOne(
                "SELECT COALESCE(SUM(grand_total), 0.0) AS total_sales, COUNT(*) AS order_count 
                 FROM orders 
                 WHERE restaurant_id = :restro 
                   AND status = 'COMPLETED' 
                   AND COALESCE(is_deleted, 0) = 0
                   AND DATE(COALESCE(created_at, completed_at)) = :pdate",
                ['restro' => $restaurantId, 'pdate' => $date]
            );

            $totalSales = $salesRow ? (float)$salesRow['total_sales'] : 0.0;
            $orderCount = $salesRow ? (int)$salesRow['order_count'] : 0;

            // 2. Total Purchases & Other Expenses for this Restaurant on requested date
            $purchasesRow = Database::fetchOne(
                "SELECT 
                    COALESCE(SUM(CASE WHEN expense_type = 'PURCHASE' THEN amount ELSE 0 END), 0.0) AS total_purchases,
                    COALESCE(SUM(CASE WHEN expense_type = 'EXPENSE' THEN amount ELSE 0 END), 0.0) AS total_expenses,
                    COALESCE(SUM(amount), 0.0) AS total_outflow,
                    COUNT(*) AS entry_count
                 FROM daily_purchases 
                 WHERE restaurant_id = :restro 
                   AND purchase_date = :pdate",
                ['restro' => $restaurantId, 'pdate' => $date]
            );

            $totalPurchases = $purchasesRow ? (float)$purchasesRow['total_purchases'] : 0.0;
            $totalExpenses  = $purchasesRow ? (float)$purchasesRow['total_expenses'] : 0.0;
            $totalOutflow   = $purchasesRow ? (float)$purchasesRow['total_outflow'] : 0.0;
            $entryCount     = $purchasesRow ? (int)$purchasesRow['entry_count'] : 0;

            $netProfit = $totalSales - $totalOutflow;

            Response::success([
                'date'            => $date,
                'total_sales'     => $totalSales,
                'order_count'     => $orderCount,
                'total_purchases' => $totalPurchases,
                'total_expenses'  => $totalExpenses,
                'total_outflow'   => $totalOutflow,
                'entry_count'     => $entryCount,
                'net_profit'      => $netProfit,
            ], 'Daily summary retrieved successfully');
        } catch (\Exception $e) {
            Response::error('Failed to compute daily summary: ' . $e->getMessage(), 500);
        }
    }
}
