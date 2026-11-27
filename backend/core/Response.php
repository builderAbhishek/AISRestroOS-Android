<?php

namespace AISRestro\Core;

class Response
{
    public static bool $exitOnSend = true;
    public static bool $hasSent = false;
    public static ?array $lastResponse = null;
    public static int $lastStatusCode = 200;

    public static function reset(): void
    {
        self::$hasSent = false;
        self::$lastResponse = null;
        self::$lastStatusCode = 200;
    }

    public static function getLastResponse(): ?array
    {
        return self::$lastResponse;
    }

    public static function getLastStatusCode(): int
    {
        return self::$lastStatusCode;
    }

    /**
     * Send standardized success JSON response matching ApiResponseWrapper<T>
     */
    public static function success(mixed $data = null, string $message = 'Operation successful', int $statusCode = 200, array $extraTopLevel = []): void
    {
        self::$hasSent = true;
        self::$lastStatusCode = $statusCode;
        if (!headers_sent()) {
            http_response_code($statusCode);
            header('Content-Type: application/json; charset=UTF-8');
        }

        $response = array_merge([
            'status'  => 'success',
            'message' => $message,
            'data'    => $data,
        ], $extraTopLevel);

        self::$lastResponse = $response;

        echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if (self::$exitOnSend) {
            exit;
        }
    }

    /**
     * Send standardized error JSON response matching ApiResponseWrapper<T>
     */
    public static function error(string $message = 'An error occurred', int $statusCode = 400, ?array $errors = null): void
    {
        self::$hasSent = true;
        self::$lastStatusCode = $statusCode;
        if (!headers_sent()) {
            http_response_code($statusCode);
            header('Content-Type: application/json; charset=UTF-8');
        }

        $response = [
            'status'  => 'error',
            'message' => $message,
            'data'    => null,
        ];

        if ($errors !== null) {
            $response['errors'] = $errors;
        }

        self::$lastResponse = $response;

        echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if (self::$exitOnSend) {
            exit;
        }
    }

    /**
     * Return custom raw JSON (e.g. for dual-compatible DTOs)
     */
    public static function rawJson(array $data, int $statusCode = 200): void
    {
        self::$hasSent = true;
        self::$lastStatusCode = $statusCode;
        if (!headers_sent()) {
            http_response_code($statusCode);
            header('Content-Type: application/json; charset=UTF-8');
        }
        self::$lastResponse = $data;
        echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if (self::$exitOnSend) {
            exit;
        }
    }

    /**
     * Unauthorized response (401)
     */
    public static function unauthorized(string $message = 'Unauthenticated. Invalid or expired token.'): void
    {
        self::error($message, 401);
    }

    /**
     * Forbidden response (403)
     */
    public static function forbidden(string $message = 'Access forbidden. Tenant authorization denied.'): void
    {
        self::error($message, 403);
    }

    /**
     * Not found response (404)
     */
    public static function notFound(string $message = 'Resource not found.'): void
    {
        self::error($message, 404);
    }
}


