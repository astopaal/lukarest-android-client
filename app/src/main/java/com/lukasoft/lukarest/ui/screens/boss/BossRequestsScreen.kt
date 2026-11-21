package com.lukasoft.lukarest.ui.screens.boss
import com.lukasoft.lukarest.data.store.AuthStore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.lukasoft.lukarest.data.api.StaffRequestListItemDto
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BossRequestsScreen(
    viewModel: LukaViewModel,
    onNavigateToRequestDetail: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenNotes: () -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isOwner by AuthStore.globalRole.collectAsState()
    var serverRequests by remember { mutableStateOf<List<StaffRequestListItemDto>>(emptyList()) }
    var filter by remember { mutableStateOf("all") } // "all", "pending", "approved", "rejected"
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshRequests() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val res = ApiClient.requestsService.getBranchRequests()
                if (res.isSuccessful && res.body() != null) {
                    serverRequests = res.body()!!
                }
            } catch (_: Exception) {
            } finally {
                delay(300)
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshRequests()
    }

    val pendingCount = serverRequests.count { it.status.equals("Pending", ignoreCase = true) }

    val filtered = serverRequests.filter { req ->
        when (filter) {
            "pending" -> req.status.equals("Pending", ignoreCase = true)
            "approved" -> req.status.equals("Approved", ignoreCase = true)
            "rejected" -> req.status.equals("Rejected", ignoreCase = true)
            else -> true
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
                    title = "Tüm Talepler",
                    subtitle = "Vardiya, izin ve operasyon",
                    initials = "P",
                    gradient = BossGradient,
                    actionIcon = Icons.Default.Menu,
                    actionDescription = "Yönetim menüsünü aç",
                    onActionClick = { menuOpen = true }
                )
                Box(Modifier.align(Alignment.BottomEnd)) {
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = White) {
                        DropdownMenuItem(text = { Text("Bildirimler") }, onClick = { menuOpen = false; onOpenNotifications() })
                        DropdownMenuItem(text = { Text("Operasyon defteri") }, onClick = { menuOpen = false; onOpenNotes() })
                        if (isOwner == "CompanyOwner") {
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Çıkış yap", color = PillErr.fg) }, onClick = { menuOpen = false; onLogout() })
                        }
                    }
                }
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
                // Filtre Barı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "all" to "Tümü",
                        "pending" to "Bekleyen ($pendingCount)",
                        "approved" to "Onaylandı",
                        "rejected" to "Reddedildi"
                    ).forEach { (id, label) ->
                        val isSelected = filter == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (isSelected) StaffGradStart else White)
                                .clickable { filter = id },
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

                // Talep Listesi
                if (filtered.isEmpty() && !isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Görüntülenecek talep bulunamadı",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { req ->
                            val isShift = req.requestType.contains("Shift", ignoreCase = true)

                            LukaCard(
                                modifier = Modifier.clickable { onNavigateToRequestDetail(req.id) },
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = req.staffFullName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                        Text(
                                            text = when (req.requestType) {
                                                "ShiftHourChange" -> "Vardiya Saati Değişikliği"
                                                "WeeklyOffDayChange" -> "Haftalık İzin Değişimi"
                                                "AnnualLeave" -> "Yıllık İzin Talebi"
                                                "ExcusedLeave" -> "Mazeret İzni"
                                                "StockDepletion" -> "Stok Tükenme Bildirimi"
                                                "EquipmentFault" -> "Ekipman Arızası"
                                                "GeneralNeed" -> "Genel ihtiyaç"
                                                else -> req.requestType
                                            },
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }

                                    val pillColor = when (req.status.lowercase()) {
                                        "approved" -> PillOk
                                        "rejected" -> PillErr
                                        "pending" -> PillWarn
                                        else -> PillNeutral
                                    }
                                    val pillText = when (req.status.lowercase()) {
                                        "approved" -> "Onaylandı"
                                        "rejected" -> "Reddedildi"
                                        "pending" -> "Beklemede"
                                        "cancelled" -> "İptal Edildi"
                                        else -> req.status
                                    }
                                    StatusPill(text = pillText, pillColor = pillColor)
                                }
                                Text("Öncelik: ${com.lukasoft.lukarest.ui.components.priorityLabels[req.priority] ?: req.priority}", style = MaterialTheme.typography.bodySmall, color = TextMain)

                                Spacer(modifier = Modifier.height(8.dp))

                                // Detay Satırı
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF3F7FC))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isShift) Icons.Default.SwapHoriz else Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = StaffGradStart,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = req.summaryText.ifBlank { req.reason },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BossGradStart
                                    )
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


