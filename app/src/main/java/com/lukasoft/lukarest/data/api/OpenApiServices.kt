package com.lukasoft.lukarest.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// ══════════════════════════════════════════════════════════════════
// STAFF ONBOARDING (QR SESSION & CLAIM)
// ══════════════════════════════════════════════════════════════════

data class QrSessionResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("pairingCode") val pairingCode: String,
    @SerializedName("expiresAtUtc") val expiresAtUtc: String? = null
)

data class QrSessionStatusResponse(
    val isClaimed: Boolean,
    val assignedBranchId: String?,
    val branchName: String?,
    val canExchange: Boolean
)

data class ClaimStaffQrSessionCommand(
    @SerializedName("pairingCode") val pairingCode: String?,
    @SerializedName("qrToken") val qrToken: String?,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("employmentType") val employmentType: Int, // 1: FullTime, 2: PartTime
    @SerializedName("birthDate") val birthDate: String // YYYY-MM-DD
)

data class ExchangeStaffQrSessionCommand(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("qrToken") val qrToken: String
)

interface StaffOnboardingService {
    @POST("/api/v1/staff-onboarding/qr-session")
    suspend fun createQrSession(): Response<QrSessionResponse>

    @GET("/api/v1/staff-onboarding/qr-session/{sessionId}/status")
    suspend fun getQrSessionStatus(@Path("sessionId") sessionId: String): Response<QrSessionStatusResponse>

    @POST("/api/v1/staff-onboarding/claim")
    suspend fun claimStaff(@Body body: ClaimStaffQrSessionCommand): Response<Unit>

    @POST("/api/v1/staff-onboarding/qr-session/exchange")
    suspend fun exchangeQrSession(@Body body: ExchangeStaffQrSessionCommand): Response<AuthTokensResponse>
}

// ══════════════════════════════════════════════════════════════════
// SHIFTS
// ══════════════════════════════════════════════════════════════════

data class ShiftTemplateDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("colorHex") val colorHex: String,
    @SerializedName("shortCode") val shortCode: String,
    @SerializedName("isCustom") val isCustom: Boolean
)

data class CreateShiftTemplateCommand(
    @SerializedName("name") val name: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("colorHex") val colorHex: String,
    @SerializedName("shortCode") val shortCode: String,
    @SerializedName("isCustom") val isCustom: Boolean = false
)

data class ShiftMatrixCellDto(
    @SerializedName("shiftInstanceId") val shiftInstanceId: String? = null,
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("dayOfWeek") val dayOfWeek: String? = null,
    @SerializedName("date") val date: String,
    @SerializedName("shiftTemplateId") val shiftTemplateId: String? = null,
    @SerializedName("shortCode") val shortCode: String? = null,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("colorHex") val colorHex: String? = null,
    @SerializedName("isOverridden") val isOverridden: Boolean = false,
    @SerializedName("customStartTime") val customStartTime: String? = null,
    @SerializedName("customEndTime") val customEndTime: String? = null,
    @SerializedName("isOffDay") val isOffDay: Boolean = false
)

data class ShiftMatrixRowDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("cells") val cells: List<ShiftMatrixCellDto>
)

data class ShiftMatrixResponse(
    @SerializedName("weekStartDate") val weekStartDate: String,
    @SerializedName("days") val days: List<ShiftMatrixDayDto> = emptyList(),
    @SerializedName("rows") val rows: List<ShiftMatrixRowDto> = emptyList()
)

data class ShiftMatrixDayDto(
    @SerializedName("date") val date: String,
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    @SerializedName("activeStaffCount") val activeStaffCount: Int
)

data class CycleDayRuleDto(
    @SerializedName("dayIndex") val dayIndex: Int,
    @SerializedName("shiftTemplateId") val shiftTemplateId: String? = null,
    @SerializedName("isOffDay") val isOffDay: Boolean,
    @SerializedName("customStartTime") val customStartTime: String? = null,
    @SerializedName("customEndTime") val customEndTime: String? = null
)

data class ShiftCycleDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("cycleLengthDays") val cycleLengthDays: Int,
    @SerializedName("description") val description: String,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("rules") val rules: List<CycleDayRuleDto>
)

data class MaterializeCycleCommand(
    @SerializedName("cycleTemplateId") val cycleTemplateId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("anchorDayIndex") val anchorDayIndex: Int
)

