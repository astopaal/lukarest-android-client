package com.lukasoft.lukarest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BtnGradStart,
    onPrimary = White,
    primaryContainer = PillBlue.bg,
    onPrimaryContainer = PillBlue.fg,
    secondary = StaffGradEnd,
    onSecondary = White,
    secondaryContainer = PillBlue.bg,
    onSecondaryContainer = PillBlue.fg,
    tertiary = BossGradStart,
    background = BgScreenTop,
    onBackground = TextMain,
    surface = White,
    onSurface = TextMain,
    surfaceVariant = BgBoard,
    onSurfaceVariant = TextMuted,
    surfaceContainer = White,
    surfaceContainerLow = BgScreenTop,
    surfaceContainerHigh = White,
    outline = TextMuted,
    outlineVariant = BorderSoft,
    error = PillErr.fg,
    onError = White,
    errorContainer = PillErr.bg,
    onErrorContainer = PillErr.fg,
    inverseSurface = BossGradStart,
    inverseOnSurface = White,
    inversePrimary = PillBlue.bg
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
