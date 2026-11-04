package com.lukasoft.lukarest.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class BreakDto(
    @SerializedName("id") val id: String,
    @SerializedName("startUtc") val startUtc: String,
    @SerializedName("endUtc") val endUtc: String?,
    @SerializedName("durationMinutes") val durationMinutes: Int?
)

data class AttendanceDayDto(
    @SerializedName("shiftInstanceId") val shiftInstanceId: String?,
    @SerializedName("date") val date: String,
    @SerializedName("plannedStart") val plannedStart: String?,
    @SerializedName("plannedEnd") val plannedEnd: String?,
    @SerializedName("isOffDay") val isOffDay: Boolean,
    @SerializedName("clockInUtc") val clockInUtc: String?,
    @SerializedName("clockOutUtc") val clockOutUtc: String?,
    @SerializedName("breaks") val breaks: List<BreakDto> = emptyList()
)

data class TimesheetRowDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("plannedStart") val plannedStart: String?,
    @SerializedName("plannedEnd") val plannedEnd: String?,
    @SerializedName("isOffDay") val isOffDay: Boolean,
    @SerializedName("clockInUtc") val clockInUtc: String?,
    @SerializedName("clockOutUtc") val clockOutUtc: String?,
    @SerializedName("breakMinutes") val breakMinutes: Int,
    @SerializedName("workedMinutes") val workedMinutes: Int,
    @SerializedName("lateMinutes") val lateMinutes: Int
)

interface AttendanceService {
    @POST("/api/v1/attendance/clock-in")
    suspend fun clockIn(): Response<AttendanceDayDto>

    @POST("/api/v1/attendance/clock-out")
    suspend fun clockOut(): Response<AttendanceDayDto>

    @POST("/api/v1/attendance/breaks/start")
    suspend fun startBreak(): Response<AttendanceDayDto>

    @POST("/api/v1/attendance/breaks/end")
    suspend fun endBreak(): Response<AttendanceDayDto>

    @GET("/api/v1/attendance/me")
    suspend fun getMyAttendance(@Query("date") date: String? = null): Response<AttendanceDayDto>

    @GET("/api/v1/attendance")
    suspend fun getTimesheet(@Query("date") date: String): Response<List<TimesheetRowDto>>
}

