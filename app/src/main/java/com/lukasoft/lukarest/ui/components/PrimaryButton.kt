package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.ui.theme.BtnGradient
import com.lukasoft.lukarest.ui.theme.White
import com.lukasoft.lukarest.ui.theme.BgBoard
import com.lukasoft.lukarest.ui.theme.TextMuted

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    gradient: Brush = BtnGradient,
    textColor: Color = White,
    loading: Boolean = false
) {
    val interactive = enabled && !loading
    val shape = RoundedCornerShape(24.dp)
    Button(
        onClick = onClick,
        enabled = interactive,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = textColor,
            disabledContainerColor = BgBoard,
            disabledContentColor = TextMuted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height.coerceAtLeast(48.dp))
            .lukaFocusOutline(shape)
            .background(if (interactive) gradient else Brush.linearGradient(listOf(BgBoard, BgBoard)), shape)
            .semantics { if (loading) stateDescription = "İşlem sürüyor" }
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextMuted)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (interactive) textColor else TextMuted,
            modifier = Modifier.weight(1f, fill = false),
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

