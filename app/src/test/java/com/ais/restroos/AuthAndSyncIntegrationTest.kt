package com.ais.restroos

import com.ais.restroos.core.auth.AuthEvent
import com.ais.restroos.core.auth.AuthEventManager
import com.ais.restroos.data.remote.RestroApiService
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.ApiResponseWrapper
import com.ais.restroos.data.remote.dtos.AuthData
import com.ais.restroos.data.remote.dtos.CategorySyncDto
import com.ais.restroos.data.remote.dtos.CreatePurchaseRequest
import com.ais.restroos.data.remote.dtos.CustomerSyncDto
import com.ais.restroos.data.remote.dtos.DailyHisaabSummaryDto
import com.ais.restroos.data.remote.dtos.DeviceSyncDto
import com.ais.restroos.data.remote.dtos.ItemSalesReportDto
import com.ais.restroos.data.remote.dtos.LoginRequest
import com.ais.restroos.data.remote.dtos.MenuItemSyncDto
import com.ais.restroos.data.remote.dtos.OrderSyncDto
import com.ais.restroos.data.remote.dtos.PurchaseItemDto
import com.ais.restroos.data.remote.dtos.RegisterRequest
import com.ais.restroos.data.remote.dtos.SalesReportSummaryDto
import com.ais.restroos.data.remote.dtos.StockReportDto
import com.ais.restroos.data.remote.dtos.SyncPullResult
import com.ais.restroos.data.remote.dtos.SyncPushRequest
import com.ais.restroos.data.remote.dtos.SyncPushResult
import com.ais.restroos.data.remote.dtos.TableSyncDto
import com.ais.restroos.data.sync.SyncManager
import com.ais.restroos.data.sync.SyncState
import okhttp3.RequestBody
import com.ais.restroos.domain.model.DashboardOverview
import com.ais.restroos.feature.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndSyncIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authViewModel: AuthViewModel
    private val fakeApiService = FakeRestroApiService()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authViewModel = AuthViewModel(prefsManager = null, customApiService = fakeApiService)
        SyncManager.updateSyncState(SyncState.Idle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialAuthViewModelStateIsUnauthenticated() {
        val state = authViewModel.uiState.value
        assertFalse("App must not be authenticated on fresh start", state.isAuthenticated)
        assertTrue("Default mode should be login mode", state.isLoginMode)
        assertEquals("", state.emailInput)
        assertEquals("", state.passwordInput)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun testAuthModeTogglingAndFieldUpdates() {
        authViewModel.onEmailChange("test@restro.com")
        authViewModel.onPasswordChange("password123")
        assertEquals("test@restro.com", authViewModel.uiState.value.emailInput)
        assertEquals("password123", authViewModel.uiState.value.passwordInput)

        authViewModel.toggleAuthMode()
        assertFalse("Mode should toggle to registration mode", authViewModel.uiState.value.isLoginMode)

        authViewModel.onNameChange("Rohan Sharma")
        authViewModel.onRestaurantNameChange("Sharma Dhaba")
        authViewModel.onPhoneChange("9876543210")
        assertEquals("Rohan Sharma", authViewModel.uiState.value.nameInput)
        assertEquals("Sharma Dhaba", authViewModel.uiState.value.restaurantNameInput)
        assertEquals("9876543210", authViewModel.uiState.value.phoneInput)

        authViewModel.toggleAuthMode()
        assertTrue("Mode should toggle back to login mode", authViewModel.uiState.value.isLoginMode)
    }

    @Test
    fun testEmptyInputValidationRejectsSubmission() {
        var successCalled = false
        authViewModel.submitAuth { successCalled = true }

        assertFalse("Submission with empty inputs must not succeed", successCalled)
        assertNotNull("Error message must be set for empty inputs", authViewModel.uiState.value.errorMessage)
        assertEquals("Please fill in email and password.", authViewModel.uiState.value.errorMessage)
    }

    @Test
    fun testAuthEventManagerEmitsSessionExpiredAndLogoutEvents() = runTest {
        val emittedEvents = mutableListOf<AuthEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            AuthEventManager.events.collect { emittedEvents.add(it) }
        }

        // Test 401 Session Expiration notification
        AuthEventManager.notifySessionExpired()
        assertEquals(1, emittedEvents.size)
        assertTrue("Event should be SessionExpired", emittedEvents[0] is AuthEvent.SessionExpired)

        // Test Logout notification
        AuthEventManager.notifyLoggedOut()
        assertEquals(2, emittedEvents.size)
        assertTrue("Event should be LoggedOut", emittedEvents[1] is AuthEvent.LoggedOut)

        // Test Login Success notification
        AuthEventManager.notifyLoginSuccess("test_token_abc")
        assertEquals(3, emittedEvents.size)
        assertTrue("Event should be LoginSuccess", emittedEvents[2] is AuthEvent.LoginSuccess)
        assertEquals("test_token_abc", (emittedEvents[2] as AuthEvent.LoginSuccess).token)

        job.cancel()
    }

    @Test
    fun testRetrofitClientTokenInjectionAndClearing() {
        RetrofitClient.updateAuthToken("sample_jwt_or_sanctum_token")
        assertEquals("sample_jwt_or_sanctum_token", RetrofitClient.getAuthToken())

        RetrofitClient.updateAuthToken(null)
        assertNull("Auth token should be cleared", RetrofitClient.getAuthToken())
    }

    @Test
    fun testSyncManagerSyncStateTransitions() = runTest {
        // Initial state should be Idle
        assertTrue(SyncManager.syncState.value is SyncState.Idle)

        // Test Syncing state
        SyncManager.updateSyncState(SyncState.Syncing)
        assertTrue(SyncManager.syncState.value is SyncState.Syncing)

        // Test Synced state
        SyncManager.updateSyncState(SyncState.Synced())
        assertTrue(SyncManager.syncState.value is SyncState.Synced)

        // Test Pending state with count
        SyncManager.updateSyncState(SyncState.Pending(4))
        val pendingState = SyncManager.syncState.value
        assertTrue(pendingState is SyncState.Pending)
        assertEquals(4, (pendingState as SyncState.Pending).count)

        // Test Error state
        SyncManager.updateSyncState(SyncState.Error("Network timeout"))
        val errorState = SyncManager.syncState.value
        assertTrue(errorState is SyncState.Error)
        assertEquals("Network timeout", (errorState as SyncState.Error).message)
    }

    @Test
    fun testLogoutClearsStateAndNotifies() = runTest {
        var loggedOutCallbackTriggered = false
        authViewModel.logout {
            loggedOutCallbackTriggered = true
        }
        testScheduler.advanceUntilIdle()

        assertTrue("Logout callback must be triggered", loggedOutCallbackTriggered)
        assertFalse("UiState must be unauthenticated after logout", authViewModel.uiState.value.isAuthenticated)
        assertNull("RetrofitClient auth token must be nullified", RetrofitClient.getAuthToken())
    }

    private class FakeRestroApiService : RestroApiService {
        override suspend fun healthCheck(): Response<ApiResponseWrapper<Map<String, String>>> =
            Response.success(ApiResponseWrapper("success", "healthy", emptyMap()))

        override suspend fun register(request: RegisterRequest): Response<ApiResponseWrapper<AuthData>> =
            Response.success(ApiResponseWrapper("success", "ok", null))

        override suspend fun login(request: LoginRequest): Response<ApiResponseWrapper<AuthData>> =
            Response.success(ApiResponseWrapper("success", "ok", null))

        override suspend fun logout(): Response<ApiResponseWrapper<Unit>> =
            Response.success(ApiResponseWrapper("success", "logged out", Unit))

        override suspend fun getMe(): Response<ApiResponseWrapper<AuthData>> =
            Response.success(ApiResponseWrapper("success", "ok", null))

        override suspend fun getCategories(): Response<ApiResponseWrapper<List<CategorySyncDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun createCategory(request: CategorySyncDto): Response<ApiResponseWrapper<CategorySyncDto>> =
            Response.success(ApiResponseWrapper("success", "ok", request))

        override suspend fun getTables(): Response<ApiResponseWrapper<List<TableSyncDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun createTable(request: TableSyncDto): Response<ApiResponseWrapper<TableSyncDto>> =
            Response.success(ApiResponseWrapper("success", "ok", request))

        override suspend fun getMenuItems(): Response<ApiResponseWrapper<List<MenuItemSyncDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun createMenuItem(request: MenuItemSyncDto): Response<ApiResponseWrapper<MenuItemSyncDto>> =
            Response.success(ApiResponseWrapper("success", "ok", request))

        override suspend fun uploadItemImage(
            image: MultipartBody.Part,
            itemId: RequestBody?,
            oldImageUrl: RequestBody?
        ): Response<ApiResponseWrapper<Map<String, String>>> =
            Response.success(ApiResponseWrapper("success", "ok", mapOf("url" to "https://test.com/dish.jpg")))

        override suspend fun deleteMenuItem(id: String): Response<ApiResponseWrapper<Any>> =
            Response.success(ApiResponseWrapper("success", "ok", Any()))

        override suspend fun getCustomers(): Response<ApiResponseWrapper<List<CustomerSyncDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun createCustomer(request: CustomerSyncDto): Response<ApiResponseWrapper<CustomerSyncDto>> =
            Response.success(ApiResponseWrapper("success", "ok", request))

        override suspend fun getOrders(status: String?): Response<ApiResponseWrapper<List<OrderSyncDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun createOrder(request: OrderSyncDto): Response<ApiResponseWrapper<OrderSyncDto>> =
            Response.success(ApiResponseWrapper("success", "ok", request))

        override suspend fun clearOrders(): Response<ApiResponseWrapper<Any?>> =
            Response.success(ApiResponseWrapper("success", "ok", null))

        override suspend fun pushSync(idempotencyKey: String, request: SyncPushRequest): Response<ApiResponseWrapper<SyncPushResult>> =
            Response.success(ApiResponseWrapper("success", "ok", SyncPushResult(0L, emptyList(), emptyList(), emptyList())))

        override suspend fun pullSync(cursor: Long): Response<ApiResponseWrapper<SyncPullResult>> =
            Response.success(ApiResponseWrapper("success", "ok", SyncPullResult(0L, emptyList(), emptyList(), emptyList(), emptyList())))

        override suspend fun getSalesReport(): Response<ApiResponseWrapper<SalesReportSummaryDto>> =
            Response.success(ApiResponseWrapper("success", "ok", SalesReportSummaryDto(0.0, 0.0, 0.0, 0, 0.0)))

        override suspend fun getItemSalesReport(): Response<ApiResponseWrapper<List<ItemSalesReportDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun getStockReport(): Response<ApiResponseWrapper<StockReportDto>> =
            Response.success(ApiResponseWrapper("success", "ok", StockReportDto()))

        override suspend fun getDashboardOverview(branchId: String): Response<DashboardOverview> =
            Response.success(DashboardOverview(0.0, 0, 0.0, 0))

        override suspend fun getDevices(): Response<ApiResponseWrapper<List<DeviceSyncDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun createDevice(request: DeviceSyncDto): Response<ApiResponseWrapper<DeviceSyncDto>> =
            Response.success(ApiResponseWrapper("success", "ok", request))

        override suspend fun getPurchases(date: String?, fromDate: String?, toDate: String?, vendor: String?): Response<ApiResponseWrapper<List<PurchaseItemDto>>> =
            Response.success(ApiResponseWrapper("success", "ok", emptyList()))

        override suspend fun createPurchase(request: CreatePurchaseRequest): Response<ApiResponseWrapper<PurchaseItemDto>> =
            Response.success(ApiResponseWrapper("success", "ok", PurchaseItemDto("PUR-1", request.item_name, request.category, request.quantity, request.unit, request.vendor_name ?: "", request.amount, request.payment_mode, request.expense_type, request.purchase_date, request.notes ?: "")))

        override suspend fun deletePurchase(id: String): Response<ApiResponseWrapper<Unit>> =
            Response.success(ApiResponseWrapper("success", "ok", Unit))

        override suspend fun getDailySummary(date: String?): Response<ApiResponseWrapper<DailyHisaabSummaryDto>> =
            Response.success(ApiResponseWrapper("success", "ok", DailyHisaabSummaryDto(date ?: "2026-11-24", 0.0, 0, 0.0, 0.0, 0.0, 0, 0.0)))
    }
}
