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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.LocalDatabaseHelper
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.PurchaseItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ExpensePeriodFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL("All Time")
}

@Composable
fun ExpenseReportScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager(context) }
    val currencyFormat = remember { DecimalFormat("#,##0.00") }
    val dbDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    var allExpenses by remember { mutableStateOf<List<PurchaseItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(ExpensePeriodFilter.TODAY) }
    var searchQuery by remember { mutableStateOf("") }

    fun loadExpenses() {
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val restroId = prefsManager.getRestaurantId()
                    val resp = RetrofitClient.apiService.getPurchases()
                    if (resp.isSuccessful && resp.body()?.status == "success") {
                        allExpenses = resp.body()?.data ?: emptyList()
                    } else {
                        allExpenses = LocalDatabaseHelper.getInstance(context).getLocalPurchases(restroId)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.IO) {
                    val restroId = prefsManager.getRestaurantId()
                    allExpenses = LocalDatabaseHelper.getInstance(context).getLocalPurchases(restroId)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadExpenses()
    }

    val todayStr = remember { dbDateFormat.format(Date()) }
    val yesterdayStr = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.let { dbDateFormat.format(it.time) }
    }
    val weekStartStr = remember {
        Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }.let { dbDateFormat.format(it.time) }
    }
    val monthStartStr = remember {
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.let { dbDateFormat.format(it.time) }
    }

    val filteredExpenses = remember(allExpenses, selectedPeriod, searchQuery) {
        val q = searchQuery.trim().lowercase()
        allExpenses.filter { item ->
            val matchesPeriod = when (selectedPeriod) {
                ExpensePeriodFilter.TODAY -> item.purchase_date == todayStr
                ExpensePeriodFilter.YESTERDAY -> item.purchase_date == yesterdayStr
                ExpensePeriodFilter.THIS_WEEK -> item.purchase_date >= weekStartStr
                ExpensePeriodFilter.THIS_MONTH -> item.purchase_date >= monthStartStr
                ExpensePeriodFilter.ALL -> true
            }
            val matchesQuery = q.isEmpty() ||
                    item.item_name.lowercase().contains(q) ||
                    item.vendor_name.lowercase().contains(q) ||
                    item.category.lowercase().contains(q)
            matchesPeriod && matchesQuery
        }
    }

    val totalAmount = filteredExpenses.sumOf { it.amount }
    val cashTotal = filteredExpenses.filter { it.payment_mode.equals("CASH", true) }.sumOf { it.amount }
    val upiTotal = filteredExpenses.filter {
        it.payment_mode.equals("UPI", true) || it.payment_mode.equals("ONLINE", true)
    }.sumOf { it.amount }
    val creditTotal = filteredExpenses.filter { it.payment_mode.equals("UDHAR", true) }.sumOf { it.amount }

    val categoryTotals = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.category.ifBlank { "General Expense" } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Dark Navy Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Expense Report",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Sabji, Kirana, Gas, Petrol & Bills Analytics",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFFFB923C),
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp)
                    )
                } else {
                    IconButton(onClick = { loadExpenses() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFFB923C),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpensePeriodFilter.entries) { period ->
                        val isSel = selectedPeriod == period
                        Surface(
                            modifier = Modifier.clickable { selectedPeriod = period },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSel) RoyalBlue else SurfaceWhite,
                            border = BorderStroke(1.dp, if (isSel) RoyalBlue else LightBorder)
                        ) {
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else DarkNavy,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Total Expense Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL EXPENSE (${selectedPeriod.label.uppercase()})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFB923C),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${currencyFormat.format(totalAmount)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "${filteredExpenses.size} Bills",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Payment Mode Breakdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseModeCard("Cash", "₹${currencyFormat.format(cashTotal)}", Color(0xFF16A34A), Modifier.weight(1f))
                    ExpenseModeCard("Online (UPI)", "₹${currencyFormat.format(upiTotal)}", Color(0xFF2563EB), Modifier.weight(1f))
                    ExpenseModeCard("Credit/Udhar", "₹${currencyFormat.format(creditTotal)}", Color(0xFFDC2626), Modifier.weight(1f))
                }
            }

            // Top Categories Breakdown
            if (categoryTotals.isNotEmpty()) {
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
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    fontSize = 14.5.sp
                                )
                            )
                            val maxCat = categoryTotals.maxOf { it.value }.coerceAtLeast(1.0)
                            categoryTotals.forEach { (cat, amt) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DarkNavy,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Text(
                                            text = "₹${currencyFormat.format(amt)}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DarkNavy,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
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
                                                .fillMaxWidth((amt / maxCat).toFloat().coerceIn(0.05f, 1f))
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(RoyalBlue)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search item, vendor or category...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Expense Entries List
            if (filteredExpenses.isEmpty() && !isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No expenses found for ${selectedPeriod.label.lowercase()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = DarkNavy,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.item_name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.purchase_date} • ${item.vendor_name.ifBlank { item.category }}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${currencyFormat.format(item.amount)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkNavy,
                                        fontSize = 14.5.sp
                                    )
                                )
                                Text(
                                    text = item.payment_mode.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
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

@Composable
private fun ExpenseModeCard(
    label: String,
    amount: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
