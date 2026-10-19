package com.codingandcurls.spunkyshairflair.data

import android.content.Context
import com.codingandcurls.spunkyshairflair.logic.PasswordHasher
import com.codingandcurls.spunkyshairflair.model.User
import org.json.JSONArray
import org.json.JSONObject

class LocalUserRepository(context: Context) : UserRepository {
    private val prefs = context.applicationContext.getSharedPreferences("spunky_users", Context.MODE_PRIVATE)

    private fun load(): MutableList<User> {
        val array = JSONArray(prefs.getString("users", "[]") ?: "[]")
        val list = mutableListOf<User>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(User(o.getLong("id"), o.getString("name"), o.getString("email"),
                o.getString("phone"), o.getString("hash"), o.getString("salt")))
        }
        return list
    }

    private fun save(users: List<User>) {
        val array = JSONArray()
        users.forEach { u ->
            array.put(JSONObject()
                .put("id", u.id).put("name", u.name).put("email", u.email)
                .put("phone", u.phone).put("hash", u.passwordHash).put("salt", u.salt))
        }
        prefs.edit().putString("users", array.toString()).apply()
    }

    override fun emailExists(email: String): Boolean =
        load().any { it.email == email.trim().lowercase() }

    override fun register(name: String, email: String, phone: String, password: String): User? {
        if (emailExists(email)) return null
        val salt = PasswordHasher.newSalt()
        val user = User(
            id = System.currentTimeMillis(),
            name = name.trim(),
            email = email.trim().lowercase(),
            phone = phone.replace(" ", ""),
            passwordHash = PasswordHasher.hash(password, salt),
            salt = salt
        )
        val users = load()
        users.add(user)
        save(users)
        prefs.edit().putLong("session", user.id).apply()
        return user
    }

    override fun login(email: String, password: String): User? {
        val user = load().firstOrNull { it.email == email.trim().lowercase() } ?: return null
        if (!PasswordHasher.verify(password, user.salt, user.passwordHash)) return null
        prefs.edit().putLong("session", user.id).apply()
        return user
    }

    override fun getCurrentUser(): User? {
        if (!prefs.contains("session")) return null
        val id = prefs.getLong("session", -1L)
        return load().firstOrNull { it.id == id }
    }

    override fun updateProfile(userId: Long, name: String, phone: String): User? {
        val users = load()
        val index = users.indexOfFirst { it.id == userId }
        if (index < 0) return null
        val updated = users[index].copy(name = name.trim(), phone = phone.replace(" ", ""))
        users[index] = updated
        save(users)
        return updated
    }

    override fun changePassword(userId: Long, current: String, new: String): Boolean {
        val users = load()
        val index = users.indexOfFirst { it.id == userId }
        if (index < 0) return false
        val user = users[index]
        if (!PasswordHasher.verify(current, user.salt, user.passwordHash)) return false
        val salt = PasswordHasher.newSalt()
        users[index] = user.copy(salt = salt, passwordHash = PasswordHasher.hash(new, salt))
        save(users)
        return true
    }

    override fun logout() {
        prefs.edit().remove("session").apply()
    }
}
