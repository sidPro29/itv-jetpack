package com.notifiy.itv.data.remote

import com.notifiy.itv.data.model.Post
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("custom-streamit/v1/videos")
    suspend fun getVideos(): com.notifiy.itv.data.model.AssetResponse

    @GET("custom-streamit/v1/movies")
    suspend fun getMovies(): com.notifiy.itv.data.model.AssetResponse

    @GET("custom-streamit/v1/tvshows")
    suspend fun getTVShows(): com.notifiy.itv.data.model.AssetResponse


    @retrofit2.http.POST("jwt-auth/v1/token")
    suspend fun login(
        @retrofit2.http.Body request: com.notifiy.itv.data.model.LoginRequest
    ): com.notifiy.itv.data.model.LoginResponse

    @retrofit2.http.POST("wp/v2/users")
    suspend fun signup(
        @retrofit2.http.Body request: com.notifiy.itv.data.model.WpSignupRequest
    ): retrofit2.Response<okhttp3.ResponseBody>

    @retrofit2.http.GET("wp/v2/users/me")
    suspend fun getMe(
        @retrofit2.http.Header("Authorization") authHeader: String
    ): com.notifiy.itv.data.model.WpUserResponse

    @retrofit2.http.GET("pmpro/v1/get_membership_level_for_user")
    suspend fun getMembershipForUser(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @retrofit2.http.Query("user_id") userId: Long
    ): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.GET("pmpro/v1/membership_levels")
    suspend fun getMembershipLevels(
        @retrofit2.http.Header("Authorization") authHeader: String
    ): retrofit2.Response<Map<String, com.notifiy.itv.data.model.MembershipLevel>>

    
    @retrofit2.http.GET("pmpro/v1/get_orders")
    suspend fun getOrders(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @retrofit2.http.Query("user_id") userId: Long
    ): retrofit2.Response<okhttp3.ResponseBody>


    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("pmpro/v1/change_membership_level")
    suspend fun changeMembershipLevel(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @retrofit2.http.Field("level_id") levelId: String,
        @retrofit2.http.Field("user_id") userId: Long? = null
    ): retrofit2.Response<okhttp3.ResponseBody>

    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("https://api.stripe.com/v1/payment_intents")
    suspend fun createPaymentIntent(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @retrofit2.http.Field("amount") amount: Long,
        @retrofit2.http.Field("currency") currency: String,
        @retrofit2.http.Field("payment_method_types[]") paymentMethodType: String = "card",
        @retrofit2.http.Field("description") description: String
    ): com.notifiy.itv.data.model.PaymentIntentResponse
}
