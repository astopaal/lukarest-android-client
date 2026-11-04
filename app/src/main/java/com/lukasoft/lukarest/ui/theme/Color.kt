package com.lukasoft.lukarest.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Gradients ──────────────────────────────────────────────
val StaffGradStart = Color(0xFF134EA2)
val StaffGradEnd = Color(0xFF2472DE)
val StaffGradient = Brush.horizontalGradient(listOf(StaffGradStart, StaffGradEnd))

val BossGradStart = Color(0xFF0B2956)
val BossGradEnd = Color(0xFF174B94)
val BossGradient = Brush.horizontalGradient(listOf(BossGradStart, BossGradEnd))

val BtnGradStart = Color(0xFF1453AD)
val BtnGradEnd = Color(0xFF2574DE)
val BtnGradient = Brush.horizontalGradient(listOf(BtnGradStart, BtnGradEnd))

val ActiveCardGradStart = Color(0xFF0C326B)
val ActiveCardGradMid = Color(0xFF1955AC)
val ActiveCardGradEnd = Color(0xFF2B7CE9)
val ActiveCardGradient = Brush.verticalGradient(
    listOf(ActiveCardGradStart, ActiveCardGradMid, ActiveCardGradEnd)
)

val ProgGradStart = Color(0xFF1453AD)
val ProgGradEnd = Color(0xFF2B82F6)
val ProgGradient = Brush.horizontalGradient(listOf(ProgGradStart, ProgGradEnd))

// ── Arka Plan & Yüzey ─────────────────────────────────────────
val BgBoard = Color(0xFFEEF3F9)
val BgScreenTop = Color(0xFFF3F7FC)
val BgScreenBot = Color(0xFFE2ECF8)
val White = Color(0xFFFFFFFF)
val ProgTrack = Color(0xFFD6E4F5)
val BorderSoft = Color(0xFFC9D8EC)

// ── Metin ─────────────────────────────────────────────────────
val TextMain = Color(0xFF0F1F38)
val TextSoft = Color(0xFF3B4E68)
val TextMuted = Color(0xFF526580)

// ── Status Pills ───────────────────────────────────────────────
data class PillColor(val bg: Color, val fg: Color)

val PillBlue = PillColor(bg = Color(0xFFE0EEFF), fg = Color(0xFF114591))
val PillSky = PillColor(bg = Color(0xFFDCF0FA), fg = Color(0xFF03608A))
val PillOk = PillColor(bg = Color(0xFFDCFCE7), fg = Color(0xFF146C38))
val PillWarn = PillColor(bg = Color(0xFFFFEFC2), fg = Color(0xFF7A4F00))
val PillErr = PillColor(bg = Color(0xFFFFE0D8), fg = Color(0xFF9A2A12))
val PillNeutral = PillColor(bg = Color(0xFFE6EEF8), fg = Color(0xFF3B4E68))

// ── Vardiya Hücre Renkleri ─────────────────────────────────────
data class ShiftColor(val bg: Color, val fg: Color, val border: Color)

val ShiftSabah = ShiftColor(bg = Color(0xFFDCEBFA), fg = Color(0xFF114591), border = Color(0xFFB7D4F5))
val ShiftAksam = ShiftColor(bg = Color(0xFFDDF3F4), fg = Color(0xFF065F6B), border = Color(0xFFAEE0E4))
val ShiftGece = ShiftColor(bg = Color(0xFFE5E0FA), fg = Color(0xFF3D288A), border = Color(0xFFC6BBF2))
val ShiftOzel = ShiftColor(bg = Color(0xFFFFEFC2), fg = Color(0xFF7A4F00), border = Color(0xFFF2D078))
val ShiftIzin = ShiftColor(bg = Color(0xFFE8EEF6), fg = Color(0xFF485B73), border = Color(0xFFC8D5E6))
val ShiftEmpty = ShiftColor(bg = Color(0xFFF6F9FD), fg = Color(0xFF8296B0), border = Color(0xFFB8CBE3))
