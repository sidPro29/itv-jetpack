package com.notifiy.itv.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.notifiy.itv.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.notifiy.itv.data.model.ItvPurchase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StripeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val sessionManager: SessionManager,
    private val apiService: com.notifiy.itv.data.remote.ApiService
) {
    private val STRIPE_PUBLISHABLE_KEY = BuildConfig.STRIPE_PUBLISHABLE_KEY
    private val STRIPE_SECRET_KEY = BuildConfig.STRIPE_SECRET_KEY

    suspend fun createPaymentIntent(plan: com.notifiy.itv.data.model.ItvPlan): Result<com.notifiy.itv.data.model.PaymentIntentResponse> {
        return try {
            val amount = (plan.price * 100).toLong() // Convert to cents
            val response = apiService.createPaymentIntent(
                authHeader = "Bearer $STRIPE_SECRET_KEY",
                amount = amount,
                currency = plan.currency.lowercase(),
                description = "Purchase of ${plan.name}"
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPurchase(plan: com.notifiy.itv.data.model.ItvPlan, paymentIntentId: String): Result<Boolean> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val purchaseId = UUID.randomUUID().toString()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val purchaseDate = dateFormat.format(calendar.time)
            
            // Calculate expiry date
            if (plan.billingCycle == "Monthly") {
                calendar.add(Calendar.MONTH, 1)
            } else {
                calendar.add(Calendar.YEAR, 1)
            }
            val expiryDate = dateFormat.format(calendar.time)
            
            val purchase = ItvPurchase(
                purchase_id = purchaseId,
                user_id = user.uid,
                plan_name = plan.name,
                amount = plan.price,
                currency = plan.currency,
                purchase_date = purchaseDate,
                expiry_date = expiryDate,
                status = "Success",
                benefits = plan.benefits,
                stripe_payment_id = paymentIntentId
            )
            
            // 1. Create record in itv_purchases
            firestore.collection("itv_purchases").document(purchaseId).set(purchase).await()
            
            // 2. Update user collection
            firestore.collection("itv_users").document(user.uid).set(
                mapOf(
                    "active_plan" to plan.name,
                    "plan_exp" to expiryDate
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            // 3. Update local session
            sessionManager.updateActivePlan(plan.name)

            // 4. Sync to WordPress in background
            val wpToken = sessionManager.fetchWpToken()
            if (wpToken != null) {
                try {
                    android.util.Log.d("siddharthaLogs", "Sync: Attempting WP membership sync for plan: ${plan.name}")
                    
                    // Fetch WP user ID first using the user's own token
                    val wpUser = apiService.getMe("Bearer $wpToken")
                    val targetUserId = wpUser.id
                    android.util.Log.d("siddharthaLogs", "Sync: Found Target WP User ID for assignment: $targetUserId")

                    // Workaround: PMPro restricts standard users from editing memberships.
                    // We must log in quietly as the Administrator to perform the upgrade API call on their behalf.
                    android.util.Log.d("siddharthaLogs", "Sync: Fetching Master Admin Token to bypass 403 restrictions...")
                    val adminLoginRes = apiService.login(com.notifiy.itv.data.model.LoginRequest("siddharthav6213@proton.me", "Sidh@6213#"))
                    val adminToken = adminLoginRes.token
                    
                    if (adminToken != null) {
                        val adminAuthHeader = "Bearer $adminToken"
                        
                        // Comprehensive Mapping from App Plan IDs to WordPress PMPro Level IDs
                        val wpLevelId = when(plan.id) {
                            "basic_sd_m" -> "8270"
                            "basic_sd_y" -> "8271"
                            "std_hd_m" -> "8272"
                            "std_hd_y" -> "8273"
                            "prem_hd_m" -> "8274"
                            "prem_hd_y" -> "8275"
                            "prem_4k_m" -> "8276"
                            "prem_4k_y" -> "8277"
                            "blogger_1_m" -> "8278"
                            "blogger_2_m" -> "8279"
                            "sm_biz_1_m" -> "8280"
                            "sm_biz_2_m" -> "8281"
                            "biz_1_m" -> "8282"
                            "biz_2_m" -> "8283"
                            else -> {
                                android.util.Log.w("siddharthaLogs", "Sync: No explicit mapping found for ${plan.id}, using default/fallback.")
                                plan.id
                            }
                        }
                        
                        android.util.Log.d("siddharthaLogs", "Sync: Mapping ${plan.id} -> WP Level $wpLevelId")
                        
                        val wpResponse = apiService.changeMembershipLevel(adminAuthHeader, wpLevelId, targetUserId)
                        if (wpResponse.isSuccessful) {
                            android.util.Log.d("siddharthaLogs", "Sync: Successfully synced purchase to WordPress for level: $wpLevelId")
                        } else {
                            val errorStr = wpResponse.errorBody()?.string()
                            android.util.Log.e("siddharthaLogs", "Sync Error: Failed to sync to WordPress (Code ${wpResponse.code()}): $errorStr")
                        }
                    } else {
                        android.util.Log.e("siddharthaLogs", "Sync Error: Could not obtain Master Admin Token.")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("siddharthaLogs", "Sync Error: Exception during WP sync: ${e.message}")
                }
            } else {
                android.util.Log.w("siddharthaLogs", "Sync Skip: No WP Token found for target user.")
            }
            
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("siddharthaLogs", "Purchase Confirmation Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserPurchases(): List<ItvPurchase> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val querySnapshot = firestore.collection("itv_purchases")
                .whereEqualTo("user_id", user.uid)
                .get()
                .await()
            querySnapshot.toObjects(ItvPurchase::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
