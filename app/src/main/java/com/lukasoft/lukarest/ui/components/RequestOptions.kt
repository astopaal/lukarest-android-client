package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val priorityLabels = linkedMapOf("Low" to "Düşük", "Medium" to "Orta", "High" to "Yüksek", "Critical" to "Kritik")

fun priorityColor(value: String) = when (value) {
    "Critical" -> com.lukasoft.lukarest.ui.theme.PillErr
    "High" -> com.lukasoft.lukarest.ui.theme.PillWarn
    "Low" -> com.lukasoft.lukarest.ui.theme.PillNeutral
    else -> com.lukasoft.lukarest.ui.theme.PillBlue
}

@Composable
fun PrioritySelector(value: String, onChange: (String) -> Unit) {
    Column {
        Text("Görev önceliği", style = MaterialTheme.typography.labelLarge)
        LukaChoiceGroup(value = value, options = priorityLabels.toList(), onSelect = onChange)
    }
}

@Composable
fun PriorityField(value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    Box {
        OutlinedButton(onClick = { focus.clearFocus(); expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Öncelik: ${priorityLabels[value] ?: value}")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            priorityLabels.forEach { (code, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onChange(code); expanded = false })
            }
        }
    }
}
