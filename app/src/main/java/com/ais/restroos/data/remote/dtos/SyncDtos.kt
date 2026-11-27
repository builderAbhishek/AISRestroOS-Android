package com.ais.restroos.data.remote.dtos

data class SyncPushRequest(
    val orders: List<OrderSyncDto> = emptyList(),
    val customers: List<CustomerSyncDto> = emptyList()
)

data class OrderSyncDto(
    val uuid: String,
    val order_number: String,
    val order_type: String,
    val status: String,
    val table_number: String? = null,
    val customer_name: String? = null,
    val customer_phone: String? = null,
    val subtotal: Double,
    val discount_amount: Double = 0.0,
    val tax_amount: Double = 0.0,
    val service_charge: Double = 0.0,
    val grand_total: Double,
    val payment_method: String = "CASH",
    val items: List<OrderItemSyncDto>,
    val created_at: String? = null,
    val completed_at: String? = null
)

data class OrderItemSyncDto(
    val item_name: String,
    val unit_price: Double,
    val quantity: Int
)

data class CustomerSyncDto(
    val id: String? = null,
    val name: String,
    val mobile_number: String,
    val is_regular: Boolean = false
)

data class SyncPushResult(
    val latest_cursor: Long,
    val synced_uuids: List<String>,
    val failed_uuids: List<String>,
    val conflicts: List<String> = emptyList()
)

data class SyncPullResult(
    val latest_cursor: Long,
    val menu_items: List<MenuItemSyncDto> = emptyList(),
    val categories: List<CategorySyncDto> = emptyList(),
    val customers: List<CustomerSyncDto> = emptyList(),
    val tombstones: List<TombstoneSyncDto> = emptyList()
)

data class MenuItemVariantDto(
    val name: String,
    val price: Double
)

data class MenuItemSyncDto(
    val id: String,
    val code: String? = null,
    val name: String,
    val category: String,
    val price: Double,
    val tax_mode: String = "WITHOUT_TAX",
    val tax_percentage: String = "None",
    val current_stock: Int = 100,
    val is_default_tax: Boolean = true,
    val is_favourite: Boolean = false,
    val image_url: String? = null,
    val variants: List<MenuItemVariantDto>? = emptyList()
)

data class CategorySyncDto(
    val id: String,
    val name: String,
    val display_order: Int = 0
)

data class TableSyncDto(
    val id: String? = null,
    val number: String,
    val capacity: Int = 4,
    val isOccupied: Boolean = false
)

data class DeviceSyncDto(
    val id: String? = null,
    val device_name: String,
    val mac_address: String? = null,
    val device_type: String = "PRINTER",
    val paper_width: String = "58mm",
    val is_active: Int = 1
)

data class TombstoneSyncDto(
    val entity_type: String,
    val entity_uuid: String,
    val sync_seq: Long
)