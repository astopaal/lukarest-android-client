package com.lukasoft.lukarest.ui.screens.boss

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.*
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.util.Locale

private sealed interface ShiftPanel {
    data class Cell(val person: ShiftMatrixRowDto, val day: DateUtils.DayInfo) : ShiftPanel
    data class Repeat(val person: ShiftMatrixRowDto) : ShiftPanel
    data object Tools : ShiftPanel
    data object Bulk : ShiftPanel
    data object Clear : ShiftPanel
}

@Composable
fun BossShiftsScreen(onNavigateToAddShift: () -> Unit) {
    val branchId by AuthStore.activeBranchId.collectAsState()
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        primary = BossGradStart,
        onPrimary = White,
        secondaryContainer = PillBlue.bg,
        onSecondaryContainer = PillBlue.fg,
        surface = White,
        onSurface = TextMain,
        onSurfaceVariant = TextSoft,
        outline = BorderSoft,
        error = PillErr.fg
    )) {
        key(branchId) { WeeklyShiftPlanner(onNavigateToAddShift) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyShiftPlanner(onNavigateToAddShift: () -> Unit) {
    val service = ApiClient.shiftsService
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var week by remember { mutableStateOf(DateUtils.getCurrentWeekMonday()) }
    val days = remember(week) { DateUtils.getWeekDays(week) }
    val dateRange = "${days.first().displayDate} – ${days.last().displayDate}"
    var revision by remember { mutableIntStateOf(0) }
    var matrix by remember(week, revision) { mutableStateOf<ShiftMatrixResponse?>(null) }
    var templates by remember(week, revision) { mutableStateOf(emptyList<ShiftTemplateDto>()) }
    var loadError by remember(week, revision) { mutableStateOf<String?>(null) }
    var panel by remember { mutableStateOf<ShiftPanel?>(null) }
    var saving by remember { mutableStateOf(false) }
    var actionError by remember(panel) { mutableStateOf<String?>(null) }

    LaunchedEffect(week, revision) {
        try {
            val plan = service.getMatrix(week)
            val options = service.getTemplates()
            if (plan.isSuccessful && plan.body() != null && options.isSuccessful) {
                templates = options.body().orEmpty()
                matrix = plan.body()
            } else {
                loadError = "Haftalık plan alınamadı."
            }
        } catch (_: IOException) {
            loadError = "Bağlantı kurulamadı. Tekrar deneyin."
        }
    }

    fun save(message: String, request: suspend () -> Response<*>) {
        if (saving) return
        saving = true
        actionError = null
        scope.launch {
            val feedback = try {
                val result = request()
                if (result.isSuccessful) {
                    panel = null
                    revision++
                    if (result.body() is Int) "${result.body()} gün güncellendi." else message
                } else {
                    actionError = "Değişiklik kaydedilemedi (${result.code()})."
                    null
                }
            } catch (_: IOException) {
                actionError = "Sunucudan yanıt alınamadı. Planı yenileyip sonucu kontrol edin."
                null
            } finally {
                saving = false
            }
            if (feedback != null) snackbar.showSnackbar(feedback)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(BgScreenTop, BgScreenBot))
        ),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TopPill(
                    title = "Vardiya Planı",
                    subtitle = "Haftalık çalışma çizelgesi",
                    initials = "P",
                    gradient = BossGradient,
                    actionIcon = Icons.Default.Add,
                    onActionClick = onNavigateToAddShift
                )
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LukaCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { week = DateUtils.addWeeks(week, -1) }) {
                        Icon(Icons.Default.ChevronLeft, "Önceki hafta", tint = BossGradStart)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Haftalık görünüm", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(dateRange, style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center, color = BossGradStart)
                    }
                    IconButton(onClick = { week = DateUtils.addWeeks(week, 1) }) {
                        Icon(Icons.Default.ChevronRight, "Sonraki hafta", tint = BossGradStart)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hücreye dokunarak düzenle. Diğer günler için tabloyu kaydır.", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall, color = TextSoft)
                OutlinedButton(enabled = matrix != null, onClick = { panel = ShiftPanel.Tools }) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Plan araçları")
                }
            }
            val plan = matrix
            when {
                loadError != null -> Column {
                    Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { revision++ }) { Text("Tekrar dene") }
                }
                plan == null -> LinearProgressIndicator(Modifier.fillMaxWidth(), trackColor = ProgTrack)
                plan.rows.isEmpty() -> LukaCard {
                    Text("Bu şubede planlanacak personel yok.", color = TextSoft)
                }
                else -> ShiftGrid(
                    plan, days,
                    onCell = { person, day -> panel = ShiftPanel.Cell(person, day) },
                    onPerson = { panel = ShiftPanel.Repeat(it) }
                )
            }
        }
    }

    val currentPanel = panel
    if (currentPanel == ShiftPanel.Clear) {
        AlertDialog(
            onDismissRequest = { if (!saving) panel = null },
            title = { Text("Haftalık planı temizle") },
            text = {
                Column {
                    Text("$dateRange için tüm vardiya ve izin atamaları temizlenir. Puantajı olan kayıtlar da etkilenebilir.")
                    actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    save("Haftalık plan temizlendi.") { service.clearMatrix(ClearShiftMatrixCommand(week)) }
                }) { Text(if (saving) "Temizleniyor…" else "Planı temizle") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { panel = null }) { Text("Vazgeç") }
            }
        )
    } else if (currentPanel != null) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!saving) panel = null },
            sheetState = sheet,
            containerColor = BgScreenTop,
            contentColor = TextMain,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            key(currentPanel) {
                Column(
                    Modifier.fillMaxWidth().weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentPanel) {
                        is ShiftPanel.Cell -> CellEditor(currentPanel, templates, saving, actionError) { command ->
                            save("Vardiya kaydedildi.") { service.assignCell(command) }
                        }
                        is ShiftPanel.Repeat -> RepeatEditor(currentPanel.person, days, templates, dateRange, saving, actionError) { command ->
                            save("Tekrarlayan düzen uygulandı.") { service.materializeCycle(command) }
                        }
                        ShiftPanel.Bulk -> BulkEditor(matrix?.rows.orEmpty(), templates, week, dateRange, saving, actionError) { command ->
                            save("Vardiyalar atandı.") { service.bulkAssign(command) }
                        }
                        ShiftPanel.Tools -> {
                            SheetHeading("Plan araçları", dateRange)
                            ChoiceRow("Toplu vardiya ata", "Seçilen ekibe aynı vardiyayı yaz.", false, true) {
                                panel = ShiftPanel.Bulk
                            }
                            ChoiceRow("Haftayı yenile", "Sunucudaki son planı getir.", false, true) {
                                panel = null
                                revision++
                            }
                            ChoiceRow("Haftalık planı temizle", "Bu haftanın vardiya ve izin atamalarını kaldır.", false, true) {
                                panel = ShiftPanel.Clear
                            }
                        }
                        ShiftPanel.Clear -> Unit
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ShiftGrid(
    plan: ShiftMatrixResponse,
    days: List<DateUtils.DayInfo>,
    onCell: (ShiftMatrixRowDto, DateUtils.DayInfo) -> Unit,
    onPerson: (ShiftMatrixRowDto) -> Unit
) {
    val dayWidth = 52.dp
    val teamWidth = 104.dp
    val counts = plan.days.associateBy { it.date }
    val today = DateUtils.getTurkeyTodayIso()
    LukaCard(contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(teamWidth + dayWidth * days.size.toFloat())) {
                Row(Modifier.background(BossGradient), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ekip", Modifier.width(teamWidth).padding(12.dp), color = White,
                        style = MaterialTheme.typography.titleSmall)
                    days.forEach { day ->
                        Column(
                            Modifier.width(dayWidth).heightIn(min = 58.dp).padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("${day.dayName} ${day.dayNumber}", style = MaterialTheme.typography.labelMedium,
                                color = White)
                            val count = "${counts[day.isoDate]?.activeStaffCount ?: 0} kişi"
                            if (day.isoDate == today) StatusPill(count)
                            else Text(count, style = MaterialTheme.typography.labelSmall, color = PillBlue.bg)
                        }
                    }
                }
                LazyColumn {
                    items(plan.rows, key = { it.userId }) { person ->
                        val cells = person.cells.associateBy { it.date }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                Modifier.width(teamWidth).heightIn(min = 56.dp)
                                    .clickable { onPerson(person) }.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(person.fullName, style = MaterialTheme.typography.labelLarge, color = TextMain,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Düzen seç", style = MaterialTheme.typography.labelSmall, color = BossGradStart)
                            }
                            days.forEach { day ->
                                val cell = cells[day.isoDate]
                                val filled = cell?.shiftInstanceId != null
                                val colors = when {
                                    cell?.isOffDay == true -> ShiftIzin
                                    !filled -> ShiftEmpty
                                    cell?.shortCode.isNullOrBlank() && cell?.colorHex.isNullOrBlank() -> ShiftOzel
                                    else -> {
                                        val accent = parseShiftColor(cell?.colorHex)
                                        ShiftColor(accent.copy(alpha = 0.13f), TextMain, accent.copy(alpha = 0.36f))
                                    }
                                }
                                Surface(
                                    onClick = { onCell(person, day) },
                                    color = colors.bg,
                                    border = BorderStroke(1.dp, colors.border),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(dayWidth).height(52.dp).padding(3.dp)
                                        .semantics { contentDescription = "${person.fullName}, ${day.displayDate}: ${cellSummary(cell)}" }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            when {
                                                cell?.isOffDay == true -> "İzin"
                                                !filled -> "+"
                                                else -> cell?.shortCode?.takeIf { it.isNotBlank() } ?: cell?.startTime?.take(5) ?: "Vardiya"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = BgBoard)
                    }
                }
            }
        }
    }
}

@Composable
private fun CellEditor(
    panel: ShiftPanel.Cell,
    templates: List<ShiftTemplateDto>,
    saving: Boolean,
    error: String?,
    onSave: (AssignShiftCellCommand) -> Unit
) {
    val current = panel.person.cells.firstOrNull { it.date == panel.day.isoDate }
    val matchingTemplate = templates.singleOrNull {
        current?.isOffDay == false && it.shortCode == current.shortCode &&
            it.startTime.take(5) == current.startTime?.take(5) && it.endTime.take(5) == current.endTime?.take(5)
    }
    val initial = when {
        current?.isOffDay == true -> "off"
        matchingTemplate != null -> matchingTemplate.id
        current?.startTime != null && current.endTime != null -> "custom"
        else -> null
    }
    var selection by remember { mutableStateOf(initial) }
    val initialStart = current?.startTime?.take(5) ?: "09:00"
    val initialEnd = current?.endTime?.take(5) ?: "17:00"
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }

    SheetHeading(panel.person.fullName, "${panel.day.dayName}, ${panel.day.displayDate} · ${cellSummary(current)}")
    templates.forEach { template ->
        ChoiceRow(template.name, timeRange(template.startTime, template.endTime), selection == template.id, !saving) {
            selection = template.id
        }
    }
    ChoiceRow("İzin günü", "Bu gün için çalışma planlanmaz.", selection == "off", !saving) { selection = "off" }
    ChoiceRow("Özel saat", "Bu güne özel başlangıç ve bitiş seç.", selection == "custom", !saving) {
        selection = "custom"
    }
    if (selection == "custom") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftTimeButton("Başlangıç", start, !saving, Modifier.weight(1f)) { start = it }
            ShiftTimeButton("Bitiş", end, !saving, Modifier.weight(1f)) { end = it }
        }
        if (end < start) Text("Vardiya ertesi gün biter.", style = MaterialTheme.typography.bodySmall)
        if (end == start) Text("Başlangıç ve bitiş farklı olmalı.", color = MaterialTheme.colorScheme.error)
    }

    val changed = selection != initial || (selection == "custom" && (start != initialStart || end != initialEnd))
    SavePlanButton("Vardiyayı kaydet", saving, selection != null && changed && (selection != "custom" || start != end), error) {
        val chosen = selection ?: return@SavePlanButton
        onSave(AssignShiftCellCommand(
            userId = panel.person.userId,
            date = panel.day.isoDate,
            shiftTemplateId = chosen.takeUnless { it == "custom" || it == "off" },
            customStartTime = if (chosen == "custom") "$start:00" else null,
            customEndTime = if (chosen == "custom") "$end:00" else null,
            isOffDay = chosen == "off"
        ))
    }
}

