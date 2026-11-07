package com.lukasoft.lukarest.ui.screens.boss

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.CreateTaskDefinitionCommand
import com.lukasoft.lukarest.data.api.ShiftTemplateDto
import com.lukasoft.lukarest.data.model.TaskRepeat
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BossTaskCreateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(TaskRepeat.ONCE) }
    var shiftTemplates by remember { mutableStateOf<List<ShiftTemplateDto>>(emptyList()) }
    var selectedShiftId by remember { mutableStateOf<String?>(null) }
    var constraintType by remember { mutableStateOf("StartOnly") }
    var mediaRequired by remember { mutableStateOf(false) }
    var allowedProofType by remember { mutableStateOf("PhotoOnly") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.shiftsService.getTemplates()
            if (response.isSuccessful) shiftTemplates = response.body().orEmpty()
        } catch (_: Exception) { }
    }

    val handleCreate: () -> Unit = {
        val needsStart = constraintType == "StartOnly" || constraintType == "TimeRange"
        val needsEnd = constraintType == "DeadlineOnly" || constraintType == "TimeRange"
        if (title.isBlank() || title.length > 150 || description.length > 4000 ||
            (needsStart && startTime.isBlank()) || (needsEnd && endTime.isBlank()) ||
            selectedShiftId == null ||
            (mediaRequired && allowedProofType == "None")) {
            errorMessage = "Başlık, vardiya, seçilen saat koşulu ve kanıt türü alanlarını kontrol edin."
        } else {
            scope.launch {
                isSaving = true
                try {
                    val response = ApiClient.tasksService.createDefinition(
                        CreateTaskDefinitionCommand(
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
                            requiresMediaProof = mediaRequired,
                            allowedProofType = if (mediaRequired) allowedProofType else "None"
                        )
                    )
                    if (response.isSuccessful) {
                        LukaFeedback.show("Görev oluşturuldu")
                        onNavigateBack()
                    } else {
                        errorMessage = "Görev oluşturulamadı (${response.code()}): ${response.errorBody()?.string()?.take(240).orEmpty()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Görev oluşturulamadı: ${e.localizedMessage}"
                } finally {
                    isSaving = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Görev Oluştur",
                    subtitle = "Yeni Vardiya Görevi Ata",
                    initials = "LK",
                    gradient = BossGradient,
                    actionIcon = Icons.Default.Close,
                    onActionClick = onNavigateBack
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(text = "Görev Adı *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it.take(150)
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = { Text("Örn: Masaları hazırla, Kasa sayımı...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(4000) },
                        label = { Text("Açıklama (isteğe bağlı)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("Saat Koşulu", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("StartOnly" to "Başlangıç", "DeadlineOnly" to "Bitiş", "TimeRange" to "Aralık").forEach { (value, label) ->
                            val selected = constraintType == value
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                    .background(if (selected) StaffGradStart else White)
                                    .border(1.dp, if (selected) StaffGradStart else BorderSoft, RoundedCornerShape(18.dp))
                                    .clickable { constraintType = value },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp), fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, color = if (selected) White else TextMain)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (constraintType != "DeadlineOnly") {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Başlangıç Saati *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    placeholder = { Text("09:00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }

                        if (constraintType != "StartOnly") {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = if (constraintType == "DeadlineOnly") "Bitiş Saati *" else "Bitiş Saati *",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    placeholder = { Text("17:00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Tekrar
                item {
                    Text(text = "Görev Tekrarı", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            TaskRepeat.ONCE to "Tek seferlik",
                            TaskRepeat.DAILY to "Günlük",
                            TaskRepeat.CYCLE_BOUND to "Döngüye bağlı"
                        ).forEach { (rep, label) ->
                            val isSelected = repeat == rep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) StaffGradStart else White)
                                    .border(1.dp, if (isSelected) StaffGradStart else BorderSoft, RoundedCornerShape(18.dp))
                                    .clickable { repeat = rep },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) White else TextMain
                                )
                            }
                        }
                    }
                }

                // Vardiya Seçimi
                item {
                    Text(text = "Bağlı Olduğu Vardiya *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(shiftTemplates) { template ->
                    val isSelected = selectedShiftId == template.id
                    LukaCard(
                        modifier = Modifier.clickable { selectedShiftId = template.id },
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = template.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                Text(text = "${template.startTime.take(5)} — ${template.endTime.take(5)}", fontSize = 12.sp, color = TextMuted)
                            }
                            StatusPill(
                                text = if (isSelected) "Seçili" else template.shortCode,
                                pillColor = if (isSelected) PillOk else PillBlue
                            )
                        }
                    }
                }

                if (shiftTemplates.isEmpty()) {
                    item { Text("Bu şubede görev bağlanabilecek vardiya şablonu bulunamadı.", color = PillErr.fg, fontSize = 12.sp) }
                }

                // Fotoğraf Zorunluluğu Kartı
                item {
                    LukaCard(modifier = Modifier.clickable { mediaRequired = !mediaRequired }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (mediaRequired) StaffGradStart else Color.Transparent)
                                    .border(2.dp, if (mediaRequired) StaffGradStart else BorderSoft, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (mediaRequired) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column {
                                Text(text = "Medya kanıtı zorunlu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BossGradStart)
                                Text(text = "Personel görev tamamlarken izin verilen dosya türünü yükler", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }

                if (mediaRequired) {
                    item {
                        Text("İzin verilen kanıt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("PhotoOnly" to "Fotoğraf", "VideoOnly" to "Video", "PhotoOrVideo" to "Fotoğraf veya video").forEach { (value, label) ->
                                val selected = allowedProofType == value
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) StaffGradStart else White)
                                        .border(1.dp, if (selected) StaffGradStart else BorderSoft, RoundedCornerShape(16.dp))
                                        .clickable { allowedProofType = value }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, color = if (selected) White else TextMain, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Text(text = errorMessage ?: "", color = PillErr.fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Alt Butonlar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(text = if (isSaving) "Kaydediliyor..." else "Görevi Oluştur", onClick = handleCreate,
                    enabled = !isSaving, gradient = BossGradient)
                Text(
                    text = "Vazgeç",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.clickable { onNavigateBack() }.padding(4.dp)
                )
            }
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

