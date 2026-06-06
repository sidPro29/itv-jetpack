package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName

data class Post(
    @SerializedName("_id")
    val id: String,
    @SerializedName("type")
    val category: String, // video, movie, tvshow
    @SerializedName("images")
    val images: List<String>?,
    @SerializedName("videoUrl")
    val videoUrlList: List<String>?,
    @SerializedName("title")
    private val _title: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("tags")
    val tags: List<String>?,
    @SerializedName("genres")
    val genres: List<String>?,
    @SerializedName("svp_clip_id")
    val svpClipId: String?,
    @SerializedName("videoUrls")
    val videoUrls: VideoUrls?
) {
    val title: RenderedContent get() = RenderedContent(_title ?: "Untitled")
    
    // UI backward compatibility
    val portraitPoster: String get() = images?.firstOrNull() ?: ""
    val membershipLevel: List<String> get() = emptyList() // To be implemented with new Plans
    val tag: String get() = tags?.joinToString(", ") ?: ""
    val genre: String get() = genres?.joinToString(", ") ?: ""
    val imageUrl: String get() = portraitPoster
    val videoUrl: String get() = videoUrlList?.firstOrNull() ?: ""

    fun getDisplayImageUrl(): String = portraitPoster
    
    fun getEffectiveVideoUrl(): String {
        videoUrls?.let { urls ->
            if (category == "video" && !urls.hls.isNullOrEmpty()) return urls.hls
            return urls.mp4 ?: urls.hls ?: ""
        }
        if (!svpClipId.isNullOrEmpty()) {
            return "https://api.interplanetary.tv/api/media-assets/playback/$svpClipId"
        }
        return videoUrl
    }
}

data class VideoUrls(
    @SerializedName("hls")
    val hls: String?,
    @SerializedName("mp4")
    val mp4: String?
)

data class RenderedContent(val rendered: String)
