package com.ais.restroos.feature.orders.closed

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.ais.restroos.domain.model.ClosedOrder
import java.text.DecimalFormat

@Composable
fun ClosedOrdersScreen(
    viewModel: ClosedOrdersViewModel,
    onBackClick: () -> Unit,
    onEditOrder: (orderId: String) -> Unit,
    onAddNewOrder: () -> Unit,
    onViewBillReceipt: ((orderId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.userToastMessage) {
        uiState.userToastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Controlled Edit Audit Dialog
    uiState.orderToEdit?.let { order ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditDialog() },
            title = {
                Text(
                    text = "Edit Closed Order ${order.invoiceNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                )
            },
            text = {
                Text(
                    text = "Editing a closed order will log an audit trail modification record before reloading it in the POS editor. Do you wish to proceed?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = order.orderId
                        viewModel.dismissEditDialog()
                        onEditOrder(id)
                    }
                ) {
                    Text(
                        text = "Proceed to POS",
                        color = RoyalBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissEditDialog() }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Print Receipt Modal Dialog
    uiState.orderToPrint?.let { order ->
        BillPrintPreviewDialog(
            order = order,
            onDismiss = { viewModel.dismissPrintDialog() },
            onPrintConfirm = {
                Toast.makeText(context, "Bill sent to thermal printer for ${order.invoiceNumber}", Toast.LENGTH_LONG).show()
                viewModel.dismissPrintDialog()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // A. Top App Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BackgroundLight,
                shadowElevation = 0.dp
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
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = DarkNavy,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Closed Order",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = DarkNavy
                            )
                        )
                    }

                    IconButton(onClick = { viewModel.refreshOrders() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = RoyalBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // B. Time Filter Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterTabButton(
                    text = "ALL TIME",
                    isSelected = uiState.activeFilter == ClosedOrdersFilter.ALL_TIME,
                    onClick = { viewModel.setFilter(ClosedOrdersFilter.ALL_TIME) }
                )

                FilterTabButton(
                    text = "LAST 60 MINS",
                    isSelected = uiState.activeFilter == ClosedOrdersFilter.LAST_60_MINS,
                    onClick = { viewModel.setFilter(ClosedOrdersFilter.LAST_60_MINS) }
                )
            }

            // C. Closed Orders List / Empty State
            if (uiState.orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEBF1FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (uiState.activeFilter == ClosedOrdersFilter.LAST_60_MINS)
                                "No closed orders in last 60 mins"
                            else "No closed orders found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = DarkNavy
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Completed restaurant bills and paid POS orders will appear here.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = TextSecondary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.orders,
                        key = { it.orderId }
                    ) { order ->
                        ClosedOrderCard(
                            order = order,
                            onEditClick = { viewModel.requestEditClosedOrder(order) },
                            onPrintClick = { viewModel.requestPrintBill(order) },
                            onCardClick = {
                                if (onViewBillReceipt != null) {
                                    onViewBillReceipt(order.invoiceNumber)
                                } else {
                                    viewModel.requestEditClosedOrder(order)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Fixed Bottom Action Button ("Add Order")
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onAddNewOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Add Order",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) RoyalBlue else LightBorder
    val textColor = if (isSelected) RoyalBlue else Color(0xFF374151)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BillPrintPreviewDialog(
    order: ClosedOrder,
    onDismiss: () -> Unit,
    onPrintConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "चौधरी कैफे एंड फैमिली रेस्टोरेंट",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy,
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "INVOICE / TAX RECEIPT • ${order.invoiceNumber}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = LightBorder)

                if (!order.tableNumber.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Table No:", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = TextSecondary))
                        Text(text = order.tableNumber, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkNavy))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Payment Method:", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = TextSecondary))
                    Text(text = order.paymentMethod, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen))
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Items Ordered:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = DarkNavy))

                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.displayText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = DarkNavy),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "₹${java.text.DecimalFormat("#,##0.00").format(item.price * item.quantity)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkNavy)
                        )
                    }
                }

                HorizontalDivider(color = LightBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Paid:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkNavy))
                    Text(text = "₹${DecimalFormat("#,##0.00").format(order.totalAmount)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RoyalBlue))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPrintConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Print Bill", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(16.dp)
    )
}