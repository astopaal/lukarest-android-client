package com.lukasoft.lukarest

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.lukasoft.lukarest.data.api.*
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.screens.boss.OperationalNotesScreen
import com.lukasoft.lukarest.ui.screens.staff.StaffShiftsScreen
import com.lukasoft.lukarest.ui.theme.LukarestTheme
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class NotebookAndShiftsUiTest {
    @get:Rule val compose = createComposeRule()
    private val today = DateUtils.getTurkeyTodayIso()
    private val people = listOf(NoteMentionableUserDto("ayse", "Ayşe Yılmaz", "ayse.yilmaz_12345678", false), NoteMentionableUserDto("samet", "Samet Kaya", "samet.kaya_87654321", true))

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use { output ->
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test fun notebookSuggestsPeopleAndSendsOnlyTheRemainingMention() {
        var saved: CreateOperationalNoteCommand? = null
        val service = object : OperationalNotesService by ApiClient.operationalNotesService {
            override suspend fun getNotes(date: String?, category: String?) = Response.success(emptyList<OperationalNoteDto>())
            override suspend fun getMentionableUsers(search: String?) = Response.success(people)
            override suspend fun createNote(body: CreateOperationalNoteCommand): Response<OperationalNoteDto> {
                saved = body
                return Response.success(OperationalNoteDto("note", body.category, body.targetDate, null, null, null, null, body.noteContent, "me", "Test kullanıcısı", "2026-11-14T12:00:00Z"))
            }
        }
        compose.setContent { LukarestTheme { OperationalNotesScreen({}, service = service) } }
        compose.onNodeWithText("Duyuru ekle").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Akşam devir teslim @ayse")
        compose.onNodeWithText("Ayşe Yılmaz").assertIsDisplayed()
        compose.onNodeWithText("Samet Kaya").assertDoesNotExist()
        capture("notebook-mention-suggestions")
        compose.onNodeWithText("Ayşe Yılmaz").performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("@ayse.yilmaz_12345678", substring = true)
        compose.onNode(hasSetTextAction()).performTextInput("@sa")
        compose.onNodeWithText("Samet Kaya").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Akşam devir teslim @samet.kaya_87654321")
        compose.onNodeWithText("Kaydet").performClick()
        compose.runOnIdle { assertEquals(listOf("samet"), saved?.mentionedUserIds) }
    }

    @Test fun notificationOpensTheNotesDateAndCategory() {
        val date = "2026-11-15"
        val note = OperationalNoteDto("note", "HandoverNote", date, null, null, null, null, "Akşam ekibi teslim notunu kontrol etsin.", "author", "Ayşe Yılmaz", "2026-11-14T12:00:00Z")
        val service = object : OperationalNotesService by ApiClient.operationalNotesService {
            override suspend fun getNote(id: String) = Response.success(note)
            override suspend fun getNotes(date: String?, category: String?) = Response.success(if (date == note.targetDate) listOf(note) else emptyList())
        }
        compose.setContent { LukarestTheme { OperationalNotesScreen({}, initialNoteId = "note", service = service) } }
        compose.onNodeWithText(date).assertIsDisplayed()
        compose.onNodeWithText(note.noteContent).assertIsDisplayed()
        compose.onNodeWithText("Bildirimdeki not").assertExists()
        capture("notebook-notification")
    }

    @Test fun shiftDaysAndRequestShortcutsWork() {
        var shiftRequest = false
        var leaveRequest = false
        val week = DateUtils.getCurrentWeekMonday()
        val days = DateUtils.getWeekDays(week)
        val service = object : ShiftsService by ApiClient.shiftsService {
            override suspend fun getMyShifts(weekStartDate: String?) = Response.success(MyShiftsResponseDto(week, days.mapIndexed { index, day ->
                MyShiftDayDto(day.isoDate, day.dayName, "AK", if (index == 6) null else "14:00:00", if (index == 6) null else "22:30:00", index == 6, index == 2, "#114591")
            }, 17.0, 34.0))
        }
        compose.setContent { LukarestTheme { StaffShiftsScreen(LukaViewModel(), { shiftRequest = true }, { leaveRequest = true }, service) } }
        compose.onNodeWithText("Haftanın özeti").assertIsDisplayed()
        capture("staff-shifts-overview")
        compose.onNodeWithText(days[6].dayNumber).performScrollTo().performClick()
        compose.onNodeWithText("İzin günü").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Vardiya saati değişikliği iste").performScrollTo().performClick()
        compose.onNodeWithText("İzin talebi oluştur").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(shiftRequest); assertTrue(leaveRequest) }
    }

    @Test fun closingAnUnsavedNoteAsksBeforeDiscarding() {
        val service = object : OperationalNotesService by ApiClient.operationalNotesService {
            override suspend fun getNotes(date: String?, category: String?) = Response.success(emptyList<OperationalNoteDto>())
            override suspend fun getMentionableUsers(search: String?) = Response.success(people)
        }
        compose.setContent { LukarestTheme { OperationalNotesScreen({}, service = service) } }
        compose.onNodeWithText("Duyuru ekle").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Kaydedilmemiş devir notu")
        compose.onNodeWithText("Vazgeç").performClick()
        compose.onNodeWithText("Yazmaya devam et").performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Kaydedilmemiş devir notu")
    }

    @Test fun shiftSelectorWorksAtPhoneWidthWithLargerText() {
        val week = DateUtils.getCurrentWeekMonday()
        val days = DateUtils.getWeekDays(week)
        val service = object : ShiftsService by ApiClient.shiftsService {
            override suspend fun getMyShifts(weekStartDate: String?) = Response.success(MyShiftsResponseDto(week, days.map { day ->
                MyShiftDayDto(day.isoDate, day.dayName, "GE", "22:00:00", "06:00:00", false, false, "#114591")
            }, 8.0, 48.0))
        }
        compose.setContent { LukarestTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                Box(Modifier.width(360.dp).fillMaxHeight()) {
                    StaffShiftsScreen(LukaViewModel(), {}, {}, service)
                }
            }
        } }
        compose.onNodeWithText(days[6].dayNumber).performScrollTo().performClick()
        compose.onNodeWithText("Vardiya ertesi gün biter.").performScrollTo().assertIsDisplayed()
        capture("staff-shifts-phone-large-text")
    }
}
