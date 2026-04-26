package com.notifiy.itv.data.util

object VideoUrlManager {

    private const val BAD_URL_PREFIX = "http://103.133.215.134/interplanetary/wp-content/uploads/"
    private const val GOOD_URL_PREFIX = "https://interplanetary.tv/wp-content/uploads/"

    // Hardcoded list of video URLs indexed by card ID
    private val hardcodedVideos = mapOf(
        25524 to "https://play.webvideocore.net/popplayer.php?it=td54sa9zvwgw&autoplay=1",
//        29938 to "https://play.webvideocore.net/popplayer.php?it=td54sa9zvwgw&autoplay=1"
        // Add more hardcoded video URLs here as needed
        // 12345 to "https://your-video-url.com",
    )

    /**
     * Fixes an incorrect IP-based video URL at runtime.
     * If the URL starts with the raw IP address, replaces it with the correct domain.
     * Data files are never touched — this fix happens only before playback.
     */
    fun fixVideoUrl(url: String): String {
        return if (url.startsWith(BAD_URL_PREFIX)) {
            url.replaceFirst(BAD_URL_PREFIX, GOOD_URL_PREFIX)
        } else {
            url
        }
    }

    /**
     * Gets the video URL for a given ID.
     * Returns the hardcoded URL if available for the provided ID.
     */
    fun getHardcodedVideoUrl(id: Int): String? {
        return hardcodedVideos[id]
    }

    /**
     * Resolves the video URL for a post.
     * Priority: 1. Hardcoded fallback URL, 2. API provided URL
     * Also applies fixVideoUrl() to ensure IP-based URLs are corrected at runtime.
     */
    fun getVideoUrl(id: Int, apiVideoUrl: String?): String {
        val hardcoded = getHardcodedVideoUrl(id)
        if (!hardcoded.isNullOrEmpty()) {
            return hardcoded
        }
        return fixVideoUrl(apiVideoUrl ?: "")
    }
}
