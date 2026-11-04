package com.lukasoft.lukarest.data.model

enum class RequestType {
    SHIFT_CHANGE,
    LEAVE_CHANGE
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

enum class LeaveType {
    DAY_CHANGE,
    ANNUAL,
    EXCUSE,
    UNPAID
}

enum class ShiftReason {
    APPOINTMENT,
    HEALTH,
    FAMILY,
    OTHER
}

sealed interface BaseRequest {
    val id: String
    val staffId: String
    val type: RequestType
    val status: RequestStatus
    val managerNote: String?
    val createdAt: String
}

data class ShiftChangeRequest(
    override val id: String,
    override val staffId: String,
    override val type: RequestType = RequestType.SHIFT_CHANGE,
    val dayIndex: Int,
    val currentStart: String,
    val currentEnd: String,
    val requestedStart: String,
    val requestedEnd: String,
    val reason: ShiftReason,
    val note: String? = null,
    override val status: RequestStatus = RequestStatus.PENDING,
    override val managerNote: String? = null,
    override val createdAt: String
) : BaseRequest

data class LeaveChangeRequest(
    override val id: String,
    override val staffId: String,
    override val type: RequestType = RequestType.LEAVE_CHANGE,
    val leaveType: LeaveType,
    val currentDayIndex: Int,
    val requestedDayIndex: Int,
    val durationDays: Int,
    val note: String? = null,
    override val status: RequestStatus = RequestStatus.PENDING,
    override val managerNote: String? = null,
    override val createdAt: String
) : BaseRequest

