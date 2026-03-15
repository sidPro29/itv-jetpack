package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName
import com.notifiy.itv.data.util.VideoUrlManager

data class Post(
    val id: Int,
    val title: Rendered,
    val content: Rendered,
    val excerpt: Rendered,
    val date: String,
    val link: String,
    @SerializedName("featured_media") val featuredMedia: Int,
    
    // Custom fields
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("video_embed") val videoEmbed: String?,
    @SerializedName("video_choice") val videoChoice: String?,
    @SerializedName("portrait_poster") val portraitPoster: String?,
    @SerializedName("portrait_image") val portraitImage: PortraitImage?,
    @SerializedName("membership_level") val membershipLevel: List<String>?,
    val subtitles: List<Subtitle>?,
    val _embedded: Embedded?
) {
    fun getDisplayImageUrl(): String {
        return portraitImage?.medium?.let { if (it.isNotEmpty()) it else null }
            ?: portraitPoster?.let { if (it.isNotEmpty()) it else null }
            ?: _embedded?.featuredMedia?.firstOrNull()?.sourceUrl
            ?: ""
    }

    fun getEffectiveVideoUrl(): String {
        return VideoUrlManager.getVideoUrl(id, videoUrl)
    }
}

data class Rendered(
    val rendered: String
)

data class PortraitImage(
    val id: Int,
    val full: String,
    val large: String,
    val medium: String,
    val thumbnail: String
)

data class Subtitle(
    @SerializedName("file_url") val fileUrl: String,
    val language: String
)

data class Embedded(
    @SerializedName("wp:featuredmedia") val featuredMedia: List<FeaturedMedia>?,
    val author: List<Author>?
)

data class FeaturedMedia(
    @SerializedName("source_url") val sourceUrl: String
)

data class Author(
    val id: Int,
    val name: String,
    @SerializedName("avatar_urls") val avatarUrls: AvatarUrls
)

data class AvatarUrls(
    @SerializedName("96") val small: String,
    @SerializedName("48") val medium: String,
    @SerializedName("24") val large: String
)
