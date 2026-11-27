<?php

namespace AISRestro\Middleware;

use AISRestro\Core\Request;
use AISRestro\Core\Response;
use AISRestro\Core\Auth;

class AuthMiddleware
{
    public function handle(Request $request): void
    {
        $token = $request->getBearerToken();

        if (!$token) {
            Response::unauthorized("Bearer token is missing from Authorization header.");
            return;
        }

        $session = Auth::validateToken($token);

        if (!$session) {
            Response::unauthorized("Invalid or expired session token. Please re-login.");
            return;
        }

        $request->setAttribute('user', $session['user']);
        $request->setAttribute('restaurant_id', $session['restaurant_id']);
        $request->setAttribute('branch_id', $session['branch_id']);
        $request->setAttribute('role', $session['role']);
    }
}
