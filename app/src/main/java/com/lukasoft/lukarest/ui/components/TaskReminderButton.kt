package com.lukasoft.lukarest.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.ApiClient
import kotlinx.coroutines.launch

@Composable
fun TaskReminderButton(taskId: String) {
    val scope = rememberCoroutineScope()
    var busy by remember(taskId) { mutableStateOf(false) }
    var armed by remember(taskId) { mutableStateOf(false) }
    var error by remember(taskId) { mutableStateOf<String?>(null) }
    Column(horizontalAlignment = Alignment.End) {
    TextButton(enabled = !busy && !armed, onClick = {
        scope.launch {
            busy = true
            error = null
            try {
                val result = ApiClient.tasksService.setReminder(taskId)
                if (result.isSuccessful) armed = true else error = "Hatırlatıcı kurulamadı (${result.code()})."
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: java.io.IOException) { error = "Sunucuya ulaşılamadı."
            } finally { busy = false }
        }
    }) {
        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(if (armed) "Kuruldu" else if (busy) "Kuruluyor…" else "Hatırlat", color = LocalContentColor.current)
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
