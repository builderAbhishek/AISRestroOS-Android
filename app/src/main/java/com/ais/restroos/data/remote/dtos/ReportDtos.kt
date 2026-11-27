package com.ais.restroos.data.remote.dtos

data class SalesReportSummaryDto(
    val total_revenue: Double,
    val total_tax: Double,
    val total_discount: Double,
    val completed_orders_count: Int,
    val average_order_value: Double
)

data class ItemSalesReportDto(
    val item_name: String,
    val total_quantity: Int,
    val total_amount: Double
)

data class StockReportDto(
    val total_items: Int = 0,
    val in_stock_count: Int = 0,
    val out_of_stock_count: Int = 0,
    val items: List<StockItemDto> = emptyList()
)

data class StockItemDto(
    val id: String,
    val code: String? = null,
    val name: String,
    val category: String,
    val price: Double,
    val current_stock: Int = 0,
    val is_available: Int = 1
)

data class PurchaseItemDto(
    val id: String,
    val item_name: String,
    val category: String = "Vegetables",
    val quantity: Double = 1.0,
    val unit: String = "kg",
    val vendor_name: String = "Local Market",
    val amount: Double = 0.0,
    val payment_mode: String = "CASH",
    val expense_type: String = "PURCHASE",
    val purchase_date: String,
    val notes: String = "",
    val created_at: String? = null
)

data class CreatePurchaseRequest(
    val id: String? = null,
    val item_name: String,
    val category: String = "Vegetables",
    val quantity: Double = 1.0,
    val unit: String = "kg",
    val vendor_name: String? = null,
    val amount: Double,
    val payment_mode: String = "CASH",
    val expense_type: String = "PURCHASE",
    val purchase_date: String,
    val notes: String? = null
)

data class DailyHisaabSummaryDto(
    val date: String,
    val total_sales: Double = 0.0,
    val order_count: Int = 0,
    val total_purchases: Double = 0.0,
    val total_expenses: Double = 0.0,
    val total_outflow: Double = 0.0,
    val entry_count: Int = 0,
    val net_profit: Double = 0.0
)

data class RestaurantProfileDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val gst_number: String = "",
    val fssai_number: String = "",
    val currency: String = "INR",
    val plan_name: String = "Business Mobile Plan",
    val plan_status: String = "ACTIVE"
)

data class UpdateRestaurantProfileRequest(
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val city: String,
    val state: String,
    val pincode: String,
    val gst_number: String,
    val fssai_number: String
)

data class StaffMemberDto(
    val id: String = "",
    val user_id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "CASHIER",
    val status: String = "ACTIVE",
    val is_owner: Boolean = false,
    val created_at: String = ""
)

data class StaffListResponseDto(
    val plan_id: String = "PLAN-BIZ-MOBILE",
    val plan_name: String = "Business Mobile Plan",
    val max_staff: Int = 2,
    val used_staff: Int = 0,
    val staff: List<StaffMemberDto> = emptyList()
)

data class CreateStaffRequest(
    val name: String,
    val email: String,
    val phone: String = "",
    val role: String = "CASHIER",
    val password: String
)

data class SubscriptionDto(
    val id: Int = 1,
    val plan_id: String = "PLAN-BIZ-MOBILE",
    val plan_name: String = "Business Mobile Plan",
    val status: String = "active",
    val price: Double = 0.0,
    val starts_at: String = "",
    val expires_at: String = "",
    val features: String = "",
    val is_expired: Boolean = false,
    val is_active: Boolean = true
)

data class SubscriptionPlanDto(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val duration_days: Int = 30,
    val features: String = ""
)

data class ReceiptSettingsDto(
    val header_text: String = "",
    val footer_text: String = "",
    val qr_image_url: String? = null,
    val show_qr: Boolean = true,
    val paper_width: String = "58mm",
    val font_size: Int = 18
)