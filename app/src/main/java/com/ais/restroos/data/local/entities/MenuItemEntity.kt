package com.ais.restroos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val isAvailable: Boolean = true,
    val isVeg: Boolean = true
)