package com.ais.restroos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val tableNumber: String = "",
    val itemCount: Int = 0,
    val totalAmount: Double = 0.0,
    val status: String = "ON_HOLD",
    val itemsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)