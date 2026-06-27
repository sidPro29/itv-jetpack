package com.notifiy.itv.data.repository

import com.notifiy.itv.data.model.ItvUser
import com.notifiy.itv.data.model.LoginResponse
import com.notifiy.itv.data.model.WpSignupRequest
import com.notifiy.itv.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    private val TAG = "siddharthaLogs"

    suspend fun login(email: String, password: String): Result<Boolean> {
        android.util.Log.d(TAG, "Node.js Backend Login attempt for: $email")
        
        return try {
            val loginRequest = com.notifiy.itv.data.model.LoginRequest(email, password)
            val response = apiService.login(loginRequest)
            
            if (response.token != null) {
                android.util.Log.d(TAG, "Node.js Backend Login Successful!")
                sessionManager.saveWpToken(response.token)
                sessionManager.saveAuthToken(response.token) // Unified Token
                
                sessionManager.saveUserInfo(
                    response.user?.email ?: email,
                    response.user?.username ?: "",
                    ""
                )
                
                // Fetch and sync active plan membership
                syncMembershipWithWp()
                
                Result.success(true)
            } else {
                Result.failure(Exception(response.message ?: "Invalid login credentials"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Node.js Backend Login Failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<Boolean> {
        android.util.Log.d(TAG, "Node.js Backend Signup attempt for: $email")
        
        return try {
            val signupRequest = WpSignupRequest(
                username = email.split("@")[0],
                name = name,
                email = email,
                password = password
            )
            val response = apiService.signup(signupRequest)
            
            if (response.isSuccessful) {
                android.util.Log.d(TAG, "Node.js Backend Signup Successful. Logging in...")
                login(email, password)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e(TAG, "Node.js Backend Signup Failed: $errorBody")
                Result.failure(Exception("Signup failed on backend: $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during Node.js Backend Signup: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncMembershipWithWp() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            android.util.Log.d(TAG, "No auth token available for membership sync.")
            return
        }

        try {
            android.util.Log.d(TAG, "Syncing membership from Node.js Backend...")
            val user = apiService.getMe()
            sessionManager.saveWpUserId(user.id)
            
            sessionManager.saveUserInfo(
                user.email ?: "",
                user.username ?: "",
                "" // Plan set below
            )
            
            val activePlan = user.activePlans?.firstOrNull()?.planName ?: ""
            android.util.Log.d(TAG, "Membership plan synced: $activePlan")
            sessionManager.updateActivePlan(activePlan)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during membership sync: ${e.message}")
        }
    }

    fun logout() {
        android.util.Log.d(TAG, "Logout triggered")
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }
    
    fun getCurrentUserUid(): String? {
        return sessionManager.fetchAuthToken()
    }

    suspend fun cancelMembership(wpUserId: Long): Boolean {
        // Stripe subscriptions cancelation handled server-side / mock success
        sessionManager.updateActivePlan("")
        return true
    }
}
