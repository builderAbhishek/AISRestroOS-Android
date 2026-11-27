package com.ais.restroos.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Items : Screen("items")
    object AddMenuItem : Screen("menu/add")
    object Reports : Screen("reports")
    object ItemReports : Screen("reports/items")
    object OrderReports : Screen("reports/orders")
    object StockSummary : Screen("reports/stock")
    object DailyHisaab : Screen("reports/daily-hisaab")
    object ExpenseReport : Screen("reports/expenses")
    object Insights : Screen("insights")
    object More : Screen("more")
    object RestaurantProfile : Screen("more/profile")
    object PlanDetails : Screen("more/plan")
    object RegularCustomers : Screen("more/customers")
    object ManageStaff : Screen("more/staff")
    object Support : Screen("more/support")
    object PrinterSetup : Screen("more/printer")
    object ClosedOrders : Screen("orders_closed")
    object HoldOrders : Screen("orders/on-hold")
    object AllOrders : Screen("orders_all")
    object AddOrderPOS : Screen("pos/new")
    object EditOrderPOS : Screen("pos/edit?orderId={orderId}") {
        fun createRoute(orderId: String) = "pos/edit?orderId=${android.net.Uri.encode(orderId)}"
    }
    object CartPOS : Screen("pos/cart")
    object PreferencesPOS : Screen("pos/preferences")
    object BillReceipt : Screen("pos/bill-receipt?orderId={orderId}") {
        fun createRoute(orderId: String) = "pos/bill-receipt?orderId=${android.net.Uri.encode(orderId)}"
    }
    object SubscriptionExpired : Screen("subscription-expired")
    object ReceiptSettings : Screen("receipt-settings")
    object DigitalQrMenu : Screen("more/digital-qr-menu")
    object UdhaarKhata : Screen("reports/udhaar-khata")
}