package com.lukasoft.lukarest.data.model

enum class TaskStatus {
    PENDING,
    DONE,
    LATE
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
    val group: String = "a" // "a" veya "b"
)

