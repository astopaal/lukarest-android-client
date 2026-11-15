package com.lukasoft.lukarest

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.lukasoft.lukarest.data.api.*
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.TaskListCard
import com.lukasoft.lukarest.ui.screens.staff.StaffShiftsScreen
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class ShiftAndTaskCardReviewTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun selectedDayUpdatesTheTopCardWithoutRepeatingTheHoursBelow() {
        val week = DateUtils.getCurrentWeekMonday()
        val today = DateUtils.getTurkeyTodayIso()
        val days = DateUtils.getWeekDays(week)
        val target = days.first { it.isoDate != today }
        val off = days.last { it.isoDate != today && it.isoDate != target.isoDate }
        val shifts = days.map { day -> MyShiftDayDto(day.isoDate, day.dayName, "V", if (day == off) null else if (day == target) "09:00:00" else "13:00:00",
            if (day == off) null else if (day == target) "17:00:00" else "21:00:00", day == off, false, "#114591") }
        val service = object : ShiftsService by ApiClient.shiftsService {
            override suspend fun getMyShifts(weekStartDate: String?) = Response.success(MyShiftsResponseDto(week, shifts, 0.0, 48.0))
        }
        compose.setContent { LukarestTheme { StaffShiftsScreen(LukaViewModel(), {}, {}, service) } }
        compose.onNodeWithText("Bugünün vardiyası").assertIsDisplayed()
        compose.onAllNodesWithText("13:00 - 21:00").assertCountEquals(1)
        compose.onNodeWithText(target.dayNumber).performScrollTo().performClick()
        compose.onNodeWithText("Seçili günün vardiyası").assertIsDisplayed()
        compose.onAllNodesWithText("09:00 - 17:00").assertCountEquals(1)
        compose.onNodeWithText("13:00 - 21:00").assertDoesNotExist()
        capture("selected-shift-top-card")
        compose.onNodeWithText(off.dayNumber).performScrollTo().performClick()
        compose.onNodeWithText("İzin günü").assertIsDisplayed()
        compose.onNodeWithText("09:00 - 17:00").assertDoesNotExist()
    }

    @Test fun taskCardsShowClearStatusAndKeepProofAndDetailActions() {
        var opened = false
        var proof = false
        val tasks = listOf(
            TaskItem(id = "review-one", title = "Soğuk dolap sıcaklıklarını kontrol et", description = "Ölçüm değerlerini kontrol listesine kaydet.", startTime = "13:00", endTime = "14:00", shiftId = "", assignedTo = "me", priority = "Critical", mediaRequired = true, allowedProofType = "PhotoOnly"),
            TaskItem(id = "review-two", title = "Servis öncesi masa düzenini tamamla", startTime = "16:30", shiftId = "", assignedTo = "me", priority = "High", status = TaskStatus.AWAITING_APPROVAL),
            TaskItem(id = "review-three", title = "Depo kontrol listesini güncelle", startTime = "11:00", shiftId = "", assignedTo = "me", priority = "Low", status = TaskStatus.DONE)
        )
        compose.setContent { LukarestTheme {
            Column(Modifier.fillMaxSize().background(BgScreenTop).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tasks.forEach { TaskListCard(it, { opened = true }, { proof = true }, showReminder = true) }
            }
        } }
        compose.onNodeWithText("Onay bekliyor").assertIsDisplayed()
        compose.onNodeWithText("Tamamlandı").assertIsDisplayed()
        capture("task-cards-tablet")
        compose.onNodeWithText("Kanıt ekle").performClick()
        compose.runOnIdle { assertTrue(proof) }
        compose.onNodeWithText(tasks.first().title).performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun taskCardFitsPhoneWidthWithLargeText() {
        val task = TaskItem(id = "review-phone", title = "Soğuk dolap sıcaklıklarını kontrol et ve ölçümleri kaydet", description = "Vardiya başlamadan önce kontrol listesindeki değerleri doğrula.",
            startTime = "13:00", endTime = "14:00", shiftId = "", assignedTo = "me", priority = "Critical", mediaRequired = true, allowedProofType = "PhotoOnly")
        compose.setContent { LukarestTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                Box(Modifier.width(360.dp).background(BgScreenTop).padding(16.dp)) { TaskListCard(task, {}, {}, showReminder = true) }
            }
        } }
        compose.onNodeWithText("Kritik").assertIsDisplayed()
        compose.onNodeWithText("Kanıt ekle").assertIsDisplayed()
        compose.onNodeWithText("Hatırlat").assertIsDisplayed()
        capture("task-card-phone-large-text")
    }
}
