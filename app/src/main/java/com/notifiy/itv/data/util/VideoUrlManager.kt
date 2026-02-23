package com.notifiy.itv.data.util

object VideoUrlManager {
    // Hardcoded list of video URLs indexed by card ID
    private val hardcodedVideos = mapOf(
        25524 to "https://play.webvideocore.net/popplayer.php?it=td54sa9zvwgw&autoplay=1",
//        29938 to "https://play.webvideocore.net/popplayer.php?it=td54sa9zvwgw&autoplay=1"
        // Add more hardcoded video URLs here as needed
        // 12345 to "https://your-video-url.com",
    )

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
     */
    fun getVideoUrl(id: Int, apiVideoUrl: String?): String {
        val hardcoded = getHardcodedVideoUrl(id)
        if (!hardcoded.isNullOrEmpty()) {
            return hardcoded
        }
        return apiVideoUrl ?: ""
    }
}
