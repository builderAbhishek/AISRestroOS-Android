package com.ais.restroos.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.printer.BluetoothPrinterService
import com.ais.restroos.data.printer.EscPosPrinter
import com.ais.restroos.data.repository.CustomerRepository
import com.ais.restroos.data.repository.MenuRepository
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.CartItem
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.Customer
import com.ais.restroos.domain.model.HeldOrder
import com.ais.restroos.domain.model.MenuItemVariant
import com.ais.restroos.domain.model.OrderItem
import com.ais.restroos.domain.model.OrderStatus
import com.ais.restroos.domain.model.OrderType
import com.ais.restroos.domain.model.POSMenuItem
import com.ais.restroos.domain.model.PaymentMethod
import com.ais.restroos.domain.model.PrinterState
import com.ais.restroos.domain.model.RestaurantTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AddOrderUiState(
    val menuItems: List<POSMenuItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val categories: List<String> = listOf("All", "Favourite"),
    val cart: Map<String, CartItem> = emptyMap(),
    val showUnsavedDialog: Boolean = false,
    val showCustomerSheet: Boolean = false,
    val showDiscountDialog: Boolean = false,
    val showServiceChargeDialog: Boolean = false,
    val isTableDropdownExpanded: Boolean = false,
    val editingOrderId: String? = null,
    val tableNumber: String = "",
    val tables: List<RestaurantTable> = emptyList(),
    val customerPhone: String = "",
    val customerName: String = "",
    val saveAsRegularCustomer: Boolean = false,
    val attachedCustomer: Customer? = null,
    val orderType: OrderType = OrderType.DINE_IN,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val discountAmount: Double = 0.0,
    val serviceChargeAmount: Double = 0.0,
    val taxPercentage: Double = 0.0,
    val occupiedTablePrompt: RestaurantTable? = null,
    val toastMessage: String? = null,
    val isImageView: Boolean = true,
    val isKotPrintEnabled: Boolean = true,
    val isAutoPrintBillEnabled: Boolean = false
) {
    val totalItemCount: Int
        get() = cart.values.sumOf { it.quantity }

    val cartSubtotal: Double
        get() = cart.values.sumOf { it.totalPrice }

    val taxAmount: Double
        get() = (cartSubtotal - discountAmount).coerceAtLeast(0.0) * (taxPercentage / 100.0)

    val grandTotal: Double
        get() = (cartSubtotal - discountAmount + taxAmount + serviceChargeAmount).coerceAtLeast(0.0)

    val isCustomerPhoneValid: Boolean
        get() = customerPhone.length == 10 && customerPhone.all { it.isDigit() }
}

