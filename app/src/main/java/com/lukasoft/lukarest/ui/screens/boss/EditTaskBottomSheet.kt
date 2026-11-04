package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskBottomSheet(
    task: TaskItem?,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit,
    onDelete: (String) -> Unit
) {
    if (task == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(task) { mutableStateOf(task.title) }
    var desc by remember(task) { mutableStateOf(task.description ?: "") }
    var startTime by remember(task) { mutableStateOf(task.startTime) }
    var endTime by remember(task) { mutableStateOf(task.endTime ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = Color(0xFFCBD5E1)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Görevi Düzenle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BossGradStart
                    )
                    Text(
                        text = "Görev detaylarını ve çalışma saatlerini güncelleyin",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                IconButton(onClick = {
                    onDelete(task.id)
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = PillErr.fg
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Görev Başlığı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Açıklama (İsteğe bağlı)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Başlangıç") },
                    placeholder = { Text("09:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Bitiş") },
                    placeholder = { Text("17:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            PrimaryButton(
                text = "Değişiklikleri Kaydet",
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            task.copy(
                                title = title.trim(),
                                description = desc.trim().ifBlank { null },
                                startTime = startTime.trim(),
                                endTime = endTime.trim().ifBlank { null }
                            )
                        )
                        onDismiss()
                    }
                },
                gradient = BossGradient,
                modifier = Modifier.fillMaxWidth(),
                height = 46.dp
            )
        }
    }
}

