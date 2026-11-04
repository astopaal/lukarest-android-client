package com.lukasoft.lukarest.data.model

import androidx.compose.ui.graphics.Color

data class StaffMember(
    val id: String,
    val fullName: String,
    val initials: String,
    val avatarGrad: Pair<Color, Color>,
    val avatarFg: Color,
    val workType: String, // "full-time" or "part-time"
    val phone: String
)

