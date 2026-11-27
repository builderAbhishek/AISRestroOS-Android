package com.ais.restroos.feature.menu

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ais.restroos.core.utils.ImageCompressor
import com.ais.restroos.data.repository.MenuRepository
import com.ais.restroos.domain.model.MenuItemVariant
import com.ais.restroos.domain.model.POSMenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class VariantDraft(
    val name: String = "",
    val price: String = ""
)

data class AddMenuItemUiState(
    val isEditMode: Boolean = false,
    val editingItemId: String? = null,
    val editingItemCode: String = "",
    val itemName: String = "",
    val category: String = "General",
    val availableCategories: List<String> = emptyList(),
    val salePrice: String = "",
    val isVariantMode: Boolean = false,
    val variants: List<VariantDraft> = emptyList(),
    val taxMode: String = "Without Tax",
    val taxPercentage: String = "None",
    val isDefaultFirmTax: Boolean = true,
    val isFavourite: Boolean = false,
    val imageUrl: String? = null,
    val localImageUri: Uri? = null,
    val isUploadingImage: Boolean = false,
    val isLoading: Boolean = false,
    val toastMessage: String? = null
) {
    val isFormValid: Boolean
        get() {
            if (itemName.isBlank()) return false
            return if (isVariantMode) {
                variants.isNotEmpty() && variants.all {
                    it.name.isNotBlank() && (it.price.toDoubleOrNull() ?: 0.0) > 0.0
                }
            } else {
                (salePrice.toDoubleOrNull() ?: 0.0) > 0.0
            }
        }
}

