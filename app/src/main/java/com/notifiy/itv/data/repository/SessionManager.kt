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
        const val USER_EMAIL = "user_email"
        const val USER_NAME = "user_name"
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
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

    fun updateActivePlan(plan: String) {
        prefs.edit().putString("active_plan", plan).apply()
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.remove(USER_EMAIL)
        editor.remove(USER_NAME)
        editor.remove("active_plan")
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    fun toggleWatchlist(id: Int) {
        val watchlist = getWatchlist().toMutableSet()
        val idString = id.toString()
        if (watchlist.contains(idString)) {
            watchlist.remove(idString)
        } else {
            watchlist.add(idString)
        }
        prefs.edit().putStringSet("watchlist", watchlist).apply()
    }

    fun isInWatchlist(id: Int): Boolean {
        return getWatchlist().contains(id.toString())
    }

    fun getWatchlist(): Set<String> {
        return prefs.getStringSet("watchlist", emptySet()) ?: emptySet()
    }
}
