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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.navigation.AppNavigation
import com.lukasoft.lukarest.ui.components.LukaFeedbackOverlay
import com.lukasoft.lukarest.ui.theme.LukarestTheme

class MainActivity : ComponentActivity() {
    private val deepLinkTokenState = mutableStateOf<String?>(null)

    private val pushState = mutableStateOf<com.lukasoft.lukarest.data.api.PushPayload?>(null)

    private fun readPush(intent: Intent?) {
        val type = intent?.getStringExtra("type") ?: return
        val id = intent.getStringExtra("id") ?: return
        pushState.value = com.lukasoft.lukarest.data.api.PushPayload(type, id, intent.getStringExtra("notificationId"), intent.getStringExtra("priority"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kalıcı depolamayı başlat (Session Persistence)
        AuthStore.init(applicationContext)
        ApiClient.setValidationErrorHandler { com.lukasoft.lukarest.ui.components.LukaFeedback.show(it) }

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
        intent?.data = null

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
                Box(Modifier.fillMaxSize()) {
                    AppNavigation(
                        initialToken = deepLinkTokenState.value,
                        onInitialTokenHandled = { deepLinkTokenState.value = null },
                        pendingPush = pushState.value,
                        onPushHandled = { pushState.value = null }
                    )
                    LukaFeedbackOverlay(Modifier.align(Alignment.BottomCenter))
                }
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
            intent.data = null
        }
    }
}



