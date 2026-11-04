package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.ui.theme.*

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    pillColor: PillColor = PillBlue
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillColor.bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = pillColor.fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

