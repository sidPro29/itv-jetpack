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
        android.util.Log.d(TAG, "WordPress Direct Login attempt for: $email")
        
        return try {
            val loginRequest = com.notifiy.itv.data.model.LoginRequest(email, password)
            val wpResponse = apiService.login(loginRequest)
            
            if (wpResponse.token != null) {
                android.util.Log.d(TAG, "WordPress Login Successful!")
                sessionManager.saveWpToken(wpResponse.token)
                sessionManager.saveAuthToken(wpResponse.token) // Reusing WP Token as Auth Token
                sessionManager.saveUserInfo(
                    wpResponse.userEmail ?: email,
                    wpResponse.userDisplayName ?: wpResponse.userNiceName ?: "",
                    "" // Plan will be fetched next
                )
                
                // Fetch membership status from WordPress (PMPro)
                syncMembershipWithWp()
                
                Result.success(true)
            } else {
                Result.failure(Exception(wpResponse.message ?: "Invalid login credentials"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "WordPress Login Failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<Boolean> {
        android.util.Log.d(TAG, "WordPress Direct Signup attempt for: $email")
        
        return try {
            val wpSignupRequest = WpSignupRequest(
                username = email.split("@")[0], // Email prefix as username
                name = name,
                email = email,
                password = password
            )
            val wpResponse = apiService.signup(wpSignupRequest)
            
            if (wpResponse.isSuccessful) {
                android.util.Log.d(TAG, "WordPress Signup Successful. Logging in...")
                // After signup, automatically login to get the token
                login(email, password)
            } else {
                val errorBody = wpResponse.errorBody()?.string()
                android.util.Log.e(TAG, "WordPress Signup Failed: $errorBody")
                Result.failure(Exception("Signup failed on WordPress: $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during WordPress Signup: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncMembershipWithWp() {
        val wpToken = sessionManager.fetchWpToken()
        if (wpToken == null) {
            android.util.Log.d(TAG, "No WP token available for membership sync.")
            return
        }

        try {
            android.util.Log.d(TAG, "Syncing membership from WordPress...")
            val authHeader = "Bearer $wpToken"
            
            // Step 1: Get current WP User ID
            val wpUser = apiService.getMe(authHeader)
            sessionManager.saveWpUserId(wpUser.id)
            
            // Step 2: Get Membership using that WP User ID

            // Using admin token if needed (as in old code) to ensure bypass restrictions
            val adminLoginRes = try {
                apiService.login(com.notifiy.itv.data.model.LoginRequest("siddharthav6213@proton.me", "Sidh@6213#"))
            } catch (e: Exception) { null }
            
            val fetchHeader = adminLoginRes?.token?.let { "Bearer $it" } ?: authHeader
            
            val response = apiService.getMembershipForUser(fetchHeader, wpUser.id)
            if (response.isSuccessful) {
                val jsonString = response.body()?.string() ?: ""
                android.util.Log.d(TAG, "Membership JSON: $jsonString")
                
                if (jsonString.contains("\"name\":\"")) {
                    val planName = jsonString.substringAfter("\"name\":\"").substringBefore("\"")
                    android.util.Log.d(TAG, "Plan found -> $planName")
                    sessionManager.updateActivePlan(planName)
                } else {
                    sessionManager.updateActivePlan("")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during direct membership sync: ${e.message}")
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
        // Return WP Token or some identifier from sessionManager
        return sessionManager.fetchAuthToken()
    }
}