data class AssignShiftCellCommand(
    @SerializedName("userId") val userId: String,
    @SerializedName("date") val date: String,
    @SerializedName("shiftTemplateId") val shiftTemplateId: String?,
    @SerializedName("customStartTime") val customStartTime: String? = null,
    @SerializedName("customEndTime") val customEndTime: String? = null,
    @SerializedName("isOffDay") val isOffDay: Boolean = false
)

data class BulkAssignShiftsCommand(
    @SerializedName("shiftTemplateId") val shiftTemplateId: String,
    @SerializedName("weekStartDate") val weekStartDate: String,
    @SerializedName("scope") val scope: String, // "WeekdaysOnly" or "EntireWeek"
    @SerializedName("targetUserIds") val targetUserIds: List<String>? = null
)

data class ClearShiftMatrixCommand(
    @SerializedName("weekStartDate") val weekStartDate: String
)

data class MyShiftDayDto(
    @SerializedName("date") val date: String,
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    @SerializedName("shortCode") val shortCode: String?,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("isOffDay") val isOffDay: Boolean,
    @SerializedName("isOverridden") val isOverridden: Boolean,
    @SerializedName("colorHex") val colorHex: String?
)

data class MyShiftsResponseDto(
    @SerializedName("weekStartDate") val weekStartDate: String,
    @SerializedName("days") val days: List<MyShiftDayDto> = emptyList(),
    @SerializedName("completedHours") val completedHours: Double,
    @SerializedName("remainingHours") val remainingHours: Double
)

interface ShiftsService {
    @GET("/api/v1/shifts/templates")
    suspend fun getTemplates(): Response<List<ShiftTemplateDto>>

    @POST("/api/v1/shifts/templates")
    suspend fun createTemplate(@Body body: CreateShiftTemplateCommand): Response<ShiftTemplateDto>

    @DELETE("/api/v1/shifts/templates/{id}")
    suspend fun deleteTemplate(@Path("id") id: String): Response<Unit>

    @GET("/api/v1/shifts/matrix")
    suspend fun getMatrix(@Query("weekStartDate") weekStartDate: String): Response<ShiftMatrixResponse>

    @GET("/api/v1/shifts/matrix/latest")
    suspend fun getLatestMatrix(): Response<ShiftMatrixResponse>

    @POST("/api/v1/shifts/matrix/assign-cell")
    suspend fun assignCell(@Body body: AssignShiftCellCommand): Response<Unit>

    @POST("/api/v1/shifts/matrix/bulk-assign")
    suspend fun bulkAssign(@Body body: BulkAssignShiftsCommand): Response<Unit>

    @POST("/api/v1/shifts/matrix/clear")
    suspend fun clearMatrix(@Body body: ClearShiftMatrixCommand): Response<Unit>

    @GET("/api/v1/shifts/cycles")
    suspend fun getCycles(): Response<List<ShiftCycleDto>>

    @POST("/api/v1/shifts/cycles/materialize")
    suspend fun materializeCycle(@Body body: MaterializeCycleCommand): Response<Int>

    @GET("/api/v1/shifts/my-shifts")
    suspend fun getMyShifts(@Query("weekStartDate") weekStartDate: String? = null): Response<MyShiftsResponseDto>
}

// ══════════════════════════════════════════════════════════════════
// TASKS
// ══════════════════════════════════════════════════════════════════

data class TaskDefinitionDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("targetShiftTemplateId") val targetShiftTemplateId: String?,
    @SerializedName("constraintType") val constraintType: String,
    @SerializedName("targetStartTime") val targetStartTime: String?,
    @SerializedName("targetEndTime") val targetEndTime: String?,
    @SerializedName("requiresMediaProof") val requiresMediaProof: Boolean,
    @SerializedName("recurrenceType") val recurrenceType: String,
    @SerializedName("allowedProofType") val allowedProofType: String
)

data class CreateTaskDefinitionCommand(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("targetShiftTemplateId") val targetShiftTemplateId: String?,
    @SerializedName("constraintType") val constraintType: String = "StartOnly",
    @SerializedName("targetStartTime") val targetStartTime: String?,
    @SerializedName("targetEndTime") val targetEndTime: String?,
    @SerializedName("recurrenceType") val recurrenceType: String = "OneShot",
    @SerializedName("requiresMediaProof") val requiresMediaProof: Boolean = false,
    @SerializedName("allowedProofType") val allowedProofType: String = "None"
)