class AddOrderViewModel(
    private val menuRepo: MenuRepository = MenuRepository,
    private val orderRepo: OrderRepository = OrderRepository,
    private val customerRepo: CustomerRepository = CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddOrderUiState())
    val uiState: StateFlow<AddOrderUiState> = _uiState.asStateFlow()

    private val _lastCompletedOrder = MutableStateFlow<ClosedOrder?>(null)
    val lastCompletedOrder: StateFlow<ClosedOrder?> = _lastCompletedOrder.asStateFlow()

    // Tracks previously printed items/quantities for incremental KOT delta printing
    private var originalHeldOrderItems: Map<String, Int> = emptyMap()

    var prefsManager: PreferencesManager? = null
        set(value) {
            field = value
            if (value != null) {
                observePreferences(value)
            }
        }

    private fun observePreferences(prefs: PreferencesManager) {
        viewModelScope.launch {
            prefs.isImageViewEnabled.collect { enabled ->
                _uiState.update { it.copy(isImageView = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.kotPrintEnabled.collect { enabled ->
                _uiState.update { it.copy(isKotPrintEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.autoPrintBill.collect { enabled ->
                _uiState.update { it.copy(isAutoPrintBillEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.defaultTaxPercent.collect { tax ->
                if (_uiState.value.editingOrderId == null && _uiState.value.cart.isEmpty()) {
                    _uiState.update { it.copy(taxPercentage = tax) }
                }
            }
        }
        viewModelScope.launch {
            prefs.defaultOrderType.collect { typeStr ->
                if (_uiState.value.editingOrderId == null && _uiState.value.cart.isEmpty()) {
                    val type = try { OrderType.valueOf(typeStr) } catch (e: Exception) { OrderType.DINE_IN }
                    _uiState.update { it.copy(orderType = type) }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            menuRepo.menuItems.collect { items ->
                val allCats = (listOf("All", "Favourite") + (menuRepo.categoriesFlow.value + items.map { it.category })
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != "All" && it != "None" })
                    .distinct()
                _uiState.update { it.copy(categories = allCats) }
                filterMenuItems()
            }
        }
        viewModelScope.launch {
            menuRepo.categoriesFlow.collect { cats ->
                val allCats = (listOf("All", "Favourite") + (cats + menuRepo.menuItems.value.map { it.category })
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != "All" && it != "None" })
                    .distinct()
                _uiState.update { it.copy(categories = allCats) }
            }
        }
        viewModelScope.launch {
            orderRepo.tables.collect { tableList ->
                _uiState.update { current ->
                    val defaultTable = if (current.tableNumber.isBlank()) {
                        tableList.firstOrNull { !it.isOccupied }?.number ?: ""
                    } else current.tableNumber
                    current.copy(tables = tableList, tableNumber = defaultTable)
                }
            }
        }
        viewModelScope.launch {
            menuRepo.fetchMenuItemsFromBackend()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterMenuItems()
    }

    fun onCategorySelect(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        filterMenuItems()
    }

    private fun filterMenuItems() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val category = _uiState.value.selectedCategory

        val allItems = menuRepo.menuItems.value

        val filtered = allItems.filter { item ->
            val matchesCategory = when (category) {
                "All" -> true
                "Favourite" -> item.isFavourite
                else -> item.category.equals(category, ignoreCase = true) || item.name.contains(category, ignoreCase = true)
            }

            val matchesQuery = query.isEmpty() ||
                    item.name.lowercase().contains(query) ||
                    item.code.lowercase().contains(query) ||
                    item.category.lowercase().contains(query)

            matchesCategory && matchesQuery
        }

        _uiState.update { it.copy(menuItems = filtered) }
    }

    fun addToCart(item: POSMenuItem, variant: MenuItemVariant? = null) {
        val cartKey = if (variant != null) "${item.id}__${variant.name}" else item.id
        val cartMenuItem = if (variant != null) {
            item.copy(name = "${item.name} (${variant.name})", price = variant.price)
        } else {
            item
        }
        _uiState.update { state ->
            val existing = state.cart[cartKey]
            val updatedCart = state.cart.toMutableMap()
            if (existing != null) {
                updatedCart[cartKey] = existing.copy(quantity = existing.quantity + 1)
            } else {
                updatedCart[cartKey] = CartItem(menuItem = cartMenuItem, quantity = 1)
            }
            state.copy(cart = updatedCart)
        }
    }

    fun incrementQuantity(itemId: String) {
        _uiState.update { state ->
            val existing = state.cart[itemId] ?: return@update state
            val updatedCart = state.cart.toMutableMap()
            updatedCart[itemId] = existing.copy(quantity = existing.quantity + 1)
            state.copy(cart = updatedCart)
        }
    }

    fun decrementQuantity(itemId: String) {
        _uiState.update { state ->
            val existing = state.cart[itemId] ?: return@update state
            val updatedCart = state.cart.toMutableMap()
            if (existing.quantity > 1) {
                updatedCart[itemId] = existing.copy(quantity = existing.quantity - 1)
            } else {
                updatedCart.remove(itemId)
            }
            state.copy(cart = updatedCart)
        }
    }

    fun setOrderType(type: OrderType) {
        _uiState.update { it.copy(orderType = type) }
    }

    fun toggleTableDropdown() {
        _uiState.update { it.copy(isTableDropdownExpanded = !it.isTableDropdownExpanded) }
    }

    fun selectTable(table: RestaurantTable) {
        if (table.isOccupied) {
            if (_uiState.value.editingOrderId == table.activeOrderId && table.activeOrderId != null) {
                _uiState.update { it.copy(tableNumber = table.number, isTableDropdownExpanded = false) }
            } else {
                _uiState.update { it.copy(occupiedTablePrompt = table) }
            }
            return
        }
        _uiState.update {
            it.copy(
                tableNumber = table.number,
                isTableDropdownExpanded = false,
                occupiedTablePrompt = null
            )
        }
    }

    fun dismissOccupiedPrompt() {
        _uiState.update { it.copy(occupiedTablePrompt = null) }
    }

    fun addCustomTable(number: String) {
        val trimmed = number.trim()
        if (trimmed.isNotBlank()) {
            orderRepo.addTable(trimmed)
            _uiState.update { current ->
                val newTable = RestaurantTable(number = trimmed, isOccupied = false)
                val updated = (current.tables + newTable).distinctBy { it.number }.sortedBy { it.number }
                current.copy(
                    tables = updated,
                    tableNumber = trimmed,
                    isTableDropdownExpanded = false
                )
            }
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun openCustomerSheet() {
        _uiState.update { it.copy(showCustomerSheet = true) }
    }

    fun dismissCustomerSheet() {
        _uiState.update { it.copy(showCustomerSheet = false) }
    }

    fun openDiscountDialog() {
        _uiState.update { it.copy(showDiscountDialog = true) }
    }

    fun dismissDiscountDialog() {
        _uiState.update { it.copy(showDiscountDialog = false) }
    }

    fun applyDiscount(amount: Double) {
        _uiState.update { it.copy(discountAmount = amount, showDiscountDialog = false) }
    }

    fun openServiceChargeDialog() {
        _uiState.update { it.copy(showServiceChargeDialog = true) }
    }

    fun dismissServiceChargeDialog() {
        _uiState.update { it.copy(showServiceChargeDialog = false) }
    }

    fun applyServiceCharge(amount: Double) {
        _uiState.update { it.copy(serviceChargeAmount = amount, showServiceChargeDialog = false) }
    }

    fun onCustomerPhoneChange(phone: String) {
        if (phone.length <= 10 && phone.all { it.isDigit() }) {
            _uiState.update { it.copy(customerPhone = phone) }
            if (phone.length == 10) {
                val existing = customerRepo.findCustomerByMobile(phone)
                if (existing != null && _uiState.value.customerName.isBlank()) {
                    _uiState.update { it.copy(customerName = existing.name, saveAsRegularCustomer = existing.isRegular) }
                }
            }
        }
    }

    fun onCustomerNameChange(name: String) {
        _uiState.update { it.copy(customerName = name) }
    }

    fun onSaveAsRegularToggle(checked: Boolean) {
        _uiState.update { it.copy(saveAsRegularCustomer = checked) }
    }

    fun saveCustomerDetails() {
        val phone = _uiState.value.customerPhone.trim()
        val name = _uiState.value.customerName.trim().ifEmpty { "Customer" }

        if (phone.length != 10) {
            _uiState.update { it.copy(toastMessage = "Please enter a valid 10-digit mobile number") }
            return
        }

        val customer = customerRepo.saveOrUpdateCustomer(
            name = name,
            mobileNumber = phone,
            isRegular = _uiState.value.saveAsRegularCustomer
        )

        _uiState.update {
            it.copy(
                attachedCustomer = customer,
                customerName = name,
                customerPhone = phone,
                showCustomerSheet = false
            )
        }
    }

    fun loadOrderForEditing(orderId: String) {
        viewModelScope.launch {
            val heldOrder = orderRepo.getOrderById(orderId)
            if (heldOrder != null) {
                originalHeldOrderItems = heldOrder.items.associate { it.name to it.quantity }
                val newCart = mutableMapOf<String, CartItem>()
                heldOrder.items.forEach { orderItem ->
                    val posItem = menuRepo.menuItems.value.find { it.name == orderItem.name }
                        ?: POSMenuItem(
                            id = UUID.randomUUID().toString(),
                            code = "00",
                            name = orderItem.name,
                            category = "Custom",
                            price = orderItem.price
                        )
                    newCart[posItem.id] = CartItem(posItem, orderItem.quantity)
                }

                val restoredCust = if (!heldOrder.customerPhone.isNullOrBlank()) {
                    customerRepo.findCustomerByMobile(heldOrder.customerPhone)
                        ?: Customer(
                            id = "CUST-${heldOrder.customerPhone}",
                            name = heldOrder.customerName ?: "Customer",
                            mobileNumber = heldOrder.customerPhone,
                            isRegular = false
                        )
                } else null

                _uiState.update {
                    it.copy(
                        editingOrderId = heldOrder.orderId,
                        tableNumber = heldOrder.tableNumber ?: "G1",
                        cart = newCart,
                        customerName = heldOrder.customerName.orEmpty(),
                        customerPhone = heldOrder.customerPhone.orEmpty(),
                        attachedCustomer = restoredCust,
                        occupiedTablePrompt = null
                    )
                }
            } else {
                val closedOrder = orderRepo.getClosedOrderById(orderId)
                if (closedOrder != null) {
                    originalHeldOrderItems = closedOrder.items.associate { it.name to it.quantity }
                    val newCart = mutableMapOf<String, CartItem>()
                    closedOrder.items.forEach { orderItem ->
                        val posItem = menuRepo.menuItems.value.find { it.name == orderItem.name }
                            ?: POSMenuItem(
                                id = UUID.randomUUID().toString(),
                                code = "00",
                                name = orderItem.name,
                                category = "Custom",
                                price = orderItem.price
                            )
                        newCart[posItem.id] = CartItem(posItem, orderItem.quantity)
                    }

                    _uiState.update {
                        it.copy(
                            editingOrderId = closedOrder.orderId,
                            tableNumber = closedOrder.tableNumber ?: "G1",
                            cart = newCart,
                            occupiedTablePrompt = null
                        )
                    }
                }
            }
        }
    }

    fun saveAndHoldOrder(onSuccess: () -> Unit) {
        val currentCart = _uiState.value.cart
        if (currentCart.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Cart is empty! Add items first.") }
            return
        }

        viewModelScope.launch {
            // Guard: Prevent creating new conflicting order on an occupied table
            if (_uiState.value.orderType == OrderType.DINE_IN && _uiState.value.editingOrderId == null) {
                val occupiedTable = _uiState.value.tables.find {
                    it.number.equals(_uiState.value.tableNumber, ignoreCase = true) && it.isOccupied
                }
                if (occupiedTable != null) {
                    _uiState.update { it.copy(occupiedTablePrompt = occupiedTable) }
                    return@launch
                }
            }

            val isEditing = _uiState.value.editingOrderId != null
            val existingHeld = _uiState.value.editingOrderId?.let { orderRepo.getOrderById(it) }
            val orderId = _uiState.value.editingOrderId ?: java.util.UUID.randomUUID().toString()
            val kotNum = existingHeld?.kotNumber ?: orderRepo.getNextKotNumber()
            val orderItems = currentCart.values.map {
                OrderItem(
                    id = it.menuItem.id,
                    name = it.menuItem.name,
                    quantity = it.quantity,
                    price = it.menuItem.price
                )
            }

            // Calculate incremental delta items: only newly added items or increased quantities!
            val deltaItems = mutableListOf<OrderItem>()
            if (!isEditing) {
                deltaItems.addAll(orderItems)
            } else {
                currentCart.values.forEach { cartItem ->
                    val prevQty = originalHeldOrderItems[cartItem.menuItem.name] ?: 0
                    val diff = cartItem.quantity - prevQty
                    if (diff > 0) {
                        deltaItems.add(
                            OrderItem(
                                id = cartItem.menuItem.id,
                                name = cartItem.menuItem.name,
                                quantity = diff,
                                price = cartItem.menuItem.price
                            )
                        )
                    }
                }
            }

            val heldOrder = HeldOrder(
                orderId = orderId,
                kotNumber = kotNum,
                tableNumber = if (_uiState.value.orderType == OrderType.DINE_IN) _uiState.value.tableNumber.ifBlank { null } else null,
                totalAmount = _uiState.value.cartSubtotal,
                items = orderItems,
                status = OrderStatus.ON_HOLD,
                orderType = _uiState.value.orderType.name,
                customerName = _uiState.value.customerName.ifBlank { null },
                customerPhone = _uiState.value.customerPhone.ifBlank { null }
            )

            orderRepo.saveOrder(heldOrder)

            // Print KOT Ticket ONLY if enabled and there are new/increased items!
            var printMsg = if (isEditing) "Order $kotNum updated." else "KOT $kotNum saved on hold."

            if (_uiState.value.isKotPrintEnabled && deltaItems.isNotEmpty()) {
                if (BluetoothPrinterService.printerState.value == PrinterState.ONLINE) {
                    val bytes = EscPosPrinter.buildKotReceipt(
                        order = heldOrder,
                        itemsToPrint = deltaItems,
                        isAddOn = isEditing
                    )
                    val printResult = BluetoothPrinterService.printData(bytes)
                    printMsg = if (printResult.isSuccess) {
                        if (isEditing) "Order $orderId updated & Add-on KOT (${deltaItems.sumOf { it.quantity }} item) printed!"
                        else "KOT $orderId saved & printed!"
                    } else {
                        "KOT $orderId saved! KOT print failed (Retry available)."
                    }
                }
            } else if (!_uiState.value.isKotPrintEnabled) {
                printMsg = if (isEditing) "Order $orderId updated (KOT printing disabled in preferences)." else "KOT $orderId saved (Printing disabled)."
            } else if (isEditing) {
                printMsg = "Order $orderId updated (No new items for kitchen)."
            }

            originalHeldOrderItems = emptyMap()

            _uiState.update {
                it.copy(
                    cart = emptyMap(),
                    editingOrderId = null,
                    occupiedTablePrompt = null
                )
            }
            onSuccess()
        }
    }

    fun completeBillingOrder(shouldPrint: Boolean = false, onSuccess: (String) -> Unit) {
        val currentCart = _uiState.value.cart
        if (currentCart.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Cart is empty! Add items first.") }
            return
        }

        if (_uiState.value.orderType == OrderType.DINE_IN && _uiState.value.tableNumber.isBlank()) {
            _uiState.update { it.copy(toastMessage = "Please select a table for Dine-In order.") }
            return
        }

        viewModelScope.launch {
            val invoiceId = orderRepo.getNextInvoiceNumber()
            val orderItems = currentCart.values.map {
                OrderItem(
                    id = it.menuItem.id,
                    name = it.menuItem.name,
                    quantity = it.quantity,
                    price = it.menuItem.price
                )
            }

            // If this was an existing held order for a table, transition its existing UUID directly to COMPLETED
            val heldOrderMatch = _uiState.value.editingOrderId?.let { orderRepo.getOrderById(it) }
                ?: orderRepo.onHoldOrders.value.find {
                    _uiState.value.orderType == OrderType.DINE_IN &&
                    it.tableNumber?.equals(_uiState.value.tableNumber, ignoreCase = true) == true
                }
            val heldId = _uiState.value.editingOrderId ?: heldOrderMatch?.orderId

            val effectiveOrderId = heldId ?: java.util.UUID.randomUUID().toString()
            // Preserve KOT cut timestamp so pre-midnight KOTs count in the KOT's date
            val salesTimestamp = heldOrderMatch?.timestamp ?: System.currentTimeMillis()

            val closedOrder = ClosedOrder(
                orderId = effectiveOrderId,
                invoiceNumber = invoiceId,
                tableNumber = if (_uiState.value.orderType == OrderType.DINE_IN) _uiState.value.tableNumber else null,
                subtotal = _uiState.value.cartSubtotal,
                discountAmount = _uiState.value.discountAmount,
                taxAmount = _uiState.value.taxAmount,
                serviceCharge = _uiState.value.serviceChargeAmount,
                totalAmount = _uiState.value.grandTotal,
                items = orderItems,
                status = OrderStatus.COMPLETED,
                completedTimestamp = salesTimestamp,
                paymentMethod = _uiState.value.paymentMethod.name,
                orderType = _uiState.value.orderType.name,
                customerName = _uiState.value.customerName.ifBlank { null },
                customerPhone = _uiState.value.customerPhone.ifBlank { null }
            )

            _lastCompletedOrder.value = closedOrder
            orderRepo.saveClosedOrder(closedOrder)

            if (heldId != null && heldId != effectiveOrderId) {
                orderRepo.deleteOrder(heldId)
            }

            if (shouldPrint && BluetoothPrinterService.printerState.value == PrinterState.ONLINE) {
                val bytes = EscPosPrinter.buildOrderBillReceipt(closedOrder)
                BluetoothPrinterService.printData(bytes)
            }

            originalHeldOrderItems = emptyMap()

            _uiState.update {
                it.copy(
                    cart = emptyMap(),
                    editingOrderId = null,
                    occupiedTablePrompt = null
                )
            }
            onSuccess(invoiceId)
        }
    }

    fun handleBackClick(onConfirmBack: () -> Unit) {
        if (_uiState.value.cart.isNotEmpty()) {
            _uiState.update { it.copy(showUnsavedDialog = true) }
        } else {
            onConfirmBack()
        }
    }

    fun dismissUnsavedDialog() {
        _uiState.update { it.copy(showUnsavedDialog = false) }
    }

    fun confirmDiscardCart(onConfirmBack: () -> Unit) {
        originalHeldOrderItems = emptyMap()
        _uiState.update { it.copy(cart = emptyMap(), showUnsavedDialog = false) }
        onConfirmBack()
    }

    fun resetForNewOrder() {
        originalHeldOrderItems = emptyMap()
        val defaultTax = prefsManager?.let { 0.0 } ?: 0.0
        _uiState.update {
            it.copy(
                editingOrderId = null,
                cart = emptyMap(),
                tableNumber = "",
                customerName = "",
                customerPhone = "",
                attachedCustomer = null,
                discountAmount = 0.0,
                serviceChargeAmount = 0.0,
                occupiedTablePrompt = null,
                searchQuery = "",
                selectedCategory = "All"
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}