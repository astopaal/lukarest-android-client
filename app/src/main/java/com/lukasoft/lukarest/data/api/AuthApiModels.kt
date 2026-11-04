package com.lukasoft.lukarest.data.api

import com.google.gson.annotations.SerializedName

data class RequestPatronEmailLoginCommand(
    @SerializedName("email") val email: String
)

data class VerifyPatronEmailLoginCommand(
    @SerializedName("token") val token: String
)

data class RefreshAccessTokenCommand(
    @SerializedName("refreshToken") val refreshToken: String
)

data class LogoutCommand(
    @SerializedName("refreshToken") val refreshToken: String
)

data class SwitchBranchCommand(
    @SerializedName("branchId") val branchId: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthTokensResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Long? = null,
    @SerializedName("requiresSetup") val requiresSetup: Boolean = false
)

data class StaffAssignmentDto(
    @SerializedName("branchId") val branchId: String,
    @SerializedName("branchName") val branchName: String,
    @SerializedName("roleInBranch") val roleInBranch: String?,
    @SerializedName("isPrimary") val isPrimary: Boolean
)

data class MeDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("birthDate") val birthDate: String,
    @SerializedName("employmentType") val employmentType: String,
    @SerializedName("globalRole") val globalRole: String,
    @SerializedName("companyId") val companyId: String,
    @SerializedName("companyName") val companyName: String,
    @SerializedName("activeBranchId") val activeBranchId: String?,
    @SerializedName("activeBranchName") val activeBranchName: String?,
    @SerializedName("roleInBranch") val roleInBranch: String?,
    @SerializedName("assignments") val assignments: List<StaffAssignmentDto> = emptyList(),
    @SerializedName("requiresSetup") val requiresSetup: Boolean = false
)

