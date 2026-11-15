package com.lukasoft.lukarest.ui.screens.staff

import com.lukasoft.lukarest.ui.components.LukaBottomSheet

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffRequestsScreen(
    viewModel: LukaViewModel,
    onNavigateToShifts: () -> Unit,
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
    var showRequestTypes by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    val visibleRequests = myRequests.filter { statusFilter == null || it.status.equals(statusFilter, ignoreCase = true) }
    if (showRequestTypes) LukaBottomSheet(onDismissRequest = { showRequestTypes = false }, containerColor = BgScreenTop) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ne talep etmek istiyorsunuz?", style = MaterialTheme.typography.titleLarge, color = TextMain)
            listOf(
                Triple("Vardiya saatini değiştir", "Çalışma saatlerin için yeni bir aralık öner.", onNavigateToShiftChange),
                Triple("İzin talebi oluştur", "Haftalık, yıllık veya mazeret izni iste.", onNavigateToLeaveChange),
                Triple("İhtiyaç veya sorun bildir", "Stok, ekipman arızası ya da diğer ihtiyaçları ilet.", { showIssue = true })
            ).forEach { (title, description, action) ->
                OutlinedCard(onClick = { showRequestTypes = false; action() }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = TextMain)
                        Text(description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    var approvedRequest by remember { mutableStateOf<StaffRequestListItemDto?>(null) }
    val preferences = remember { context.getSharedPreferences("request_approvals", android.content.Context.MODE_PRIVATE) }
    fun dismissApproval() {
        approvedRequest?.let { preferences.edit().putBoolean("${AuthStore.userId.value}:${it.id}", true).apply() }
        approvedRequest = null
    }

    fun refreshRequests() {
        coroutineScope.launch {
            isRefreshing = true
            loadError = false
            try {
                val res = ApiClient.requestsService.getMyRequests()
                if (res.isSuccessful && res.body() != null) {
                    myRequests = res.body()!!
                    approvedRequest = myRequests.firstOrNull {
                        it.staffUserId == AuthStore.userId.value && it.status == "Approved" &&
                            it.requestType in listOf("ShiftHourChange", "WeeklyOffDayChange", "AnnualLeave", "ExcusedLeave") &&
                            !preferences.getBoolean("${AuthStore.userId.value}:${it.id}", false)
                    }
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
    approvedRequest?.let {
        AlertDialog(onDismissRequest = { dismissApproval() }, title = { Text("Talebin onaylandı") },
            text = { Text("İzin veya vardiya değişikliği planına işlendi. Güncel vardiyalarını açmak ister misin?") },
            confirmButton = { TextButton(onClick = { dismissApproval(); onNavigateToShifts() }) { Text("Vardiyalarımı aç") } },
            dismissButton = { TextButton(onClick = { dismissApproval() }) { Text("Daha sonra") } })
    }

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
                PrimaryButton(text = "Yeni talep oluştur", onClick = { showRequestTypes = true })
                Spacer(Modifier.height(12.dp))
                Text("Talep geçmişi", style = MaterialTheme.typography.titleMedium, color = TextMain)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Tümü", "Pending" to "Bekleyen", "Approved" to "Onaylanan", "Rejected" to "Reddedilen", "Cancelled" to "İptal").forEach { (code, label) ->
                        val count = myRequests.count { code == null || it.status.equals(code, ignoreCase = true) }
                        FilterChip(selected = statusFilter == code, onClick = { statusFilter = code }, label = { Text("$label ($count)", color = TextMain) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PillBlue.bg))
                    }
                }
                if (visibleRequests.isEmpty() && myRequests.isNotEmpty()) Text("Bu durumda bir talep yok.", color = TextMuted, modifier = Modifier.padding(vertical = 16.dp))

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
                        items(visibleRequests) { req ->
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

                                Text("Öncelik: ${priorityLabels[req.priority] ?: req.priority}", color = TextMain)
                                if (req.staffUserId != AuthStore.userId.value) Text("${req.staffFullName} seni bu talepte etiketledi", color = BossGradStart)
                                if (isPending && req.staffUserId == AuthStore.userId.value) {
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueRequestDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val types = listOf("StockDepletion" to "Stok bitti", "EquipmentFault" to "Arıza", "GeneralNeed" to "İhtiyaç")
    var type by remember { mutableStateOf(types.first().first) }
    var item by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Normal") }
    var priority by remember { mutableStateOf("Medium") }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = quantity.trim().replace(',', '.').toDoubleOrNull()
    val canSend = !busy && item.trim().length in 1..200 && urgency.trim().length in 1..50 &&
        reason.trim().length in 5..500 && (quantity.isBlank() || amount?.let { it.isFinite() && it > 0 } == true)
    LukaBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BgScreenTop
    ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Operasyon talebi", style = MaterialTheme.typography.headlineSmall, color = BossGradStart)
                Text("Stok, ekipman veya ihtiyaç bilgisini şubenle paylaş.", color = TextMain)
                types.forEach { (code, label) ->
                    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = type == code, enabled = !busy, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = { type = code }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(type == code, onClick = null, enabled = !busy)
                        Text(label)
                    }
                }
                OutlinedTextField(item, { item = it.take(200) }, label = { Text("Ürün veya ekipman") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Miktar (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !busy,
                    supportingText = { Text("${reason.length}/500 · En az 5 karakter") })
                PriorityField(priority, enabled = !busy, onChange = { priority = it; urgency = priorityLabels[it] ?: "Normal" })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = canSend,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val result = ApiClient.requestsService.createMyRequest(
                                CreateStaffRequestCommand(type, reason.trim(),
                                    issue = OperationalIssueDetailsDto(item.trim(), urgency.trim(), amount), priority = priority)
                            )
                            if (result.isSuccessful) onSaved()
                            else error = com.lukasoft.lukarest.data.api.apiErrorMessage(result.errorBody()?.string(), "Talep kaydedilemedi (${result.code()}).")
                        } catch (_: java.io.IOException) {
                            error = "Sunucuya ulaşılamadı."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) { Text(if (busy) "Gönderiliyor..." else "Talebi gönder", color = if (canSend) White else TextMuted) }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Vazgeç") }
        }
    }
}
