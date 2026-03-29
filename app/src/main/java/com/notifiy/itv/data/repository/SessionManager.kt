package com.notifiy.itv.data.repository

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
) {
    private val prefs: android.content.SharedPreferences = context.getSharedPreferences("itv_prefs", android.content.Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
        const val WP_TOKEN = "wp_token"
        const val USER_EMAIL = "user_email"
        const val USER_NAME = "user_name"
        const val WP_USER_ID = "wp_user_id"
    }

    fun saveWpUserId(id: Long) {
        prefs.edit().putLong(WP_USER_ID, id).apply()
    }

    fun fetchWpUserId(): Long {
        return prefs.getLong(WP_USER_ID, -1L)
    }

    fun saveAuthToken(token: String) {

        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    fun saveWpToken(token: String) {
        val editor = prefs.edit()
        editor.putString(WP_TOKEN, token)
        editor.apply()
    }

    fun fetchWpToken(): String? {
        return prefs.getString(WP_TOKEN, null)
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    fun saveUserInfo(email: String, name: String, activePlan: String = "") {
        val editor = prefs.edit()
        editor.putString(USER_EMAIL, email)
        editor.putString(USER_NAME, name)
        editor.putString("active_plan", activePlan)
        editor.apply()
    }

    fun fetchActivePlan(): String? {
        val plan = prefs.getString("active_plan", "")
        return if (plan.isNullOrEmpty()) null else plan
    }

    fun getUserName(): String {
        return prefs.getString(USER_NAME, "") ?: ""
    }

    fun getUserEmail(): String {
        return prefs.getString(USER_EMAIL, "") ?: ""
    }

    fun updateActivePlan(plan: String) {
        prefs.edit().putString("active_plan", plan).apply()
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.remove(WP_TOKEN)
        editor.remove(USER_EMAIL)
        editor.remove(USER_NAME)
        editor.remove(WP_USER_ID)
        editor.remove("active_plan")
        editor.apply()

    }

    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    fun toggleWatchlist(id: Int) {
        toggleSetItem("watchlist", id)
    }

    fun isInWatchlist(id: Int): Boolean {
        return getSet("watchlist").contains(id.toString())
    }

    fun getWatchlist(): Set<String> {
        return getSet("watchlist")
    }

    fun toggleLiked(id: Int) {
        toggleSetItem("liked", id)
    }

    fun isLiked(id: Int): Boolean {
        return getSet("liked").contains(id.toString())
    }

    fun getLiked(): Set<String> {
        return getSet("liked")
    }

    fun togglePlaylist(id: Int) {
        toggleSetItem("playlist", id)
    }

    fun isInPlaylist(id: Int): Boolean {
        return getSet("playlist").contains(id.toString())
    }

    fun getPlaylist(): Set<String> {
        return getSet("playlist")
    }

    private fun toggleSetItem(key: String, id: Int) {
        val set = getSet(key).toMutableSet()
        val idString = id.toString()
        if (set.contains(idString)) {
            set.remove(idString)
        } else {
            set.add(idString)
        }
        prefs.edit().putStringSet(key, set).apply()
    }

    private fun getSet(key: String): Set<String> {
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }
}
