package com.codingandcurls.spunkyshairflair.data

import com.codingandcurls.spunkyshairflair.model.Booking
import com.codingandcurls.spunkyshairflair.model.Service
import com.codingandcurls.spunkyshairflair.model.SlotAvailability
import com.codingandcurls.spunkyshairflair.model.Stylist
import com.codingandcurls.spunkyshairflair.model.User
import org.json.JSONArray
import org.json.JSONObject

/** Converts the API's JSON (see docs/API.md) into the app's model classes. */
object ApiJson {
    fun user(o: JSONObject) = User(
        id = o.getLong("id"),
        name = o.getString("name"),
        email = o.getString("email"),
        phone = o.getString("phone")
    )

    fun userToJson(user: User): JSONObject = JSONObject()
        .put("id", user.id)
        .put("name", user.name)
        .put("email", user.email)
        .put("phone", user.phone)

    fun service(o: JSONObject) = Service(
        id = o.getInt("id"),
        name = o.getString("name"),
        price = o.getInt("price"),
        duration = o.getString("duration")
    )

    fun stylist(o: JSONObject) = Stylist(
        id = o.getInt("id"),
        name = o.getString("name"),
        specialty = o.getString("specialty"),
        rating = o.getDouble("rating"),
        reviews = o.getInt("reviews")
    )

    fun booking(o: JSONObject) = Booking(
        id = o.getLong("id"),
        userId = o.getLong("userId"),
        service = o.getString("service"),
        price = o.getInt("price"),
        stylistName = o.getString("stylistName"),
        date = o.getString("date"),
        time = o.getString("time"),
        deposit = o.getInt("deposit"),
        status = o.getString("status")
    )

    fun slot(o: JSONObject) = SlotAvailability(
        time = o.getString("time"),
        available = o.getBoolean("available"),
        reason = if (o.isNull("reason")) "" else o.getString("reason")
    )

    fun <T> list(array: JSONArray, map: (JSONObject) -> T): List<T> {
        val result = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            result.add(map(array.getJSONObject(i)))
        }
        return result
    }
}
