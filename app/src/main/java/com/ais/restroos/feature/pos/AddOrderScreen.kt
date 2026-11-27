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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.notification.NotificationManager
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.RoyalBlueLight
import com.ais.restroos.core.theme.StatusRed
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.domain.model.OrderType

@Composable
fun AddOrderScreen(
    viewModel: AddOrderViewModel,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewCartClick: () -> Unit,
    onHoldSavedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showTableDialog by remember { mutableStateOf(false) }
    var pendingTableAction by remember { mutableStateOf<String?>(null) }
    var variantPickerItem by remember { mutableStateOf<com.ais.restroos.domain.model.POSMenuItem?>(null) }

    if (variantPickerItem != null) {
        val selectedItem = variantPickerItem!!
        VariantPickerBottomSheet(
            item = selectedItem,
            cart = uiState.cart,
            onAddVariant = { variant -> viewModel.addToCart(selectedItem, variant) },
            onIncrementVariant = { key -> viewModel.incrementQuantity(key) },
            onDecrementVariant = { key -> viewModel.decrementQuantity(key) },
            onDismiss = { variantPickerItem = null }
        )
    }

    // Unsaved Cart Discard Confirmation Dialog
    if (uiState.showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnsavedDialog() },
            title = {
                Text(
                    text = "Discard Unsaved Cart?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                )
            },
            text = {
                Text(
                    text = "You have ${uiState.totalItemCount} items in your current order cart. Navigating back will discard this cart.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDiscardCart(onBackClick) }
                ) {
                    Text(text = "Discard", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissUnsavedDialog() }
                ) {
                    Text(text = "Keep Cart", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
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

    // Premium Table Selection & Custom Table Addition Dialog
    if (showTableDialog || uiState.occupiedTablePrompt != null) {
        TableSelectionDialog(
            tables = uiState.tables,
            selectedTableNumber = uiState.tableNumber,
            occupiedPromptTable = uiState.occupiedTablePrompt,
            onTableSelect = { table ->
                viewModel.selectTable(table)
                if (!table.isOccupied) {
                    val action = pendingTableAction
                    pendingTableAction = null
                    if (action == "KOT") {
                        showTableDialog = false
                        viewModel.saveAndHoldOrder {
                            onHoldSavedClick()
                        }
                    } else if (action == "REVIEW") {
                        showTableDialog = false
                        onViewCartClick()
                    }
                }
            },
            onAddTable = { name ->
                viewModel.addCustomTable(name)
            },
            onOpenOccupiedOrder = { orderId ->
                viewModel.loadOrderForEditing(orderId)
                showTableDialog = false
            },
            onDismissOccupiedPrompt = {
                viewModel.dismissOccupiedPrompt()
            },
            onDismissRequest = {
                showTableDialog = false
                pendingTableAction = null
                viewModel.dismissOccupiedPrompt()
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
            // A & B. Modern Dark Navy POS Header (Matches Home Screen) + Order Type & Table Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.handleBackClick(onBackClick) }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Column {
                                Text(
                                    text = if (uiState.editingOrderId != null) "Edit Order" else "New POS Order",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.5.sp,
                                        color = Color.White
                                    )
                                )
                                if (uiState.editingOrderId != null) {
                                    Text(
                                        text = "#${uiState.editingOrderId}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFFB923C),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Order Preferences",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Order Type Pills & Table Chip inside Dark Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) RoyalBlue else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) RoyalBlue else Color(0xFF334155))
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (uiState.orderType == OrderType.DINE_IN) {
                            Surface(
                                onClick = { showTableDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableRestaurant,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (uiState.tableNumber.isNotBlank()) "Table ${uiState.tableNumber}" else "+ Table",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // C. Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "Search dishes by name or code...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = RoyalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = LightBorder,
                        focusedBorderColor = RoyalBlue,
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(48.dp)
                )
            }

            // D. Category Filter Tabs (LazyRow)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.categories) { category ->
                        val isSelected = uiState.selectedCategory == category
                        val isFav = category.equals("Favourite", ignoreCase = true)
                        Surface(
                            onClick = { viewModel.onCategorySelect(category) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) RoyalBlue else SurfaceWhite,
                            border = BorderStroke(1.dp, if (isSelected) RoyalBlue else LightBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                if (isFav) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else DarkNavy
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // E. Menu Products Grid (LazyVerticalGrid 3 columns)
            if (uiState.menuItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fastfood,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No dishes match '${uiState.searchQuery}'" else "No dishes found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                fontSize = 15.5.sp
                            )
                        )
                    }
                }
            } else if (uiState.isImageView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.menuItems,
                        key = { it.id }
                    ) { item ->
                        val currentQty = if (item.variants.isNotEmpty()) {
                            uiState.cart.filter { it.value.menuItem.id == item.id }.values.sumOf { it.quantity }
                        } else {
                            uiState.cart[item.id]?.quantity ?: 0
                        }
                        MenuProductCard(
                            item = item,
                            quantity = currentQty,
                            onAddToCart = {
                                if (item.variants.isNotEmpty()) variantPickerItem = item
                                else viewModel.addToCart(item)
                            },
                            onIncrement = {
                                if (item.variants.isNotEmpty()) variantPickerItem = item
                                else viewModel.incrementQuantity(item.id)
                            },
                            onDecrement = {
                                if (item.variants.isNotEmpty()) variantPickerItem = item
                                else viewModel.decrementQuantity(item.id)
                            }
                        )
                    }
                }
            } else {
                // High-Speed List View (when Image View is turned OFF in Preferences)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.menuItems,
                        key = { it.id }
                    ) { item ->
                        val currentQty = if (item.variants.isNotEmpty()) {
                            uiState.cart.filter { it.value.menuItem.id == item.id }.values.sumOf { it.quantity }
                        } else {
                            uiState.cart[item.id]?.quantity ?: 0
                        }
                        MenuListProductRow(
                            item = item,
                            quantity = currentQty,
                            onAddToCart = {
                                if (item.variants.isNotEmpty()) variantPickerItem = item
                                else viewModel.addToCart(item)
                            },
                            onIncrement = {
                                if (item.variants.isNotEmpty()) variantPickerItem = item
                                else viewModel.incrementQuantity(item.id)
                            },
                            onDecrement = {
                                if (item.variants.isNotEmpty()) variantPickerItem = item
                                else viewModel.decrementQuantity(item.id)
                            }
                        )
                    }
                }
            }
        }

        // F. Floating Cart Overlay & Fixed Bottom Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SurfaceWhite)
                .navigationBarsPadding()
        ) {
            // Floating Cart Preview
            if (uiState.totalItemCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    FloatingCartOverlay(
                        totalItems = uiState.totalItemCount,
                        subtotal = uiState.cartSubtotal,
                        customerName = uiState.attachedCustomer?.name ?: uiState.customerName.ifEmpty { null },
                        onViewCartClick = onViewCartClick,
                        onCustomerDetailsClick = { viewModel.openCustomerSheet() }
                    )
                }
            }

            // Fixed Action Buttons: KOT & Hold | Review & Bill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT: KOT & Hold
                OutlinedButton(
                    onClick = {
                        if (uiState.totalItemCount > 0 && uiState.orderType == OrderType.DINE_IN && uiState.tableNumber.isBlank()) {
                            pendingTableAction = "KOT"
                            showTableDialog = true
                            Toast.makeText(context, "Please select a table for Dine-In", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveAndHoldOrder {
                                onHoldSavedClick()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFDDE1EA)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "KOT & Hold",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkNavy
                        )
                    )
                }

                // RIGHT: Review & Bill
                Button(
                    onClick = {
                        if (uiState.totalItemCount <= 0) {
                            NotificationManager.showError("Cart is empty! Tap '+' on dishes to add.")
                        } else if (uiState.orderType == OrderType.DINE_IN && uiState.tableNumber.isBlank()) {
                            pendingTableAction = "REVIEW"
                            showTableDialog = true
                            Toast.makeText(context, "Please select a table for Dine-In", Toast.LENGTH_SHORT).show()
                        } else {
                            onViewCartClick()
                        }
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Review & Bill",
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
private fun MenuListProductRow(
    item: com.ais.restroos.domain.model.POSMenuItem,
    quantity: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (quantity == 0) onAddToCart() else onIncrement()
            },
        shape = RoundedCornerShape(10.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, com.ais.restroos.core.theme.LightBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Veg / Non-Veg dot indicator
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, if (item.isVeg) Color(0xFF16A34A) else Color(0xFFDC2626)),
                    modifier = Modifier.size(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = if (item.isVeg) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.size(6.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = DarkNavy
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val priceLabel = if (item.variants.isNotEmpty()) {
                            val minP = item.variants.minOf { it.price }
                            if (minP % 1.0 == 0.0) "From ₹${minP.toInt()}" else "From ₹${String.format("%.2f", minP)}"
                        } else {
                            "₹${item.price.toInt()}"
                        }
                        Text(
                            text = priceLabel,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = RoyalBlue
                            )
                        )
                        if (item.category.isNotBlank() && item.category != "General") {
                            Text(
                                text = "• ${item.category}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    color = com.ais.restroos.core.theme.TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Quick Add / Stepper
            if (quantity == 0) {
                Surface(
                    modifier = Modifier.clickable { onAddToCart() },
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ADD +",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = RoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        )
                    )
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = RoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VariantPickerBottomSheet(
    item: com.ais.restroos.domain.model.POSMenuItem,
    cart: Map<String, com.ais.restroos.domain.model.CartItem>,
    onAddVariant: (com.ais.restroos.domain.model.MenuItemVariant) -> Unit,
    onIncrementVariant: (String) -> Unit,
    onDecrementVariant: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy,
                    fontSize = 18.sp
                )
            )
            Text(
                text = "Select portion / size",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item.variants.forEach { variant ->
                    val cartKey = "${item.id}__${variant.name}"
                    val qty = cart[cartKey]?.quantity ?: 0
                    val priceStr = if (variant.price % 1.0 == 0.0) {
                        "₹${variant.price.toInt()}"
                    } else {
                        "₹${String.format("%.2f", variant.price)}"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (qty > 0) RoyalBlue.copy(alpha = 0.06f) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (qty > 0) RoyalBlue else com.ais.restroos.core.theme.LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = variant.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkNavy
                                )
                                Text(
                                    text = priceStr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = RoyalBlue
                                )
                            }

                            if (qty == 0) {
                                Surface(
                                    onClick = { onAddVariant(variant) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = RoyalBlue
                                ) {
                                    Text(
                                        text = "ADD +",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { onDecrementVariant(cartKey) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = RoyalBlue.copy(alpha = 0.14f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                tint = RoyalBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$qty",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DarkNavy
                                    )
                                    Surface(
                                        onClick = { onIncrementVariant(cartKey) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = RoyalBlue,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
