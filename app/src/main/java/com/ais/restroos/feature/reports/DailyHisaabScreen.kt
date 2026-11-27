package com.ais.restroos.feature.reports

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.HisaabSuggestionsManager
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.remote.dtos.CreatePurchaseRequest
import com.ais.restroos.data.remote.dtos.PurchaseItemDto
import com.ais.restroos.data.repository.PurchaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHisaabScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager(context) }
    val currentRestroId by prefsManager.restaurantId.collectAsState(initial = "RESTRO-001")

    // Formatters
    val currencyFormat = remember { DecimalFormat("#,##0.00") }
    val dbDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    val todayStr = remember { dbDateFormat.format(Date()) }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dbDateFormat.format(cal.time)
    }

    var selectedDate by remember { mutableStateOf(todayStr) }
    var showAddSheet by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PurchaseItemDto?>(null) }

    val purchases by PurchaseRepository.purchases.collectAsState()
    val summary by PurchaseRepository.summary.collectAsState()
    val isLoading by PurchaseRepository.isLoading.collectAsState()

    // Trigger load once on screen entry & date change (no background loop to prevent server spam)
    LaunchedEffect(selectedDate) {
        val restroId = prefsManager.getRestaurantId()
        PurchaseRepository.loadHisaab(context, restroId, selectedDate)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // 1. Modern Dark Navy Header Bar (matches Home, Reports & More)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Daily Hisaab",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Sales, Expenses & Net Cash",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                IconButton(onClick = {
                    coroutineScope.launch {
                        val restroId = prefsManager.getRestaurantId()
                        PurchaseRepository.loadHisaab(context, restroId, selectedDate)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 2. Date Quick Select Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Today Chip
                    DateSelectionChip(
                        label = "Today",
                        isSelected = (selectedDate == todayStr),
                        onClick = { selectedDate = todayStr }
                    )

                    // Yesterday Chip
                    DateSelectionChip(
                        label = "Yesterday",
                        isSelected = (selectedDate == yesterdayStr),
                        onClick = { selectedDate = yesterdayStr }
                    )

                    // Calendar Custom Date Picker
                    val isCustom = (selectedDate != todayStr && selectedDate != yesterdayStr)
                    Surface(
                        modifier = Modifier
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val c = Calendar.getInstance()
                                        c.set(year, month, dayOfMonth)
                                        selectedDate = dbDateFormat.format(c.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isCustom) RoyalBlue else SurfaceWhite,
                        border = BorderStroke(1.dp, if (isCustom) RoyalBlue else LightBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Date",
                                tint = if (isCustom) Color.White else DarkNavy,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isCustom) {
                                    try {
                                        displayDateFormat.format(dbDateFormat.parse(selectedDate)!!)
                                    } catch (_: Exception) { selectedDate }
                                } else "Pick Date",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCustom) Color.White else DarkNavy,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // 3. P&L Main Profit Banner Card: "TODAY'S REMAIN (BACHA HUA AMOUNT)"
            item {
                val isProfit = summary.net_profit >= 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfit) Color(0xFF0F172A) else Color(0xFF7F1D1D)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isProfit) "TODAY'S REMAIN (BACHA HUA AMOUNT)" else "TODAY'S SHORTAGE / DEFICIT",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isProfit) Color(0xFF38BDF8) else Color(0xFFFCA5A5),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isProfit) "₹${currencyFormat.format(summary.net_profit)}" else "-₹${currencyFormat.format(kotlin.math.abs(summary.net_profit))}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isProfit) Color(0xFF166534) else Color(0xFF991B1B)
                            ) {
                                Text(
                                    text = if (isProfit) "Bacha Hua" else "Kam Pade",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Clear Formula Footnote: Sales - Expense = Today's Remain
                        Text(
                            text = "Formula: Sales (₹${currencyFormat.format(summary.total_sales)}) − Total Expense (₹${currencyFormat.format(summary.total_outflow)})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // 4. Metric Breakdown Cards Row: Today's Sales & Total Expense (Merged)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Sales Card
                    MetricSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Today's Sales",
                        subtitle = "${summary.order_count} Orders Delivered",
                        amount = "₹${currencyFormat.format(summary.total_sales)}",
                        accentColor = Color(0xFF16A34A),
                        bgColor = Color(0xFFF0FDF4)
                    )

                    // Total Expense Card (Sabji, Kirana, Gas, Petrol, Staff, etc. merged)
                    MetricSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Expense",
                        subtitle = "${purchases.size} Entries (Sabji, Kirana, Gas...)",
                        amount = "₹${currencyFormat.format(summary.total_outflow)}",
                        accentColor = Color(0xFFEA580C),
                        bgColor = Color(0xFFFFF7ED)
                    )
                }
            }

            // 5. Section Header & Add Expense Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Expenses & Bills (${purchases.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DarkNavy
                            )
                        )
                        Text(
                            text = "Sabji, Kirana, Dudh, Gas, Petrol, Staff...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        )
                    }

                    Button(
                        onClick = { showAddSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Expense", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 6. Purchases & Expenses Item List
            if (isLoading && purchases.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoyalBlue, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (purchases.isEmpty()) {
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
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No expense or inward records for this date.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkNavy
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ Add Expense' above to add Sabji, Kirana, Gas, Petrol or bills.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            } else {
                items(
                    items = purchases,
                    key = { it.id }
                ) { purchase ->
                    PurchaseItemCard(
                        item = purchase,
                        currencyFormat = currencyFormat,
                        onDeleteClick = { itemToDelete = purchase }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(text = "Delete Expense Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${item.item_name}' (₹${currencyFormat.format(item.amount)}) from hisaab?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val restroId = prefsManager.getRestaurantId()
                            PurchaseRepository.deletePurchase(context, restroId, item.id, selectedDate)
                            itemToDelete = null
                            Toast.makeText(context, "Record deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Purchase / Expense Modal Sheet
    if (showAddSheet) {
        AddPurchaseModalSheet(
            currentDate = selectedDate,
            restaurantId = currentRestroId,
            onDismiss = { showAddSheet = false },
            onSave = { request ->
                coroutineScope.launch {
                    PurchaseRepository.addPurchase(context, currentRestroId, request)
                    showAddSheet = false
                    Toast.makeText(context, "Expense record saved successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------
// HELPER COMPONENTS
// ----------------------------------------------------------------------------

@Composable
private fun DateSelectionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) RoyalBlue else SurfaceWhite,
        border = BorderStroke(1.dp, if (isSelected) RoyalBlue else LightBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else DarkNavy,
                fontSize = 12.sp
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun MetricSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    amount: String,
    accentColor: Color,
    bgColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkNavy,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 10.5.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF475569),
                fontSize = 11.5.sp
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PurchaseItemCard(
    item: PurchaseItemDto,
    currencyFormat: DecimalFormat,
    onDeleteClick: () -> Unit
) {
    val lower = (item.item_name + " " + item.category).lowercase()
    val (avatarIcon, avatarTint, avatarBg) = when {
        lower.contains("gas") || lower.contains("cylinder") || lower.contains("lpg") ->
            Triple(Icons.Default.LocalGasStation, Color(0xFFD97706), Color(0xFFFEF3C7))
        lower.contains("petrol") || lower.contains("diesel") || lower.contains("fuel") ->
            Triple(Icons.Default.LocalGasStation, Color(0xFFEA580C), Color(0xFFFFF7ED))
        lower.contains("dudh") || lower.contains("milk") || lower.contains("dairy") || lower.contains("paneer") ->
            Triple(Icons.Default.Storefront, Color(0xFF2563EB), Color(0xFFEFF6FF))
        lower.contains("sabji") || lower.contains("veg") || lower.contains("mandi") || lower.contains("tamatar") || lower.contains("pyaz") || lower.contains("aloo") ->
            Triple(Icons.Default.ShoppingBag, Color(0xFF16A34A), Color(0xFFF0FDF4))
        lower.contains("water") || lower.contains("paani") || lower.contains("jar") ->
            Triple(Icons.Default.ShoppingBag, Color(0xFF0284C7), Color(0xFFE0F2FE))
        lower.contains("kirana") || lower.contains("grocery") || lower.contains("oil") || lower.contains("flour") || lower.contains("rice") ->
            Triple(Icons.Default.Restaurant, Color(0xFF9333EA), Color(0xFFFAF5FF))
        lower.contains("staff") || lower.contains("salary") || lower.contains("wage") ->
            Triple(Icons.Default.Payment, Color(0xFF4F46E5), Color(0xFFEEF2FF))
        else ->
            Triple(Icons.Default.Payment, Color(0xFF64748B), Color(0xFFF1F5F9))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Avatar Icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = avatarBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = null,
                            tint = avatarTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.item_name.replace("\n", " ").trim(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val cleanVendor = item.vendor_name.replace("\n", " ").trim()
                        Text(
                            text = if (cleanVendor.isNotBlank()) "Paid to: $cleanVendor" else item.category.trim(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.quantity > 0 && item.unit.isNotBlank()) {
                            Text(
                                text = "• ${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit.trim()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkNavy,
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Right: Amount, Mode Chip & Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${currencyFormat.format(item.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = DarkNavy
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (item.payment_mode.uppercase()) {
                            "UDHAR" -> Color(0xFFFEE2E2)
                            "ONLINE", "UPI" -> Color(0xFFE0E7FF)
                            else -> Color(0xFFDCFCE7)
                        }
                    ) {
                        Text(
                            text = when (item.payment_mode.uppercase()) {
                                "UDHAR" -> "CREDIT / DUE"
                                "ONLINE", "UPI" -> "ONLINE (UPI)"
                                else -> "CASH"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = when (item.payment_mode.uppercase()) {
                                    "UDHAR" -> Color(0xFFB91C1C)
                                    "ONLINE", "UPI" -> Color(0xFF3730A3)
                                    else -> Color(0xFF15803D)
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// AUTOCOMPLETE TEXT FIELD COMPONENT (100% LOCAL & OFFLINE)
// ----------------------------------------------------------------------------

@Composable
private fun AutocompleteOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    onSuggestionSelected: ((String) -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var userDismissed by remember { mutableStateOf(false) }

    val query = value.trim()
    val matchingSuggestions = remember(query, suggestions, isFocused, userDismissed) {
        if (!isFocused || userDismissed) {
            emptyList()
        } else if (query.isEmpty()) {
            suggestions.take(4) // Show top suggestions on focus
        } else {
            // When user types 1, 2, or 3+ letters, match contains (case-insensitive)
            suggestions.filter { it.contains(query, ignoreCase = true) }.take(5)
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                userDismissed = false
                onValueChange(it)
            },
            label = { Text(label, fontSize = 13.sp) },
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, fontSize = 12.sp, color = TextSecondary) }
            } else null,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            leadingIcon = leadingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        userDismissed = false
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RoyalBlue,
                unfocusedBorderColor = LightBorder
            )
        )

        // Dropdown suggestions list when focused and matching items available
        if (matchingSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    matchingSuggestions.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(item)
                                    userDismissed = true
                                    onSuggestionSelected?.invoke(item)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (item.equals(value.trim(), ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                    color = DarkNavy
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (index < matchingSuggestions.size - 1) {
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// ADD DAILY EXPENSE / BILL MODAL SHEET (UNIFIED EXPENSES & PURCHASES)
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPurchaseModalSheet(
    currentDate: String,
    restaurantId: String,
    onDismiss: () -> Unit,
    onSave: (CreatePurchaseRequest) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vegetables") }
    var quantityText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("bill") }
    var vendorName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    // Retrieve offline suggestions for autocomplete (no network calls, instant 1-2 char matching)
    val itemSuggestions = remember(restaurantId) {
        HisaabSuggestionsManager.getSuggestions(context, restaurantId, "ALL_EXPENSES")
    }
    val unitSuggestions = remember(restaurantId) {
        HisaabSuggestionsManager.getSuggestions(context, restaurantId, "UNIT")
    }
    val vendorSuggestions = remember(restaurantId) {
        HisaabSuggestionsManager.getSuggestions(context, restaurantId, "VENDOR")
    }

    // Quick Pick Chips for Indian Restaurants (Sabji, Kirana, Gas, Petrol, Milk, Water, Staff, etc.)
    val quickPicks = remember {
        listOf(
            "Sabji Mandi (Daily Vegetables)",
            "Kirana Store (Groceries Bill)",
            "Dudh / Dairy (Milk, Paneer)",
            "Commercial Gas Cylinder (LPG)",
            "Petrol / Vehicle Fuel",
            "Paani (Drinking Water Jar)",
            "Staff Daily Wages / Kharcha",
            "Chicken / Meat Supplier",
            "Cooking Oil & Ghee",
            "Food Packaging & Containers",
            "Shop Rent / Bijli Bill",
            "Cleaning & Supplies"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Add Daily Expense / Bill",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkNavy
                    )
                )
                Text(
                    text = "Record Sabji Mandi, Kirana, Dudh, Gas, Petrol, Paani, Staff, etc.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                )
            }

            // Quick Pick Suggestion Chips
            Column {
                Text(
                    text = "Quick Select:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickPicks) { pick ->
                        val isSelected = itemName.equals(pick, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable {
                                itemName = pick
                                category = guessCategory(pick)
                                val guessedUnit = guessUnitForItem(pick)
                                if (guessedUnit != null) unit = guessedUnit
                                val guessedVendor = guessVendorForItem(pick)
                                if (guessedVendor != null && vendorName.isBlank()) vendorName = guessedVendor
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) RoyalBlue else BackgroundLight,
                            border = BorderStroke(1.dp, if (isSelected) RoyalBlue else LightBorder)
                        ) {
                            Text(
                                text = pick,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else DarkNavy
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Particulars / Expense Name with Autocomplete
            AutocompleteOutlinedField(
                value = itemName,
                onValueChange = { itemName = it },
                label = "Expense / Particulars (e.g. Sabji, Kirana, Gas, Petrol) *",
                placeholder = "Type 1-2 letters for instant suggestions...",
                suggestions = itemSuggestions,
                onSuggestionSelected = { selected ->
                    category = guessCategory(selected)
                    val guessedUnit = guessUnitForItem(selected)
                    if (guessedUnit != null) unit = guessedUnit
                    val guessedVendor = guessVendorForItem(selected)
                    if (guessedVendor != null && vendorName.isBlank()) vendorName = guessedVendor
                }
            )

            // Amount Field (Primary Numeric Input)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Total Amount (₹) *", fontSize = 13.sp) },
                placeholder = { Text("e.g. 850", fontSize = 12.sp, color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalBlue,
                    unfocusedBorderColor = LightBorder
                )
            )

            // Vendor / Shop / Supplier with Autocomplete
            AutocompleteOutlinedField(
                value = vendorName,
                onValueChange = { vendorName = it },
                label = "Vendor / Shop / Paid To (e.g. Mandi, Kirana, Agency, Staff)",
                placeholder = "Type shop or vendor name...",
                suggestions = vendorSuggestions
            )

            // Optional Quantity & Unit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity (Opt)", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = LightBorder
                    )
                )

                AutocompleteOutlinedField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = "Unit",
                    placeholder = "kg, cylinder, jar, bill",
                    suggestions = unitSuggestions,
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Payment Method Selection
            Column {
                Text(
                    text = "Payment Method:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CASH" to "Cash", "UPI" to "Online (UPI)", "UDHAR" to "Credit / Due").forEach { (mode, label) ->
                        val isSel = (paymentMode == mode)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMode = mode },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) DarkNavy else BackgroundLight,
                            border = BorderStroke(1.dp, if (isSel) DarkNavy else LightBorder)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else DarkNavy
                                )
                            }
                        }
                    }
                }
            }

            // Optional Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Remarks (Optional)", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalBlue,
                    unfocusedBorderColor = LightBorder
                )
            )

            // Save Button
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (itemName.isBlank() || amt == null || amt <= 0.0) {
                        Toast.makeText(context, "Please enter particulars and a valid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 1. Save entry to offline suggestions immediately for fast matching
                    HisaabSuggestionsManager.recordEntry(
                        context = context,
                        restaurantId = restaurantId,
                        expenseType = "EXPENSE",
                        itemName = itemName,
                        unit = unit,
                        vendorName = vendorName
                    )

                    // 2. Trigger repository save (saves locally & syncs to backend)
                    onSave(
                        CreatePurchaseRequest(
                            item_name = itemName.trim(),
                            category = category.ifEmpty { "General Expense" },
                            quantity = quantityText.toDoubleOrNull() ?: 1.0,
                            unit = unit.trim().ifEmpty { "bill" },
                            vendor_name = vendorName.trim().ifEmpty { "Local Market / Store" },
                            amount = amt,
                            payment_mode = paymentMode,
                            expense_type = "EXPENSE",
                            purchase_date = currentDate,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Save Expense",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// SMART CATEGORY, UNIT & VENDOR GUESSERS
// ----------------------------------------------------------------------------

private fun guessCategory(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.contains("gas") || lower.contains("lpg") || lower.contains("cylinder") -> "Gas & Fuel"
        lower.contains("petrol") || lower.contains("diesel") || lower.contains("transport") || lower.contains("auto") -> "Transport & Fuel"
        lower.contains("water") || lower.contains("paani") || lower.contains("jar") || lower.contains("ro") -> "Drinking Water"
        lower.contains("sabji") || lower.contains("veg") || lower.contains("mandi") ||
        lower.contains("potato") || lower.contains("aloo") || lower.contains("onion") || lower.contains("pyaz") ||
        lower.contains("tomato") || lower.contains("tamatar") || lower.contains("chilli") -> "Vegetables (Mandi)"

        lower.contains("dudh") || lower.contains("milk") || lower.contains("dairy") ||
        lower.contains("paneer") || lower.contains("butter") || lower.contains("curd") || lower.contains("dahi") -> "Dairy"

        lower.contains("kirana") || lower.contains("grocery") || lower.contains("oil") ||
        lower.contains("atta") || lower.contains("flour") || lower.contains("rice") ||
        lower.contains("masala") || lower.contains("salt") || lower.contains("sugar") -> "Kirana & Grocery"

        lower.contains("staff") || lower.contains("salary") || lower.contains("wage") || lower.contains("karigar") -> "Staff & Wages"
        lower.contains("chicken") || lower.contains("meat") || lower.contains("mutton") || lower.contains("egg") -> "Meat & Poultry"
        lower.contains("rent") -> "Rent"
        lower.contains("electric") || lower.contains("power") || lower.contains("bijli") -> "Electricity"
        lower.contains("clean") || lower.contains("detergent") || lower.contains("soap") -> "Cleaning & Supplies"
        lower.contains("box") || lower.contains("pack") || lower.contains("foil") || lower.contains("container") -> "Packaging & Supplies"
        else -> "General Expense"
    }
}

private fun guessUnitForItem(name: String): String? {
    val lower = name.lowercase()
    return when {
        lower.contains("cylinder") || lower.contains("lpg") || lower.contains("gas") -> "cylinder"
        lower.contains("water") || lower.contains("paani") || lower.contains("jar") -> "jar"
        lower.contains("milk") || lower.contains("doodh") || lower.contains("oil") || lower.contains("diesel") || lower.contains("petrol") -> "liter"
        lower.contains("sabji") || lower.contains("kirana") -> "bill"
        lower.contains("staff") || lower.contains("wage") -> "day"
        lower.contains("rent") -> "month"
        lower.contains("potato") || lower.contains("aloo") || lower.contains("onion") || lower.contains("pyaz") ||
        lower.contains("tomato") || lower.contains("tamatar") || lower.contains("paneer") || lower.contains("chicken") ||
        lower.contains("mutton") || lower.contains("rice") || lower.contains("flour") || lower.contains("atta") -> "kg"
        lower.contains("egg") -> "tray"
        else -> "item"
    }
}

private fun guessVendorForItem(name: String): String? {
    val lower = name.lowercase()
    return when {
        lower.contains("gas") || lower.contains("cylinder") || lower.contains("lpg") -> "Commercial Gas Agency"
        lower.contains("sabji") || lower.contains("mandi") || lower.contains("veg") -> "Local Mandi (Vegetable Market)"
        lower.contains("kirana") || lower.contains("grocery") -> "Kirana / Wholesale Grocery Store"
        lower.contains("dudh") || lower.contains("milk") || lower.contains("dairy") || lower.contains("paneer") -> "Dairy Farm / Milk Supplier"
        lower.contains("petrol") || lower.contains("fuel") -> "Petrol Pump / Fuel"
        lower.contains("water") || lower.contains("paani") || lower.contains("jar") -> "Water Jar Supplier (Paani)"
        lower.contains("chicken") || lower.contains("meat") -> "Meat / Poultry Vendor"
        lower.contains("staff") || lower.contains("wage") -> "Staff / Karigar"
        lower.contains("rent") -> "Landlord / Rent"
        else -> null
    }
}
