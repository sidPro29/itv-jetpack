package com.notifiy.itv.data.repository

import com.notifiy.itv.data.model.LoginRequest
import com.notifiy.itv.data.model.LoginResponse
import com.notifiy.itv.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.token != null) {
                sessionManager.saveAuthToken(response.token)
                sessionManager.saveUserInfo(
                    response.userEmail ?: "",
                    response.userDisplayName ?: response.userNiceName ?: ""
                )
                Result.success(response)
            } else {
                Result.failure(Exception(response.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }
}
