package com.notifiy.itv.data.remote

import com.notifiy.itv.data.model.Post
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("streamit/v1/videos")
    suspend fun getVideos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 700
    ): List<Post>

    @GET("streamit/v1/movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<Post>

    @GET("streamit/v1/tvshows")
    suspend fun getTVShows(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<Post>

    @retrofit2.http.POST("jwt-auth/v1/token")
    suspend fun login(
        @retrofit2.http.Body request: com.notifiy.itv.data.model.LoginRequest
    ): com.notifiy.itv.data.model.LoginResponse
}
