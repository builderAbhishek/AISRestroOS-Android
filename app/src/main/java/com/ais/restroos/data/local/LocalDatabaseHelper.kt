package com.ais.restroos.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ais.restroos.data.remote.dtos.OrderItemSyncDto
import com.ais.restroos.data.remote.dtos.OrderSyncDto
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.HeldOrder
import com.ais.restroos.domain.model.OrderItem
import com.ais.restroos.domain.model.OrderStatus
import com.ais.restroos.domain.model.POSMenuItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalDatabaseHelper private constructor(private val context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "ais_restro_local.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_ORDERS = "orders"
        const val TABLE_MENU_ITEMS = "menu_items"
        const val TABLE_CUSTOMERS = "customers"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_TABLES = "restaurant_tables"
        const val TABLE_DAILY_PURCHASES = "daily_purchases"

        @Volatile
        private var instance: LocalDatabaseHelper? = null

        fun getInstance(context: Context): LocalDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: LocalDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val gson = Gson()
    private val itemType = object : TypeToken<List<OrderItem>>() {}.type
    private val prefs by lazy { context.getSharedPreferences("ais_pos_sequences", Context.MODE_PRIVATE) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ORDERS (
                order_id TEXT PRIMARY KEY,
                invoice_number TEXT,
                table_number TEXT,
                order_type TEXT DEFAULT 'DINE_IN',
                status TEXT DEFAULT 'ON_HOLD',
                subtotal REAL DEFAULT 0.0,
                discount_amount REAL DEFAULT 0.0,
                tax_amount REAL DEFAULT 0.0,
                service_charge REAL DEFAULT 0.0,
                grand_total REAL DEFAULT 0.0,
                customer_name TEXT,
                customer_phone TEXT,
                payment_method TEXT DEFAULT 'CASH',
                items_json TEXT NOT NULL,
                sync_status TEXT DEFAULT 'PENDING',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_MENU_ITEMS (
                id TEXT PRIMARY KEY,
                code TEXT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL,
                image_url TEXT,
                is_available INTEGER DEFAULT 1,
                is_favourite INTEGER DEFAULT 0,
                sync_status TEXT DEFAULT 'SYNCED',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CUSTOMERS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                mobile_number TEXT UNIQUE NOT NULL,
                is_regular INTEGER DEFAULT 0,
                sync_status TEXT DEFAULT 'SYNCED',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                id TEXT PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                display_order INTEGER DEFAULT 0,
                sync_status TEXT DEFAULT 'SYNCED',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TABLES (
                id TEXT PRIMARY KEY,
                number TEXT UNIQUE NOT NULL,
                capacity INTEGER DEFAULT 4,
                is_occupied INTEGER DEFAULT 0,
                sync_status TEXT DEFAULT 'SYNCED',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DAILY_PURCHASES (
                id TEXT PRIMARY KEY,
                restaurant_id TEXT NOT NULL,
                item_name TEXT NOT NULL,
                category TEXT DEFAULT 'Vegetables',
                quantity REAL DEFAULT 1.0,
                unit TEXT DEFAULT 'kg',
                vendor_name TEXT,
                amount REAL DEFAULT 0.0,
                payment_mode TEXT DEFAULT 'CASH',
                expense_type TEXT DEFAULT 'PURCHASE',
                purchase_date TEXT NOT NULL,
                notes TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchases_restro_date ON $TABLE_DAILY_PURCHASES(restaurant_id, purchase_date)")

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_status ON $TABLE_ORDERS(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_sync ON $TABLE_ORDERS(sync_status)")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                id TEXT PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                display_order INTEGER DEFAULT 0,
                sync_status TEXT DEFAULT 'SYNCED',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TABLES (
                id TEXT PRIMARY KEY,
                number TEXT UNIQUE NOT NULL,
                capacity INTEGER DEFAULT 4,
                is_occupied INTEGER DEFAULT 0,
                sync_status TEXT DEFAULT 'SYNCED',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // Defensive column verification for menu item image URLs & variants
        try {
            db.execSQL("ALTER TABLE $TABLE_MENU_ITEMS ADD COLUMN image_url TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE $TABLE_MENU_ITEMS ADD COLUMN variants_json TEXT")
        } catch (_: Exception) {}

        // Ensure daily_purchases table exists
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DAILY_PURCHASES (
                id TEXT PRIMARY KEY,
                restaurant_id TEXT NOT NULL,
                item_name TEXT NOT NULL,
                category TEXT DEFAULT 'Vegetables',
                quantity REAL DEFAULT 1.0,
                unit TEXT DEFAULT 'kg',
                vendor_name TEXT,
                amount REAL DEFAULT 0.0,
                payment_mode TEXT DEFAULT 'CASH',
                expense_type TEXT DEFAULT 'PURCHASE',
                purchase_date TEXT NOT NULL,
                notes TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchases_restro_date ON $TABLE_DAILY_PURCHASES(restaurant_id, purchase_date)")

        // Clear any old orders/bills/KOT from local SQLite DB on open
        try {
            db.delete(TABLE_ORDERS, null, null)
        } catch (_: Exception) {}
        prefs.edit().remove("last_kot_seq").remove("last_inv_seq").apply()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                    id TEXT PRIMARY KEY,
                    name TEXT UNIQUE NOT NULL,
                    display_order INTEGER DEFAULT 0,
                    sync_status TEXT DEFAULT 'SYNCED',
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_TABLES (
                    id TEXT PRIMARY KEY,
                    number TEXT UNIQUE NOT NULL,
                    capacity INTEGER DEFAULT 4,
                    is_occupied INTEGER DEFAULT 0,
                    sync_status TEXT DEFAULT 'SYNCED',
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    // ------------------------------------------------------------------------
    // Order Operations (Offline Local DB storage disabled as requested)
    // Orders, Bills & KOTs are fetched & synced directly with the server.
    // ------------------------------------------------------------------------

    fun getNextInvoiceNumber(): String {
        val nextNum = prefs.getInt("last_inv_seq", 0) + 1
        prefs.edit().putInt("last_inv_seq", nextNum).apply()
        return String.format(java.util.Locale.US, "INV-%02d", nextNum)
    }

    fun getNextKotNumber(): String {
        val nextNum = prefs.getInt("last_kot_seq", 0) + 1
        prefs.edit().putInt("last_kot_seq", nextNum).apply()
        return String.format(java.util.Locale.US, "KOT-%02d", nextNum)
    }

    fun clearAllOrders() {
        try {
            val db = writableDatabase
            db.delete(TABLE_ORDERS, null, null)
        } catch (_: Exception) {}
        prefs.edit().remove("last_kot_seq").remove("last_inv_seq").apply()
    }

    fun saveHeldOrder(order: HeldOrder, syncStatus: String = "PENDING") {
        // Disabled: KOT and held orders are not stored in local DB
    }

    fun saveClosedOrder(order: ClosedOrder, syncStatus: String = "PENDING") {
        // Disabled: Bills and closed orders are not stored in local DB
    }

    fun markOrderSynced(orderId: String, invoiceNumber: String? = null) {
        // No-op
    }

    fun markOrdersSynced(orderIds: List<String>) {
        // No-op
    }

    fun deleteOrder(orderId: String) {
        try {
            val db = writableDatabase
            db.delete(TABLE_ORDERS, "order_id = ?", arrayOf(orderId))
        } catch (_: Exception) {}
    }

    fun deleteClosedOrders(orderIds: List<String>) {
        if (orderIds.isEmpty()) return
        try {
            val db = writableDatabase
            for (id in orderIds) {
                db.delete(TABLE_ORDERS, "order_id = ?", arrayOf(id))
            }
        } catch (_: Exception) {}
    }

    fun getAllHeldOrders(): List<HeldOrder> {
        return emptyList()
    }

    fun getAllClosedOrders(): List<ClosedOrder> {
        return emptyList()
    }

    fun getClosedOrderById(orderId: String): ClosedOrder? {
        return null
    }

    fun getPendingOrdersForSync(): List<OrderSyncDto> {
        return emptyList()
    }

    fun getPendingSyncCount(): Int {
        return 0
    }

    // ------------------------------------------------------------------------
    // Menu Operations
    // ------------------------------------------------------------------------

    fun saveMenuItem(item: POSMenuItem, syncStatus: String = "SYNCED") {
        val db = writableDatabase
        val variantsJson = if (item.variants.isNotEmpty()) gson.toJson(item.variants) else null
        val values = ContentValues().apply {
            put("id", item.id)
            put("code", item.code)
            put("name", item.name)
            put("category", item.category)
            put("price", item.price)
            put("image_url", item.imageUrl)
            put("variants_json", variantsJson)
            put("is_available", 1)
            put("is_favourite", if (item.isFavourite) 1 else 0)
            put("sync_status", syncStatus)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_MENU_ITEMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveMenuItems(items: List<POSMenuItem>, syncStatus: String = "SYNCED") {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (item in items) {
                val variantsJson = if (item.variants.isNotEmpty()) gson.toJson(item.variants) else null
                val values = ContentValues().apply {
                    put("id", item.id)
                    put("code", item.code)
                    put("name", item.name)
                    put("category", item.category)
                    put("price", item.price)
                    put("image_url", item.imageUrl)
                    put("variants_json", variantsJson)
                    put("is_available", 1)
                    put("is_favourite", if (item.isFavourite) 1 else 0)
                    put("sync_status", syncStatus)
                    put("updated_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_MENU_ITEMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllMenuItems(): List<POSMenuItem> {
        val list = mutableListOf<POSMenuItem>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM $TABLE_MENU_ITEMS ORDER BY name ASC",
            null
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val codeIdx = it.getColumnIndexOrThrow("code")
            val nameIdx = it.getColumnIndexOrThrow("name")
            val catIdx = it.getColumnIndexOrThrow("category")
            val priceIdx = it.getColumnIndexOrThrow("price")
            val imgIdx = it.getColumnIndexOrThrow("image_url")
            val favIdx = it.getColumnIndexOrThrow("is_favourite")
            val varIdx = it.getColumnIndex("variants_json")
            val varType = object : TypeToken<List<com.ais.restroos.domain.model.MenuItemVariant>>() {}.type

            while (it.moveToNext()) {
                val rawUrl = it.getString(imgIdx)
                val fullUrl = if (!rawUrl.isNullOrBlank()) {
                    val trimmed = rawUrl.trim()
                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        trimmed
                    } else if (trimmed.startsWith("/")) {
                        "https://restro.trakiyo.in/api$trimmed"
                    } else {
                        "https://restro.trakiyo.in/api/$trimmed"
                    }
                } else null

                val parsedVariants: List<com.ais.restroos.domain.model.MenuItemVariant> =
                    if (varIdx >= 0) {
                        val vJson = it.getString(varIdx)
                        if (!vJson.isNullOrBlank()) {
                            try {
                                gson.fromJson(vJson, varType) ?: emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }
                        } else emptyList()
                    } else emptyList()

                list.add(
                    POSMenuItem(
                        id = it.getString(idIdx),
                        code = it.getString(codeIdx) ?: "",
                        name = it.getString(nameIdx),
                        category = it.getString(catIdx),
                        price = it.getDouble(priceIdx),
                        imageUrl = fullUrl,
                        isFavourite = it.getInt(favIdx) == 1,
                        variants = parsedVariants
                    )
                )
            }
        }
        return list
    }

    fun deleteMenuItem(id: String) {
        val db = writableDatabase
        db.delete(TABLE_MENU_ITEMS, "id = ?", arrayOf(id))
    }

    // ------------------------------------------------------------------------
    // Category Operations
    // ------------------------------------------------------------------------

    fun getAllCategories(): List<String> {
        val list = mutableListOf<String>()
        try {
            val db = readableDatabase
            db.rawQuery("SELECT name FROM $TABLE_CATEGORIES ORDER BY display_order ASC, name ASC", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndexOrThrow("name")
                    do {
                        list.add(cursor.getString(idx))
                    } while (cursor.moveToNext())
                }
            }
        } catch (_: Exception) {
            // Table may be created on first open
        }
        return list
    }

    fun saveCategory(name: String, id: String = java.util.UUID.randomUUID().toString()) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("display_order", 0)
            put("sync_status", "PENDING")
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveCategories(names: List<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            names.forEachIndexed { index, name ->
                val values = ContentValues().apply {
                    put("id", "CAT-" + Math.abs(name.hashCode()).toString())
                    put("name", name)
                    put("display_order", index)
                    put("sync_status", "SYNCED")
                    put("updated_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ------------------------------------------------------------------------
    // Table Operations
    // ------------------------------------------------------------------------

    fun getAllTables(): List<com.ais.restroos.domain.model.RestaurantTable> {
        val list = mutableListOf<com.ais.restroos.domain.model.RestaurantTable>()
        try {
            val db = readableDatabase
            val query = """
                SELECT t.number, 
                       CASE WHEN h.table_number IS NOT NULL THEN 1 ELSE 0 END AS dynamic_occupied,
                       h.order_id,
                       h.grand_total,
                       h.items_json
                FROM $TABLE_TABLES t
                LEFT JOIN (
                    SELECT table_number, order_id, grand_total, items_json 
                    FROM $TABLE_ORDERS 
                    WHERE status = 'ON_HOLD' AND table_number IS NOT NULL AND table_number != ''
                ) h ON TRIM(t.number) = TRIM(h.table_number)
                GROUP BY t.number
                ORDER BY t.number ASC
            """.trimIndent()
            db.rawQuery(query, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val numIdx = cursor.getColumnIndexOrThrow("number")
                    val occIdx = cursor.getColumnIndexOrThrow("dynamic_occupied")
                    val orderIdIdx = cursor.getColumnIndexOrThrow("order_id")
                    val totalIdx = cursor.getColumnIndexOrThrow("grand_total")
                    val itemsJsonIdx = cursor.getColumnIndexOrThrow("items_json")
                    do {
                        val isOcc = cursor.getInt(occIdx) == 1
                        val orderId = if (isOcc && !cursor.isNull(orderIdIdx)) cursor.getString(orderIdIdx) else null
                        val total = if (isOcc && !cursor.isNull(totalIdx)) cursor.getDouble(totalIdx) else null
                        var itemCount: Int? = null
                        if (isOcc && !cursor.isNull(itemsJsonIdx)) {
                            try {
                                val itemType = object : com.google.gson.reflect.TypeToken<List<com.ais.restroos.domain.model.OrderItem>>() {}.type
                                val items: List<com.ais.restroos.domain.model.OrderItem>? = gson.fromJson(cursor.getString(itemsJsonIdx), itemType)
                                itemCount = items?.sumOf { it.quantity }
                            } catch (_: Exception) {}
                        }
                        list.add(
                            com.ais.restroos.domain.model.RestaurantTable(
                                number = cursor.getString(numIdx),
                                isOccupied = isOcc,
                                activeOrderId = orderId,
                                activeOrderTotal = total,
                                activeItemCount = itemCount
                            )
                        )
                    } while (cursor.moveToNext())
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveTable(table: com.ais.restroos.domain.model.RestaurantTable, id: String = java.util.UUID.randomUUID().toString()) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("number", table.number)
            put("capacity", 4)
            put("is_occupied", if (table.isOccupied) 1 else 0)
            put("sync_status", "PENDING")
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_TABLES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveTables(tables: List<com.ais.restroos.domain.model.RestaurantTable>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            tables.forEach { table ->
                val values = ContentValues().apply {
                    put("id", "TBL-" + Math.abs(table.number.hashCode()).toString())
                    put("number", table.number)
                    put("capacity", 4)
                    put("is_occupied", if (table.isOccupied) 1 else 0)
                    put("sync_status", "SYNCED")
                    put("updated_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_TABLES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ==========================================
    // DAILY PURCHASES & EXPENSES LOCAL STORAGE
    // ==========================================

    fun savePurchase(item: com.ais.restroos.data.remote.dtos.PurchaseItemDto, restaurantId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", item.id)
            put("restaurant_id", restaurantId)
            put("item_name", item.item_name)
            put("category", item.category)
            put("quantity", item.quantity)
            put("unit", item.unit)
            put("vendor_name", item.vendor_name)
            put("amount", item.amount)
            put("payment_mode", item.payment_mode)
            put("expense_type", item.expense_type)
            put("purchase_date", item.purchase_date)
            put("notes", item.notes)
            put("created_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_DAILY_PURCHASES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun savePurchases(items: List<com.ais.restroos.data.remote.dtos.PurchaseItemDto>, restaurantId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                val values = ContentValues().apply {
                    put("id", item.id)
                    put("restaurant_id", restaurantId)
                    put("item_name", item.item_name)
                    put("category", item.category)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("vendor_name", item.vendor_name)
                    put("amount", item.amount)
                    put("payment_mode", item.payment_mode)
                    put("expense_type", item.expense_type)
                    put("purchase_date", item.purchase_date)
                    put("notes", item.notes)
                    put("created_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_DAILY_PURCHASES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replacePurchasesForDate(
        items: List<com.ais.restroos.data.remote.dtos.PurchaseItemDto>,
        restaurantId: String,
        date: String
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                TABLE_DAILY_PURCHASES,
                "(restaurant_id = ? OR restaurant_id = 'default' OR restaurant_id = 'RESTRO-001') AND purchase_date = ?",
                arrayOf(restaurantId, date)
            )
            items.forEach { item ->
                val values = ContentValues().apply {
                    put("id", item.id)
                    put("restaurant_id", restaurantId)
                    put("item_name", item.item_name)
                    put("category", item.category)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("vendor_name", item.vendor_name)
                    put("amount", item.amount)
                    put("payment_mode", item.payment_mode)
                    put("expense_type", item.expense_type)
                    put("purchase_date", item.purchase_date)
                    put("notes", item.notes)
                    put("created_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_DAILY_PURCHASES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteLocalPurchase(id: String, restaurantId: String) {
        val db = writableDatabase
        db.delete(TABLE_DAILY_PURCHASES, "id = ?", arrayOf(id))
    }

    fun getLocalPurchases(
        restaurantId: String,
        date: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): List<com.ais.restroos.data.remote.dtos.PurchaseItemDto> {
        val list = mutableListOf<com.ais.restroos.data.remote.dtos.PurchaseItemDto>()
        val db = readableDatabase

        var selection = "(restaurant_id = ? OR restaurant_id = 'default' OR restaurant_id = 'RESTRO-001')"
        val args = mutableListOf(restaurantId)

        if (!date.isNullOrBlank()) {
            selection += " AND purchase_date = ?"
            args.add(date)
        } else if (!fromDate.isNullOrBlank() && !toDate.isNullOrBlank()) {
            selection += " AND purchase_date BETWEEN ? AND ?"
            args.add(fromDate)
            args.add(toDate)
        }

        val cursor = db.query(
            TABLE_DAILY_PURCHASES,
            null,
            selection,
            args.toTypedArray(),
            null,
            null,
            "purchase_date DESC, created_at DESC"
        )

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val nameIdx = c.getColumnIndexOrThrow("item_name")
            val catIdx = c.getColumnIndexOrThrow("category")
            val qtyIdx = c.getColumnIndexOrThrow("quantity")
            val unitIdx = c.getColumnIndexOrThrow("unit")
            val venIdx = c.getColumnIndexOrThrow("vendor_name")
            val amtIdx = c.getColumnIndexOrThrow("amount")
            val modeIdx = c.getColumnIndexOrThrow("payment_mode")
            val typeIdx = c.getColumnIndexOrThrow("expense_type")
            val dateIdx = c.getColumnIndexOrThrow("purchase_date")
            val notesIdx = c.getColumnIndexOrThrow("notes")

            while (c.moveToNext()) {
                list.add(
                    com.ais.restroos.data.remote.dtos.PurchaseItemDto(
                        id = c.getString(idIdx),
                        item_name = c.getString(nameIdx),
                        category = c.getString(catIdx),
                        quantity = c.getDouble(qtyIdx),
                        unit = c.getString(unitIdx),
                        vendor_name = c.getString(venIdx) ?: "Local Market",
                        amount = c.getDouble(amtIdx),
                        payment_mode = c.getString(modeIdx) ?: "CASH",
                        expense_type = c.getString(typeIdx) ?: "PURCHASE",
                        purchase_date = c.getString(dateIdx),
                        notes = c.getString(notesIdx) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun getDistinctPurchaseField(restaurantId: String, column: String, expenseType: String? = null): List<String> {
        val list = mutableListOf<String>()
        try {
            val db = readableDatabase
            val query = if (expenseType != null) {
                "SELECT DISTINCT $column FROM $TABLE_DAILY_PURCHASES WHERE (restaurant_id = ? OR restaurant_id = 'default' OR restaurant_id = 'RESTRO-001') AND expense_type = ? AND $column IS NOT NULL AND $column != '' LIMIT 50"
            } else {
                "SELECT DISTINCT $column FROM $TABLE_DAILY_PURCHASES WHERE (restaurant_id = ? OR restaurant_id = 'default' OR restaurant_id = 'RESTRO-001') AND $column IS NOT NULL AND $column != '' LIMIT 50"
            }
            val args = if (expenseType != null) arrayOf(restaurantId, expenseType) else arrayOf(restaurantId)
            val cursor = db.rawQuery(query, args)
            cursor.use {
                val idx = it.getColumnIndex(column)
                if (idx >= 0) {
                    while (it.moveToNext()) {
                        val value = it.getString(idx)
                        if (!value.isNullOrBlank()) {
                            list.add(value)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun getLocalDailySummary(restaurantId: String, date: String): com.ais.restroos.data.remote.dtos.DailyHisaabSummaryDto {
        val db = readableDatabase

        // 1. Calculate Today's Sales from in-memory closed orders
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val startMillis = try { sdf.parse(date)?.time ?: 0L } catch (_: Exception) { 0L }
        val endMillis = startMillis + 86400000L - 1L

        val matchedOrders = com.ais.restroos.data.repository.OrderRepository.closedOrders.value.filter {
            it.completedTimestamp in startMillis..endMillis
        }
        val orderCount = matchedOrders.size
        val totalSales = matchedOrders.sumOf { it.totalAmount }

        // 2. Calculate Today's Purchases and Expenses
        var totalPurchases = 0.0
        var totalExpenses = 0.0
        var totalOutflow = 0.0
        var entryCount = 0

        val purCursor = db.rawQuery(
            """
            SELECT 
                COUNT(*),
                COALESCE(SUM(CASE WHEN expense_type = 'PURCHASE' THEN amount ELSE 0 END), 0.0),
                COALESCE(SUM(CASE WHEN expense_type = 'EXPENSE' THEN amount ELSE 0 END), 0.0),
                COALESCE(SUM(amount), 0.0)
            FROM $TABLE_DAILY_PURCHASES 
            WHERE (restaurant_id = ? OR restaurant_id = 'default' OR restaurant_id = 'RESTRO-001') AND purchase_date = ?
            """.trimIndent(),
            arrayOf(restaurantId, date)
        )
        purCursor.use {
            if (it.moveToFirst()) {
                entryCount = it.getInt(0)
                totalPurchases = it.getDouble(1)
                totalExpenses = it.getDouble(2)
                totalOutflow = it.getDouble(3)
            }
        }

        return com.ais.restroos.data.remote.dtos.DailyHisaabSummaryDto(
            date = date,
            total_sales = totalSales,
            order_count = orderCount,
            total_purchases = totalPurchases,
            total_expenses = totalExpenses,
            total_outflow = totalOutflow,
            entry_count = entryCount,
            net_profit = totalSales - totalOutflow
        )
    }
}
