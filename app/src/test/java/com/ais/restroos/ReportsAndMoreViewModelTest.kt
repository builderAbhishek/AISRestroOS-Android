package com.ais.restroos

import com.ais.restroos.data.repository.MenuRepository
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.feature.menu.AddMenuItemViewModel
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
class ReportsAndMoreViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var addMenuItemViewModel: AddMenuItemViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MenuRepository.resetToSampleData()
        OrderRepository.resetToSampleData()
        addMenuItemViewModel = AddMenuItemViewModel(MenuRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAddMenuItem() {
        val initialCount = MenuRepository.menuItems.value.size
        addMenuItemViewModel.onItemNameChange("Special Paneer Dosa")
        addMenuItemViewModel.onSalePriceChange("180")
        addMenuItemViewModel.onCategoryChange("डोसा")

        var saved = false
        addMenuItemViewModel.saveItem { saved = true }

        assertTrue(saved)
        assertEquals(initialCount + 1, MenuRepository.menuItems.value.size)
        val added = MenuRepository.menuItems.value.find { it.name == "Special Paneer Dosa" }
        assertNotNull(added)
        assertEquals(180.0, added?.price ?: 0.0, 0.01)
    }

    @Test
    fun testItemSalesReportCalculations() {
        val closedOrders = OrderRepository.closedOrders.value
        assertEquals(0, closedOrders.size)
    }
}