package com.notifiy.itv.data.model

import com.google.gson.annotations.SerializedName

data class NewsArticle(
    @SerializedName("_id")
    val id: String,
    @SerializedName("publishedDate")
    val date: String?,
    @SerializedName("createdAt")
    val createdAtStr: String?,
    @SerializedName("title")
    private val titleStr: String?,
    @SerializedName("description")
    private val descriptionStr: String?,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("images")
    val imagesList: List<String>? = null,
    @SerializedName("keywords")
    val keywordsList: List<String>? = null,
    @SerializedName("author")
    val author: String? = null
) {
    val link: String get() = ""
    val title: RenderedContent get() = RenderedContent(titleStr ?: "")
    val excerpt: RenderedContent get() = RenderedContent(descriptionStr ?: "")
    val content: RenderedContent get() = RenderedContent(descriptionStr ?: "")
    val featuredMediaId: Int get() = 0

    fun getThumbnailUrl(): String {
        val rawUrl = imageUrl ?: imagesList?.firstOrNull() ?: ""
        return if (rawUrl.startsWith("/api/")) {
            "https://api.interplanetary.tv$rawUrl"
        } else {
            rawUrl
        }
    }

    fun getCleanTitle(): String = titleStr ?: ""

    fun getCleanExcerpt(): String = descriptionStr ?: ""

    fun getCleanContent(): String = descriptionStr ?: ""

    fun getFormattedDate(): String {
        val dateVal = date ?: createdAtStr ?: ""
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            val d = inputFormat.parse(dateVal)
            if (d != null) outputFormat.format(d) else dateVal
        } catch (e: Exception) {
            dateVal
        }
    }

    fun getTags(): List<String> = keywordsList ?: emptyList()

    fun getAuthorName(): String = author ?: "Interplanetary Team"
}
