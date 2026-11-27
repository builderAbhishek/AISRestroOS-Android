package com.ais.restroos.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.printer.BluetoothPrinterService
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.repository.MenuRepository
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.DashboardOverview
import com.ais.restroos.domain.model.PrinterState
import com.ais.restroos.domain.model.RestaurantBranch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val printerState: PrinterState = PrinterState.OFFLINE,
    val isRefreshingPrinter: Boolean = false,
    val overview: DashboardOverview = DashboardOverview(),
    val branches: List<RestaurantBranch> = listOf(
        RestaurantBranch("1", "My Restaurant", "Main Branch", true)
    ),
    val selectedBranch: RestaurantBranch = RestaurantBranch("1", "My Restaurant", "Main Branch", true),
    val showBranchSelectorDialog: Boolean = false,
    val isLoadingData: Boolean = false,
    val messageToast: String? = null
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    var prefsManager: PreferencesManager? = null
        set(value) {
            field = value
            if (value != null) {
                observeRestaurantInfo(value)
            }
        }

    init {
        observeLiveMetrics()
        observePrinterConnectionState()
    }

    private fun observeRestaurantInfo(prefs: PreferencesManager) {
        viewModelScope.launch {
            prefs.restaurantName.collect { restroName ->
                val cleanName = restroName.ifBlank { "My Restaurant" }
                _uiState.update { state ->
                    val updatedBranch = state.selectedBranch.copy(name = cleanName)
                    state.copy(
                        selectedBranch = updatedBranch,
                        branches = listOf(updatedBranch)
                    )
                }
            }
        }
    }

    private fun observePrinterConnectionState() {
        viewModelScope.launch {
            BluetoothPrinterService.printerState.collect { state ->
                _uiState.update { it.copy(printerState = state) }
            }
        }
    }

    private fun observeLiveMetrics() {
        viewModelScope.launch {
            combine(
                OrderRepository.closedOrders,
                OrderRepository.onHoldOrders,
                MenuRepository.menuItems
            ) { closed, _, _ ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val todayStart = cal.timeInMillis
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                val yesterdayStart = cal.timeInMillis

                val todayClosed = closed.filter { it.completedTimestamp >= todayStart }
                val yesterdayClosed = closed.filter { it.completedTimestamp in yesterdayStart until todayStart }

                DashboardOverview(
                    todaySales = todayClosed.sumOf { it.totalAmount },
                    todayOrders = todayClosed.size,
                    yesterdaySales = yesterdayClosed.sumOf { it.totalAmount },
                    yesterdayOrders = yesterdayClosed.size
                )
            }.collect { metrics ->
                _uiState.update { it.copy(overview = metrics) }
            }
        }
    }

    fun loadBackendData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingData = true) }
            try {
                MenuRepository.fetchMenuItemsFromBackend()
                OrderRepository.fetchOrdersFromBackend()
                val profileResp = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getRestaurantProfile()
                }
                if (profileResp.isSuccessful && profileResp.body()?.status == "success") {
                    profileResp.body()?.data?.let { profile ->
                        if (profile.name.isNotBlank()) {
                            prefsManager?.updateRestaurantName(profile.name)
                            _uiState.update { state ->
                                val updated = state.selectedBranch.copy(id = profile.id.ifBlank { "1" }, name = profile.name)
                                state.copy(selectedBranch = updated, branches = listOf(updated))
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
            _uiState.update { it.copy(isLoadingData = false) }
        }
    }

    fun triggerSilentPrinterAutoConnect(context: Context) {
        viewModelScope.launch {
            BluetoothPrinterService.autoConnectSavedPrinter(context)
        }
    }

    fun refreshPrinterStatus(context: Context? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingPrinter = true) }
            if (context != null && BluetoothPrinterService.printerState.value != PrinterState.ONLINE) {
                BluetoothPrinterService.autoConnectSavedPrinter(context)
            }
            delay(500)
            val currentState = BluetoothPrinterService.printerState.value
            _uiState.update {
                it.copy(
                    isRefreshingPrinter = false,
                    printerState = currentState
                )
            }
        }
    }

    fun togglePrinterState() {
        val nextState = when (_uiState.value.printerState) {
            PrinterState.ONLINE -> PrinterState.OFFLINE
            PrinterState.OFFLINE -> PrinterState.CONNECTING
            PrinterState.CONNECTING -> PrinterState.ERROR
            PrinterState.ERROR -> PrinterState.ONLINE
        }
        _uiState.update { it.copy(printerState = nextState) }
    }

    fun onBranchClick() {
        _uiState.update { it.copy(showBranchSelectorDialog = true) }
    }

    fun dismissBranchSelector() {
        _uiState.update { it.copy(showBranchSelectorDialog = false) }
    }

    fun selectBranch(branch: RestaurantBranch) {
        _uiState.update {
            it.copy(
                selectedBranch = branch,
                branches = it.branches.map { b -> b.copy(isSelected = b.id == branch.id) },
                showBranchSelectorDialog = false,
                messageToast = "Switched to ${branch.name}"
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(messageToast = null) }
    }
}