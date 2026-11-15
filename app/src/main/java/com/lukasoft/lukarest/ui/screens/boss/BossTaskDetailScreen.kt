package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.LukaTimeField
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel

@Composable
fun BossTaskDetailScreen(
    taskId: String,
    viewModel: LukaViewModel,
    onNavigateBack: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val task = tasks.find { it.id == taskId }

    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var desc by remember(task) { mutableStateOf(task?.description ?: "") }
    var startTime by remember(task) { mutableStateOf(task?.startTime ?: "") }
    var endTime by remember(task) { mutableStateOf(task?.endTime ?: "") }

    val handleSave = {
        if (task != null && title.isNotBlank()) {
            viewModel.updateTask(
                task.copy(
                    title = title,
                    description = desc.ifBlank { null },
                    startTime = startTime,
                    endTime = endTime.ifBlank { null }
                )
            )
            onNavigateBack()
        }
    }

    val handleDelete = {
        if (task != null) {
            viewModel.deleteTask(task.id)
            onNavigateBack()
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
                    title = "Görev Detayı & Düzenle",
                    subtitle = task?.title ?: "",
                    initials = "LK",
                    gradient = BossGradient,
                    actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Özet Kartı
                item {
                    LukaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusPill(text = "Sabah Vardiyası", pillColor = PillBlue)
                            StatusPill(text = "Tek seferlik", pillColor = PillSky)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BossGradStart)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Saat: $startTime — $endTime • ${if (task?.mediaRequired == true) "Medya zorunlu" else "Medya serbest"}",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                // Düzenleme Alanları
                item {
                    Text(text = "GÖREV BİLGİLERİNİ DÜZENLE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BossGradStart)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Görev Adı") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Açıklama / Talimat") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LukaTimeField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = "Başlangıç",
                            modifier = Modifier.weight(1f)
                        )
                        LukaTimeField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = "Bitiş",
                            modifier = Modifier.weight(1f),
                            allowClear = true
                        )
                    }
                }

                // Görevi Sil Butonu
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleDelete() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Görevi Sil",
                            color = PillErr.fg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Alt Butonlar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(text = "Değişiklikleri Kaydet", onClick = handleSave, gradient = BossGradient)
                Text(
                    text = "Geri Dön",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.clickable { onNavigateBack() }.padding(4.dp)
                )
            }
        }
    }
}

