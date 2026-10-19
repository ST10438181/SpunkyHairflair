package com.codingandcurls.spunkyshairflair.model

data class Service(val id: Int, val name: String, val price: Int, val duration: String)

data class Stylist(
    val id: Int,
    val name: String,
    val specialty: String,
    val rating: Double,
    val reviews: Int
)

/** Passwords are never stored in plain text - only [passwordHash] + [salt]. */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val salt: String
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
