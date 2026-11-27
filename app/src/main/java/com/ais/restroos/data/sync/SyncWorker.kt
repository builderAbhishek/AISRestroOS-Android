package com.ais.restroos.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ais.restroos.data.local.LocalDatabaseHelper
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.SyncPushRequest
import com.ais.restroos.data.repository.MenuRepository
import com.ais.restroos.data.repository.OrderRepository
import java.util.UUID

class SyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        SyncManager.updateSyncState(SyncState.Syncing)

        return try {
            val dbHelper = LocalDatabaseHelper.getInstance(appContext)
            val pendingOrders = dbHelper.getPendingOrdersForSync()

            if (pendingOrders.isNotEmpty()) {
                val idempotencyKey = UUID.randomUUID().toString()
                val pushRequest = SyncPushRequest(orders = pendingOrders)

                val response = RetrofitClient.apiService.pushSync(
                    idempotencyKey = idempotencyKey,
                    request = pushRequest
                )

                if (response.isSuccessful && response.body()?.status == "success") {
                    val resultData = response.body()?.data
                    val syncedUuids = resultData?.synced_uuids ?: pendingOrders.map { it.uuid }
                    dbHelper.markOrdersSynced(syncedUuids)
                    OrderRepository.loadFromLocalDatabase()

                    // Also pull latest data from server
                    OrderRepository.fetchOrdersFromBackend()
                    MenuRepository.fetchMenuItemsFromBackend()

                    val remainingPending = dbHelper.getPendingSyncCount()
                    if (remainingPending > 0) {
                        SyncManager.updateSyncState(SyncState.Pending(remainingPending))
                    } else {
                        SyncManager.updateSyncState(SyncState.Synced(System.currentTimeMillis()))
                    }
                    Result.success()
                } else {
                    val errMsg = response.body()?.message ?: "Server sync rejected (HTTP ${response.code()})"
                    SyncManager.updateSyncState(SyncState.Error(errMsg))
                    Result.retry()
                }
            } else {
                // No pending outgoing orders; perform background pull to refresh data
                OrderRepository.fetchOrdersFromBackend()
                MenuRepository.fetchMenuItemsFromBackend()
                SyncManager.updateSyncState(SyncState.Synced(System.currentTimeMillis()))
                Result.success()
            }
        } catch (e: Exception) {
            val remainingPending = LocalDatabaseHelper.getInstance(appContext).getPendingSyncCount()
            SyncManager.updateSyncState(
                if (remainingPending > 0) SyncState.Pending(remainingPending)
                else SyncState.Error(e.localizedMessage ?: "Sync connection failed")
            )
            Result.retry()
        }
    }
}
