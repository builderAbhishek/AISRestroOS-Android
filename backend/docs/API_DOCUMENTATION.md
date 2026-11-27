# AISRestroOS REST API Specification

**Base Production URL:** `https://restro.trakiyo.in/api/` (Also supports `/api/v1/` prefix)  
**Authentication Scheme:** HTTP Bearer Token  
**Content-Type:** `application/json; charset=UTF-8`  
**Response Format:** Standardized `ApiResponseWrapper<T>`  

---

## 1. Common Response Envelope

All API endpoints (unless documented otherwise) return the standardized `ApiResponseWrapper<T>` structure:

### Success Response (HTTP 200 / 201)
```json
{
  "status": "success",
  "message": "Operation successful",
  "data": { ... }
}
```

### Error Response (HTTP 400 / 401 / 403 / 404 / 422 / 500)
```json
{
  "status": "error",
  "message": "Validation failed",
  "data": null,
  "errors": {
    "email": ["The email field is required."]
  }
}
```

---

## 2. Diagnostics

### `GET /health`
Returns system health, server runtime, and UTC timestamp.
- **Auth:** Public
- **Response:**
```json
{
  "status": "success",
  "message": "AISRestroOS backend is healthy",
  "data": {
    "status": "online",
    "service": "AISRestroOS Core PHP REST API",
    "version": "1.0.0",
    "runtime": "PHP 8.3",
    "time": "2026-11-23 12:30:00"
  }
}
```

---

## 3. Authentication Module

### `POST /auth/register`
Creates user, restaurant, default branch, sequence counters, and issues bearer token.
- **Auth:** Public
- **Request Body:**
```json
{
  "name": "Lucky Singh",
  "email": "luckysingh955416@gmail.com",
  "password": "Password123",
  "restaurant_name": "Siddharth Family Resturent",
  "phone": "9876543210"
}
```
- **Response (201 Created):**
```json
{
  "status": "success",
  "message": "Restaurant registered successfully",
  "data": {
    "token": "1|3d7f9a2e8c1b4...",
    "user": {
      "id": 1,
      "name": "Lucky Singh",
      "email": "luckysingh955416@gmail.com",
      "phone": "9876543210"
    },
    "restaurant_id": "RESTRO-ABC12345",
    "branch_id": "BR-XYZ67890",
    "role": "owner"
  }
}
```

### `POST /auth/login`
Authenticates existing credentials and issues a session token.
- **Auth:** Public
- **Request Body:**
```json
{
  "email": "luckysingh955416@gmail.com",
  "password": "Password123"
}
```
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Logged in successfully",
  "data": {
    "token": "2|e4b5c6d7a8...",
    "user": {
      "id": 1,
      "name": "Lucky Singh",
      "email": "luckysingh955416@gmail.com",
      "phone": "9876543210"
    },
    "restaurant_id": "RESTRO-ABC12345",
    "branch_id": "BR-XYZ67890",
    "role": "owner"
  }
}
```

### `POST /auth/logout`
Revokes active session token.
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Logged out successfully",
  "data": null
}
```

### `GET /auth/me`
Retrieves current authenticated profile and restaurant membership.
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Authenticated user retrieved",
  "data": {
    "token": "2|e4b5c6d7a8...",
    "user": {
      "id": 1,
      "name": "Lucky Singh",
      "email": "luckysingh955416@gmail.com",
      "phone": "9876543210"
    },
    "restaurant_id": "RESTRO-ABC12345",
    "branch_id": "BR-XYZ67890",
    "role": "owner"
  }
}
```

---

## 4. Menu & Category Management

### `GET /categories`
Lists food categories.
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Categories retrieved successfully",
  "data": [
    { "id": "CAT-01", "name": "डोसा", "display_order": 1 },
    { "id": "CAT-02", "name": "Main Course", "display_order": 2 }
  ]
}
```

### `POST /categories`
Creates or updates a category.
- **Auth:** Bearer Token
- **Request Body:**
```json
{
  "id": "CAT-01",
  "name": "डोसा",
  "display_order": 1
}
```

### `GET /menu-items`
Lists all dishes matching Android `MenuItemSyncDto`.
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Menu items retrieved successfully",
  "data": [
    {
      "id": "ITEM-DOSA-01",
      "code": "05",
      "name": "मसाला डोसा Special",
      "category": "डोसा",
      "price": 120.0,
      "tax_mode": "WITHOUT_TAX",
      "tax_percentage": "None",
      "current_stock": 50,
      "is_default_tax": true,
      "is_favourite": true,
      "image_url": "https://restro.trakiyo.in/api/uploads/menu/dish_abc123.jpg"
    }
  ]
}
```

### `POST /menu-items`
Creates or updates a menu item.
- **Auth:** Bearer Token
- **Request Body:** Matching `MenuItemSyncDto`.

### `POST /menu-items/upload-image`
Uploads a dish picture via multipart form.
- **Auth:** Bearer Token
- **Content-Type:** `multipart/form-data`
- **Form Field:** `image` (binary file: JPG, PNG, WEBP, GIF, max 5MB)
- **Response (201 Created):**
```json
{
  "status": "success",
  "message": "Image uploaded successfully",
  "data": {
    "url": "https://restro.trakiyo.in/api/uploads/menu/dish_84e1b82a_1732360000.jpg",
    "path": "uploads/menu/dish_84e1b82a_1732360000.jpg"
  }
}
```

---

## 5. Orders, KOT & Billing

### `GET /orders?status={status}`
Fetches orders filtered by status (`ON_HOLD`, `COMPLETED`, or `ALL`).
- **Auth:** Bearer Token
- **Query Params:** `status` (optional)
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Orders retrieved successfully",
  "data": [
    {
      "uuid": "7a8b9c-uuid-1",
      "order_number": "HOLD-1001",
      "order_type": "DINE_IN",
      "status": "ON_HOLD",
      "table_number": "G1",
      "customer_name": "Rohit Sharma",
      "customer_phone": "9811122233",
      "subtotal": 240.0,
      "discount_amount": 0.0,
      "tax_amount": 0.0,
      "service_charge": 0.0,
      "grand_total": 240.0,
      "items": [
        {
          "item_name": "मसाला डोसा Special",
          "unit_price": 120.0,
          "quantity": 2
        }
      ]
    }
  ]
}
```

