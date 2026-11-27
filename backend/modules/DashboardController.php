<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;

class DashboardController
{
    /**
     * GET /branches/{branchId}/overview
     */
    public function overview(Request $request): void
    {
        if (method_exists(Database::class, 'ensureOrderSoftDeleteColumn')) {
            Database::ensureOrderSoftDeleteColumn();
        }
        $restaurantId = $request->getAttribute('restaurant_id');
        $branchId = $request->param('branchId');

        $curDateExpr = Database::isSqlite() ? "date('now')" : "CURDATE()";
        $prevDateExpr = Database::isSqlite() ? "date('now', '-1 day')" : "DATE_SUB(CURDATE(), INTERVAL 1 DAY)";

        // Today metrics (attributed to KOT creation date created_at)
        $todaySql = "SELECT 
                        COALESCE(SUM(grand_total), 0) AS sales,
                        COUNT(id) AS orders_count 
                     FROM orders 
                     WHERE restaurant_id = :restro 
                       AND status = 'COMPLETED' 
                       AND COALESCE(is_deleted, 0) = 0
                       AND DATE(COALESCE(created_at, completed_at)) = {$curDateExpr}";
        $paramsToday = ['restro' => $restaurantId];

        // Yesterday metrics (attributed to KOT creation date created_at)
        $yesterdaySql = "SELECT 
                            COALESCE(SUM(grand_total), 0) AS sales,
                            COUNT(id) AS orders_count 
                         FROM orders 
                         WHERE restaurant_id = :restro 
                           AND status = 'COMPLETED' 
                           AND COALESCE(is_deleted, 0) = 0
                           AND DATE(COALESCE(created_at, completed_at)) = {$prevDateExpr}";
        $paramsYesterday = ['restro' => $restaurantId];

        $today = Database::fetchOne($todaySql, $paramsToday);
        $yesterday = Database::fetchOne($yesterdaySql, $paramsYesterday);

        $todaySales = (float)($today['sales'] ?? 0.0);
        $todayOrders = (int)($today['orders_count'] ?? 0);
        $yesterdaySales = (float)($yesterday['sales'] ?? 0.0);
        $yesterdayOrders = (int)($yesterday['orders_count'] ?? 0);

        // Return dual-compatible payload: top-level matches DashboardOverview, data matches ApiResponseWrapper
        Response::rawJson([
            'todaySales'       => $todaySales,
            'todayOrders'      => $todayOrders,
            'yesterdaySales'   => $yesterdaySales,
            'yesterdayOrders'  => $yesterdayOrders,
            'status'           => 'success',
            'message'          => 'Dashboard metrics retrieved successfully',
            'data'             => [
                'todaySales'      => $todaySales,
                'todayOrders'     => $todayOrders,
                'yesterdaySales'  => $yesterdaySales,
                'yesterdayOrders' => $yesterdayOrders,
            ],
        ]);
    }
}
