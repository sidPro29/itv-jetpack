package com.notifiy.itv.data.remote

import com.notifiy.itv.data.model.NewsArticle
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.model.ItvPlan
import com.notifiy.itv.data.model.ItvPurchase
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.Response
import okhttp3.ResponseBody

interface ApiService {

    @GET("articles")
    suspend fun getNewsArticles(): List<NewsArticle>

    @GET("articles")
    suspend fun searchNewsArticles(
        @Query("search") query: String
    ): List<NewsArticle>

    @GET("articles/{id}")
    suspend fun getNewsArticleById(
        @Path("id") id: String
    ): NewsArticle

    @GET("media-assets?type=video")
    suspend fun getVideosList(): List<Post>

    @GET("media-assets?type=movie")
    suspend fun getMoviesList(): List<Post>

    @GET("media-assets?type=tvshow")
    suspend fun getTVShowsList(): List<Post>

    @POST("auth/login")
    suspend fun login(
        @Body request: com.notifiy.itv.data.model.LoginRequest
    ): com.notifiy.itv.data.model.LoginResponse

    @POST("auth/signup")
    suspend fun signup(
        @Body request: com.notifiy.itv.data.model.WpSignupRequest
    ): Response<ResponseBody>

    @GET("users/me")
    suspend fun getMe(): com.notifiy.itv.data.model.WpUserResponse

    @GET("plans")
    suspend fun getMembershipLevels(): List<ItvPlan>

    @POST("plans/confirm-purchase")
    suspend fun confirmPurchase(
        @Body request: Map<String, String>
    ): Response<ResponseBody>

    @GET("users/my-purchases")
    suspend fun getUserPurchases(): List<ItvPurchase>

    @retrofit2.http.FormUrlEncoded
    @POST("https://api.stripe.com/v1/payment_intents")
    suspend fun createPaymentIntent(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @retrofit2.http.Field("amount") amount: Long,
        @retrofit2.http.Field("currency") currency: String,
        @retrofit2.http.Field("payment_method_types[]") paymentMethodType: String = "card",
        @retrofit2.http.Field("description") description: String
    ): com.notifiy.itv.data.model.PaymentIntentResponse
}
