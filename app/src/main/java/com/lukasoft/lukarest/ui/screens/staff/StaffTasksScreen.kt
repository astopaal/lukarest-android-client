package com.lukasoft.lukarest.ui.screens.staff

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.CompleteTaskBody
import com.lukasoft.lukarest.data.api.toTaskItem
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StaffTasksScreen(
    viewModel: LukaViewModel,
    onNavigateToTaskComplete: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    var selectedFilter by remember { mutableStateOf("all") } // "all", "pending", "done"

    val filteredTasks = when (selectedFilter) {
        "pending" -> tasks.filter { it.status == TaskStatus.PENDING }
        "done" -> tasks.filter { it.status == TaskStatus.DONE }
        else -> tasks
    }

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshStaffTasks() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val res = ApiClient.tasksService.getMyTasks()
                if (res.isSuccessful && res.body() != null) {
                    viewModel.setTasks(res.body()!!.tasks.map { it.toTaskItem("me") })
                } else {
                    Toast.makeText(context, "Görevler alınamadı (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
            } finally {
                delay(400)
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { refreshStaffTasks() }

    fun completeTask(task: TaskItem) {
        if (task.status == TaskStatus.DONE) return
        if (task.mediaRequired) {
            onNavigateToTaskComplete(task.id)
            return
        }
        coroutineScope.launch {
            try {
                val response = ApiClient.tasksService.completeTask(task.id, CompleteTaskBody(completionNote = null))
                if (response.isSuccessful) refreshStaffTasks()
                else Toast.makeText(context, "Görev tamamlanamadı (${response.code()})", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Görev tamamlanamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                TopPill(
                    title = "Görevler",
                    subtitle = "Vardiya Görev Listesi",
                    initials = "AY",
                    gradient = StaffGradient,
                    onActionClick = onLogout
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshStaffTasks() },
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
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFE2ECF8))
                    .padding(3.dp)
            ) {
                listOf(
                    "all" to "Tümü",
                    "pending" to "Bekleyen",
                    "done" to "Tamamlanan"
                ).forEach { (key, label) ->
                    val selected = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) StaffGradStart else Color.Transparent)
                            .clickable { selectedFilter = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) White else TextSoft
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Görevler
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks) { task ->
                    TaskCardItem(
                        task = task,
                        onToggle = { completeTask(task) },
                        onCompleteWithMedia = { onNavigateToTaskComplete(task.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
}

@Composable
fun TaskCardItem(
    task: TaskItem,
    onToggle: () -> Unit,
    onCompleteWithMedia: () -> Unit
) {
    val isDone = task.status == TaskStatus.DONE

    LukaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isDone) StaffGradStart else Color.Transparent)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.LightGray.copy(alpha = 0.35f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) TextMuted else TextMain,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${task.startTime}${if (task.endTime != null) " — ${task.endTime}" else ""}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            if (task.mediaRequired) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clickable { onCompleteWithMedia() }) {
                    StatusPill(
                        text = if (task.hasMedia) "Kanıt yüklendi" else "Kanıt ekle",
                        pillColor = if (task.hasMedia) PillOk else PillBlue
                    )
                }
            }
        }
    }
}

