package com.lukasoft.lukarest.ui.screens.boss

import com.lukasoft.lukarest.ui.components.LukaBottomSheet
import com.lukasoft.lukarest.ui.components.LukaAccordionHeader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.toTaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.LukaProgressBar
import com.lukasoft.lukarest.ui.components.LukaSnackbarHost
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BossTasksScreen(
    viewModel: LukaViewModel,
    onNavigateToCreateTask: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToDefinitions: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var staffGroups by remember { mutableStateOf<List<StaffTaskSection>>(emptyList()) }
    val tasks = remember(staffGroups) { staffGroups.flatMap { it.tasks } }
    var filter by remember { mutableStateOf("all") }
    var isRefreshing by remember { mutableStateOf(false) }
    var expandedStaffIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedTask by remember { mutableStateOf<TaskItem?>(null) }
    var reviewNote by remember { mutableStateOf("") }
    var reviewing by remember { mutableStateOf(false) }
    var reviewError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTask?.id) { reviewNote = ""; reviewError = null }

    fun refreshTasks() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val today = DateUtils.getTurkeyTodayIso()
                val res = ApiClient.tasksService.getOverview(today)
                if (res.isSuccessful && res.body() != null) {
                    staffGroups = res.body()!!.staff.map { group ->
                        val shiftCode = group.shiftShortCode.orEmpty()
                        StaffTaskSection(
                            userId = group.userId,
                            name = group.fullName,
                            shiftCode = shiftCode,
                            tasks = group.tasks.map { it.toTaskItem(group.fullName, shiftCode) }
                        )
                    }
                    viewModel.setTasks(staffGroups.flatMap { it.tasks })
                }
            } catch (_: Exception) {
            } finally {
                delay(400)
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTasks()
    }

    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val lateCount = tasks.count { it.status == TaskStatus.LATE }
    val pendingStatuses = setOf(TaskStatus.PENDING, TaskStatus.AWAITING_APPROVAL)
    val pendingCount = tasks.count { it.status in pendingStatuses }

    fun matchesFilter(task: TaskItem): Boolean =
        when (filter) {
            "late" -> task.status == TaskStatus.LATE
            "pending" -> task.status in pendingStatuses
            "done" -> task.status == TaskStatus.DONE
            else -> true
        }

    val visibleGroups = staffGroups.mapNotNull { group ->
        val filtered = group.tasks.filter(::matchesFilter)
        if (filtered.isEmpty()) null else group to filtered
    }

    Scaffold(
        snackbarHost = { LukaSnackbarHost(snackbar) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Görev Takibi",
                    subtitle = "Tüm Vardiya Görevleri",
                    initials = "P",
                    gradient = BossGradient,
                    actionIcon = Icons.Default.Add,
                    onActionClick = onNavigateToCreateTask
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshTasks() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filtre Barı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "all" to "Tümü (${tasks.size})",
                        "late" to "Gecikmiş ($lateCount)",
                        "pending" to "Bekleyen ($pendingCount)",
                        "done" to "Bitti ($doneCount)"
                    ).forEach { (id, label) ->
                        val isSelected = filter == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (isSelected) StaffGradStart else White)
                                .border(1.dp, if (isSelected) StaffGradStart else BorderSoft, RoundedCornerShape(17.dp))
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

                // İlerleme Özeti Kartı
                LukaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$doneCount / ${tasks.size} görev tamamlandı",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        if (lateCount > 0) {
                            StatusPill(text = "$lateCount gecikmiş", pillColor = PillErr)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val progress = if (tasks.isNotEmpty()) doneCount.toFloat() / tasks.size else 0f
                    LukaProgressBar(progress = progress, height = 7.dp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TextButton(onClick = onNavigateToDefinitions) { Text("Görev tanımları") }
                    }
                    visibleGroups.firstOrNull { it.first.userId == SharedTaskGroupId }?.let { (_, sharedTasks) ->
                        item(key = SharedTaskGroupId) {
                            LukaCard {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Ortak görevler", style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                                    sharedTasks.forEach { task -> HourlyTaskRow(task) { selectedTask = task } }
                                }
                            }
                        }
                    }
                    val personGroups = visibleGroups.filter { it.first.userId != SharedTaskGroupId }
                    if (personGroups.isNotEmpty()) {
                        item {
                            Text(
                                "Personelin saatlik görevleri",
                                style = MaterialTheme.typography.labelMedium,
                                color = PillBlue.fg
                            )
                        }
                    }
                    items(personGroups, key = { (group, _) -> group.userId }) { (group, filteredTasks) ->
                        val expanded = group.userId in expandedStaffIds
                        StaffTaskAccordion(
                            group = group,
                            visibleTasks = filteredTasks,
                            expanded = expanded,
                            onToggle = {
                                expandedStaffIds = if (expanded) {
                                    expandedStaffIds - group.userId
                                } else {
                                    expandedStaffIds + group.userId
                                }
                            },
                            onTaskClick = { selectedTask = it }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    selectedTask?.let { task ->
        val (statusText, statusColor) = when (task.status) {
            TaskStatus.AWAITING_APPROVAL -> "Onay bekliyor" to PillWarn
            TaskStatus.REJECTED -> "Geri gönderildi" to PillErr
            TaskStatus.DONE -> "Tamamlandı" to PillOk
            TaskStatus.LATE -> "Gecikti" to PillErr
            TaskStatus.PENDING -> "Bekliyor" to PillBlue
        }
        val timeRange = listOfNotNull(
            task.startTime.takeIf { it.isNotBlank() },
            task.endTime?.takeIf { it.isNotBlank() }
        ).joinToString(" – ").ifBlank { "Saat belirtilmemiş" }

        LukaBottomSheet(
            onDismissRequest = { if (!reviewing) selectedTask = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = BgScreenTop
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Görev detayı",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextMuted
                )
                IconButton(onClick = { selectedTask = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Görev detayını kapat", tint = BossGradStart)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LukaCard(elevation = 0.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Öncelik: ${com.lukasoft.lukarest.ui.components.priorityLabels[task.priority] ?: task.priority}")
                        StatusPill(text = statusText, pillColor = statusColor)
                        if (task.isShared) {
                            Text("Ortak görev · %d / %d tamamlandı".format(task.completedCount, task.requiredCount), color = StaffGradStart)
                            LukaProgressBar(task.completedCount.toFloat() / task.requiredCount.coerceAtLeast(1))
                        }
                        Text(
                            task.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BossGradStart
                        )
                        HorizontalDivider(color = BorderSoft)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TaskDetailFact(
                                icon = Icons.Default.Person,
                                label = if (task.isShared) "Atama" else "Atanan personel",
                                value = if (task.isShared) "Ortak görev" else task.assignedTo.ifBlank { "Personel belirtilmemiş" },
                                modifier = Modifier.weight(1f)
                            )
                            TaskDetailFact(
                                icon = Icons.Default.Schedule,
                                label = "Planlanan saat",
                                value = timeRange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                task.reviewNote?.let { Text("İnceleme notu: $it", color = TextMain) }
                task.submittedAtUtc?.let { Text("Onaya gönderim: ${DateUtils.formatUtcTimestamp(it)}", style = MaterialTheme.typography.bodySmall) }
                task.reviewedAtUtc?.let { Text("İnceleme zamanı: ${DateUtils.formatUtcTimestamp(it)}", style = MaterialTheme.typography.bodySmall) }
                if (task.status == TaskStatus.AWAITING_APPROVAL) {
                    OutlinedTextField(reviewNote, { reviewNote = it.take(2000) }, label = { Text("İnceleme notu (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth(), enabled = !reviewing)
                    reviewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(true to "Onayla", false to "Geri gönder").forEach { (approved, label) ->
                            Button(enabled = !reviewing, onClick = {
                                coroutineScope.launch {
                                    reviewing = true
                                    reviewError = null
                                    try {
                                        val result = ApiClient.tasksService.reviewTask(task.id, com.lukasoft.lukarest.data.api.ReviewTaskBody(approved, reviewNote.trim().ifBlank { null }))
                                        if (result.isSuccessful) { selectedTask = null; refreshTasks() }
                                        else reviewError = "İnceleme kaydedilemedi (${result.code()}). Görev durumu değişmiş olabilir."
                                    } catch (e: kotlinx.coroutines.CancellationException) { throw e
                                    } catch (_: java.io.IOException) { reviewError = "Sunucuya ulaşılamadı."
                                    } finally { reviewing = false }
                                }
                            }) { Text(label, color = if (reviewing) TextMuted else White) }
                        }
                    }
                }
                LukaCard(elevation = 0.dp) {
                    Text("Açıklama", style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        task.description?.takeIf { it.isNotBlank() } ?: "Açıklama eklenmemiş.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSoft
                    )
                }
                task.completionNote?.takeIf { it.isNotBlank() }?.let {
                    LukaCard(backgroundColor = PillBlue.bg, elevation = 0.dp) {
                        Text(if (task.isShared) "Son katkı notu" else "Personelin notu", style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = TextMain)
                    }
                }
                if (task.proofMedia.isNotEmpty()) {
                    Text("Görev kanıtları (${task.proofMedia.size})", style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                    task.proofMedia.forEachIndexed { index, proof ->
                        val isVideo = proof.mediaType.startsWith("video/")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = White,
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val response = ApiClient.tasksService.downloadTaskMedia(task.id, proof.id)
                                        val bytes = if (response.isSuccessful) response.body()?.let { body ->
                                            withContext(Dispatchers.IO) { body.bytes() }
                                        } else null
                                        if (!response.isSuccessful || bytes == null || bytes.isEmpty()) {
                                            snackbar.showSnackbar("Kanıt açılamadı (${response.code()}).")
                                        } else {
                                            val file = withContext(Dispatchers.IO) {
                                                val dir = File(context.cacheDir, "task-proofs").apply { mkdirs() }
                                                File(dir, "${proof.id}.${if (isVideo) "mp4" else "jpg"}").apply { writeBytes(bytes) }
                                            }
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, proof.mediaType)
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            context.startActivity(view)
                                        }
                                    } catch (_: Exception) {
                                        snackbar.showSnackbar("Kanıt dosyası açılamadı.")
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if (isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt, null, tint = BossGradStart)
                                Column(Modifier.weight(1f)) {
                                    Text("${index + 1}. ${if (isVideo) "Video" else "Fotoğraf"}", style = MaterialTheme.typography.titleSmall, color = TextMain)
                                    Text("Görüntülemek için dokun", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Icon(Icons.Default.OpenInNew, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else if (task.mediaRequired) {
                    LukaCard(elevation = 0.dp) {
                        Text("Görev kanıtları", style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                        Spacer(Modifier.height(4.dp))
                        Text("Henüz kanıt yüklenmemiş.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }
            }
        }
    }

}

private const val SharedTaskGroupId = "00000000-0000-0000-0000-000000000000"

private data class StaffTaskSection(
    val userId: String,
    val name: String,
    val shiftCode: String,
    val tasks: List<TaskItem>
)

@Composable
private fun StaffTaskAccordion(
    group: StaffTaskSection,
    visibleTasks: List<TaskItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onTaskClick: (TaskItem) -> Unit
) {
    val doneCount = group.tasks.count { it.status == TaskStatus.DONE }
    val name = group.name.ifBlank { "Personel" }
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.take(1) }.uppercase(Locale.forLanguageTag("tr-TR"))

    LukaCard(contentPadding = PaddingValues(0.dp)) {
        LukaAccordionHeader(
            expanded = expanded,
            onToggle = onToggle,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(PillBlue.bg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, style = MaterialTheme.typography.labelLarge, color = PillBlue.fg)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BossGradStart,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (group.shiftCode.isNotBlank()) {
                    Text(group.shiftCode, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            Column(Modifier.widthIn(min = 48.dp), horizontalAlignment = Alignment.End) {
                Text("$doneCount / ${group.tasks.size}", style = MaterialTheme.typography.labelMedium, color = TextMain)
                Spacer(Modifier.height(3.dp))
                LukaProgressBar(
                    progress = doneCount.toFloat() / group.tasks.size,
                    modifier = Modifier.width(48.dp),
                    height = 5.dp
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(26.dp).background(BgScreenTop, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = PillBlue.fg,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp, bottom = 11.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(color = BorderSoft)
                Text("Saatlik görevler", style = MaterialTheme.typography.labelMedium, color = BtnGradStart)
                visibleTasks.forEach { task ->
                    HourlyTaskRow(task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

@Composable
private fun HourlyTaskRow(task: TaskItem, onClick: () -> Unit) {
    com.lukasoft.lukarest.ui.components.TaskListCard(task, onOpen = onClick)
}

@Composable
private fun TaskDetailFact(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = StaffGradStart, modifier = Modifier.size(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TextMain)
        }
    }
}
