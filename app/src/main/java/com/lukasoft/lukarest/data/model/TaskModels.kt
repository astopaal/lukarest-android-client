package com.lukasoft.lukarest.data.model

enum class TaskStatus {
    PENDING,
    DONE,
    LATE,
    AWAITING_APPROVAL,
    REJECTED
}

enum class TaskRepeat {
    ONCE,
    DAILY,
    CYCLE_BOUND
}

data class MediaItem(
    val id: Long,
    val type: String, // "photo" | "video"
    val uri: String,
    val name: String
)

data class TaskItem(
    val priority: String = "Medium",
    val requiresApproval: Boolean = false,
    val submittedAtUtc: String? = null,
    val reviewedAtUtc: String? = null,
    val reviewNote: String? = null,
    val id: String,
    val title: String,
    val description: String? = null,
    val completionNote: String? = null,
    val proofMedia: List<com.lukasoft.lukarest.data.api.TaskMediaDto> = emptyList(),
    val startTime: String,
    val endTime: String? = null,
    val shiftId: String,
    val assignedTo: String, // "shift" veya staffId
    val mediaRequired: Boolean = false,
    val allowedProofType: String = "None",
    val hasMedia: Boolean = false,
    val repeat: TaskRepeat = TaskRepeat.ONCE,
    val status: TaskStatus = TaskStatus.PENDING,
    val mediaProof: List<MediaItem> = emptyList(),
    val completedAt: String? = null,
    val completedBy: String? = null,
    val group: String = "a", // "a" veya "b"
    val completionRule: String = "Each",
    val requiredCount: Int = 1,
    val completedCount: Int = 0
) {
    val isShared: Boolean get() = completionRule == "Quota"
}

