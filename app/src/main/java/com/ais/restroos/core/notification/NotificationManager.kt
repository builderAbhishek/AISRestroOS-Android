package com.ais.restroos.core.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.StatusGreen
import com.ais.restroos.core.theme.StatusRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationType {
    object Error : NotificationType()
    object Success : NotificationType()
    object Info : NotificationType()
}

data class AppNotification(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: NotificationType = NotificationType.Info
)

object NotificationManager {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var dismissJob: Job? = null

    private val _currentNotification = MutableStateFlow<AppNotification?>(null)
    val currentNotification: StateFlow<AppNotification?> = _currentNotification.asStateFlow()

    fun showError(message: String) {
        show(message, NotificationType.Error)
    }

    fun showSuccess(message: String) {
        show(message, NotificationType.Success)
    }

    fun showInfo(message: String) {
        show(message, NotificationType.Info)
    }

    fun dismiss() {
        dismissJob?.cancel()
        _currentNotification.value = null
    }

    private fun show(message: String, type: NotificationType) {
        dismissJob?.cancel()
        _currentNotification.value = AppNotification(message = message, type = type)
        dismissJob = scope.launch {
            delay(4000)
            _currentNotification.value = null
        }
    }
}

@Composable
fun AppNotificationBanner(
    modifier: Modifier = Modifier
) {
    val notification by NotificationManager.currentNotification.collectAsState()

    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        notification?.let { item ->
            val (icon, iconColor, borderColor, bgColor) = when (item.type) {
                is NotificationType.Error -> Quad(
                    Icons.Default.ErrorOutline,
                    StatusRed,
                    StatusRed.copy(alpha = 0.5f),
                    Color(0xFF2A1215)
                )
                is NotificationType.Success -> Quad(
                    Icons.Default.CheckCircle,
                    StatusGreen,
                    StatusGreen.copy(alpha = 0.5f),
                    Color(0xFF10261A)
                )
                is NotificationType.Info -> Quad(
                    Icons.Default.Info,
                    Color(0xFF64B5F6),
                    Color(0xFF64B5F6).copy(alpha = 0.4f),
                    Color(0xFF131D2E)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(iconColor.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 3
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { NotificationManager.dismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
