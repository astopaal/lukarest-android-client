package com.lukasoft.lukarest.data.api

import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.data.model.TaskStatus

fun TaskExecutionDto.toTaskItem(assignedTo: String = "", shiftShortCode: String = ""): TaskItem {
    val taskStatus = when (status.lowercase()) {
        "completed", "done" -> TaskStatus.DONE
        "late" -> TaskStatus.LATE
        else -> TaskStatus.PENDING
    }
    return TaskItem(
        id = id,
        title = title,
        startTime = startTime?.take(5).orEmpty(),
        endTime = endTime?.take(5),
        shiftId = shiftShortCode,
        assignedTo = assignedTo,
        mediaRequired = requiresMediaProof,
        hasMedia = hasMedia,
        status = taskStatus
    )
}
