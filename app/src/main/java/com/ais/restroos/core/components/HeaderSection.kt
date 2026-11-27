package com.ais.restroos.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.AvatarGoldBg
import com.ais.restroos.core.theme.AvatarGoldText
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.data.sync.SyncManager
import com.ais.restroos.data.sync.SyncState

@Composable
fun TopHeaderSection(
    restaurantName: String,
    onBranchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncState by SyncManager.syncState.collectAsState()

    val (syncText, syncTextColor, syncBgColor) = when (val s = syncState) {
        is SyncState.Syncing -> Triple("🔄 Syncing...", Color(0xFF0D47A1), Color(0xFFE3F2FD))
        is SyncState.Synced  -> Triple("🟢 Online • Synced", Color(0xFF1B5E20), Color(0xFFE8F5E9))
        is SyncState.Pending -> Triple("🟠 Offline • ${s.count} Pending", Color(0xFFE65100), Color(0xFFFFF3E0))
        is SyncState.Error   -> Triple("🔴 Sync Error", Color(0xFFB71C1C), Color(0xFFFFEBEE))
        is SyncState.Idle    -> Triple("🟢 Online", Color(0xFF1B5E20), Color(0xFFE8F5E9))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBranchClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = restaurantName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = DarkNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Branch",
                    tint = DarkNavy,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Branch  •  ",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = RoyalBlue
                )
                // Visible Sync Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(syncBgColor)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = syncText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = syncTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Profile Avatar Badge with App Logo
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.ais.restroos.R.drawable.app_logo),
                contentDescription = "AISRestroOS Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}
