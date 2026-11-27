package com.ais.restroos

import com.ais.restroos.domain.model.PrinterState
import com.ais.restroos.feature.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DashboardViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() {
        val state = viewModel.uiState.value
        assertEquals(PrinterState.OFFLINE, state.printerState)
        assertEquals("My Restaurant", state.selectedBranch.name)
        assertEquals(0.00, state.overview.todaySales, 0.01)
        assertEquals(0, state.overview.todayOrders)
        assertFalse(state.showBranchSelectorDialog)
    }

    @Test
    fun testPrinterStateToggle() {
        assertEquals(PrinterState.OFFLINE, viewModel.uiState.value.printerState)
        viewModel.togglePrinterState()
        assertEquals(PrinterState.CONNECTING, viewModel.uiState.value.printerState)
        viewModel.togglePrinterState()
        assertEquals(PrinterState.ERROR, viewModel.uiState.value.printerState)
        viewModel.togglePrinterState()
        assertEquals(PrinterState.ONLINE, viewModel.uiState.value.printerState)
        viewModel.togglePrinterState()
        assertEquals(PrinterState.OFFLINE, viewModel.uiState.value.printerState)
    }

    @Test
    fun testBranchSelection() {
        viewModel.onBranchClick()
        assertTrue(viewModel.uiState.value.showBranchSelectorDialog)

        val targetBranch = viewModel.uiState.value.branches[0]
        viewModel.selectBranch(targetBranch)

        assertFalse(viewModel.uiState.value.showBranchSelectorDialog)
        assertEquals(targetBranch.id, viewModel.uiState.value.selectedBranch.id)
    }
}