package com.lukasoft.lukarest.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

data class RegisterDeviceCommand(
    @SerializedName("fcmPushToken") val fcmPushToken: String,
    @SerializedName("deviceModel") val deviceModel: String,
    @SerializedName("operatingSystem") val operatingSystem: String,
    @SerializedName("isPushEnabled") val isPushEnabled: Boolean = true
)

data class UnregisterDeviceCommand(
    @SerializedName("fcmPushToken") val fcmPushToken: String
)

interface DevicesService {
    @PUT("/api/v1/devices")
    suspend fun registerDevice(@Body command: RegisterDeviceCommand): Response<Unit>

    @POST("/api/v1/devices/unregister")
    suspend fun unregisterDevice(@Body command: UnregisterDeviceCommand): Response<Unit>
}

