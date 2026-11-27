package com.ais.restroos.data.repository

import android.content.Context
import com.ais.restroos.data.local.LocalDatabaseHelper
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.CreatePurchaseRequest
import com.ais.restroos.data.remote.dtos.DailyHisaabSummaryDto
import com.ais.restroos.data.remote.dtos.PurchaseItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object PurchaseRepository {

    private val _purchases = MutableStateFlow<List<PurchaseItemDto>>(emptyList())
    val purchases: StateFlow<List<PurchaseItemDto>> = _purchases.asStateFlow()

    private val _summary = MutableStateFlow(
        DailyHisaabSummaryDto(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
    )
    val summary: StateFlow<DailyHisaabSummaryDto> = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun loadHisaab(
        context: Context,
        restaurantId: String,
        selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val localHelper = LocalDatabaseHelper.getInstance(context)

        // 1. Instant Local Load (Show cached records without resetting non-zero sales)
        val localList = localHelper.getLocalPurchases(restaurantId, date = selectedDate)
        if (localList.isNotEmpty()) {
            _purchases.value = localList
        }

        val localSummary = localHelper.getLocalDailySummary(restaurantId, selectedDate)
        val currentSummary = _summary.value
        if (currentSummary.date == selectedDate && currentSummary.total_sales > 0 && localSummary.total_sales == 0.0) {
            _summary.value = localSummary.copy(
                total_sales = currentSummary.total_sales,
                order_count = currentSummary.order_count,
                net_profit = currentSummary.total_sales - localSummary.total_outflow
            )
        } else {
            _summary.value = localSummary
        }

        // 2. Network Refresh (Official single source of truth)
        try {
            val api = RetrofitClient.apiService

            val summaryResp = api.getDailySummary(date = selectedDate)
            if (summaryResp.isSuccessful && summaryResp.body()?.status == "success") {
                summaryResp.body()?.data?.let { remoteSummary ->
                    _summary.value = remoteSummary
                }
            }

            val purchasesResp = api.getPurchases(date = selectedDate)
            if (purchasesResp.isSuccessful && purchasesResp.body()?.status == "success") {
                val remoteList = purchasesResp.body()?.data ?: emptyList()
                localHelper.replacePurchasesForDate(remoteList, restaurantId, selectedDate)
                _purchases.value = remoteList
            }
        } catch (_: Exception) {
            // Offline fallback remains cleanly active
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun addPurchase(
        context: Context,
        restaurantId: String,
        request: CreatePurchaseRequest
    ): Boolean = withContext(Dispatchers.IO) {
        val id = request.id ?: ("PUR-" + UUID.randomUUID().toString().take(8).uppercase())
        val item = PurchaseItemDto(
            id = id,
            item_name = request.item_name,
            category = request.category,
            quantity = request.quantity,
            unit = request.unit,
            vendor_name = request.vendor_name ?: "Local Market",
            amount = request.amount,
            payment_mode = request.payment_mode,
            expense_type = request.expense_type,
            purchase_date = request.purchase_date,
            notes = request.notes ?: ""
        )

        val localHelper = LocalDatabaseHelper.getInstance(context)
        localHelper.savePurchase(item, restaurantId)

        // Update local state immediately
        val updated = (listOf(item) + _purchases.value).distinctBy { it.id }
        _purchases.value = updated

        // Preserve live sales in optimistic summary calculation
        val currentSales = _summary.value.total_sales
        val currentOrders = _summary.value.order_count
        val localSum = localHelper.getLocalDailySummary(restaurantId, request.purchase_date)
        val salesToUse = if (currentSales > 0) currentSales else localSum.total_sales
        val ordersToUse = if (currentOrders > 0) currentOrders else localSum.order_count
        _summary.value = localSum.copy(
            total_sales = salesToUse,
            order_count = ordersToUse,
            net_profit = salesToUse - localSum.total_outflow
        )

        // Sync with backend and fetch official summary
        try {
            val api = RetrofitClient.apiService
            val resp = api.createPurchase(request.copy(id = id))
            if (resp.isSuccessful && resp.body()?.status == "success") {
                resp.body()?.data?.let { savedRemote ->
                    localHelper.savePurchase(savedRemote, restaurantId)
                }
            }
            val summaryResp = api.getDailySummary(date = request.purchase_date)
            if (summaryResp.isSuccessful && summaryResp.body()?.status == "success") {
                summaryResp.body()?.data?.let { _summary.value = it }
            }
        } catch (_: Exception) {}

        true
    }

    suspend fun deletePurchase(
        context: Context,
        restaurantId: String,
        purchaseId: String,
        purchaseDate: String
    ): Boolean = withContext(Dispatchers.IO) {
        val localHelper = LocalDatabaseHelper.getInstance(context)
        localHelper.deleteLocalPurchase(purchaseId, restaurantId)

        _purchases.value = _purchases.value.filterNot { it.id == purchaseId }

        // Preserve live sales in optimistic summary calculation
        val currentSales = _summary.value.total_sales
        val currentOrders = _summary.value.order_count
        val localSum = localHelper.getLocalDailySummary(restaurantId, purchaseDate)
        val salesToUse = if (currentSales > 0) currentSales else localSum.total_sales
        val ordersToUse = if (currentOrders > 0) currentOrders else localSum.order_count
        _summary.value = localSum.copy(
            total_sales = salesToUse,
            order_count = ordersToUse,
            net_profit = salesToUse - localSum.total_outflow
        )

        try {
            val api = RetrofitClient.apiService
            api.deletePurchase(purchaseId)
            val summaryResp = api.getDailySummary(date = purchaseDate)
            if (summaryResp.isSuccessful && summaryResp.body()?.status == "success") {
                summaryResp.body()?.data?.let { _summary.value = it }
            }
        } catch (_: Exception) {}

        true
    }
}
