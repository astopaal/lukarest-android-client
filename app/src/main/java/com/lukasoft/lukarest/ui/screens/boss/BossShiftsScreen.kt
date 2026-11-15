package com.lukasoft.lukarest.ui.screens.boss

import com.lukasoft.lukarest.ui.components.LukaBottomSheet
import com.lukasoft.lukarest.ui.components.LukaChoiceRow

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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
import com.lukasoft.lukarest.ui.components.LukaSnackbarHost
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
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
    var latestWeek by remember { mutableStateOf(DateUtils.getCurrentWeekMonday()) }
    var latestWeekLoaded by remember { mutableStateOf(false) }
    val days = remember(week) { DateUtils.getWeekDays(week) }
    val dateRange = DateUtils.weekLabel(week)
    var revision by remember { mutableIntStateOf(0) }
    var matrix by remember(week, revision) { mutableStateOf<ShiftMatrixResponse?>(null) }
    var templates by remember(week, revision) { mutableStateOf(emptyList<ShiftTemplateDto>()) }
    var loadError by remember(week, revision) { mutableStateOf<String?>(null) }
    var panel by remember { mutableStateOf<ShiftPanel?>(null) }
    var saving by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var actionError by remember(panel) { mutableStateOf<String?>(null) }
    var coverageWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val latest = service.getLatestMatrix()
            latest.body()?.weekStartDate?.let {
                week = it
                latestWeek = it
            }
        } catch (_: IOException) {
            loadError = "Son planlanan hafta alınamadı."
        } finally {
            latestWeekLoaded = true
        }
    }

    LaunchedEffect(week, revision, latestWeekLoaded) {
        if (!latestWeekLoaded) return@LaunchedEffect
        loadError = null
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
        } finally {
            isRefreshing = false
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
                    val coverage = (result.body() as? ShiftMatrixCellDto)?.coverage
                    coverageWarning = coverage?.message?.takeIf { coverage.hasGap }
                    panel = null
                    if (week > latestWeek) latestWeek = week
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

    coverageWarning?.let { message ->
        AlertDialog(onDismissRequest = { coverageWarning = null }, title = { Text("Vardiyada personel boşluğu oluştu", color = PillErr.fg) },
            text = { Text(message) }, confirmButton = { TextButton(onClick = { coverageWarning = null }) { Text("Uyarıyı gördüm") } })
    }
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(BgScreenTop, BgScreenBot))
        ),
        snackbarHost = { LukaSnackbarHost(snackbar) },
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; revision++ },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
          Column(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(dateRange, style = MaterialTheme.typography.titleMedium, color = BossGradStart)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Yedi gün tek bakışta. Düzenlemek için bir güne dokun.", Modifier.weight(1f),
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
        LukaBottomSheet(
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
                            ChoiceRow("Sonraki haftayı planla", "Bir sonraki haftanın yedi günlük planını aç.", false, true) {
                                week = DateUtils.addWeeks(week, 1)
                                panel = null
                            }
                            ChoiceRow("Önceki haftayı aç", "Bir önceki haftanın planını incele.", false, true) {
                                week = DateUtils.addWeeks(week, -1)
                                panel = null
                            }
                            ChoiceRow("Son planlanan haftayı aç", "Sunucudaki en ileri haftaya dön.", false, true) {
                                week = latestWeek
                                panel = null
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
    val counts = plan.days.associateBy { it.date }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BossGradient).padding(horizontal = 10.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.dayName, style = MaterialTheme.typography.labelMedium, color = White, maxLines = 1)
                    Text("${counts[day.isoDate]?.activeStaffCount ?: 0}", style = MaterialTheme.typography.labelSmall, color = PillBlue.bg)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(plan.rows, key = { it.userId }) { person ->
                val cells = person.cells.associateBy { it.date }
                LukaCard(contentPadding = PaddingValues(10.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { onPerson(person) }.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(person.fullName, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Text("Haftalık ata", style = MaterialTheme.typography.labelMedium, color = BossGradStart)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        days.forEach { day ->
                            val cell = cells[day.isoDate]
                            val filled = cell?.shiftInstanceId != null
                            val colors = when {
                                cell?.isOffDay == true -> ShiftIzin
                                !filled -> ShiftEmpty
                                else -> {
                                    val accent = parseShiftColor(cell?.colorHex)
                                    ShiftColor(accent.copy(alpha = 0.13f), TextMain, accent.copy(alpha = 0.36f))
                                }
                            }
                            Surface(
                                onClick = { onCell(person, day) }, color = colors.bg,
                                border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                                    .semantics { contentDescription = "${person.fullName}, ${day.dayName}: ${cellSummary(cell)}" }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(when {
                                        cell?.isOffDay == true -> "İzin"
                                        !filled -> "+"
                                        else -> cell?.shortCode?.takeIf { it.isNotBlank() } ?: "V"
                                    }, style = MaterialTheme.typography.labelSmall, color = colors.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
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

    SheetHeading(panel.person.fullName, "${panel.day.dayName} · ${cellSummary(current)}")
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
    var weekdaysOnly by remember { mutableStateOf(false) }
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
            FilterChip(!weekdaysOnly, { weekdaysOnly = false }, label = { Text("7 gün · önerilen") })
            FilterChip(weekdaysOnly, { weekdaysOnly = true }, label = { Text("Hafta içi · 5 gün") })
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
        LukaCard(elevation = 0.dp) {
            Text(template?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, color = BossGradStart)
            template?.let { Text(timeRange(it.startTime, it.endTime), color = TextSoft) }
            Spacer(Modifier.height(8.dp))
            Text("${people.size} personel · ${dates.size} gün · ${people.size * dates.size} atama", color = TextMain)
            Text("${dates.first().displayDate} - ${dates.last().displayDate}", color = TextMuted)
        }
        Text("Atanacak personel", style = MaterialTheme.typography.titleSmall)
        people.forEach { person ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(person.fullName, Modifier.weight(1f), color = TextMain)
                Text("${dates.size} gün", color = TextMuted)
            }
        }
        if (occupied > 0) {
            LukaCard(elevation = 0.dp, backgroundColor = PillWarn.bg) {
                Text("$occupied dolu hücrenin üzerine yazılacak. Mevcut vardiyalar ve izin günleri değişebilir.",
                    color = PillWarn.fg, style = MaterialTheme.typography.bodyMedium)
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
    val startDate = days.first().isoDate
    val endDate = days.last().isoDate
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
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${day.displayDate} · ${day.dayName}", Modifier.weight(1f), color = BossGradStart,
                            style = MaterialTheme.typography.labelLarge)
                        if (protected) StatusPill("Korunacak", pillColor = PillNeutral)
                        else if (rule?.isOffDay == true) StatusPill("İzin günü", pillColor = PillNeutral)
                        }
                        Text(summary, color = TextMain,
                            style = MaterialTheme.typography.bodySmall)
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
private fun SheetHeading(title: String, subtitle: String) {
    LukaCard(elevation = 0.dp, backgroundColor = PillBlue.bg) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BossGradStart)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = PillBlue.fg)
    }
}

@Composable
private fun ChoiceRow(title: String, detail: String?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    LukaChoiceRow(title = title, detail = detail, selected = selected, enabled = enabled, onClick = onClick)
}

@Composable
private fun SavePlanButton(label: String, saving: Boolean, enabled: Boolean, error: String?, onClick: () -> Unit) {
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    PrimaryButton(text = if (saving) "Kaydediliyor…" else label,
        onClick = onClick, enabled = enabled && !saving, gradient = BossGradient, loading = saving)
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
