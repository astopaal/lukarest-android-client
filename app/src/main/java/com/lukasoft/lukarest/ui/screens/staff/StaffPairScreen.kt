package com.lukasoft.lukarest.ui.screens.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.ExchangeStaffQrSessionCommand
import com.lukasoft.lukarest.data.api.RenewStaffQrSessionCommand
import com.lukasoft.lukarest.data.store.AppRole
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.QrCodeView
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun StaffPairScreen(
    onNavigateToBossLogin: () -> Unit,
    onPairSuccess: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var pairingCode by remember { mutableStateOf("") }
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var currentQrToken by remember { mutableStateOf<String?>(null) }
    var expiresAtUtc by remember { mutableStateOf<String?>(null) }
    var remainingSeconds by remember { mutableStateOf(0L) }
    var claimed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var exchanging by remember { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(0) }

    var pairingError by remember { mutableStateOf<String?>(null) }

    val createSession: () -> Unit = {
        coroutineScope.launch {
            if (busy) return@launch
            busy = true
            try {
                pairingError = null
                val id = currentSessionId
                val token = currentQrToken
                var res = if (id != null && !claimed && !token.isNullOrBlank()) {
                    ApiClient.staffOnboardingService.renewQrSession(id, RenewStaffQrSessionCommand(token))
                } else {
                    ApiClient.staffOnboardingService.createQrSession()
                }
                if (!res.isSuccessful && qrErrorCode(res.errorBody()?.string()) in setOf(
                        "qr_session_claimed",
                        "qr_token_invalid",
                        "qr_session_not_found"
                    )
                ) {
                    res = ApiClient.staffOnboardingService.createQrSession()
                }
                val body = res.body()
                if (res.isSuccessful && body != null) {
                    pairingCode = body.pairingCode
                    currentQrToken = body.qrToken
                    currentSessionId = body.sessionId
                    expiresAtUtc = body.expiresAtUtc
                    claimed = false
                    generation++
                } else {
                    pairingError = "QR oturumu yenilenemedi. Tekrar deneyin."
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: java.io.IOException) {
                pairingError = "Sunucuya ulaşılamadı. Tekrar deneyin."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(expiresAtUtc) {
        val expiry = runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(expiresAtUtc.orEmpty().take(19))?.time
        }.getOrNull()
        while (isActive && expiry != null) {
            remainingSeconds = ((expiry - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        createSession()
    }

    // Patron onayladı mı polling döngüsü
    LaunchedEffect(currentSessionId, currentQrToken, generation) {
        val sessionId = currentSessionId ?: return@LaunchedEffect
        val qrToken = currentQrToken
        while (isActive) {
            delay(1500)
            try {
                // 1) Durum kontrolü
                val statusRes = ApiClient.staffOnboardingService.getQrSessionStatus(sessionId)
                if (!statusRes.isSuccessful) {
                    val errorBody = statusRes.errorBody()?.string()
                    val errorCode = qrErrorCode(errorBody)
                    if (errorCode.equals("qr_session_expired", ignoreCase = true)) {
                        pairingCode = ""
                        createSession()
                        break
                    }
                    continue
                }
                val status = statusRes.body() ?: continue
                claimed = status.isClaimed
                status.expiresAtUtc?.let { expiresAtUtc = it }
                if (!status.canExchange || qrToken == null) continue
                exchanging = true
                val exchange = ApiClient.staffOnboardingService.exchangeQrSession(
                    ExchangeStaffQrSessionCommand(sessionId, qrToken)
                )
                val tokens = exchange.body()
                if (!exchange.isSuccessful || tokens == null) {
                    if (exchange.code() == 400 && qrErrorCode(exchange.errorBody()?.string()) == "qr_session_expired") {
                        claimed = true
                        pairingCode = ""
                        createSession()
                        break
                    }
                    pairingError = "Eşleştirme tamamlanamadı. QR kodunu yenileyin."
                    break
                }
                AuthStore.setTokens(tokens.accessToken, tokens.refreshToken)
                val me = ApiClient.meService.getMe()
                val profile = me.body()
                if (!me.isSuccessful || profile == null) {
                    AuthStore.clearTokens()
                    pairingError = "Personel profili alınamadı. QR kodunu yenileyin."
                    break
                }
                AuthStore.updateUser(profile)
                onPairSuccess()
                break
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: java.io.IOException) {
                if (!AuthStore.accessToken.value.isNullOrBlank()) {
                    AuthStore.clearTokens()
                    pairingError = "Personel profili alınamadı. QR kodunu yenileyin."
                    break
                }
            } finally {
                exchanging = false
            }
        }
    }

    val handleRefreshQr = {
        createSession()
    }

    val handleGoToBossLogin = {

        onNavigateToBossLogin()
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Şubeye Katıl",
                    subtitle = "Personel Eşleştirme",
                    initials = "QR",
                    showAction = false,
                    gradient = StaffGradient
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            pairingError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(if (busy) "QR hazırlanıyor..." else if (expiresAtUtc == null) "Oturum süresi sunucudan bekleniyor"
                else "${if (claimed) "Giriş" else "Eşleştirme"} için kalan süre: ${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}", color = TextMain)
            StatusPill(text = "Henüz hiçbir şubeye bağlı değilsin", pillColor = PillBlue)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "QR kodunu patronuna okut",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = BossGradStart,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Şubeye tanımlanmak için patron uygulamasından bu kod okutulmalıdır.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSoft,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            LukaCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (pairingCode.isNotBlank() && remainingSeconds > 0 && !busy) QrCodeView(
                        content = "luka-staff:$pairingCode",
                        size = 210.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tek kullanımlık eşleşme kodu",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (busy) "" else pairingCode,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BossGradStart,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38A169))
                )
                Text(
                    text = "Patron uygulamasından okutulması bekleniyor...",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = handleRefreshQr,
                    enabled = !busy && !exchanging,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StaffGradStart)
                ) {
                    Text(text = "QR Kodu Yenile", fontWeight = FontWeight.Bold)
                }

                PrimaryButton(
                    text = "Patron Girişi",
                    onClick = handleGoToBossLogin,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    gradient = StaffGradient
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun qrErrorCode(body: String?): String = runCatching {
    val problem = JSONObject(body.orEmpty())
    problem.optString("code").ifBlank { problem.optString("title") }
}.getOrDefault("")


