package com.ais.restroos.data.remote

import com.ais.restroos.data.remote.dtos.ApiResponseWrapper
import com.ais.restroos.data.remote.dtos.AuthData
import com.ais.restroos.data.remote.dtos.CategorySyncDto
import com.ais.restroos.data.remote.dtos.CreatePurchaseRequest
import com.ais.restroos.data.remote.dtos.CreateStaffRequest
import com.ais.restroos.data.remote.dtos.CustomerSyncDto
import com.ais.restroos.data.remote.dtos.KhataEntryDto
import com.ais.restroos.data.remote.dtos.KhataRecordRequest
import com.ais.restroos.data.remote.dtos.DailyHisaabSummaryDto
import com.ais.restroos.data.remote.dtos.DeviceSyncDto
import com.ais.restroos.data.remote.dtos.ItemSalesReportDto
import com.ais.restroos.data.remote.dtos.LoginRequest
import com.ais.restroos.data.remote.dtos.MenuItemSyncDto
import com.ais.restroos.data.remote.dtos.OrderSyncDto
import com.ais.restroos.data.remote.dtos.PurchaseItemDto
import com.ais.restroos.data.remote.dtos.ReceiptSettingsDto
import com.ais.restroos.data.remote.dtos.RegisterRequest
import com.ais.restroos.data.remote.dtos.RestaurantProfileDto
import com.ais.restroos.data.remote.dtos.SalesReportSummaryDto
import com.ais.restroos.data.remote.dtos.StaffListResponseDto
import com.ais.restroos.data.remote.dtos.StaffMemberDto
import com.ais.restroos.data.remote.dtos.StockReportDto
import com.ais.restroos.data.remote.dtos.SubscriptionDto
import com.ais.restroos.data.remote.dtos.SubscriptionPlanDto
import com.ais.restroos.data.remote.dtos.SyncPullResult
import com.ais.restroos.data.remote.dtos.SyncPushRequest
import com.ais.restroos.data.remote.dtos.SyncPushResult
import com.ais.restroos.data.remote.dtos.TableSyncDto
import com.ais.restroos.data.remote.dtos.UpdateRestaurantProfileRequest
import com.ais.restroos.domain.model.DashboardOverview
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface RestroApiService {

    @GET("health")
    suspend fun healthCheck(): Response<ApiResponseWrapper<Map<String, String>>>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponseWrapper<AuthData>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponseWrapper<AuthData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponseWrapper<Unit>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponseWrapper<AuthData>>

    @GET("categories")
    suspend fun getCategories(): Response<ApiResponseWrapper<List<CategorySyncDto>>>

    @POST("categories")
    suspend fun createCategory(
        @Body request: CategorySyncDto
    ): Response<ApiResponseWrapper<CategorySyncDto>>

    @GET("tables")
    suspend fun getTables(): Response<ApiResponseWrapper<List<TableSyncDto>>>

    @POST("tables")
    suspend fun createTable(
        @Body request: TableSyncDto
    ): Response<ApiResponseWrapper<TableSyncDto>>

    @GET("menu-items")
    suspend fun getMenuItems(): Response<ApiResponseWrapper<List<MenuItemSyncDto>>>

    @POST("menu-items")
    suspend fun createMenuItem(
        @Body request: MenuItemSyncDto
    ): Response<ApiResponseWrapper<MenuItemSyncDto>>

    @Multipart
    @POST("menu-items/upload-image")
    suspend fun uploadItemImage(
        @Part image: MultipartBody.Part,
        @Part("item_id") itemId: RequestBody? = null,
        @Part("old_image_url") oldImageUrl: RequestBody? = null
    ): Response<ApiResponseWrapper<Map<String, String>>>

    @retrofit2.http.DELETE("menu-items/{id}")
    suspend fun deleteMenuItem(
        @retrofit2.http.Path("id") id: String
    ): Response<ApiResponseWrapper<Any>>

    @GET("customers")
    suspend fun getCustomers(): Response<ApiResponseWrapper<List<CustomerSyncDto>>>

    @POST("customers")
    suspend fun saveCustomer(
        @Body request: CustomerSyncDto
    ): Response<ApiResponseWrapper<CustomerSyncDto>>

    @GET("customers/khata")
    suspend fun getKhataLedger(
        @Query("mobile") mobile: String? = null
    ): Response<ApiResponseWrapper<List<KhataEntryDto>>>

    @POST("customers/khata")
    suspend fun recordKhataEntry(
        @Body request: KhataRecordRequest
    ): Response<ApiResponseWrapper<Map<String, Any?>>>

    @POST("customers")
    suspend fun createCustomer(
        @Body request: CustomerSyncDto
    ): Response<ApiResponseWrapper<CustomerSyncDto>>

    @GET("orders")
    suspend fun getOrders(
        @Query("status") status: String? = null
    ): Response<ApiResponseWrapper<List<OrderSyncDto>>>

    @POST("orders")
    suspend fun createOrder(
        @Body request: OrderSyncDto
    ): Response<ApiResponseWrapper<OrderSyncDto>>

    @POST("orders/item/cancel")
    suspend fun cancelOrder(
        @Query("id") id: String
    ): Response<ApiResponseWrapper<Any?>>

    @retrofit2.http.DELETE("orders")
    suspend fun clearOrders(): Response<ApiResponseWrapper<Any?>>

    @retrofit2.http.DELETE("orders/item")
    suspend fun softDeleteOrder(
        @Query("id") id: String,
        @Query("order_id") queryId: String = id
    ): Response<ApiResponseWrapper<Any?>>

    @POST("sync/push")
    suspend fun pushSync(
        @Header("X-Idempotency-Key") idempotencyKey: String,
        @Body request: SyncPushRequest
    ): Response<ApiResponseWrapper<SyncPushResult>>

    @GET("sync/pull")
    suspend fun pullSync(
        @Query("cursor") cursor: Long
    ): Response<ApiResponseWrapper<SyncPullResult>>

    @GET("reports/sales")
    suspend fun getSalesReport(): Response<ApiResponseWrapper<SalesReportSummaryDto>>

    @GET("reports/itemized")
    suspend fun getItemSalesReport(): Response<ApiResponseWrapper<List<ItemSalesReportDto>>>

    @GET("reports/stock")
    suspend fun getStockReport(): Response<ApiResponseWrapper<StockReportDto>>

    @GET("branches/{branchId}/overview")
    suspend fun getDashboardOverview(
        @Path("branchId") branchId: String
    ): Response<DashboardOverview>

    @GET("devices")
    suspend fun getDevices(): Response<ApiResponseWrapper<List<DeviceSyncDto>>>

    @POST("devices")
    suspend fun createDevice(
        @Body request: DeviceSyncDto
    ): Response<ApiResponseWrapper<DeviceSyncDto>>

    // --- DAILY PURCHASES & EXPENSE ENDPOINTS ---
    @GET("inventory/purchases")
    suspend fun getPurchases(
        @Query("date") date: String? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null,
        @Query("vendor") vendor: String? = null
    ): Response<ApiResponseWrapper<List<PurchaseItemDto>>>

    @POST("inventory/purchases")
    suspend fun createPurchase(
        @Body request: CreatePurchaseRequest
    ): Response<ApiResponseWrapper<PurchaseItemDto>>

    @DELETE("inventory/purchases/{id}")
    suspend fun deletePurchase(
        @Path("id") id: String,
        @Query("id") queryId: String = id
    ): Response<ApiResponseWrapper<Unit>>

    @GET("inventory/daily-summary")
    suspend fun getDailySummary(
        @Query("date") date: String? = null
    ): Response<ApiResponseWrapper<DailyHisaabSummaryDto>>

    // --- RESTAURANT PROFILE, STAFF & SUBSCRIPTION PLANS ---
    @GET("restaurant/profile")
    suspend fun getRestaurantProfile(): Response<ApiResponseWrapper<RestaurantProfileDto>>

    @POST("restaurant/profile")
    suspend fun updateRestaurantProfile(
        @Body request: UpdateRestaurantProfileRequest
    ): Response<ApiResponseWrapper<RestaurantProfileDto>>

    @GET("staff")
    suspend fun getStaffList(): Response<ApiResponseWrapper<StaffListResponseDto>>

    @POST("staff")
    suspend fun createStaff(
        @Body request: CreateStaffRequest
    ): Response<ApiResponseWrapper<StaffMemberDto>>

    @DELETE("staff/{id}")
    suspend fun deleteStaff(
        @Path("id") id: String,
        @Query("id") queryId: String = id
    ): Response<ApiResponseWrapper<Any?>>

    @GET("subscriptions")
    suspend fun getActiveSubscription(): Response<ApiResponseWrapper<SubscriptionDto>>

    @GET("subscription-plans")
    suspend fun getSubscriptionPlans(): Response<ApiResponseWrapper<List<SubscriptionPlanDto>>>

    // --- RECEIPT & PRINTER CUSTOMIZATION ---
    @GET("printer-settings")
    suspend fun getReceiptSettings(): Response<ApiResponseWrapper<ReceiptSettingsDto>>

    @POST("printer-settings")
    suspend fun saveReceiptSettings(
        @Body request: ReceiptSettingsDto
    ): Response<ApiResponseWrapper<ReceiptSettingsDto>>

    @Multipart
    @POST("printer-settings/qr")
    suspend fun uploadReceiptQr(
        @Part image: MultipartBody.Part
    ): Response<ApiResponseWrapper<Map<String, String>>>
}