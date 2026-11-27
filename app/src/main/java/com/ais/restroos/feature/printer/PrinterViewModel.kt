package com.ais.restroos.feature.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.printer.BluetoothPrinterService
import com.ais.restroos.data.printer.EscPosPrinter
import com.ais.restroos.data.printer.PrinterDeviceInfo
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.DeviceSyncDto
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.HeldOrder
import com.ais.restroos.domain.model.PrinterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrinterUiState(
    val pairedDevices: List<PrinterDeviceInfo> = emptyList(),
    val selectedDevice: PrinterDeviceInfo? = null,
    val paperWidth: String = "58mm",
    val autoCut: Boolean = false,
    val copies: Int = 1,
    val isScanning: Boolean = false,
    val connectionState: PrinterState = PrinterState.OFFLINE,
    val userMessage: String? = null
)

class PrinterViewModel(
    private var _prefsManager: PreferencesManager? = null
) : ViewModel() {

    var prefsManager: PreferencesManager?
        get() = _prefsManager
        set(value) {
            _prefsManager = value
            loadSavedPrinterConfig()
        }

    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()

    init {
        observePrinterState()
        loadSavedPrinterConfig()
    }

    private fun observePrinterState() {
        viewModelScope.launch {
            BluetoothPrinterService.printerState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }

    private fun loadSavedPrinterConfig() {
        viewModelScope.launch {
            prefsManager?.let { prefs ->
                val mac = prefs.printerMac.firstOrNull()
                val name = prefs.printerName.firstOrNull()
                val width = prefs.printerPaperWidth.firstOrNull() ?: "58mm"
                val cut = prefs.printerAutoCut.firstOrNull() ?: false
                val copies = prefs.printerCopies.firstOrNull() ?: 1

                val savedDevice = if (!mac.isNullOrBlank() && !name.isNullOrBlank()) {
                    PrinterDeviceInfo(name = name, macAddress = mac, isPaired = true)
                } else null

                _uiState.update {
                    it.copy(
                        selectedDevice = savedDevice,
                        paperWidth = width,
                        autoCut = cut,
                        copies = copies
                    )
                }
            }
            refreshDevices()
        }
    }

    fun refreshDevices() {
        if (!BluetoothPrinterService.isBluetoothAvailable()) {
            _uiState.update { it.copy(userMessage = "Bluetooth is disabled or not available") }
            return
        }

        val devices = BluetoothPrinterService.getPairedDevices()
        _uiState.update { it.copy(pairedDevices = devices) }
    }

    fun selectDevice(device: PrinterDeviceInfo) {
        _uiState.update { it.copy(selectedDevice = device) }
    }

    fun updatePaperWidth(width: String) {
        _uiState.update { it.copy(paperWidth = width) }
        saveConfig()
    }

    fun toggleAutoCut(enabled: Boolean) {
        _uiState.update { it.copy(autoCut = enabled) }
        saveConfig()
    }

    private fun saveConfig() {
        viewModelScope.launch {
            val device = _uiState.value.selectedDevice
            if (device != null) {
                prefsManager?.savePrinterConfig(
                    macAddress = device.macAddress,
                    name = device.name,
                    paperWidth = _uiState.value.paperWidth,
                    autoCut = _uiState.value.autoCut,
                    copies = _uiState.value.copies
                )
            }
        }
    }

    fun connectSelectedPrinter() {
        val device = _uiState.value.selectedDevice
        if (device == null) {
            _uiState.update { it.copy(userMessage = "Please select a printer from the list") }
            return
        }

        viewModelScope.launch {
            saveConfig()
            val result = BluetoothPrinterService.connectToPrinter(device.macAddress, device.name)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Connected to ${device.name}") }
                // Persist device data online in database
                try {
                    val dto = DeviceSyncDto(
                        id = "DEV-" + device.macAddress.replace(":", "").takeLast(8).uppercase(),
                        device_name = device.name,
                        mac_address = device.macAddress,
                        device_type = "PRINTER",
                        paper_width = _uiState.value.paperWidth
                    )
                    RetrofitClient.apiService.createDevice(dto)
                } catch (_: Exception) {}
            } else {
                _uiState.update { it.copy(userMessage = result.exceptionOrNull()?.message ?: "Connection failed") }
            }
        }
    }

    fun disconnectPrinter() {
        BluetoothPrinterService.disconnect()
        _uiState.update { it.copy(userMessage = "Printer disconnected") }
    }

    fun testPrint() {
        viewModelScope.launch {
            val bytes = EscPosPrinter.buildTestReceipt(paperWidth = _uiState.value.paperWidth)
            val result = BluetoothPrinterService.printData(bytes)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Test print bytes transmitted successfully!") }
            } else {
                _uiState.update { it.copy(userMessage = "Test print failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun printOrderBill(order: ClosedOrder, restaurantName: String = "Siddharth Family Resturent") {
        viewModelScope.launch {
            val bytes = EscPosPrinter.buildOrderBillReceipt(
                order = order,
                restaurantName = restaurantName,
                paperWidth = _uiState.value.paperWidth
            )
            val result = BluetoothPrinterService.printData(bytes)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Tax Bill ${order.invoiceNumber} printed!") }
            } else {
                _uiState.update { it.copy(userMessage = "Printing failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun printKotTicket(order: HeldOrder, restaurantName: String = "Siddharth Family Resturent") {
        viewModelScope.launch {
            val bytes = EscPosPrinter.buildKotReceipt(
                order = order,
                restaurantName = restaurantName,
                paperWidth = _uiState.value.paperWidth
            )
            val result = BluetoothPrinterService.printData(bytes)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "KOT Ticket ${order.orderId} printed!") }
            } else {
                _uiState.update { it.copy(userMessage = "KOT print failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}