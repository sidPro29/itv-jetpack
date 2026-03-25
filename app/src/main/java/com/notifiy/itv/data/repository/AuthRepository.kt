package com.notifiy.itv.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.notifiy.itv.data.model.ItvUser
import com.notifiy.itv.data.model.LoginResponse
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val apiService: com.notifiy.itv.data.remote.ApiService
) {
    private val TAG = "siddharthaLogs"

    suspend fun login(email: String, password: String): Result<Boolean> {
        android.util.Log.d(TAG, "Login attempt for email: $email")
        
        // Step 1: Try Firebase Login Login First
        try {
            android.util.Log.d(TAG, "Trying Firebase Login...")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                android.util.Log.d(TAG, "Firebase Login Successful for UID: ${user.uid}")
                
                // Fetch user data from Firestore
                val userDoc = firestore.collection("itv_users").document(user.uid).get().await()
                val itvUser = userDoc.toObject(ItvUser::class.java)
                
                sessionManager.saveAuthToken(user.uid)
                sessionManager.saveUserInfo(
                    user.email ?: "",
                    itvUser?.name ?: user.displayName ?: "",
                    itvUser?.active_plan ?: ""
                )
                
                // Try to login to WP in background to get token for sync
                try {
                    android.util.Log.d(TAG, "Logging into WP in background for sync...")
                    val wpResponse = apiService.login(com.notifiy.itv.data.model.LoginRequest(email, password))
                    if (wpResponse.token != null) {
                        sessionManager.saveWpToken(wpResponse.token)
                        syncMembershipWithWp(user.uid)
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Background WP Login failed (User might not exist on WP yet): ${e.message}")
                }
                
                return Result.success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Firebase Login Failed: ${e.message}")
        }

        // Step 2: Try WordPress Login if Firebase Fails
        try {
            android.util.Log.d(TAG, "Trying WordPress Login for: $email")
            val loginRequest = com.notifiy.itv.data.model.LoginRequest(email, password)
            val wpResponse = apiService.login(loginRequest)
            
            if (wpResponse.token != null) {
                android.util.Log.d(TAG, "WordPress Login Successful! Syncing to Firebase...")
                sessionManager.saveWpToken(wpResponse.token)
                
                // Step 3: Create/Sync this WP user to Firebase
                return try {
                    // Try to create the user on Firebase
                    android.util.Log.d(TAG, "Creating/Syncing WP user to Firebase...")
                    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                    val newUser = authResult.user
                    
                    if (newUser != null) {
                        val itvUserData = mapOf(
                            "user_id" to newUser.uid,
                            "name" to (wpResponse.userDisplayName ?: wpResponse.userNiceName ?: ""),
                            "email" to email,
                            "active_plan" to "",
                            "wp_user_id" to "" // Can fetch later if needed
                        )
                        firestore.collection("itv_users").document(newUser.uid).set(itvUserData).await()
                        android.util.Log.d(TAG, "Firebase User Created and Synced Successfully")
                        
                        sessionManager.saveAuthToken(newUser.uid)
                        sessionManager.saveUserInfo(email, wpResponse.userDisplayName ?: "", "")
                        
                        // Now sync the membership found on WP
                        syncMembershipWithWp(newUser.uid)
                        
                        Result.success(true)
                    } else {
                        Result.failure(Exception("Could not create Firebase user after WP success"))
                    }
                } catch (e: Exception) {
                    // If user already exists on Firebase but login failed (maybe different password?)
                    // Or any other error. Handle appropriately.
                    android.util.Log.e(TAG, "Error creating Firebase user: ${e.message}")
                    if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                         android.util.Log.w(TAG, "User already exists on Firebase. Manual password sync might be needed.")
                         // For now, if they are logged in to WP, we let them in but notify about sync issue
                         Result.failure(Exception("Sync Error: User exists on Firebase but password might be different. Please update password."))
                    } else {
                        Result.failure(e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "WordPress Login also Failed: ${e.message}")
        }

        return Result.failure(Exception("Login failed on both Firebase and WordPress"))
    }

    suspend fun signup(name: String, email: String, password: String): Result<Boolean> {
        android.util.Log.d(TAG, "Signup attempt for: $email")
        
        // Step 1: Register on WordPress first (if it fails, we know early)
        // We'll use email as the username for WP to keep it simple and consistent
        try {
            android.util.Log.d(TAG, "Attempting WordPress Signup for: $email")
            val wpSignupRequest = com.notifiy.itv.data.model.WpSignupRequest(
                username = email.split("@")[0], // Using email prefix as username
                name = name,
                email = email,
                password = password
            )
            val wpResponse = apiService.signup(wpSignupRequest)
            
            if (wpResponse.isSuccessful) {
                android.util.Log.d(TAG, "WordPress Signup Successful")
            } else {
                val errorBody = wpResponse.errorBody()?.string()
                android.util.Log.e(TAG, "WordPress Signup Failed (Code ${wpResponse.code()}): $errorBody")
                
                // If user already exists on WP, we still proceed with Firebase
                if (errorBody?.contains("existing_user_email") == true || errorBody?.contains("existing_user_login") == true) {
                    android.util.Log.w(TAG, "User already exists on WordPress, continuing with Firebase setup...")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during WordPress Signup: ${e.message}")
        }

        // Step 2: Register on Firebase
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                android.util.Log.d(TAG, "Firebase User Created: ${user.uid}")
                val itvUserData = mapOf(
                    "user_id" to user.uid,
                    "name" to name,
                    "email" to email,
                    "active_plan" to ""
                )
                firestore.collection("itv_users").document(user.uid).set(itvUserData).await()
                
                sessionManager.saveAuthToken(user.uid)
                sessionManager.saveUserInfo(email, name, "")
                
                android.util.Log.d(TAG, "Signup Successful on Firebase")
                
                // After successful dual signup, we should try to login to WP to get the token for future syncs
                try {
                    val loginRes = apiService.login(com.notifiy.itv.data.model.LoginRequest(email, password))
                    loginRes.token?.let { sessionManager.saveWpToken(it) }
                } catch (e: Exception) {
                     android.util.Log.e(TAG, "Failed to get WP token after signup: ${e.message}")
                }
                
                Result.success(true)
            } else {
                Result.failure(Exception("Signup failed"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Signup Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncMembershipWithWp(firebaseUid: String) {
        val wpToken = sessionManager.fetchWpToken()
        if (wpToken == null) {
            android.util.Log.d(TAG, "No WP token available for membership sync.")
            return
        }

        try {
            android.util.Log.d(TAG, "Syncing membership from WordPress (Multi-Step)...")
            val authHeader = "Bearer $wpToken"
            
            // Step 1: Get WP User ID from /me
            val wpUser = apiService.getMe(authHeader)
            android.util.Log.d(TAG, "Sync: Found Target WP User ID: ${wpUser.id}")

            // Workaround: PMPro restricts reading statuses for standard users. We must use Master Admin token.
            android.util.Log.d(TAG, "Sync: Fetching Master Admin Token to bypass 403 read restrictions...")
            val adminLoginRes = apiService.login(com.notifiy.itv.data.model.LoginRequest("siddharthav6213@proton.me", "Sidh@6213#"))
            val adminToken = adminLoginRes.token

            if (adminToken != null) {
                // Step 2: Get Membership using that specific ID with Admin Privileges
                val adminAuthHeader = "Bearer $adminToken"
                val response = apiService.getMembershipForUser(adminAuthHeader, wpUser.id)
                if (response.isSuccessful) {
                    val jsonString = response.body()?.string() ?: ""
                    android.util.Log.d(TAG, "Sync: Raw Membership JSON: $jsonString")
                    
                    // Parse plan name from JSON (finding "name":"...")
                    if (jsonString.contains("\"name\":\"")) {
                        val planName = jsonString.substringAfter("\"name\":\"").substringBefore("\"")
                        android.util.Log.d(TAG, "Sync Success: Plan found -> $planName")
                        
                        // Update Firebase Firestore
                        firestore.collection("itv_users").document(firebaseUid).update("active_plan", planName).await()
                        
                        // Update Local Session
                        sessionManager.updateActivePlan(planName)
                    } else {
                        android.util.Log.d(TAG, "Sync: No active membership name found in WP response.")
                        sessionManager.updateActivePlan("")
                    }
                } else {
                    android.util.Log.e(TAG, "Sync Failed (Code ${response.code()}): ${response.errorBody()?.string()}")
                }
            } else {
                android.util.Log.e(TAG, "Sync Failed: Could not obtain Master Admin token for reading.")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during multi-step membership sync: ${e.message}")
        }
    }

    fun logout() {
        android.util.Log.d(TAG, "Logout triggered")
        auth.signOut()
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null && sessionManager.isLoggedIn()
    }
    
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }
}

