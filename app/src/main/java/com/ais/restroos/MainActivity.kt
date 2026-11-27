package com.ais.restroos

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.ais.restroos.core.navigation.MainAppNavigation
import com.ais.restroos.core.permission.PermissionHelper
import com.ais.restroos.core.permission.PermissionRationaleDialog
import com.ais.restroos.core.theme.AISRestroOSTheme

class MainActivity : ComponentActivity() {

    private var showPermissionDialog by mutableStateOf(false)
    private var isPermanentlyDenied by mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val ungranted = permissionsResult.filterValues { !it }.keys
        if (ungranted.isEmpty()) {
            showPermissionDialog = false
            Toast.makeText(this, "All POS permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            val permanentlyDenied = ungranted.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }
            isPermanentlyDenied = permanentlyDenied
            showPermissionDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Immediately hydrate cached token synchronously to prevent 401 on startup
        val cachedToken = com.ais.restroos.data.local.PreferencesManager.getCachedToken(applicationContext)
        if (!cachedToken.isNullOrBlank()) {
            com.ais.restroos.data.remote.RetrofitClient.updateAuthToken(cachedToken)
        }

        // Initialize SQLite Offline Repositories and WorkManager Background Sync
        com.ais.restroos.data.repository.OrderRepository.initialize(applicationContext)
        com.ais.restroos.data.repository.MenuRepository.initialize(applicationContext)
        com.ais.restroos.data.sync.SyncManager.schedulePeriodicSync(applicationContext)

        // Eagerly hydrate Retrofit auth token from local storage & start real-time sync
        val prefs = com.ais.restroos.data.local.PreferencesManager(applicationContext)
        lifecycleScope.launch {
            val token = prefs.getAuthToken() ?: cachedToken
            if (!token.isNullOrBlank()) {
                com.ais.restroos.data.remote.RetrofitClient.updateAuthToken(token)
                com.ais.restroos.data.repository.MenuRepository.fetchMenuItemsFromBackend()
                com.ais.restroos.data.repository.OrderRepository.fetchOrdersFromBackend()
                com.ais.restroos.data.repository.OrderRepository.startRealtimeSync()
            }
            // Silent background auto-connect to configured printer
            com.ais.restroos.data.printer.BluetoothPrinterService.autoConnectSavedPrinter(applicationContext)
        }

        checkAndRequestStartupPermissions()

        setContent {
            AISRestroOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showPermissionDialog) {
                        PermissionRationaleDialog(
                            isPermanentlyDenied = isPermanentlyDenied,
                            onRequestPermissions = {
                                checkAndRequestStartupPermissions()
                            },
                            onOpenSettings = {
                                PermissionHelper.openAppSettings(this)
                            },
                            onDismiss = {
                                showPermissionDialog = false
                            }
                        )
                    }

                    MainAppNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from App Settings or background
        val missing = PermissionHelper.getMissingPermissions(this)
        if (missing.isEmpty()) {
            showPermissionDialog = false
        }
        val token = com.ais.restroos.data.remote.RetrofitClient.getAuthToken()
        if (!token.isNullOrBlank()) {
            com.ais.restroos.data.repository.OrderRepository.startRealtimeSync()
        }
    }

    private fun checkAndRequestStartupPermissions() {
        val missing = PermissionHelper.getMissingPermissions(this)
        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        } else {
            showPermissionDialog = false
        }
    }
}