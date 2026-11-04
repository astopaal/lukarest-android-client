package com.lukasoft.lukarest

import android.content.Intent
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.navigation.AppNavigation
import com.lukasoft.lukarest.ui.theme.LukarestTheme

class MainActivity : ComponentActivity() {
    private val deepLinkTokenState = mutableStateOf<String?>(null)

    private val pushState = mutableStateOf<Pair<String, String>?>(null)

    private fun readPush(intent: Intent?) {
        val type = intent?.getStringExtra("type") ?: return
        val id = intent.getStringExtra("id") ?: return
        pushState.value = type to id
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kalıcı depolamayı başlat (Session Persistence)
        AuthStore.init(applicationContext)

        // API Client için AccessToken sağlayıcısı
        ApiClient.setAccessTokenProvider {
            AuthStore.accessToken.value
        }
        ApiClient.setRefreshHandlers(
            refreshToken = { AuthStore.refreshToken.value },
            onTokensRefreshed = { tokens ->
                AuthStore.setTokens(tokens.accessToken, tokens.refreshToken, tokens.requiresSetup)
            },
            onRefreshRejected = { AuthStore.logout() }
        )

        // Deep link kontrolü (luka://auth/verify?token=... veya http://...)
        deepLinkTokenState.value = intent?.data?.getQueryParameter("token")

        readPush(intent)

        setContent {
            LukarestTheme {
                val userId by AuthStore.userId.collectAsState()
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    FirebaseMessaging.getInstance().register()
                }
                LaunchedEffect(userId) {
                    if (userId != null && FirebaseApp.getApps(applicationContext).isNotEmpty()) {
                        val preferences = getPreferences(MODE_PRIVATE)
                        val shouldAsk = Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                            !preferences.getBoolean("notificationPermissionAsked", false)
                        if (shouldAsk) {
                            preferences.edit().putBoolean("notificationPermissionAsked", true).apply()
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            FirebaseMessaging.getInstance().register()
                        }
                    }
                }
                AppNavigation(
                    initialToken = deepLinkTokenState.value,
                    pendingPush = pushState.value,
                    onPushHandled = { pushState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readPush(intent)
        val token = intent.data?.getQueryParameter("token")
        if (!token.isNullOrBlank()) {
            deepLinkTokenState.value = token
        }
    }
}



