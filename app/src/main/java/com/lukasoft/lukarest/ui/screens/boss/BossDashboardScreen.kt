package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Logout
import kotlinx.coroutines.Job
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.BranchDto
import com.lukasoft.lukarest.data.api.SwitchBranchCommand
import com.lukasoft.lukarest.data.api.TimesheetRowDto
import com.lukasoft.lukarest.data.api.UpcomingBirthdayDto
import com.lukasoft.lukarest.data.api.toTaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch

@Composable
fun BossDashboardScreen(
    viewModel: LukaViewModel,
    onNavigateToRequests: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToNotes: () -> Unit = {},
    onLogout: () -> Unit
) {
    val staffList by viewModel.staffList.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val activeBranchName by AuthStore.activeBranchName.collectAsState()
    val activeBranchId by AuthStore.activeBranchId.collectAsState()

    var timesheet by remember { mutableStateOf<List<TimesheetRowDto>>(emptyList()) }
    var upcomingBirthdays by remember { mutableStateOf<List<UpcomingBirthdayDto>>(emptyList()) }
    var pendingReqCount by remember { mutableIntStateOf(0) }
    var branches by remember { mutableStateOf<List<BranchDto>>(emptyList()) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var switchingBranch by remember { mutableStateOf(false) }
    var branchError by remember { mutableStateOf<String?>(null) }
    var dashboardJob by remember { mutableStateOf<Job?>(null) }

    val completedTasksCount = tasks.count { it.status == TaskStatus.DONE }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshDashboard() {
        dashboardJob?.cancel()
        dashboardJob = coroutineScope.launch {
            isRefreshing = true
            try {
                val today = DateUtils.getTurkeyTodayIso()

                // 1) Şubedeki güncel görevleri çek
                val tRes = ApiClient.tasksService.getOverview(today)
                if (tRes.isSuccessful && tRes.body() != null) {
                    val serverList = tRes.body()!!.staff.flatMap { group ->
                        group.tasks.map { it.toTaskItem(group.fullName, group.shiftShortCode.orEmpty()) }
                    }
                    viewModel.setTasks(serverList)
                }

                // 2) Canlı Puantaj (Timesheet)
                val attRes = ApiClient.attendanceService.getTimesheet(today)
                if (attRes.isSuccessful && attRes.body() != null) {
                    timesheet = attRes.body()!!
                }

                // 3) Bekleyen Talepler
                val rRes = ApiClient.requestsService.getBranchRequests()
                if (rRes.isSuccessful && rRes.body() != null) {
                    pendingReqCount = rRes.body()!!.count { it.status.equals("Pending", ignoreCase = true) }
                }

                // 4) Yaklaşan Doğum Günleri
                val bRes = ApiClient.requestsService.getUpcomingBirthdays(7)
                if (bRes.isSuccessful && bRes.body() != null) {
                    upcomingBirthdays = bRes.body()!!
                }

                // 5) Şubeler Listesi
                val brRes = ApiClient.branchService.getBranches()
                if (brRes.isSuccessful && brRes.body() != null) {
                    branches = brRes.body()!!

                }

                // 6) Personel Listesi
                val stRes = ApiClient.staffAdminService.getStaff()
                if (stRes.isSuccessful && stRes.body() != null) {
                    val serverStaff = stRes.body()!!.map {
                        com.lukasoft.lukarest.data.model.StaffMember(
                            id = it.userId,
                            fullName = it.fullName,
                            initials = it.fullName.split(" ").mapNotNull { n -> n.firstOrNull()?.toString() }.take(2).joinToString("").uppercase(),
                            avatarGrad = Pair(Color(0xFFC9DEFF), Color(0xFF90B8F8)),
                            avatarFg = Color(0xFF12306B),
                            workType = if (it.employmentType.contains("Full", ignoreCase = true)) "full-time" else "part-time",
                            phone = it.phoneNumber
                        )
                    }
                    viewModel.setStaffList(serverStaff)
                }
            } catch (_: Exception) {
            } finally {
                isRefreshing = false
            }
        }
    }

    fun handleSwitchBranch(targetBranch: BranchDto) {
        if (switchingBranch) return
        if (targetBranch.id == activeBranchId) {
            showBranchDialog = false
            return
        }
        val refreshToken = AuthStore.refreshToken.value
        if (refreshToken == null) {
            branchError = "Oturum bilgisi bulunamadı. Yeniden giriş yapın."
            return
        }
        switchingBranch = true
        branchError = null
        coroutineScope.launch {
            try {
                val result = ApiClient.authService.switchBranch(SwitchBranchCommand(targetBranch.id, refreshToken))
                val tokens = result.body()
                if (!result.isSuccessful || tokens == null) {
                    branchError = "Şube değiştirilemedi (${result.code()})."
                    return@launch
                }
                dashboardJob?.cancel()
                AuthStore.setTokens(tokens.accessToken, tokens.refreshToken)
                AuthStore.updateActiveBranch(targetBranch.id, targetBranch.name)
                showBranchDialog = false
                ApiClient.meService.getMe().body()?.let(AuthStore::updateUser)
            } catch (_: java.io.IOException) {
                if (showBranchDialog) branchError = "Sunucuya ulaşılamadı."
            } finally {
                switchingBranch = false
            }
        }
    }

    LaunchedEffect(activeBranchId) {
        timesheet = emptyList()
        upcomingBirthdays = emptyList()
        pendingReqCount = 0
        viewModel.setStaffList(emptyList())
        viewModel.setTasks(emptyList())
        refreshDashboard()
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Yönetim Paneli",
                    subtitle = activeBranchName ?: "Şube seç",
                    initials = "P",
                    gradient = BossGradient,
                    actionIcon = Icons.AutoMirrored.Filled.Logout,
                    onActionClick = onLogout
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshDashboard() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            item {
                Text("Bugün", style = MaterialTheme.typography.titleMedium, color = BossGradStart)
            }

            // KPI Satırı
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeInShift = timesheet.count { it.clockInUtc != null && it.clockOutUtc == null }
                    val totalStaffCount = if (timesheet.isNotEmpty()) timesheet.size else staffList.size


                    // KPI 1 - Canlı Puantaj
                    LukaCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "$activeInShift", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BossGradStart)
                            Text(text = " / $totalStaffCount", fontSize = 12.sp, color = TextMuted)
                        }
                        Text(text = "aktif vardiyada", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    }


                    // KPI 2 - Görevler
                    LukaCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "$completedTasksCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BossGradStart)
                            Text(text = " / ${tasks.size}", fontSize = 12.sp, color = TextMuted)
                        }
                        Text(text = "görev bitti", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    }


                    // KPI 3 - Bekleyen Talepler
                    LukaCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToRequests() },
                        contentPadding = PaddingValues(10.dp),
                        backgroundColor = Color(0xFFFFEFC2)
                    ) {
                        Text(text = "$pendingReqCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A4F00))
                        Text(text = "talep bekliyor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A4F00))
                    }
                }
            }

            // Doğum Günü Bildirim Kartı (Varsa)
            if (upcomingBirthdays.isNotEmpty()) {
                item {
                    LukaCard(backgroundColor = Color(0xFFFFF7ED)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "🎂", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "Yaklaşan Doğum Günleri (Bu Hafta)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                                Text(
                                    text = upcomingBirthdays.joinToString { "${it.fullName} (${if (it.daysUntil == 0) "Bugün!" else "${it.daysUntil} gün"})" },
                                    fontSize = 12.sp,
                                    color = Color(0xFF9A3412)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (showBranchDialog) {
        AlertDialog(
            onDismissRequest = { if (!switchingBranch) showBranchDialog = false },
            containerColor = White,
            title = { Text(text = "Şube Değiştir", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeBranches = branches.filter { it.isActive }
                    if (activeBranches.isEmpty()) Text(
                        if (isRefreshing) "Şubeler yükleniyor…" else "Geçiş yapılabilecek şube bulunamadı.",
                        color = TextSoft
                    )
                    branchError?.let { Text(it, color = PillErr.fg) }
                    if (switchingBranch) LinearProgressIndicator(Modifier.fillMaxWidth(), color = BossGradStart)
                    activeBranches.forEach { br ->
                        val isSelected = br.id == activeBranchId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PillBlue.bg else BgScreenTop)
                                .clickable(enabled = !switchingBranch) { handleSwitchBranch(br) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = br.name, fontWeight = FontWeight.Bold, color = TextMain)
                                Text(text = "${br.branchCode} • ${br.openingTime}-${br.closingTime}", fontSize = 12.sp, color = TextMuted)
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = StaffGradStart)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !switchingBranch, onClick = { showBranchDialog = false }) {
                    Text(text = "Kapat")
                }
            }
        )
    }
}
}






