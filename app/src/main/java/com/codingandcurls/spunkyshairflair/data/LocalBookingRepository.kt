package com.codingandcurls.spunkyshairflair.data

import android.content.Context
import com.codingandcurls.spunkyshairflair.model.Booking
import org.json.JSONArray
import org.json.JSONObject

class LocalBookingRepository(context: Context) : BookingRepository {
    private val prefs = context.applicationContext.getSharedPreferences("spunky_bookings", Context.MODE_PRIVATE)

    override fun getAll(): List<Booking> {
        val array = JSONArray(prefs.getString("bookings", "[]") ?: "[]")
        val list = mutableListOf<Booking>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(Booking(o.getLong("id"), o.getLong("userId"), o.getString("service"),
                o.getInt("price"), o.getString("stylistName"), o.getString("date"),
                o.getString("time"), o.getInt("deposit"), o.getString("status")))
        }
        return list
    }

    private fun save(bookings: List<Booking>) {
        val array = JSONArray()
        bookings.forEach { b ->
            array.put(JSONObject()
                .put("id", b.id).put("userId", b.userId).put("service", b.service)
                .put("price", b.price).put("stylistName", b.stylistName).put("date", b.date)
                .put("time", b.time).put("deposit", b.deposit).put("status", b.status))
        }
        prefs.edit().putString("bookings", array.toString()).apply()
    }

    override fun getForUser(userId: Long): List<Booking> = getAll().filter { it.userId == userId }

    override fun add(booking: Booking) {
        save(getAll() + booking)
    }

    override fun cancel(bookingId: Long) {
        save(getAll().filter { it.id != bookingId })
    }

    override fun nextId(): Long = System.currentTimeMillis()
}
