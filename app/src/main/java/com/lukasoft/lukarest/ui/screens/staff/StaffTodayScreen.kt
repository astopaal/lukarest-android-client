package com.lukasoft.lukarest.ui.screens.staff

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.AttendanceDayDto
import com.lukasoft.lukarest.data.api.MyShiftDayDto
import com.lukasoft.lukarest.data.api.toTaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.*
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch

@Composable
fun StaffTodayScreen(
    onOpenNotifications: () -> Unit,
    viewModel: LukaViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToTaskComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val todayIso = DateUtils.getTurkeyTodayIso()
    val currentMonday = remember { DateUtils.getCurrentWeekMonday() }
    val weekDays = remember(currentMonday) { DateUtils.getWeekDays(currentMonday) }

    var isLoading by remember { mutableStateOf(false) }
    val todayTasks by viewModel.tasks.collectAsState()
    var todayMyShiftDay by remember { mutableStateOf<MyShiftDayDto?>(null) }
    var attendanceData by remember { mutableStateOf<AttendanceDayDto?>(null) }
    var isAttendanceActionLoading by remember { mutableStateOf(false) }
    var loadErrors by remember { mutableStateOf(emptySet<String>()) }
    var clockInError by remember { mutableStateOf<String?>(null) }
    val breakStartedAt = attendanceData?.breaks?.lastOrNull { it.endUtc == null }?.startUtc
    val breakMinutes by produceState(0L, breakStartedAt) {
        val start = breakStartedAt?.let(DateUtils::utcTimestampMillis) ?: return@produceState
        while (true) {
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0L)
            value = elapsed / 60_000L
            kotlinx.coroutines.delay(60_000L - elapsed % 60_000L)
        }
    }

    fun loadTodayData() {
        coroutineScope.launch {
            isLoading = true
            val failedSections = mutableSetOf<String>()

            // 1) Personelin kendi haftalık vardiyalarını çek (Yönetici gerektirmez)
            try {
                val sRes = ApiClient.shiftsService.getMyShifts(currentMonday)
                if (sRes.isSuccessful && sRes.body() != null) {
                    val days = sRes.body()!!.days
                    todayMyShiftDay = days.find { it.date == todayIso }
                } else {
                    failedSections += "Vardiya"
                }
            } catch (_: Exception) {
                failedSections += "Vardiya"
            }

            // 2) Bugünkü görevleri çek
            try {
                val taskRes = ApiClient.tasksService.getMyTasks()
                if (taskRes.isSuccessful && taskRes.body() != null) {
                    val taskResponse = taskRes.body()!!
                    viewModel.setTasks(taskResponse.tasks.map { it.toTaskItem("me") })
                } else {
                    failedSections += "Görevler"
                }
            } catch (_: Exception) {
                failedSections += "Görevler"
            }

            // 3) Puantaj / Mola durumunu çek
            try {
                val attRes = ApiClient.attendanceService.getMyAttendance()
                if (attRes.isSuccessful && attRes.body() != null) {
                    attendanceData = attRes.body()
                } else {
                    failedSections += "Puantaj"
                }
            } catch (_: Exception) {
                failedSections += "Puantaj"
            } finally {
                loadErrors = failedSections
                isLoading = false
            }
        }
    }

    fun handleClockIn() {
        coroutineScope.launch {
            isAttendanceActionLoading = true
            clockInError = null
            try {
                val res = ApiClient.attendanceService.clockIn()
                if (res.isSuccessful && res.body() != null) {
                    attendanceData = res.body()
                    LukaFeedback.show("Vardiyaya giriş yapıldı!")
                } else {
                    clockInError = com.lukasoft.lukarest.data.api.apiErrorMessage(
                        res.errorBody()?.string(), "Vardiya başlatılamadı. Tekrar deneyin."
                    )
                }
            } catch (_: java.io.IOException) {
                clockInError = "Bağlantı kurulamadı. Vardiyayı tekrar başlatmayı deneyin."
            } finally {
                isAttendanceActionLoading = false
                loadTodayData()
            }
        }
    }

    fun handleClockOut() {
        coroutineScope.launch {
            isAttendanceActionLoading = true
            try {
                val res = ApiClient.attendanceService.clockOut()
                if (res.isSuccessful && res.body() != null) {
                    attendanceData = res.body()
                    LukaFeedback.show("Vardiya tamamlandı!")
                } else {
                    LukaFeedback.show("Çıkış yapılamadı: ${res.code()}")
                }
            } catch (e: Exception) {
                LukaFeedback.show("Hata: ${e.localizedMessage}")
            } finally {
                isAttendanceActionLoading = false
                loadTodayData()
            }
        }
    }

    fun handleStartBreak() {
        coroutineScope.launch {
            isAttendanceActionLoading = true
            try {
                val res = ApiClient.attendanceService.startBreak()
                if (res.isSuccessful && res.body() != null) {
                    attendanceData = res.body()
                    LukaFeedback.show("Mola başladı!")
                } else {
                    LukaFeedback.show("Mola başlatılamadı: ${res.code()}")
                }
            } catch (e: Exception) {
                LukaFeedback.show("Hata: ${e.localizedMessage}")
            } finally {
                isAttendanceActionLoading = false
                loadTodayData()
            }
        }
    }

    fun handleEndBreak() {
        coroutineScope.launch {
            isAttendanceActionLoading = true
            try {
                val res = ApiClient.attendanceService.endBreak()
                if (res.isSuccessful && res.body() != null) {
                    attendanceData = res.body()
                    LukaFeedback.show("Mola bitti!")
                } else {
                    LukaFeedback.show("Mola bitirilemedi: ${res.code()}")
                }
            } catch (e: Exception) {
                LukaFeedback.show("Hata: ${e.localizedMessage}")
            } finally {
                isAttendanceActionLoading = false
                loadTodayData()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTodayData()
    }

    val completedCount = todayTasks.count { it.status == TaskStatus.DONE }
    val totalCount = todayTasks.size

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                TopPill(
                    title = "Bugün",
                    subtitle = currentUser?.fullName ?: AuthStore.staffName.value ?: "Personel",
                    initials = "P",
                    gradient = StaffGradient,
                    actionIcon = Icons.Default.Notifications,
                    actionDescription = "Bildirimleri aç",
                    onActionClick = onOpenNotifications
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { loadTodayData() },
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
            if (loadErrors.isNotEmpty()) {
                item {
                    LukaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bazı bilgiler güncellenemedi. Gösterilen veriler eski olabilir.",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = PillWarn.fg
                            )
                            TextButton(onClick = { loadTodayData() }) {
                                Text("Tekrar dene")
                            }
                        }
                    }
                }
            }

            // Haftalık Gün Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { day ->
                        val isToday = day.isoDate == todayIso
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = day.dayName,
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isToday) StaffGradStart else TextMuted
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) StaffGradStart else White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.dayNumber,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) White else TextMain
                                )
                            }
                        }
                    }
                }
            }

            // Aktif Vardiya Kartı
            item {
                val isOff = attendanceData?.takeIf { it.shiftInstanceId != null }?.isOffDay
                    ?: (todayMyShiftDay?.isOffDay == true)
                val hasShift = (todayMyShiftDay?.startTime != null && todayMyShiftDay?.endTime != null) || attendanceData?.plannedStart != null
                val isClockedIn = attendanceData?.clockInUtc != null
                val isClockedOut = attendanceData?.clockOutUtc != null
                val hasOpenBreak = attendanceData?.breaks?.any { it.endUtc == null } == true

                val statusText = when {
                    isOff -> "İzinli"
                    isClockedOut -> "Çıkış Yapıldı"
                    hasOpenBreak -> "Molada"
                    isClockedIn -> "Vardiyada (Aktif)"
                    hasShift -> "Başlamadı"
                    else -> "Serbest"
                }

                val statusPillColor = when {
                    isOff -> PillErr
                    isClockedOut -> PillNeutral
                    hasOpenBreak -> PillWarn
                    isClockedIn -> PillOk
                    hasShift -> PillBlue
                    else -> PillNeutral
                }

                LukaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bugünkü Vardiya",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            Text(
                                text = when {
                                    isOff -> "Bugün Haftalık İzinlisiniz"
                                    hasShift -> "Vardiya (${todayMyShiftDay?.shortCode ?: "Planlı"})"
                                    else -> "Bugün İçin Planlanan Vardiya Yok"
                                },
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                        StatusPill(
                            text = statusText,
                            pillColor = statusPillColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val startFormatted = (attendanceData?.plannedStart ?: todayMyShiftDay?.startTime)?.take(5)
                    val endFormatted = (attendanceData?.plannedEnd ?: todayMyShiftDay?.endTime)?.take(5)
                    clockInError?.let { Text(it, color = PillErr.fg, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    if (hasOpenBreak && breakStartedAt != null) {
                        Text("Mola süresi: $breakMinutes dk", color = TextMain, style = MaterialTheme.typography.titleSmall)
                    }

                    Text(
                        text = when {
                            isOff -> "İzin Günü"
                            startFormatted != null && endFormatted != null -> "$startFormatted – $endFormatted"
                            else -> "—"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffGradStart
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isOff) {
                        when {
                            !isClockedIn -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PrimaryButton(
                                        text = when {
                                            isAttendanceActionLoading -> "İşleniyor..."
                                            isLoading -> "Durum kontrol ediliyor..."
                                            "Puantaj" in loadErrors -> "Puantaj alınamadı"
                                            else -> "Vardiyaya Giriş Yap"
                                        },
                                        onClick = { if (!isAttendanceActionLoading && !isLoading && "Puantaj" !in loadErrors) handleClockIn() },
                                        enabled = !isAttendanceActionLoading && !isLoading && "Puantaj" !in loadErrors,
                                        modifier = Modifier.weight(1f),
                                        height = 42.dp
                                    )
                                    OutlinedButton(
                                        onClick = { loadTodayData() },
                                        modifier = Modifier.height(42.dp),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            !isClockedOut -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PrimaryButton(
                                        text = when {
                                            isAttendanceActionLoading -> "İşleniyor..."
                                            isLoading -> "Durum kontrol ediliyor..."
                                            "Puantaj" in loadErrors -> "Puantaj alınamadı"
                                            hasOpenBreak -> "Molayı Bitir"
                                            else -> "Mola Başlat"
                                        },
                                        onClick = {
                                            if (!isAttendanceActionLoading && !isLoading && "Puantaj" !in loadErrors) {
                                                if (hasOpenBreak) handleEndBreak() else handleStartBreak()
                                            }
                                        },
                                        enabled = !isAttendanceActionLoading && !isLoading && "Puantaj" !in loadErrors,
                                        modifier = Modifier.weight(1f),
                                        height = 42.dp
                                    )
                                    OutlinedButton(
                                        onClick = { if (!isAttendanceActionLoading) handleClockOut() },
                                        modifier = Modifier.weight(1f).height(42.dp),
                                        enabled = !isAttendanceActionLoading && !isLoading && "Puantaj" !in loadErrors,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PillErr.fg)
                                    ) {
                                        Text(text = "Vardiyayı Bitir", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vardiyanız tamamlandı, iyi dinlenmeler!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PillOk.fg
                                    )
                                    OutlinedButton(
                                        onClick = { loadTodayData() },
                                        modifier = Modifier.height(36.dp),
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bugünkü Görevler Başlığı
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Bugünkü Görevler",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$completedCount / $totalCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = "Tümünü Gör",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffGradStart,
                        modifier = Modifier.clickable { onNavigateToTasks() }
                    )
                }
            }

            // Görev Listesi
            if (todayTasks.isEmpty()) {
                item {
                    LukaCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    "Görevler" in loadErrors -> "Görevler yüklenemedi. Tekrar deneyin."
                                    isLoading -> "Görevler yükleniyor..."
                                    else -> "Bugün için atanmış bir görev bulunmuyor."
                                },
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(todayTasks) { task ->
                    com.lukasoft.lukarest.ui.components.TaskListCard(task, onOpen = { onNavigateToTaskComplete(task.id) })
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
}
