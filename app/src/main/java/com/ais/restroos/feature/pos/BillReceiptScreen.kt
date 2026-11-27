package com.ais.restroos.feature.pos

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.StatusGreen
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.LocalDatabaseHelper
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.printer.BluetoothPrinterService
import com.ais.restroos.data.printer.EscPosPrinter
import com.ais.restroos.data.repository.OrderRepository
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.PrinterState
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillReceiptScreen(
    orderId: String,
    viewModel: AddOrderViewModel? = null,
    onCloseClick: () -> Unit,
    onPrintAndRedirect: () -> Unit = onCloseClick,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val restroName by prefsManager.restaurantName.collectAsState(initial = "Siddharth Family Resturent")

    // Find the order from viewModel last completed or repository / local database
    val lastCompleted by viewModel?.lastCompletedOrder?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val order: ClosedOrder? = remember(orderId, lastCompleted) {
        if (lastCompleted != null && (lastCompleted?.invoiceNumber == orderId || lastCompleted?.orderId == orderId)) {
            lastCompleted
        } else {
            OrderRepository.getClosedOrderById(orderId)
                ?: OrderRepository.closedOrders.value.find { it.invoiceNumber == orderId || it.orderId == orderId }
        }
    }

    val currencyFormat = DecimalFormat("#,##0.00")
    val coroutineScope = rememberCoroutineScope()

    val autoPrintBillPref by prefsManager.autoPrintBill.collectAsState(initial = false)
    var autoPrintFired by remember { androidx.compose.runtime.mutableStateOf(false) }

    fun triggerPrint() {
        if (order == null) {
            Toast.makeText(context, "No order found to print", Toast.LENGTH_SHORT).show()
            onPrintAndRedirect()
            return
        }

        coroutineScope.launch {
            if (BluetoothPrinterService.printerState.value == PrinterState.ONLINE) {
                val bytes = EscPosPrinter.buildOrderBillReceipt(order, restroName)
                val result = BluetoothPrinterService.printData(bytes)
                if (result.isSuccess) {
                    Toast.makeText(context, "Bill ${order.invoiceNumber} printed successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Printer error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Bill ${order.invoiceNumber} receipt ready! (Bluetooth printer is offline)", Toast.LENGTH_SHORT).show()
            }
            // Redirect to on-hold orders as requested
            onPrintAndRedirect()
        }
    }

    androidx.compose.runtime.LaunchedEffect(order, autoPrintBillPref) {
        if (order != null && autoPrintBillPref && !autoPrintFired) {
            autoPrintFired = true
            triggerPrint()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // 1. Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DarkNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "Billing Summary",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = DarkNavy
                            )
                        )
                        Text(
                            text = "Bill receipt & payment breakdown",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Paid Tag Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BILLED & PAID",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. Receipt Card Container (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (order == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = "Order Details Not Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No bill details were found for ID: $orderId",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Restaurant Brand Header
                        Text(
                            text = restroName.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = DarkNavy,
                                letterSpacing = 0.5.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "TAX INVOICE / BILL RECEIPT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Bill Meta Data (Invoice #, Date, Service, Customer)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "INVOICE NUMBER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = order.invoiceNumber,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = RoyalBlue
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "DATE & TIME",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(order.completedTimestamp))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = DarkNavy
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "SERVICE / TABLE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                val tableInfo = if (!order.tableNumber.isNullOrBlank()) {
                                    "Dine-In • Table ${order.tableNumber}"
                                } else {
                                    order.orderType.replace("_", "-")
                                }
                                Text(
                                    text = tableInfo,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = DarkNavy
                                    )
                                )
                            }

                            if (!order.customerPhone.isNullOrBlank() || !order.customerName.isNullOrBlank()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "CUSTOMER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "${order.customerName ?: "Customer"} (${order.customerPhone ?: ""})",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = DarkNavy
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Itemized Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ITEM (QTY)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DarkNavy
                                ),
                                modifier = Modifier.weight(2.2f)
                            )
                            Text(
                                text = "RATE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DarkNavy,
                                    textAlign = TextAlign.End
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "AMOUNT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DarkNavy,
                                    textAlign = TextAlign.End
                                ),
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Item Rows
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2.2f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            color = DarkNavy
                                        )
                                    )
                                    Text(
                                        text = "Qty: ${item.quantity}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }

                                val unitPrice = item.price
                                val lineTotal = unitPrice * item.quantity
                                Text(
                                    text = "₹${currencyFormat.format(unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.End
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "₹${currencyFormat.format(lineTotal)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = DarkNavy,
                                        textAlign = TextAlign.End
                                    ),
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 0.5.dp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Financial Breakdown (kiska kitna laga & kitna discount hua)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Subtotal
                            val computedSubtotal = if (order.subtotal > 0.0) order.subtotal else order.items.sumOf { it.price * it.quantity }
                            val computedGrandTotal = if (order.totalAmount > 0.0) order.totalAmount else (computedSubtotal - order.discountAmount + order.serviceCharge + order.taxAmount)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Items Subtotal",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        color = DarkNavy
                                    )
                                )
                                Text(
                                    text = "₹${currencyFormat.format(computedSubtotal)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = DarkNavy
                                    )
                                )
                            }

                            // Discount
                            if (order.discountAmount > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Discount Applied",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = StatusGreen
                                        )
                                    )
                                    Text(
                                        text = "- ₹${currencyFormat.format(order.discountAmount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = StatusGreen
                                        )
                                    )
                                }
                            }

                            // Taxes / GST
                            if (order.taxAmount > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Taxes / GST",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    )
                                    Text(
                                        text = "+ ₹${currencyFormat.format(order.taxAmount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }
                            }

                            // Service Charge
                            if (order.serviceCharge > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Service Charge",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = DarkNavy
                                        )
                                    )
                                    Text(
                                        text = "+ ₹${currencyFormat.format(order.serviceCharge)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = DarkNavy
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.5.dp)

                            // Grand Total Paid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "GRAND TOTAL PAID",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = DarkNavy
                                        )
                                    )
                                    Text(
                                        text = "Paid via ${order.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = StatusGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                Text(
                                    text = "₹${currencyFormat.format(computedGrandTotal)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp,
                                        color = RoyalBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Footer Note
                        Text(
                            text = "Thank you for dining with us! Visit again.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 12.sp,
                                color = TextSecondary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Powered by AISRestroOS POS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = Color(0xFF9CA3AF)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Fixed Bottom Actions: [ Close ] and [ Print Bill ]
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = SurfaceWhite,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button 1: Close (Returns to POS / Home)
                OutlinedButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF6B7280))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF374151)
                        )
                    )
                }

                // Button 2: Print Bill (Prints Thermal Receipt)
                Button(
                    onClick = { triggerPrint() },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print Bill",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Print Bill",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
