package com.lukasoft.lukarest.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class StaffItemDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("employmentType") val employmentType: String,
    @SerializedName("birthDate") val birthDate: String,
    @SerializedName("roleInBranch") val roleInBranch: String?,
    @SerializedName("primaryBranchId") val primaryBranchId: String?,
    @SerializedName("isActive") val isActive: Boolean
)

data class ChangeStaffRoleBody(
    @SerializedName("role") val role: String
)

data class StaffDetailDto(val staff: StaffItemDto, val assignments: List<StaffAssignmentDto>)

data class AssignStaffBody(val role: String, val makePrimary: Boolean)

interface StaffAdminService {
    @POST("/api/v1/staff/{userId}/assignment")
    suspend fun assignStaff(@Path("userId") userId: String, @Body body: AssignStaffBody): Response<StaffDetailDto>

    @DELETE("/api/v1/staff/{userId}/assignment")
    suspend fun unassignStaff(@Path("userId") userId: String): Response<Unit>

    @GET("/api/v1/staff")
    suspend fun getStaff(@Query("includeInactive") includeInactive: Boolean = false): Response<List<StaffItemDto>>

    @GET("/api/v1/staff/{userId}")
    suspend fun getStaffDetail(@Path("userId") userId: String): Response<StaffDetailDto>

    @PUT("/api/v1/staff/{userId}/role")
    suspend fun changeRole(@Path("userId") userId: String, @Body body: ChangeStaffRoleBody): Response<Unit>

    @POST("/api/v1/staff/{userId}/deactivate")
    suspend fun deactivateStaff(@Path("userId") userId: String): Response<Unit>

    @POST("/api/v1/staff/{userId}/activate")
    suspend fun activateStaff(@Path("userId") userId: String): Response<Unit>
}



