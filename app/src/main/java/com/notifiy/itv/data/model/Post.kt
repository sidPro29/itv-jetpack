package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName
import com.notifiy.itv.data.util.VideoUrlManager

data class AssetResponse(
    val page: Int,
    @SerializedName("per_page")
    val perPage: Int,
    val total: Int,
    @SerializedName("total_pages")
    val totalPages: Int,
    val results: List<Post>
)

data class Post(
    @SerializedName("asset_id")
    val id: Int,
    val category: String, // video, movie, tvshow
    @SerializedName("imageUrl")
    val imageUrl: String?,
    @SerializedName("videoUrl")
    val videoUrl: String?,
    @SerializedName("title")
    private val _title: String?,
    val description: String?,
    val tag: String?,
    val genre: String?,
    @SerializedName("membership_level")
    private val _membershipLevel: String? // comma separated string like "20354, 20353"
) {
    val title: RenderedContent get() = RenderedContent(_title ?: "")
    
    // UI backward compatibility
    val portraitPoster: String get() = imageUrl ?: ""
    val membershipLevel: List<String> get() = _membershipLevel?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun getDisplayImageUrl(): String = imageUrl ?: ""
    fun getEffectiveVideoUrl(): String = VideoUrlManager.fixVideoUrl(videoUrl ?: "")
}

data class RenderedContent(val rendered: String)