class AddMenuItemViewModel(
    private val menuRepo: MenuRepository = MenuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMenuItemUiState())
    val uiState: StateFlow<AddMenuItemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            menuRepo.categoriesFlow.collect { cats ->
                val allCats = (cats + menuRepo.categories).filter { it.isNotBlank() && it != "All" }.distinct()
                _uiState.update { current ->
                    current.copy(
                        availableCategories = if (allCats.isEmpty()) listOf("General") else allCats
                    )
                }
            }
        }
    }

    fun initForNewItem() {
        val cats = menuRepo.categories.filter { it.isNotBlank() && it != "All" }
        _uiState.update {
            AddMenuItemUiState(
                isEditMode = false,
                editingItemId = null,
                editingItemCode = "",
                category = cats.firstOrNull() ?: "General",
                availableCategories = if (cats.isEmpty()) listOf("General") else cats
            )
        }
    }

    fun loadItemForEditing(item: POSMenuItem) {
        val cats = (menuRepo.categories + listOf(item.category)).filter { it.isNotBlank() && it != "All" }.distinct()
        val priceStr = if (item.price % 1.0 == 0.0) item.price.toInt().toString() else item.price.toString()
        val hasVariants = item.variants.isNotEmpty()
        val variantDrafts = item.variants.map { v ->
            val vp = if (v.price % 1.0 == 0.0) v.price.toInt().toString() else v.price.toString()
            VariantDraft(name = v.name, price = vp)
        }
        _uiState.update {
            it.copy(
                isEditMode = true,
                editingItemId = item.id,
                editingItemCode = item.code,
                itemName = item.name,
                category = item.category.ifBlank { "General" },
                availableCategories = if (cats.isEmpty()) listOf("General") else cats,
                salePrice = priceStr,
                isVariantMode = hasVariants,
                variants = variantDrafts,
                imageUrl = item.imageUrl,
                isFavourite = item.isFavourite
            )
        }
    }

    fun onItemNameChange(name: String) {
        _uiState.update { it.copy(itemName = name) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun addCustomCategory(newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isNotBlank() && !trimmed.equals("All", ignoreCase = true)) {
            menuRepo.addCategory(trimmed)
            _uiState.update { current ->
                val updatedList = (current.availableCategories + trimmed).distinct()
                current.copy(
                    category = trimmed,
                    availableCategories = updatedList
                )
            }
        }
    }

    fun onSalePriceChange(price: String) {
        if (price.isEmpty() || price.toDoubleOrNull() != null || price.endsWith(".")) {
            _uiState.update { it.copy(salePrice = price) }
        }
    }

    fun onVariantModeToggle(enabled: Boolean) {
        _uiState.update { current ->
            val nextVariants = if (enabled && current.variants.isEmpty()) {
                listOf(
                    VariantDraft("Half", ""),
                    VariantDraft("Full", current.salePrice)
                )
            } else {
                current.variants
            }
            current.copy(isVariantMode = enabled, variants = nextVariants)
        }
    }

    fun applyVariantPreset(presetNames: List<String>) {
        _uiState.update { current ->
            val nextVariants = presetNames.mapIndexed { idx, name ->
                VariantDraft(name = name, price = current.variants.getOrNull(idx)?.price ?: "")
            }
            current.copy(isVariantMode = true, variants = nextVariants)
        }
    }

    fun addVariantRow() {
        _uiState.update { current ->
            current.copy(variants = current.variants + VariantDraft("", ""))
        }
    }

    fun removeVariantRow(index: Int) {
        _uiState.update { current ->
            if (current.variants.size <= 1 || index !in current.variants.indices) return@update current
            current.copy(variants = current.variants.filterIndexed { i, _ -> i != index })
        }
    }

    fun updateVariantRow(index: Int, name: String? = null, price: String? = null) {
        _uiState.update { current ->
            if (index !in current.variants.indices) return@update current
            if (price != null && price.isNotEmpty() && price.toDoubleOrNull() == null && !price.endsWith(".")) {
                return@update current
            }
            val updated = current.variants.mapIndexed { i, row ->
                if (i == index) {
                    row.copy(
                        name = name ?: row.name,
                        price = price ?: row.price
                    )
                } else row
            }
            current.copy(variants = updated)
        }
    }

    fun onTaxModeChange(mode: String) {
        _uiState.update { it.copy(taxMode = mode) }
    }

    fun onTaxPercentageChange(tax: String) {
        _uiState.update { it.copy(taxPercentage = tax) }
    }

    fun onDefaultTaxToggle(checked: Boolean) {
        _uiState.update { it.copy(isDefaultFirmTax = checked) }
    }

    fun onFavouriteToggle(checked: Boolean) {
        _uiState.update { it.copy(isFavourite = checked) }
    }

    fun onImageSelected(url: String) {
        _uiState.update { it.copy(imageUrl = url) }
    }

    fun uploadAndSetDishImage(context: Context, imageUri: Uri) {
        _uiState.update { it.copy(localImageUri = imageUri, isUploadingImage = true) }
        viewModelScope.launch {
            Log.d("AddMenuItemViewModel", "Starting compression for imageUri: $imageUri")
            val compressResult = ImageCompressor.compressImage(context, imageUri)
            if (compressResult.isSuccess) {
                val compressedFile = compressResult.getOrThrow()
                val currentItemId = _uiState.value.editingItemId ?: UUID.randomUUID().toString()
                val oldUrl = _uiState.value.imageUrl
                _uiState.update { it.copy(editingItemId = currentItemId) }

                Log.d("AddMenuItemViewModel", "Uploading compressed dish image (${compressedFile.length()} bytes) for item $currentItemId")
                val uploadResult = menuRepo.uploadDishImage(compressedFile, currentItemId, oldUrl)
                if (uploadResult.isSuccess) {
                    val url = uploadResult.getOrThrow()
                    Log.d("AddMenuItemViewModel", "Upload succeeded, live URL: $url")
                    _uiState.update {
                        it.copy(
                            imageUrl = url,
                            isUploadingImage = false,
                            toastMessage = "Dish photo compressed and uploaded successfully!"
                        )
                    }
                } else {
                    val err = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                    Log.e("AddMenuItemViewModel", "Upload failed: $err")
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            toastMessage = "Upload failed: $err"
                        )
                    }
                }
            } else {
                val err = compressResult.exceptionOrNull()?.message ?: "Compression failed"
                Log.e("AddMenuItemViewModel", "Compression failed: $err")
                _uiState.update {
                    it.copy(
                        isUploadingImage = false,
                        toastMessage = "Compression failed: $err"
                    )
                }
            }
        }
    }

    fun removeDishImage() {
        val oldUrl = _uiState.value.imageUrl
        _uiState.update { it.copy(imageUrl = null, localImageUri = null) }
        val editId = _uiState.value.editingItemId
        if (!editId.isNullOrBlank() && !oldUrl.isNullOrBlank()) {
            viewModelScope.launch {
                val currentItem = menuRepo.menuItems.value.firstOrNull { it.id == editId }
                if (currentItem != null) {
                    val updatedItem = currentItem.copy(imageUrl = null)
                    menuRepo.updateMenuItem(updatedItem)
                    menuRepo.saveMenuItemToBackend(updatedItem)
                }
            }
        }
    }

    fun saveItem(onSuccess: () -> Unit) {
        if (!_uiState.value.isFormValid) {
            _uiState.update {
                it.copy(
                    toastMessage = if (it.isVariantMode) {
                        "Please enter item name and valid variant names & prices"
                    } else {
                        "Please enter valid item name and sale price"
                    }
                )
            }
            return
        }

        val state = _uiState.value
        val itemId = if (!state.editingItemId.isNullOrBlank()) {
            state.editingItemId
        } else {
            UUID.randomUUID().toString()
        }

        val itemCode = if (state.editingItemCode.isNotBlank()) {
            state.editingItemCode
        } else {
            (10..99).random().toString()
        }

        val finalVariants = if (state.isVariantMode) {
            state.variants.mapNotNull { row ->
                val p = row.price.toDoubleOrNull() ?: 0.0
                if (row.name.isNotBlank() && p > 0.0) {
                    MenuItemVariant(name = row.name.trim(), price = p)
                } else null
            }
        } else emptyList()

        val finalPrice = if (finalVariants.isNotEmpty()) {
            finalVariants.minOf { it.price }
        } else {
            state.salePrice.toDoubleOrNull() ?: 0.0
        }

        val item = POSMenuItem(
            id = itemId,
            code = itemCode,
            name = state.itemName.trim(),
            category = if (state.category.isBlank() || state.category == "None") "General" else state.category.trim(),
            price = finalPrice,
            imageUrl = state.imageUrl,
            isFavourite = state.isFavourite,
            variants = finalVariants
        )

        if (state.isEditMode) {
            menuRepo.updateMenuItem(item)
            viewModelScope.launch {
                menuRepo.saveMenuItemToBackend(item)
                menuRepo.fetchMenuItemsFromBackend()
            }
            _uiState.update {
                it.copy(toastMessage = "Menu Item '${item.name}' updated successfully!")
            }
        } else {
            menuRepo.addMenuItem(item)
            viewModelScope.launch {
                menuRepo.saveMenuItemToBackend(item)
                menuRepo.fetchMenuItemsFromBackend()
            }
            _uiState.update {
                it.copy(toastMessage = "Menu Item '${item.name}' created successfully!")
            }
        }

        onSuccess()
    }

    fun saveAndNew() {
        if (!_uiState.value.isFormValid) {
            _uiState.update {
                it.copy(
                    toastMessage = if (it.isVariantMode) {
                        "Please enter item name and valid variant names & prices"
                    } else {
                        "Please enter valid item name and sale price"
                    }
                )
            }
            return
        }

        val state = _uiState.value
        val finalVariants = if (state.isVariantMode) {
            state.variants.mapNotNull { row ->
                val p = row.price.toDoubleOrNull() ?: 0.0
                if (row.name.isNotBlank() && p > 0.0) {
                    MenuItemVariant(name = row.name.trim(), price = p)
                } else null
            }
        } else emptyList()

        val finalPrice = if (finalVariants.isNotEmpty()) {
            finalVariants.minOf { it.price }
        } else {
            state.salePrice.toDoubleOrNull() ?: 0.0
        }

        val newItem = POSMenuItem(
            id = UUID.randomUUID().toString(),
            code = (10..99).random().toString(),
            name = state.itemName.trim(),
            category = if (state.category.isBlank() || state.category == "None") "General" else state.category.trim(),
            price = finalPrice,
            imageUrl = state.imageUrl,
            isFavourite = state.isFavourite,
            variants = finalVariants
        )

        menuRepo.addMenuItem(newItem)

        viewModelScope.launch {
            menuRepo.saveMenuItemToBackend(newItem)
        }

        val cats = menuRepo.categories.filter { it.isNotBlank() && it != "All" }
        _uiState.update {
            AddMenuItemUiState(
                category = state.category,
                availableCategories = if (cats.isEmpty()) listOf("General") else cats,
                toastMessage = "Menu Item '${newItem.name}' saved! Enter next item."
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
