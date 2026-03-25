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
    private val sessionManager: com.notifiy.itv.data.repository.SessionManager
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _activePlan = MutableStateFlow(sessionManager.fetchActivePlan())
    val activePlan = _activePlan.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    fun updateLoginStatus() {
        _isLoggedIn.value = authRepository.isLoggedIn()
        _activePlan.value = sessionManager.fetchActivePlan()
        viewModelScope.launch {
            authRepository.getCurrentUserUid()?.let { uid ->
                authRepository.syncMembershipWithWp(uid)
                _activePlan.value = sessionManager.fetchActivePlan()
            }
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
