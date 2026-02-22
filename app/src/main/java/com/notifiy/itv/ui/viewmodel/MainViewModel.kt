package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.repository.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val itvRepository: com.notifiy.itv.data.repository.ItvRepository
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    fun updateLoginStatus() {
        _isLoggedIn.value = sessionManager.isLoggedIn()
        viewModelScope.launch {
            itvRepository.clearCache()
            _refreshTrigger.value += 1
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _isLoggedIn.value = false
        viewModelScope.launch {
            itvRepository.clearCache()
            _refreshTrigger.value += 1
        }
    }
}
