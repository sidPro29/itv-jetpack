package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    @SerializedName("user_email")
    val userEmail: String?,
    @SerializedName("user_nicename")
    val userNiceName: String?,
    @SerializedName("user_display_name")
    val userDisplayName: String?,
    val message: String?,
    val code: String?
)
