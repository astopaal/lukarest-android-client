package com.lukasoft.lukarest.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class OperationalNoteDto(
    @SerializedName("id") val id: String,
    @SerializedName("category") val category: String, // "Reservation", "DailyNotice", "HandoverNote"
    @SerializedName("targetDate") val targetDate: String,
    @SerializedName("guestName") val guestName: String?,
    @SerializedName("guestPhone") val guestPhone: String?,
    @SerializedName("guestCount") val guestCount: Int?,
    @SerializedName("tableNo") val tableNo: String?,
    @SerializedName("noteContent") val noteContent: String,
    @SerializedName("authorUserId") val authorUserId: String,
    @SerializedName("authorFullName") val authorFullName: String,
    @SerializedName("createdAtUtc") val createdAtUtc: String,
    @SerializedName("branchId") val branchId: String? = null,
    @SerializedName("mentionedUserIds") val mentionedUserIds: List<String>? = null
)

data class NoteMentionableUserDto(val userId: String, val fullName: String, val handle: String, val isOwner: Boolean)

data class CreateOperationalNoteCommand(
    @SerializedName("category") val category: String,
    @SerializedName("targetDate") val targetDate: String,
    @SerializedName("guestName") val guestName: String? = null,
    @SerializedName("guestPhone") val guestPhone: String? = null,
    @SerializedName("guestCount") val guestCount: Int? = null,
    @SerializedName("tableNo") val tableNo: String? = null,
    @SerializedName("noteContent") val noteContent: String,
    @SerializedName("mentionedUserIds") val mentionedUserIds: List<String> = emptyList()
)

interface OperationalNotesService {
    @GET("/api/v1/operational-notes/mentionable-users")
    suspend fun getMentionableUsers(@Query("search") search: String? = null): Response<List<NoteMentionableUserDto>>

    @GET("/api/v1/operational-notes/{id}")
    suspend fun getNote(@Path("id") id: String): Response<OperationalNoteDto>

    @GET("/api/v1/operational-notes")
    suspend fun getNotes(
        @Query("date") date: String? = null,
        @Query("category") category: String? = null
    ): Response<List<OperationalNoteDto>>

    @POST("/api/v1/operational-notes")
    suspend fun createNote(@Body body: CreateOperationalNoteCommand): Response<OperationalNoteDto>

    @PUT("/api/v1/operational-notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body body: CreateOperationalNoteCommand): Response<OperationalNoteDto>

    @DELETE("/api/v1/operational-notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Unit>
}


