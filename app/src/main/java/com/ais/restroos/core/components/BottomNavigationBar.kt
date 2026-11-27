package com.ais.restroos.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ais.restroos.core.theme.SaffronPrimary
import com.ais.restroos.core.theme.SurfaceWhite
import com.ais.restroos.core.theme.TextSecondary

sealed class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : NavItem("home", "Home", Icons.Default.Home)
    object Items : NavItem("items", "Items", Icons.Default.RestaurantMenu)
    object AddOrder : NavItem("add_order", "Add Order", Icons.Default.Add)
    object Reports : NavItem("reports", "Reports", Icons.Default.Analytics)
    object More : NavItem("more", "More", Icons.Default.Menu)
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onAddOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Bottom Bar Surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceWhite,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                NavBarItem(
                    item = NavItem.Home,
                    isSelected = currentRoute == NavItem.Home.route,
                    onClick = {
                        if (currentRoute != NavItem.Home.route) {
                            onNavigate(NavItem.Home.route)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 2. Items
                NavBarItem(
                    item = NavItem.Items,
                    isSelected = currentRoute == NavItem.Items.route,
                    onClick = {
                        if (currentRoute != NavItem.Items.route) {
                            onNavigate(NavItem.Items.route)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 3. Center (+) Add Order FAB
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SaffronPrimary)
                            .clickable { onAddOrderClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Order",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // 4. Reports
                NavBarItem(
                    item = NavItem.Reports,
                    isSelected = currentRoute == NavItem.Reports.route,
                    onClick = {
                        if (currentRoute != NavItem.Reports.route) {
                            onNavigate(NavItem.Reports.route)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 5. More
                NavBarItem(
                    item = NavItem.More,
                    isSelected = currentRoute == NavItem.More.route,
                    onClick = {
                        if (currentRoute != NavItem.More.route) {
                            onNavigate(NavItem.More.route)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = SaffronPrimary
    val inactiveColor = TextSecondary

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}