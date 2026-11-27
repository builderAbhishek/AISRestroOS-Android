package com.ais.restroos.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.ais.restroos.domain.model.PrinterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class PrinterDeviceInfo(
    val name: String,
    val macAddress: String,
    val isPaired: Boolean = true,
    val isConnected: Boolean = false
)

object BluetoothPrinterService {

    // Standard Bluetooth Classic SPP UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _printerState = MutableStateFlow(PrinterState.OFFLINE)
    val printerState: StateFlow<PrinterState> = _printerState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<PrinterDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices.map { device ->
                PrinterDeviceInfo(
                    name = device.name ?: "Unknown Printer",
                    macAddress = device.address,
                    isPaired = true,
                    isConnected = device.address == _connectedDeviceName.value
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToPrinter(macAddress: String, name: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _printerState.value = PrinterState.CONNECTING
                disconnect()

                val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth not supported on device"))
                if (!adapter.isEnabled) {
                    _printerState.value = PrinterState.OFFLINE
                    return@withContext Result.failure(Exception("Bluetooth is turned off"))
                }

                val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                adapter.cancelDiscovery()

                socket.connect()
                activeSocket = socket
                outputStream = socket.outputStream

                _printerState.value = PrinterState.ONLINE
                _connectedDeviceName.value = name

                Result.success(Unit)
            } catch (e: Exception) {
                disconnect()
                _printerState.value = PrinterState.OFFLINE
                Result.failure(Exception("Failed to connect to printer at $macAddress: ${e.message}"))
            }
        }
    }

    suspend fun autoConnectSavedPrinter(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                if (_printerState.value == PrinterState.ONLINE && activeSocket?.isConnected == true) {
                    return@withContext
                }

                val prefs = com.ais.restroos.data.local.PreferencesManager(context)
                var savedMac = prefs.printerMac.firstOrNull()
                var savedName = prefs.printerName.firstOrNull() ?: "Thermal Printer"

                if (savedMac.isNullOrBlank()) {
                    // Try to restore printer device from online backend if available
                    try {
                        val token = prefs.getAuthToken()
                        if (!token.isNullOrBlank()) {
                            val resp = com.ais.restroos.data.remote.RetrofitClient.apiService.getDevices()
                            if (resp.isSuccessful) {
                                val onlinePrinter = resp.body()?.data?.firstOrNull {
                                    it.device_type.equals("PRINTER", ignoreCase = true) && !it.mac_address.isNullOrBlank()
                                }
                                if (onlinePrinter?.mac_address != null) {
                                    savedMac = onlinePrinter.mac_address
                                    savedName = onlinePrinter.device_name
                                    prefs.savePrinterConfig(
                                        macAddress = onlinePrinter.mac_address,
                                        name = onlinePrinter.device_name,
                                        paperWidth = onlinePrinter.paper_width
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (savedMac.isNullOrBlank()) {
                    _printerState.value = PrinterState.OFFLINE
                    return@withContext
                }

                if (!isBluetoothAvailable()) {
                    _printerState.value = PrinterState.OFFLINE
                    return@withContext
                }

                _printerState.value = PrinterState.CONNECTING
                val res = connectToPrinter(savedMac, savedName)
                if (!res.isSuccess) {
                    _printerState.value = PrinterState.OFFLINE
                }
            } catch (_: Exception) {
                _printerState.value = PrinterState.OFFLINE
            }
        }
    }

    suspend fun printData(bytes: ByteArray): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val stream = outputStream
                if (stream == null || activeSocket?.isConnected != true) {
                    _printerState.value = PrinterState.OFFLINE
                    return@withContext Result.failure(Exception("Printer is disconnected"))
                }

                val chunkSize = 1024
                var offset = 0
                while (offset < bytes.size) {
                    val count = minOf(chunkSize, bytes.size - offset)
                    stream.write(bytes, offset, count)
                    stream.flush()
                    offset += count
                    if (offset < bytes.size) {
                        Thread.sleep(15)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                _printerState.value = PrinterState.ERROR
                Result.failure(Exception("Print write error: ${e.message}"))
            }
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            activeSocket?.close()
        } catch (_: Exception) { }
        outputStream = null
        activeSocket = null
        _printerState.value = PrinterState.OFFLINE
        _connectedDeviceName.value = null
    }
}