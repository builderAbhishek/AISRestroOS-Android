package com.ais.restroos.feature.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ais.restroos.core.theme.SaffronPrimary
import com.ais.restroos.core.theme.StatusRed
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.SubscriptionDto
import com.ais.restroos.data.remote.dtos.SubscriptionPlanDto
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionExpiredScreen(
    onSubscriptionActive: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }

    var isChecking by remember { mutableStateOf(false) }
    var isLoadingPlans by remember { mutableStateOf(true) }
    var currentSub by remember { mutableStateOf<SubscriptionDto?>(null) }
    var plans by remember { mutableStateOf<List<SubscriptionPlanDto>>(emptyList()) }
    var cachedPlanName by remember { mutableStateOf("Subscription") }

    LaunchedEffect(Unit) {
        isLoadingPlans = true
        val state = prefs.getSubscriptionCheckState()
        if (state.third.isNotBlank()) cachedPlanName = state.third
        try {
            val subResp = RetrofitClient.apiService.getActiveSubscription()
            if (subResp.isSuccessful && subResp.body()?.status == "success") {
                currentSub = subResp.body()?.data
            }
            val plansResp = RetrofitClient.apiService.getSubscriptionPlans()
            if (plansResp.isSuccessful && plansResp.body()?.status == "success") {
                plans = plansResp.body()?.data ?: emptyList()
            }
        } catch (_: Exception) {
        } finally {
            isLoadingPlans = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .navigationBarsPadding()
    ) {
        // DarkNavy Header
        Surface(
            color = DarkNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Subscription Required",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "Renew your plan to unlock POS & Reports",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Expiry Notice Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = StatusRed.copy(alpha = 0.12f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = StatusRed,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your Plan Has Expired",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val planLabel = currentSub?.plan_name?.ifBlank { cachedPlanName } ?: cachedPlanName
                    val expiryLabel = currentSub?.expires_at?.takeIf { it.isNotBlank() }?.let { " on $it" } ?: ""
                    Text(
                        text = "$planLabel expired$expiryLabel. Please activate or renew a subscription to continue using billing, KOT printing, and analytics.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Available Plans Section
            if (isLoadingPlans) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RoyalBlue, modifier = Modifier.size(28.dp))
                }
            } else if (plans.isNotEmpty()) {
                Text(
                    text = "Available Plans",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy,
                        fontSize = 15.5.sp
                    )
                )

                plans.forEach { plan ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plan.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkNavy,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SaffronPrimary.copy(alpha = 0.12f)
                                ) {
                                    val priceStr = if (plan.price % 1.0 == 0.0) "₹${plan.price.toInt()}" else "₹${plan.price}"
                                    Text(
                                        text = if (plan.price <= 0) "Free / ${plan.duration_days}d" else "$priceStr / ${plan.duration_days} days",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SaffronPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            val featureList = plan.features
                                .removePrefix("[").removeSuffix("]")
                                .split(",", "\n")
                                .map { it.trim().trim('"') }
                                .filter { it.isNotBlank() }
                            if (featureList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                featureList.forEach { feat ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = feat,
                                            fontSize = 12.sp,
                                            color = DarkNavy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Contact / Renewal Help Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Once your subscription is renewed by your administrator, tap 'Check Status Again' below to unlock immediately.",
                        fontSize = 12.5.sp,
                        color = DarkNavy
                    )
                }
            }
        }

        // Bottom Actions
        Surface(
            color = SurfaceWhite,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (isChecking) return@Button
                        isChecking = true
                        scope.launch {
                            try {
                                val resp = RetrofitClient.apiService.getActiveSubscription()
                                val sub = resp.body()?.data
                                if (resp.isSuccessful && resp.body()?.status == "success" && sub != null) {
                                    currentSub = sub
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    prefs.saveSubscriptionCheck(
                                        dateStr = today,
                                        isExpired = sub.is_expired,
                                        planName = sub.plan_name,
                                        expiresAt = sub.expires_at
                                    )
                                    if (!sub.is_expired && sub.is_active) {
                                        Toast.makeText(context, "Subscription active! Welcome back.", Toast.LENGTH_SHORT).show()
                                        onSubscriptionActive()
                                    } else {
                                        Toast.makeText(context, "Subscription is still expired. Please renew.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Unable to verify subscription status.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network error: ${e.message ?: "Check internet"}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isChecking = false
                            }
                        }
                    },
                    enabled = !isChecking,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Status Again", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold, color = StatusRed, fontSize = 14.sp)
                }
            }
        }
    }
}
