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
    val selectedPlan: ItvPlan? = null,
    val availablePlans: List<ItvPlan> = emptyList()
)

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val sessionManager: com.notifiy.itv.data.repository.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val plans = stripeRepository.getMembershipLevels()
            _uiState.update { it.copy(isLoading = false, availablePlans = plans) }
        }
    }

    fun toggleBillingCycle() {
        _uiState.update { it.copy(
            selectedBillingCycle = if (it.selectedBillingCycle == "Monthly") "Yearly" else "Monthly"
        ) }
    }
    fun purchasePlan(plan: ItvPlan) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPaymentProcessing = true, error = null, selectedPlan = plan) }
            
            // Checks for already active plan (both WP and Firestore)
            val currentWpPlan = sessionManager.fetchActivePlan()
            val isAlreadyActiveInFirestore = stripeRepository.hasActivePlan(plan.name)
            
            if (currentWpPlan == plan.name || isAlreadyActiveInFirestore) {
                _uiState.update { it.copy(isPaymentProcessing = false, error = "You already have this plan, choose other") }
                return@launch
            }

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

