package com.lukasoft.lukarest.ui.screens.staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.MyShiftDayDto
import com.lukasoft.lukarest.data.api.MyShiftsResponseDto
import com.lukasoft.lukarest.data.api.ShiftsService
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.*
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

fun plannedShiftMinutes(start: String?, end: String?): Int? {
    fun minutes(value: String?): Int? {
        val parts = value?.split(':') ?: return null
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val mins = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return if (hours in 0..23 && mins in 0..59) hours * 60 + mins else null
    }
    val startMinute = minutes(start) ?: return null
    val endMinute = minutes(end) ?: return null
    return (endMinute - startMinute).let { if (it < 0) it + 24 * 60 else it }
}

private fun shiftDuration(day: MyShiftDayDto): String {
    val minutes = plannedShiftMinutes(day.startTime, day.endTime) ?: return ""
    return if (minutes % 60 == 0) "${minutes / 60} saat" else "${minutes / 60} saat ${minutes % 60} dk"
}

@Composable
fun StaffShiftsScreen(viewModel: LukaViewModel, onNavigateToShiftChange: () -> Unit, onNavigateToLeaveChange: () -> Unit, service: ShiftsService = ApiClient.shiftsService) {
    val currentUser by viewModel.currentUser.collectAsState()
    val today = DateUtils.getTurkeyTodayIso()
    val currentWeek = DateUtils.getCurrentWeekMonday()
    var week by remember { mutableStateOf(currentWeek) }
    val days = remember(week) { DateUtils.getWeekDays(week) }
    var selectedDate by remember(week) { mutableStateOf(days.firstOrNull { it.isoDate == today }?.isoDate ?: week) }
    var response by remember { mutableStateOf<MyShiftsResponseDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(week, refresh) {
        loading = true
        error = null
        response = null
        try {
            val result = service.getMyShifts(week)
            if (result.isSuccessful && result.body() != null) response = result.body()
            else error = "Vardiyalar yüklenemedi (${result.code()})."
        } catch (e: CancellationException) { throw e
        } catch (_: IOException) { error = "Sunucuya ulaşılamadı. Bağlantınızı kontrol edin."
        } finally { loading = false }
    }
    val shifts = response?.days.orEmpty()
    val selected = shifts.firstOrNull { it.date == selectedDate }
    val assigned = shifts.filter { !it.isOffDay && it.startTime != null && it.endTime != null }
    val offDays = shifts.count { it.isOffDay }
    val plannedMinutes = assigned.sumOf { plannedShiftMinutes(it.startTime, it.endTime) ?: 0 }
    val completed = response?.completedHours ?: 0.0
    val remaining = response?.remainingHours ?: 0.0
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { Box(Modifier.padding(16.dp)) {
        TopPill(title = "Vardiyalarım", subtitle = currentUser?.fullName ?: AuthStore.staffName.value ?: "Personel", initials = "P", gradient = StaffGradient, showAction = false)
    } }, containerColor = BgScreenTop) { padding ->
        PullToRefreshBox(isRefreshing = loading, onRefresh = { refresh++ }, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { week = DateUtils.addWeeks(week, -1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Önceki hafta", tint = StaffGradStart) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(DateUtils.weekLabel(week), style = MaterialTheme.typography.titleMedium, color = TextMain)
                            Text("${days.first().displayDate} - ${days.last().displayDate}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        IconButton(onClick = { week = DateUtils.addWeeks(week, 1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Sonraki hafta", tint = StaffGradStart) }
                    }
                    if (week != currentWeek) TextButton(onClick = { week = currentWeek }, modifier = Modifier.fillMaxWidth()) { Text("Bu haftaya dön") }
                }
                when {
                    loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    error != null -> item { LukaCard {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { refresh++ }) { Text("Tekrar dene") }
                    } }
                    else -> {
                        item { LukaCard {
                            Text(if (selectedDate == today) "Bugünün vardiyası" else "Seçili günün vardiyası", style = MaterialTheme.typography.labelLarge, color = StaffGradStart)
                            Spacer(Modifier.height(6.dp))
                            val day = days.first { it.isoDate == selectedDate }
                            Text("${day.displayDate} · ${day.dayName}", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            when {
                                selected?.isOffDay == true -> {
                                    Text("İzin günü", style = MaterialTheme.typography.headlineSmall, color = TextMain)
                                    Text("Bu gün için çalışma planlanmadı.", color = TextMuted)
                                }
                                selected?.startTime != null -> {
                                    Text("${selected.startTime.take(5)} - ${selected.endTime?.take(5)}", style = MaterialTheme.typography.headlineMedium, color = TextMain, fontWeight = FontWeight.Bold)
                                    Text("${shiftDuration(selected)} · ${selected.shortCode ?: "Planlı vardiya"}", color = TextMuted)
                                    if ((plannedShiftMinutes(selected.startTime, selected.endTime) ?: 0) > 0 && selected.endTime.orEmpty() < selected.startTime)
                                        Text("Vardiya ertesi gün biter.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                else -> Text("Bu gün için henüz vardiya atanmadı.", style = MaterialTheme.typography.titleMedium, color = TextMain)
                            }
                            if (selected?.isOverridden == true) { Spacer(Modifier.height(8.dp)); StatusPill("Plan güncellendi", pillColor = PillWarn) }
                        } }
                        item { LukaCard {
                            Text("Haftanın özeti", style = MaterialTheme.typography.titleMedium, color = TextMain)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill("${assigned.size} çalışma günü")
                                StatusPill("$offDays izin günü", pillColor = PillNeutral)
                                StatusPill("${String.format(Locale.forLanguageTag("tr-TR"), "%.1f", plannedMinutes / 60.0)} saat planlı", pillColor = PillSky)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Geçmiş günlerin planı: ${String.format(Locale.forLanguageTag("tr-TR"), "%.1f", completed)} saat\nBugün ve sonrası: ${String.format(Locale.forLanguageTag("tr-TR"), "%.1f", remaining)} saat", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        } }
                        item {
                            Text("Günlük plan", style = MaterialTheme.typography.titleMedium, color = TextMain)
                            Text("Vardiya ayrıntısını görmek için bir gün seç.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Spacer(Modifier.height(10.dp))
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val dayWidth = ((maxWidth - 24.dp) / 7).coerceAtLeast(48.dp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                days.forEach { day ->
                                    val shift = shifts.firstOrNull { it.date == day.isoDate }
                                    val active = day.isoDate == selectedDate
                                    Surface(onClick = {
                                        selectedDate = day.isoDate
                                        scope.launch { listState.animateScrollToItem(1) }
                                    }, modifier = Modifier.width(dayWidth), shape = MaterialTheme.shapes.medium,
                                        color = if (active) StaffGradStart else White,
                                        border = BorderStroke(if (day.isoDate == today) 2.dp else 1.dp, if (day.isoDate == today) StaffGradStart else BorderSoft)) {
                                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(day.dayName, style = MaterialTheme.typography.labelSmall, color = if (active) White else TextMuted)
                                            Text(day.dayNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (active) White else TextMain)
                                            Text(when { shift?.isOffDay == true -> "İzin"; shift?.startTime != null -> "Mesai"; else -> "Boş" },
                                                style = MaterialTheme.typography.labelSmall, color = if (active) White else TextMuted)
                                        }
                                    }
                                }
                            }
                            }
                        }
                        if (assigned.isEmpty() && offDays == 0) item { Text("Bu haftanın planı henüz yayınlanmamış olabilir. Güncel bilgi için yöneticinle iletişime geç.", color = TextMuted) }
                        item { LukaCard {
                            Text("Plan değişikliği mi gerekiyor?", style = MaterialTheme.typography.titleMedium, color = TextMain)
                            Text("Talebin yönetici onayından sonra planına yansır.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = onNavigateToShiftChange, modifier = Modifier.fillMaxWidth()) { Text("Vardiya saati değişikliği iste") }
                            OutlinedButton(onClick = onNavigateToLeaveChange, modifier = Modifier.fillMaxWidth()) { Text("İzin talebi oluştur") }
                        } }
                    }
                }
            }
        }
    }
}
