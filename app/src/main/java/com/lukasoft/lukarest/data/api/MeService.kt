package com.lukasoft.lukarest.data.api

import retrofit2.Response
import retrofit2.http.GET

interface MeService {
    @GET("/api/v1/me")
    suspend fun getMe(): Response<MeDto>
}

