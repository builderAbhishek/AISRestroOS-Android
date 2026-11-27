package com.ais.restroos.feature.orders.closed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.ClosedOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClosedOrdersFilter {
    ALL_TIME,
    LAST_60_MINS
}

data class ClosedOrdersUiState(
    val orders: List<ClosedOrder> = emptyList(),
    val activeFilter: ClosedOrdersFilter = ClosedOrdersFilter.LAST_60_MINS,
    val selectedCount: Int = 0,
    val orderToEdit: ClosedOrder? = null,
    val orderToPrint: ClosedOrder? = null,
    val userToastMessage: String? = null,
    val isLoading: Boolean = false
)

class ClosedOrdersViewModel(
    private val repository: OrderRepository = OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClosedOrdersUiState())
    val uiState: StateFlow<ClosedOrdersUiState> = _uiState.asStateFlow()

    init {
        observeClosedOrders()
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.fetchOrdersFromBackend()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeClosedOrders() {
        viewModelScope.launch {
            repository.closedOrders.collect { allClosed ->
                val now = System.currentTimeMillis()
                val sixtyMinsAgo = now - (60 * 60 * 1000)

                val filtered = when (_uiState.value.activeFilter) {
                    ClosedOrdersFilter.ALL_TIME -> allClosed
                    ClosedOrdersFilter.LAST_60_MINS -> allClosed.filter { it.completedTimestamp >= sixtyMinsAgo }
                }

                _uiState.update { state ->
                    state.copy(
                        orders = filtered,
                        selectedCount = filtered.count { it.isSelected }
                    )
                }
            }
        }
    }

    fun setFilter(filter: ClosedOrdersFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
        observeClosedOrders()
    }

    fun toggleOrderSelection(orderId: String) {
        repository.toggleClosedOrderSelection(orderId)
    }

    fun selectAllOrders(selectAll: Boolean) {
        repository.selectAllClosedOrders(selectAll)
    }

    fun requestEditClosedOrder(order: ClosedOrder) {
        _uiState.update { it.copy(orderToEdit = order) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(orderToEdit = null) }
    }

    fun requestPrintBill(order: ClosedOrder) {
        _uiState.update {
            it.copy(
                orderToPrint = order,
                userToastMessage = "Printing bill for ${order.invoiceNumber}..."
            )
        }
    }

    fun dismissPrintDialog() {
        _uiState.update { it.copy(orderToPrint = null) }
    }

    fun clearToast() {
        _uiState.update { it.copy(userToastMessage = null) }
    }
}