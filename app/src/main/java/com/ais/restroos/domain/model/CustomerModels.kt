package com.ais.restroos.domain.model

data class Customer(
    val id: String,
    val name: String,
    val mobileNumber: String,
    val isRegular: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)