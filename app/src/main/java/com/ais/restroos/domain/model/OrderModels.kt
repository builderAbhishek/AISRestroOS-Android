package com.ais.restroos.domain.model

enum class OrderStatus {
    ON_HOLD,
    SENT_TO_KITCHEN,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED
}

data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val price: Double // Unit Price / Rate of 1 item
) {
    val unitPrice: Double
        get() = price

    val lineTotal: Double
        get() = price * quantity

    val displayText: String
        get() = "$name × $quantity"
}

data class HeldOrder(
    val orderId: String,
    val kotNumber: String? = null,
    val tableNumber: String? = null,
    val totalAmount: Double,
    val items: List<OrderItem>,
    val status: OrderStatus = OrderStatus.ON_HOLD,
    val timestamp: Long = System.currentTimeMillis(),
    val orderType: String = "DINE_IN",
    val customerName: String? = null,
    val customerPhone: String? = null
)

data class ClosedOrder(
    val orderId: String,
    val invoiceNumber: String = orderId,
    val tableNumber: String? = null,
    val totalAmount: Double,
    val items: List<OrderItem>,
    val status: OrderStatus = OrderStatus.COMPLETED,
    val createdTimestamp: Long = System.currentTimeMillis() - 3600000,
    val completedTimestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "CASH",
    val isSelected: Boolean = false,
    val subtotal: Double = totalAmount,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val orderType: String = "DINE_IN"
)