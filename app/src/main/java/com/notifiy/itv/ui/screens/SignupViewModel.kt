package com.notifiy.itv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    val uiState: StateFlow<SignupUiState> = _uiState

    fun signup(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = SignupUiState.Loading
            val result = authRepository.signup(name, email, password)
            result.onSuccess {
                _uiState.value = SignupUiState.Success
            }.onFailure {
                _uiState.value = SignupUiState.Error(it.message ?: "An unknown error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = SignupUiState.Idle
    }
}

sealed class SignupUiState {
    object Idle : SignupUiState()
    object Loading : SignupUiState()
    object Success : SignupUiState()
    data class Error(val message: String) : SignupUiState()
}
