package com.ais.restroos.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.LightBorder
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.RoyalBlueLight
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary

@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val supportPhone = "+918917738295"
    val whatsappDigits = "918917738295"

    var showSetupGuide by remember { mutableStateOf(true) }
    var showFaqs by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Modern Dark Navy Header (matches Home Screen)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
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
                        text = "Help & POS Support",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Call, WhatsApp, Setup Guide & Terms",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. WhatsApp Support (+918917738295)
            SupportCardItem(
                title = "WhatsApp Support",
                subtitle = "+91 89177 38295 • Chat with Alag Innovative Solution",
                icon = Icons.Default.HeadsetMic,
                accentBg = Color(0xFFDCFCE7),
                accentTint = Color(0xFF16A34A),
                onClick = {
                    try {
                        val uri = Uri.parse("https://wa.me/$whatsappDigits?text=${Uri.encode("Hello AISRestroOS Support, I need help with my POS.")}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        Toast.makeText(context, "WhatsApp not installed. Call $supportPhone", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 2. Call Support (+918917738295)
            SupportCardItem(
                title = "Call Support Hotline",
                subtitle = "+91 89177 38295 • Direct phone assistance",
                icon = Icons.Default.Phone,
                accentBg = RoyalBlueLight,
                accentTint = RoyalBlue,
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Unable to open dialer ($supportPhone)", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 3. How to Setup Guide
            ExpandableSupportSection(
                title = "How to Setup AISRestroOS",
                subtitle = "Step-by-step guide for Menu, Printer & QR",
                icon = Icons.Default.Build,
                expanded = showSetupGuide,
                onToggle = { showSetupGuide = !showSetupGuide }
            ) {
                SetupStepRow("1. Add Menu & Portions", "Go to Items → + Add Menu Item. Enter dish name, category, and choose Single Price or Variants (Half/Full, Small/Medium/Large).")
                SetupStepRow("2. Connect Thermal Printer", "Pair your 58mm/80mm Bluetooth printer in Phone Settings, then open More → Printer and tap your printer to connect.")
                SetupStepRow("3. Customize Bill & UPI QR", "Open More → Receipt & Bill Settings to set your custom Header, Footer, 58mm text size, and upload your UPI QR code.")
                SetupStepRow("4. Take Orders & Print KOT", "Tap + at bottom center, select Table or Takeaway, add dishes, tap KOT & Hold or Review & Bill.")
                SetupStepRow("5. Track Daily Hisaab", "Tap Add Expense on Home to log daily Sabji, Kirana, Gas, Petrol & Staff expenses and view Bacha Hua (Net Cash).")
            }

            // 4. FAQs
            ExpandableSupportSection(
                title = "Frequently Asked Questions (FAQ)",
                subtitle = "Common questions about billing, printing & staff",
                icon = Icons.Default.QuestionAnswer,
                expanded = showFaqs,
                onToggle = { showFaqs = !showFaqs }
            ) {
                FaqRow(
                    q = "Can I print Hindi or Nepali dish names on my thermal printer?",
                    a = "Yes. AISRestroOS uses high-contrast raster printing so Hindi, Nepali, English, and ₹ symbols print cleanly on any 58mm or 80mm Bluetooth thermal printer."
                )
                FaqRow(
                    q = "What if my QR code is not scanning clearly on paper?",
                    a = "Upload a clear square UPI QR image in More → Receipt & Bill Settings and adjust Paper Width to 58mm or 80mm. Use Test Print to verify scan quality."
                )
                FaqRow(
                    q = "How do I delete a wrong bill or expense?",
                    a = "Open Reports → Order Reports (or Daily Hisaab for expenses) and tap the delete icon. Deleted records are excluded from totals automatically."
                )
                FaqRow(
                    q = "Can multiple staff members log in together?",
                    a = "Yes, depending on your active subscription plan, you can create staff accounts under More → Manage Staff."
                )
            }

            // 5. Terms & Conditions
            ExpandableSupportSection(
                title = "Terms & Conditions",
                subtitle = "Service, subscription & data policy",
                icon = Icons.Default.Description,
                expanded = showTerms,
                onToggle = { showTerms = !showTerms }
            ) {
                Text(
                    text = "1. Software License: AISRestroOS is licensed by Alag Innovative Solution for restaurant billing, KOT management, and business analytics on a subscription basis.\n\n" +
                            "2. Subscription & Trial: Access to POS billing, reports, and cloud sync requires an active subscription or valid trial period. Expired accounts remain locked until renewed.\n\n" +
                            "3. Data & Privacy: Your menu, orders, expenses, and customer records belong solely to your restaurant and are stored securely for your operational use.\n\n" +
                            "4. Hardware Compatibility: Bluetooth ESC/POS 58mm and 80mm thermal printers are supported. Print clarity depends on paper quality and printer battery level.\n\n" +
                            "5. Support: For billing, renewals, or technical support, contact Alag Innovative Solution at +91 89177 38295.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SupportCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentBg: Color = RoyalBlueLight,
    accentTint: Color = RoyalBlue,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Surface(
            onClick = onClick,
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
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
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandableSupportSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = onToggle,
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RoyalBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = DarkNavy
                                )
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = DarkNavy
        )
        Text(
            text = detail,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun FaqRow(q: String, a: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = "Q: $q",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = DarkNavy
        )
        Text(
            text = a,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
        )
    }
}
