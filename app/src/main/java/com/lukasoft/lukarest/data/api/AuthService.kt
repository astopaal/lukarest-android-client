package com.lukasoft.lukarest.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/api/v1/auth/patron/email")
    suspend fun requestPatronEmail(
        @Body body: RequestPatronEmailLoginCommand
    ): Response<Unit>

    @POST("/api/v1/auth/patron/email/resend")
    suspend fun resendPatronEmail(
        @Body body: RequestPatronEmailLoginCommand
    ): Response<Unit>

    @POST("/api/v1/auth/patron/email/verify")
    suspend fun verifyPatronEmail(
        @Body body: VerifyPatronEmailLoginCommand
    ): Response<AuthTokensResponse>

    @POST("/api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body body: RefreshAccessTokenCommand
    ): Response<AuthTokensResponse>

    @POST("/api/v1/auth/logout")
    suspend fun logout(
        @Body body: LogoutCommand
    ): Response<Unit>

    @POST("/api/v1/auth/switch-branch")
    suspend fun switchBranch(
        @Body body: SwitchBranchCommand
    ): Response<AuthTokensResponse>
}

