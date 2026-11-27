package com.ais.restroos

import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.HeldOrder
import com.ais.restroos.domain.model.OrderItem
import com.ais.restroos.domain.model.OrderStatus
import com.ais.restroos.feature.orders.hold.OnHoldOrdersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnHoldOrdersViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: OnHoldOrdersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        OrderRepository.resetToSampleData()
        viewModel = OnHoldOrdersViewModel(OrderRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialOrdersEmpty() {
        val state = viewModel.uiState.value
        assertEquals(0, state.orders.size)
    }

    @Test
    fun testSaveAndHoldOrderReflectsInList() {
        val heldOrder = HeldOrder(
            orderId = "HOLD-99",
            tableNumber = "G3",
            totalAmount = 150.0,
            items = listOf(OrderItem("1", "Special Dosa", 1, 150.0)),
            status = OrderStatus.ON_HOLD
        )
        OrderRepository.saveOrder(heldOrder)

        val state = viewModel.uiState.value
        assertEquals(1, state.orders.size)
        assertEquals("G3", state.orders[0].tableNumber)
        assertEquals(150.0, state.orders[0].totalAmount, 0.01)
    }

    @Test
    fun testRequestDeleteAndDismiss() {
        val heldOrder = HeldOrder(
            orderId = "HOLD-99",
            tableNumber = "G3",
            totalAmount = 150.0,
            items = listOf(OrderItem("1", "Special Dosa", 1, 150.0)),
            status = OrderStatus.ON_HOLD
        )
        OrderRepository.saveOrder(heldOrder)

        val order = viewModel.uiState.value.orders[0]
        viewModel.requestDeleteOrder(order)
        assertEquals("HOLD-99", viewModel.uiState.value.orderToDelete?.orderId)

        viewModel.dismissDeleteDialog()
        assertNull(viewModel.uiState.value.orderToDelete)
    }
}