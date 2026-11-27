package com.ais.restroos.domain.model

data class MenuItemVariant(
    val name: String,
    val price: Double
)

data class POSMenuItem(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val price: Double,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val isVeg: Boolean = true,
    val isFavourite: Boolean = false,
    val variants: List<MenuItemVariant> = emptyList()
)

data class CartItem(
    val menuItem: POSMenuItem,
    val quantity: Int
) {
    val totalPrice: Double
        get() = menuItem.price * quantity
}

data class RestaurantTable(
    val number: String,
    val isOccupied: Boolean = false,
    val activeOrderId: String? = null,
    val activeOrderTotal: Double? = null,
    val activeItemCount: Int? = null
)

enum class OrderType {
    DINE_IN,
    TAKEAWAY,
    DELIVERY
}

enum class PaymentMethod {
    CASH,
    UPI,
    PHONE_PE,
    GOOGLE_PAY,
    PAYTM,
    CARD
}