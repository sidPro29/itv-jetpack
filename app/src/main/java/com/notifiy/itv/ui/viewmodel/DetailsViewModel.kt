package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.notifiy.itv.data.repository.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _isInWatchlist = MutableStateFlow(false)
    val isInWatchlist = _isInWatchlist.asStateFlow()

    fun checkWatchlistStatus(id: Int) {
        _isInWatchlist.value = sessionManager.isInWatchlist(id)
    }

    fun toggleWatchlist(id: Int) {
        sessionManager.toggleWatchlist(id)
        _isInWatchlist.value = sessionManager.isInWatchlist(id)
    }
}
