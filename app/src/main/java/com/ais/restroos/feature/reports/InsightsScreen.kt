package com.ais.restroos.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.SaffronPrimary
import com.ais.restroos.core.theme.StatusGreen
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.ItemSalesReportDto
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.data.repository.PurchaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class InsightsPeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    ALL_TIME("All Time")
}

private data class TopItemStat(
    val name: String,
    val quantity: Int,
    val revenue: Double
)

private data class TimeSlotStat(
    val label: String,
    val subLabel: String,
    val orders: Int,
    val revenue: Double
)

@Composable
fun InsightsScreen(
    onBackClick: () -> Unit,
    onNavigateToDailyHisaab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager(context) }
    val currencyFormat = remember { DecimalFormat("#,##0") }
    val decimalFormat = remember { DecimalFormat("#,##0.00") }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val closedOrders by OrderRepository.closedOrders.collectAsState()
    val hisaabSummary by PurchaseRepository.summary.collectAsState()

    var selectedPeriod by remember { mutableStateOf(InsightsPeriod.TODAY) }
    var backendItemReport by remember { mutableStateOf<List<ItemSalesReportDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshAll() {
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    OrderRepository.fetchOrdersFromBackend()
                    val resp = RetrofitClient.apiService.getItemSalesReport()
                    if (resp.isSuccessful && resp.body()?.status == "success") {
                        backendItemReport = resp.body()?.data ?: emptyList()
                    }
                    val restroId = prefsManager.getRestaurantId()
                    PurchaseRepository.loadHisaab(context, restroId, todayDateStr)
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

    // Time boundaries
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val yesterdayStart = remember(todayStart) {
        Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
    }
    val weekStart = remember(todayStart) {
        Calendar.getInstance().apply {
            timeInMillis = todayStart
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }.timeInMillis
    }

    val filteredOrders = remember(closedOrders, selectedPeriod, todayStart, yesterdayStart, weekStart) {
        when (selectedPeriod) {
            InsightsPeriod.TODAY -> closedOrders.filter { it.completedTimestamp >= todayStart }
            InsightsPeriod.YESTERDAY -> closedOrders.filter { it.completedTimestamp in yesterdayStart until todayStart }
            InsightsPeriod.THIS_WEEK -> closedOrders.filter { it.completedTimestamp >= weekStart }
            InsightsPeriod.ALL_TIME -> closedOrders
        }
    }

    val totalRevenue = filteredOrders.sumOf { it.totalAmount }
    val totalOrdersCount = filteredOrders.size
    val avgBillValue = if (totalOrdersCount > 0) totalRevenue / totalOrdersCount else 0.0

    // Yesterday revenue for growth comparison on Today
    val yesterdayRevenue = remember(closedOrders, todayStart, yesterdayStart) {
        closedOrders.filter { it.completedTimestamp in yesterdayStart until todayStart }.sumOf { it.totalAmount }
    }

    // Service type split
    val dineInOrders = filteredOrders.filter { it.orderType.equals("DINE_IN", ignoreCase = true) }
    val takeawayOrders = filteredOrders.filter { !it.orderType.equals("DINE_IN", ignoreCase = true) }
    val dineInRevenue = dineInOrders.sumOf { it.totalAmount }
    val takeawayRevenue = takeawayOrders.sumOf { it.totalAmount }

    // Top Selling Items for selected period
    val topItems = remember(filteredOrders, backendItemReport, selectedPeriod) {
        if (selectedPeriod == InsightsPeriod.ALL_TIME && backendItemReport.isNotEmpty()) {
            backendItemReport.map {
                TopItemStat(it.item_name, it.total_quantity, it.total_amount)
            }.sortedByDescending { it.quantity }.take(8)
        } else {
            val map = linkedMapOf<String, TopItemStat>()
            filteredOrders.forEach { order ->
                order.items.forEach { item ->
                    val existing = map[item.name]
                    val lineRev = item.price * item.quantity
                    if (existing == null) {
                        map[item.name] = TopItemStat(item.name, item.quantity, lineRev)
                    } else {
                        map[item.name] = existing.copy(
                            quantity = existing.quantity + item.quantity,
                            revenue = existing.revenue + lineRev
                        )
                    }
                }
            }
            map.values.sortedByDescending { it.quantity }.take(8)
        }
    }

    // Peak Hours distribution
    val timeSlots = remember(filteredOrders) {
        var mOrders = 0; var mRev = 0.0
        var aOrders = 0; var aRev = 0.0
        var eOrders = 0; var eRev = 0.0
        var nOrders = 0; var nRev = 0.0
        val cal = Calendar.getInstance()
        filteredOrders.forEach { order ->
            cal.timeInMillis = order.completedTimestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..11 -> { mOrders++; mRev += order.totalAmount }
                in 12..15 -> { aOrders++; aRev += order.totalAmount }
                in 16..19 -> { eOrders++; eRev += order.totalAmount }
                else -> { nOrders++; nRev += order.totalAmount }
            }
        }
        listOf(
            TimeSlotStat("Morning", "6 AM - 12 PM", mOrders, mRev),
            TimeSlotStat("Afternoon", "12 PM - 4 PM", aOrders, aRev),
            TimeSlotStat("Evening", "4 PM - 8 PM", eOrders, eRev),
            TimeSlotStat("Night", "8 PM - Late", nOrders, nRev)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Business Insights",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = DarkNavy
                            )
                        )
                        Text(
                            text = "Live Sales, Top Items & Peak Hours",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = RoyalBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp)
                    )
                } else {
                    IconButton(onClick = { refreshAll() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Insights",
                            tint = RoyalBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Period Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightsPeriod.entries.forEach { period ->
                        val isSel = selectedPeriod == period
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPeriod = period },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSel) RoyalBlue else SurfaceWhite,
                            border = BorderStroke(1.dp, if (isSel) RoyalBlue else LightBorder)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = period.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else DarkNavy,
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Owner KPI 2x2 Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val vsYesterdayText = if (selectedPeriod == InsightsPeriod.TODAY && yesterdayRevenue > 0) {
                            val pct = (((totalRevenue - yesterdayRevenue) / yesterdayRevenue) * 100).toInt()
                            if (pct >= 0) "+$pct% vs yesterday" else "$pct% vs yesterday"
                        } else {
                            "${selectedPeriod.label} revenue"
                        }

                        InsightKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Sales",
                            value = "₹${decimalFormat.format(totalRevenue)}",
                            subtitle = vsYesterdayText,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = StatusGreen,
                            bgTint = Color(0xFFF0FDF4)
                        )

                        InsightKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Orders",
                            value = "$totalOrdersCount",
                            subtitle = "Completed bills",
                            icon = Icons.Default.Receipt,
                            accentColor = RoyalBlue,
                            bgTint = Color(0xFFFFF7ED)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InsightKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Avg. Bill Value",
                            value = "₹${currencyFormat.format(avgBillValue)}",
                            subtitle = "Per order average",
                            icon = Icons.Default.Restaurant,
                            accentColor = Color(0xFF2563EB),
                            bgTint = Color(0xFFEFF6FF)
                        )

                        val remainVal = totalRevenue - hisaabSummary.total_outflow
                        InsightKpiCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToDailyHisaab() },
                            title = if (selectedPeriod == InsightsPeriod.TODAY) "Today's Remain" else "Total Units Sold",
                            value = if (selectedPeriod == InsightsPeriod.TODAY) {
                                "₹${currencyFormat.format(remainVal)}"
                            } else {
                                "${topItems.sumOf { it.quantity }}"
                            },
                            subtitle = if (selectedPeriod == InsightsPeriod.TODAY) {
                                "Expense: ₹${currencyFormat.format(hisaabSummary.total_outflow)}"
                            } else {
                                "Tap for Daily Hisaab"
                            },
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = if (selectedPeriod == InsightsPeriod.TODAY && remainVal < 0) Color(0xFFDC2626) else Color(0xFF7C3AED),
                            bgTint = if (selectedPeriod == InsightsPeriod.TODAY && remainVal < 0) Color(0xFFFEF2F2) else Color(0xFFF5F3FF)
                        )
                    }
                }
            }

            // 3. Dine-In vs Takeaway Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Order Channel Split",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DarkNavy
                            )
                        )

                        val dineInRatio = if (totalOrdersCount > 0) dineInOrders.size.toFloat() / totalOrdersCount.toFloat() else 0.5f

                        // Visual Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (totalOrdersCount > 0) {
                                    if (dineInRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(dineInRatio.coerceAtLeast(0.01f))
                                                .background(SaffronPrimary)
                                        )
                                    }
                                    val takeawayRatio = 1f - dineInRatio
                                    if (takeawayRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(takeawayRatio.coerceAtLeast(0.01f))
                                                .background(Color(0xFF2563EB))
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SaffronPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Dine-In (${dineInOrders.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 12.5.sp
                                        )
                                    )
                                    Text(
                                        text = "₹${currencyFormat.format(dineInRevenue)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Takeaway / Parcel (${takeawayOrders.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 12.5.sp
                                        )
                                    )
                                    Text(
                                        text = "₹${currencyFormat.format(takeawayRevenue)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Top Selling Items Leaderboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Top Selling Items",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                        color = DarkNavy
                                    )
                                )
                            }

                            Text(
                                text = selectedPeriod.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        if (topItems.isEmpty()) {
                            Text(
                                text = "No item sales recorded for ${selectedPeriod.label.lowercase()}.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            val maxQty = topItems.maxOf { it.quantity }.coerceAtLeast(1)
                            topItems.forEachIndexed { index, item ->
                                val rankColor = when (index) {
                                    0 -> Color(0xFFD97706) // Gold
                                    1 -> Color(0xFF64748B) // Silver
                                    2 -> Color(0xFFB45309) // Bronze
                                    else -> RoyalBlue
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(rankColor.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = rankColor,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DarkNavy,
                                                    fontSize = 13.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${currencyFormat.format(item.revenue)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkNavy,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                            Text(
                                                text = "${item.quantity} sold",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }

                                    // Proportional Bar
                                    val ratio = item.quantity.toFloat() / maxQty.toFloat()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFF1F5F9))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(ratio.coerceIn(0.05f, 1f))
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(rankColor)
                                        )
                                    }
                                }

                                if (index < topItems.size - 1) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Peak Business Hours Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, LightBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Peak Business Hours",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = DarkNavy
                                )
                            )
                        }

                        val maxSlotOrders = timeSlots.maxOf { it.orders }.coerceAtLeast(1)
                        timeSlots.forEach { slot ->
                            val ratio = slot.orders.toFloat() / maxSlotOrders.toFloat()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(96.dp)) {
                                    Text(
                                        text = slot.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 12.5.sp
                                        )
                                    )
                                    Text(
                                        text = slot.subLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFFF1F5F9))
                                ) {
                                    if (slot.orders > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(ratio.coerceIn(0.06f, 1f))
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(RoyalBlue)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(
                                    modifier = Modifier.width(72.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "${slot.orders} ord",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = "₹${currencyFormat.format(slot.revenue)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    bgTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = DarkNavy
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
