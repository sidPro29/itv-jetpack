package com.notifiy.itv.data.repository

import android.util.Log
import com.notifiy.itv.BuildConfig
import com.notifiy.itv.data.model.ItvPlan
import com.notifiy.itv.data.model.ItvPurchase
import com.notifiy.itv.data.model.PaymentIntentResponse
import com.notifiy.itv.data.remote.ApiService
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StripeRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiService: ApiService
) {
    private val TAG = "siddharthaLogs"
    private val STRIPE_SECRET_KEY = BuildConfig.STRIPE_SECRET_KEY

    suspend fun getMembershipLevels(): List<ItvPlan> {
        return try {
            Log.d(TAG, "Fetching membership levels from backend...")
            apiService.getMembershipLevels()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching plans from backend: ${e.message}")
            getDefaultPlans()
        }
    }

    private fun getDefaultPlans() = listOf(
        ItvPlan("8270", "Basic SD All Access AVOD", 1.99, "EUR", "Monthly", "Basic SD", "SD quality, with ads"),
        ItvPlan("8271", "Basic SD All Access AVOD Yearly", 19.99, "EUR", "Yearly", "Basic SD", "SD quality, with ads (Yearly)"),
        ItvPlan("8272", "Standard HD Monthly", 4.99, "EUR", "Monthly", "Standard HD", "High Definition"),
        ItvPlan("8273", "Standard HD Yearly", 49.99, "EUR", "Yearly", "Standard HD", "High Definition (Yearly)"),
        ItvPlan("8274", "Premium UHD Monthly", 7.99, "EUR", "Monthly", "Premium UHD", "Ultra High Definition"),
        ItvPlan("8275", "Premium UHD Yearly", 79.99, "EUR", "Yearly", "Premium UHD", "Ultra High Definition (Yearly)")
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
        return try {
            val params = mapOf(
                "planId" to plan.id,
                "paymentIntentId" to paymentIntentId
            )
            val response = apiService.confirmPurchase(params)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Node.js Backend purchase registered successfully.")
                sessionManager.updateActivePlan(plan.name)
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Failed to register purchase on backend: $errorBody")
                Result.failure(Exception("Failed to register purchase: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "confirmPurchase Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserPurchases(): List<ItvPurchase> {
        return try {
            apiService.getUserPurchases()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching purchases from backend: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun hasActivePlan(planName: String): Boolean {
        val purchases = getUserPurchases()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return purchases.any { it.plan_name == planName && it.expiry_date > now }
    }
}
