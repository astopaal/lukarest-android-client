package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.CreateShiftTemplateCommand
import com.lukasoft.lukarest.data.api.ShiftTemplateDto
import com.lukasoft.lukarest.data.model.ShiftColorKey
import com.lukasoft.lukarest.data.model.ShiftDefinition
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.LukaTimeField
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch

@Composable
fun BossShiftAddScreen(
    viewModel: LukaViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var shiftName by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var existingTemplates by remember { mutableStateOf<List<ShiftTemplateDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.shiftsService.getTemplates()
            if (res.isSuccessful && res.body() != null) {
                existingTemplates = res.body()!!
            }
        } catch (_: Exception) {}
    }

    val quickPresets = listOf(
        Triple("Sabah", "08:00", "16:00"),
        Triple("Öğle", "12:00", "20:00"),
        Triple("Akşam", "16:00", "00:00"),
        Triple("Tam Gün", "10:00", "22:00")
    )

    val handleSave: () -> Unit = {
        val s = startTime.trim()
        val e = endTime.trim()
        val name = shiftName.trim()

        if (name.isBlank() || s.isBlank() || e.isBlank()) {
            errorMessage = "Lütfen vardiya adı ve saat aralıklarını doldurun."
        } else {
            isLoading = true
            errorMessage = null

            val formattedStart = if (s.length == 5) "$s:00" else s
            val formattedEnd = if (e.length == 5) "$e:00" else e
            val shortText = "${s.take(2)}-${e.take(2)}"

            coroutineScope.launch {
                try {
                    val command = CreateShiftTemplateCommand(
                        name = name,
                        startTime = formattedStart,
                        endTime = formattedEnd,
                        colorHex = "#2563EB",
                        shortCode = shortText,
                        isCustom = true
                    )
                    val res = ApiClient.shiftsService.createTemplate(command)
                    val created = res.body()
                    val newId = created?.id ?: "shift-${System.currentTimeMillis()}"

                    viewModel.addShiftDefinition(
                        ShiftDefinition(
                            id = newId,
                            name = name,
                            short = shortText,
                            start = s,
                            end = e,
                            colorKey = ShiftColorKey.OZEL
                        )
                    )
                    onNavigateBack()
                } catch (ex: Exception) {
                    val newDef = ShiftDefinition(
                        id = "shift-${System.currentTimeMillis()}",
                        name = name,
                        short = shortText,
                        start = s,
                        end = e,
                        colorKey = ShiftColorKey.OZEL
                    )
                    viewModel.addShiftDefinition(newDef)
                    onNavigateBack()
                } finally {
                    isLoading = false
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
                    title = "Yeni Vardiya",
                    subtitle = "Vardiya Şablonu Oluştur",
                    initials = "V",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vardiya İsmi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = shiftName,
                        onValueChange = { shiftName = it },
                        placeholder = { Text("Örn: Sabah Servisi, Akşam Vardiyası", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BossGradStart,
                            unfocusedBorderColor = BorderSoft,
                            focusedContainerColor = White,
                            unfocusedContainerColor = White
                        )
                    )
                }

                // Hızlı Seçim Rozetleri
                item {
                    Text(
                        text = "Hızlı Şablonlar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPresets.forEach { (pName, pStart, pEnd) ->
                            val isSelected = shiftName == pName && startTime == pStart && endTime == pEnd
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) BossGradStart else White)
                                    .border(1.dp, if (isSelected) BossGradStart else BorderSoft, RoundedCornerShape(20.dp))
                                    .clickable {
                                        shiftName = pName
                                        startTime = pStart
                                        endTime = pEnd
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = pName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) White else TextMain
                                )
                            }
                        }
                    }
                }

                // Saat Aralığı Kartı
                item {
                    LukaCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEBF3FC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = StaffGradStart,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Çalışma Saat Aralığı",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )
                                Text(
                                    text = "24 saat formatında (SS:DD) girin",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LukaTimeField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = "Başlangıç",
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "→",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }

                            LukaTimeField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = "Bitiş",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFECEC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                if (existingTemplates.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "KAYITLI VARDİYALAR (${existingTemplates.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }

                    items(existingTemplates) { def ->
                        LukaCard(contentPadding = PaddingValues(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = StaffGradStart,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(text = def.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                        Text(text = "${def.startTime} — ${def.endTime}", fontSize = 12.sp, color = TextMuted)
                                    }
                                }
                                StatusPill(text = def.shortCode.ifEmpty { "V" }, pillColor = PillBlue)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    text = if (isLoading) "Kaydediliyor..." else "Vardiyayı Kaydet",
                    loading = isLoading,
                    onClick = handleSave,
                    enabled = !isLoading,
                    gradient = BossGradient
                )
                Text(
                    text = "Vazgeç",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier
                        .clickable { onNavigateBack() }
                        .padding(4.dp)
                )
            }
        }
    }
}
