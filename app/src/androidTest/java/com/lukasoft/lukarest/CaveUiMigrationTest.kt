package com.lukasoft.lukarest

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.lukasoft.lukarest.ui.components.*
import com.lukasoft.lukarest.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CaveUiMigrationTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "caveui-$name.png")
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot()!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun primaryButtonHasButtonRoleAndKeyboardActivation() {
        var clicks = 0
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            LukarestTheme { PrimaryButton("Kaydet", { clicks++ }) }
        }
        val button = compose.onNodeWithText("Kaydet")
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        button.performClick()
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        button.performSemanticsAction(SemanticsActions.RequestFocus)
        button.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(2, clicks) }
        capture("button-focus")
    }

    @Test fun loadingButtonPreventsDuplicateActions() {
        compose.setContent { LukarestTheme { PrimaryButton("Kaydediliyor", { fail("Loading button activated") }, loading = true) } }
        compose.onNodeWithText("Kaydediliyor").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "İşlem sürüyor"))
    }

    @Test fun choicesWrapAt320DpAndDoubleFontScale() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                LukarestTheme { Surface(color = BgScreenTop) {
                    Column(Modifier.width(320.dp).padding(16.dp)) {
                        Text("[TEST] Görev seçenekleri", style = MaterialTheme.typography.titleMedium)
                        PrioritySelector("Medium", {})
                        PrimaryButton("Kurulumu tamamla ve yönetim paneline dön", {})
                    }
                } }
            }
        }
        listOf("Düşük", "Orta", "Yüksek", "Kritik").forEach { label ->
            val node = compose.onNodeWithText(label).assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("$label right bound", bounds.right <= 320.dp)
            assertTrue("$label left bound", bounds.left >= 0.dp)
            assertTrue("$label touch target", bounds.bottom - bounds.top >= 48.dp)
        }
        compose.onNodeWithText("Kurulumu tamamla ve yönetim paneline dön").assertIsDisplayed()
        capture("320dp-font200")
    }

    @Test fun prioritySelectionUpdatesSelectedState() {
        var result = "Medium"
        compose.setContent { LukarestTheme {
            var value by remember { mutableStateOf("Medium") }
            PrioritySelector(value) { value = it; result = it }
        } }
        compose.onNodeWithText("Kritik").performClick().assertIsSelected()
        compose.onNodeWithText("Orta").assertIsNotSelected()
        compose.runOnIdle { assertEquals("Critical", result) }
    }

    @Test fun radioAndCheckboxRowsExposeAndChangeTheirStates() {
        compose.setContent { LukarestTheme {
            var radio by remember { mutableStateOf(false) }
            var checked by remember { mutableStateOf(false) }
            Column {
                LukaChoiceRow("Sabah vardiyası", radio, { radio = true }, detail = "09:00 - 17:00")
                LukaCheckRow("Test personeli", checked, { checked = it })
            }
        } }
        compose.onNodeWithText("Sabah vardiyası").performClick().assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose.onNodeWithText("Test personeli").performClick().assertIsOn()
        compose.onNodeWithText("Test personeli").performClick().assertIsOff()
    }

    @Test fun searchCanBeTypedAndCleared() {
        compose.setContent { LukarestTheme {
            var query by remember { mutableStateOf("") }
            LukaSearchField(query, { query = it }, "Ada göre ara")
        } }
        compose.onNodeWithText("Ada göre ara").performTextInput("test")
        compose.onNode(hasSetTextAction()).assertTextContains("test")
        compose.onNodeWithContentDescription("Aramayı temizle").performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("Ada göre ara", "")
        compose.onNodeWithContentDescription("Aramayı temizle").assertDoesNotExist()
    }

    @Test fun bottomNavigationChangesDestinationAndSelection() {
        var route = "panel"
        val items = listOf(NavItem("panel", "Panel", Icons.Default.Home), NavItem("tasks", "Görevler", Icons.Default.List))
        compose.setContent { LukarestTheme {
            var current by remember { mutableStateOf("panel") }
            LukaBottomNav(items, current, { current = it; route = it })
        } }
        compose.onNodeWithText("Panel").assertIsSelected()
        compose.onNodeWithText("Görevler").performClick().assertIsSelected()
        compose.onNodeWithText("Panel").assertIsNotSelected()
        compose.runOnIdle { assertEquals("tasks", route) }
        capture("navigation")
    }

    @Test fun bossQrFabAndTabsHaveRealActions() {
        var qr = 0
        var route = "panel"
        compose.setContent { LukarestTheme {
            BossBottomNavWithCenterFab(
                listOf(NavItem("panel", "Panel", Icons.Default.Home)),
                listOf(NavItem("tasks", "Görevler", Icons.Default.List)),
                "panel", { route = it }, { qr++ }
            )
        } }
        compose.onNodeWithContentDescription("QR Okut").performClick()
        compose.onNodeWithText("Görevler").performClick()
        compose.runOnIdle { assertEquals(1, qr); assertEquals("tasks", route) }
    }

    @Test fun progressIsAccessibleAndClampsInvalidValues() {
        var progress by mutableFloatStateOf(Float.NaN)
        compose.setContent { LukarestTheme { LukaProgressBar(progress) } }
        fun assertProgress(expected: Float) {
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(expected, 0f..1f)))
        }
        assertProgress(0f)
        compose.runOnIdle { progress = 2f }
        assertProgress(1f)
        compose.runOnIdle { progress = -1f }
        assertProgress(0f)
    }

    @Test fun accordionAnnouncesExpansionAndSupportsKeyboard() {
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            LukarestTheme {
            var open by remember { mutableStateOf(false) }
            Column {
                LukaAccordionHeader(open, { open = !open }) { Text("Vardiyalar") }
                if (open) Text("09:00 - 17:00")
            }
        } }
        val header = compose.onNodeWithText("Vardiyalar")
        header.performClick().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Açık"))
        compose.onNodeWithText("09:00 - 17:00").assertIsDisplayed()
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        header.performSemanticsAction(SemanticsActions.RequestFocus)
        header.performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("09:00 - 17:00").assertDoesNotExist()
    }

    @Test fun emptyLoadingAndErrorStatesArePerceivableAndRetryWorks() {
        var retries = 0
        compose.setContent { LukarestTheme { Surface(color = BgScreenTop) {
            Column(Modifier.width(360.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LukaLoadingState("Görevler yükleniyor.")
                LukaEmptyState("Görev yok.", "Görev atandığında burada görünecek.")
                LukaErrorState("Görevler yüklenemedi.", { retries++ })
            }
        } } }
        compose.onNodeWithText("Görevler yükleniyor.").assertIsDisplayed()
        compose.onNodeWithText("Görev yok.").assertIsDisplayed()
        compose.onNodeWithText("Tekrar dene").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        capture("content-states")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun bottomSheetDismissesAndCallsItsAction() {
        var applied = 0
        compose.setContent { LukarestTheme {
            var open by remember { mutableStateOf(true) }
            if (open) LukaBottomSheet(onDismissRequest = { open = false }) {
                Text("Test seçim paneli", Modifier.padding(16.dp))
                TextButton(onClick = { applied++; open = false }) { Text("Uygula") }
            }
        } }
        compose.onNodeWithText("Test seçim paneli").assertIsDisplayed()
        compose.onNodeWithText("Uygula").performClick()
        compose.onNodeWithText("Test seçim paneli").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, applied) }
    }

    @Test fun timeFieldKeepsValueOnCancelAndCanClear() {
        var result = "09:30"
        compose.setContent { LukarestTheme {
            var value by remember { mutableStateOf("09:30") }
            LukaTimeField(value, { value = it; result = it }, "Başlangıç", Modifier.fillMaxWidth(), allowClear = true)
        } }
        compose.onNodeWithText("Başlangıç").performClick()
        compose.onNodeWithText("Vazgeç").performClick()
        compose.runOnIdle { assertEquals("09:30", result) }
        compose.onNodeWithText("Başlangıç").performClick()
        capture("time-picker")
        compose.onNodeWithText("Seç", substring = false).performClick()
        compose.runOnIdle { assertEquals("09:30", result) }
        compose.onNodeWithText("Başlangıç").performClick()
        compose.onNodeWithText("Temizle").performClick()
        compose.onNodeWithText("Saat seç").assertIsDisplayed()
        compose.runOnIdle { assertEquals("", result) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun bottomSheetClosesWithEscape() {
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            LukarestTheme {
                var open by remember { mutableStateOf(true) }
                if (open) LukaBottomSheet(onDismissRequest = { open = false }) {
                    TextButton(onClick = {}) { Text("Test paneli eylemi") }
                }
            }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Test paneli eylemi").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Test paneli eylemi").performKeyInput { pressKey(Key.Escape) }
        compose.onNodeWithText("Test paneli eylemi").assertDoesNotExist()
    }

    @Test fun timePickerClosesWithEscape() {
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            LukarestTheme { LukaTimeField("09:30", {}, "Saat", Modifier.fillMaxWidth()) }
        }
        compose.onNodeWithText("Saat").performClick()
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Vazgeç").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Vazgeç").performKeyInput { pressKey(Key.Escape) }
        compose.onNodeWithText("Vazgeç").assertDoesNotExist()
    }

    @Test fun snackbarActionAndDismissReturnTheCorrectResult() {
        var result: SnackbarResult? = null
        val data = object : SnackbarData {
            override val visuals = object : SnackbarVisuals {
                override val message = "Kaydedildi"
                override val actionLabel = "Geri al"
                override val withDismissAction = true
                override val duration = SnackbarDuration.Indefinite
            }
            override fun performAction() { result = SnackbarResult.ActionPerformed }
            override fun dismiss() { result = SnackbarResult.Dismissed }
        }
        compose.setContent { LukarestTheme {
            Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                LukaSnackbar(data)
            }
        } }
        compose.onNodeWithText("Geri al").performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals(SnackbarResult.ActionPerformed, result) }
        compose.onNodeWithContentDescription("Kapat").performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals(SnackbarResult.Dismissed, result) }
    }

    @Test fun topPillActionWorksWithLongTextAndLargeFont() {
        var actions = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                LukarestTheme {
                    TopPill("Test vardiya ve görev yönetimi", "Test şubesi", "T", Modifier.width(320.dp),
                        actionDescription = "Menüyü aç", onActionClick = { actions++ })
                }
            }
        }
        compose.onNodeWithContentDescription("Menüyü aç").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, actions) }
        capture("header-font200")
    }

    @Test fun keyboardTabOrderFollowsTheButtonOrder() {
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            LukarestTheme { Column {
                PrimaryButton("Birinci eylem", {})
                PrimaryButton("İkinci eylem", {})
            } }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Birinci eylem").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Birinci eylem").performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithText("İkinci eylem").assertIsFocused()
        compose.onNodeWithText("İkinci eylem").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        compose.onNodeWithText("Birinci eylem").assertIsFocused()
    }
}
