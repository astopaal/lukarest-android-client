package com.lukasoft.lukarest.ui.screens.boss

import com.lukasoft.lukarest.ui.components.LukaFeedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.UpdateBranchCommand
import com.lukasoft.lukarest.data.api.UpdateCompanyCommand
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BossCompanySetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) } // 1: Şirket, 2: Şube

    // ── Şirket Form Alanları (Adım 1) ──
    var legalName by remember { mutableStateOf("") }
    var tradeName by remember { mutableStateOf("") }
    var taxNumber by remember { mutableStateOf("") }
    var taxOffice by remember { mutableStateOf("") }

    // ── Şube Form Alanları (Adım 2) ──
    var branchId by remember { mutableStateOf(AuthStore.activeBranchId.value ?: "") }
    var branchName by remember { mutableStateOf("Ana Şube") }
    var branchCode by remember { mutableStateOf("") }
    var branchAddress by remember { mutableStateOf("") }
    var branchCity by remember { mutableStateOf("") }
    var branchOpeningTime by remember { mutableStateOf("09:00") }
    var branchClosingTime by remember { mutableStateOf("23:00") }

    var isInitialLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // İlk açılışta var olan taslak şirket ve şube bilgilerini çek
    LaunchedEffect(Unit) {
        try {
            // Şirket taslağı
            val compRes = ApiClient.branchService.getCompany()
            if (compRes.isSuccessful && compRes.body() != null) {
                val company = compRes.body()!!
                if (company.legalName.isNotBlank() && !company.legalName.startsWith("patron", ignoreCase = true)) {
                    legalName = company.legalName
                }
                if (company.tradeName.isNotBlank() && !company.tradeName.startsWith("patron", ignoreCase = true)) {
                    tradeName = company.tradeName
                }
                taxNumber = company.taxNumber ?: ""
                taxOffice = company.taxOffice ?: ""
            }

            // Şube taslağı
            val brRes = ApiClient.branchService.getBranches()
            if (brRes.isSuccessful && !brRes.body().isNullOrEmpty()) {
                val firstBranch = brRes.body()!!.first()
                branchId = firstBranch.id
                if (firstBranch.name.isNotBlank()) branchName = firstBranch.name
                if (firstBranch.branchCode.isNotBlank()) branchCode = firstBranch.branchCode
                if (!firstBranch.address.isNullOrBlank()) branchAddress = firstBranch.address
                if (!firstBranch.city.isNullOrBlank()) branchCity = firstBranch.city
                if (firstBranch.openingTime.isNotBlank()) branchOpeningTime = firstBranch.openingTime.take(5)
                if (firstBranch.closingTime.isNotBlank()) branchClosingTime = firstBranch.closingTime.take(5)
            }
        } catch (_: Exception) {
            // Form varsayılan değerlerle açılır
        } finally {
            isInitialLoading = false
        }
    }

    // Saati HH:mm:ss formatına tamamlar
    fun formatTimeForApi(timeStr: String): String {
        val trimmed = timeStr.trim()
        return when {
            trimmed.count { it == ':' } == 1 -> "$trimmed:00"
            trimmed.count { it == ':' } == 2 -> trimmed
            else -> "09:00:00"
        }
    }

    // ── ADIM 1 KAYDI (ŞİRKET) ──
    fun handleSaveCompany() {
        val trimmedLegal = legalName.trim()
        val trimmedTrade = tradeName.trim()
        val trimmedTaxNo = taxNumber.trim()
        val trimmedTaxOff = taxOffice.trim()

        if (trimmedTrade.isEmpty()) {
            errorMessage = "Lütfen ticari işletme / marka adını girin."
            return
        }
        if (trimmedLegal.isEmpty()) {
            errorMessage = "Lütfen resmi şirket ünvanını girin."
            return
        }

        errorMessage = null
        isSubmitting = true

        coroutineScope.launch {
            try {
                val command = UpdateCompanyCommand(
                    legalName = trimmedLegal,
                    tradeName = trimmedTrade,
                    taxNumber = trimmedTaxNo,
                    taxOffice = trimmedTaxOff
                )
                val res = ApiClient.branchService.updateCompany(command)
                if (res.isSuccessful && res.body() != null) {
                    // Şirket kaydedildi. Backend requiresSetup'ı false yapar.
                    // Şube adımını atlamamak için bu aşamada /me tekrar okunmaz, doğrudan Adım 2'ye geçilir.
                    if (branchCode.isBlank()) {
                        branchCode = trimmedTrade.take(3).uppercase()
                    }
                    currentStep = 2
                } else {
                    errorMessage = "Şirket kaydedilemedi: ${res.code()} - ${res.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Sunucu hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            } finally {
                isSubmitting = false
            }
        }
    }

    // ── ADIM 2 KAYDI (ŞUBE) ──
    fun handleSaveBranch() {
        val trimmedName = branchName.trim()
        val trimmedCode = branchCode.trim().uppercase()
        val trimmedAddr = branchAddress.trim()
        val trimmedCity = branchCity.trim()

        if (trimmedName.isEmpty()) {
            errorMessage = "Lütfen şube adını girin."
            return
        }
        if (trimmedCode.isEmpty()) {
            errorMessage = "Lütfen şube kodunu girin (örn: ALS)."
            return
        }

        errorMessage = null
        isSubmitting = true

        coroutineScope.launch {
            try {
                // Eğer şube ID'si henüz alınamadıysa son kez listeden al
                var targetId = branchId
                if (targetId.isBlank()) {
                    val brRes = ApiClient.branchService.getBranches()
                    if (brRes.isSuccessful && !brRes.body().isNullOrEmpty()) {
                        targetId = brRes.body()!!.first().id
                        branchId = targetId
                    }
                }

                if (targetId.isBlank()) {
                    errorMessage = "Şube kimliği bulunamadı. Lütfen sayfayı yenileyin."
                    isSubmitting = false
                    return@launch
                }

                val branchCommand = UpdateBranchCommand(
                    name = trimmedName,
                    branchCode = trimmedCode,
                    address = trimmedAddr,
                    city = trimmedCity,
                    openingTime = formatTimeForApi(branchOpeningTime),
                    closingTime = formatTimeForApi(branchClosingTime)
                )

                val updateRes = ApiClient.branchService.updateBranch(targetId, branchCommand)
                if (updateRes.isSuccessful && updateRes.body() != null) {
                    val updatedBranch = updateRes.body()!!

                    // İki form da tamamlandı: Artık /me ve güncel aktif şubeyi çekip kurulumu bitir
                    try {
                        val meRes = ApiClient.meService.getMe()
                        if (meRes.isSuccessful && meRes.body() != null) {
                            AuthStore.updateUser(meRes.body()!!)
                        }
                    } catch (_: Exception) {}

                    AuthStore.setRequiresSetup(false)
                    AuthStore.updateActiveBranch(updatedBranch.id, updatedBranch.name)

                    LukaFeedback.show("İşletme ve ilk şubeniz hazır!")
                    onSetupComplete()
                } else {
                    errorMessage = "Şube kaydedilemedi: ${updateRes.code()} - ${updateRes.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "Sunucu hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)
            ) {
                TopPill(
                    title = if (currentStep == 1) "Şirket Kurulumu (1/2)" else "Şube Tanımlama (2/2)",
                    subtitle = "Luka Restoran Portalı",
                    initials = "LK",
                    gradient = BossGradient,
                    actionIcon = Icons.Default.ArrowBack,
                    onActionClick = { currentStep = 1; errorMessage = null },
                    showAction = currentStep == 2
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        if (isInitialLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BossGradStart)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── ADIM GÖSTERGESİ ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Adım 1 Çubuğu
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(BossGradStart)
                    )
                    // Adım 2 Çubuğu
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (currentStep == 2) BossGradStart else BorderSoft)
                    )
                }

                if (currentStep == 1) {
                    // ──────────────────────────────────────────────
                    // ADIM 1: ŞİRKET BİLGİLERİ FORMU
                    // ──────────────────────────────────────────────
                    LukaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(BossGradStart.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = BossGradStart,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "1. Adım: Şirket Bilgileri",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )
                                Text(
                                    text = "İşletmenizin kurumsal ve resmi bilgilerini girin.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    LukaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Ticari Marka / İşletme Adı
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Ticari İşletme Adı / Marka *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = tradeName,
                                    onValueChange = {
                                        tradeName = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: Luka Restoran") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            // Resmi Şirket Ünvanı
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Resmi Şirket Ünvanı *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = legalName,
                                    onValueChange = {
                                        legalName = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: Luka Restoran Grubu A.Ş.") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            // Vergi Dairesi
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Vergi Dairesi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = taxOffice,
                                    onValueChange = {
                                        taxOffice = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: Konak") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            // Vergi Numarası
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Vergi Numarası / VKN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = taxNumber,
                                    onValueChange = {
                                        taxNumber = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: 1234567890") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = PillErr.fg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            PrimaryButton(
                                text = if (isSubmitting) "Kaydediliyor..." else "Devam Et (Şube Bilgileri) →",
                                onClick = { if (!isSubmitting) handleSaveCompany() },
                                enabled = !isSubmitting,
                                gradient = BossGradient,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // ──────────────────────────────────────────────
                    // ADIM 2: ŞUBE BİLGİLERİ FORMU
                    // ──────────────────────────────────────────────
                    LukaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(BossGradStart.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = BossGradStart,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "2. Adım: İlk Şubenizi Tanımlayın",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )
                                Text(
                                    text = "Sistemdeki varsayılan ana şubenizin ad ve adres bilgilerini güncelleyin.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    LukaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Şube Adı
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Şube Adı *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = branchName,
                                    onValueChange = {
                                        branchName = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: Alsancak Şubesi") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            // Şube Kodu
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Şube Kodu * (Büyük Harf)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = branchCode,
                                    onValueChange = {
                                        branchCode = it.uppercase()
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: ALS") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            // Şehir
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Şehir",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = branchCity,
                                    onValueChange = {
                                        branchCity = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: İzmir") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            // Adres
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Adres",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                OutlinedTextField(
                                    value = branchAddress,
                                    onValueChange = {
                                        branchAddress = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: Kıbrıs Şehitleri Caddesi No: 42") },
                                    maxLines = 2,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGradStart,
                                        unfocusedBorderColor = BorderSoft,
                                        focusedContainerColor = White,
                                        unfocusedContainerColor = White
                                    )
                                )
                            }

                            // Açılış / Kapanış Saatleri
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Açılış Saati",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted
                                    )
                                    OutlinedTextField(
                                        value = branchOpeningTime,
                                        onValueChange = { branchOpeningTime = it },
                                        placeholder = { Text("09:00") },
                                        singleLine = true,
                                        enabled = !isSubmitting,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGradStart,
                                            unfocusedBorderColor = BorderSoft,
                                            focusedContainerColor = White,
                                            unfocusedContainerColor = White
                                        )
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Kapanış Saati",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted
                                    )
                                    OutlinedTextField(
                                        value = branchClosingTime,
                                        onValueChange = { branchClosingTime = it },
                                        placeholder = { Text("23:00") },
                                        singleLine = true,
                                        enabled = !isSubmitting,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGradStart,
                                            unfocusedBorderColor = BorderSoft,
                                            focusedContainerColor = White,
                                            unfocusedContainerColor = White
                                        )
                                    )
                                }
                            }

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = PillErr.fg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            PrimaryButton(
                                text = if (isSubmitting) "Kaydediliyor..." else "Kurulumu Tamamla ve Panele Geç",
                                onClick = { if (!isSubmitting) handleSaveBranch() },
                                enabled = !isSubmitting,
                                gradient = BossGradient,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
