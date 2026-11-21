package com.lukasoft.lukarest

import com.google.gson.JsonParser
import com.lukasoft.lukarest.data.api.RenewStaffQrSessionCommand
import com.lukasoft.lukarest.data.api.StaffOnboardingService
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StaffQrContractTest {
    @Test
    fun renewSendsCurrentTokenInPostBodyAndReadsReplacement() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/staff-onboarding/qr-session/session-id/renew", request.url.encodedPath)
            val body = requireNotNull(request.body)
            assertEquals("json", body.contentType()?.subtype)
            val payload = Buffer().also { body.writeTo(it) }.readUtf8()
            assertEquals("current-token", JsonParser.parseString(payload).asJsonObject["qrToken"].asString)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """{"sessionId":"session-id","qrToken":"replacement-token","pairingCode":"123456"}"""
                        .toResponseBody("application/json".toMediaType())
                )
                .build()
        }.build()
        val service = Retrofit.Builder()
            .baseUrl("https://example.test/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StaffOnboardingService::class.java)

        val response = service.renewQrSession("session-id", RenewStaffQrSessionCommand("current-token"))

        assertTrue(response.isSuccessful)
        assertEquals("replacement-token", response.body()?.qrToken)
    }
}
