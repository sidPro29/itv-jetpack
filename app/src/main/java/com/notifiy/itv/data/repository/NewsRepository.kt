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
        Log.d(TAG, "NewsRepository: Fetching articles — page=$page, perPage=$perPage")
        return try {
            val articles = apiService.getNewsArticles(categories = 10794, perPage = perPage, page = page)
            Log.d(TAG, "NewsRepository: Fetched ${articles.size} articles")
            articles.forEachIndexed { i, a ->
                Log.d(TAG, "NewsRepository: [$i] id=${a.id} title='${a.title.rendered}' thumb='${a.getThumbnailUrl()}'")
            }
            Result.success(articles)
        } catch (e: Exception) {
            Log.e(TAG, "NewsRepository: Error fetching articles: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchNewsArticles(query: String): Result<List<NewsArticle>> {
        Log.d(TAG, "NewsRepository: Searching articles — query='$query'")
        return try {
            val articles = apiService.searchNewsArticles(query = query, categories = 10794, perPage = 10)
            Log.d(TAG, "NewsRepository: Search returned ${articles.size} results for '$query'")
            Result.success(articles)
        } catch (e: Exception) {
            Log.e(TAG, "NewsRepository: Search error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getNewsArticleById(id: Int): Result<NewsArticle> {
        Log.d(TAG, "NewsRepository: Fetching article by id=$id")
        return try {
            val article = apiService.getNewsArticleById(id = id)
            Log.d(TAG, "NewsRepository: Fetched article '${article.title.rendered}', contentLen=${article.content?.rendered?.length}")
            Result.success(article)
        } catch (e: Exception) {
            Log.e(TAG, "NewsRepository: Error fetching article id=$id: ${e.message}", e)
            Result.failure(e)
        }
    }
}
