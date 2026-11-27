package com.ais.restroos.feature.reports

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.RoyalBlueLight
import com.ais.restroos.core.theme.StatusGreen
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.ItemSalesReportDto
import com.ais.restroos.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

data class ItemSalesUiEntry(
    val itemName: String,
    val quantity: Int,
    val totalRevenue: Double
)

@Composable
fun ItemReportsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val closedOrders by OrderRepository.closedOrders.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var backendItems by remember { mutableStateOf<List<ItemSalesReportDto>>(emptyList()) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    OrderRepository.fetchOrdersFromBackend()
                    val resp = RetrofitClient.apiService.getItemSalesReport()
                    if (resp.isSuccessful && resp.body()?.status == "success") {
                        backendItems = resp.body()?.data ?: emptyList()
                    }
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // Direct live data from backend server database (no temp or fallback blending)
    val allEntries = backendItems.map { bi ->
        ItemSalesUiEntry(
            itemName = bi.item_name,
            quantity = bi.total_quantity,
            totalRevenue = bi.total_amount
        )
    }.sortedByDescending { it.quantity }

    val filteredEntries = allEntries.filter { item ->
        searchQuery.trim().isEmpty() || item.itemName.contains(searchQuery.trim(), ignoreCase = true)
    }

    val totalUnitsSold = allEntries.sumOf { it.quantity }
    val totalItemRevenue = allEntries.sumOf { it.totalRevenue }
    val topSellingItem = allEntries.firstOrNull()?.itemName ?: "N/A"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // A. Modern Dark Navy Header (matches Home Screen)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "Item Sales Reports",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Dish Popularity & Quantity Sales",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                        )
                    }
                }

                IconButton(onClick = { loadData() }) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFFFB923C))
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFB923C), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. KPI Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Units Sold", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, color = TextSecondary))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$totalUnitsSold", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RoyalBlue))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Item Revenue", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, color = TextSecondary))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹${DecimalFormat("#,##0").format(totalItemRevenue)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StatusGreen))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Top Dish", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, color = TextSecondary))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(topSellingItem, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkNavy), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // 2. Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search dish name...", fontSize = 13.sp, color = TextSecondary) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = LightBorder,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }

            // 3. Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dish Performance (${filteredEntries.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "Ranked by units sold",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                }
            }

            // 4. Item List
            if (filteredEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Fastfood, contentDescription = null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No item sales recorded", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkNavy))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Completed orders will populate dish performance statistics automatically.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                itemsIndexed(filteredEntries, key = { _, item -> item.itemName }) { index, entry ->
                    val rank = index + 1
                    val (rankBg, rankText) = when (rank) {
                        1 -> Color(0xFFFEF3C7) to Color(0xFFB45309) // Gold
                        2 -> Color(0xFFF1F5F9) to Color(0xFF475569) // Silver
                        3 -> Color(0xFFFFEDD5) to Color(0xFFC2410C) // Bronze
                        else -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
                    }

                    val sharePercent = if (totalUnitsSold > 0) (entry.quantity.toFloat() / totalUnitsSold.toFloat()) else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Rank Badge
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(rankBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#$rank",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = rankText,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = entry.itemName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = DarkNavy
                                            )
                                        )
                                        Text(
                                            text = "${entry.quantity} orders • ${(sharePercent * 100).toInt()}% of volume",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.5.sp,
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "₹${DecimalFormat("#,##0.00").format(entry.totalRevenue)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = RoyalBlue
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Share Progress Bar
                            LinearProgressIndicator(
                                progress = { sharePercent.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (rank == 1) RoyalBlue else Color(0xFF60A5FA),
                                trackColor = Color(0xFFEFF6FF)
                            )
                        }
                    }
                }
            }
        }
    }
}
