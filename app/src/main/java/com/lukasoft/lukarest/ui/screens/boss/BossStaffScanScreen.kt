package com.lukasoft.lukarest.ui.screens.boss

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.ClaimStaffQrSessionCommand
import com.lukasoft.lukarest.ui.components.*
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BossStaffScanScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var workType by remember { mutableStateOf(1) } // 1: Full-time, 2: Part-time
    var isSubmitting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    var selected by remember { mutableStateOf<com.lukasoft.lukarest.data.api.StaffItemDto?>(null) }
    var candidates by remember { mutableStateOf<List<com.lukasoft.lukarest.data.api.StaffItemDto>?>(null) }
    var addingNew by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var loadingStaff by remember { mutableStateOf(false) }

    val handleSaveStaff = {
        val code = scannedCode
        if (code != null && (addingNew || selected != null) && !isSubmitting) {
            isSubmitting = true
            feedbackMessage = null
            coroutineScope.launch {
                try {
                    val result = ApiClient.staffOnboardingService.claimStaff(
                        ClaimStaffQrSessionCommand(
                            pairingCode = code,
                            qrToken = null,
                            fullName = name.trim(),
                            phoneNumber = phone.trim(),
                            employmentType = workType,
                            birthDate = birthDate.trim()
                        )
                    )
                    isSuccess = result.isSuccessful
                    feedbackMessage = if (isSuccess) "QR eşleştirildi. Personel cihazında giriş tamamlanabilir."
                    else when (result.code()) {
                        400 -> "QR kodunu ve personel bilgilerini kontrol edin."
                        403 -> "Bu şubede QR eşleştirme yetkiniz yok."
                        404 -> "QR oturumu bulunamadı. Personel yeni bir kod oluşturabilir."
                        409 -> "Telefon numarası başka bir kayıtta kullanılıyor."
                        else -> "Eşleştirme tamamlanamadı."
                    }
                    if (isSuccess) {
                        delay(1000)
                        onNavigateBack()
                    }
                } catch (_: java.io.IOException) {
                    isSuccess = false
                    feedbackMessage = "Sunucuya ulaşılamadı."
                } finally {
                    isSubmitting = false
                }
            }
        }
    }

    candidates?.let { staff ->
        AlertDialog(
            onDismissRequest = { candidates = null },
            title = { Text("Mevcut personeli seç") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (staff.isEmpty()) Text("Bu şubede personel bulunamadı.")
                    staff.forEach { person ->
                        TextButton(onClick = {
                            selected = person
                            name = person.fullName
                            phone = person.phoneNumber
                            birthDate = person.birthDate
                            workType = if (person.employmentType == "PartTime") 2 else 1
                            candidates = null
                        }) {
                            Text(person.fullName + if (person.isActive) " · Yeni cihaz" else " · Pasif")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { candidates = null }) { Text("Kapat") } }
        )
    }

    if (showBirthDatePicker) {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val today = com.lukasoft.lukarest.data.util.DateUtils.getTurkeyTodayIso()
        val maxDate = format.parse(today)?.time ?: System.currentTimeMillis()
        val initial = runCatching { format.parse(birthDate)?.time }.getOrNull()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial,
            initialDisplayedMonthMillis = initial ?: maxDate,
            yearRange = 1900..today.take(4).toInt(),
            selectableDates = remember(maxDate) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxDate
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showBirthDatePicker = false },
            confirmButton = {
                TextButton(enabled = state.selectedDateMillis != null, onClick = {
                    state.selectedDateMillis?.let { birthDate = format.format(java.util.Date(it)) }
                    showBirthDatePicker = false
                }) { Text("Seç") }
            },
            dismissButton = { TextButton(onClick = { showBirthDatePicker = false }) { Text("Vazgeç") } }
        ) { DatePicker(state = state) }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Personel QR Okut",
                    subtitle = if (scannedCode == null) "Canlı Kamera Vizörü" else "QR Kod Alındı",
                    initials = "LK",
                    gradient = BossGradient,
                    actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // QR henüz okunmadıysa KAMERA AÇIK, okunduysa KAMERA DURUR ve Onay Kartı Çıkar
            if (scannedCode == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraBarcodeScanner(
                            onBarcodeScanned = { barcode ->
                                val cleanCode = barcode.replace("luka-staff:", "").trim()
                                scannedCode = cleanCode
                            }
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kamera izni gerekiyor",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PrimaryButton(
                                text = "İzin Ver",
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.width(160.dp),
                                height = 38.dp
                            )
                        }
                    }
                }
            } else {
                // Kamera Durduruldu, Başarılı Okundu Kartı
                LukaCard(backgroundColor = Color(0xFFDCFCE7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF146C38)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "QR Kod Başarıyla Okundu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF146C38)
                            )
                            Text(
                                text = "Eşleşme Kodu: $scannedCode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F5132)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scannedCode = null
                                selected = null
                                addingNew = false
                                feedbackMessage = null
                            },
                            enabled = !isSubmitting && !loadingStaff,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF146C38))
                        ) {
                            Text(text = "Yeniden Tara", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (scannedCode != null) LukaCard {
                if (selected == null && !addingNew) {
                    Text("İşlem seç", style = MaterialTheme.typography.titleLarge, color = BossGradStart)
                    Text("Okuttuğun kodu bir personelle eşleştir.", style = MaterialTheme.typography.bodyMedium, color = TextSoft)
                    Spacer(Modifier.height(16.dp))
                    PairingOption(
                        title = "Yeni personel ekle",
                        description = "İlk kez kayıt olacak kişi için.",
                        icon = Icons.Default.PersonAdd,
                        enabled = !loadingStaff,
                        onClick = {
                            name = ""
                            phone = ""
                            birthDate = ""
                            workType = 1
                            addingNew = true
                            feedbackMessage = null
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(true, false)
                        .forEach { active ->
                            PairingOption(
                                title = if (active) "Yeni cihazı eşleştir" else "Pasif personeli etkinleştir",
                                description = if (active) "Aktif personelin hesabını bu cihaza bağla."
                                    else "Hesabı yeniden aç ve cihazı bağla.",
                                icon = if (active) Icons.Default.Smartphone else Icons.Default.Person,
                                enabled = !loadingStaff,
                                onClick = {
                                loadingStaff = true
                                feedbackMessage = null
                                coroutineScope.launch {
                                    try {
                                        val result = ApiClient.staffAdminService.getStaff(includeInactive = !active)
                                        if (result.isSuccessful) {
                                            candidates = result.body().orEmpty().filter { it.isActive == active }
                                        } else feedbackMessage = "Personel listesi alınamadı."
                                    } catch (_: java.io.IOException) {
                                        feedbackMessage = "Sunucuya ulaşılamadı."
                                    } finally {
                                        loadingStaff = false
                                    }
                                }
                                }
                            )
                            if (active) Spacer(Modifier.height(8.dp))
                        }
                    if (loadingStaff) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                }

                if (addingNew || selected != null) {
                    Text(
                        if (addingNew) "Yeni personel bilgileri" else "Personel bilgilerini doğrula",
                        style = MaterialTheme.typography.titleLarge,
                        color = BossGradStart
                    )
                    TextButton(enabled = !isSubmitting, onClick = {
                        selected = null
                        addingNew = false
                        feedbackMessage = null
                    }) { Text("İşlemi değiştir") }

                    OutlinedTextField(
                        readOnly = selected != null || isSubmitting,
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ad Soyad *") },
                        placeholder = { Text("Örn: Mehmet Akif") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        readOnly = selected != null || isSubmitting,
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefon Numarası *") },
                        placeholder = { Text("0532XXXXXXX") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        enabled = selected == null && !isSubmitting,
                        onClick = { showBirthDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (birthDate.isBlank()) "Doğum tarihi seç" else "Doğum tarihi: " + birthDate.split("-").reversed().joinToString("."))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Çalışma Şekli",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val fullTimeActive = workType == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (fullTimeActive) StaffGradStart else Color.White)
                                .clickable(enabled = selected == null && !isSubmitting) { workType = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tam Zamanlı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (fullTimeActive) White else TextMain
                            )
                        }

                        val partTimeActive = workType == 2
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (partTimeActive) StaffGradStart else Color.White)
                                .clickable(enabled = selected == null && !isSubmitting) { workType = 2 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Yarı Zamanlı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (partTimeActive) White else TextMain
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = if (isSubmitting) "Kaydediliyor..." else when (selected?.isActive) {
                            true -> "Yeni Cihazı Eşleştir"
                            false -> "Personeli Etkinleştir ve Eşleştir"
                            null -> "Yeni Personeli Bağla"
                        },
                        onClick = handleSaveStaff,
                        enabled = !isSubmitting && scannedCode != null && name.trim().length >= 2 &&
                            phone.isNotBlank() && birthDate.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")),
                        gradient = BossGradient
                    )
                }

                if (feedbackMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSuccess) PillOk.bg else PillErr.bg)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = feedbackMessage ?: "",
                            color = if (isSuccess) PillOk.fg else PillErr.fg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}




@Composable
private fun PairingOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = White)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(Color(0xFFEAF1FC), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BossGradStart)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSoft)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted)
        }
    }
}
