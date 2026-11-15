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
    onInitialTokenHandled: () -> Unit = {},
    pendingPush: com.lukasoft.lukarest.data.api.PushPayload? = null,
    onPushHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    viewModel: LukaViewModel = viewModel()
) {
    val sessionScope = rememberCoroutineScope()
    val role by AuthStore.role.collectAsState()
    val isPaired by AuthStore.isPaired.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && !AuthStore.accessToken.value.isNullOrBlank()) {
                sessionScope.launch {
                    try {
                        val response = com.lukasoft.lukarest.data.api.ApiClient.meService.getMe()
                        if (response.isSuccessful) response.body()?.let { AuthStore.updateUser(it) }
                        else if (response.code() == 401) AuthStore.logout()
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e
                    } catch (_: java.io.IOException) { }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(role, currentRoute) {
        if (role == AppRole.NONE && (currentRoute.startsWith("staff/") && currentRoute != Screen.StaffPair.route || currentRoute.startsWith("boss/") && currentRoute != Screen.Login.route)) {
            viewModel.setTasks(emptyList())
            navController.navigate(Screen.StaffPair.route) { popUpTo(0) { inclusive = true }; launchSingleTop = true }
        }
    }

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

    val showStaffBottomBar = role == AppRole.STAFF && currentRoute in staffNavItems.map { it.route }
    val showBossBottomBar = role == AppRole.BOSS && (currentRoute in bossAllItems.map { it.route } || currentRoute == Screen.OperationalNotes.route)

    val me by AuthStore.me.collectAsState()
    var drawerPush by remember { mutableStateOf<com.lukasoft.lukarest.data.api.PushPayload?>(null) }
    val activePush = drawerPush ?: pendingPush
    LaunchedEffect(activePush, currentRoute, me, role) {
        val push = activePush ?: return@LaunchedEffect
        val type = push.type.removeSuffix(".reminder")
        val id = push.id
        if (me == null || currentRoute.isEmpty() || currentRoute == "gate" ||
            currentRoute == Screen.BossCompanySetup.route || currentRoute.startsWith("verify")) return@LaunchedEffect
        val validId = runCatching { java.util.UUID.fromString(id) }.isSuccess
        push.notificationId?.let { notificationId ->
            try {
                val result = com.lukasoft.lukarest.data.api.ApiClient.notificationsService.open(notificationId)
                if (!result.isSuccessful) com.lukasoft.lukarest.ui.components.LukaFeedback.show("Bildirim açılışı kaydedilemedi. Bildirim panelinden tekrar açabilirsiniz.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: java.io.IOException) { com.lukasoft.lukarest.ui.components.LukaFeedback.show("Bildirim açılışı kaydedilemedi. Bağlantınızı kontrol edin.") }
        }
        val destination = when (type) {
            "request.mentioned" -> if (role == AppRole.STAFF) Screen.StaffRequests.route else if (validId) Screen.BossRequestDetail.createRoute(id) else null
            "task.approval_rejected", "task.due_soon", "task.critical_open", "task.approval_expired" -> if (role == AppRole.STAFF) Screen.StaffTasks.route else Screen.BossTasks.route
            "shift.coverage_gap" -> if (role == AppRole.BOSS) Screen.BossShifts.route else Screen.StaffShifts.route
            "staff_request.created" -> if (role == AppRole.BOSS && validId)
                Screen.BossRequestDetail.createRoute(id) else null
            "staff_request.decided" -> if (role == AppRole.STAFF) Screen.StaffRequests.route else null
            "task.assigned" -> if (role == AppRole.STAFF) Screen.StaffTasks.route else null
            "task.completed" -> if (role == AppRole.BOSS) Screen.BossTasks.route else null
            "shift.changed", "shift.week_assigned" -> if (role == AppRole.STAFF) Screen.StaffShifts.route else null
            "notice.created", "notice.mentioned" -> if (validId) "boss/notes/$id" else Screen.OperationalNotes.route
            else -> null
        }
        onPushHandled()
        drawerPush = null
        if (destination != null) {
            navController.navigate(destination) {
                if (currentRoute == destination) popUpTo(currentRoute) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    com.lukasoft.lukarest.ui.components.NotificationDrawer(onSelect = { drawerPush = it }) { openNotifications ->
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
                        onInitialTokenHandled()
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
                    onOpenNotifications = openNotifications,
                    viewModel = viewModel,
                    onNavigateToTasks = { navController.navigate(Screen.StaffTasks.route) },
                    onNavigateToTaskComplete = { taskId ->
                        navController.navigate(Screen.StaffTaskComplete.createRoute(taskId))
                    }
                )
            }

            composable(Screen.StaffShifts.route) {
                StaffShiftsScreen(
                    viewModel = viewModel,
                    onNavigateToShiftChange = { navController.navigate(Screen.StaffShiftChange.route) },
                    onNavigateToLeaveChange = { navController.navigate(Screen.StaffLeaveChange.route) }
                )
            }

            composable(Screen.StaffTasks.route) {
                StaffTasksScreen(
                    viewModel = viewModel,
                    onNavigateToTaskComplete = { taskId ->
                        navController.navigate(Screen.StaffTaskComplete.createRoute(taskId))
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
                    onNavigateToShifts = { navController.navigate(Screen.StaffShifts.route) },
                    viewModel = viewModel,
                    onNavigateToShiftChange = { navController.navigate(Screen.StaffShiftChange.route) },
                    onNavigateToLeaveChange = { navController.navigate(Screen.StaffLeaveChange.route) }
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
                    onOpenNotifications = openNotifications,
                    viewModel = viewModel,
                    onNavigateToRequests = { navController.navigate(Screen.BossRequests.route) },
                    onNavigateToStaff = { navController.navigate("boss/staff") },
                    onNavigateToBranches = { navController.navigate(Screen.BossBranches.route) },
                    onNavigateToNotes = { navController.navigate(Screen.OperationalNotes.route) },
                    onNavigateToShifts = { navController.navigate(Screen.BossShifts.route) },
                    onNavigateToTasks = { navController.navigate(Screen.BossTasks.route) },
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

            composable(Screen.BossBranches.route) {
                BranchManagementScreen(onBack = { navController.popBackStack() })
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
                    onNavigateToDefinitions = { navController.navigate("boss/tasks/definitions") },
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

            composable("boss/tasks/definitions") {
                BossTaskDefinitionsScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("boss/tasks/definitions/$id") }
                )
            }

            composable("boss/tasks/definitions/{id}") { entry ->
                BossTaskDefinitionsScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = {},
                    definitionId = entry.arguments?.getString("id")
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
                    onNavigateToShifts = { navController.navigate(Screen.BossShifts.route) },
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

            composable("boss/notes/{noteId}") { entry ->
                OperationalNotesScreen(onNavigateBack = { navController.popBackStack() }, initialNoteId = entry.arguments?.getString("noteId"))
            }
        }
    }
    }
}









