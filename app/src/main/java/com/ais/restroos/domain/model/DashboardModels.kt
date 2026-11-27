package com.ais.restroos.domain.model

enum class PrinterState {
    ONLINE,
    OFFLINE,
    CONNECTING,
    ERROR;

    val label: String
        get() = when (this) {
            ONLINE -> "Printer Connected"
            OFFLINE -> "Not Connected"
            CONNECTING -> "Connecting to printer..."
            ERROR -> "Not Connected"
        }

    val badgeText: String
        get() = when (this) {
            ONLINE -> "Connected"
            OFFLINE -> "Not Connected"
            CONNECTING -> "Connecting"
            ERROR -> "Not Connected"
        }
}

data class DashboardOverview(
    val todaySales: Double = 0.0,
    val todayOrders: Int = 0,
    val yesterdaySales: Double = 0.0,
    val yesterdayOrders: Int = 0
)

data class RestaurantBranch(
    val id: String,
    val name: String,
    val subtitle: String = "Main Branch",
    val isSelected: Boolean = true
)

enum class FeatureCardType {
    WHATSAPP,
    GOOGLE_BUSINESS
}

data class FeaturePromotionalCard(
    val id: String,
    val title: String,
    val description: String,
    val type: FeatureCardType
)

data class OrderSummary(
    val orderId: String,
    val tableNumber: String,
    val itemCount: Int,
    val totalAmount: Double,
    val status: String,
    val timestamp: String
)

data class MenuItem(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val isAvailable: Boolean = true,
    val isVeg: Boolean = true
)