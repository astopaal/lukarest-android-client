package com.lukasoft.lukarest.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.lukasoft.lukarest.data.store.AppRole
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.components.BossBottomNavWithCenterFab
import com.lukasoft.lukarest.ui.components.LukaBottomNav
import com.lukasoft.lukarest.ui.components.NavItem
import com.lukasoft.lukarest.ui.screens.auth.BossLoginScreen
import com.lukasoft.lukarest.ui.screens.auth.VerifyScreen
import com.lukasoft.lukarest.ui.screens.boss.*
import com.lukasoft.lukarest.ui.screens.staff.*
import com.lukasoft.lukarest.ui.theme.BossGradStart
import com.lukasoft.lukarest.ui.theme.StaffGradStart
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel

@Composable
fun AppNavigation(
    initialToken: String? = null,
    pendingPush: Pair<String, String>? = null,
    onPushHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    viewModel: LukaViewModel = viewModel()
) {
    val sessionScope = rememberCoroutineScope()
    val role by AuthStore.role.collectAsState()
    val isPaired by AuthStore.isPaired.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // Staff Alt Bar Sekmeleri
    val staffNavItems = listOf(
        NavItem(Screen.StaffToday.route, "Bugün", Icons.Default.Today),
        NavItem(Screen.StaffShifts.route, "Vardiyalarım", Icons.Default.CalendarToday),
        NavItem(Screen.StaffTasks.route, "Görevler", Icons.Default.Checklist),
        NavItem(Screen.StaffRequests.route, "Taleplerim", Icons.Default.SwapHoriz),
        NavItem(Screen.OperationalNotes.route, "Notlar", Icons.Default.Edit)
    )

    // Boss Alt Bar Sekmeleri (Sol 2 ve Sağ 2)
    val bossLeftItems = listOf(
        NavItem(Screen.BossDashboard.route, "Panel", Icons.Default.Dashboard),
        NavItem(Screen.BossShifts.route, "Vardiyalar", Icons.Default.CalendarMonth)
    )
    val bossRightItems = listOf(
        NavItem(Screen.BossTasks.route, "Görevler", Icons.Default.TaskAlt),
        NavItem(Screen.BossRequests.route, "Talepler", Icons.Default.Notifications)
    )
    val bossAllItems = bossLeftItems + bossRightItems

    val showStaffBottomBar = currentRoute in staffNavItems.map { it.route }
    val showBossBottomBar = currentRoute in bossAllItems.map { it.route }

    val me by AuthStore.me.collectAsState()
    LaunchedEffect(pendingPush, currentRoute, me, role) {
        val (type, id) = pendingPush ?: return@LaunchedEffect
        if (me == null || currentRoute.isEmpty() || currentRoute == "gate" ||
            currentRoute == Screen.BossCompanySetup.route || currentRoute.startsWith("verify")) return@LaunchedEffect
        val validId = runCatching { java.util.UUID.fromString(id) }.isSuccess
        val destination = when (type) {
            "staff_request.created" -> if (role == AppRole.BOSS && validId)
                Screen.BossRequestDetail.createRoute(id) else null
            "staff_request.decided" -> if (role == AppRole.STAFF) Screen.StaffRequests.route else null
            "task.assigned" -> if (role == AppRole.STAFF) Screen.StaffTasks.route else null
            "shift.changed", "shift.week_assigned" -> if (role == AppRole.STAFF) Screen.StaffShifts.route else null
            "notice.created" -> Screen.OperationalNotes.route
            else -> null
        }
        onPushHandled()
        if (destination != null) {
            navController.navigate(destination) {
                if (currentRoute == destination) popUpTo(currentRoute) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        bottomBar = {
            if (showStaffBottomBar) {
                LukaBottomNav(
                    items = staffNavItems,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.StaffToday.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    activeColor = StaffGradStart
                )
            } else if (showBossBottomBar) {
                BossBottomNavWithCenterFab(
                    leftItems = bossLeftItems,
                    rightItems = bossRightItems,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.BossDashboard.route) {
                                    saveState = (route != Screen.BossDashboard.route)
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onCenterQrClick = {
                        navController.navigate(Screen.BossStaffScan.route)
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "gate",
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Gate Screen (Yönlendirici) ──
            composable("gate") {
                LaunchedEffect(role, isPaired, initialToken) {
                    if (!initialToken.isNullOrBlank()) {
                        navController.navigate("verify?token=$initialToken") {
                            popUpTo("gate") { inclusive = true }
                        }
                    } else {
                        if (!AuthStore.accessToken.value.isNullOrBlank()) {
                            try {
                                val meRes = com.lukasoft.lukarest.data.api.ApiClient.meService.getMe()
                                if (meRes.isSuccessful && meRes.body() != null) {
                                    AuthStore.updateUser(meRes.body()!!)
                                }
                            } catch (_: Exception) {}
                        }

                        when {
                            AuthStore.role.value == AppRole.BOSS -> {
                                val target = if (AuthStore.requiresSetup.value) Screen.BossCompanySetup.route else Screen.BossDashboard.route
                                navController.navigate(target) {
                                    popUpTo("gate") { inclusive = true }
                                }
                            }
                            AuthStore.role.value == AppRole.STAFF -> navController.navigate(Screen.StaffToday.route) {
                                popUpTo("gate") { inclusive = true }
                            }
                            else -> navController.navigate(Screen.StaffPair.route) {
                                popUpTo("gate") { inclusive = true }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StaffGradStart)
                }
            }

            // ── Passwordless Boss Auth ──
            composable(Screen.Login.route) {
                BossLoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVerify = { token ->
                        navController.navigate("verify?token=$token")
                    }
                )
            }

            composable("verify?token={token}") { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token")
                VerifyScreen(
                    token = token,
                    onNavigateToDashboard = {
                        navController.navigate(Screen.BossDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToCompanySetup = {
                        navController.navigate(Screen.BossCompanySetup.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Boss Şirket Kurulumu (Onboarding) ──
            composable(Screen.BossCompanySetup.route) {
                BossCompanySetupScreen(
                    onSetupComplete = {
                        navController.navigate(Screen.BossDashboard.route) {
                            popUpTo(Screen.BossCompanySetup.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Staff Ekranları ──
            composable(Screen.StaffPair.route) {
                StaffPairScreen(
                    onNavigateToBossLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    onPairSuccess = {
                        val destination = if (AuthStore.role.value == AppRole.BOSS) Screen.BossDashboard.route else Screen.StaffToday.route
                        navController.navigate(destination) {
                            popUpTo(Screen.StaffPair.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.StaffToday.route) {
                StaffTodayScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { navController.navigate(Screen.StaffTasks.route) },
                    onNavigateToPair = { navController.navigate(Screen.StaffPair.route) },
                    onNavigateToTaskComplete = { taskId ->
                        navController.navigate(Screen.StaffTaskComplete.createRoute(taskId))
                    }
                )
            }

            composable(Screen.StaffShifts.route) {
                StaffShiftsScreen(
                    viewModel = viewModel,
                    onLogout = {
                        sessionScope.launch {
                            AuthStore.endSession()
                            navController.navigate(Screen.StaffPair.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.StaffTasks.route) {
                StaffTasksScreen(
                    viewModel = viewModel,
                    onNavigateToTaskComplete = { taskId ->
                        navController.navigate(Screen.StaffTaskComplete.createRoute(taskId))
                    },
                    onLogout = {
                        sessionScope.launch {
                            AuthStore.endSession()
                            navController.navigate(Screen.StaffPair.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.StaffTaskComplete.route) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                StaffTaskCompleteScreen(
                    taskId = taskId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.StaffRequests.route) {
                StaffRequestsScreen(
                    viewModel = viewModel,
                    onNavigateToShiftChange = { navController.navigate(Screen.StaffShiftChange.route) },
                    onNavigateToLeaveChange = { navController.navigate(Screen.StaffLeaveChange.route) },
                    onLogout = {
                        sessionScope.launch {
                            AuthStore.endSession()
                            navController.navigate(Screen.StaffPair.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.StaffShiftChange.route) {
                StaffShiftChangeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.StaffLeaveChange.route) {
                StaffLeaveChangeScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Boss Ekranları ──
            composable(Screen.BossDashboard.route) {
                BossDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToRequests = { navController.navigate(Screen.BossRequests.route) },
                    onNavigateToStaff = { navController.navigate("boss/staff") },
                    onNavigateToNotes = { navController.navigate(Screen.OperationalNotes.route) },
                    onLogout = {
                        sessionScope.launch {
                            AuthStore.endSession()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.BossStaffScan.route) {
                BossStaffScanScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BossShifts.route) {
                BossShiftsScreen(
                    onNavigateToAddShift = { navController.navigate(Screen.BossAddShift.route) }
                )
            }

            composable(Screen.BossAddShift.route) {
                BossShiftAddScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BossTasks.route) {
                BossTasksScreen(
                    viewModel = viewModel,
                    onNavigateToCreateTask = { navController.navigate(Screen.BossCreateTask.route) },
                    onLogout = {
                        sessionScope.launch {
                            AuthStore.endSession()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.BossCreateTask.route) {
                BossTaskCreateScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BossRequests.route) {
                BossRequestsScreen(
                    viewModel = viewModel,
                    onNavigateToRequestDetail = { reqId ->
                        navController.navigate(Screen.BossRequestDetail.createRoute(reqId))
                    },
                    onLogout = {
                        sessionScope.launch {
                            AuthStore.endSession()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.BossRequestDetail.route) { backStackEntry ->
                val reqId = backStackEntry.arguments?.getString("requestId") ?: ""
                BossRequestDetailScreen(
                    requestId = reqId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("boss/staff") {
                StaffManagementScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.OperationalNotes.route) {
                OperationalNotesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}









