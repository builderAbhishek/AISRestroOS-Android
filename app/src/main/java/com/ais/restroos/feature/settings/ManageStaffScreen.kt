package com.ais.restroos.feature.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.data.remote.dtos.CreateStaffRequest
import com.ais.restroos.data.remote.dtos.StaffMemberDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStaffScreen(
    onBackClick: () -> Unit,
    onNavigateToPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var staffList by remember { mutableStateOf<List<StaffMemberDto>>(emptyList()) }
    var planName by remember { mutableStateOf("Business Mobile Plan") }
    var maxStaff by remember { mutableIntStateOf(2) }
    var usedStaff by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<StaffMemberDto?>(null) }

    fun loadStaff() {
        coroutineScope.launch {
            isLoading = true
            try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getStaffList()
                }
                if (resp.isSuccessful && resp.body()?.status == "success") {
                    resp.body()?.data?.let { d ->
                        staffList = d.staff
                        planName = d.plan_name
                        maxStaff = d.max_staff
                        usedStaff = d.used_staff
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Could not load staff from server", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadStaff()
    }

    staffToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = { Text("Revoke Staff Access?", fontWeight = FontWeight.Bold, color = DarkNavy) },
            text = { Text("${target.name} (${target.email}) will no longer be able to login on their mobile.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val delId = target.id
                        staffToDelete = null
                        coroutineScope.launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.deleteStaff(delId, delId)
                                }
                                if (resp.isSuccessful) {
                                    Toast.makeText(context, "Staff access removed", Toast.LENGTH_SHORT).show()
                                    loadStaff()
                                } else {
                                    Toast.makeText(context, "Failed to remove staff", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { staffToDelete = null }) { Text("Cancel") }
            },
            containerColor = SurfaceWhite
        )
    }

    if (showAddSheet) {
        AddStaffModalSheet(
            onDismiss = { showAddSheet = false },
            onSave = { req ->
                coroutineScope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.createStaff(req)
                        }
                        if (resp.isSuccessful && resp.body()?.status == "success") {
                            showAddSheet = false
                            Toast.makeText(context, "Staff login created! They can now login on their mobile.", Toast.LENGTH_LONG).show()
                            loadStaff()
                        } else {
                            val errMsg = resp.body()?.message ?: "Staff limit reached or invalid details"
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, "Failed to add staff", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Dark Navy Header
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = "Manage Staff & Access",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Plan-based multi-mobile staff logins",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                IconButton(onClick = { loadStaff() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFB923C), modifier = Modifier.size(22.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plan Quota Card
            item {
                val limitReached = usedStaff >= maxStaff
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPlan() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (limitReached) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
                    ),
                    border = BorderStroke(1.dp, if (limitReached) StatusRed.copy(alpha = 0.35f) else Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (limitReached) StatusRed else Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "$planName Limit",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = if (limitReached) {
                                        "Limit reached ($usedStaff/$maxStaff logins). Tap to view plans."
                                    } else {
                                        "$usedStaff of $maxStaff multi-mobile logins used"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (limitReached) StatusRed else TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (limitReached) StatusRed else Color(0xFF2563EB)
                        ) {
                            Text(
                                text = "$usedStaff / $maxStaff",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoyalBlue)
                    }
                }
            } else {
                items(staffList, key = { it.id }) { staff ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = RoyalBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = staff.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = DarkNavy
                                        )
                                    )
                                    Text(
                                        text = staff.role.replace("_", " "),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = RoyalBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = if (staff.phone.isNotBlank()) "${staff.email} • ${staff.phone}" else staff.email,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = staff.status,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = StatusGreen,
                                            fontSize = 10.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (!staff.is_owner) {
                                    IconButton(onClick = { staffToDelete = staff }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Revoke",
                                            tint = StatusRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = SurfaceWhite,
            shadowElevation = 6.dp
        ) {
            val limitReached = usedStaff >= maxStaff
            Button(
                onClick = {
                    if (limitReached) {
                        Toast.makeText(context, "Plan limit reached ($usedStaff/$maxStaff). Upgrade plan for more logins.", Toast.LENGTH_LONG).show()
                        onNavigateToPlan()
                    } else {
                        showAddSheet = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (limitReached) Color(0xFF475569) else RoyalBlue)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (limitReached) "Upgrade Plan for More Staff" else "Add New Staff Login",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffModalSheet(
    onDismiss: () -> Unit,
    onSave: (CreateStaffRequest) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CASHIER") }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create Staff Mobile Login",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkNavy)
            )
            Text(
                text = "Staff can login on another phone using this email & password.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Staff Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Login Email *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Login Password (min 6 chars) *") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Select Role:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("CASHIER", "MANAGER", "WAITER", "CHEF").forEach { r ->
                    val isSel = role == r
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { role = r },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) RoyalBlue else BackgroundLight,
                        border = BorderStroke(1.dp, if (isSel) RoyalBlue else LightBorder)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = r,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else DarkNavy
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (name.isBlank() || !email.contains("@") || password.length < 6) {
                        Toast.makeText(context, "Enter name, valid email & password (6+ chars)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(
                        CreateStaffRequest(
                            name = name.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            role = role,
                            password = password
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Create Staff Login", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
