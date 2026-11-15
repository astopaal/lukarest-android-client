package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.BorderSoft
import com.lukasoft.lukarest.ui.theme.TextMain
import com.lukasoft.lukarest.ui.theme.TextMuted
import com.lukasoft.lukarest.ui.theme.White
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LukaTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    Surface(
        onClick = { showPicker = true },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = White,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    value.take(5).ifBlank { "Saat seç" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) TextMuted else TextMain
                )
            }
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }

    if (showPicker) {
        val parts = value.split(':')
        val pickerState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: 0,
            is24Hour = true
        )
        AlertDialog(
            modifier = Modifier.onPreviewKeyEvent {
                if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
                    showPicker = false
                    true
                } else false
            },
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = {
                BoxWithConstraints {
                    if (maxWidth >= 300.dp && LocalDensity.current.fontScale <= 1.3f) {
                        TimePicker(state = pickerState)
                    } else {
                        TimeInput(state = pickerState)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(String.format(Locale.ROOT, "%02d:%02d", pickerState.hour, pickerState.minute))
                    showPicker = false
                }) { Text("Seç") }
            },
            dismissButton = {
                Row {
                    if (allowClear && value.isNotBlank()) {
                        TextButton(onClick = {
                            onValueChange("")
                            showPicker = false
                        }) { Text("Temizle") }
                    }
                    TextButton(onClick = { showPicker = false }) { Text("Vazgeç") }
                }
            }
        )
    }
}
