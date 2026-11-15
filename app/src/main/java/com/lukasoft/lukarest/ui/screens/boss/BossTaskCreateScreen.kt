package com.lukasoft.lukarest.ui.screens.boss

import com.lukasoft.lukarest.ui.components.LukaChoiceGroup

import com.lukasoft.lukarest.ui.components.LukaFeedback
import com.lukasoft.lukarest.ui.components.LukaTimeField

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.CreateTaskDefinitionCommand
import com.lukasoft.lukarest.data.api.ShiftTemplateDto
import com.lukasoft.lukarest.data.api.TaskDefinitionDto
import com.lukasoft.lukarest.data.api.StaffItemDto
import com.lukasoft.lukarest.data.model.TaskRepeat
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BossTaskCreateScreen(
    onNavigateBack: () -> Unit,
    definition: TaskDefinitionDto? = null
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val descriptionFocus = remember { FocusRequester() }
    var step by remember { mutableIntStateOf(1) }
    var shiftPickerOpen by remember { mutableStateOf(false) }
    var staffPickerOpen by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(definition?.title.orEmpty()) }
    var description by remember { mutableStateOf(definition?.description.orEmpty()) }
    var startTime by remember { mutableStateOf(definition?.targetStartTime?.take(5).orEmpty()) }
    var endTime by remember { mutableStateOf(definition?.targetEndTime?.take(5).orEmpty()) }
    var repeat by remember { mutableStateOf(when (definition?.recurrenceType) { "Daily" -> TaskRepeat.DAILY; "CycleBound" -> TaskRepeat.CYCLE_BOUND; else -> TaskRepeat.ONCE }) }
    var shiftTemplates by remember { mutableStateOf<List<ShiftTemplateDto>>(emptyList()) }
    var selectedShiftId by remember { mutableStateOf<String?>(definition?.targetShiftTemplateId) }
    var constraintType by remember { mutableStateOf(definition?.constraintType ?: "StartOnly") }
    var mediaRequired by remember { mutableStateOf(definition?.requiresMediaProof ?: false) }
    var allowedProofType by remember { mutableStateOf(definition?.allowedProofType?.takeUnless { it == "None" } ?: "PhotoOnly") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var audience by remember { mutableStateOf(definition?.audience ?: "ShiftStaff") }
    var priority by remember { mutableStateOf(definition?.priority ?: "Medium") }
    var requiresApproval by remember { mutableStateOf(definition?.requiresApproval ?: false) }
    var completionRule by remember { mutableStateOf(definition?.completionRule ?: "Each") }
    var quota by remember { mutableStateOf((definition?.requiredCount ?: 1).toString()) }
    var assignees by remember { mutableStateOf(definition?.assigneeUserIds.orEmpty().toSet()) }
    var staff by remember { mutableStateOf<List<StaffItemDto>?>(null) }
    var staffError by remember { mutableStateOf(false) }
    var staffRetry by remember { mutableIntStateOf(0) }

    LaunchedEffect(audience, staffRetry) {
        if (audience != "SelectedStaff" || staff != null) return@LaunchedEffect
        staffError = false
        try {
            val response = ApiClient.staffAdminService.getStaff()
            if (response.isSuccessful) staff = response.body().orEmpty() else staffError = true
        } catch (_: java.io.IOException) {
            staffError = true
        }
    }

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.shiftsService.getTemplates()
            if (response.isSuccessful) shiftTemplates = response.body().orEmpty()
        } catch (_: Exception) { }
    }

    fun shiftError(): String? = when {
        constraintType != "DeadlineOnly" && startTime.isBlank() -> "Başlangıç saatini seçin."
        constraintType != "StartOnly" && endTime.isBlank() -> "Bitiş saatini seçin."
        else -> null
    }

    fun assignmentError(): String? = when {
        (audience == "ShiftStaff" || completionRule == "Quota") && selectedShiftId == null -> "Vardiya personeli ve ortak hedef için bir vardiya seçin."
        audience == "SelectedStaff" && assignees.isEmpty() -> "En az bir personel seçin."
        completionRule == "Quota" && (quota.toIntOrNull() ?: 0) < 1 -> "Tamamlama hedefi en az 1 olmalı."
        else -> null
    }

    fun goBack() {
        focusManager.clearFocus()
        errorMessage = null
        step--
    }

    fun goNext() {
        errorMessage = when (step) {
            1 -> if (title.isBlank()) "Görev adını girin." else null
            2 -> shiftError()
            else -> return
        }
        if (errorMessage == null) {
            focusManager.clearFocus()
            step++
        }
    }

    BackHandler(enabled = step > 1 && !shiftPickerOpen && !staffPickerOpen) {
        if (!isSaving) goBack()
    }

    val handleSave: () -> Unit = {
        focusManager.clearFocus()
        errorMessage = null
        val needsStart = constraintType == "StartOnly" || constraintType == "TimeRange"
        val needsEnd = constraintType == "DeadlineOnly" || constraintType == "TimeRange"
        val count = quota.toIntOrNull()
        val validationError = shiftError() ?: assignmentError()
            ?: "Bir kanıt türü seçin.".takeIf { mediaRequired && allowedProofType == "None" }
        if (validationError != null) {
            errorMessage = validationError
        } else {
            scope.launch {
                isSaving = true
                try {
                    val command = CreateTaskDefinitionCommand(
                            priority = priority,
                            requiresApproval = requiresApproval,
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            targetShiftTemplateId = selectedShiftId,
                            constraintType = constraintType,
                            targetStartTime = startTime.takeIf { needsStart }?.toApiTime(),
                            targetEndTime = endTime.takeIf { needsEnd }?.toApiTime(),
                            recurrenceType = when (repeat) {
                                TaskRepeat.ONCE -> "OneShot"
                                TaskRepeat.DAILY -> "Daily"
                                TaskRepeat.CYCLE_BOUND -> "CycleBound"
                            },
                            requiresMediaProof = mediaRequired || requiresApproval,
                            allowedProofType = if (mediaRequired || requiresApproval) allowedProofType else "None",
                            audience = audience,
                            completionRule = completionRule,
                            requiredCount = count.takeIf { completionRule == "Quota" },
                            assigneeUserIds = assignees.toList().takeIf { audience == "SelectedStaff" }
                    )
                    val response = if (definition == null) ApiClient.tasksService.createDefinition(command)
                    else ApiClient.tasksService.updateDefinition(definition.id, command)
                    if (response.isSuccessful) {
                        LukaFeedback.show(if (definition == null) "Görev oluşturuldu" else "Görev tanımı güncellendi")
                        onNavigateBack()
                    } else {
                        errorMessage = com.lukasoft.lukarest.data.api.apiErrorMessage(response.errorBody()?.string(), "Görev kaydedilemedi (${response.code()}). Atama ve saat bilgilerini kontrol edin.")
                    }
                } catch (e: Exception) {
                    errorMessage = "Görev kaydedilemedi: ${e.localizedMessage}"
                } finally {
                    isSaving = false
                }
            }
        }
    }

    if (step == 2 && shiftPickerOpen) {
        TaskShiftPickerSheet(
            templates = shiftTemplates,
            selectedId = selectedShiftId,
            onDismiss = { shiftPickerOpen = false },
            onSelect = { template ->
                selectedShiftId = template.id
                shiftPickerOpen = false
                errorMessage = null
            }
        )
    }
    if (step == 3 && staffPickerOpen) {
        TaskStaffPickerSheet(
            staff = staff,
            failed = staffError,
            selectedIds = assignees,
            onRetry = { staffRetry++ },
            onDismiss = { staffPickerOpen = false },
            onApply = { selection ->
                assignees = selection
                staffPickerOpen = false
                errorMessage = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopPill(
                title = if (definition == null) "Görev Oluştur" else "Görevi Düzenle",
                subtitle = "Vardiya görevleri",
                initials = "P",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                gradient = BossGradient,
                actionIcon = Icons.Default.Close,
                actionDescription = "Formu kapat",
                onActionClick = onNavigateBack
            )
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            TaskFormProgress(step, Modifier.padding(horizontal = 16.dp))
            key(step) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step == 1) {
                        item {
                            TaskFormCard("Görev bilgileri", Icons.Default.EditNote) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    TaskFormLabel("Görev adı *")
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = {
                                            title = it.take(150)
                                            errorMessage = null
                                        },
                                        placeholder = { Text("Örn. Kasa sayımı") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = taskFormFieldColors(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { descriptionFocus.requestFocus() })
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    TaskFormLabel("Açıklama · isteğe bağlı")
                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it.take(4000) },
                                        placeholder = { Text("Personelin bilmesi gerekenler") },
                                        modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocus),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = taskFormFieldColors(),
                                        minLines = 2,
                                        maxLines = 4,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { goNext() })
                                    )
                                }
                                HorizontalDivider(color = ProgTrack)
                                TaskFormLabel("Görev tekrarı")
                                com.lukasoft.lukarest.ui.components.PrioritySelector(priority) { priority = it }
                                TaskFormSegments(
                                    repeat,
                                    listOf(
                                        TaskRepeat.ONCE to "Tek seferlik",
                                        TaskRepeat.DAILY to "Günlük",
                                        TaskRepeat.CYCLE_BOUND to "Döngüye bağlı"
                                    )
                                ) { repeat = it }
                            }
                        }
                    } else if (step == 2) {
                        item {
                            val selectedShift = shiftTemplates.firstOrNull { it.id == selectedShiftId }
                            TaskFormCard("Vardiya ve saat", Icons.Default.CalendarMonth) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    TaskFormLabel("Vardiya (seçili personel için isteğe bağlı)")
                                    if (selectedShiftId != null) OutlinedButton(onClick = { selectedShiftId = null }, modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PillErr.fg)) {
                                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Vardiya bağlantısını kaldır", color = PillErr.fg)
                                    }
                                    TaskFormSelectRow(
                                        title = selectedShift?.name ?: "Vardiya seç",
                                        subtitle = selectedShift?.let {
                                            it.startTime.take(5) + " – " + it.endTime.take(5)
                                        },
                                        enabled = shiftTemplates.isNotEmpty(),
                                        onClick = { shiftPickerOpen = true }
                                    )
                                    if (shiftTemplates.isEmpty()) {
                                        Text("Seçilebilecek vardiya yok.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                }
                                TaskFormLabel("Saat koşulu")
                                TaskFormSegments(
                                    constraintType,
                                    listOf("StartOnly" to "Başlangıç", "DeadlineOnly" to "Bitiş", "TimeRange" to "Aralık")
                                ) { constraintType = it }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    if (constraintType != "DeadlineOnly") {
                                        LukaTimeField(startTime, { startTime = it }, "Başlangıç saati *", Modifier.weight(1f))
                                    }
                                    if (constraintType != "StartOnly") {
                                        LukaTimeField(endTime, { endTime = it }, "Bitiş saati *", Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                    } else {
                        item {
                            TaskFormCard("Atama ve tamamlama", Icons.Default.People) {
                                Row(modifier = Modifier.heightIn(min = 48.dp).toggleable(value = requiresApproval, role = Role.Checkbox,
                                    onValueChange = { requiresApproval = it; if (it) mediaRequired = true }), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(requiresApproval, onCheckedChange = null)
                                    Text("Tamamlama yönetici onayı gerektirsin")
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    TaskFormLabel("Kim yapacak?")
                                    TaskFormSegments(
                                        audience,
                                        listOf("ShiftStaff" to "Vardiyadaki herkes", "SelectedStaff" to "Seçili personel")
                                    ) { audience = it }
                                    if (audience == "SelectedStaff") {
                                        TaskFormSelectRow(
                                            title = if (assignees.isEmpty()) "Personel seç" else "%d personel seçildi".format(assignees.size),
                                            onClick = { staffPickerOpen = true }
                                        )
                                        Text(
                                            if (selectedShiftId == null) "Seçilen kişilere doğrudan görev kartı açılır." else "Seçilen kişilerden o gün bu vardiyada çalışanlara kart açılır.",
                                            style = MaterialTheme.typography.bodySmall, color = TextMuted
                                        )
                                    }
                                }
                                HorizontalDivider(color = ProgTrack)
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    TaskFormLabel("Nasıl kapanacak?")
                                    TaskFormSegments(
                                        completionRule,
                                        listOf("Each" to "Kişi başına", "Quota" to "Ortak hedef")
                                    ) { completionRule = it }
                                    if (completionRule == "Each") {
                                        Text("Her personel kendi görevini tamamlar.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    } else if (completionRule == "Quota") {
                                        OutlinedTextField(
                                            value = quota,
                                            onValueChange = { value -> if (value.all { it in '0'..'9' }) quota = value },
                                            label = { Text("Tamamlama hedefi") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = taskFormFieldColors(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                                        )
                                        Text(
                                            if (quota.toIntOrNull() == 1) "İlk tamamlayan görevi kapatır." else "Aynı personel birden fazla katkı yapabilir.",
                                            style = MaterialTheme.typography.bodySmall, color = TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            TaskFormCard("Tamamlama kanıtı", Icons.Default.CameraAlt) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().toggleable(
                                        value = mediaRequired || requiresApproval, enabled = !requiresApproval, role = Role.Switch, onValueChange = { mediaRequired = it }
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        TaskFormLabel("Medya kanıtı iste")
                                        Text(if (requiresApproval) "Onay gereken görevlerde kamera kanıtı zorunludur." else "Personel görevi bitirirken kameradan kanıt ekler.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                    Switch(
                                        checked = mediaRequired || requiresApproval,
                                        onCheckedChange = null,
                                        colors = SwitchDefaults.colors(checkedTrackColor = BtnGradStart, checkedThumbColor = White)
                                    )
                                }
                                if (mediaRequired || requiresApproval) {
                                    TaskFormSegments(
                                        allowedProofType,
                                        listOf("PhotoOnly" to "Fotoğraf", "VideoOnly" to "Video", "PhotoOrVideo" to "Fotoğraf veya video")
                                    ) { allowedProofType = it }
                                }
                            }
                        }
                    }
                }
            }

            Surface(color = White) {
                Column {
                    HorizontalDivider(color = ProgTrack)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        errorMessage?.let {
                            Text(it, color = PillErr.fg, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (step > 1) {
                                OutlinedButton(
                                    onClick = { goBack() },
                                    enabled = !isSaving,
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, BorderSoft),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BossGradStart)
                                ) { Text("Geri") }
                            }
                            PrimaryButton(
                                text = when {
                                    step < 3 -> "İleri"
                                    isSaving -> "Kaydediliyor..."
                                    definition == null -> "Görevi Oluştur"
                                    else -> "Değişiklikleri Kaydet"
                                },
                                onClick = { if (step < 3) goNext() else handleSave() },
                                enabled = !isSaving,
                                loading = isSaving,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskFormProgress(step: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        TaskFormStep(1, "Görev detayları", step, Modifier.weight(1f))
        HorizontalDivider(Modifier.width(12.dp).padding(top = 13.dp), color = if (step >= 2) BtnGradStart else BorderSoft)
        TaskFormStep(2, "Vardiya detayları", step, Modifier.weight(1f))
        HorizontalDivider(Modifier.width(12.dp).padding(top = 13.dp), color = if (step >= 3) BtnGradStart else BorderSoft)
        TaskFormStep(3, "Atama ve kanıt", step, Modifier.weight(1f))
    }
}

@Composable
private fun TaskFormStep(number: Int, label: String, step: Int, modifier: Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(26.dp).background(if (number <= step) BtnGradStart else PillNeutral.bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (number < step) {
                Icon(Icons.Default.Check, contentDescription = "Adım tamamlandı", tint = White, modifier = Modifier.size(15.dp))
            } else {
                Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = if (number == step) White else TextMuted)
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            fontWeight = if (number == step) FontWeight.Bold else FontWeight.Medium,
            color = if (number == step) BossGradStart else TextMuted
        )
    }
}

@Composable
private fun TaskFormCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    LukaCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(PillBlue.bg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = PillBlue.fg, modifier = Modifier.size(18.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BossGradStart)
            }
            content()
        }
    }
}

@Composable
private fun TaskFormLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = TextMain)
}

@Composable
private fun taskFormFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BtnGradStart,
    unfocusedBorderColor = BorderSoft,
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted,
    cursorColor = BtnGradStart
)

@Composable
private fun TaskFormSelectRow(title: String, subtitle: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = if (enabled) TextMain else TextMuted)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted) }
            }
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = PillBlue.fg, modifier = Modifier.size(20.dp))
        }
    }
}

private fun String.toApiTime(): String {
    val normalized = trim()
    val parts = normalized.split(":")
    return when (parts.size) {
        2 -> "$normalized:00"
        3 -> normalized
        else -> normalized
    }
}


@Composable
private fun <T> TaskFormSegments(selected: T, choices: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    LukaChoiceGroup(value = selected, options = choices, onSelect = onSelect)
}
