package com.ais.restroos.core.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AuthEvent {
    data class SessionExpired(val message: String = "Session expired. Please log in again.") : AuthEvent()
    data class LoginSuccess(val token: String) : AuthEvent()
    object LoggedOut : AuthEvent()
}

object AuthEventManager {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun notifySessionExpired(message: String = "Session expired. Please log in again.") {
        _events.tryEmit(AuthEvent.SessionExpired(message))
    }

    fun notifyLoginSuccess(token: String) {
        _events.tryEmit(AuthEvent.LoginSuccess(token))
    }

    fun notifyLoggedOut() {
        _events.tryEmit(AuthEvent.LoggedOut)
    }
}
