package com.lukasoft.lukarest.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.VerifyPatronEmailLoginCommand
import com.lukasoft.lukarest.data.store.AppRole
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun VerifyScreen(
    token: String?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCompanySetup: () -> Unit = onNavigateToDashboard,
    onNavigateToLogin: () -> Unit,
    authService: com.lukasoft.lukarest.data.api.AuthService = ApiClient.authService
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        if (token.isNullOrBlank()) {
            isLoading = false
            error = "Geçersiz veya eksik bağlantı."
            return@LaunchedEffect
        }

        coroutineScope.launch {
            try {
                val response = authService.verifyPatronEmail(
                    VerifyPatronEmailLoginCommand(token)
                )
                if (response.isSuccessful && response.body() != null) {
                    val tokens = response.body()!!
                    AuthStore.setTokens(tokens.accessToken, tokens.refreshToken, tokens.requiresSetup)

                    try {
                        val meRes = ApiClient.meService.getMe()
                        if (meRes.isSuccessful && meRes.body() != null) {
                            AuthStore.updateUser(meRes.body()!!)
                        } else {
                            AuthStore.setRole(AppRole.BOSS)
                        }
                    } catch (_: Exception) {
                        AuthStore.setRole(AppRole.BOSS)
                    }

                    val needsSetup = tokens.requiresSetup || AuthStore.requiresSetup.value
                    if (needsSetup) {
                        onNavigateToCompanySetup()
                    } else {
                        onNavigateToDashboard()
                    }
                } else {
                    isLoading = false
                    error = "Bağlantı süresi dolmuş veya geçersiz."
                }
            } catch (e: Exception) {
                isLoading = false
                error = "Sunucuya bağlanılamadı: ${e.localizedMessage ?: "Hata"}"
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
                    title = "Giriş Doğrulama",
                    subtitle = "Patron Portalı",
                    initials = "LK",
                    gradient = BossGradient
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LukaCard {
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = StaffGradStart)
                        Text(
                            text = "Giriş yapılıyor...",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = BossGradStart
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PillErr.bg
                        ) {
                            Text(
                                text = "Giriş Başarısız",
                                color = PillErr.fg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = error ?: "Doğrulama gerçekleştirilemedi.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSoft,
                            textAlign = TextAlign.Center
                        )

                        PrimaryButton(
                            text = "Giriş Ekranına Dön",
                            onClick = onNavigateToLogin,
                            gradient = BossGradient,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

