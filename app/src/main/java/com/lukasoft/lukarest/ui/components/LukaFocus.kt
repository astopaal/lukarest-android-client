package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.BossGradStart

@Composable
internal fun Modifier.lukaFocusOutline(shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    var focused by remember { mutableStateOf(false) }
    val observed = onFocusChanged { focused = it.isFocused }
    return if (focused) observed.border(2.dp, BossGradStart, shape) else observed
}
