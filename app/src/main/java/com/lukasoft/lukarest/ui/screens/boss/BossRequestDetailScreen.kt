package com.lukasoft.lukarest.ui.screens.boss

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import com.lukasoft.lukarest.data.api.DecideStaffRequestBody
import com.lukasoft.lukarest.data.api.StaffRequestDetailDto
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch

@Composable
fun BossRequestDetailScreen(
    requestId: String,
    viewModel: LukaViewModel,
    onNavigateToShifts: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var detail by remember { mutableStateOf<StaffRequestDetailDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var managerNote by remember { mutableStateOf("") }
    var showApprovedPrompt by remember { mutableStateOf(false) }

    fun loadDetail() {
        coroutineScope.launch {
            isLoading = true
            try {
                val res = ApiClient.requestsService.getRequestDetail(requestId)
                if (res.isSuccessful && res.body() != null) {
                    detail = res.body()
                    managerNote = res.body()?.managerNote ?: ""
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(requestId) {
        loadDetail()
    }

    val handleDecision: (Boolean) -> Unit = { isApproved ->
        coroutineScope.launch {
            isSubmitting = true
            try {
                val res = ApiClient.requestsService.decideRequest(
                    id = requestId,
                    body = DecideStaffRequestBody(
                        isApproved = isApproved,
                        managerNote = managerNote.ifBlank { null }
                    )
                )
                if (res.isSuccessful) {
                    detail = res.body() ?: detail
                    val msg = if (isApproved) "Talep onaylandı ve vardiyaya işlendi" else "Talep reddedildi"
                    LukaFeedback.show(msg)
                    if (isApproved && (detail?.shiftChange != null || detail?.leave != null)) showApprovedPrompt = true
                    else onNavigateBack()
                } else {
                    LukaFeedback.show("İşlem başarısız: ${res.code()}")
                }
            } catch (e: Exception) {
                LukaFeedback.show("Hata: ${e.localizedMessage}")
            } finally {
                isSubmitting = false
            }
        }
    }

    if (showApprovedPrompt) AlertDialog(
        onDismissRequest = { showApprovedPrompt = false },
        title = { Text("Vardiya planı güncellendi") },
        text = { Text(if (detail?.coverage?.hasGap == true) "Personel boşluğu oluştu: ${detail?.coverage?.message}\n\nVardiya planını açmak ister misiniz?" else "Güncellenen vardiya planını açmak ister misiniz?") },
        confirmButton = { TextButton(onClick = { showApprovedPrompt = false; onNavigateToShifts() }) { Text("Vardiya planını aç") } },
        dismissButton = { TextButton(onClick = { showApprovedPrompt = false }) { Text("Bu ekranda kal") } }
    )
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Talep İnceleme",
                    subtitle = detail?.staffFullName ?: "Personel Talebi",
                    initials = detail?.staffInitials ?: "P",
                    gradient = BossGradient,
                    actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onActionClick = onNavigateBack
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StaffGradStart)
            }
        } else {
            val req = detail
            val isPending = req?.status.equals("Pending", ignoreCase = true)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Personel Başlık Kartı
                LukaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC9DEFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = req?.staffInitials ?: "P",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF12306B)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = req?.staffFullName ?: "Personel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
                            Text("Öncelik: ${com.lukasoft.lukarest.ui.components.priorityLabels[req?.priority] ?: req?.priority.orEmpty()}", fontSize = 12.sp, color = TextMain)
                            Text(
                                text = when (req?.requestType) {
                                    "ShiftHourChange" -> "Vardiya Saati Değişikliği"
                                    "WeeklyOffDayChange" -> "Haftalık İzin Değişimi"
                                    "AnnualLeave" -> "Yıllık İzin Talebi"
                                    "ExcusedLeave" -> "Mazeret İzni"
                                    "StockDepletion" -> "Stok Tükenme Bildirimi"
                                    "EquipmentFault" -> "Ekipman Arızası"
                                    else -> req?.requestType ?: "Talep"
                                },
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        val pillColor = when (req?.status?.lowercase()) {
                            "approved" -> PillOk
                            "rejected" -> PillErr
                            "pending" -> PillWarn
                            else -> PillNeutral
                        }
                        val pillText = when (req?.status?.lowercase()) {
                            "approved" -> "Onaylandı"
                            "rejected" -> "Reddedildi"
                            "pending" -> "İnceleme Bekliyor"
                            "cancelled" -> "İptal Edildi"
                            else -> req?.status ?: "—"
                        }
                        StatusPill(
                            text = pillText,
                            pillColor = pillColor
                        )
                    }
                }

                // Kapsama Etkisi (Coverage Impact) Kartı (Backend'den hesaplanıp döner)
                if (req?.coverage != null) {
                    LukaCard(backgroundColor = if (req.coverage.hasGap) PillErr.bg else PillBlue.bg) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = StaffGradStart,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (req.coverage.hasGap) "DİKKAT: VARDİYADA PERSONEL BOŞLUĞU" else "Vardiya kapsamı",
                                fontWeight = FontWeight.Bold,
                                color = if (req.coverage.hasGap) PillErr.fg else BossGradStart,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = req.coverage.message,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        )
                        if (req.coverage.staffNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Şubede görevli diğer personel: ${req.coverage.staffNames.joinToString(", ")}",
                                fontSize = 12.sp,
                                color = TextSoft
                            )
                        }
                    }
                }

                // Vardiya veya İzin Detayı Kartı
                if (req?.shiftChange != null) {
                    val sc = req.shiftChange
                    LukaCard {
                        Text(text = "Hedef Tarih: ${sc.targetDate}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Mevcut Vardiya", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(
                            text = "${sc.currentStartTime} — ${sc.currentEndTime}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaffGradStart
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Talep Edilen Yeni Saat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BossGradStart)
                        Text(
                            text = "${sc.proposedStartTime} — ${sc.proposedEndTime}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0C326B)
                        )

                        if (req.reason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Gerekçe: ${req.reason}", fontSize = 13.sp, color = TextSoft)
                        }
                    }
                } else if (req?.leave != null) {
                    val lv = req.leave
                    LukaCard {
                        Text(text = "Talep Edilen İzin", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(
                            text = "${lv.durationDays} Gün (${lv.leaveCategory})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StaffGradStart
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Mevcut İzin: ${lv.currentOffDate} → İstenen: ${lv.proposedOffDate}",
                            fontSize = 14.sp,
                            color = TextSoft
                        )

                        if (req.reason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Gerekçe: ${req.reason}", fontSize = 13.sp, color = TextSoft)
                        }
                    }
                } else if (req?.issue != null) {
                    val isDetail = req.issue
                    LukaCard {
                        Text(text = "Operasyonel Bildirim", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(
                            text = "${isDetail.itemOrEquipmentName} (${isDetail.urgencyLevel})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PillErr.fg
                        )
                        if (isDetail.quantity != null) {
                            Text(text = "Miktar: ${isDetail.quantity}", fontSize = 13.sp, color = TextSoft)
                        }
                        if (req.reason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Açıklama: ${req.reason}", fontSize = 13.sp, color = TextSoft)
                        }
                    }
                }

                // Yönetici Notu Girişi
                LukaCard {
                    Text(text = "Yönetici Notu", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = managerNote,
                        onValueChange = { managerNote = it },
                        placeholder = { Text("Personel için onay/ret gerekçesi girin...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = isPending && !isSubmitting,
                        minLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Karar Butonları (Yalnızca beklemedeyse aktiftir)
                if (isPending) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryButton(
                            text = if (isSubmitting) "İşleniyor..." else "Onayla",
                            onClick = { if (!isSubmitting) handleDecision(true) },
                            gradient = StaffGradient,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { if (!isSubmitting) handleDecision(false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isSubmitting
                        ) {
                            Text(text = "Reddet", color = PillErr.fg, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE5E7EB))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bu talep daha önce karara bağlanmıştır.",
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}


