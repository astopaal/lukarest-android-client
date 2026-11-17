package com.lukasoft.lukarest.data.api

import android.util.Log
import com.lukasoft.lukarest.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

object ApiClient {

    private var accessTokenProvider: (() -> String?)? = null
    private var refreshTokenProvider: (() -> String?)? = null
    private var tokenUpdateHandler: ((AuthTokensResponse) -> Unit)? = null
    private var refreshRejectedHandler: (() -> Unit)? = null
    private val refreshLock = Any()
    private var validationErrorHandler: ((String) -> Unit)? = null

    fun setValidationErrorHandler(handler: (String) -> Unit) { validationErrorHandler = handler }

    fun setAccessTokenProvider(provider: () -> String?) {
        accessTokenProvider = provider
    }

    fun setRefreshHandlers(
        refreshToken: () -> String?,
        onTokensRefreshed: (AuthTokensResponse) -> Unit,
        onRefreshRejected: () -> Unit
    ) {
        refreshTokenProvider = refreshToken
        tokenUpdateHandler = onTokensRefreshed
        refreshRejectedHandler = onRefreshRejected
    }

    private interface BlockingRefreshService {
        @POST("/api/v1/auth/refresh")
        fun refresh(@Body body: RefreshAccessTokenCommand): Call<AuthTokensResponse>
    }

    private val refreshService: BlockingRefreshService by lazy {
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(Config.API_URL)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BlockingRefreshService::class.java)
    }

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: OkHttpResponse): Request? {
            if (responseCount(response) >= 2) return null
            if (response.request.url.encodedPath.startsWith("/api/v1/auth/")) return null
            if (response.request.url.encodedPath.startsWith("/api/v1/staff-onboarding/qr-session")) return null
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.takeIf { it.isNotBlank() }

            synchronized(refreshLock) {
                val currentAccessToken = accessTokenProvider?.invoke()
                // Another request already refreshed this token while this request was in flight.
                if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestToken) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentAccessToken")
                        .build()
                }

                val refreshToken = refreshTokenProvider?.invoke()?.takeIf { it.isNotBlank() }
                    ?: return null
                val refreshResponse = try {
                    refreshService.refresh(RefreshAccessTokenCommand(refreshToken)).execute()
                } catch (_: Exception) {
                    return null
                }

                if (refreshResponse.code() == 401 || refreshResponse.code() == 400 || refreshResponse.code() == 409) {
                    refreshRejectedHandler?.invoke()
                    return null
                }
                if (!refreshResponse.isSuccessful) return null

                val tokens = refreshResponse.body() ?: return null
                tokenUpdateHandler?.invoke(tokens)
                if (tokens.accessToken.isBlank()) return null
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            }
        }

        private fun responseCount(response: OkHttpResponse): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor(
        HttpLoggingInterceptor.Logger { message ->
            Log.i("OkHttp", redactSensitiveJson(message))
        }
    ).apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val requestBuilder = request.newBuilder()

                val path = request.url.encodedPath
                val isPublicAuthEndpoint = path.contains("/api/v1/auth/patron/email")
                        || (path.startsWith("/api/v1/staff-onboarding/qr-session"))
                        || path.contains("/api/v1/auth/refresh")
                        || path.contains("/api/v1/auth/logout")
                        || path.contains("/api/v1/system/info")

                val token = accessTokenProvider?.invoke()
                if (!token.isNullOrBlank() && !isPublicAuthEndpoint) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 400) {
                    val message = apiErrorMessage(response.peekBody(16384).string(), "")
                    if (message.isNotBlank()) validationErrorHandler?.invoke(message)
                }
                if (BuildConfig.DEBUG) {
                    val body = response.peekBody(1024L * 1024L).string()
                    Log.i("ApiResponse", "<-- ${response.code} ${response.request.url}")
                    logInChunks("ApiResponse", if (body.isBlank()) "<empty body>" else redactSensitiveJson(body))
                }
                response
            }
            .authenticator(tokenAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Config.API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    val meService: MeService by lazy {
        retrofit.create(MeService::class.java)
    }

    val attendanceService: AttendanceService by lazy {
        retrofit.create(AttendanceService::class.java)
    }

    val staffOnboardingService: StaffOnboardingService by lazy {
        retrofit.create(StaffOnboardingService::class.java)
    }

    val shiftsService: ShiftsService by lazy {
        retrofit.create(ShiftsService::class.java)
    }

    val tasksService: TasksService by lazy {
        retrofit.create(TasksService::class.java)
    }

    val requestsService: RequestsService by lazy {
        retrofit.create(RequestsService::class.java)
    }

    val notificationsService: NotificationsService by lazy {
        retrofit.create(NotificationsService::class.java)
    }

    val branchService: BranchService by lazy {
        retrofit.create(BranchService::class.java)
    }

    val companyService: BranchService get() = branchService

    val staffAdminService: StaffAdminService by lazy {
        retrofit.create(StaffAdminService::class.java)
    }

    val operationalNotesService: OperationalNotesService by lazy {
        retrofit.create(OperationalNotesService::class.java)
    }

    val devicesService: DevicesService by lazy {
        retrofit.create(DevicesService::class.java)
    }
}

private fun redactSensitiveJson(message: String): String = message.replace(
    Regex("""(?i)("(?:accessToken|refreshToken|qrToken|token|password)"\s*:\s*")[^"]*(")""")
) { match -> "${match.groupValues[1]}<redacted>${match.groupValues[2]}" }

private fun logInChunks(tag: String, message: String) {
    message.chunked(3000).forEachIndexed { index, chunk ->
        Log.i(tag, "[$index] $chunk")
    }
}


