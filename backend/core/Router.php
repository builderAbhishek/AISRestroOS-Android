<?php

namespace AISRestro\Core;

class Router
{
    private array $routes = [];
    private array $globalMiddlewares = [];

    public function addGlobalMiddleware(callable|string $middleware): void
    {
        $this->globalMiddlewares[] = $middleware;
    }

    public function get(string $pattern, callable|array $handler, array $middlewares = []): void
    {
        $this->addRoute('GET', $pattern, $handler, $middlewares);
    }

    public function post(string $pattern, callable|array $handler, array $middlewares = []): void
    {
        $this->addRoute('POST', $pattern, $handler, $middlewares);
    }

    public function put(string $pattern, callable|array $handler, array $middlewares = []): void
    {
        $this->addRoute('PUT', $pattern, $handler, $middlewares);
    }

    public function delete(string $pattern, callable|array $handler, array $middlewares = []): void
    {
        $this->addRoute('DELETE', $pattern, $handler, $middlewares);
    }

    private function addRoute(string $method, string $pattern, callable|array $handler, array $middlewares = []): void
    {
        $cleanPattern = '/' . trim($pattern, '/');
        $this->routes[] = [
            'method'      => $method,
            'pattern'     => $cleanPattern,
            'handler'     => $handler,
            'middlewares' => $middlewares,
        ];
    }

    /**
     * Normalize URL path by stripping prefix directories like /restro/api or /api or /v1
     */
    public function normalizePath(string $rawPath): string
    {
        $path = '/' . trim($rawPath, '/');

        // Strip known base path prefixes if request was rewritten or proxied
        $prefixes = [
            '/restro/api/v1',
            '/restro/api',
            '/api/v1',
            '/api',
            '/v1',
        ];

        foreach ($prefixes as $prefix) {
            if ($path === $prefix) {
                return '/';
            }
            if (str_starts_with($path, $prefix . '/')) {
                $path = substr($path, strlen($prefix));
                break;
            }
        }

        return '/' . trim($path, '/');
    }

    /**
     * Dispatch the request
     */
    public function dispatch(Request $request): void
    {
        // Handle preflight OPTIONS requests for CORS
        if ($request->getMethod() === 'OPTIONS') {
            http_response_code(204);
            exit;
        }

        $requestMethod = $request->getMethod();
        $normalizedPath = $this->normalizePath($request->getPath());
        $request->setPath($normalizedPath);

        foreach ($this->routes as $route) {
            if ($route['method'] !== $requestMethod) {
                continue;
            }

            // Convert pattern with {param} placeholders to regex
            $patternRegex = preg_replace('/\{([a-zA-Z0-9_]+)\}/', '(?P<$1>[^/]+)', $route['pattern']);
            $patternRegex = '#^' . $patternRegex . '$#';

            if (preg_match($patternRegex, $normalizedPath, $matches)) {
                // Extract named route parameters
                $params = [];
                foreach ($matches as $key => $value) {
                    if (is_string($key)) {
                        $params[$key] = urldecode($value);
                    }
                }
                $request->setRouteParams($params);

                // Run global middlewares
                foreach ($this->globalMiddlewares as $middleware) {
                    if (Response::$hasSent) {
                        return;
                    }
                    $this->executeMiddleware($middleware, $request);
                }

                // Run route-level middlewares
                foreach ($route['middlewares'] as $middleware) {
                    if (Response::$hasSent) {
                        return;
                    }
                    $this->executeMiddleware($middleware, $request);
                }

                if (Response::$hasSent) {
                    return;
                }

                // Invoke route handler
                $handler = $route['handler'];
                if (is_array($handler)) {
                    [$class, $method] = $handler;
                    $controller = new $class();
                    $controller->$method($request);
                    return;
                }

                if (is_callable($handler)) {
                    $handler($request);
                    return;
                }
            }
        }

        Response::notFound("Route not found: [{$requestMethod}] {$normalizedPath}");
    }

    private function executeMiddleware(callable|string $middleware, Request $request): void
    {
        if (is_string($middleware)) {
            $instance = new $middleware();
            $instance->handle($request);
            return;
        }

        if (is_callable($middleware)) {
            $middleware($request);
        }
    }
}
