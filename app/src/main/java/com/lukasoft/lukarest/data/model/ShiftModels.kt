package com.lukasoft.lukarest.data.model

import androidx.compose.ui.graphics.Color
import com.lukasoft.lukarest.ui.theme.*

enum class ShiftColorKey {
    SABAH,
    AKSAM,
    GECE,
    OZEL,
    IZIN;

    fun toShiftColor(): ShiftColor = when(this) {
        SABAH -> ShiftSabah
        AKSAM -> ShiftAksam
        GECE -> ShiftGece
        OZEL -> ShiftOzel
        IZIN -> ShiftIzin
    }
}

data class ShiftDefinition(
    val id: String,
    val name: String,
    val short: String,
    val start: String,
    val end: String,
    val colorKey: ShiftColorKey
)

data class CalendarAssignment(
    val staffId: String,
    val dayIndex: Int, // 0=Pzt ... 6=Paz
    val shiftId: String
)

enum class ShiftDayStatus {
    DONE,
    ACTIVE,
    UPCOMING,
    OFF,
    PENDING
}

data class MyShiftDay(
    val dayIndex: Int,
    val dayLabel: String,
    val shift: ShiftDefinition? = null,
    val status: ShiftDayStatus = ShiftDayStatus.UPCOMING
)

