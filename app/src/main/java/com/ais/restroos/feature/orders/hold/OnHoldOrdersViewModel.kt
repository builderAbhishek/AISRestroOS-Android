package com.ais.restroos.feature.orders.hold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.HeldOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnHoldOrdersUiState(
    val orders: List<HeldOrder> = emptyList(),
    val isLoading: Boolean = false,
    val orderToDelete: HeldOrder? = null,
    val userMessage: String? = null
)

class OnHoldOrdersViewModel(
    private val repository: OrderRepository = OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnHoldOrdersUiState())
    val uiState: StateFlow<OnHoldOrdersUiState> = _uiState.asStateFlow()

    init {
        observeOnHoldOrders()
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.fetchOrdersFromBackend()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeOnHoldOrders() {
        viewModelScope.launch {
            repository.onHoldOrders.collect { orderList ->
                _uiState.update { it.copy(orders = orderList) }
            }
        }
    }

    fun requestDeleteOrder(order: HeldOrder) {
        _uiState.update { it.copy(orderToDelete = order) }
    }

    fun confirmDeleteOrder() {
        val targetOrder = _uiState.value.orderToDelete ?: return
        viewModelScope.launch {
            repository.deleteOrder(targetOrder.orderId)
            _uiState.update {
                it.copy(
                    orderToDelete = null,
                    userMessage = "Order ${targetOrder.orderId} deleted successfully"
                )
            }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(orderToDelete = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}