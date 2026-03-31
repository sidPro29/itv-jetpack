package com.notifiy.itv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.NewsArticle
import com.notifiy.itv.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsDetailUiState(
    val isLoading: Boolean = true,
    val article: NewsArticle? = null,
    val error: String? = null
)

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "siddharthaLogs"
    private val articleId: Int = savedStateHandle.get<Int>("articleId") ?: 0

    private val _uiState = MutableStateFlow(NewsDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        Log.d(TAG, "NewsDetailViewModel: init — articleId=$articleId")
        loadArticle()
    }

    fun loadArticle() {
        if (articleId == 0) {
            Log.e(TAG, "NewsDetailViewModel: Invalid articleId=0")
            _uiState.update { it.copy(isLoading = false, error = "Invalid article ID") }
            return
        }
        Log.d(TAG, "NewsDetailViewModel: Loading article id=$articleId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            newsRepository.getNewsArticleById(id = articleId)
                .onSuccess { article ->
                    Log.d(TAG, "NewsDetailViewModel: Loaded '${article.title.rendered}', tags=${article.getTags()}")
                    _uiState.update { it.copy(isLoading = false, article = article) }
                }
                .onFailure { e ->
                    Log.e(TAG, "NewsDetailViewModel: Failed to load id=$articleId: ${e.message}", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load article") }
                }
        }
    }
}
