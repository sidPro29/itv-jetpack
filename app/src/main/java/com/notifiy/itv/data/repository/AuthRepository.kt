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
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                // Fetch user data from Firestore
                val userDoc = firestore.collection("itv_users").document(user.uid).get().await()
                val itvUser = userDoc.toObject(ItvUser::class.java)
                
                sessionManager.saveAuthToken(user.uid)
                sessionManager.saveUserInfo(
                    user.email ?: "",
                    itvUser?.name ?: user.displayName ?: "",
                    itvUser?.active_plan ?: ""
                )
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<Boolean> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                val itvUserData = mapOf(
                    "user_id" to user.uid,
                    "name" to name,
                    "email" to email,
                    "mobile" to "",
                    "active_plan" to "",
                    "plan_exp" to ""
                )
                
                // Save to Firestore using explicit UID
                firestore.collection("itv_users").document(user.uid).set(itvUserData).await()
                
                sessionManager.saveAuthToken(user.uid)
                sessionManager.saveUserInfo(email, name, "")
                
                Result.success(true)
            } else {
                Result.failure(Exception("Signup failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
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

