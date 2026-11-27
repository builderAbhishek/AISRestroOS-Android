package com.ais.restroos.feature.settings

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.RoyalBlueLight
import com.ais.restroos.core.theme.SaffronPrimary
import com.ais.restroos.core.theme.StatusRed
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.printer.BluetoothPrinterService
import com.ais.restroos.data.printer.EscPosPrinter
import com.ais.restroos.data.remote.dtos.ReceiptSettingsDto
import com.ais.restroos.data.repository.ReceiptSettingsRepository
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.OrderItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PrinterSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }

    var headerText by remember { mutableStateOf("") }
    var footerText by remember { mutableStateOf("Thank you! Visit Again") }
    var paperWidth by remember { mutableStateOf("58mm") }
    var fontSize by remember { mutableIntStateOf(18) }
    var showQr by remember { mutableStateOf(true) }
    var qrImageUrl by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isUploadingQr by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val synced = ReceiptSettingsRepository.loadAndSyncFromServer(context)
        headerText = synced.header_text
        footerText = synced.footer_text
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.contains("AISRestroOS", ignoreCase = true) }
            .joinToString("\n")
            .ifBlank { "Thank you! Visit Again" }
        paperWidth = synced.paper_width.ifBlank { "58mm" }
        fontSize = synced.font_size.coerceIn(14, 24)
        showQr = synced.show_qr
        qrImageUrl = synced.qr_image_url
        qrBitmap = EscPosPrinter.activeQrBitmap ?: ReceiptSettingsRepository.loadCachedQrBitmap(prefs)
        isLoading = false
    }

    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingQr = true
            scope.launch {
                val res = ReceiptSettingsRepository.uploadQrImage(context, uri)
                isUploadingQr = false
                if (res.isSuccess) {
                    val (url, bmp) = res.getOrThrow()
                    qrImageUrl = url
                    qrBitmap = bmp
                    showQr = true
                    Toast.makeText(context, "QR Code uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "QR upload failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val sampleOrder = remember {
        ClosedOrder(
            orderId = "INV-01",
            invoiceNumber = "INV-0001",
            tableNumber = "T-04",
            subtotal = 500.0,
            discountAmount = 20.0,
            serviceCharge = 0.0,
            totalAmount = 480.0,
            items = listOf(
                OrderItem("1", "Paneer Tikka (Half)", 2, 160.0),
                OrderItem("2", "अंडा चाउमीन (Full)", 1, 180.0)
            )
        )
    }

    val previewBitmap = remember(headerText, footerText, paperWidth, fontSize, showQr, qrBitmap) {
        val currentDto = ReceiptSettingsDto(
            header_text = headerText,
            footer_text = footerText,
            qr_image_url = qrImageUrl,
            show_qr = showQr,
            paper_width = paperWidth,
            font_size = fontSize
        )
        EscPosPrinter.buildOrderBillPreviewBitmap(
            order = sampleOrder,
            restaurantName = headerText.ifBlank { "MY RESTAURANT" },
            settings = currentDto,
            qrBitmap = qrBitmap
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // DarkNavy Header
        Surface(
            color = DarkNavy,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
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
                        text = "Receipt & Bill Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Header, Footer, Payment QR & 58mm Fit",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RoyalBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Live Thermal Bill Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Bill Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = DarkNavy
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkNavy
                            ) {
                                Text(
                                    text = "$paperWidth • ${fontSize}px",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth(if (paperWidth == "58mm") 0.78f else 0.96f)
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Live Thermal Bill Preview",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                // 2. Paper Width & Font Size Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Paper Width & Text Size",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("58mm" to "58mm (2 inch)", "80mm" to "80mm (3 inch)").forEach { (value, label) ->
                                val selected = paperWidth == value
                                Surface(
                                    onClick = { paperWidth = value },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) RoyalBlue else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (selected) RoyalBlue else LightBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selected) Color.White else DarkNavy
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bill Text Size",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = DarkNavy
                                )
                                val sizeTag = when {
                                    fontSize <= 16 -> "Compact ($fontSize px)"
                                    fontSize <= 19 -> "Normal ($fontSize px)"
                                    else -> "Large ($fontSize px)"
                                }
                                Text(
                                    text = sizeTag,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = RoyalBlue
                                )
                            }
                            Slider(
                                value = fontSize.toFloat(),
                                onValueChange = { fontSize = it.roundToInt().coerceIn(14, 24) },
                                valueRange = 14f..24f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = RoyalBlue,
                                    activeTrackColor = RoyalBlue
                                )
                            )
                        }
                    }
                }

                // 3. Header & Footer Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Header & Footer Text",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        )

                        OutlinedTextField(
                            value = headerText,
                            onValueChange = { headerText = it },
                            label = { Text("Bill Header (Name, Address, Phone, GSTIN)") },
                            placeholder = { Text("My Restaurant\nMain Market Road\nPh: +91 98XXXXXXXX") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = LightBorder,
                                focusedBorderColor = RoyalBlue,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = footerText,
                            onValueChange = { footerText = it },
                            label = { Text("Bill Footer (Greeting / Notes)") },
                            placeholder = { Text("Thank you! Visit Again") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = LightBorder,
                                focusedBorderColor = RoyalBlue,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Note: \"Powered by AISRestroOS\" is fixed at the bottom of every bill.",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                // 4. Payment QR Code Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Payment QR Code on Bill",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkNavy
                                )
                                Text(
                                    text = "Upload your UPI / PhonePe / GPay QR",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = showQr,
                                onCheckedChange = { showQr = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalBlue)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, LightBorder),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploadingQr) {
                                        CircularProgressIndicator(color = RoyalBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap!!.asImageBitmap(),
                                            contentDescription = "Uploaded QR",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .padding(4.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        qrPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isUploadingQr,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, RoyalBlue),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (qrBitmap != null) "Change QR Code" else "Upload QR Image",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = RoyalBlue
                                    )
                                }

                                if (qrBitmap != null || !qrImageUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            qrImageUrl = null
                                            qrBitmap = null
                                            prefs.getQrCacheFile().delete()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Remove QR", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = StatusRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            color = SurfaceWhite,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val currentDto = ReceiptSettingsDto(
                                header_text = headerText,
                                footer_text = footerText,
                                qr_image_url = qrImageUrl,
                                show_qr = showQr,
                                paper_width = paperWidth,
                                font_size = fontSize
                            )
                            EscPosPrinter.updateActiveReceiptConfig(currentDto, qrBitmap)
                            val bytes = EscPosPrinter.buildOrderBillReceipt(
                                order = sampleOrder,
                                restaurantName = headerText.ifBlank { "MY RESTAURANT" },
                                paperWidth = paperWidth
                            )
                            val res = BluetoothPrinterService.printData(bytes)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Sample bill printed!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Connect printer first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SaffronPrimary),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Print", fontWeight = FontWeight.Bold, color = SaffronPrimary, fontSize = 13.5.sp)
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        scope.launch {
                            val dto = ReceiptSettingsDto(
                                header_text = headerText.trim(),
                                footer_text = footerText.trim(),
                                qr_image_url = qrImageUrl,
                                show_qr = showQr,
                                paper_width = paperWidth,
                                font_size = fontSize
                            )
                            val res = ReceiptSettingsRepository.saveSettingsToServer(context, dto, qrBitmap)
                            isSaving = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Receipt settings saved to server!", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            } else {
                                Toast.makeText(context, "Saved locally (Server: ${res.exceptionOrNull()?.message})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.5.sp)
                    }
                }
            }
        }
    }
}
