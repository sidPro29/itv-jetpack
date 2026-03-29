package com.notifiy.itv.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.notifiy.itv.BuildConfig
import com.notifiy.itv.data.model.ItvPlan
import com.notifiy.itv.data.model.ItvPurchase
import com.notifiy.itv.data.model.MembershipLevel
import com.notifiy.itv.data.model.PaymentIntentResponse
import com.notifiy.itv.data.remote.ApiService
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StripeRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiService: ApiService,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "siddharthaLogs"
    private val STRIPE_PUBLISHABLE_KEY = BuildConfig.STRIPE_PUBLISHABLE_KEY
    private val STRIPE_SECRET_KEY = BuildConfig.STRIPE_SECRET_KEY

    suspend fun getMembershipLevels(): List<ItvPlan> {
        val wpToken = sessionManager.fetchWpToken() ?: ""
        val authHeader = "Bearer $wpToken"
        
        return try {
            val response = apiService.getMembershipLevels(authHeader)
            if (response.isSuccessful) {
                val levelsMap = response.body() ?: emptyMap()
                levelsMap.values.map { level ->
                    val price = (level.billing_amount ?: level.initial_payment ?: "0").toDoubleOrNull() ?: 0.0
                    val billingCycle = if (level.cycle_period?.lowercase()?.contains("year") == true) "Yearly" else "Monthly"
                    
                    ItvPlan(
                        id = level.id ?: "",
                        name = level.name ?: "Unknown Plan",
                        price = price,
                        currency = "EUR",
                        billingCycle = billingCycle,
                        category = level.name ?: "Membership",
                        description = level.description ?: ""
                    )
                }
            } else {
                Log.e(TAG, "Failed to fetch membership levels: ${response.code()}")
                getDefaultPlans()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching membership levels: ${e.message}")
            getDefaultPlans()
        }
    }

    private fun getDefaultPlans() = listOf(
        // Basic SD
        ItvPlan("8270", "Basic SD All Access AVOD", 1.99, "EUR", "Monthly", "Basic SD", "SD quality, with ads"),
        // Standard
        ItvPlan("8272", "Standard HD Monthly", 4.99, "EUR", "Monthly", "Standard HD", "High Definition"),
        // Premium
        ItvPlan("8274", "Premium UHD Monthly", 7.99, "EUR", "Monthly", "Premium UHD", "Ultra High Definition")
    )


    suspend fun createPaymentIntent(plan: ItvPlan): Result<PaymentIntentResponse> {
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

    suspend fun confirmPurchase(plan: ItvPlan, paymentIntentId: String): Result<Boolean> {
        val wpToken = sessionManager.fetchWpToken() ?: return Result.failure(Exception("WordPress Token not found."))
        val wpUserId = sessionManager.fetchWpUserId()
        
        if (wpUserId == -1L) {
             return Result.failure(Exception("WordPress User ID not found. Please log in again."))
        }

        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val purchaseDate = dateFormat.format(calendar.time)
            
            // Calculate expiry (1 month or 1 year)
            if (plan.billingCycle == "Yearly") {
                calendar.add(Calendar.YEAR, 1)
            } else {
                calendar.add(Calendar.MONTH, 1)
            }
            val expiryDate = dateFormat.format(calendar.time)

            // 1. Log in as Administrator to perform the upgrade API call
            val adminLoginRes = try {
                apiService.login(com.notifiy.itv.data.model.LoginRequest("siddhartha.verma", "sidSat@6213#"))
            } catch (e: Exception) { 
                Log.e(TAG, "Admin login failed: ${e.message}")
                null 
            }
            
            val adminToken = adminLoginRes?.token
            if (adminToken != null) {
                val adminAuthHeader = "Bearer $adminToken"
                val wpResponse = apiService.changeMembershipLevel(adminAuthHeader, plan.id, wpUserId)
                
                if (wpResponse.isSuccessful) {
                    Log.d(TAG, "WP membership updated successfully using admin token.")
                    
                    // 2. Save to Firestore (itv_purchase collection)
                    val purchase = ItvPurchase(
                        purchase_id = UUID.randomUUID().toString(),
                        user_id = wpUserId.toString(),
                        plan_name = plan.name,
                        amount = plan.price,
                        currency = plan.currency,
                        purchase_date = purchaseDate,
                        expiry_date = expiryDate,
                        status = "Success",
                        stripe_payment_id = paymentIntentId
                    )
                    
                    firestore.collection("itv_purchase")
                        .document(purchase.purchase_id)
                        .set(purchase)
                        .await()
                    
                    // 3. Update local session
                    sessionManager.updateActivePlan(plan.name)
                    
                    return Result.success(true)
                } else {
                    val errorBody = wpResponse.errorBody()?.string()
                    Log.e(TAG, "Failed to update membership with admin access: $errorBody")
                    return Result.failure(Exception("Failed to update membership (Admin Mode): ${wpResponse.code()} - $errorBody"))
                }
            } else {
                return Result.failure(Exception("Server Auth Error: Admin login failed. Check credentials."))
            }


        } catch (e: Exception) {
            Log.e(TAG, "confirmPurchase Error: ${e.message}")
            return Result.failure(e)
        }
    }

    suspend fun getUserPurchases(): List<ItvPurchase> {
        val wpUserId = sessionManager.fetchWpUserId()
        if (wpUserId == -1L) return emptyList()

        return try {
            val snapshot = firestore.collection("itv_purchase")
                .whereEqualTo("user_id", wpUserId.toString())
                .get()
                .await()
            
            snapshot.toObjects(ItvPurchase::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching purchases: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun hasActivePlan(planName: String): Boolean {
        val purchases = getUserPurchases()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        return purchases.any { it.plan_name == planName && it.expiry_date > now }
    }
}
