package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.*
import com.lukasoft.lukarest.data.store.AuthStore
import kotlinx.coroutines.launch

@Composable
fun NotificationDrawer(onSelect: (PushPayload) -> Unit, service: NotificationsService = ApiClient.notificationsService, content: @Composable (() -> Unit) -> Unit) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val userId by AuthStore.userId.collectAsState()
    var rows by remember(userId) { mutableStateOf<List<NotificationItemDto>>(emptyList()) }
    var page by remember(userId) { mutableIntStateOf(1) }
    var hasMore by remember(userId) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var muted by remember(userId) { mutableStateOf(emptySet<String>()) }
    var muting by remember { mutableStateOf<String?>(null) }
    var mutedDay by remember { mutableStateOf(com.lukasoft.lukarest.data.util.DateUtils.getTurkeyTodayIso()) }
    androidx.activity.compose.BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    fun load(next: Boolean) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            val target = if (next) page + 1 else 1
            try {
                val response = service.list(target)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    rows = ((if (next) rows else emptyList()) + body.items).distinctBy { it.notificationId }
                    page = target
                    hasMore = body.hasMore
                } else error = "Bildirimler yüklenemedi (${response.code()})."
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: java.io.IOException) { error = "Sunucuya ulaşılamadı."
            } finally { busy = false }
        }
    }
    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = drawer.isOpen,
        drawerContent = {
            ModalDrawerSheet(Modifier.fillMaxWidth(0.92f), drawerContainerColor = com.lukasoft.lukarest.ui.theme.BgScreenTop, drawerContentColor = com.lukasoft.lukarest.ui.theme.TextMain) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bildirimler", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 14.dp))
                    TextButton(onClick = { scope.launch { drawer.close() } }) { Text("Kapat") }
                }
                Text("Bugün sustur", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall)
                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    listOf("request" to "Talep", "task" to "Görev", "shift" to "Vardiya", "notice" to "Duyuru").chunked(2).forEach { categories ->
                    Row(Modifier.fillMaxWidth()) {
                    categories.forEach { (category, label) ->
                        TextButton(modifier = Modifier.weight(1f), enabled = muting == null && category !in muted, onClick = {
                            scope.launch {
                                muting = category
                                try {
                                    val result = service.mute(MuteNotificationsBody(category))
                                    if (result.isSuccessful) {
                                        muted = muted + category
                                        LukaFeedback.show("$label bildirimleri Türkiye günü sonuna kadar susturuldu")
                                    } else error = "Bildirimler susturulamadı (${result.code()})."
                                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                                } catch (_: java.io.IOException) { error = "Sunucuya ulaşılamadı."
                                } finally { muting = null }
                            }
                        }) { Text(if (category in muted) "$label ✓" else label, maxLines = 1) }
                    }
                    }
                    }
                }
                HorizontalDivider()
                if (busy) LukaLoadingState("Bildirimler yükleniyor.")
                error?.let {
                    if (!busy) LukaErrorState(it, onRetry = { load(false) }, modifier = Modifier.padding(horizontal = 12.dp))
                }
                if (!busy && error == null && rows.isEmpty()) LukaEmptyState(
                    "Henüz bildirimin yok.", "Görev, vardiya ve talep bildirimlerin burada görünecek.",
                    modifier = Modifier.padding(12.dp)
                )
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.notificationId }) { row ->
                        OutlinedCard(onClick = {
                            onSelect(PushPayload(row.type, row.id, row.notificationId))
                            scope.launch { drawer.close() }
                        }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = com.lukasoft.lukarest.ui.theme.White), border = androidx.compose.foundation.BorderStroke(1.dp, com.lukasoft.lukarest.ui.theme.BorderSoft)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(notificationTitle(row.type), style = MaterialTheme.typography.titleSmall)
                                row.contextText?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                                StatusPill(if (row.openedAtUtc == null) "Açılmadı" else "Açıldı", pillColor = if (row.openedAtUtc == null) com.lukasoft.lukarest.ui.theme.PillBlue else com.lukasoft.lukarest.ui.theme.PillNeutral)
                                Text(com.lukasoft.lukarest.data.util.DateUtils.formatUtcTimestamp(row.sentAtUtc), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        if (hasMore) OutlinedButton(onClick = { load(true) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Daha fazla bildirim yükle") }
                        TextButton(onClick = { load(false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Bildirimleri yenile") }
                    }
                }
            }
        }
    ) {
        content {
            val today = com.lukasoft.lukarest.data.util.DateUtils.getTurkeyTodayIso()
            if (today != mutedDay) { mutedDay = today; muted = emptySet() }
            load(false)
            scope.launch { drawer.open() }
        }
    }
}

private fun notificationTitle(type: String): String {
    val base = type.removeSuffix(".reminder")
    val title = when (base) {
        "staff_request.created" -> "Yeni talep"
        "staff_request.decided" -> "Talebin sonuçlandı"
        "request.mentioned" -> "Bir talepte etiketlendin"
        "task.assigned" -> "Yeni görev atandı"
        "task.completed" -> "Görev tamamlandı"
        "task.approval_rejected" -> "Görev geri gönderildi"
        "task.approval_expired" -> "Görev 24 saattir onay bekliyor"
        "task.due_soon" -> "Görevin bitişi yaklaşıyor"
        "task.critical_open" -> "Önemli görev açık kaldı"
        "shift.coverage_gap" -> "Vardiyada personel boşluğu var"
        "shift.changed", "shift.week_assigned" -> "Vardiya planı güncellendi"
        "notice.created" -> "Yeni operasyon notu"
        "notice.mentioned" -> "Operasyon defterinde senden bahsedildi"
        else -> "Operasyon bildirimi"
    }
    return if (base != type) "Hatırlatma: $title" else title
}
