package com.lukasoft.lukarest.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.http.Query

data class ReviewTaskBody(val isApproved: Boolean, val note: String?)
data class MuteNotificationsBody(val category: String)
data class NotificationItemDto(val notificationId: String, val type: String, val id: String, val category: String, val sentAtUtc: String, val openedAtUtc: String?, val contextText: String? = null)
data class NotificationPageDto(val items: List<NotificationItemDto>, val hasMore: Boolean)
data class PushPayload(val type: String, val id: String, val notificationId: String? = null, val priority: String? = null)

interface NotificationsService {
    @GET("/api/v1/notifications")
    suspend fun list(@Query("page") page: Int, @Query("pageSize") pageSize: Int = 20): Response<NotificationPageDto>

    @POST("/api/v1/notifications/{id}/open")
    suspend fun open(@Path("id") id: String): Response<Unit>

    @POST("/api/v1/notifications/mute")
    suspend fun mute(@Body body: MuteNotificationsBody): Response<Unit>
}
