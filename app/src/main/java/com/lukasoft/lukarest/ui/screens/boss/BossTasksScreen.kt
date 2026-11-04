package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
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
import com.lukasoft.lukarest.data.api.toTaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.LukaProgressBar
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BossTasksScreen(
    viewModel: LukaViewModel,
    onNavigateToCreateTask: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val tasks by viewModel.tasks.collectAsState()
    var filter by remember { mutableStateOf("all") }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshTasks() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val today = DateUtils.getTurkeyTodayIso()
                val res = ApiClient.tasksService.getOverview(today)
                if (res.isSuccessful && res.body() != null) {
                    val serverList = res.body()!!.staff.flatMap { group ->
                        group.tasks.map { it.toTaskItem(group.fullName, group.shiftShortCode.orEmpty()) }
                    }
                    viewModel.setTasks(serverList)
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
    val pendingCount = tasks.count { it.status == TaskStatus.PENDING }

    val filteredTasks = tasks.filter {
        when (filter) {
            "late" -> it.status == TaskStatus.LATE
            "pending" -> it.status == TaskStatus.PENDING
            "done" -> it.status == TaskStatus.DONE
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

                // Görev Listesi
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks) { task ->
                        LukaCard(
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${task.startTime}${if (task.endTime != null) " — ${task.endTime}" else ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StaffGradStart
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val pillColor = when (task.status) {
                                        TaskStatus.DONE -> PillOk
                                        TaskStatus.LATE -> PillErr
                                        TaskStatus.PENDING -> PillBlue
                                    }
                                    val pillText = when (task.status) {
                                        TaskStatus.DONE -> "Bitti"
                                        TaskStatus.LATE -> "Gecikti"
                                        TaskStatus.PENDING -> "Bekliyor"
                                    }

                                    StatusPill(text = pillText, pillColor = pillColor)

                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = task.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )

                            if (task.assignedTo.isNotBlank()) {
                                Text(text = task.assignedTo, fontSize = 11.sp, color = StaffGradStart, fontWeight = FontWeight.Medium)
                            }

                            if (task.description != null) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = task.description,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    maxLines = 2
                                )
                            }

                            if (task.mediaRequired) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = StaffGradStart,
                                        modifier = Modifier.size(14.dp)
                                    )
                                Text(
                                    text = if (task.hasMedia) "Kanıt yüklendi" else "Kanıt zorunlu",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StaffGradStart
                                    )
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
