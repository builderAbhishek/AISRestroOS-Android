package com.ais.restroos.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun FloatingCartOverlay(
    totalItems: Int,
    subtotal: Double,
    customerName: String?,
    onViewCartClick: () -> Unit,
    onCustomerDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalItems <= 0) return

    val subtotalFormatted = if (subtotal % 1.0 == 0.0) {
        "₹${DecimalFormat("#,##0").format(subtotal.toLong())}"
    } else {
        "₹${DecimalFormat("#,##0.00").format(subtotal)}"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A. View Cart Wide Translucent Panel
        Surface(
            onClick = onViewCartClick,
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 3.dp,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "View Cart",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkNavy
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$totalItems Items  $subtotalFormatted" + if (!customerName.isNullOrBlank()) " • $customerName" else "",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFF4B5563)
                    )
                )
            }
        }

        // B. Cart Chevron Shortcut Button
        Surface(
            onClick = onViewCartClick,
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go to Cart",
                    tint = DarkNavy,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // C. Customer Details Shortcut Button
        Surface(
            onClick = onCustomerDetailsClick,
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = "Customer Details",
                    tint = if (!customerName.isNullOrBlank()) Color(0xFF2864E8) else DarkNavy,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}