package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    val password: String,
    val skip2FA: Boolean = true
)

data class LoginUser(
    val id: String,
    val username: String,
    val email: String,
    val role: String,
    @SerializedName("activePlans")
    val activePlans: List<UserActivePlan>? = null
)

data class LoginResponse(
    val token: String?,
    val user: LoginUser?,
    val message: String? = null,
    val code: String? = null
) {
    val userEmail: String? get() = user?.email
    val userNiceName: String? get() = user?.username
    val userDisplayName: String? get() = user?.username
}

data class WpSignupRequest(
    val username: String,
    val name: String,
    val email: String,
    val password: String
)

data class WpUserResponse(
    @SerializedName("_id")
    val idString: String,
    val username: String?,
    val email: String?,
    val mobile: String?,
    val role: String?,
    @SerializedName("activePlans")
    val activePlans: List<UserActivePlan>? = null
) {
    val id: Long get() = idString.hashCode().toLong()
}

data class UserActivePlan(
    val planName: String? = null,
    val planId: String? = null,
    val expiryDate: String? = null
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
    @SerializedName("_id") val id: String = "",
    val name: String = "",
    @SerializedName("amount") val price: Double = 0.0,
    val currency: String = "EUR",
    val billingCycle: String = "Monthly", // "Monthly" or "Yearly"
    @SerializedName("type") val category: String = "regular", // The backend uses 'type' instead of 'category'
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
