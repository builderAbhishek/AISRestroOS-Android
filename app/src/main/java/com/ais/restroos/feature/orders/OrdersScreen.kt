package com.ais.restroos.feature.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.RoyalBlueLight
import com.ais.restroos.core.theme.StatusGreen
import com.ais.restroos.core.theme.StatusGreenLight
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary

enum class OrderFilter {
    ALL, CLOSED, ON_HOLD
}

@Composable
fun OrdersScreen(
    initialFilter: OrderFilter = OrderFilter.ALL,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleOrders = listOf(
        OrderSummaryUi("ORD-1042", "Table 4", 3, 480.00, "CLOSED", "Today, 02:15 PM"),
        OrderSummaryUi("ORD-1041", "Table 2", 5, 1250.00, "CLOSED", "Today, 01:45 PM"),
        OrderSummaryUi("ORD-1040", "Parcel / Takeaway", 2, 320.00, "ON_HOLD", "Today, 01:20 PM"),
        OrderSummaryUi("ORD-1039", "Table 8", 4, 890.00, "CLOSED", "Today, 12:50 PM"),
        OrderSummaryUi("ORD-1038", "Table 1", 1, 150.00, "ON_HOLD", "Today, 12:10 PM")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = DarkNavy
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when (initialFilter) {
                        OrderFilter.CLOSED -> "Closed Orders"
                        OrderFilter.ON_HOLD -> "On Hold Orders"
                        OrderFilter.ALL -> "POS Orders & History"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkNavy
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleOrders.filter {
                when (initialFilter) {
                    OrderFilter.CLOSED -> it.status == "CLOSED"
                    OrderFilter.ON_HOLD -> it.status == "ON_HOLD"
                    OrderFilter.ALL -> true
                }
            }) { order ->
                OrderItemCard(order = order)
            }
        }
    }
}

private data class OrderSummaryUi(
    val orderId: String,
    val source: String,
    val items: Int,
    val amount: Double,
    val status: String,
    val time: String
)

@Composable
private fun OrderItemCard(order: OrderSummaryUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (order.status == "CLOSED") StatusGreenLight else RoyalBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (order.status == "CLOSED") Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = order.status,
                        tint = if (order.status == "CLOSED") StatusGreen else RoyalBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = order.orderId,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkNavy
                        )
                    )
                    Text(
                        text = "${order.source} • ${order.items} items • ${order.time}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    )
                }
            }

            Text(
                text = "₹ ${String.format("%.2f", order.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkNavy
                )
            )
        }
    }
}