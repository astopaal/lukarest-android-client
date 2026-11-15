package com.lukasoft.lukarest.data.api

import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.data.model.TaskStatus

fun TaskExecutionDto.toTaskItem(assignedTo: String = "", shiftShortCode: String = ""): TaskItem {
    val taskStatus = when (status.lowercase()) {
        "completed", "done" -> TaskStatus.DONE
        "late" -> TaskStatus.LATE
        "awaitingapproval" -> TaskStatus.AWAITING_APPROVAL
        "rejected" -> TaskStatus.REJECTED
        else -> TaskStatus.PENDING
    }
    return TaskItem(
        priority = priority ?: "Medium",
        requiresApproval = requiresApproval,
        submittedAtUtc = submittedAtUtc,
        reviewedAtUtc = reviewedAtUtc,
        reviewNote = reviewNote,
        id = id,
        title = title,
        description = description,
        completionNote = completionNote,
        proofMedia = media.orEmpty(),
        startTime = startTime?.take(5).orEmpty(),
        endTime = endTime?.take(5),
        shiftId = shiftShortCode,
        assignedTo = assignedTo,
        mediaRequired = requiresMediaProof,
        allowedProofType = allowedProofType,
        hasMedia = hasMedia,
        completionRule = completionRule ?: "Each",
        requiredCount = requiredCount ?: 1,
        completedCount = completedCount ?: if (taskStatus == TaskStatus.DONE) 1 else 0,
        status = taskStatus
    )
}
