package com.lukasoft.lukarest

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.AttendanceDayDto
import com.lukasoft.lukarest.data.api.AttendanceService
import com.lukasoft.lukarest.data.api.MeDto
import com.lukasoft.lukarest.data.model.*
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.components.PriorityField
import com.lukasoft.lukarest.ui.screens.staff.StaffTaskCompleteScreen
import com.lukasoft.lukarest.ui.theme.LukarestTheme
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.After
import retrofit2.Response

class TaskUiContractTest {
    @get:Rule val compose = createComposeRule()
    @After fun resetSession() { com.lukasoft.lukarest.data.store.AuthStore.logout() }

    private fun capture(name: String) {
        val resolver = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "lukarest-$name.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/lukarest-verification")
        })!!
        resolver.openOutputStream(uri)!!.use { output ->
            compose.waitForIdle()
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun showTask(proof: String, status: TaskStatus = TaskStatus.PENDING, clockedIn: Boolean = true) {
        val vm = LukaViewModel()
        vm.setTasks(listOf(TaskItem(id = "proof-test", title = "Kanıt kontrolü", startTime = "10:00", shiftId = "", assignedTo = "me",
            mediaRequired = true, allowedProofType = proof, status = status, reviewNote = if (status == TaskStatus.REJECTED) "Yeni fotoğraf gerekli" else null)))
        val attendance = object : AttendanceService by ApiClient.attendanceService {
            override suspend fun getMyAttendance(date: String?) = Response.success(
                AttendanceDayDto("shift", "2026-11-21", "09:00:00", "17:00:00", false,
                    if (clockedIn) "2026-11-21T06:00:00Z" else null, null)
            )
        }
        compose.setContent { LukarestTheme { StaffTaskCompleteScreen("proof-test", vm, {}, attendance) } }
    }

    @Test fun photoProofHasOnlyTheCameraAction() {
        showTask("PhotoOnly")
        compose.onNodeWithText("Kamerayı aç").assertExists().assertHasClickAction()
        compose.onNodeWithText("Video çek").assertDoesNotExist()
        compose.onNodeWithText("Fotoğraf seç").assertDoesNotExist()
        compose.onNodeWithText("Medya seç").assertDoesNotExist()
        capture("photo-proof")
    }

    @Test fun videoProofHasOnlyVideoCapture() {
        showTask("VideoOnly")
        compose.onNodeWithText("Video çek").assertExists().assertHasClickAction()
        compose.onNodeWithText("Kamerayı aç").assertDoesNotExist()
        compose.onNodeWithText("Video seç").assertDoesNotExist()
        capture("video-proof")
    }

    @Test fun approvalPendingCannotBeSubmittedAgain() {
        showTask("PhotoOnly", TaskStatus.AWAITING_APPROVAL)
        compose.onNode(hasText("Görevi Tamamla") and hasClickAction()).assertIsNotEnabled()
    }

    @Test fun rejectedTaskShowsTheReviewNoteAndAllowsResubmission() {
        showTask("PhotoOnly", TaskStatus.REJECTED)
        compose.onNodeWithText("Yönetici notu: Yeni fotoğraf gerekli").assertExists()
        compose.onNode(hasText("Görevi Tamamla") and hasClickAction()).assertIsEnabled()
    }

    @Test fun taskActionsRequireClockIn() {
        showTask("PhotoOnly", clockedIn = false)
        compose.onNodeWithText("Göreve başlamak için önce Bugün ekranından vardiyanızı başlatın.").assertExists()
        compose.onNodeWithText("Kamerayı aç").assertIsNotEnabled()
        compose.onNode(hasText("Görevi Tamamla") and hasClickAction()).assertIsNotEnabled()
    }

    @Test fun prioritySelectionUpdatesTheDisplayedValue() {
        compose.setContent {
            var priority by remember { mutableStateOf("Medium") }
            LukarestTheme { PriorityField(priority) { priority = it } }
        }
        compose.onNodeWithText("Öncelik: Orta").performClick()
        compose.onNodeWithText("Kritik").performClick()
        compose.onNodeWithText("Öncelik: Kritik").assertExists()
    }

    @Test fun ownerKeepsTheManagementBarInTheOperationalNotebook() {
        com.lukasoft.lukarest.data.store.AuthStore.setRole(com.lukasoft.lukarest.data.store.AppRole.BOSS)
        lateinit var navigation: androidx.navigation.NavHostController
        compose.setContent {
            navigation = androidx.navigation.compose.rememberNavController()
            LukarestTheme { com.lukasoft.lukarest.ui.navigation.AppNavigation(navController = navigation, viewModel = LukaViewModel()) }
        }
        compose.waitUntil(10000) { navigation.currentDestination?.route == "boss/dashboard" }
        compose.runOnIdle { navigation.navigate("boss/notes") }
        compose.onNodeWithText("Panel").assertExists()
        compose.onNodeWithText("Vardiyalarım").assertDoesNotExist()
        compose.runOnIdle { navigation.popBackStack() }
        compose.onNodeWithText("Panel").assertExists()
    }

    @Test fun notificationDrawerPaginatesMutesAndSelectsTheDelivery() {
        val owner = MeDto("owner", "Test Patron", null, "", "", "", "CompanyOwner", "company", "Test", "branch", "Şube", "Owner")
        AuthStore.updateUser(owner)
        var mutedCategories = emptyList<String>()
        var selected: com.lukasoft.lukarest.data.api.PushPayload? = null
        val service = object : com.lukasoft.lukarest.data.api.NotificationsService {
            override suspend fun list(page: Int, pageSize: Int) = retrofit2.Response.success(com.lukasoft.lukarest.data.api.NotificationPageDto(
                listOf(com.lukasoft.lukarest.data.api.NotificationItemDto("delivery-$page", "task.assigned", "task-$page", "task", "2026-11-12T10:00:00Z", null, "Kontrol kartı $page")), page == 1, mutedCategories))
            override suspend fun open(id: String) = retrofit2.Response.success<Unit>(null)
            override suspend fun mute(body: com.lukasoft.lukarest.data.api.MuteNotificationsBody): retrofit2.Response<Unit> {
                mutedCategories = if (body.muted) mutedCategories + body.category else mutedCategories - body.category
                return retrofit2.Response.success(null)
            }
        }
        compose.setContent {
            LukarestTheme {
                com.lukasoft.lukarest.ui.components.NotificationDrawer(onSelect = { selected = it }, service = service) { open ->
                    androidx.compose.material3.Button(onClick = open) { androidx.compose.material3.Text("Bildirimleri aç") }
                }
            }
        }
        compose.onNodeWithText("Bildirimleri aç").performClick()
        compose.onNodeWithText("Kontrol kartı 1").assertExists()
        compose.onNodeWithText("Kapat").performClick()
        compose.onNodeWithText("Bildirimleri aç").performClick()
        compose.onNodeWithText("Bildirimleri yenile").performScrollTo().performClick()
        compose.onNodeWithText("Daha fazla bildirim yükle").performScrollTo().performClick()
        compose.onNodeWithText("Kontrol kartı 2").assertExists()
        compose.onNodeWithText("Talep: Bugün sustur").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(listOf("request"), mutedCategories) }
        compose.onNodeWithText("Talep: Yeniden aç").performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(mutedCategories.isEmpty()) }
        compose.runOnIdle { AuthStore.updateUser(owner.copy(userId = "worker", globalRole = "Staff", roleInBranch = "Waiter")) }
        compose.onNodeWithText("Bildirim tercihleri").assertDoesNotExist()
        capture("notification-drawer")
        compose.onNodeWithText("Kapat").performClick()
        compose.onNodeWithText("Bildirimleri aç").performClick()
        compose.onNodeWithText("Daha fazla bildirim yükle").performScrollTo().performClick()
        compose.onNodeWithText("Kontrol kartı 2").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals("delivery-2", selected?.notificationId) }
    }

    @Test fun staffHeaderOpensNotificationsWithoutAnOffboardingAction() {
        var opened = false
        compose.setContent {
            LukarestTheme { com.lukasoft.lukarest.ui.screens.staff.StaffTodayScreen(
                onOpenNotifications = { opened = true }, viewModel = LukaViewModel(), onNavigateToTasks = {}, onNavigateToTaskComplete = {}) }
        }
        compose.onNodeWithContentDescription("Bildirimleri aç").performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(opened) }
        compose.onNodeWithText("Çıkış yap").assertDoesNotExist()
        compose.onNodeWithContentDescription("QR Kodu").assertDoesNotExist()
    }

    @Test fun photoButtonLaunchesTheSystemCaptureIntent() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, android.Manifest.permission.CAMERA)
        showTask("PhotoOnly")
        compose.onNodeWithText("Kamerayı aç").performClick()
        compose.waitUntil(10000) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities"))
                .bufferedReader().use { it.readText() }.contains("android.media.action.IMAGE_CAPTURE")
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
    }

    @Test fun stockRequestSheetValidatesInputsAndCanBeDismissed() {
        compose.setContent {
            LukarestTheme { com.lukasoft.lukarest.ui.screens.staff.StaffRequestsScreen(
                viewModel = LukaViewModel(), onNavigateToShifts = {}, onNavigateToShiftChange = {}, onNavigateToLeaveChange = {}) }
        }
        compose.onNodeWithText("Yeni talep oluştur").performClick()
        compose.onNodeWithText("İhtiyaç veya sorun bildir").performClick()
        compose.onNodeWithText("Talebi gönder").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Ürün veya ekipman").performScrollTo().performTextInput("Bardak")
        compose.onNodeWithText("Açıklama").performScrollTo().performTextInput("Bir koli bardak gerekiyor")
        compose.onNodeWithText("Öncelik: Orta").performScrollTo().performClick()
        compose.onNodeWithText("Yüksek").performClick()
        compose.onNodeWithText("Açıklama").assertIsNotFocused()
        compose.onNodeWithText("Talebi gönder").performScrollTo().assertIsEnabled()
        capture("stock-request-sheet")
        compose.onNodeWithText("Vazgeç").performScrollTo().performClick()
        compose.onNodeWithText("Operasyon talebi").assertDoesNotExist()
    }
}
