package com.lukasoft.lukarest.ui.screens.staff

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.toTaskItem
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.components.LukaChoiceGroup
import com.lukasoft.lukarest.ui.components.LukaLoadingState
import com.lukasoft.lukarest.ui.components.LukaErrorState
import com.lukasoft.lukarest.ui.components.LukaEmptyState
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StaffTasksScreen(
    viewModel: LukaViewModel,
    onNavigateToTaskComplete: (String) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    var selectedFilter by remember { mutableStateOf("all") } // "all", "pending", "done"

    val filteredTasks = when (selectedFilter) {
        "pending" -> tasks.filter { it.status != TaskStatus.DONE }
        "done" -> tasks.filter { it.status == TaskStatus.DONE }
        else -> tasks
    }

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun refreshStaffTasks() {
        coroutineScope.launch {
            isRefreshing = true
            loadError = null
            try {
                val res = ApiClient.tasksService.getMyTasks()
                if (res.isSuccessful && res.body() != null) {
                    viewModel.setTasks(res.body()!!.tasks.map { it.toTaskItem("me") })
                } else {
                    loadError = "Görevler alınamadı (${res.code()})."
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: java.io.IOException) {
                loadError = "Görevler yüklenemedi. Bağlantını kontrol edip tekrar dene."
            } finally {
                delay(400)
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { refreshStaffTasks() }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                TopPill(
                    title = "Görevler",
                    subtitle = "Vardiya Görev Listesi",
                    initials = "AY",
                    gradient = StaffGradient,
                    showAction = false
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
            LukaChoiceGroup(
                value = selectedFilter,
                options = listOf(
                    "all" to "Tümü",
                    "pending" to "Bekleyen",
                    "done" to "Tamamlanan"
                ),
                onSelect = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Görevler
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isRefreshing && tasks.isEmpty()) item { LukaLoadingState("Görevler yükleniyor.") }
                loadError?.let { message -> item { LukaErrorState(message, onRetry = { refreshStaffTasks() }) } }
                if (!isRefreshing && loadError == null && filteredTasks.isEmpty()) item {
                    LukaEmptyState(
                        "Bu görünümde görev yok.",
                        if (tasks.isEmpty()) "Görev atandığında burada görünecek. Listeyi aşağı çekerek yenileyebilirsin."
                        else "Diğer görevleri görmek için filtreyi değiştir."
                    )
                }
                items(filteredTasks) { task ->
                    TaskCardItem(
                        task = task,
                        onToggle = { onNavigateToTaskComplete(task.id) },
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
    com.lukasoft.lukarest.ui.components.TaskListCard(task, onOpen = onToggle,
        onAddProof = onCompleteWithMedia, showReminder = true)
}

