package com.lukasoft.lukarest

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lukasoft.lukarest.data.model.TaskItem
import com.lukasoft.lukarest.ui.components.PrioritySelector
import com.lukasoft.lukarest.ui.screens.staff.TaskCardItem
import com.lukasoft.lukarest.ui.theme.LukarestTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UxRefinementTest {
    @get:Rule val compose = createComposeRule()

    @Test fun priorityCanBeChosenWithoutADropdown() {
        var selected = "Medium"
        compose.setContent { LukarestTheme {
            var value by remember { mutableStateOf("Medium") }
            PrioritySelector(value) { value = it; selected = it }
        } }
        compose.onNodeWithText("Kritik").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Critical", selected) }
        compose.onNodeWithText("Kritik").assertIsSelected()
    }

    @Test fun taskTitleOpensDetailsAndPriorityIsVisible() {
        var opened = false
        compose.setContent { LukarestTheme {
            TaskCardItem(TaskItem(id = "test", title = "Dolap kontrolü", startTime = "13:00", shiftId = "", assignedTo = "me", priority = "Critical"),
                onToggle = { opened = true }, onCompleteWithMedia = {})
        } }
        compose.onNodeWithText("Kritik").assertExists()
        compose.onNodeWithText("Dolap kontrolü").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
}