### `POST /orders`
Creates a new order (held or completed) with line items.
- **Auth:** Bearer Token
- **Request Body:** Matching `OrderSyncDto`.

### `POST /orders/{id}/complete`
Marks order as completed, allocates a per-restaurant transaction-safe Bill Number (e.g. `INV-0001`), and records payment tender.
- **Auth:** Bearer Token
- **Request Body:**
```json
{
  "payment_method": "UPI"
}
```
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Order completed and billed successfully",
  "data": {
    "uuid": "7a8b9c-uuid-1",
    "invoice_number": "INV-0001",
    "status": "COMPLETED",
    "grand_total": 240.0
  }
}
```

### `POST /kot`
Generates sequential KOT ticket for kitchen. If KOT already exists for `order_uuid`, retains original number without jumping sequence.
- **Auth:** Bearer Token
- **Request Body:**
```json
{
  "order_uuid": "7a8b9c-uuid-1",
  "table_number": "G1",
  "items": [
    {
      "item_name": "मसाला डोसा Special",
      "quantity": 2,
      "notes": "Extra crispy"
    }
  ]
}
```
- **Response (201 Created):**
```json
{
  "status": "success",
  "message": "KOT ticket created successfully",
  "data": {
    "id": 1,
    "kot_number": "KOT-0001",
    "order_uuid": "7a8b9c-uuid-1",
    "table_number": "G1",
    "status": "SENT_TO_KITCHEN",
    "reprint_count": 0,
    "items": [
      { "item_name": "मसाला डोसा Special", "quantity": 2, "notes": "Extra crispy" }
    ]
  }
}
```

### `POST /kot/{id}/reprint`
Increments `reprint_count` and retains the original KOT number.
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "KOT ticket reprint verified. Original KOT number preserved.",
  "data": {
    "id": 1,
    "kot_number": "KOT-0001",
    "order_uuid": "7a8b9c-uuid-1",
    "reprint_count": 1,
    "items": [ ... ]
  }
}
```

---

## 6. Customers & Tables

### `GET /customers`
Lists registered customers.
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Customers retrieved successfully",
  "data": [
    {
      "id": "CUST-A1B2C3",
      "name": "Rohit Sharma",
      "mobile_number": "9811122233",
      "is_regular": true
    }
  ]
}
```

### `POST /customers`
Upserts customer profile by mobile number.
- **Auth:** Bearer Token
- **Request Body:** Matching `CustomerSyncDto`.

### `GET /tables`
Lists restaurant tables and occupancy state.
- **Auth:** Bearer Token

---

## 7. Reports & Dashboard Metrics

### `GET /reports/sales`
Returns sales financial overview (`SalesReportSummaryDto`).
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Sales summary retrieved successfully",
  "data": {
    "total_revenue": 240.0,
    "total_tax": 0.0,
    "total_discount": 0.0,
    "completed_orders_count": 1,
    "average_order_value": 240.0
  }
}
```

### `GET /reports/itemized`
Returns itemized dish sales report (`List<ItemSalesReportDto>`).
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Itemized sales report retrieved successfully",
  "data": [
    {
      "item_name": "मसाला डोसा Special",
      "total_quantity": 2,
      "total_amount": 240.0
    }
  ]
}
```

### `GET /branches/{branchId}/overview`
Returns live overview metrics. Supports both direct `DashboardOverview` deserialization and wrapper deserialization.
- **Auth:** Bearer Token
- **Response (200 OK):**
```json
{
  "todaySales": 240.0,
  "todayOrders": 1,
  "yesterdaySales": 0.0,
  "yesterdayOrders": 0,
  "status": "success",
  "message": "Dashboard metrics retrieved successfully",
  "data": {
    "todaySales": 240.0,
    "todayOrders": 1,
    "yesterdaySales": 0.0,
    "yesterdayOrders": 0
  }
}
```

---

## 8. Real-Time Offline Synchronization

### `POST /sync/push`
Syncs offline held and completed orders and customers from Android WorkManager (`SyncWorker`).
- **Auth:** Bearer Token
- **Headers:** `X-Idempotency-Key: <UUID>`
- **Request Body:** Matching `SyncPushRequest`.
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Sync push processed successfully",
  "data": {
    "latest_cursor": 1732360000,
    "synced_uuids": ["OFFLINE-ORDER-99"],
    "failed_uuids": [],
    "conflicts": []
  }
}
```

### `GET /sync/pull?cursor={cursor}`
Returns records modified after `cursor`. If `cursor=0`, returns full initial dataset.
- **Auth:** Bearer Token
- **Query Params:** `cursor` (integer timestamp or sync_seq)
- **Response (200 OK):**
```json
{
  "status": "success",
  "message": "Sync pull retrieved successfully",
  "data": {
    "latest_cursor": 1732360500,
    "menu_items": [ ... ],
    "categories": [ ... ],
    "customers": [ ... ],
    "tombstones": []
  }
}
```
