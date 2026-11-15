package com.lukasoft.lukarest

import com.google.gson.Gson
import com.lukasoft.lukarest.data.api.*
import com.lukasoft.lukarest.data.model.TaskStatus
import org.junit.Assert.*
import org.junit.Test

class TaskContractTest {
    private val gson = Gson()
    @Test fun utcNotificationTimesUseTheTurkeyCalendarDay() {
        assertEquals("13 Kas 2026 01:30", com.lukasoft.lukarest.data.util.DateUtils.formatUtcTimestamp("2026-11-12T22:30:00.1234567+00:00"))
    }

    @Test fun approvalAndRejectionRemainDistinctFromPending() {
        val dto = gson.fromJson("""{"id":"task","title":"Kontrol","status":"AwaitingApproval","priority":"Critical","requiresApproval":true,"requiresMediaProof":true,"allowedProofType":"PhotoOnly","submittedAtUtc":"2026-11-12T10:00:00Z"}""", TaskExecutionDto::class.java)
        val item = dto.toTaskItem()
        assertEquals(TaskStatus.AWAITING_APPROVAL, item.status)
        assertEquals("Critical", item.priority)
        assertTrue(item.requiresApproval)
        assertEquals("2026-11-12T10:00:00Z", item.submittedAtUtc)
        val rejected = dto.copy(status = "Rejected", reviewNote = "Yeni fotoğraf gerekli").toTaskItem()
        assertEquals(TaskStatus.REJECTED, rejected.status)
        assertEquals("Yeni fotoğraf gerekli", rejected.reviewNote)
    }

    @Test fun selectedStaffCanSerializeWithoutAShiftAndKeepsNewFields() {
        val command = CreateTaskDefinitionCommand(title = "Kontrol", description = null, targetShiftTemplateId = null,
            targetStartTime = "10:00:00", targetEndTime = null, audience = "SelectedStaff", assigneeUserIds = listOf("person"), requiresApproval = true)
        val json = gson.toJsonTree(command).asJsonObject
        assertFalse(json.has("targetShiftTemplateId"))
        assertEquals("Medium", json["priority"].asString)
        assertTrue(json["requiresApproval"].asBoolean)
        assertEquals("person", json["assigneeUserIds"].asJsonArray[0].asString)
    }

    @Test fun requestBodyHasPriorityWithoutMentions() {
        val request = CreateStaffRequestCommand("GeneralNeed", "Yeni bardak gerekli")
        val json = gson.toJsonTree(request).asJsonObject
        assertEquals("Medium", json["priority"].asString)
        assertFalse(json.has("mentionedUserIds"))
    }
}
