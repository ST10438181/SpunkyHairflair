package com.codingandcurls.spunkyshairflair.data

import android.content.Context
import com.codingandcurls.spunkyshairflair.model.User
import org.json.JSONException
import org.json.JSONObject

/** Remembers who is logged in: the API bearer token plus a copy of the user's profile. */
interface SessionStorage {
    fun token(): String?
    fun user(): User?
    fun save(token: String, user: User)
    fun saveUser(user: User)
    fun clear()
}

class SharedPrefsSession(context: Context) : SessionStorage {
    private val prefs =
        context.applicationContext.getSharedPreferences("spunky_session", Context.MODE_PRIVATE)

    override fun token(): String? = prefs.getString("token", null)

    override fun user(): User? {
        if (token() == null) return null
        val text = prefs.getString("user", null) ?: return null
        return try {
            ApiJson.user(JSONObject(text))
        } catch (e: JSONException) {
            null
        }
    }

    override fun save(token: String, user: User) {
        prefs.edit()
            .putString("token", token)
            .putString("user", ApiJson.userToJson(user).toString())
            .apply()
    }

    override fun saveUser(user: User) {
        prefs.edit().putString("user", ApiJson.userToJson(user).toString()).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
