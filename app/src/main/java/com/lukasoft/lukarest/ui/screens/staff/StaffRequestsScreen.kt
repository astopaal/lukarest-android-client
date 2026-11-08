package com.lukasoft.lukarest.ui.screens.staff

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.lukasoft.lukarest.data.api.OperationalIssueDetailsDto
import com.lukasoft.lukarest.data.api.StaffRequestListItemDto
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.components.*
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StaffRequestsScreen(
    viewModel: LukaViewModel,
    onNavigateToShiftChange: () -> Unit,
    onNavigateToLeaveChange: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val staffName by AuthStore.staffName.collectAsState()

    var myRequests by remember { mutableStateOf<List<StaffRequestListItemDto>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    var showIssue by remember { mutableStateOf(false) }

    fun refreshRequests() {
        coroutineScope.launch {
            isRefreshing = true
            loadError = false
            try {
                val res = ApiClient.requestsService.getMyRequests()
                if (res.isSuccessful && res.body() != null) {
                    myRequests = res.body()!!
                } else {
                    loadError = true
                }
            } catch (_: Exception) {
                loadError = true
            } finally {
                delay(300)
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshRequests()
    }

    fun handleCancel(requestId: String) {
        coroutineScope.launch {
            try {
                val res = ApiClient.requestsService.cancelMyRequest(requestId)
                if (res.isSuccessful) {
                    LukaFeedback.show("Talep iptal edildi")
                    refreshRequests()
                } else {
                    LukaFeedback.show("İptal edilemedi: ${res.code()}")
                }
            } catch (e: Exception) {
                LukaFeedback.show("Hata: ${e.localizedMessage}")
            }
        }
    }

    if (showIssue) IssueRequestDialog(
        onDismiss = { showIssue = false },
        onSaved = { showIssue = false; refreshRequests() }
    )

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                TopPill(
                    title = "Taleplerim",
                    subtitle = staffName ?: "İzin & Vardiya Talepleri",
                    initials = (staffName?.take(2) ?: "P").uppercase(),
                    gradient = StaffGradient,
                    showAction = false
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshRequests() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Yeni Talep Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryButton(
                        text = "Vardiya Değişimi",
                        onClick = onNavigateToShiftChange,
                        modifier = Modifier.weight(1f),
                        height = 42.dp
                    )
                    PrimaryButton(
                        text = "İzin Talebi",
                        onClick = onNavigateToLeaveChange,
                        modifier = Modifier.weight(1f),
                        height = 42.dp,
                        gradient = BtnGradient
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = { showIssue = true }) { Text("Stok, arıza veya ihtiyaç bildir") }

                // Talep Geçmişi Listesi
                if (myRequests.isEmpty() && !isRefreshing && loadError) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Talepler yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.",
                            color = PillErr.fg,
                            fontSize = 14.sp
                        )
                        TextButton(onClick = { refreshRequests() }) {
                            Text("Tekrar dene")
                        }
                    }
                } else if (myRequests.isEmpty() && !isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz bir talebiniz bulunmuyor",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    if (loadError) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Güncellenemedi. Gösterilen talepler son başarılı yüklemeden.",
                                modifier = Modifier.weight(1f),
                                color = PillWarn.fg,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = { refreshRequests() }) {
                                Text("Tekrar dene")
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(myRequests) { req ->
                            val isPending = req.status.equals("Pending", ignoreCase = true)
                            val statusPillColor = when (req.status.lowercase()) {
                                "pending" -> PillWarn
                                "approved" -> PillOk
                                "rejected" -> PillErr
                                else -> PillNeutral
                            }
                            val statusText = when (req.status.lowercase()) {
                                "pending" -> "Beklemede"
                                "approved" -> "Onaylandı"
                                "rejected" -> "Reddedildi"
                                "cancelled" -> "İptal Edildi"
                                else -> req.status
                            }

                            LukaCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (req.requestType) {
                                            "ShiftHourChange" -> "Vardiya Takas / Saat Talebi"
                                            "WeeklyOffDayChange" -> "Haftalık İzin Değişimi"
                                            "AnnualLeave" -> "Yıllık İzin Talebi"
                                            "ExcusedLeave" -> "Mazeret İzni"
                                            else -> req.requestType
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMain
                                    )
                                    StatusPill(text = statusText, pillColor = statusPillColor)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = req.summaryText.ifBlank { req.reason },
                                    fontSize = 13.sp,
                                    color = TextSoft
                                )

                                if (!req.managerNote.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Yönetici Notu: ${req.managerNote}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StaffGradStart
                                    )
                                }

                                if (isPending) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { handleCancel(req.id) },
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PillErr.fg)
                                        ) {
                                            Text(text = "Talebi İptal Et", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    }
}


@Composable
private fun IssueRequestDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val types = listOf("StockDepletion" to "Stok bitti", "EquipmentFault" to "Arıza", "GeneralNeed" to "İhtiyaç")
    var type by remember { mutableStateOf(types.first().first) }
    var item by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Normal") }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = quantity.trim().replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Operasyon talebi") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                types.forEach { (code, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(type == code, onClick = { type = code })
                        Text(label)
                    }
                }
                OutlinedTextField(item, { item = it }, label = { Text("Ürün veya ekipman") })
                OutlinedTextField(urgency, { urgency = it }, label = { Text("Aciliyet") })
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Miktar (isteğe bağlı)") })
                OutlinedTextField(reason, { reason = it }, label = { Text("Açıklama") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && item.trim().length in 1..200 && urgency.trim().length in 1..50 &&
                    reason.trim().length in 5..500 && (quantity.isBlank() || amount?.let { it.isFinite() && it > 0 } == true),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val result = ApiClient.requestsService.createMyRequest(
                                CreateStaffRequestCommand(type, reason.trim(),
                                    issue = OperationalIssueDetailsDto(item.trim(), urgency.trim(), amount))
                            )
                            if (result.isSuccessful) onSaved()
                            else error = "Talep kaydedilemedi (${result.code()})."
                        } catch (_: java.io.IOException) {
                            error = "Sunucuya ulaşılamadı."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) { Text("Gönder") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Vazgeç") } }
    )
}
