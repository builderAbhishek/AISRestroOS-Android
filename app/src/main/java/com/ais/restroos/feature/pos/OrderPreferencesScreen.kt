package com.ais.restroos.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun OrderPreferencesScreen(
    onBackClick: () -> Unit,
    preferencesManager: PreferencesManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = preferencesManager ?: remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val kotModeEnabled by manager.kotPrintEnabled.collectAsState(initial = true)
    val isImageView by manager.isImageViewEnabled.collectAsState(initial = true)
    val autoPrintBill by manager.autoPrintBill.collectAsState(initial = false)
    val defaultTaxPercent by manager.defaultTaxPercent.collectAsState(initial = 0.0)
    val defaultOrderType by manager.defaultOrderType.collectAsState(initial = "DINE_IN")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    text = "Order Preferences",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkNavy
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. KOT Printing Toggle
            PreferenceSwitchCard(
                title = "KOT Printing Mode",
                subtitle = "Automatically print Kitchen Order Tokens on hold / bill",
                checked = kotModeEnabled,
                onCheckedChange = { isChecked ->
                    coroutineScope.launch {
                        manager.setKotPrintEnabled(isChecked)
                    }
                }
            )

            // 2. Menu Grid View Toggle
            PreferenceSwitchCard(
                title = "Image View for Menu Grid",
                subtitle = "Display product cards with images. Turn off for compact text list view",
                checked = isImageView,
                onCheckedChange = { isChecked ->
                    coroutineScope.launch {
                        manager.setImageViewEnabled(isChecked)
                    }
                }
            )

            // 3. Auto-Print Bill Toggle
            PreferenceSwitchCard(
                title = "Auto-Print Bill on Checkout",
                subtitle = "Dispatch receipt to thermal printer immediately after completing billing",
                checked = autoPrintBill,
                onCheckedChange = { isChecked ->
                    coroutineScope.launch {
                        manager.setAutoPrintBill(isChecked)
                    }
                }
            )

            // 4. Default Order Service Type
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Default Order Service Type",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkNavy
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pre-selected service mode when creating a new POS order",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            "DINE_IN" to "Dine In",
                            "TAKEAWAY" to "Takeaway",
                            "DELIVERY" to "Delivery"
                        ).forEach { (key, label) ->
                            val isSelected = defaultOrderType.equals(key, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        coroutineScope.launch {
                                            manager.setDefaultOrderType(key)
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) RoyalBlue else BackgroundLight,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) RoyalBlue else LightBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else DarkNavy,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Default GST / Tax Percentage
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, LightBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Default GST / Tax Rate",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DarkNavy
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Standard tax rate applied to new orders (can be adjusted per order)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0.0 to "0% (None)",
                            5.0 to "5% GST",
                            12.0 to "12% GST",
                            18.0 to "18% GST"
                        ).forEach { (rate, label) ->
                            val isSelected = (defaultTaxPercent == rate)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        coroutineScope.launch {
                                            manager.setDefaultTaxPercent(rate)
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) RoyalBlue else BackgroundLight,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) RoyalBlue else LightBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else DarkNavy,
                                            fontSize = 11.5.sp
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
private fun PreferenceSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkNavy
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RoyalBlue
                )
            )
        }
    }
}
