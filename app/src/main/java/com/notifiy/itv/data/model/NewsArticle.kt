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

    /** Decode all HTML entities + strip tags using Android's Html parser */
    private fun decodeHtml(html: String): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(html).toString()
            }
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "")
        }
    }

    /** Clean title — decodes &#8217; &#8220; &#8221; &amp; etc. */
    fun getCleanTitle(): String = decodeHtml(title.rendered).trim()

    /** Strip HTML tags, decode entities, remove membership wall text */
    private fun stripHtml(html: String): String {
        return decodeHtml(html)
            .replace(Regex("Membership Required.*", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("You must be a member.*", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

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

    fun getTags(): List<String> {
        return embedded?.terms
            ?.flatten()
            ?.filter { it.taxonomy == "post_tag" }
            ?.map { it.name }
            ?: emptyList()
    }

    fun getAuthorName(): String {
        return embedded?.author?.firstOrNull()?.name ?: "Interplanetary Team"
    }
}

data class NewsEmbedded(
    @SerializedName("author")
    val author: List<WpAuthor>?,
    @SerializedName("wp:featuredmedia")
    val featuredMedia: List<FeaturedMedia>?,
    @SerializedName("wp:term")
    val terms: List<List<WpTerm>>?
)

data class WpAuthor(
    val id: Int,
    val name: String
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