@Composable
private fun ShiftTimeButton(
    label: String,
    value: String,
    enabled: Boolean,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    OutlinedButton(enabled = enabled, modifier = modifier, onClick = {
        TimePickerDialog(context, { _, hour, minute ->
            onSelect(String.format(Locale.ROOT, "%02d:%02d", hour, minute))
        }, value.substringBefore(':').toInt(), value.substringAfter(':').toInt(), true).show()
    }) { Text("$label $value") }
}

@Composable
private fun BulkEditor(
    rows: List<ShiftMatrixRowDto>,
    templates: List<ShiftTemplateDto>,
    week: String,
    dateRange: String,
    saving: Boolean,
    error: String?,
    onSave: (BulkAssignShiftsCommand) -> Unit
) {
    var templateId by remember { mutableStateOf<String?>(null) }
    var weekdaysOnly by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var showPreview by remember { mutableStateOf(false) }
    val template = templates.firstOrNull { it.id == templateId }
    val people = rows.filter { it.userId in selected }
    val dates = DateUtils.getWeekDays(week).take(if (weekdaysOnly) 5 else 7)
    val occupied = people.sumOf { person ->
        dates.count { day -> person.cells.any { it.date == day.isoDate && it.shiftInstanceId != null } }
    }

    SheetHeading("Toplu vardiya ata", dateRange)
    if (templates.isEmpty()) Text("Önce bir vardiya şablonu oluştur.")
    if (rows.isEmpty()) Text("Bu şubede planlanacak personel yok.")
    if (!showPreview) {
        Text("Vardiya", style = MaterialTheme.typography.titleSmall)
        templates.forEach {
            ChoiceRow(it.name, timeRange(it.startTime, it.endTime), templateId == it.id, true) { templateId = it.id }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(weekdaysOnly, { weekdaysOnly = true }, label = { Text("Hafta içi") })
            FilterChip(!weekdaysOnly, { weekdaysOnly = false }, label = { Text("Tüm hafta") })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Personel · ${selected.size} seçili", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            TextButton(enabled = rows.isNotEmpty(), onClick = {
                selected = if (selected.size == rows.size) emptySet() else rows.map { it.userId }.toSet()
            }) { Text(if (rows.isNotEmpty() && selected.size == rows.size) "Seçimi kaldır" else "Tümünü seç") }
        }
        rows.forEach { person ->
            ChoiceRow(person.fullName, null, person.userId in selected, true) {
                selected = if (person.userId in selected) selected - person.userId else selected + person.userId
            }
        }
        SavePlanButton("Önizle", false, template != null && selected.isNotEmpty(), null) { showPreview = true }
    } else {
        Text("${template?.name} · ${people.size} kişi · ${people.size * dates.size} hücre",
            style = MaterialTheme.typography.titleMedium)
        Text(dates.joinToString(" · ") { it.dayName }, color = TextSoft)
        Text(people.joinToString(", ") { it.fullName }, color = TextSoft)
        if (occupied > 0) {
            LukaCard(elevation = 0.dp, backgroundColor = PillWarn.bg) {
                Text("$occupied dolu hücrenin üzerine yazılacak. Mevcut vardiyalar ve izin günleri değişebilir.",
                    color = PillWarn.fg, style = MaterialTheme.typography.bodyMedium)
            }
            dates.forEach { day ->
                val affected = people.filter { person -> person.cells.any { it.date == day.isoDate && it.shiftInstanceId != null } }
                if (affected.isNotEmpty()) Text("${day.dayName}: ${affected.joinToString(", ") { it.fullName }}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(enabled = !saving, onClick = { showPreview = false }) { Text("Seçimi değiştir") }
        SavePlanButton("Atamayı onayla", saving, template != null && selected.isNotEmpty(), error) {
            val chosen = template ?: return@SavePlanButton
            onSave(BulkAssignShiftsCommand(chosen.id, week, if (weekdaysOnly) "WeekdaysOnly" else "EntireWeek", selected.toList()))
        }
    }
}

@Composable
private fun RepeatEditor(
    person: ShiftMatrixRowDto,
    days: List<DateUtils.DayInfo>,
    templates: List<ShiftTemplateDto>,
    dateRange: String,
    saving: Boolean,
    error: String?,
    onSave: (MaterializeCycleCommand) -> Unit
) {
    var cycles by remember { mutableStateOf<List<ShiftCycleDto>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<ShiftCycleDto?>(null) }
    var firstDay by remember { mutableIntStateOf(1) }
    var chooseStart by remember { mutableStateOf(false) }
    var chooseDates by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(days.first().isoDate) }
    var endDate by remember { mutableStateOf(days.last().isoDate) }
    val byTemplateId = templates.associateBy { it.id }
    val byDate = person.cells.associateBy { it.date }

    LaunchedEffect(retry) {
        try {
            val result = ApiClient.shiftsService.getCycles()
            if (result.isSuccessful) cycles = result.body().orEmpty().filter { it.isActive }
            else loadError = "Tekrarlayan düzenler alınamadı."
        } catch (_: IOException) {
            loadError = "Sunucuya ulaşılamadı."
        }
    }

    SheetHeading("Tekrarlayan düzen", "${person.fullName} · $dateRange")
    val options = cycles
    val cycle = chosen
    when {
        loadError != null -> {
            Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { loadError = null; retry++ }) { Text("Tekrar dene") }
        }
        options == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
        options.isEmpty() -> Text("Kullanılabilir tekrarlayan düzen bulunamadı.")
        cycle == null -> options.forEach { option ->
            val summary = option.rules.sortedBy { it.dayIndex }.take(4).joinToString(" → ") {
                ruleTitle(it, byTemplateId)
            } + if (option.rules.size > 4) " …" else ""
            ChoiceRow(option.name, summary, false, true) {
                chosen = option
                firstDay = 1
                chooseStart = false
            }
        }
        else -> {
            val byIndex = cycle.rules.associateBy { it.dayIndex }
            Text(cycle.name, style = MaterialTheme.typography.titleMedium)
            TextButton(enabled = !saving, onClick = { chosen = null }) { Text("Düzeni değiştir") }
            Text("İlk gün · ${ruleTitle(byIndex[firstDay], byTemplateId)}")
            TextButton(enabled = !saving, onClick = { chooseStart = !chooseStart }) {
                Text(if (chooseStart) "Başlangıç seçimini kapat" else "Başlangıcı değiştir")
            }
            if (chooseStart) cycle.rules.sortedBy { it.dayIndex }.forEach { rule ->
                ChoiceRow("${rule.dayIndex}. gün · ${ruleTitle(rule, byTemplateId)}", null,
                    firstDay == rule.dayIndex, !saving) {
                    firstDay = rule.dayIndex
                    chooseStart = false
                }
            }
            TextButton(enabled = !saving, onClick = { chooseDates = !chooseDates }) {
                Text(if (chooseDates) "Tarih seçimini kapat" else "Tarih aralığını değiştir")
            }
            if (chooseDates) {
                ShiftDateButton("Başlangıç", startDate, !saving) {
                    startDate = it
                    if (endDate < it) endDate = it
                }
                ShiftDateButton("Bitiş", endDate, !saving) { endDate = it }
            }
            val previewDays = remember(startDate, endDate) {
                DateUtils.getWeekDays(startDate).filter { it.isoDate <= endDate }
            }
            val validRange = endDate >= startDate
            val extendsBeyondWeek = startDate < days.first().isoDate || endDate > days.last().isoDate
            if (!validRange) Text("Bitiş tarihi başlangıçtan önce olamaz.", color = MaterialTheme.colorScheme.error)
            Text("Plan önizlemesi", style = MaterialTheme.typography.titleSmall)
            previewDays.forEachIndexed { offset, day ->
                val rule = byIndex[(firstDay - 1 + offset) % cycle.cycleLengthDays + 1]
                val existing = byDate[day.isoDate]
                val protected = existing?.isOverridden == true
                val summary = if (protected) cellSummary(existing) else ruleTitle(rule, byTemplateId)
                Surface(
                    color = White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSoft)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day.displayDate, Modifier.width(54.dp), color = BossGradStart,
                            style = MaterialTheme.typography.labelMedium)
                        Text(summary, Modifier.weight(1f), color = TextMain,
                            style = MaterialTheme.typography.bodySmall)
                        if (protected) StatusPill("Korunacak", pillColor = PillNeutral)
                        else if (rule?.isOffDay == true) StatusPill("İzin günü", pillColor = PillNeutral)
                    }
                }
            }
            val canChange = validRange && (extendsBeyondWeek || previewDays.any { byDate[it.isoDate]?.isOverridden != true })
            if (validRange && !canChange) Text("Bu aralıktaki tüm hücreler elle düzenlenmiş ve korunuyor.", color = TextSoft)
            if (extendsBeyondWeek) {
                Text("Önizleme ilk 7 günü gösterir. Açık hafta dışındaki elle düzenlemeleri sunucu uygulama sırasında korur.", color = TextSoft)
            }
            if (previewDays.any { day ->
                    byDate[day.isoDate]?.let { it.shiftInstanceId != null && !it.isOverridden } == true
                }) {
                Text("Korunacak olarak işaretlenmeyen mevcut vardiyalar güncellenir.", color = TextSoft)
            }
            SavePlanButton(if (extendsBeyondWeek) "Seçilen tarihlere uygula" else "Planı uygula", saving, canChange, error) {
                onSave(MaterializeCycleCommand(cycle.id, person.userId, startDate, endDate, firstDay))
            }
        }
    }
}

