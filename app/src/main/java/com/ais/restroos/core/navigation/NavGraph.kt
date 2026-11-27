package com.ais.restroos.core.navigation

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ais.restroos.core.auth.AuthEvent
import com.ais.restroos.core.auth.AuthEventManager
import com.ais.restroos.core.components.BottomNavigationBar
import com.ais.restroos.core.notification.AppNotificationBanner
import com.ais.restroos.core.notification.NotificationManager
import com.ais.restroos.core.theme.BackgroundLight
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.core.theme.SaffronPrimary
import com.ais.restroos.core.theme.TextSecondary
import com.ais.restroos.data.local.PreferencesManager
import com.ais.restroos.data.remote.RetrofitClient
import com.ais.restroos.feature.auth.AuthScreen
import com.ais.restroos.feature.auth.AuthViewModel
import com.ais.restroos.feature.dashboard.DashboardScreen
import com.ais.restroos.feature.dashboard.DashboardViewModel
import com.ais.restroos.feature.menu.AddMenuItemScreen
import com.ais.restroos.feature.menu.AddMenuItemViewModel
import com.ais.restroos.feature.menu.MenuScreen
import com.ais.restroos.feature.orders.closed.ClosedOrdersScreen
import com.ais.restroos.feature.orders.closed.ClosedOrdersViewModel
import com.ais.restroos.feature.orders.hold.OnHoldOrdersScreen
import com.ais.restroos.feature.orders.hold.OnHoldOrdersViewModel
import com.ais.restroos.feature.pos.AddOrderScreen
import com.ais.restroos.feature.pos.AddOrderViewModel
import com.ais.restroos.feature.pos.BillReceiptScreen
import com.ais.restroos.feature.pos.CartScreen
import com.ais.restroos.feature.pos.OrderPreferencesScreen
import com.ais.restroos.feature.printer.PrinterSetupScreen
import com.ais.restroos.feature.printer.PrinterViewModel
import com.ais.restroos.feature.reports.DailyHisaabScreen
import com.ais.restroos.feature.reports.ExpenseReportScreen
import com.ais.restroos.feature.reports.InsightsScreen
import com.ais.restroos.feature.reports.ItemReportsScreen
import com.ais.restroos.feature.reports.OrderReportsScreen
import com.ais.restroos.feature.reports.ReportsScreen
import com.ais.restroos.feature.reports.StockSummaryScreen
import com.ais.restroos.feature.settings.ManageStaffScreen
import com.ais.restroos.feature.settings.MoreScreen
import com.ais.restroos.feature.settings.PlanDetailsScreen
import com.ais.restroos.feature.settings.PrinterSettingsScreen
import com.ais.restroos.feature.settings.RegularCustomersScreen
import com.ais.restroos.feature.settings.RestaurantProfileScreen
import com.ais.restroos.feature.settings.SubscriptionExpiredScreen
import com.ais.restroos.feature.settings.SupportScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainAppNavigation(
    navController: NavHostController = rememberNavController(),
    dashboardViewModel: DashboardViewModel = viewModel(),
    onHoldOrdersViewModel: OnHoldOrdersViewModel = viewModel(),
    closedOrdersViewModel: ClosedOrdersViewModel = viewModel(),
    addOrderViewModel: AddOrderViewModel = viewModel(),
    addMenuItemViewModel: AddMenuItemViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    printerViewModel: PrinterViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    authViewModel.prefsManager = preferencesManager
    printerViewModel.prefsManager = preferencesManager
    addOrderViewModel.prefsManager = preferencesManager
    dashboardViewModel.prefsManager = preferencesManager

    var authCheckState by remember { mutableStateOf("CHECKING") }
    val scope = rememberCoroutineScope()

    fun checkDailySubscription(force: Boolean = false) {
        scope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val (lastDate, wasExpired, _) = preferencesManager.getSubscriptionCheckState()
                if (!force && lastDate == today) {
                    if (wasExpired && navController.currentDestination?.route != Screen.SubscriptionExpired.route) {
                        navController.navigate(Screen.SubscriptionExpired.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    return@launch
                }
                val resp = RetrofitClient.apiService.getActiveSubscription()
                val sub = resp.body()?.data
                if (resp.isSuccessful && resp.body()?.status == "success" && sub != null) {
                    val expired = sub.is_expired || !sub.is_active
                    preferencesManager.saveSubscriptionCheck(
                        dateStr = today,
                        isExpired = expired,
                        planName = sub.plan_name,
                        expiresAt = sub.expires_at
                    )
                    if (expired && navController.currentDestination?.route != Screen.SubscriptionExpired.route) {
                        navController.navigate(Screen.SubscriptionExpired.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(authCheckState) {
        if (authCheckState == "AUTHENTICATED") {
            checkDailySubscription(force = false)
            scope.launch {
                com.ais.restroos.data.repository.ReceiptSettingsRepository.loadAndSyncFromServer(context)
            }
        }
    }

    // Global Session Event Listener (401/403 or Logout)
    LaunchedEffect(Unit) {
        AuthEventManager.events.collect { event ->
            when (event) {
                is AuthEvent.SessionExpired -> {
                    com.ais.restroos.data.repository.OrderRepository.stopRealtimeSync()
                    NotificationManager.showError(event.message)
                    authCheckState = "UNAUTHENTICATED"
                    if (navController.currentDestination?.route != Screen.Auth.route) {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                is AuthEvent.LoggedOut -> {
                    com.ais.restroos.data.repository.OrderRepository.stopRealtimeSync()
                    NotificationManager.showInfo("Logged out successfully.")
                    authCheckState = "UNAUTHENTICATED"
                    if (navController.currentDestination?.route != Screen.Auth.route) {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                is AuthEvent.LoginSuccess -> {
                    authCheckState = "AUTHENTICATED"
                    NotificationManager.showSuccess("Terminal authenticated successfully!")
                    dashboardViewModel.loadBackendData()
                    com.ais.restroos.data.repository.OrderRepository.startRealtimeSync()
                    checkDailySubscription(force = true)
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    val showBottomBar = authCheckState == "AUTHENTICATED" && currentRoute in listOf(
        Screen.Home.route,
        Screen.Items.route,
        Screen.Reports.route,
        Screen.More.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAddOrderClick = {
                            addOrderViewModel.resetForNewOrder()
                            navController.navigate(Screen.AddOrderPOS.route)
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(150)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        authViewModel = authViewModel,
                        onAuthenticated = {
                            authCheckState = "AUTHENTICATED"
                            dashboardViewModel.loadBackendData()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        },
                        onUnauthenticated = {
                            authCheckState = "UNAUTHENTICATED"
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Auth.route) {
                    AuthScreen(
                        viewModel = authViewModel,
                        onAuthSuccess = {
                            authCheckState = "AUTHENTICATED"
                            dashboardViewModel.loadBackendData()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                    )
                }

            composable(Screen.Home.route) {
                if (authCheckState != "AUTHENTICATED") {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToClosedOrders = { navController.navigate(Screen.ClosedOrders.route) },
                        onNavigateToHoldOrders = { navController.navigate(Screen.HoldOrders.route) },
                        onNavigateToAddExpense = { navController.navigate(Screen.DailyHisaab.route) },
                        onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                        onPrinterStatusClick = { navController.navigate(Screen.PrinterSetup.route) },
                        onWhatsAppClick = {
                            Toast.makeText(context, "Opening WhatsApp Business Marketing Hub", Toast.LENGTH_SHORT).show()
                        },
                        onGoogleBusinessClick = {
                            Toast.makeText(context, "Opening Google Business AI Profile Manager", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            composable(Screen.Items.route) {
                MenuScreen(
                    onAddMenuItemClick = {
                        addMenuItemViewModel.initForNewItem()
                        navController.navigate(Screen.AddMenuItem.route)
                    },
                    onEditMenuItemClick = { item ->
                        addMenuItemViewModel.loadItemForEditing(item)
                        navController.navigate(Screen.AddMenuItem.route)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.AddMenuItem.route) {
                AddMenuItemScreen(
                    viewModel = addMenuItemViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    onNavigateToDailyHisaab = { navController.navigate(Screen.DailyHisaab.route) },
                    onNavigateToItemReports = { navController.navigate(Screen.ItemReports.route) },
                    onNavigateToOrderReports = { navController.navigate(Screen.OrderReports.route) },
                    onNavigateToStockSummary = { navController.navigate(Screen.StockSummary.route) },
                    onNavigateToExpenseReport = { navController.navigate(Screen.ExpenseReport.route) },
                    onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                    onBackClick = if (currentRoute != Screen.Reports.route) { { navController.popBackStack() } } else null
                )
            }

            composable(Screen.ExpenseReport.route) {
                ExpenseReportScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.DailyHisaab.route) {
                DailyHisaabScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToDailyHisaab = { navController.navigate(Screen.DailyHisaab.route) }
                )
            }

            composable(Screen.ItemReports.route) {
                ItemReportsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.OrderReports.route) {
                OrderReportsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.StockSummary.route) {
                StockSummaryScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.HoldOrders.route) {
                OnHoldOrdersScreen(
                    viewModel = onHoldOrdersViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditOrder = { orderId ->
                        navController.navigate(Screen.EditOrderPOS.createRoute(orderId))
                    },
                    onAddNewOrder = {
                        addOrderViewModel.resetForNewOrder()
                        navController.navigate(Screen.AddOrderPOS.route)
                    }
                )
            }

            composable(Screen.ClosedOrders.route) {
                ClosedOrdersScreen(
                    viewModel = closedOrdersViewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditOrder = { orderId ->
                        navController.navigate(Screen.EditOrderPOS.createRoute(orderId))
                    },
                    onViewBillReceipt = { orderId ->
                        navController.navigate(Screen.BillReceipt.createRoute(orderId))
                    },
                    onAddNewOrder = {
                        addOrderViewModel.resetForNewOrder()
                        navController.navigate(Screen.AddOrderPOS.route)
                    }
                )
            }

            composable(Screen.AddOrderPOS.route) {
                LaunchedEffect(Unit) {
                    if (addOrderViewModel.uiState.value.editingOrderId != null) {
                        addOrderViewModel.resetForNewOrder()
                    }
                }
                AddOrderScreen(
                    viewModel = addOrderViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Screen.PreferencesPOS.route) },
                    onViewCartClick = { navController.navigate(Screen.CartPOS.route) },
                    onHoldSavedClick = {
                        navController.navigate(Screen.HoldOrders.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.CartPOS.route) {
                CartScreen(
                    viewModel = addOrderViewModel,
                    onBackClick = { navController.popBackStack() },
                    onOrderCompleted = { invoiceNum ->
                        navController.navigate(Screen.BillReceipt.createRoute(invoiceNum)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.BillReceipt.route,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                BillReceiptScreen(
                    orderId = orderId,
                    viewModel = addOrderViewModel,
                    onCloseClick = {
                        navController.navigate(Screen.HoldOrders.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onPrintAndRedirect = {
                        navController.navigate(Screen.HoldOrders.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.PreferencesPOS.route) {
                OrderPreferencesScreen(
                    onBackClick = { navController.popBackStack() },
                    preferencesManager = preferencesManager
                )
            }

            composable(
                route = Screen.EditOrderPOS.route,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                LaunchedEffect(orderId) {
                    addOrderViewModel.loadOrderForEditing(orderId)
                }
                AddOrderScreen(
                    viewModel = addOrderViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Screen.PreferencesPOS.route) },
                    onViewCartClick = { navController.navigate(Screen.CartPOS.route) },
                    onHoldSavedClick = {
                        navController.navigate(Screen.HoldOrders.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToRestaurantProfile = { navController.navigate(Screen.RestaurantProfile.route) },
                    onNavigateToPlan = { navController.navigate(Screen.PlanDetails.route) },
                    onNavigateToCustomers = { navController.navigate(Screen.RegularCustomers.route) },
                    onNavigateToStaff = { navController.navigate(Screen.ManageStaff.route) },
                    onNavigateToPrinterSettings = {
                        navController.navigate(Screen.PrinterSetup.route)
                    },
                    onNavigateToReceiptSettings = {
                        navController.navigate(Screen.ReceiptSettings.route)
                    },
                    onNavigateToSupport = { navController.navigate(Screen.Support.route) },
                    onLogoutClick = {
                        authViewModel.logout {
                            authCheckState = "UNAUTHENTICATED"
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.PrinterSetup.route) {
                PrinterSetupScreen(
                    viewModel = printerViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.RegularCustomers.route) {
                RegularCustomersScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageStaff.route) {
                ManageStaffScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToPlan = { navController.navigate(Screen.PlanDetails.route) }
                )
            }

            composable(Screen.RestaurantProfile.route) {
                RestaurantProfileScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.PlanDetails.route) {
                PlanDetailsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Support.route) {
                SupportScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.ReceiptSettings.route) {
                PrinterSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.SubscriptionExpired.route) {
                SubscriptionExpiredScreen(
                    onSubscriptionActive = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onLogout = {
                        authViewModel.logout {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }

    // Modern floating bottom notification overlay
    AppNotificationBanner(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = if (showBottomBar) 76.dp else 16.dp)
    )
}
}

@Composable
fun SplashScreen(
    authViewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit
) {
    LaunchedEffect(Unit) {
        authViewModel.verifyBackendSession { isValid ->
            if (isValid) {
                onAuthenticated()
            } else {
                onUnauthenticated()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = com.ais.restroos.R.drawable.app_logo),
                contentDescription = "AISRestroOS Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "AISRestroOS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 24.sp,
                    color = DarkNavy
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Smart Restaurant Terminal",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 13.5.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            CircularProgressIndicator(
                color = SaffronPrimary,
                modifier = Modifier.size(26.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}
