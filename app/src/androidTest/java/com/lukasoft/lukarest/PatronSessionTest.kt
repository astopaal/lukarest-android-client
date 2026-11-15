package com.lukasoft.lukarest

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lukasoft.lukarest.data.api.AuthService
import com.lukasoft.lukarest.data.store.AppRole
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.screens.auth.VerifyScreen
import com.lukasoft.lukarest.ui.theme.LukarestTheme
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class PatronSessionTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetSession() { AuthStore.logout() }

    @Test fun invalidEmailLinkPreservesExistingPatronSession() {
        AuthStore.setRole(AppRole.BOSS)
        AuthStore.setTokens("existing-access", "existing-refresh")
        val rejectedAuth = Proxy.newProxyInstance(AuthService::class.java.classLoader, arrayOf(AuthService::class.java)) { _, method, _ ->
            check(method.name == "verifyPatronEmail")
            Response.error<Any>(401, "{\"title\":\"invalid_login_token\"}".toResponseBody())
        } as AuthService
        compose.setContent {
            LukarestTheme {
                VerifyScreen("used-email-link", {}, onNavigateToLogin = {}, authService = rejectedAuth)
            }
        }
        compose.onNodeWithText("Giriş Başarısız").assertExists()
        assertEquals("existing-access", AuthStore.accessToken.value)
        assertEquals("existing-refresh", AuthStore.refreshToken.value)
        assertEquals(AppRole.BOSS, AuthStore.role.value)
    }
}
