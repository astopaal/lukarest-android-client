package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.*

@Composable
fun <T> LukaChoiceGroup(
    value: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = value == key,
                onClick = { onSelect(key) },
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp).lukaFocusOutline(),
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = White,
                    labelColor = TextSoft,
                    selectedContainerColor = PillBlue.bg,
                    selectedLabelColor = PillBlue.fg
                )
            )
        }
    }
}

@Composable
fun LukaChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PillBlue.bg else White,
        contentColor = TextMain,
        border = BorderStroke(1.dp, if (selected) BtnGradStart else BorderSoft)
    ) {
        Row(
            modifier = Modifier.lukaFocusOutline().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                detail?.let { Text(it, color = TextMuted, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun LukaCheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (checked) PillBlue.bg else White,
        contentColor = TextMain,
        border = BorderStroke(1.dp, if (checked) BtnGradStart else BorderSoft)
    ) {
        Row(
            Modifier.lukaFocusOutline().toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
                .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.invoke()
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}
