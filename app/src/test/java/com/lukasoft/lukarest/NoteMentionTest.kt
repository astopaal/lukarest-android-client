package com.lukasoft.lukarest

import com.lukasoft.lukarest.data.api.NoteMentionableUserDto
import com.lukasoft.lukarest.ui.components.activeNoteMention
import com.lukasoft.lukarest.ui.components.noteMentionIds
import com.lukasoft.lukarest.ui.components.noteDisplayText
import com.lukasoft.lukarest.ui.screens.staff.plannedShiftMinutes
import org.junit.Assert.*
import org.junit.Test

class NoteMentionTest {
    private val people = listOf(
        NoteMentionableUserDto("1", "Ali", "ali_123", false),
        NoteMentionableUserDto("2", "Ayşe", "ayse_456", true)
    )

    @Test fun detectsMentionAtCursorWithoutTreatingEmailAsMention() {
        assertEquals(8 until 11, activeNoteMention("Merhaba @al sonra", 11))
        assertEquals(0 until 1, activeNoteMention("@", 1))
        assertNull(activeNoteMention("ali@example.com", 15))
        assertNull(activeNoteMention("@ali bitti", 10))
        assertNull(activeNoteMention("@@ali", 5))
    }

    @Test fun onlyCompleteRemainingHandlesProduceRecipients() {
        assertEquals(listOf("1", "2"), noteMentionIds("@ali_123 @AYSE_456 @ali_123", people))
        assertEquals(emptyList<String>(), noteMentionIds("@ali_12 @ayse_456x mail@ali_123", people))
        assertEquals(listOf("2"), noteMentionIds("Ali kaldırıldı, (@ayse_456)", people))
        assertEquals(listOf("1"), noteMentionIds("Kontrol: @ali_123.", people))
    }

    @Test fun showsNamesAndEveryoneWithoutExposingHandleSuffixes() {
        assertEquals("@Ali, @Ayşe ve @Herkes.", noteDisplayText("@ali_123, @ayse_456 ve @all.", people))
        assertEquals("@eski personel mail@all.com", noteDisplayText("@eski.personel_ab12cd34 mail@all.com"))
    }

    @Test fun calculatesOvernightAndFractionalShifts() {
        assertEquals(480, plannedShiftMinutes("22:00:00", "06:00:00"))
        assertEquals(450, plannedShiftMinutes("09:30:00", "17:00:00"))
        assertNull(plannedShiftMinutes(null, "17:00"))
        assertNull(plannedShiftMinutes("25:00", "17:00"))
    }
}
