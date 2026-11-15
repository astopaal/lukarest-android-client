package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.*

@Composable
fun LukaLoadingState(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(Modifier.size(24.dp), color = BtnGradStart, strokeWidth = 2.dp)
        Text(message, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LukaEmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    LukaCard(modifier = modifier, elevation = 0.dp) {
        Text(title, color = TextMain, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LukaErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    LukaCard(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        backgroundColor = PillErr.bg,
        elevation = 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = PillErr.fg)
            Text(message, color = PillErr.fg, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = PillErr.fg)) {
            Text("Tekrar dene")
        }
    }
}
