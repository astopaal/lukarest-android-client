package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.ui.theme.White

@Composable
fun LukaCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    elevation: Dp = 2.dp,
    backgroundColor: Color = White,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x100F1F38),
                spotColor = Color(0x150F1F38)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

