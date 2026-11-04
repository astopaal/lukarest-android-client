package com.lukasoft.lukarest.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class CompanyDto(
    @SerializedName("id") val id: String,
    @SerializedName("legalName") val legalName: String,
    @SerializedName("tradeName") val tradeName: String,
    @SerializedName("taxNumber") val taxNumber: String?,
    @SerializedName("taxOffice") val taxOffice: String?,
    @SerializedName("maxAllowedBranches") val maxAllowedBranches: Int,
    @SerializedName("activeBranchCount") val activeBranchCount: Int
)

data class UpdateCompanyCommand(
    @SerializedName("legalName") val legalName: String,
    @SerializedName("tradeName") val tradeName: String,
    @SerializedName("taxNumber") val taxNumber: String,
    @SerializedName("taxOffice") val taxOffice: String
)

data class BranchDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("branchCode") val branchCode: String,
    @SerializedName("address") val address: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("openingTime") val openingTime: String,
    @SerializedName("closingTime") val closingTime: String,
    @SerializedName("isActive") val isActive: Boolean
)

data class CreateBranchCommand(
    @SerializedName("name") val name: String,
    @SerializedName("branchCode") val branchCode: String,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("openingTime") val openingTime: String,
    @SerializedName("closingTime") val closingTime: String
)

data class UpdateBranchCommand(
    @SerializedName("name") val name: String,
    @SerializedName("branchCode") val branchCode: String,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("openingTime") val openingTime: String,
    @SerializedName("closingTime") val closingTime: String
)

interface BranchService {
    @GET("/api/v1/company")
    suspend fun getCompany(): Response<CompanyDto>

    @PUT("/api/v1/company")
    suspend fun updateCompany(@Body body: UpdateCompanyCommand): Response<CompanyDto>

    @GET("/api/v1/branches")
    suspend fun getBranches(): Response<List<BranchDto>>

    @POST("/api/v1/branches")
    suspend fun createBranch(@Body body: CreateBranchCommand): Response<BranchDto>

    @PUT("/api/v1/branches/{id}")
    suspend fun updateBranch(@Path("id") id: String, @Body body: UpdateBranchCommand): Response<BranchDto>

    @POST("/api/v1/branches/{id}/activate")
    suspend fun activateBranch(@Path("id") id: String): Response<BranchDto>

    @POST("/api/v1/branches/{id}/deactivate")
    suspend fun deactivateBranch(@Path("id") id: String): Response<BranchDto>
}

