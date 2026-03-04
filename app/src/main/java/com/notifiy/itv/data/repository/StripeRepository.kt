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
            
            Result.success(true)
        } catch (e: Exception) {
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
