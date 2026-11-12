package com.codingandcurls.spunkyshairflair.model

data class Service(val id: Int, val name: String, val price: Int, val duration: String)

data class Stylist(
    val id: Int,
    val name: String,
    val specialty: String,
    val rating: Double,
    val reviews: Int
)

/** The signed-in customer. Passwords are hashed on the server and never reach the app. */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String
)

data class Booking(
    val id: Long,
    val userId: Long,
    val service: String,
    val price: Int,
    val stylistName: String,
    val date: String,   // ISO yyyy-MM-dd, same format as the website
    val time: String,   // e.g. "9:00 AM"
    val deposit: Int,
    val status: String
)

/** One time slot for a stylist on a date. [reason] is "past", "booked" or "" when available. */
data class SlotAvailability(val time: String, val available: Boolean, val reason: String)
