package com.lukasoft.lukarest.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.RequestPatronEmailLoginCommand
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 60

@Composable
fun BossLoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVerify: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var isSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(0) }

    // Countdown sayacı
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    val handleSendLink: () -> Unit = {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            error = "Lütfen e-posta adresinizi girin."
        } else {
            error = null
            isLoading = true
            coroutineScope.launch {
                try {
                    val response = ApiClient.authService.requestPatronEmail(
                        RequestPatronEmailLoginCommand(trimmed)
                    )
                    if (response.isSuccessful) {
                        isSent = true
                        countdown = RESEND_COOLDOWN_SECONDS
                    } else {
                        error = "Bağlantı gönderilemedi. Lütfen tekrar deneyin."
                    }
                } catch (e: Exception) {
                    error = "Sunucuya bağlanılamadı: ${e.localizedMessage ?: "Hata"}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val handleResendLink: () -> Unit = {
        if (countdown == 0 && !isLoading) {
            val trimmed = email.trim()
            if (trimmed.isNotEmpty()) {
                error = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        val response = ApiClient.authService.resendPatronEmail(
                            RequestPatronEmailLoginCommand(trimmed)
                        )
                        if (response.isSuccessful) {
                            countdown = RESEND_COOLDOWN_SECONDS
                        } else {
                            error = "Yeniden gönderim başarısız oldu. Lütfen tekrar deneyin."
                        }
                    } catch (e: Exception) {
                        error = "Sunucuya bağlanılamadı: ${e.localizedMessage ?: "Hata"}"
                    } finally {
                        isLoading = false
                    }
                }
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
                    title = "Patron Girişi",
                    subtitle = "Şifresiz Giriş Bağlantısı",
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
                .padding(16.dp)
        ) {
            // Intro Wrap
            Column(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 24.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Yönetici Paneline Bağlan",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BossGradStart
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Restoran vardiyalarını, görevleri ve talepleri yönetmek için giriş yapın.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSoft,
                    lineHeight = 20.sp
                )
            }

            // Form Card
            LukaCard {
                if (!isSent) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "E-posta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    if (error != null) error = null
                                },
                                placeholder = { Text("ornek@restoran.com") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StaffGradStart,
                                    unfocusedBorderColor = BorderSoft,
                                    focusedContainerColor = White,
                                    unfocusedContainerColor = White
                                )
                            )
                        }

                        if (error != null) {
                            Text(
                                text = error ?: "",
                                color = PillErr.fg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        PrimaryButton(
                            text = if (isLoading) "Gönderiliyor..." else "Giriş Linki Gönder",
                            loading = isLoading,
                            onClick = handleSendLink,
                            enabled = !isLoading,
                            gradient = BossGradient,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    // Sent Wrap
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PillOk.bg)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Giriş Bağlantısı Gönderildi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PillOk.fg
                            )
                        }

                        Text(
                            text = email,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BossGradStart,
                            textAlign = TextAlign.Center
                        )

                        if (error != null) {
                            Text(
                                text = error ?: "",
                                color = PillErr.fg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = handleResendLink,
                            enabled = countdown == 0 && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = if (countdown > 0) "Tekrar Gönder (${countdown}s)"
                                else if (isLoading) "Gönderiliyor..."
                                else "Tekrar Gönder",
                                fontWeight = FontWeight.Bold,
                                color = if (countdown == 0 && !isLoading) BossGradStart else TextMuted
                            )
                        }

                        TextButton(
                            onClick = {
                                isSent = false
                                error = null
                                countdown = 0
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Farklı E-posta Kullan",
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

