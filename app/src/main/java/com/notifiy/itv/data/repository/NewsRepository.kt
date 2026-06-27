package com.notifiy.itv.data.repository

import android.util.Log
import com.notifiy.itv.data.model.NewsArticle
import com.notifiy.itv.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val TAG = "siddharthaLogs"

    suspend fun getNewsArticles(page: Int = 1, perPage: Int = 20): Result<List<NewsArticle>> {
        Log.d(TAG, "NewsRepository: Fetching articles from Node.js backend...")
        return try {
            val articles = apiService.getNewsArticles()
            Log.d(TAG, "NewsRepository: Fetched ${articles.size} articles")
            Result.success(articles)
        } catch (e: Exception) {
            Log.e(TAG, "NewsRepository: Error fetching articles: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchNewsArticles(query: String): Result<List<NewsArticle>> {
        Log.d(TAG, "NewsRepository: Searching articles — query='$query'")
        return try {
            val articles = apiService.searchNewsArticles(query = query)
            Log.d(TAG, "NewsRepository: Search returned ${articles.size} results for '$query'")
            Result.success(articles)
        } catch (e: Exception) {
            Log.e(TAG, "NewsRepository: Search error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getNewsArticleById(id: String): Result<NewsArticle> {
        Log.d(TAG, "NewsRepository: Fetching article from backend by id=$id")
        return try {
            val article = apiService.getNewsArticleById(id = id)
            Log.d(TAG, "NewsRepository: Fetched article '${article.title.rendered}'")
            Result.success(article)
        } catch (e: Exception) {
            Log.e(TAG, "NewsRepository: Error fetching article id=$id: ${e.message}", e)
            Result.failure(e)
        }
    }
}
