package com.lukasoft.lukarest.data.model

data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null
)

enum class UserRole {
    STAFF,
    BOSS
}

data class AuthUser(
    val id: String,
    val fullName: String,
    val role: UserRole,
    val phone: String,
    val branchName: String = "Luka Restoran Kadıköy"
)

