<?php

namespace AISRestro\Middleware;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Database;
use AISRestro\Core\UUID;

class TenantMiddleware
{
    public function handle(Request $request): void
    {
        $restaurantId = $request->getAttribute('restaurant_id');

        if (empty($restaurantId)) {
            $user = $request->getAttribute('user');
            if (!$user) {
                Response::unauthorized("Unauthorized tenant context.");
            }

            // Look up restaurant owned or associated with user
            $membership = Database::fetchOne(
                "SELECT restaurant_id, branch_id FROM restaurant_memberships WHERE user_id = :uid LIMIT 1",
                ['uid' => $user['id']]
            );

            if ($membership && !empty($membership['restaurant_id'])) {
                $restaurantId = $membership['restaurant_id'];
                $request->setAttribute('restaurant_id', $restaurantId);
                $request->setAttribute('branch_id', $membership['branch_id']);
            } else {
                // If user exists without restaurant, create default restaurant for them
                $newRestroId   = 'RESTRO-' . strtoupper(substr(UUID::v4(), 0, 8));
                $newBranchId   = 'BR-' . strtoupper(substr(UUID::v4(), 0, 8));
                $membershipId  = UUID::v4();
                $restroName    = $user['name'] . "'s Restaurant";
                $slug          = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $restroName))) . '-' . substr(UUID::v4(), 0, 5);

                Database::execute(
                    "INSERT INTO restaurants (id, name, slug, owner_id, email, currency, is_active, created_at, updated_at) 
                     VALUES (:id, :name, :slug, :owner, :email, 'INR', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    [
                        'id'    => $newRestroId,
                        'name'  => $restroName,
                        'slug'  => $slug,
                        'owner' => $user['id'],
                        'email' => $user['email'],
                    ]
                );

                Database::execute(
                    "INSERT INTO branches (id, restaurant_id, name, code, is_main, is_main_branch, is_active, created_at, updated_at) 
                     VALUES (:id, :restro, 'Main Branch', 'MAIN-01', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    [
                        'id'     => $newBranchId,
                        'restro' => $newRestroId,
                    ]
                );

                Database::execute(
                    "INSERT INTO restaurant_memberships (id, user_id, restaurant_id, branch_id, role, status, is_active, created_at, updated_at) 
                     VALUES (:id, :uid, :restro, :branch, 'RESTAURANT_OWNER', 'active', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    [
                        'id'     => $membershipId,
                        'uid'    => $user['id'],
                        'restro' => $newRestroId,
                        'branch' => $newBranchId,
                    ]
                );

                Database::execute(
                    "INSERT INTO restaurant_settings (restaurant_id, key_name, key_value, bill_prefix, last_bill_number, kot_prefix, last_kot_number, created_at, updated_at) 
                     VALUES (:restro, 'sequence', 'Sequence counters initialized', 'INV-', 0, 'KOT-', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    ['restro' => $newRestroId]
                );

                $request->setAttribute('restaurant_id', $newRestroId);
                $request->setAttribute('branch_id', $newBranchId);
            }
        }
    }
}