data class TaskExecutionDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("status") val status: String,
    @SerializedName("requiresMediaProof") val requiresMediaProof: Boolean,
    @SerializedName("hasMedia") val hasMedia: Boolean = false
)

data class MyTasksResponseDto(
    @SerializedName("date") val date: String,
    @SerializedName("completionPercent") val completionPercent: Int,
    @SerializedName("tasks") val tasks: List<TaskExecutionDto>
)

data class StaffTaskGroupDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("shiftShortCode") val shiftShortCode: String?,
    @SerializedName("completionPercent") val completionPercent: Int,
    @SerializedName("tasks") val tasks: List<TaskExecutionDto>
)

data class BossTaskOverviewDto(
    @SerializedName("totalCount") val totalCount: Int,
    @SerializedName("completedCount") val completedCount: Int,
    @SerializedName("lateCount") val lateCount: Int,
    @SerializedName("staff") val staff: List<StaffTaskGroupDto>
)

data class TaskMediaDto(
    @SerializedName("id") val id: String,
    @SerializedName("mediaUrl") val mediaUrl: String,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long,
    @SerializedName("uploadedAtUtc") val uploadedAtUtc: String
)

data class CompleteTaskBody(
    @SerializedName("completionNote") val completionNote: String?,
    @SerializedName("mediaProofIds") val mediaProofIds: List<String> = emptyList()
)

interface TasksService {
    @GET("/api/v1/tasks/definitions")
    suspend fun getDefinitions(
        @Query("shiftTemplateId") shiftTemplateId: String? = null,
        @Query("recurrenceType") recurrenceType: String? = null
    ): Response<List<TaskDefinitionDto>>

    @POST("/api/v1/tasks/definitions")
    suspend fun createDefinition(@Body body: CreateTaskDefinitionCommand): Response<TaskDefinitionDto>

    @PUT("/api/v1/tasks/definitions/{id}")
    suspend fun updateDefinition(@Path("id") id: String, @Body body: CreateTaskDefinitionCommand): Response<TaskDefinitionDto>

    @DELETE("/api/v1/tasks/definitions/{id}")
    suspend fun deleteDefinition(@Path("id") id: String): Response<Unit>

    @GET("/api/v1/tasks/overview")
    suspend fun getOverview(@Query("date") date: String, @Query("status") status: String? = null): Response<BossTaskOverviewDto>

    @GET("/api/v1/tasks/my-tasks")
    suspend fun getMyTasks(@Query("date") date: String? = null, @Query("status") status: String? = null): Response<MyTasksResponseDto>

    @POST("/api/v1/tasks/executions/{id}/complete")
    suspend fun completeTask(@Path("id") id: String, @Body body: CompleteTaskBody): Response<TaskExecutionDto>

    @Multipart
    @POST("/api/v1/tasks/executions/{id}/media")
    suspend fun uploadTaskMedia(@Path("id") id: String, @Part file: MultipartBody.Part): Response<TaskMediaDto>
}

// ══════════════════════════════════════════════════════════════════
// REQUESTS
// ══════════════════════════════════════════════════════════════════

data class ShiftChangeRequestDetailsDto(
    @SerializedName("targetDate") val targetDate: String,
    @SerializedName("currentStartTime") val currentStartTime: String,
    @SerializedName("currentEndTime") val currentEndTime: String,
    @SerializedName("proposedStartTime") val proposedStartTime: String,
    @SerializedName("proposedEndTime") val proposedEndTime: String
)

data class LeaveRequestDetailsDto(
    @SerializedName("currentOffDate") val currentOffDate: String,
    @SerializedName("proposedOffDate") val proposedOffDate: String,
    @SerializedName("durationDays") val durationDays: Int,
    @SerializedName("leaveCategory") val leaveCategory: String
)

data class OperationalIssueDetailsDto(
    @SerializedName("itemOrEquipmentName") val itemOrEquipmentName: String,
    @SerializedName("urgencyLevel") val urgencyLevel: String,
    @SerializedName("quantity") val quantity: Double? = null
)

data class CoverageImpactDto(
    @SerializedName("message") val message: String,
    @SerializedName("staffNames") val staffNames: List<String> = emptyList()
)

