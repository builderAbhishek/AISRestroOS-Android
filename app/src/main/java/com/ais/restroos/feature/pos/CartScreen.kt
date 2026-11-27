package com.ais.restroos.feature.pos

import android.widget.Toast
import com.ais.restroos.core.components.TableSelectionDialog
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.ais.restroos.domain.model.OrderType
import com.ais.restroos.domain.model.PaymentMethod
import com.ais.restroos.domain.model.RestaurantTable
import java.text.DecimalFormat

@Composable
fun CartScreen(
    viewModel: AddOrderViewModel,
    onBackClick: () -> Unit,
    onOrderCompleted: (invoiceNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showTableSelectionDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Add Customer Details Bottom Sheet
    if (uiState.showCustomerSheet) {
        CustomerDetailsBottomSheet(
            customerPhone = uiState.customerPhone,
            customerName = uiState.customerName,
            saveAsRegular = uiState.saveAsRegularCustomer,
            onPhoneChange = { viewModel.onCustomerPhoneChange(it) },
            onNameChange = { viewModel.onCustomerNameChange(it) },
            onSaveAsRegularToggle = { viewModel.onSaveAsRegularToggle(it) },
            onSaveDetailsClick = { viewModel.saveCustomerDetails() },
            onDismiss = { viewModel.dismissCustomerSheet() }
        )
    }

    // Discount Amount Input Dialog
    if (uiState.showDiscountDialog) {
        DiscountInputDialog(
            onDismiss = { viewModel.dismissDiscountDialog() },
            onApply = { amount -> viewModel.applyDiscount(amount) }
        )
    }

    // Service Charge Input Dialog
    if (uiState.showServiceChargeDialog) {
        ServiceChargeInputDialog(
            onDismiss = { viewModel.dismissServiceChargeDialog() },
            onApply = { amount -> viewModel.applyServiceCharge(amount) }
        )
    }

    // Premium Table Selection & Custom Table Addition Dialog
    if (showTableSelectionDialog || uiState.occupiedTablePrompt != null) {
        TableSelectionDialog(
            tables = uiState.tables,
            selectedTableNumber = uiState.tableNumber,
            occupiedPromptTable = uiState.occupiedTablePrompt,
            onTableSelect = { table ->
                viewModel.selectTable(table)
            },
            onAddTable = { name ->
                viewModel.addCustomTable(name)
            },
            onOpenOccupiedOrder = { orderId ->
                viewModel.loadOrderForEditing(orderId)
                showTableSelectionDialog = false
            },
            onDismissOccupiedPrompt = {
                viewModel.dismissOccupiedPrompt()
            },
            onDismissRequest = {
                showTableSelectionDialog = false
                viewModel.dismissOccupiedPrompt()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
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
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "Billing & Checkout",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = DarkNavy
                            )
                        )
                        Text(
                            text = "${uiState.totalItemCount} Items selected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Order Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlueLight,
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = when (uiState.orderType) {
                            OrderType.DINE_IN -> if (uiState.tableNumber.isNotBlank()) "Table ${uiState.tableNumber}" else "Dine-In"
                            OrderType.TAKEAWAY -> "Takeaway"
                            OrderType.DELIVERY -> "Delivery"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Itemized Cart Breakdown Card (Clean Vertical Bill List)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order Items (${uiState.totalItemCount})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = DarkNavy
                            )
                        )

                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }

                    HorizontalDivider(color = LightBorder)

                    if (uiState.cart.isEmpty()) {
                        Text(
                            text = "No items in cart. Please go back and add dishes.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        uiState.cart.values.forEach { cartItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Name & Unit Price
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cartItem.menuItem.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = DarkNavy
                                        )
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", cartItem.menuItem.price)} each",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                // Middle: [-] qty [+] Stepper
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease",
                                            tint = DarkNavy,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { viewModel.decrementQuantity(cartItem.menuItem.id) }
                                        )
                                        Text(
                                            text = "${cartItem.quantity}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkNavy
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase",
                                            tint = RoyalBlue,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { viewModel.incrementQuantity(cartItem.menuItem.id) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Right: Total Price for this item
                                Text(
                                    text = "₹${String.format("%.2f", cartItem.totalPrice)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = DarkNavy
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Service Type & Table Picker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Order Service Type",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        )
                    )

                    // Segmented Selector
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val types = listOf(
                                OrderType.DINE_IN to "Dine-In",
                                OrderType.TAKEAWAY to "Takeaway",
                                OrderType.DELIVERY to "Delivery"
                            )

                            types.forEach { (type, label) ->
                                val isSelected = uiState.orderType == type
                                Surface(
                                    onClick = { viewModel.setOrderType(type) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (type) {
                                                    OrderType.DINE_IN -> Icons.Default.Restaurant
                                                    OrderType.TAKEAWAY -> Icons.Default.ShoppingBag
                                                    OrderType.DELIVERY -> Icons.Default.LocalShipping
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) RoyalBlue else Color(0xFF6B7280),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) RoyalBlue else Color(0xFF4B5563)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Table Selection Row (for Dine-In)
                    if (uiState.orderType == OrderType.DINE_IN) {
                        HorizontalDivider(color = LightBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TableRestaurant,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Assigned Table",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                                    )
                                    Text(
                                        text = if (uiState.tableNumber.isNotBlank()) "Table ${uiState.tableNumber}" else "No table assigned",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = DarkNavy
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = { showTableSelectionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueLight),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (uiState.tableNumber.isNotBlank()) "Change Table" else "+ Add / Pick Table",
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Customer Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Customer Details",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                            )
                            Text(
                                text = if (uiState.attachedCustomer != null || uiState.customerPhone.isNotBlank())
                                    "${uiState.customerName.ifEmpty { "Customer" }} • ${uiState.customerPhone}"
                                else "Add customer info (Optional)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = if (uiState.customerPhone.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                    color = DarkNavy
                                )
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.openCustomerSheet() }
                    ) {
                        Text(
                            text = if (uiState.customerPhone.isNotBlank()) "EDIT" else "+ ADD",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RoyalBlue
                            )
                        )
                    }
                }
            }

            // 4. Payment Method Selector (Cash, UPI, Card, Net Banking)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Payment Mode",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PaymentMethod.values()) { method ->
                            val isSelected = uiState.paymentMethod == method
                            Surface(
                                onClick = { viewModel.setPaymentMethod(method) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) RoyalBlueLight else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) RoyalBlue else LightBorder)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = method.name.replace("_", " "),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) RoyalBlue else DarkNavy
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Bill Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Payment Summary",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        )
                    )
                    HorizontalDivider(color = LightBorder)

                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Items Subtotal", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, color = DarkNavy))
                        Text(text = "₹${String.format("%.2f", uiState.cartSubtotal)}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = DarkNavy))
                    }

                    // Discount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Discount", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, color = DarkNavy))
                        Text(
                            text = if (uiState.discountAmount > 0) "- ₹${String.format("%.2f", uiState.discountAmount)}" else "+ Add Discount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.discountAmount > 0) StatusGreen else RoyalBlue
                            ),
                            modifier = Modifier.clickable { viewModel.openDiscountDialog() }
                        )
                    }

                    // Taxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Taxes / GST", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, color = TextSecondary))
                        Text(text = "₹${String.format("%.2f", uiState.taxAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, color = TextSecondary))
                    }

                    // Service Charge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Service Charge", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, color = DarkNavy))
                        Text(
                            text = if (uiState.serviceChargeAmount > 0) "+ ₹${String.format("%.2f", uiState.serviceChargeAmount)}" else "+ Add Charge",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            ),
                            modifier = Modifier.clickable { viewModel.openServiceChargeDialog() }
                        )
                    }

                    HorizontalDivider(color = LightBorder)

                    // Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total Payable",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkNavy
                            )
                        )
                        Text(
                            text = "₹${String.format("%.2f", uiState.grandTotal)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = RoyalBlue
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Fixed Bottom Billing Action (Single SAVE & BILL Button)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = SurfaceWhite,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (uiState.orderType == OrderType.DINE_IN && uiState.tableNumber.isBlank()) {
                            showTableSelectionDialog = true
                            Toast.makeText(context, "Please select a table for Dine-In", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.completeBillingOrder(shouldPrint = false) { invNum ->
                                onOrderCompleted(invNum)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE & BILL",
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

@Composable
fun DiscountInputDialog(
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Apply Discount",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter discount amount in ₹:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) input = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = LightBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = input.toDoubleOrNull() ?: 0.0
                    onApply(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ServiceChargeInputDialog(
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Service Charge",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter service charge amount in ₹:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) input = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = LightBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = input.toDoubleOrNull() ?: 0.0
                    onApply(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(16.dp)
    )
}
