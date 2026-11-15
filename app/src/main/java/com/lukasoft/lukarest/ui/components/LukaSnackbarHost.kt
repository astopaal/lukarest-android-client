package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.BossGradStart
import com.lukasoft.lukarest.ui.theme.PillOk
import com.lukasoft.lukarest.ui.theme.White

@Composable
fun LukaSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        LukaSnackbar(data)
    }
}

@Composable
internal fun LukaSnackbar(data: SnackbarData) {
    val success = listOf("eklendi", "kaydedildi", "tamamlandı", "güncellendi", "silindi", "atandı")
        .any { data.visuals.message.contains(it, ignoreCase = true) }
    Snackbar(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = if (success) Color(0xFF0F3B6D) else BossGradStart,
        contentColor = White,
        actionContentColor = White,
        dismissActionContentColor = White,
        action = {
            data.visuals.actionLabel?.let { label ->
                TextButton(onClick = data::performAction) { Text(label, color = White) }
            }
        },
        dismissAction = if (data.visuals.withDismissAction) ({
            IconButton(onClick = { data.dismiss() }) { Icon(Icons.Default.Close, contentDescription = "Kapat", tint = White) }
        }) else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (success) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PillOk.bg)
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            }
            Text(data.visuals.message, color = White)
        }
    }
}
