<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;

class ReportController
{
    /**
     * GET /reports/sales
     * Returns SalesReportSummaryDto
     */
    public function sales(Request $request): void
    {
        if (method_exists(Database::class, 'ensureOrderSoftDeleteColumn')) {
            Database::ensureOrderSoftDeleteColumn();
        }
        $restaurantId = $request->getAttribute('restaurant_id');

        $row = Database::fetchOne(
            "SELECT 
                COALESCE(SUM(grand_total), 0) AS total_revenue,
                COALESCE(SUM(tax_amount), 0) AS total_tax,
                COALESCE(SUM(discount_amount), 0) AS total_discount,
                COUNT(id) AS completed_orders_count
             FROM orders 
             WHERE restaurant_id = :restro AND status = 'COMPLETED' AND COALESCE(is_deleted, 0) = 0",
            ['restro' => $restaurantId]
        );

        $revenue = (float)($row['total_revenue'] ?? 0.0);
        $tax = (float)($row['total_tax'] ?? 0.0);
        $discount = (float)($row['total_discount'] ?? 0.0);
        $count = (int)($row['completed_orders_count'] ?? 0);
        $avgValue = ($count > 0) ? round($revenue / $count, 2) : 0.0;

        Response::success([
            'total_revenue'          => $revenue,
            'total_tax'              => $tax,
            'total_discount'         => $discount,
            'completed_orders_count' => $count,
            'average_order_value'    => $avgValue,
        ], 'Sales summary retrieved successfully');
    }

    /**
     * GET /reports/itemized
     * Returns List<ItemSalesReportDto>
     */
    public function itemized(Request $request): void
    {
        if (method_exists(Database::class, 'ensureOrderSoftDeleteColumn')) {
            Database::ensureOrderSoftDeleteColumn();
        }
        $restaurantId = $request->getAttribute('restaurant_id');

        $rows = Database::fetchAll(
            "SELECT 
                oi.item_name,
                COALESCE(SUM(oi.quantity), 0) AS total_quantity,
                COALESCE(SUM(COALESCE(oi.total_price, oi.total, 0.0)), 0) AS total_amount
             FROM order_items oi
             JOIN orders o ON (o.uuid = oi.order_uuid OR o.id = oi.order_id)
             WHERE o.restaurant_id = :restro AND o.status = 'COMPLETED' AND COALESCE(o.is_deleted, 0) = 0
             GROUP BY oi.item_name
             ORDER BY total_amount DESC",
            ['restro' => $restaurantId]
        );

        $data = array_map(function ($row) {
            return [
                'item_name'      => (string)$row['item_name'],
                'total_quantity' => (int)$row['total_quantity'],
                'total_amount'   => (float)$row['total_amount'],
            ];
        }, $rows);

        Response::success($data, 'Itemized sales report retrieved successfully');
    }

    /**
     * GET /reports/stock
     */
    public function stock(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $menuStats = Database::fetchOne(
            "SELECT 
                COUNT(id) AS total_items,
                SUM(CASE WHEN current_stock > 0 THEN 1 ELSE 0 END) AS in_stock_count,
                SUM(CASE WHEN current_stock <= 0 THEN 1 ELSE 0 END) AS out_of_stock_count
             FROM menu_items 
             WHERE restaurant_id = :restro",
            ['restro' => $restaurantId]
        );

        $items = Database::fetchAll(
            "SELECT id, code, name, category, price, current_stock, COALESCE(is_active, 1) AS is_available 
             FROM menu_items 
             WHERE restaurant_id = :restro 
             ORDER BY category ASC, name ASC",
            ['restro' => $restaurantId]
        );

        $itemList = array_map(function ($row) {
            return [
                'id'            => (string)$row['id'],
                'code'          => (string)($row['code'] ?? ''),
                'name'          => (string)$row['name'],
                'category'      => (string)$row['category'],
                'price'         => (float)$row['price'],
                'current_stock' => (int)($row['current_stock'] ?? 0),
                'is_available'  => (int)($row['is_available'] ?? 1),
            ];
        }, $items);

        Response::success([
            'total_items'        => (int)($menuStats['total_items'] ?? 0),
            'in_stock_count'     => (int)($menuStats['in_stock_count'] ?? 0),
            'out_of_stock_count' => (int)($menuStats['out_of_stock_count'] ?? 0),
            'items'              => $itemList
        ], 'Stock summary retrieved successfully');
    }
}
