package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
fun LukaAccordionHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp)
            .lukaFocusOutline()
            .semantics { stateDescription = if (expanded) "Açık" else "Kapalı" }
            .clickable(role = Role.Button, onClickLabel = if (expanded) "Daralt" else "Genişlet", onClick = onToggle),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        content = content
    )
}
