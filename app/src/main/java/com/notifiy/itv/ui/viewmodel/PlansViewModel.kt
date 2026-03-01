package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.ItvPlan
import com.notifiy.itv.data.repository.StripeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansUiState(
    val isLoading: Boolean = false,
    val isPaymentProcessing: Boolean = false,
    val paymentSuccess: Boolean = false,
    val error: String? = null,
    val selectedBillingCycle: String = "Monthly", // "Monthly" or "Yearly"
    val clientSecret: String? = null,
    val selectedPlan: ItvPlan? = null
)

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val sessionManager: com.notifiy.itv.data.repository.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState = _uiState.asStateFlow()

    val availablePlans = listOf(
        // Basic SD
        ItvPlan("basic_sd_m", "Basic SD Monthly", 1.99, "EUR", "Monthly", "Basic SD", "All Access AVOD", listOf("SD Quality", "With Ads")),
        ItvPlan("basic_sd_y", "Basic SD Yearly", 19.99, "EUR", "Yearly", "Basic SD", "All Access AVOD", listOf("SD Quality", "With Ads", "Save 15%")),
        
        // Standard HD
        ItvPlan("std_hd_m", "Standard HD Monthly", 5.99, "EUR", "Monthly", "Standard HD", "HD Access", listOf("HD Quality", "No Ads")),
        ItvPlan("std_hd_y", "Standard HD Yearly", 59.00, "EUR", "Yearly", "Standard HD", "HD Access", listOf("HD Quality", "No Ads", "Save 18%")),
        
        // Premium HD
        ItvPlan("prem_hd_m", "Premium HD Monthly", 8.99, "EUR", "Monthly", "Premium HD", "High Quality", listOf("HD Quality", "No Ads", "Multi-device")),
        ItvPlan("prem_hd_y", "Premium HD Yearly", 89.99, "EUR", "Yearly", "Premium HD", "High Quality", listOf("HD Quality", "No Ads", "Multi-device")),
        
        // Premium+ 4K
        ItvPlan("prem_4k_m", "Premium+ 4K Monthly", 9.99, "EUR", "Monthly", "Premium+ 4K", "Ultra HD", listOf("4K + HDR", "No Ads", "All Access")),
        ItvPlan("prem_4k_y", "Premium+ 4K Yearly", 99.00, "EUR", "Yearly", "Premium+ 4K", "Ultra HD", listOf("4K + HDR", "No Ads", "All Access")),
        
        // Blogger Ads Plan
        ItvPlan("blogger_1_m", "Blogger 1 Ad Plan", 199.0, "EUR", "Monthly", "Blogger Ads Plan", "1 Ad Spot", listOf("1 Monthly Ad Slot", "Monthly Reach Stats")),
        ItvPlan("blogger_2_m", "Blogger 2 Ad Plan", 299.0, "EUR", "Monthly", "Blogger Ads Plan", "2 Ad Spots", listOf("2 Monthly Ad Slots", "Detailed Analytics")),

        // Small Business Ads Plan
        ItvPlan("sm_biz_1_m", "Small Business 1 Ad Plan", 999.0, "EUR", "Monthly", "Small Business Ads Plan", "1 Ad Spot", listOf("1 Monthly Ad Slot", "Priority Support", "Email Notifications")),
        ItvPlan("sm_biz_2_m", "Small Business 2 Ad Plan", 1999.0, "EUR", "Monthly", "Small Business Ads Plan", "2 Ad Spots", listOf("2 Monthly Ad Slots", "Dedicated Account Manager")),

        // Business Ads Plan
        ItvPlan("biz_1_m", "Business 1 Ad Plan", 2999.0, "EUR", "Monthly", "Business Ads Plan", "1 Ad Spot", listOf("1 Ad Slot", "Campaign Performance Report")),
        ItvPlan("biz_2_m", "Business 2 Ad Plan", 4999.0, "EUR", "Monthly", "Business Ads Plan", "2 Ad Spots", listOf("2 Ad Slots", "Global Targeting Options"))
    )

    fun toggleBillingCycle() {
        _uiState.update { it.copy(
            selectedBillingCycle = if (it.selectedBillingCycle == "Monthly") "Yearly" else "Monthly"
        ) }
    }

    fun purchasePlan(plan: ItvPlan) {
        val currentActivePlan = sessionManager.fetchActivePlan()
        if (currentActivePlan == plan.name) {
            _uiState.update { it.copy(error = "You already have an active ${plan.name} plan.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPaymentProcessing = true, error = null, selectedPlan = plan) }
            
            val result = stripeRepository.createPaymentIntent(plan)
            result.onSuccess { response ->
                _uiState.update { it.copy(clientSecret = response.client_secret) }
            }.onFailure { e ->
                _uiState.update { it.copy(isPaymentProcessing = false, error = e.message) }
            }
        }
    }

    fun finalizePurchase(paymentIntentId: String) {
        val plan = _uiState.value.selectedPlan ?: return
        viewModelScope.launch {
            val result = stripeRepository.confirmPurchase(plan, paymentIntentId)
            result.onSuccess {
                _uiState.update { it.copy(isPaymentProcessing = false, paymentSuccess = true, clientSecret = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isPaymentProcessing = false, error = e.message, clientSecret = null) }
            }
        }
    }

    fun onPaymentSheetCancelled() {
        _uiState.update { it.copy(isPaymentProcessing = false, clientSecret = null) }
    }

    fun resetPaymentStatus() {
        _uiState.update { it.copy(paymentSuccess = false, error = null) }
    }
}
