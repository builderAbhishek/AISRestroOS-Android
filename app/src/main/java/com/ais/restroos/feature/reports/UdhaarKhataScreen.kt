package com.ais.restroos.feature.reports

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.RoyalBlueLight
import com.ais.restroos.core.theme.StatusGreen
import com.ais.restroos.core.theme.StatusRed
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.repository.CustomerRepository
import com.ais.restroos.domain.model.Customer
import com.ais.restroos.domain.model.KhataEntry
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.DecimalFormat

@Composable
fun UdhaarKhataScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val restroName by prefs.restaurantName.collectAsState(initial = "My Restaurant")
    val customers by CustomerRepository.customers.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyDue by remember { mutableStateOf(true) }
    var selectedCustomerForKhata by remember { mutableStateOf<Customer?>(null) }
    var showAddUdhaarDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CustomerRepository.fetchCustomersFromBackend()
    }

    val totalPendingUdhaar = customers.sumOf { it.udhaarBalance }
    val filteredCustomers = customers.filter { cust ->
        val query = searchQuery.trim().lowercase()
        val matchesQuery = query.isEmpty() || cust.name.lowercase().contains(query) || cust.mobileNumber.contains(query)
        val matchesFilter = !filterOnlyDue || cust.udhaarBalance > 0.0
        matchesQuery && matchesFilter
    }

    if (selectedCustomerForKhata != null) {
        val targetCust = selectedCustomerForKhata!!
        KhataSettleDialog(
            customer = targetCust,
            onDismiss = { selectedCustomerForKhata = null },
            onSaveEntry = { type, amount, note ->
                scope.launch {
                    val res = CustomerRepository.recordKhata(
                        name = targetCust.name,
                        mobile = targetCust.mobileNumber,
                        entryType = type,
                        amount = amount,
                        note = note
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, "Khata updated!", Toast.LENGTH_SHORT).show()
                        selectedCustomerForKhata = null
                    } else {
                        Toast.makeText(context, "Failed to update Khata", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showAddUdhaarDialog) {
        AddUdhaarEntryDialog(
            onDismiss = { showAddUdhaarDialog = false },
            onSave = { name, mobile, amount, note ->
                scope.launch {
                    CustomerRepository.saveOrUpdateCustomer(name, mobile, true)
                    val res = CustomerRepository.recordKhata(
                        name = name,
                        mobile = mobile,
                        entryType = "UDHAAR",
                        amount = amount,
                        note = note.ifBlank { "Manual Udhaar Entry" }
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, "Udhaar added for $name!", Toast.LENGTH_SHORT).show()
                        showAddUdhaarDialog = false
                    } else {
                        Toast.makeText(context, "Failed to save Udhaar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundLight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddUdhaarDialog = true },
                containerColor = RoyalBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Udhaar")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            ) {
                Column(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                text = "Udhaar Khata",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Customer dues & WhatsApp reminders",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFFFB923C),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Udhaar Pending",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "₹${DecimalFormat("#,##0.00").format(totalPendingUdhaar)}",
                                color = if (totalPendingUdhaar > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search customer by name or mobile",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9CA3AF), fontSize = 14.sp)
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = LightBorder,
                    focusedBorderColor = RoyalBlue,
                    unfocusedContainerColor = SurfaceWhite,
                    focusedContainerColor = SurfaceWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dueCount = customers.count { it.udhaarBalance > 0 }
                FilterChip(
                    selected = filterOnlyDue,
                    onClick = { filterOnlyDue = true },
                    label = { Text("Udhaar Due ($dueCount)") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEE2E2))
                )
                FilterChip(
                    selected = !filterOnlyDue,
                    onClick = { filterOnlyDue = false },
                    label = { Text("All Customers (${customers.size})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlueLight)
                )
            }

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (filterOnlyDue) "No pending Udhaar! Tap + to add Udhaar." else "No customers found.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCustomerForKhata = cust },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(RoyalBlueLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = RoyalBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = cust.name,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = DarkNavy
                                                )
                                            )
                                            Text(
                                                text = "+91 ${cust.mobileNumber}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            )
                                        }
                                    }

                                    val hasDue = cust.udhaarBalance > 0.0
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (hasDue) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = if (hasDue) "₹${DecimalFormat("#,##0").format(cust.udhaarBalance)} Due" else "Settled",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasDue) StatusRed else StatusGreen
                                            ),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedCustomerForKhata = cust },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Khata / Settle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    if (cust.udhaarBalance > 0.0) {
                                        Button(
                                            onClick = {
                                                val cleanMob = cust.mobileNumber.filter { it.isDigit() }.takeLast(10)
                                                val amtStr = DecimalFormat("#,##0").format(cust.udhaarBalance)
                                                val msg = "Namaste ${cust.name} ji, *$restroName* se payment reminder: aapka ₹$amtStr ka Udhaar (Khata) pending hai. Kripya jaldi clear karne ki kripa karein. Dhanyavaad!"
                                                val waUrl = "https://wa.me/91$cleanMob?text=${URLEncoder.encode(msg, "UTF-8")}"
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remind", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
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
private fun KhataSettleDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSaveEntry: (entryType: String, amount: Double, note: String) -> Unit
) {
    var entryType by remember { mutableStateOf(if (customer.udhaarBalance > 0) "PAYMENT" else "UDHAAR") }
    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var ledger by remember { mutableStateOf<List<KhataEntry>>(emptyList()) }

    LaunchedEffect(customer.mobileNumber) {
        val res = CustomerRepository.fetchKhataLedger(customer.mobileNumber)
        if (res.isSuccess) {
            ledger = res.getOrNull().orEmpty()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(customer.name, fontWeight = FontWeight.Bold, color = DarkNavy, fontSize = 17.sp)
                Text(
                    "Pending Udhaar: ₹${DecimalFormat("#,##0.00").format(customer.udhaarBalance)}",
                    color = if (customer.udhaarBalance > 0) StatusRed else StatusGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = entryType == "PAYMENT",
                        onClick = { entryType = "PAYMENT" },
                        label = { Text("Jama (Received)") }
                    )
                    FilterChip(
                        selected = entryType == "UDHAAR",
                        onClick = { entryType = "UDHAAR" },
                        label = { Text("+ Add Udhaar") }
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (ledger.isNotEmpty()) {
                    Text("Recent Entries", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ledger.take(4).forEach { e ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = e.note ?: e.entryType,
                                    fontSize = 11.5.sp,
                                    color = DarkNavy,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = (if (e.entryType == "UDHAAR") "+₹" else "-₹") + DecimalFormat("#,##0").format(e.amount),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (e.entryType == "UDHAAR") StatusRed else StatusGreen
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSaveEntry(entryType, amt, noteStr.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Save Entry", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun AddUdhaarEntryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, mobile: String, amount: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Add Udhaar Entry", fontWeight = FontWeight.Bold, color = DarkNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter { c -> c.isDigit() }.take(10) },
                    label = { Text("Mobile Number (10 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Udhaar Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && mobile.length == 10 && amt > 0.0) {
                        onSave(name.trim(), mobile, amt, note.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Save Udhaar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
