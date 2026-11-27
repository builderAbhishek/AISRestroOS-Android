package com.ais.restroos.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ais.restroos.core.auth.AuthEventManager
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.remote.RestroApiService
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.LoginRequest
import com.ais.restroos.data.remote.dtos.RegisterRequest
import com.ais.restroos.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val emailInput: String = "",
    val passwordInput: String = "",
    val nameInput: String = "",
    val restaurantNameInput: String = "",
    val phoneInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    var prefsManager: PreferencesManager? = null,
    private val customApiService: RestroApiService? = null
) : ViewModel() {

    private val apiService: RestroApiService
        get() = customApiService ?: RetrofitClient.apiService

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleAuthMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(nameInput = name, errorMessage = null) }
    }

    fun onRestaurantNameChange(restroName: String) {
        _uiState.update { it.copy(restaurantNameInput = restroName, errorMessage = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phoneInput = phone, errorMessage = null) }
    }

    fun verifyBackendSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val token = prefsManager?.getAuthToken()
            if (token.isNullOrBlank()) {
                _uiState.update { it.copy(isAuthenticated = false) }
                onResult(false)
                return@launch
            }

            RetrofitClient.updateAuthToken(token)

            try {
                val response = apiService.getMe()
                if (response.isSuccessful && response.body()?.status == "success") {
                    val authData = response.body()?.data
                    if (authData != null) {
                        prefsManager?.saveAuthSession(
                            token = token,
                            userName = authData.user.name,
                            userEmail = authData.user.email,
                            restaurantName = authData.restaurant_name?.takeIf { it.isNotBlank() } ?: "My Restaurant",
                            restaurantId = authData.restaurant_id ?: "AISR/01",
                            branchId = authData.branch_id,
                            role = authData.role
                        )
                    }
                    _uiState.update { it.copy(isAuthenticated = true) }
                    onResult(true)
                } else {
                    val code = response.code()
                    if (code == 401 || code == 403) {
                        prefsManager?.clearAuthSession()
                        RetrofitClient.updateAuthToken(null)
                        _uiState.update { it.copy(isAuthenticated = false) }
                        onResult(false)
                    } else {
                        // Transient server issue (e.g. 500, 502, 503) or maintenance:
                        // Preserve offline POS terminal access for valid local session
                        _uiState.update { it.copy(isAuthenticated = true) }
                        onResult(true)
                    }
                }
            } catch (e: Exception) {
                // If offline / network error but token was already locally verified,
                // allow offline access to preserve offline functionality
                _uiState.update { it.copy(isAuthenticated = true) }
                onResult(true)
            }
        }
    }

    fun submitAuth(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.emailInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (state.isLoginMode) {
                    val response = apiService.login(
                        LoginRequest(email = state.emailInput.trim(), password = state.passwordInput)
                    )
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val authData = response.body()?.data
                        authData?.token?.let { token ->
                            prefsManager?.saveAuthSession(
                                token = token,
                                userName = authData.user.name,
                                userEmail = authData.user.email,
                                restaurantName = authData.restaurant_name?.takeIf { it.isNotBlank() } ?: "My Restaurant",
                                restaurantId = authData.restaurant_id ?: "AISR/01",
                                branchId = authData.branch_id,
                                role = authData.role
                            )
                            RetrofitClient.updateAuthToken(token)
                            AuthEventManager.notifyLoginSuccess(token)
                        }
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true, successMessage = "Logged in successfully!") }
                        onSuccess()
                    } else {
                        val errorMsg = response.body()?.message ?: "Login failed. Check your email & password."
                        _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                    }
                } else {
                    if (state.nameInput.isBlank() || state.restaurantNameInput.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Please enter your name and restaurant name.") }
                        return@launch
                    }

                    val response = apiService.register(
                        RegisterRequest(
                            name = state.nameInput.trim(),
                            email = state.emailInput.trim(),
                            password = state.passwordInput,
                            restaurant_name = state.restaurantNameInput.trim(),
                            phone = state.phoneInput.ifBlank { null }
                        )
                    )
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val authData = response.body()?.data
                        authData?.token?.let { token ->
                            prefsManager?.saveAuthSession(
                                token = token,
                                userName = authData.user.name,
                                userEmail = authData.user.email,
                                restaurantName = state.restaurantNameInput.trim(),
                                restaurantId = authData.restaurant_id ?: "RESTRO-001",
                                branchId = authData.branch_id,
                                role = authData.role
                            )
                            RetrofitClient.updateAuthToken(token)
                            AuthEventManager.notifyLoginSuccess(token)
                        }
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true, successMessage = "Restaurant registered successfully!") }
                        onSuccess()
                    } else {
                        val errorMsg = response.body()?.message ?: "Registration failed. Please check inputs."
                        _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Network error: ${e.localizedMessage ?: "Unable to connect to server"}") }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                apiService.logout()
            } catch (_: Exception) { }
            prefsManager?.clearAuthSession()
            RetrofitClient.updateAuthToken(null)
            OrderRepository.resetToSampleData()
            AuthEventManager.notifyLoggedOut()
            _uiState.update { AuthUiState() }
            onLoggedOut()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
