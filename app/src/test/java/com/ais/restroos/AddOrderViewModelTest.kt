package com.ais.restroos

import com.ais.restroos.data.repository.MenuRepository
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.OrderStatus
import com.ais.restroos.domain.model.OrderType
import com.ais.restroos.domain.model.POSMenuItem
import com.ais.restroos.feature.pos.AddOrderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddOrderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AddOrderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MenuRepository.resetToSampleData()
        OrderRepository.resetToSampleData()
        MenuRepository.setMenuItems(
            listOf(
                POSMenuItem("191-L", "191", "191 पानी बड़ा", "Beverages", 20.0),
                POSMenuItem("DOSA-01", "05", "मसाला डोसा Special", "डोसा", 120.0)
            )
        )
        viewModel = AddOrderViewModel(MenuRepository, OrderRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialMenuItemsLoaded() {
        val state = viewModel.uiState.value
        assertTrue(state.menuItems.isNotEmpty())
        assertEquals(0, state.cart.size)
        assertEquals(0, state.totalItemCount)
        assertEquals(0.0, state.cartSubtotal, 0.01)
    }

    @Test
    fun testSearchFiltering() {
        viewModel.onSearchQueryChange("191")
        val state = viewModel.uiState.value
        assertEquals(1, state.menuItems.size)
        assertEquals("191", state.menuItems[0].code)
    }

    @Test
    fun testCategoryFiltering() {
        viewModel.onCategorySelect("डोसा")
        val state = viewModel.uiState.value
        assertEquals(1, state.menuItems.size)
        assertEquals("मसाला डोसा Special", state.menuItems[0].name)
    }

    @Test
    fun testAddToCartAndQuantityManagement() {
        val item = MenuRepository.menuItems.value[0]
        viewModel.addToCart(item)

        var state = viewModel.uiState.value
        assertEquals(1, state.cart.size)
        assertEquals(1, state.totalItemCount)
        assertEquals(20.0, state.cartSubtotal, 0.01)

        viewModel.incrementQuantity(item.id)
        state = viewModel.uiState.value
        assertEquals(2, state.totalItemCount)
        assertEquals(40.0, state.cartSubtotal, 0.01)

        viewModel.decrementQuantity(item.id)
        state = viewModel.uiState.value
        assertEquals(1, state.totalItemCount)

        viewModel.decrementQuantity(item.id)
        state = viewModel.uiState.value
        assertEquals(0, state.cart.size)
        assertEquals(0.0, state.cartSubtotal, 0.01)
    }

    @Test
    fun testSaveAndHoldOrder() {
        val item = MenuRepository.menuItems.value[0]
        viewModel.addToCart(item)
        viewModel.setOrderType(OrderType.DINE_IN)
        viewModel.selectTable(com.ais.restroos.domain.model.RestaurantTable("T-99", false))

        var saved = false
        viewModel.saveAndHoldOrder { saved = true }

        assertTrue(saved)
        assertEquals(0, viewModel.uiState.value.cart.size)
        val holdOrders = OrderRepository.onHoldOrders.value
        val newlyHeld = holdOrders.find { it.tableNumber == "T-99" }
        assertNotNull(newlyHeld)
        assertEquals(OrderStatus.ON_HOLD, newlyHeld?.status)
    }

    @Test
    fun testCompleteBillingOrder() {
        val item = MenuRepository.menuItems.value[0]
        viewModel.addToCart(item)
        viewModel.selectTable(com.ais.restroos.domain.model.RestaurantTable("G1", false))

        var billed = false
        viewModel.completeBillingOrder { billed = true }

        assertTrue(billed)
        assertEquals(0, viewModel.uiState.value.cart.size)
        val closedOrders = OrderRepository.closedOrders.value
        assertTrue(closedOrders.isNotEmpty())
        assertEquals(OrderStatus.COMPLETED, closedOrders.first().status)
    }
}