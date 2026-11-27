package com.ais.restroos.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ais.restroos.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ais_restro_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_SELECTED_BRANCH_ID = stringPreferencesKey("selected_branch_id")
        val KEY_PRINTER_IP = stringPreferencesKey("printer_ip")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_ROLE = stringPreferencesKey("user_role")
        val KEY_RESTAURANT_NAME = stringPreferencesKey("restaurant_name")
        val KEY_RESTAURANT_ID = stringPreferencesKey("restaurant_id")

        // Bluetooth Thermal Printer Keys
        val KEY_PRINTER_MAC = stringPreferencesKey("printer_mac")
        val KEY_PRINTER_NAME = stringPreferencesKey("printer_name")
        val KEY_PRINTER_PAPER_WIDTH = stringPreferencesKey("printer_paper_width") // "58mm" or "80mm"
        val KEY_PRINTER_AUTO_CUT = booleanPreferencesKey("printer_auto_cut")
        val KEY_PRINTER_COPIES = intPreferencesKey("printer_copies")

        // Order & POS Preferences
        val KEY_KOT_PRINT_ENABLED = booleanPreferencesKey("pref_kot_print_enabled")
        val KEY_IMAGE_VIEW_ENABLED = booleanPreferencesKey("pref_image_view_enabled")
        val KEY_AUTO_PRINT_BILL = booleanPreferencesKey("pref_auto_print_bill")
        val KEY_DEFAULT_TAX_PERCENT = stringPreferencesKey("pref_default_tax_percent")
        val KEY_DEFAULT_ORDER_TYPE = stringPreferencesKey("pref_default_order_type")

        // Daily Subscription Check Cache
        val KEY_SUB_LAST_CHECK_DATE = stringPreferencesKey("sub_last_check_date")
        val KEY_SUB_IS_EXPIRED = booleanPreferencesKey("sub_is_expired")
        val KEY_SUB_PLAN_NAME = stringPreferencesKey("sub_plan_name")
        val KEY_SUB_EXPIRES_AT = stringPreferencesKey("sub_expires_at")

        // Receipt Customization Keys
        val KEY_RECEIPT_HEADER = stringPreferencesKey("receipt_header_text")
        val KEY_RECEIPT_FOOTER = stringPreferencesKey("receipt_footer_text")
        val KEY_RECEIPT_QR_URL = stringPreferencesKey("receipt_qr_url")
        val KEY_RECEIPT_SHOW_QR = booleanPreferencesKey("receipt_show_qr")
        val KEY_RECEIPT_FONT_SIZE = intPreferencesKey("receipt_font_size")

        fun getCachedToken(context: Context): String? {
            return context.getSharedPreferences("ais_auth_cache", Context.MODE_PRIVATE)
                .getString("cached_auth_token", null)
        }

        fun getCachedRestaurantId(context: Context): String? {
            return context.getSharedPreferences("ais_auth_cache", Context.MODE_PRIVATE)
                .getString("cached_restaurant_id", null)
        }
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_AUTH_TOKEN]
        RetrofitClient.updateAuthToken(token)
        token
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_AUTH_TOKEN].isNullOrBlank()
    }

    suspend fun getAuthToken(): String? {
        val prefs = context.dataStore.data.first()
        val token = prefs[KEY_AUTH_TOKEN]
        RetrofitClient.updateAuthToken(token)
        return token
    }

    val selectedBranchId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_BRANCH_ID] ?: "1"
    }

    val restaurantId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_RESTAURANT_ID] ?: "RESTRO-001"
    }

    suspend fun getRestaurantId(): String {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_RESTAURANT_ID] ?: "RESTRO-001"
    }

    val restaurantName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_RESTAURANT_NAME] ?: "My Restaurant"
    }

    val userEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL] ?: ""
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: ""
    }

    suspend fun updateRestaurantName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RESTAURANT_NAME] = name
        }
    }

    suspend fun getSubscriptionCheckState(): Triple<String, Boolean, String> {
        val prefs = context.dataStore.data.first()
        return Triple(
            prefs[KEY_SUB_LAST_CHECK_DATE] ?: "",
            prefs[KEY_SUB_IS_EXPIRED] ?: false,
            prefs[KEY_SUB_PLAN_NAME] ?: ""
        )
    }

    suspend fun saveSubscriptionCheck(dateStr: String, isExpired: Boolean, planName: String, expiresAt: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUB_LAST_CHECK_DATE] = dateStr
            prefs[KEY_SUB_IS_EXPIRED] = isExpired
            prefs[KEY_SUB_PLAN_NAME] = planName
            prefs[KEY_SUB_EXPIRES_AT] = expiresAt
        }
    }

    val printerMac: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRINTER_MAC]
    }

    val printerName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRINTER_NAME]
    }

    val printerPaperWidth: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRINTER_PAPER_WIDTH] ?: "58mm"
    }

    val printerAutoCut: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRINTER_AUTO_CUT] ?: false
    }

    val printerCopies: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRINTER_COPIES] ?: 1
    }

    // Order & POS Flow Preferences
    val kotPrintEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_KOT_PRINT_ENABLED] ?: true
    }

    val isImageViewEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IMAGE_VIEW_ENABLED] ?: true
    }

    val autoPrintBill: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_PRINT_BILL] ?: false
    }

    val defaultTaxPercent: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_TAX_PERCENT]?.toDoubleOrNull() ?: 0.0
    }

    val defaultOrderType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_ORDER_TYPE] ?: "DINE_IN"
    }

    suspend fun setKotPrintEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KOT_PRINT_ENABLED] = enabled }
    }

    suspend fun setImageViewEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_IMAGE_VIEW_ENABLED] = enabled }
    }

    suspend fun setAutoPrintBill(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_PRINT_BILL] = enabled }
    }

    suspend fun setDefaultTaxPercent(tax: Double) {
        context.dataStore.edit { it[KEY_DEFAULT_TAX_PERCENT] = tax.toString() }
    }

    suspend fun setDefaultOrderType(type: String) {
        context.dataStore.edit { it[KEY_DEFAULT_ORDER_TYPE] = type }
    }

    suspend fun savePrinterConfig(
        macAddress: String,
        name: String,
        paperWidth: String = "58mm",
        autoCut: Boolean = false,
        copies: Int = 1
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRINTER_MAC] = macAddress
            prefs[KEY_PRINTER_NAME] = name
            prefs[KEY_PRINTER_PAPER_WIDTH] = paperWidth
            prefs[KEY_PRINTER_AUTO_CUT] = autoCut
            prefs[KEY_PRINTER_COPIES] = copies
        }
    }

    suspend fun saveAuthSession(
        token: String,
        userName: String,
        userEmail: String,
        restaurantName: String,
        restaurantId: String,
        branchId: String? = null,
        role: String? = null
    ) {
        RetrofitClient.updateAuthToken(token)
        context.getSharedPreferences("ais_auth_cache", Context.MODE_PRIVATE)
            .edit()
            .putString("cached_auth_token", token)
            .putString("cached_restaurant_id", restaurantId)
            .apply()

        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
            prefs[KEY_USER_NAME] = userName
            prefs[KEY_USER_EMAIL] = userEmail
            prefs[KEY_RESTAURANT_NAME] = restaurantName
            prefs[KEY_RESTAURANT_ID] = restaurantId
            branchId?.let { prefs[KEY_SELECTED_BRANCH_ID] = it }
            role?.let { prefs[KEY_USER_ROLE] = it }
        }
    }

    suspend fun clearAuthSession() {
        RetrofitClient.updateAuthToken(null)
        context.getSharedPreferences("ais_auth_cache", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        context.dataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_ROLE)
            prefs.remove(KEY_RESTAURANT_NAME)
            prefs.remove(KEY_RESTAURANT_ID)
            prefs.remove(KEY_SELECTED_BRANCH_ID)
        }
    }

    suspend fun saveSelectedBranchId(branchId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_BRANCH_ID] = branchId
        }
    }

    suspend fun getReceiptSettings(): com.ais.restroos.data.remote.dtos.ReceiptSettingsDto {
        val prefs = context.dataStore.data.first()
        val restroName = prefs[KEY_RESTAURANT_NAME] ?: ""
        return com.ais.restroos.data.remote.dtos.ReceiptSettingsDto(
            header_text = prefs[KEY_RECEIPT_HEADER] ?: restroName,
            footer_text = prefs[KEY_RECEIPT_FOOTER] ?: "Thank you! Visit Again\nPowered by AISRestroOS POS",
            qr_image_url = prefs[KEY_RECEIPT_QR_URL],
            show_qr = prefs[KEY_RECEIPT_SHOW_QR] ?: true,
            paper_width = prefs[KEY_PRINTER_PAPER_WIDTH] ?: "58mm",
            font_size = (prefs[KEY_RECEIPT_FONT_SIZE] ?: 18).coerceIn(14, 24)
        )
    }

    suspend fun saveReceiptSettings(dto: com.ais.restroos.data.remote.dtos.ReceiptSettingsDto) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RECEIPT_HEADER] = dto.header_text
            prefs[KEY_RECEIPT_FOOTER] = dto.footer_text
            if (dto.qr_image_url.isNullOrBlank()) {
                prefs.remove(KEY_RECEIPT_QR_URL)
            } else {
                prefs[KEY_RECEIPT_QR_URL] = dto.qr_image_url
            }
            prefs[KEY_RECEIPT_SHOW_QR] = dto.show_qr
            prefs[KEY_PRINTER_PAPER_WIDTH] = dto.paper_width.ifBlank { "58mm" }
            prefs[KEY_RECEIPT_FONT_SIZE] = dto.font_size.coerceIn(14, 24)
        }
    }

    fun getQrCacheFile(): java.io.File {
        return java.io.File(context.filesDir, "receipt_qr_cache.png")
    }
}
