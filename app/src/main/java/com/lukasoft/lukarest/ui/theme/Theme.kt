package com.lukasoft.lukarest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BtnGradStart,
    onPrimary = White,
    secondary = StaffGradEnd,
    onSecondary = White,
    tertiary = BossGradStart,
    background = BgScreenTop,
    onBackground = TextMain,
    surface = White,
    onSurface = TextMain,
    surfaceVariant = BgBoard,
    outline = BorderSoft
)

@Composable
fun LukarestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
