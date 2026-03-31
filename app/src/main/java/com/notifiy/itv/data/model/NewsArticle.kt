package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName

data class NewsArticle(
    val id: Int,
    val date: String,
    val link: String,
    val title: RenderedContent,
    val excerpt: RenderedContent,
    val content: RenderedContent? = null,
    @SerializedName("featured_media")
    val featuredMediaId: Int,
    @SerializedName("_embedded")
    val embedded: NewsEmbedded? = null
) {
    fun getThumbnailUrl(): String {
        return embedded?.featuredMedia
            ?.firstOrNull()
            ?.getImageUrl()
            ?: ""
    }

    /** Strip HTML + membership-wall boilerplate */
    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), "")
            .replace("\\n", " ")
            .replace(Regex("Membership Required.*", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("You must be a member.*", RegexOption.DOT_MATCHES_ALL), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

    fun getCleanExcerpt(): String = stripHtml(excerpt.rendered)

    fun getCleanContent(): String = content?.let { stripHtml(it.rendered) } ?: getCleanExcerpt()

    fun getFormattedDate(): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            val d = inputFormat.parse(date)
            if (d != null) outputFormat.format(d) else date
        } catch (e: Exception) {
            date
        }
    }

    /** Returns post_tag terms only */
    fun getTags(): List<String> {
        return embedded?.terms
            ?.flatten()
            ?.filter { it.taxonomy == "post_tag" }
            ?.map { it.name }
            ?: emptyList()
    }
}

data class NewsEmbedded(
    @SerializedName("wp:featuredmedia")
    val featuredMedia: List<FeaturedMedia>?,
    @SerializedName("wp:term")
    val terms: List<List<WpTerm>>?
)

data class WpTerm(
    val id: Int,
    val name: String,
    val slug: String,
    val taxonomy: String  // "category" or "post_tag"
)

data class FeaturedMedia(
    val id: Int,
    @SerializedName("source_url")
    val sourceUrl: String?,
    @SerializedName("media_details")
    val mediaDetails: MediaDetails?
) {
    fun getImageUrl(): String {
        return mediaDetails?.sizes?.mediumLarge?.sourceUrl
            ?: mediaDetails?.sizes?.large?.sourceUrl
            ?: mediaDetails?.sizes?.medium?.sourceUrl
            ?: sourceUrl
            ?: ""
    }
}

data class MediaDetails(val sizes: MediaSizes?)

data class MediaSizes(
    @SerializedName("medium") val medium: MediaSize?,
    @SerializedName("large") val large: MediaSize?,
    @SerializedName("medium_large") val mediumLarge: MediaSize?
)

data class MediaSize(
    @SerializedName("source_url") val sourceUrl: String?
)
