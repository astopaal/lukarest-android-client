package com.lukasoft.lukarest.ui.screens.staff

import com.lukasoft.lukarest.ui.components.LukaFeedback
import com.lukasoft.lukarest.ui.components.LukaTimeField

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.lukasoft.lukarest.data.api.CreateStaffRequestCommand
import com.lukasoft.lukarest.data.api.ShiftChangeRequestDetailsDto
import com.lukasoft.lukarest.data.model.ShiftReason
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch

@Composable
fun StaffShiftChangeScreen(
    viewModel: LukaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentMonday = remember { DateUtils.getCurrentWeekMonday() }
    val weekDays = remember(currentMonday) { DateUtils.getWeekDays(currentMonday) }
    val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    val daysFull = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

    var selectedDay by remember { mutableIntStateOf(0) }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("18:00") }
    var reason by remember { mutableStateOf(ShiftReason.APPOINTMENT) }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val handleSubmit = {
        if (startTime.isBlank() || endTime.isBlank()) {
            errorMessage = "Lütfen başlangıç ve bitiş saatini girin."
        } else {
            errorMessage = null
            coroutineScope.launch {
                isSubmitting = true
                try {
                    val targetDate = weekDays.getOrNull(selectedDay)?.isoDate ?: DateUtils.getTodayIso()
                    val schedule = ApiClient.shiftsService.getMyShifts(currentMonday)
                    if (!schedule.isSuccessful) {
                        errorMessage = "Güncel vardiya alınamadı. Tekrar deneyin."
                        return@launch
                    }
                    val currentShift = schedule.body()?.days?.firstOrNull { it.date == targetDate && !it.isOffDay }
                    if (currentShift?.startTime == null || currentShift.endTime == null) {
                        errorMessage = "Seçilen günde değiştirilebilecek bir vardiya yok."
                        return@launch
                    }
                    val reasonDesc = if (note.trim().length >= 5) {
                        note.trim()
                    } else {
                        "Vardiya saat değişikliği talebi (${reason.name})"
                    }

                    val res = ApiClient.requestsService.createMyRequest(
                        CreateStaffRequestCommand(
                            requestType = "ShiftHourChange",
                            priority = priority,
                            reason = reasonDesc,
                            shiftChange = ShiftChangeRequestDetailsDto(
                                targetDate = targetDate,
                                currentStartTime = currentShift.startTime,
                                currentEndTime = currentShift.endTime,
                                proposedStartTime = startTime.trim(),
                                proposedEndTime = endTime.trim()
                            )
                        )
                    )
                    if (res.isSuccessful) {
                        LukaFeedback.show("Talebiniz yöneticiye iletildi")
                        onNavigateBack()
                    } else {
                        errorMessage = com.lukasoft.lukarest.data.api.apiErrorMessage(res.errorBody()?.string(), "Talep iletilemedi: ${res.code()}")
                    }
                } catch (e: Exception) {
                    errorMessage = "Hata: ${e.localizedMessage}"
                } finally {
                    isSubmitting = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Vardiya Değişikliği",
                    subtitle = "Yeni Saat Talep Et",
                    initials = "AY",
                    gradient = StaffGradient,
                    actionIcon = Icons.Default.Close,
                    onActionClick = onNavigateBack
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Değiştirilecek Gün Seçimi
            com.lukasoft.lukarest.ui.components.PriorityField(priority, enabled = !isSubmitting, onChange = { priority = it })
            Text(text = "Değiştirilecek gün", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { idx, day ->
                    val isSelected = selectedDay == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StaffGradStart else White)
                            .clickable { selectedDay = idx }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = day,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) White else TextMuted
                        )
                    }
                }
            }

            // Seçili Gün Kartı
            LukaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Seçili Gün", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(text = daysFull[selectedDay], fontSize = 13.sp, color = TextMuted)
                    }
                    StatusPill(text = "Mevcut Vardiya", pillColor = PillBlue)
                }
            }

            // Yeni Saat Aralığı
            Text(text = "Talep edilen yeni saat aralığı", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LukaCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                    LukaTimeField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = "Başlangıç",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LukaCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
                    LukaTimeField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = "Bitiş",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Neden Seçimi
            Text(text = "Değişiklik gerekçesi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    ShiftReason.APPOINTMENT to "Randevu",
                    ShiftReason.HEALTH to "Sağlık",
                    ShiftReason.FAMILY to "Aile",
                    ShiftReason.OTHER to "Diğer"
                ).forEach { (res, label) ->
                    val isSelected = reason == res
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) StaffGradStart else White)
                            .clickable { reason = res },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) White else TextMain
                        )
                    }
                }
            }

            // Açıklama Notu
            Text(text = "Yöneticiye not", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("İsteğe bağlı açıklama...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                minLines = 2
            )

            if (errorMessage != null) {
                Text(text = errorMessage ?: "", color = PillErr.fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryButton(
                text = if (isSubmitting) "Gönderiliyor..." else "Talebi Gönder",
                loading = isSubmitting,
                onClick = { if (!isSubmitting) handleSubmit() },
                gradient = StaffGradient
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateBack() }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Vazgeç", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            }
        }
    }
}

