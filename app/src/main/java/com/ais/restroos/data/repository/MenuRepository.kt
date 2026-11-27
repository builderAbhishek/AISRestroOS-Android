package com.ais.restroos.data.repository

import android.content.Context
import com.ais.restroos.data.local.LocalDatabaseHelper
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.CategorySyncDto
import com.ais.restroos.data.remote.dtos.MenuItemSyncDto
import com.ais.restroos.data.remote.dtos.MenuItemVariantDto
import com.ais.restroos.domain.model.MenuItemVariant
import com.ais.restroos.domain.model.POSMenuItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MenuRepository {

    private var dbHelper: LocalDatabaseHelper? = null

    private val _menuItems = MutableStateFlow<List<POSMenuItem>>(emptyList())
    val menuItems: StateFlow<List<POSMenuItem>> = _menuItems.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categoriesFlow: StateFlow<List<String>> = _categories.asStateFlow()

    val categories: List<String>
        get() {
            val list = (_categories.value + _menuItems.value.map { it.category })
                .map { it.trim() }
                .filter { it.isNotBlank() && it != "All" && it != "None" }
                .distinct()
            return listOf("All") + list
        }

    fun initialize(context: Context) {
        dbHelper = LocalDatabaseHelper.getInstance(context)
        loadFromLocalDatabase()
    }

    fun loadFromLocalDatabase() {
        val db = dbHelper ?: return
        val localItems = db.getAllMenuItems()
        if (localItems.isNotEmpty()) {
            _menuItems.value = localItems
        }
        val localCats = db.getAllCategories()
        val itemCats = localItems.map { it.category }.filter { it.isNotBlank() }
        val merged = (localCats + itemCats).distinct()
        if (merged.isNotEmpty()) {
            _categories.value = merged
        }
    }

    fun addCategory(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || cleanName.equals("All", ignoreCase = true)) return

        if (!_categories.value.contains(cleanName)) {
            _categories.update { it + cleanName }
            dbHelper?.saveCategory(cleanName)

            // Sync with backend asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dto = CategorySyncDto(
                        id = "CAT-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
                        name = cleanName
                    )
                    RetrofitClient.apiService.createCategory(dto)
                } catch (_: Exception) {
                    // Stored in SQLite for future sync
                }
            }
        }
    }

    fun setMenuItems(items: List<POSMenuItem>) {
        _menuItems.value = items
        dbHelper?.saveMenuItems(items, "SYNCED")
    }

    fun addMenuItem(item: POSMenuItem) {
        dbHelper?.saveMenuItem(item, "PENDING")
        _menuItems.update { current ->
            val index = current.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                current.toMutableList().apply { set(index, item) }
            } else {
                listOf(item) + current
            }
        }
    }

    fun updateMenuItem(item: POSMenuItem) {
        dbHelper?.saveMenuItem(item, "PENDING")
        _menuItems.update { current ->
            current.map { if (it.id == item.id) item else it }
        }
    }

    suspend fun saveMenuItemToBackend(item: POSMenuItem): Result<POSMenuItem> = withContext(Dispatchers.IO) {
        // 1. Save locally with PENDING status
        dbHelper?.saveMenuItem(item, "PENDING")
        addMenuItem(item)

        // 2. Attempt online creation
        try {
            val dto = MenuItemSyncDto(
                id = item.id,
                code = item.code.ifBlank { null },
                name = item.name,
                category = item.category,
                price = item.price,
                image_url = item.imageUrl,
                is_favourite = item.isFavourite,
                variants = item.variants.map { MenuItemVariantDto(it.name, it.price) }
            )
            val response = RetrofitClient.apiService.createMenuItem(dto)
            if (response.isSuccessful && response.body()?.status == "success") {
                val savedDto = response.body()?.data
                val savedItem = if (savedDto != null) {
                    POSMenuItem(
                        id = savedDto.id,
                        code = savedDto.code ?: item.code,
                        name = savedDto.name,
                        category = savedDto.category,
                        price = savedDto.price,
                        imageUrl = savedDto.image_url ?: item.imageUrl,
                        isFavourite = savedDto.is_favourite,
                        variants = if (!savedDto.variants.isNullOrEmpty()) {
                            savedDto.variants.map { MenuItemVariant(it.name, it.price) }
                        } else item.variants
                    )
                } else item

                dbHelper?.saveMenuItem(savedItem, "SYNCED")
                Result.success(savedItem)
            } else {
                val errMsg = response.body()?.message ?: "Server rejected menu item (HTTP ${response.code()})"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            // Keep in local SQLite as PENDING for background sync
            Result.failure(e)
        }
    }

    fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.startsWith("/")) {
            "https://restro.trakiyo.in/api$trimmed"
        } else {
            "https://restro.trakiyo.in/api/$trimmed"
        }
    }

    suspend fun uploadDishImage(
        compressedFile: java.io.File,
        itemId: String,
        oldImageUrl: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val reqFile = compressedFile.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("image", compressedFile.name, reqFile)
            val idBody = itemId.toRequestBody("text/plain".toMediaTypeOrNull())
            val oldUrlBody = oldImageUrl?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = RetrofitClient.apiService.uploadItemImage(part, idBody, oldUrlBody)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                val url = data?.get("url") ?: ""
                val fullUrl = normalizeImageUrl(url) ?: url
                Result.success(fullUrl)
            } else {
                val errMsg = response.body()?.message ?: "Failed to upload image (${response.code()})"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMenuItemsFromBackend(): Result<List<POSMenuItem>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.getMenuItems()
            if (response.isSuccessful && response.body()?.status == "success") {
                val dtoList = response.body()?.data ?: emptyList()
                val posList = dtoList.map { dto ->
                    POSMenuItem(
                        id = dto.id,
                        code = dto.code ?: "",
                        name = dto.name,
                        category = dto.category,
                        price = dto.price,
                        imageUrl = normalizeImageUrl(dto.image_url),
                        isFavourite = dto.is_favourite,
                        variants = dto.variants.orEmpty().map { MenuItemVariant(it.name, it.price) }
                    )
                }
                _menuItems.value = posList
                dbHelper?.saveMenuItems(posList, "SYNCED")
                fetchCategoriesFromBackend()
                Result.success(posList)
            } else {
                loadFromLocalDatabase()
                Result.failure(Exception(response.body()?.message ?: "Failed to load menu items"))
            }
        } catch (e: Exception) {
            loadFromLocalDatabase()
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        _menuItems.update { current -> current.filter { it.id != id } }
        dbHelper?.deleteMenuItem(id)
        try {
            RetrofitClient.apiService.deleteMenuItem(id)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCategoriesFromBackend(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.getCategories()
            if (response.isSuccessful && response.body()?.status == "success") {
                val cats = response.body()?.data?.map { it.name } ?: emptyList()
                if (cats.isNotEmpty()) {
                    _categories.update { current -> (current + cats).distinct() }
                    dbHelper?.saveCategories(cats)
                }
                Result.success(cats)
            } else {
                Result.failure(Exception("Failed to load categories"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetToSampleData() {
        _menuItems.value = emptyList()
    }
}
