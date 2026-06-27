package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName

data class MediaVideos(
    @SerializedName("clipId") val clipId: String? = null,
    @SerializedName("ytUrl") val ytUrl: String? = null,
    @SerializedName("svpRefNo") val svpRefNo: String? = null
)

data class MembershipPlan(
    @SerializedName("planId") val planId: String? = null,
    @SerializedName("planName") val planName: String? = null
)

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
    @SerializedName("_id")
    val mongoId: String? = null,
    @SerializedName("wp_asset_id")
    private val _id: Int? = null,
    @SerializedName("type")
    val type: String, // "video", "movie", "tvshow", "movies", "tvshows"
    @SerializedName("images")
    val images: List<String>? = null,
    @SerializedName("title")
    private val _title: String?,
    val description: String?,
    @SerializedName("genres")
    val genresList: List<String>? = null,
    @SerializedName("tags")
    val tagsList: List<String>? = null,
    @SerializedName("videos")
    val videos: Map<String, Any>? = null,
    @SerializedName("trailer")
    val trailer: Map<String, Any>? = null,
    @SerializedName("membership_level")
    val membershipPlanList: List<MembershipPlan>? = null
) {
    val id: Int get() = _id ?: mongoId?.hashCode() ?: 0
    val title: RenderedContent get() = RenderedContent(_title ?: "")

    val category: String get() = when (type.lowercase()) {
        "movies" -> "movie"
        "tvshows" -> "tvshow"
        else -> type
    }

    val tag: String? get() = tagsList?.joinToString(", ")
    val genre: String? get() = genresList?.joinToString(", ")

    val imageUrl: String? get() = images?.firstOrNull() ?: ""
    val portraitPoster: String get() = imageUrl ?: ""

    val membershipLevel: List<String> get() = membershipPlanList?.mapNotNull { it.planName } ?: emptyList()

    fun getDisplayImageUrl(): String {
        val rawUrl = imageUrl ?: ""
        return if (rawUrl.startsWith("/api/")) {
            "https://api.interplanetary.tv$rawUrl"
        } else {
            rawUrl
        }
    }

    fun getEffectiveVideoUrl(): String {
        val v = videos ?: return ""
        val clipId = v["clipId"] as? String
        if (!clipId.isNullOrEmpty()) {
            return "https://api.interplanetary.tv/api/media-assets/playback/$clipId"
        }
        val ytUrl = v["ytUrl"] as? String ?: v["youtube"] as? String
        if (!ytUrl.isNullOrEmpty()) {
            return ytUrl
        }
        for ((key, value) in v) {
            if (key.startsWith("non-svp") && value is String && value.startsWith("http")) {
                return value
            }
        }
        return ""
    }

    fun getEffectiveTrailerUrl(): String {
        val t = trailer ?: return ""
        val clipId = t["clipId"] as? String
        if (!clipId.isNullOrEmpty()) {
            return "https://api.interplanetary.tv/api/media-assets/playback/$clipId"
        }
        val ytUrl = t["ytUrl"] as? String ?: t["youtube"] as? String
        if (!ytUrl.isNullOrEmpty()) {
            return ytUrl
        }
        for ((key, value) in t) {
            if (key.startsWith("non-svp") && value is String && value.startsWith("http")) {
                return value
            }
        }
        return ""
    }
}


data class RenderedContent(val rendered: String)

