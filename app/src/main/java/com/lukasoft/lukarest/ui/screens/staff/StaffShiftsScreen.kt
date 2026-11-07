package com.lukasoft.lukarest.ui.screens.staff

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.MyShiftDayDto
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.LukaProgressBar
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch

@Composable
fun StaffShiftsScreen(
    viewModel: LukaViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    var weekStartDate by remember { mutableStateOf(DateUtils.getCurrentWeekMonday()) }
    val weekDays = remember(weekStartDate) { DateUtils.getWeekDays(weekStartDate) }

    var isLoading by remember { mutableStateOf(false) }
    var myShifts by remember { mutableStateOf<List<MyShiftDayDto>>(emptyList()) }
    var completedHours by remember { mutableDoubleStateOf(0.0) }
    var remainingHours by remember { mutableDoubleStateOf(0.0) }

    fun loadMyShifts() {
        coroutineScope.launch {
            isLoading = true
            try {
                val sRes = ApiClient.shiftsService.getMyShifts(weekStartDate)
                if (sRes.isSuccessful && sRes.body() != null) {
                    val body = sRes.body()!!
                    myShifts = body.days
                    completedHours = body.completedHours
                    remainingHours = body.remainingHours
                } else {
                    LukaFeedback.show("Vardiyalar alınamadı: ${sRes.code()}")
                }
            } catch (e: Exception) {
                LukaFeedback.show("Hata: ${e.localizedMessage ?: "Vardiyalar alınamadı"}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(weekStartDate) {
        loadMyShifts()
    }

    val assignedDaysCount = myShifts.count { !it.isOffDay && it.startTime != null }
    val offDaysCount = myShifts.count { it.isOffDay }
    val totalPlannedHours = completedHours + remainingHours
    val progress = if (totalPlannedHours > 0) (completedHours / totalPlannedHours).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                TopPill(
                    title = "Vardiyalarım",
                    subtitle = currentUser?.fullName ?: AuthStore.staffName.value ?: "Personel",
                    initials = "P",
                    gradient = StaffGradient,
                    showAction = false
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { loadMyShifts() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // ── HAFTA SEÇİCİ BARI ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { weekStartDate = DateUtils.addWeeks(weekStartDate, -1) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(White, CircleShape)
                                .border(1.dp, BorderSoft, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Önceki Hafta",
                                tint = StaffGradStart,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = DateUtils.weekLabel(weekStartDate),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaffGradStart
                        )

                        IconButton(
                            onClick = { weekStartDate = DateUtils.addWeeks(weekStartDate, 1) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(White, CircleShape)
                                .border(1.dp, BorderSoft, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Sonraki Hafta",
                                tint = StaffGradStart,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { loadMyShifts() },
                        modifier = Modifier
                            .size(34.dp)
                            .background(White, CircleShape)
                            .border(1.dp, BorderSoft, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = StaffGradStart,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── HAFTALIK ÖZET KARTI ──
            item {
                LukaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Haftalık Çalışma Durumu",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Text(
                            text = "${completedHours.toInt()} / ${totalPlannedHours.toInt()} Saat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaffGradStart
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LukaProgressBar(progress = progress)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Bu hafta $assignedDaysCount gün çalışma, $offDaysCount gün izin planlandı.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StaffGradStart)
                    }
                }
            } else {
                // ── 7 GÜNLÜK ÇİZELGE ──
                items(weekDays) { day ->
                    val shiftDay = myShifts.find { it.date == day.isoDate }
                    val isOff = shiftDay?.isOffDay == true
                    val hasShift = shiftDay != null && !isOff && shiftDay.startTime != null

                    val tmplColor = when {
                        isOff -> Pair(Color(0xFFFFECEC), Color(0xFFDC2626))
                        hasShift -> {
                            val parsed = try {
                                Color(android.graphics.Color.parseColor(shiftDay.colorHex ?: "#0284C7"))
                            } catch (e: Exception) {
                                Color(0xFF0284C7)
                            }
                            Pair(parsed.copy(alpha = 0.12f), parsed)
                        }
                        else -> Pair(Color(0xFFF3F4F6), Color(0xFF9CA3AF))
                    }

                    val fullDayName = when (day.dayName) {
                        "Pzt" -> "Pazartesi"
                        "Sal" -> "Salı"
                        "Çar" -> "Çarşamba"
                        "Per" -> "Perşembe"
                        "Cum" -> "Cuma"
                        "Cmt" -> "Cumartesi"
                        "Paz" -> "Pazar"
                        else -> day.dayName
                    }

                    LukaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = fullDayName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMain
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = when {
                                        isOff -> "İzin Günü (Haftalık İzin)"
                                        hasShift -> "Vardiya (${shiftDay?.shortCode ?: "Planlı"})"
                                        else -> "Planlanan vardiya bulunmuyor"
                                    },
                                    fontSize = 13.sp,
                                    color = if (isOff || hasShift) TextMain else TextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tmplColor.first)
                                    .border(1.dp, tmplColor.second.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isOff -> "İzinli"
                                        hasShift -> "${shiftDay?.startTime?.take(5)} - ${shiftDay?.endTime?.take(5)}"
                                        else -> "Atama Yok"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tmplColor.second
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
}
