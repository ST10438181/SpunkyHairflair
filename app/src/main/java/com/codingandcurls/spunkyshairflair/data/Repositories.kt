package com.codingandcurls.spunkyshairflair.data

import android.content.Context
import com.codingandcurls.spunkyshairflair.model.Booking
import com.codingandcurls.spunkyshairflair.model.User

/**
 * DATA LAYER CONTRACT.
 * The screens only talk to these two interfaces. For now they are backed by
 * SharedPreferences (Local*Repository). When the DBMS / online API is ready, write
 * new classes that implement these interfaces and return them from [Repos] -
 * no screen code has to change.
 */
interface UserRepository {
    fun emailExists(email: String): Boolean
    /** Creates the account and signs the user in. Returns null if the email is already used. */
    fun register(name: String, email: String, phone: String, password: String): User?
    /** Returns the user and signs them in, or null if the email/password is wrong. */
    fun login(email: String, password: String): User?
    fun getCurrentUser(): User?
    fun updateProfile(userId: Long, name: String, phone: String): User?
    /** Returns false if [current] is not the user's current password. */
    fun changePassword(userId: Long, current: String, new: String): Boolean
    fun logout()
}

interface BookingRepository {
    fun getForUser(userId: Long): List<Booking>
    fun getAll(): List<Booking>
    fun add(booking: Booking)
    fun cancel(bookingId: Long)
    fun nextId(): Long
}

object Repos {
    fun users(context: Context): UserRepository = LocalUserRepository(context)
    fun bookings(context: Context): BookingRepository = LocalBookingRepository(context)
}
