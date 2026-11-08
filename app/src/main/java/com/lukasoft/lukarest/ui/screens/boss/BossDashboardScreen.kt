package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import kotlinx.coroutines.Job
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.BranchDto
import com.lukasoft.lukarest.data.api.ShiftMatrixResponse
import com.lukasoft.lukarest.data.api.ShiftMatrixCellDto
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
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun BossDashboardScreen(
    viewModel: LukaViewModel,
    onNavigateToRequests: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToBranches: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToShifts: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onLogout: () -> Unit
) {
    val staffList by viewModel.staffList.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val activeBranchName by AuthStore.activeBranchName.collectAsState()
    val activeBranchId by AuthStore.activeBranchId.collectAsState()
    val weekStart = DateUtils.getCurrentWeekMonday()
    val days = remember(weekStart) { DateUtils.getWeekDays(weekStart) }
    var matrix by remember(activeBranchId, weekStart) { mutableStateOf<ShiftMatrixResponse?>(null) }
    var matrixFailed by remember(activeBranchId, weekStart) { mutableStateOf(false) }
    var expandedPerson by remember(activeBranchId, weekStart) { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    var timesheet by remember { mutableStateOf<List<TimesheetRowDto>>(emptyList()) }
    var upcomingBirthdays by remember { mutableStateOf<List<UpcomingBirthdayDto>>(emptyList()) }
    var pendingReqCount by remember { mutableIntStateOf(0) }
    var branches by remember { mutableStateOf<List<BranchDto>>(emptyList()) }
    var dashboardErrors by remember { mutableStateOf(emptySet<String>()) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var switchingBranch by remember { mutableStateOf(false) }
    var branchError by remember { mutableStateOf<String?>(null) }
    var dashboardJob by remember { mutableStateOf<Job?>(null) }

    val completedTasksCount = tasks.count { it.status == TaskStatus.DONE }

    var isRefreshing by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshDashboard() {
        dashboardJob?.cancel()
        dashboardJob = coroutineScope.launch {
            isRefreshing = true
            val failedSections = mutableSetOf<String>()
            matrixFailed = false
            try {
                val response = ApiClient.shiftsService.getMatrix(weekStart)
                val plan = response.body()
                if (response.isSuccessful && plan != null) {
                    matrix = plan
                } else {
                    matrixFailed = true
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                matrixFailed = true
            }

            suspend fun loadSection(name: String, load: suspend () -> Boolean) {
                try {
                    if (!load()) failedSections += name
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failedSections += name
                }
            }

            val today = DateUtils.getTurkeyTodayIso()
            loadSection("Görevler") {
                val response = ApiClient.tasksService.getOverview(today)
                val body = response.body()
                if (!response.isSuccessful || body == null) return@loadSection false
                val serverList = body.staff.flatMap { group ->
                    group.tasks.map { it.toTaskItem(group.fullName, group.shiftShortCode.orEmpty()) }
                }
                viewModel.setTasks(serverList)
                true
            }
            loadSection("Puantaj") {
                val response = ApiClient.attendanceService.getTimesheet(today)
                val body = response.body()
                if (!response.isSuccessful || body == null) return@loadSection false
                timesheet = body
                true
            }
            loadSection("Talepler") {
                val response = ApiClient.requestsService.getBranchRequests()
                val body = response.body()
                if (!response.isSuccessful || body == null) return@loadSection false
                pendingReqCount = body.count { it.status.equals("Pending", ignoreCase = true) }
                true
            }
            loadSection("Doğum günleri") {
                val response = ApiClient.requestsService.getUpcomingBirthdays(7)
                val body = response.body()
                if (!response.isSuccessful || body == null) return@loadSection false
                upcomingBirthdays = body
                true
            }
            loadSection("Şubeler") {
                val response = ApiClient.branchService.getBranches()
                val body = response.body()
                if (!response.isSuccessful || body == null) return@loadSection false
                branches = body
                true
            }
            loadSection("Personel") {
                val response = ApiClient.staffAdminService.getStaff()
                val body = response.body()
                if (!response.isSuccessful || body == null) return@loadSection false
                val serverStaff = body.map {
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
                true
            }
            dashboardErrors = failedSections
            isRefreshing = false
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

    LaunchedEffect(activeBranchId, weekStart) {
        branches = emptyList()
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
                    actionIcon = Icons.Default.Menu,
                    actionDescription = "Yönetim menüsünü aç",
                    onActionClick = { menuOpen = true }
                )
                Box(Modifier.align(Alignment.BottomEnd).width(48.dp)) {
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.width(248.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = White,
                        border = BorderStroke(1.dp, BorderSoft)
                    ) {
                        Column(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .background(PillBlue.bg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text("Yönetim menüsü", style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                            Text(
                                activeBranchName ?: "Aktif şube seçilmedi",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val destinations = listOf(
                            Triple("Vardiyalar", Icons.Default.CalendarMonth, onNavigateToShifts),
                            Triple("Görevler", Icons.Default.TaskAlt, onNavigateToTasks),
                            Triple("Talepler", Icons.Default.ListAlt, onNavigateToRequests),
                            Triple("Personel yönetimi", Icons.Default.People, onNavigateToStaff),
                            Triple("Operasyon defteri", Icons.Default.Description, onNavigateToNotes)
                        )
                        destinations.forEach { (label, icon, navigate) ->
                            DashboardMenuItem(label, icon) {
                                menuOpen = false
                                navigate()
                            }
                        }
                        DashboardMenuItem("Şubeleri yönet", Icons.Default.Storefront) {
                            menuOpen = false
                            onNavigateToBranches()
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = BorderSoft)
                        DashboardMenuItem("Şube değiştir", Icons.Default.Storefront) {
                            menuOpen = false
                            branchError = null
                            showBranchDialog = true
                        }
                        if (AuthStore.globalRole.collectAsState().value == "CompanyOwner") DashboardMenuItem("Çıkış yap", Icons.AutoMirrored.Filled.Logout, destructive = true) {
                            menuOpen = false
                            onLogout()
                        }
                    }
                }
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
            val plan = matrix
            val people = plan?.rows.orEmpty()
            val cellsByDay = people.flatMap { it.cells }.groupBy { it.date }
            val plannedCounts = plan?.days.orEmpty().associate { it.date to it.activeStaffCount }
            val today = DateUtils.getTurkeyTodayIso()
            if (dashboardErrors.isNotEmpty()) {
                item {
                    LukaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bazı panel bilgileri alınamadı. Gösterilen değerler eksik veya eski olabilir.",
                                modifier = Modifier.weight(1f),
                                color = PillWarn.fg,
                                fontSize = 13.sp
                            )
                            TextButton(onClick = { refreshDashboard() }) {
                                Text("Tekrar dene")
                            }
                        }
                    }
                }
            }
            item {
                LukaCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).background(PillBlue.bg, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp), tint = PillBlue.fg)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Haftalık vardiya planı", style = MaterialTheme.typography.titleMedium, color = BossGradStart)
                            Text("Pazartesi – Pazar", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    if (matrixFailed) {
                        Text("Vardiya planı güncellenemedi.", color = PillErr.fg)
                        TextButton(onClick = { refreshDashboard() }) { Text("Tekrar dene") }
                    }
                    if (plan == null && !matrixFailed) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = BossGradStart)
                    }
                    if (plan != null) {
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            days.forEach { day ->
                                val count = plannedCounts[day.isoDate]
                                    ?: cellsByDay[day.isoDate].orEmpty().count { it.hasPlannedShift() }
                                val isToday = day.isoDate == today
                                val colors = if (count > 0) PillBlue else PillNeutral
                                Surface(
                                    modifier = Modifier.weight(1f).clearAndSetSemantics {
                                        val prefix = if (isToday) "Bugün, " else ""
                                        contentDescription = "$prefix${day.dayName}, $count kişi planlı"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.bg,
                                    contentColor = colors.fg,
                                    border = if (isToday) BorderStroke(1.dp, BossGradEnd) else null
                                ) {
                                    Column(
                                        Modifier.padding(vertical = 7.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(day.dayName, style = MaterialTheme.typography.labelSmall)
                                        Text("$count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (plan != null) Text("Günlük planlı kişi sayısı", color = TextMuted, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = onNavigateToShifts,
                            colors = ButtonDefaults.textButtonColors(contentColor = BtnGradStart)
                        ) { Text("Planı aç") }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Personeller", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = BossGradStart)
                    if (plan != null) StatusPill("${people.size} kişi", pillColor = PillNeutral)
                }
                if (plan != null && people.isEmpty()) Text("Bu şubede listelenecek personel yok.", color = TextMuted)
            }
            items(people, key = { it.userId }) { person ->
                val expanded = expandedPerson == person.userId
                LukaCard(contentPadding = PaddingValues(0.dp)) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(onClickLabel = if (expanded) "Vardiyaları gizle" else "Vardiyaları göster") {
                                expandedPerson = if (expanded) null else person.userId
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initials = person.fullName.split(' ').filter { it.isNotBlank() }
                            .take(2).joinToString("") { it.take(1) }
                            .uppercase(java.util.Locale.forLanguageTag("tr-TR"))
                        Box(
                            Modifier.size(36.dp).background(PillBlue.bg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = PillBlue.fg, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.fullName, color = TextMain, fontWeight = FontWeight.SemiBold)
                            Text("${person.cells.count { it.hasPlannedShift() }} vardiya", color = TextMuted, fontSize = 12.sp)
                        }
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = BossGradStart)
                    }
                    if (expanded) {
                        val cells = person.cells.associateBy { it.date }
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            days.forEach { day ->
                                HorizontalDivider(color = BorderSoft)
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text(day.dayName, Modifier.weight(1f), color = TextMain)
                                    val cell = cells[day.isoDate]
                                    when {
                                        cell?.isOffDay == true -> StatusPill("İzinli", pillColor = PillNeutral)
                                        cell?.hasPlannedShift() == true -> StatusPill(cell.dashboardLabel(), pillColor = PillBlue)
                                        else -> Text("Planlanmadı", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                    val timesheetUnavailable = "Puantaj" in dashboardErrors || isRefreshing && timesheet.isEmpty()
                    val tasksUnavailable = "Görevler" in dashboardErrors || isRefreshing && tasks.isEmpty()
                    val requestsUnavailable = "Talepler" in dashboardErrors || isRefreshing && pendingReqCount == 0


                    // KPI 1 - Canlı Puantaj
                    LukaCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = if (timesheetUnavailable) "—" else "$activeInShift", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BossGradStart)
                            Text(text = if (timesheetUnavailable) "" else " / $totalStaffCount", fontSize = 12.sp, color = TextMuted)
                        }
                        Text(
                            text = when {
                                "Puantaj" in dashboardErrors -> "puantaj alınamadı"
                                isRefreshing && timesheet.isEmpty() -> "yükleniyor..."
                                else -> "aktif vardiyada"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }


                    // KPI 2 - Görevler
                    LukaCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = if (tasksUnavailable) "—" else "$completedTasksCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BossGradStart)
                            Text(text = if (tasksUnavailable) "" else " / ${tasks.size}", fontSize = 12.sp, color = TextMuted)
                        }
                        Text(
                            text = when {
                                "Görevler" in dashboardErrors -> "görev verisi alınamadı"
                                isRefreshing && tasks.isEmpty() -> "yükleniyor..."
                                else -> "görev bitti"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }


                    // KPI 3 - Bekleyen Talepler
                    LukaCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToRequests() },
                        contentPadding = PaddingValues(10.dp),
                        backgroundColor = Color(0xFFFFEFC2)
                    ) {
                        Text(text = if (requestsUnavailable) "—" else "$pendingReqCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A4F00))
                        Text(
                            text = when {
                                "Talepler" in dashboardErrors -> "talep bilgisi alınamadı"
                                isRefreshing && pendingReqCount == 0 -> "yükleniyor..."
                                else -> "talep bekliyor"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7A4F00)
                        )
                    }
                }
            }

            // Doğum Günü Bildirim Kartı (Varsa)
            if (upcomingBirthdays.isNotEmpty() || "Doğum günleri" in dashboardErrors) {
                item {
                    LukaCard(backgroundColor = Color(0xFFFFF7ED)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "🎂", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = if ("Doğum günleri" in dashboardErrors) "Yaklaşan Doğum Günleri alınamadı" else "Yaklaşan Doğum Günleri (Bu Hafta)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                                if (upcomingBirthdays.isNotEmpty()) {
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
                        when {
                            "Şubeler" in dashboardErrors -> "Şubeler yüklenemedi."
                            isRefreshing -> "Şubeler yükleniyor…"
                            else -> "Geçiş yapılabilecek şube bulunamadı."
                        },
                        color = if ("Şubeler" in dashboardErrors) PillErr.fg else TextSoft
                    )
                    if ("Şubeler" in dashboardErrors) {
                        TextButton(onClick = { refreshDashboard() }) { Text("Tekrar dene") }
                    }
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

@Composable
private fun DashboardMenuItem(
    label: String,
    icon: ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        colors = MenuDefaults.itemColors(
            textColor = if (destructive) PillErr.fg else TextMain,
            leadingIconColor = if (destructive) PillErr.fg else PillBlue.fg
        ),
        onClick = onClick
    )
}

private fun ShiftMatrixCellDto.hasPlannedShift(): Boolean =
    !isOffDay && (shiftTemplateId != null || shiftInstanceId != null || customStartTime != null || startTime != null)

private fun ShiftMatrixCellDto?.dashboardLabel(): String {
    if (this == null) return "Planlanmadı"
    if (isOffDay) return "İzinli"
    if (!hasPlannedShift()) return "Planlanmadı"
    val start = (customStartTime ?: startTime)?.take(5)
    val end = (customEndTime ?: endTime)?.take(5)
    return listOfNotNull(shortCode?.takeIf { it.isNotBlank() }, if (start != null && end != null) "$start – $end" else null)
        .joinToString(" · ").ifEmpty { "Vardiya" }
}






