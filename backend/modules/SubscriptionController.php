<?php

namespace AISRestro\Modules;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;

class SubscriptionController
{
    /**
     * GET /subscriptions
     */
    public function index(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        $subscription = Database::fetchOne(
            "SELECT s.id, COALESCE(s.plan_id, s.subscription_plan_id) AS plan_id, s.status, s.starts_at, 
                    COALESCE(s.expires_at, s.ends_at) AS expires_at, p.name AS plan_name, p.price, 
                    COALESCE(p.features, 'Unlimited POS Orders, ESC/POS Bluetooth Thermal Printing, Menu Management, Realtime Sync') AS features 
             FROM subscriptions s
             LEFT JOIN subscription_plans p ON (p.id = s.plan_id OR p.id = s.subscription_plan_id)
             WHERE s.restaurant_id = :restro 
             ORDER BY COALESCE(s.expires_at, s.ends_at) DESC, s.id DESC LIMIT 1",
            ['restro' => $restaurantId]
        );

        if ($subscription) {
            $statusLower = strtolower((string)($subscription['status'] ?? ''));
            $expiresAt = (string)($subscription['expires_at'] ?? '');
            $expiresTs = $expiresAt !== '' ? strtotime($expiresAt) : false;
            $isDateExpired = ($expiresTs !== false && $expiresTs < time());
            $isStatusActive = in_array($statusLower, ['active', 'trial'], true);
            $isExpired = !$isStatusActive || $isDateExpired;

            $subscription['plan_name'] = (string)($subscription['plan_name'] ?: 'Business Mobile Plan');
            $subscription['is_expired'] = $isExpired;
            $subscription['is_active']  = !$isExpired;
            if ($isExpired) {
                $subscription['status'] = 'expired';
            }
        } else {
            // Check restaurant created_at for 7-day trial fallback if no subscription row exists
            $restro = Database::fetchOne("SELECT created_at FROM restaurants WHERE id = :id LIMIT 1", ['id' => $restaurantId]);
            $createdTs = !empty($restro['created_at']) ? strtotime((string)$restro['created_at']) : time();
            $trialEndTs = $createdTs + (7 * 86400);
            $isExpired = ($trialEndTs < time());

            $subscription = [
                'id'         => 0,
                'plan_id'    => 'PLAN-TRIAL',
                'plan_name'  => '7-Day Free Trial',
                'status'     => $isExpired ? 'expired' : 'trial',
                'price'      => 0.00,
                'starts_at'  => date('Y-m-d H:i:s', $createdTs),
                'expires_at' => date('Y-m-d H:i:s', $trialEndTs),
                'features'   => 'Unlimited POS Orders, ESC/POS Bluetooth Thermal Printing, Menu Management, Realtime Sync',
                'is_expired' => $isExpired,
                'is_active'  => !$isExpired,
            ];
        }

        Response::success($subscription, 'Subscription status retrieved');
    }

    /**
     * GET /subscription-plans
     */
    public function plans(Request $request): void
    {
        $rows = Database::fetchAll(
            "SELECT id, name, price, duration_days, COALESCE(features, '') AS features
             FROM subscription_plans
             WHERE COALESCE(is_active, 1) = 1
             ORDER BY price ASC"
        );

        $plans = [];
        foreach ($rows as $row) {
            $rawFeatures = trim((string)($row['features'] ?? ''));
            if ($rawFeatures !== '' && $rawFeatures[0] === '[') {
                $decoded = json_decode($rawFeatures, true);
                if (is_array($decoded)) {
                    $rawFeatures = implode(', ', array_filter(array_map('strval', $decoded)));
                }
            }
            $plans[] = [
                'id'            => (string)$row['id'],
                'name'          => (string)$row['name'],
                'price'         => (float)$row['price'],
                'duration_days' => (int)$row['duration_days'],
                'features'      => $rawFeatures,
            ];
        }

        if (empty($plans)) {
            $plans = [
                [
                    'id'            => 'PLAN-BIZ-MOBILE',
                    'name'          => 'Business Mobile Plan',
                    'price'         => 0.00,
                    'duration_days' => 365,
                    'features'      => 'POS Terminal, Offline Engine, Bluetooth Thermal Printing, Android Sync',
                ],
                [
                    'id'            => 'PLAN-PRO',
                    'name'          => 'Enterprise Multi-Branch Plan',
                    'price'         => 999.00,
                    'duration_days' => 30,
                    'features'      => 'Multi-branch Inventory, Staff Roles, Advanced Tax Reports, Priority Support',
                ]
            ];
        }

        Response::success($plans, 'Subscription plans retrieved');
    }
}
