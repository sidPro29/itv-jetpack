package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String?,
    @SerializedName("user_email")
    val userEmail: String?,
    @SerializedName("user_nicename")
    val userNiceName: String?,
    @SerializedName("user_display_name")
    val userDisplayName: String?,
    val message: String?,
    val code: String?
)

data class WpSignupRequest(
    val username: String,
    val name: String,
    val email: String,
    val password: String
)

data class WpUserResponse(
    val id: Long,
    val username: String,
    val name: String
)

data class WpMembershipResponse(
    val membership_level: MembershipLevel? = null
)

data class MembershipLevel(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val initial_payment: String? = null,
    val billing_amount: String? = null,
    val cycle_number: String? = null,
    val cycle_period: String? = null,
    val billing_limit: String? = null,
    val trial_amount: String? = null,
    val trial_limit: String? = null
)

data class ItvUser(
    val user_id: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val active_plan: String = "",
    val plan_exp: String = ""
)

data class ItvPlan(
    val id: String,
    val name: String,
    val price: Double,
    val currency: String = "EUR",
    val billingCycle: String, // "Monthly" or "Yearly"
    val category: String, // "Basic SD", "Standard HD", "Premium HD", "Premium+ 4K", "Blogger Ads", etc.
    val description: String = "",
    val benefits: List<String> = emptyList()
)

data class ItvPurchase(
    val purchase_id: String = "",
    val user_id: String = "",
    val plan_name: String = "",
    val amount: Double = 0.0,
    val currency: String = "EUR",
    val purchase_date: String = "",
    val expiry_date: String = "",
    val status: String = "Success",
    val benefits: List<String> = emptyList(),
    val stripe_payment_id: String = ""
)


data class PaymentIntentRequest(
    val amount: Long,
    val currency: String,
    val payment_method_types: List<String> = listOf("card"),
    val description: String? = null,
    val customer_email: String? = null
)

data class PaymentIntentResponse(
    val id: String,
    val client_secret: String,
    val amount: Long,
    val currency: String,
    val status: String
)