@Composable
private fun ShiftDateButton(label: String, value: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
        val (year, month, day) = value.split('-').map { it.toInt() }
        DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
            onSelect(String.format(Locale.ROOT, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay))
        }, year, month - 1, day).show()
    }) { Text("$label · ${DateUtils.getWeekDays(value).first().displayDate}") }
}

@Composable
private fun SheetHeading(title: String, subtitle: String) {
    LukaCard(elevation = 0.dp, backgroundColor = PillBlue.bg) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BossGradStart)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = PillBlue.fg)
    }
}

@Composable
private fun ChoiceRow(title: String, detail: String?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (selected) PillBlue.bg else White,
        border = BorderStroke(1.dp, if (selected) BossGradEnd else BorderSoft),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) BossGradStart else TextMain)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSoft)
            }
            if (selected) Icon(Icons.Default.Check, contentDescription = "Seçili",
                tint = BossGradStart, modifier = Modifier.padding(start = 8.dp).size(20.dp))
        }
    }
}

@Composable
private fun SavePlanButton(label: String, saving: Boolean, enabled: Boolean, error: String?, onClick: () -> Unit) {
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    PrimaryButton(text = if (saving) "Kaydediliyor…" else label,
        onClick = onClick, enabled = enabled && !saving, gradient = BossGradient)
}

private fun timeRange(start: String?, end: String?) = "${start.orEmpty().take(5)}–${end.orEmpty().take(5)}"

private fun cellSummary(cell: ShiftMatrixCellDto?) = when {
    cell?.shiftInstanceId == null -> "Vardiya yok"
    cell.isOffDay -> "İzin günü"
    else -> timeRange(cell.startTime, cell.endTime)
}

private fun ruleTitle(rule: CycleDayRuleDto?, templates: Map<String, ShiftTemplateDto>): String {
    if (rule == null) return "Vardiya bilgisi yok"
    if (rule.isOffDay) return "İzin"
    val template = rule.shiftTemplateId?.let(templates::get)
    val start = rule.customStartTime ?: template?.startTime
    val end = rule.customEndTime ?: template?.endTime
    if (start == null || end == null) return template?.name ?: "Vardiya"
    return listOfNotNull(template?.name, timeRange(start, end)).joinToString(" · ")
}

private fun parseShiftColor(hex: String?): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(ShiftSabah.fg)
