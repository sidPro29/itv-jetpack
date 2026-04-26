package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.repository.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay

// Assuming StripeRepository and ItvPlan are defined elsewhere or will be added.
// For the purpose of this edit, we'll assume their existence.
// You might need to add imports for StripeRepository and ItvPlan if they are in different packages.
// For example:
// import com.notifiy.itv.data.repository.StripeRepository
// import com.notifiy.itv.data.model.ItvPlan

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: com.notifiy.itv.data.repository.AuthRepository,
    private val itvRepository: com.notifiy.itv.data.repository.ItvRepository,
    private val stripeRepository: com.notifiy.itv.data.repository.StripeRepository,
    private val sessionManager: com.notifiy.itv.data.repository.SessionManager
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _activePlan = MutableStateFlow(sessionManager.fetchActivePlan())
    val activePlan = _activePlan.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    private val _showExpiryPopup = MutableStateFlow(false)
    val showExpiryPopup = _showExpiryPopup.asStateFlow()

    init {
        checkPlanExpiry()
    }

    fun checkPlanExpiry() {
        if (!authRepository.isLoggedIn()) return
        
        viewModelScope.launch {
            val currentPlanName = sessionManager.fetchActivePlan()
            if (currentPlanName.isNullOrEmpty()) return@launch
            
            val purchases = stripeRepository.getUserPurchases()
            if (purchases.isEmpty()) return@launch
            
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val now = java.util.Date()
            
            // Collect all plans with this name and check their expiry dates
            val relevantPurchases = purchases.filter { it.plan_name == currentPlanName && it.status == "Success" }
            
            // If ALL purchases for this plan name have expired, then show the popup
            val isActiveInFirestore = relevantPurchases.any {
                try {
                    val expiryDate = dateFormat.parse(it.expiry_date)
                    expiryDate != null && expiryDate.after(now)
                } catch (e: Exception) { false }
            }
            
            if (!isActiveInFirestore && relevantPurchases.isNotEmpty()) {
                _showExpiryPopup.value = true
            }
        }
    }

    fun handleExpiryOk() {
        viewModelScope.launch {
            val wpUserId = sessionManager.fetchWpUserId()
            if (wpUserId != -1L) {
                authRepository.cancelMembership(wpUserId)
            }
            _showExpiryPopup.value = false
            updateLoginStatus() // Refresh UI
        }
    }

    fun updateLoginStatus() {
        _isLoggedIn.value = authRepository.isLoggedIn()
        _activePlan.value = sessionManager.fetchActivePlan()
        viewModelScope.launch {
            authRepository.syncMembershipWithWp()
            _activePlan.value = sessionManager.fetchActivePlan()
            itvRepository.clearCache()
            _refreshTrigger.value += 1
        }

    }

    fun logout() {
        authRepository.logout()
        _isLoggedIn.value = false
        _activePlan.value = null
        viewModelScope.launch {
            itvRepository.clearCache()
            _refreshTrigger.value += 1
        }
    }
}
