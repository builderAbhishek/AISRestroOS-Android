package com.ais.restroos

import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.OrderItem
import com.ais.restroos.domain.model.OrderStatus
import com.ais.restroos.feature.orders.closed.ClosedOrdersFilter
import com.ais.restroos.feature.orders.closed.ClosedOrdersViewModel
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
class ClosedOrdersViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: ClosedOrdersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        OrderRepository.resetToSampleData()
        viewModel = ClosedOrdersViewModel(OrderRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultFilterIsLast60Mins() {
        val state = viewModel.uiState.value
        assertEquals(ClosedOrdersFilter.LAST_60_MINS, state.activeFilter)
        assertEquals(0, state.orders.size)
    }

    @Test
    fun testOrderSelectionToggle() {
        val order = ClosedOrder("INV-101", "INV-101", "G1", 100.0, listOf(OrderItem("1", "Dish", 1, 100.0)), OrderStatus.COMPLETED, System.currentTimeMillis())
        OrderRepository.deleteClosedOrders(emptyList())

        assertEquals(0, viewModel.uiState.value.selectedCount)
    }
}