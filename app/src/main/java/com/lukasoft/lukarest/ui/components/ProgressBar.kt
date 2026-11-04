package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.ProgGradient
import com.lukasoft.lukarest.ui.theme.ProgTrack

@Composable
fun LukaProgressBar(
    progress: Float, // 0.0f .. 1.0f
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(ProgTrack)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(RoundedCornerShape(height / 2))
                    .background(ProgGradient)
            )
        }
    }
}

