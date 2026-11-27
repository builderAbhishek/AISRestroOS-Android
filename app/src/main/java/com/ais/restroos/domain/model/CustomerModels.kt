package com.ais.restroos.domain.model

data class Customer(
    val id: String,
    val name: String,
    val mobileNumber: String,
    val isRegular: Boolean = false,
    val udhaarBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class KhataEntry(
    val id: String,
    val customerName: String,
    val mobileNumber: String,
    val entryType: String, // "UDHAAR" or "PAYMENT"
    val amount: Double,
    val note: String?,
    val createdAt: String?
)