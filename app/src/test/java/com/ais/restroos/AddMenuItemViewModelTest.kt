package com.ais.restroos

import com.ais.restroos.data.repository.MenuRepository
import com.ais.restroos.feature.menu.AddMenuItemViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddMenuItemViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AddMenuItemViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MenuRepository.resetToSampleData()
        viewModel = AddMenuItemViewModel(MenuRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialFormInvalid() {
        val state = viewModel.uiState.value
        assertEquals("", state.itemName)
        assertEquals("", state.salePrice)
        assertFalse(state.isFormValid)
    }

    @Test
    fun testValidFormState() {
        viewModel.onItemNameChange("Paneer Tikka Roll")
        viewModel.onSalePriceChange("120")
        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun testSaveItem() {
        val initialCount = MenuRepository.menuItems.value.size
        viewModel.onItemNameChange("Paneer Tikka Roll")
        viewModel.onSalePriceChange("120")
        viewModel.onCategoryChange("Snacks")

        var saved = false
        viewModel.saveItem { saved = true }

        assertTrue(saved)
        assertEquals(initialCount + 1, MenuRepository.menuItems.value.size)
        val added = MenuRepository.menuItems.value.find { it.name == "Paneer Tikka Roll" }
        assertNotNull(added)
        assertEquals(120.0, added?.price ?: 0.0, 0.01)
    }

    @Test
    fun testSaveAndNewResetsForm() {
        val initialCount = MenuRepository.menuItems.value.size
        viewModel.onItemNameChange("Veg Burger Special")
        viewModel.onSalePriceChange("80")

        viewModel.saveAndNew()

        assertEquals(initialCount + 1, MenuRepository.menuItems.value.size)
        // Form should reset for next item
        assertEquals("", viewModel.uiState.value.itemName)
        assertEquals("", viewModel.uiState.value.salePrice)
        assertFalse(viewModel.uiState.value.isFormValid)
    }
}