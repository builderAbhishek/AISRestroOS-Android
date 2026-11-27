package com.ais.restroos.data.repository

import android.content.Context
import com.ais.restroos.data.local.LocalDatabaseHelper
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.OrderItemSyncDto
import com.ais.restroos.data.remote.dtos.OrderSyncDto
import com.ais.restroos.data.remote.dtos.TableSyncDto
import com.ais.restroos.data.sync.SyncManager
import com.ais.restroos.data.sync.SyncState
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.HeldOrder
import com.ais.restroos.domain.model.OrderItem
import com.ais.restroos.domain.model.OrderStatus
import com.ais.restroos.domain.model.RestaurantTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object OrderRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dbHelper: LocalDatabaseHelper? = null
    private var appContext: Context? = null
    private var realtimeSyncJob: Job? = null

    private val _onHoldOrders = MutableStateFlow<List<HeldOrder>>(emptyList())
    val onHoldOrders: StateFlow<List<HeldOrder>> = _onHoldOrders.asStateFlow()

    private val _closedOrders = MutableStateFlow<List<ClosedOrder>>(emptyList())
    val closedOrders: StateFlow<List<ClosedOrder>> = _closedOrders.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _tables = MutableStateFlow<List<RestaurantTable>>(emptyList())
    val tables: StateFlow<List<RestaurantTable>> = _tables.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        dbHelper = LocalDatabaseHelper.getInstance(context)
        // Clean out any stale local orders from SQLite
        dbHelper?.clearAllOrders()
        _onHoldOrders.value = emptyList()
        _closedOrders.value = emptyList()
        _pendingSyncCount.value = 0
        loadTablesFromLocalDatabase()

        // Fetch active orders and tables directly from backend
        repositoryScope.launch {
            val token = RetrofitClient.getAuthToken()
            if (!token.isNullOrBlank()) {
                fetchOrdersFromBackend()
                startRealtimeSync()
            }
        }
    }

    fun startRealtimeSync() {
        // Disabled continuous rapid polling to prevent unwanted server traffic
    }

    fun stopRealtimeSync() {
        realtimeSyncJob?.cancel()
        realtimeSyncJob = null
    }

    fun loadFromLocalDatabase() {
        // Orders, KOTs, and bills are NOT loaded from local SQLite
        _pendingSyncCount.value = 0
        refreshTableOccupancy()
    }

    fun loadTablesFromLocalDatabase() {
        val db = dbHelper ?: return
        val localTables = db.getAllTables()
        _tables.value = localTables
        refreshTableOccupancy()
    }

    fun refreshTableOccupancy() {
        val currentTables = _tables.value
        val heldList = _onHoldOrders.value
        val heldByTable = heldList.filter { !it.tableNumber.isNullOrBlank() }
            .associateBy { it.tableNumber!!.trim() }

        val updated = currentTables.map { table ->
            val held = heldByTable[table.number.trim()]
            if (held != null) {
                table.copy(
                    isOccupied = true,
                    activeOrderId = held.orderId,
                    activeOrderTotal = held.totalAmount,
                    activeItemCount = held.items.sumOf { it.quantity }
                )
            } else {
                table.copy(
                    isOccupied = false,
                    activeOrderId = null,
                    activeOrderTotal = null,
                    activeItemCount = null
                )
            }
        }
        _tables.value = updated
    }

    fun addTable(number: String) {
        val cleanNumber = number.trim()
        if (cleanNumber.isBlank()) return
        val newTable = RestaurantTable(number = cleanNumber, isOccupied = false)
        if (_tables.value.none { it.number.equals(cleanNumber, ignoreCase = true) }) {
            _tables.update { (it + newTable).sortedBy { t -> t.number } }
            dbHelper?.saveTable(newTable)
            refreshTableOccupancy()

            // Async sync with backend
            repositoryScope.launch {
                try {
                    val dto = TableSyncDto(
                        id = "TBL-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
                        number = cleanNumber,
                        capacity = 4,
                        isOccupied = false
                    )
                    RetrofitClient.apiService.createTable(dto)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun fetchTablesFromBackend(): Result<List<RestaurantTable>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.getTables()
            if (response.isSuccessful && response.body()?.status == "success") {
                val dtoList = response.body()?.data ?: emptyList()
                val tableList = dtoList.map { dto ->
                    RestaurantTable(
                        number = dto.number,
                        isOccupied = dto.isOccupied
                    )
                }
                _tables.value = tableList
                dbHelper?.saveTables(tableList)
                refreshTableOccupancy()
                Result.success(_tables.value)
            } else {
                loadTablesFromLocalDatabase()
                Result.failure(Exception("Failed to fetch tables"))
            }
        } catch (e: Exception) {
            loadTablesFromLocalDatabase()
            Result.failure(e)
        }
    }

    fun getNextInvoiceNumber(): String {
        val maxNum = _closedOrders.value.mapNotNull {
            val raw = it.invoiceNumber.trim()
            if (raw.startsWith("INV-", ignoreCase = true)) {
                raw.removePrefix("INV-").removePrefix("inv-").trim().toIntOrNull()
            } else null
        }.maxOrNull() ?: 0
        return String.format(java.util.Locale.US, "INV-%02d", maxNum + 1)
    }

    fun getNextKotNumber(): String {
        val maxNum = _onHoldOrders.value.mapNotNull {
            val raw = it.orderId.trim()
            if (raw.startsWith("KOT-", ignoreCase = true)) {
                raw.removePrefix("KOT-").removePrefix("kot-").trim().toIntOrNull()
            } else null
        }.maxOrNull() ?: 0
        return String.format(java.util.Locale.US, "KOT-%02d", maxNum + 1)
    }

    fun getOrderById(orderId: String): HeldOrder? {
        return _onHoldOrders.value.find { it.orderId == orderId }
    }

    fun getClosedOrderById(orderId: String): ClosedOrder? {
        return _closedOrders.value.find { it.orderId == orderId }
    }

    fun toggleClosedOrderSelection(orderId: String) {
        _closedOrders.update { list ->
            list.map {
                if (it.orderId == orderId) it.copy(isSelected = !it.isSelected) else it
            }
        }
    }

    fun selectAllClosedOrders(selectAll: Boolean) {
        _closedOrders.update { list ->
            list.map { it.copy(isSelected = selectAll) }
        }
    }

    fun deleteClosedOrders(orderIds: List<String>) {
        dbHelper?.deleteClosedOrders(orderIds)
        _closedOrders.update { list ->
            list.filterNot { it.orderId in orderIds }
        }
        _pendingSyncCount.value = dbHelper?.getPendingSyncCount() ?: 0
    }

    suspend fun softDeleteClosedOrder(orderId: String): Boolean = withContext(Dispatchers.IO) {
        // Optimistically remove from in-memory list so totals recalculate immediately
        val previous = _closedOrders.value
        _closedOrders.update { list -> list.filterNot { it.orderId == orderId } }
        return@withContext try {
            val resp = RetrofitClient.apiService.softDeleteOrder(orderId, orderId)
            if (resp.isSuccessful) {
                fetchOrdersFromBackend()
                true
            } else {
                _closedOrders.value = previous
                false
            }
        } catch (_: Exception) {
            _closedOrders.value = previous
            false
        }
    }

    fun deleteOrder(orderId: String) {
        dbHelper?.deleteOrder(orderId)
        _onHoldOrders.update { list ->
            list.filterNot { it.orderId == orderId }
        }
        _pendingSyncCount.value = dbHelper?.getPendingSyncCount() ?: 0
        refreshTableOccupancy()
        repositoryScope.launch {
            try {
                RetrofitClient.apiService.cancelOrder(orderId)
                fetchOrdersFromBackend()
            } catch (_: Exception) {}
        }
    }

    fun clearAllOrders() {
        dbHelper?.clearAllOrders()
        _onHoldOrders.value = emptyList()
        _closedOrders.value = emptyList()
        _pendingSyncCount.value = 0
        refreshTableOccupancy()
        repositoryScope.launch {
            try {
                RetrofitClient.apiService.clearOrders()
            } catch (_: Exception) {}
        }
    }

    fun saveOrder(order: HeldOrder) {
        // 1. Update reactive in-memory state
        _onHoldOrders.update { list ->
            val existingIndex = list.indexOfFirst { it.orderId == order.orderId }
            if (existingIndex >= 0) {
                list.toMutableList().apply { set(existingIndex, order) }
            } else {
                list + order
            }
        }
        refreshTableOccupancy()

        // 2. Immediately send to backend directly (No local SQLite order persistence)
        triggerImmediateOnlineSyncForOrder(
            OrderSyncDto(
                uuid = order.orderId,
                order_number = order.kotNumber?.ifBlank { null } ?: "KOT-01",
                order_type = order.orderType,
                status = "ON_HOLD",
                table_number = order.tableNumber,
                customer_name = order.customerName,
                customer_phone = order.customerPhone,
                subtotal = order.totalAmount,
                discount_amount = 0.0,
                tax_amount = 0.0,
                service_charge = 0.0,
                grand_total = order.totalAmount,
                created_at = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(order.timestamp)),
                completed_at = null,
                items = order.items.map { item ->
                    OrderItemSyncDto(
                        item_name = item.name,
                        unit_price = item.price,
                        quantity = item.quantity
                    )
                }
            )
        )
    }

    fun saveClosedOrder(order: ClosedOrder) {
        // 1. Remove from held orders
        _onHoldOrders.update { list ->
            list.filterNot { it.orderId == order.orderId || (!it.tableNumber.isNullOrBlank() && it.tableNumber == order.tableNumber) }
        }

        // 2. Update reactive in-memory state
        _closedOrders.update { list ->
            listOf(order) + list.filterNot { it.orderId == order.orderId }
        }
        refreshTableOccupancy()

        // 3. Immediately send to backend directly (No local SQLite bill persistence)
        triggerImmediateOnlineSyncForOrder(
            OrderSyncDto(
                uuid = order.orderId,
                order_number = order.invoiceNumber,
                order_type = order.orderType,
                status = "COMPLETED",
                table_number = order.tableNumber,
                customer_name = order.customerName,
                customer_phone = order.customerPhone,
                subtotal = order.subtotal,
                discount_amount = order.discountAmount,
                tax_amount = order.taxAmount,
                service_charge = order.serviceCharge,
                grand_total = order.totalAmount,
                payment_method = order.paymentMethod,
                created_at = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(order.completedTimestamp)),
                completed_at = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(order.completedTimestamp)),
                items = order.items.map { item ->
                    OrderItemSyncDto(
                        item_name = item.name,
                        unit_price = item.price,
                        quantity = item.quantity
                    )
                }
            )
        )
    }

    private fun triggerImmediateOnlineSyncForOrder(dto: OrderSyncDto) {
        repositoryScope.launch {
            SyncManager.updateSyncState(SyncState.Syncing)
            try {
                val response = RetrofitClient.apiService.createOrder(dto)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val serverOrder = response.body()?.data
                    if (serverOrder != null && !serverOrder.uuid.isNullOrBlank()) {
                        val srvUuid = serverOrder.uuid
                        val srvOrderNo = serverOrder.order_number.orEmpty()
                        if (dto.status == "ON_HOLD") {
                            _onHoldOrders.update { list ->
                                list.map {
                                    if (it.orderId == dto.uuid) it.copy(orderId = srvUuid, kotNumber = srvOrderNo.ifBlank { it.kotNumber })
                                    else it
                                }
                            }
                        } else if (dto.status == "COMPLETED") {
                            _closedOrders.update { list ->
                                list.map {
                                    if (it.orderId == dto.uuid) it.copy(orderId = srvUuid, invoiceNumber = srvOrderNo.ifBlank { it.invoiceNumber })
                                    else it
                                }
                            }
                        }
                    }
                    SyncManager.updateSyncState(SyncState.Synced(System.currentTimeMillis()))
                    fetchOrdersFromBackend()
                } else {
                    android.util.Log.e("OrderRepository", "createOrder failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    SyncManager.updateSyncState(SyncState.Synced(System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                android.util.Log.e("OrderRepository", "createOrder exception: ${e.message}")
                SyncManager.updateSyncState(SyncState.Synced(System.currentTimeMillis()))
            }
        }
    }

    suspend fun fetchOrdersFromBackend(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.getOrders()
            if (response.isSuccessful && response.body()?.status == "success") {
                val dtoList = response.body()?.data ?: emptyList()
                android.util.Log.d("OrderRepository", "fetchOrdersFromBackend: received ${dtoList.size} orders")
                val holds = mutableListOf<HeldOrder>()
                val closed = mutableListOf<ClosedOrder>()

                dtoList.forEach { dto ->
                    val orderNo = dto.order_number.orEmpty()
                    val orderUuid = dto.uuid.orEmpty()
                    if (orderUuid.isBlank()) return@forEach

                    if (dto.status == "COMPLETED") {
                        val parsedTime = try {
                            // Attribute sales to KOT creation date (created_at) so pre-midnight KOTs stay in that date
                            val timeStr = dto.created_at ?: dto.completed_at
                            if (!timeStr.isNullOrBlank()) {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getDefault()
                                }
                                sdf.parse(timeStr)?.time ?: System.currentTimeMillis()
                            } else {
                                System.currentTimeMillis()
                            }
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }

                        val closedOrder = ClosedOrder(
                            orderId = orderUuid,
                            invoiceNumber = orderNo.ifBlank { orderUuid },
                            tableNumber = dto.table_number,
                            subtotal = dto.subtotal,
                            discountAmount = dto.discount_amount,
                            taxAmount = dto.tax_amount,
                            serviceCharge = dto.service_charge,
                            totalAmount = dto.grand_total,
                            items = dto.items.orEmpty().map { item ->
                                OrderItem(item.item_name.orEmpty(), item.item_name.orEmpty(), item.quantity, item.unit_price)
                            },
                            status = OrderStatus.COMPLETED,
                            completedTimestamp = parsedTime,
                            orderType = dto.order_type.orEmpty().ifBlank { "DINE_IN" },
                            customerName = dto.customer_name,
                            customerPhone = dto.customer_phone
                        )
                        closed.add(closedOrder)
                    } else if (dto.status == "ON_HOLD") {
                        val parsedHoldTime = try {
                            val timeStr = dto.created_at
                            if (!timeStr.isNullOrBlank()) {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                sdf.parse(timeStr)?.time ?: System.currentTimeMillis()
                            } else {
                                System.currentTimeMillis()
                            }
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }

                        val heldOrder = HeldOrder(
                            orderId = orderUuid,
                            kotNumber = orderNo.ifBlank { null },
                            tableNumber = dto.table_number,
                            totalAmount = dto.grand_total,
                            items = dto.items.orEmpty().map { item ->
                                OrderItem(item.item_name.orEmpty(), item.item_name.orEmpty(), item.quantity, item.unit_price)
                            },
                            status = OrderStatus.ON_HOLD,
                            timestamp = parsedHoldTime,
                            orderType = dto.order_type.orEmpty().ifBlank { "DINE_IN" },
                            customerName = dto.customer_name,
                            customerPhone = dto.customer_phone
                        )
                        holds.add(heldOrder)
                    }
                }

                _onHoldOrders.value = holds
                _closedOrders.value = closed
                _pendingSyncCount.value = 0
                fetchTablesFromBackend()
                SyncManager.updateSyncState(SyncState.Synced(System.currentTimeMillis()))
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch orders from server: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("OrderRepository", "fetchOrdersFromBackend failed: ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    fun resetToSampleData() {
        _onHoldOrders.value = emptyList()
        _closedOrders.value = emptyList()
    }
}
