package com.lukasoft.lukarest.ui.screens.staff

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.lukasoft.lukarest.data.api.LeaveRequestDetailsDto
import com.lukasoft.lukarest.data.model.LeaveType
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch

@Composable
fun StaffLeaveChangeScreen(
    viewModel: LukaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentMonday = remember { DateUtils.getCurrentWeekMonday() }
    val weekDays = remember(currentMonday) { DateUtils.getWeekDays(currentMonday) }
    val daysFull = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

    var leaveType by remember { mutableStateOf(LeaveType.DAY_CHANGE) }
    var dayIndex by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(1) }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val handleSubmit = {
        coroutineScope.launch {
            isSubmitting = true
            errorMessage = null
            try {
                val proposedDate = weekDays.getOrNull(dayIndex)?.isoDate ?: DateUtils.getTodayIso()
                val schedule = ApiClient.shiftsService.getMyShifts(currentMonday)
                if (!schedule.isSuccessful) { errorMessage = "Güncel izin planı alınamadı. Tekrar deneyin."; return@launch }
                val currentOffDate = if (leaveType == LeaveType.DAY_CHANGE) schedule.body()?.days?.firstOrNull { it.isOffDay }?.date else proposedDate
                if (currentOffDate == null) { errorMessage = "Bu hafta değiştirilebilecek bir izin günü yok."; return@launch }

                val (reqType, category) = when (leaveType) {
                    LeaveType.DAY_CHANGE -> "WeeklyOffDayChange" to "Haftalık İzin"
                    LeaveType.ANNUAL -> "AnnualLeave" to "Yıllık İzin"
                    LeaveType.EXCUSE -> "ExcusedLeave" to "Mazeret İzni"
                    LeaveType.UNPAID -> "ExcusedLeave" to "Ücretsiz İzin"
                }

                val reasonText = if (note.trim().length >= 5) {
                    note.trim()
                } else {
                    "$category talebi (${duration} gün)"
                }

                val res = ApiClient.requestsService.createMyRequest(
                    CreateStaffRequestCommand(
                        requestType = reqType,
                        priority = priority,
                        reason = reasonText,
                        leave = LeaveRequestDetailsDto(
                            currentOffDate = currentOffDate,
                            proposedOffDate = proposedDate,
                            durationDays = duration,
                            leaveCategory = category
                        )
                    )
                )

                if (res.isSuccessful) {
                    LukaFeedback.show("İzin talebiniz iletildi")
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

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "İzin Değişikliği",
                    subtitle = "İzin Talep Formu",
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
            // İzin Talebi Türü
            Text(text = "İzin talebi türü", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    LeaveType.DAY_CHANGE to "Gün Değişimi",
                    LeaveType.ANNUAL to "Yıllık İzin",
                    LeaveType.EXCUSE to "Mazeret",
                    LeaveType.UNPAID to "Ücretsiz"
                ).forEach { (type, label) ->
                    val isSelected = leaveType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) StaffGradStart else White)
                            .clickable { leaveType = type },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) White else TextMain
                        )
                    }
                }
            }

            // Stepper 1: Gün Seçimi
            LukaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "İzin Günü", fontSize = 12.sp, color = TextMuted)
                        Text(text = daysFull[dayIndex], fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0EEFF))
                                .clickable { dayIndex = (dayIndex - 1 + 7) % 7 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StaffGradStart)
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0EEFF))
                                .clickable { dayIndex = (dayIndex + 1) % 7 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StaffGradStart)
                        }
                    }
                }
            }

            // Stepper 2: Toplam Süre
            LukaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Toplam Süre", fontSize = 12.sp, color = TextMuted)
                        Text(text = "$duration gün", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0EEFF))
                                .clickable { if (duration > 1) duration-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StaffGradStart)
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0EEFF))
                                .clickable { duration++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StaffGradStart)
                        }
                    }
                }
            }

            // Yöneticiye Not
            com.lukasoft.lukarest.ui.components.PriorityField(priority, enabled = !isSubmitting, onChange = { priority = it })
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
                Text(
                    text = errorMessage!!,
                    color = PillErr.fg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryButton(
                text = if (isSubmitting) "Gönderiliyor..." else "İzin Talebini Gönder",
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

