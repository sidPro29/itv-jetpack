package com.notifiy.itv.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val TAG = "siddharthaLogs"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String): Result<Boolean> {
        android.util.Log.d(TAG, "Firebase Login attempt for: $email")
        
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                android.util.Log.d(TAG, "Firebase Login Successful!")
                val token = user.getIdToken(true).await().token ?: ""
                sessionManager.saveAuthToken(token)
                sessionManager.saveUserInfo(
                    user.email ?: email,
                    user.displayName ?: email.split("@")[0],
                    "" // Plan will be fetched next
                )
                
                // Sync membership (To be implemented with Node.js Stripe Backend)
                syncMembershipWithNode()
                
                Result.success(true)
            } else {
                Result.failure(Exception("Invalid login credentials"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Firebase Login Failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<Boolean> {
        android.util.Log.d(TAG, "Firebase Signup attempt for: $email")
        
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                android.util.Log.d(TAG, "Firebase Signup Successful. Logging in...")
                // Optionally update profile with name
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
                
                // Fetch token and save session
                val token = user.getIdToken(true).await().token ?: ""
                sessionManager.saveAuthToken(token)
                sessionManager.saveUserInfo(email, name, "")
                
                Result.success(true)
            } else {
                Result.failure(Exception("Signup failed on Firebase"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during Firebase Signup: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncMembershipWithNode() {
        // TODO: Call Node.js backend to get Stripe subscription status
        android.util.Log.d(TAG, "Syncing membership from Node.js (Pending Implementation)...")
        // For now, assume no active plan or mock it
        sessionManager.updateActivePlan("")
    }

    fun logout() {
        android.util.Log.d(TAG, "Logout triggered")
        auth.signOut()
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null || sessionManager.isLoggedIn()
    }
    
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    suspend fun cancelMembership(wpUserId: Long): Boolean {
        // TODO: Call Node.js backend to cancel Stripe subscription
        return false
    }
}
