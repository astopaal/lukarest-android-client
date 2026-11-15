package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.ShiftTemplateDto
import com.lukasoft.lukarest.data.api.StaffItemDto
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.LukaBottomSheet
import com.lukasoft.lukarest.ui.components.LukaSearchField
import com.lukasoft.lukarest.ui.components.LukaCheckRow
import com.lukasoft.lukarest.ui.components.LukaLoadingState
import com.lukasoft.lukarest.ui.components.LukaEmptyState
import com.lukasoft.lukarest.ui.components.LukaErrorState
import com.lukasoft.lukarest.ui.theme.*
import java.util.Locale

@Composable
internal fun TaskShiftPickerSheet(
    templates: List<ShiftTemplateDto>,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (ShiftTemplateDto) -> Unit
) {
    TaskPickerSheet("Vardiya seç", "Görevin hangi vardiyada görüneceğini seç.", onDismiss) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                val selected = template.id == selectedId
                Surface(
                    onClick = { onSelect(template) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) PillBlue.bg else White,
                    border = BorderStroke(1.dp, if (selected) BtnGradStart else BorderSoft)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PickerBadge {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PillBlue.fg, modifier = Modifier.size(19.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(template.name, style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                            Text(
                                template.startTime.take(5) + " – " + template.endTime.take(5),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = "Seçili vardiya", tint = PillBlue.fg)
                        } else if (template.shortCode.isNotBlank()) {
                            StatusPill(template.shortCode)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TaskStaffPickerSheet(
    staff: List<StaffItemDto>?,
    failed: Boolean,
    selectedIds: Set<String>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(selectedIds) }
    val visibleStaff = remember(staff, query) {
        val search = query.trim()
        staff.orEmpty().filter { it.fullName.contains(search, ignoreCase = true) }
    }

    TaskPickerSheet(
        title = "Personel seç",
        subtitle = "Görevi görecek kişileri işaretle.",
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.88f).imePadding()
    ) {
        val focusManager = LocalFocusManager.current
        LukaSearchField(
            value = query,
            onValueChange = { query = it },
            label = "Ada göre ara"
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                failed -> item {
                    LukaErrorState("Personel listesi yüklenemedi.", onRetry)
                }
                staff == null -> item { LukaLoadingState("Personel listesi yükleniyor.") }
                visibleStaff.isEmpty() -> item {
                    LukaEmptyState(
                        if (staff.isEmpty()) "Seçilebilecek personel yok." else "Aramana uygun personel bulunamadı.",
                        if (staff.isEmpty()) "Şubeye personel eklendiğinde burada görünecek." else "Başka bir adla ara veya aramayı temizle."
                    )
                }
                else -> items(visibleStaff, key = { it.userId }) { person ->
                    val selected = person.userId in selection
                    LukaCheckRow(
                        title = person.fullName,
                        checked = selected,
                        onCheckedChange = { selection = if (it) selection + person.userId else selection - person.userId },
                        leadingContent = {
                            PickerBadge {
                                val initials = person.fullName.split(' ').filter { it.isNotBlank() }.take(2)
                                    .joinToString("") { it.take(1) }.uppercase(Locale.forLanguageTag("tr-TR"))
                                Text(initials, style = MaterialTheme.typography.labelMedium, color = PillBlue.fg)
                            }
                        }
                    )
                }
            }
        }
        HorizontalDivider(color = ProgTrack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("%d seçili".format(selection.size), style = MaterialTheme.typography.labelLarge, color = TextMuted)
            PrimaryButton(
                text = "Uygula",
                onClick = {
                    focusManager.clearFocus()
                    onApply(selection)
                },
                enabled = staff != null && !failed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskPickerSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    LukaBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BgScreenTop,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BossGradStart)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Seçimi kapat", tint = PillBlue.fg)
                }
            }
            content()
        }
    }
}

@Composable
private fun PickerBadge(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.size(36.dp).background(PillBlue.bg, CircleShape), contentAlignment = Alignment.Center, content = content)
}
