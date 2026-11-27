package com.ais.restroos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ais.restroos.data.local.dao.MenuItemDao
import com.ais.restroos.data.local.dao.OrderDao
import com.ais.restroos.data.local.entities.MenuItemEntity
import com.ais.restroos.data.local.entities.OrderEntity

@Database(
    entities = [OrderEntity::class, MenuItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun menuItemDao(): MenuItemDao
}