data class CreateStaffRequestCommand(
    @SerializedName("requestType") val requestType: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("shiftChange") val shiftChange: ShiftChangeRequestDetailsDto? = null,
    @SerializedName("leave") val leave: LeaveRequestDetailsDto? = null,
    @SerializedName("issue") val issue: OperationalIssueDetailsDto? = null
)

private val requestTypes = listOf(
    "ShiftHourChange", "WeeklyOffDayChange", "AnnualLeave", "ExcusedLeave",
    "StockDepletion", "EquipmentFault", "GeneralNeed"
)
private val requestStatuses = listOf("Pending", "Approved", "Rejected", "Cancelled")

data class StaffRequestListItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("staffUserId") val staffUserId: String,
    @SerializedName("staffFullName") val staffFullName: String,
    @SerializedName("staffInitials") val staffInitials: String,
    @SerializedName("requestType") private val requestTypeCode: Int,
    @SerializedName("reason") val reason: String,
    @SerializedName("summaryText") val summaryText: String,
    @SerializedName("status") private val statusCode: Int,
    @SerializedName("createdAtUtc") val createdAtUtc: String,
    @SerializedName("managerNote") val managerNote: String?,
    @SerializedName("reviewedAtUtc") val reviewedAtUtc: String?
) {
    val requestType: String get() = requestTypes.getOrNull(requestTypeCode - 1) ?: "Bilinmeyen talep"
    val status: String get() = requestStatuses.getOrNull(statusCode - 1) ?: "Bilinmeyen durum"
}

data class StaffRequestDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("staffUserId") val staffUserId: String,
    @SerializedName("staffFullName") val staffFullName: String,
    @SerializedName("staffInitials") val staffInitials: String,
    @SerializedName("requestType") private val requestTypeCode: Int,
    @SerializedName("reason") val reason: String,
    @SerializedName("summaryText") val summaryText: String,
    @SerializedName("status") private val statusCode: Int,
    @SerializedName("createdAtUtc") val createdAtUtc: String,
    @SerializedName("managerNote") val managerNote: String?,
    @SerializedName("reviewedAtUtc") val reviewedAtUtc: String?,
    @SerializedName("shiftChange") val shiftChange: ShiftChangeRequestDetailsDto?,
    @SerializedName("leave") val leave: LeaveRequestDetailsDto?,
    @SerializedName("issue") val issue: OperationalIssueDetailsDto?,
    @SerializedName("coverage") val coverage: CoverageImpactDto?
) {
    val requestType: String get() = requestTypes.getOrNull(requestTypeCode - 1) ?: "Bilinmeyen talep"
    val status: String get() = requestStatuses.getOrNull(statusCode - 1) ?: "Bilinmeyen durum"
}

data class UpcomingBirthdayDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("birthDate") val birthDate: String,
    @SerializedName("daysUntil") val daysUntil: Int
)

data class DecideStaffRequestBody(
    @SerializedName("isApproved") val isApproved: Boolean,
    @SerializedName("managerNote") val managerNote: String?
)

interface RequestsService {
    @POST("/api/v1/requests/my-requests")
    suspend fun createMyRequest(@Body body: CreateStaffRequestCommand): Response<StaffRequestListItemDto>

    @GET("/api/v1/requests/my-requests")
    suspend fun getMyRequests(@Query("status") status: String? = null): Response<List<StaffRequestListItemDto>>

    @GET("/api/v1/requests/my-requests/{id}")
    suspend fun getMyRequestDetail(@Path("id") id: String): Response<StaffRequestDetailDto>

    @POST("/api/v1/requests/my-requests/{id}/cancel")
    suspend fun cancelMyRequest(@Path("id") id: String): Response<StaffRequestDetailDto>

    @GET("/api/v1/requests/branch-requests")
    suspend fun getBranchRequests(@Query("status") status: String? = null): Response<List<StaffRequestListItemDto>>

    @GET("/api/v1/requests/{id}")
    suspend fun getRequestDetail(@Path("id") id: String): Response<StaffRequestDetailDto>

    @POST("/api/v1/requests/{id}/decide")
    suspend fun decideRequest(@Path("id") id: String, @Body body: DecideStaffRequestBody): Response<StaffRequestDetailDto>

    @GET("/api/v1/requests/birthdays/upcoming")
    suspend fun getUpcomingBirthdays(@Query("daysAhead") daysAhead: Int = 7): Response<List<UpcomingBirthdayDto>>
}


