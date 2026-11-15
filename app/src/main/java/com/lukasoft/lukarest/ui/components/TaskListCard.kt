package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.ui.theme.*

@Composable
fun TaskListCard(
    task: TaskItem,
    onOpen: () -> Unit,
    onAddProof: (() -> Unit)? = null,
    showReminder: Boolean = false
) {
    val open = task.status !in listOf(TaskStatus.DONE, TaskStatus.AWAITING_APPROVAL)
    val (status, statusColor) = when (task.status) {
        TaskStatus.DONE -> "Tamamlandı" to PillOk.fg
        TaskStatus.AWAITING_APPROVAL -> "Onay bekliyor" to PillWarn.fg
        TaskStatus.REJECTED -> "Geri gönderildi" to PillErr.fg
        TaskStatus.LATE -> "Gecikti" to PillErr.fg
        TaskStatus.PENDING -> "Bekliyor" to TextMuted
    }
    val time = listOfNotNull(task.startTime.takeIf { it.isNotBlank() }?.take(5), task.endTime?.takeIf { it.isNotBlank() }?.take(5))
        .joinToString(" - ").ifBlank { "Saat belirtilmedi" }
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(), color = White,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BorderSoft)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (showReminder && open) 4.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Text(task.title, modifier = Modifier.weight(1f), color = TextMain,
                    fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val priority = priorityColor(task.priority)
                Surface(color = priority.bg, shape = RoundedCornerShape(6.dp)) {
                    Text(priorityLabels[task.priority] ?: task.priority, color = priority.fg, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Text(time, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Text(status, style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
            task.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = TextSoft, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (task.status == TaskStatus.REJECTED) task.reviewNote?.takeIf { it.isNotBlank() }?.let {
                Text("Yönetici notu: $it", color = PillErr.fg, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (task.isShared) Text("Ortak görev · ${task.completedCount}/${task.requiredCount} tamamlandı", color = StaffGradStart, style = MaterialTheme.typography.bodySmall)
            if (task.mediaRequired) Text(
                when { task.hasMedia -> "Kanıt yüklendi"; task.allowedProofType == "VideoOnly" -> "Video kanıtı gerekli"; task.allowedProofType == "PhotoOnly" -> "Fotoğraf kanıtı gerekli"; else -> "Fotoğraf veya video gerekli" },
                color = TextMuted, style = MaterialTheme.typography.bodySmall)
            if (showReminder && open) {
                HorizontalDivider(color = BorderSoft)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (task.mediaRequired && onAddProof != null) {
                        TextButton(onClick = onAddProof, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(if (task.hasMedia) "Kanıtı görüntüle" else "Kanıt ekle", color = StaffGradStart) }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Ayrıntılar", color = StaffGradStart, style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = StaffGradStart, modifier = Modifier.size(14.dp))
                        }
                    }
                    TaskReminderButton(task.id)
                }
            }
        }
    }
}
