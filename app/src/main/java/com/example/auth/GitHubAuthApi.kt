package com.example.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class AccessTokenResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "scope") val scope: String?,
    @Json(name = "error") val error: String?,
    @Json(name = "error_description") val errorDescription: String?
)

interface GitHubAuthApi {
    @POST("login/oauth/access_token")
    @FormUrlEncoded
    @Headers("Accept: application/json")
    suspend fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): AccessTokenResponse
}
