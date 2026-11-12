package com.codingandcurls.spunkyshairflair.data

import android.content.Context
import com.codingandcurls.spunkyshairflair.BuildConfig
import com.codingandcurls.spunkyshairflair.model.Booking
import com.codingandcurls.spunkyshairflair.model.Service
import com.codingandcurls.spunkyshairflair.model.SlotAvailability
import com.codingandcurls.spunkyshairflair.model.Stylist
import com.codingandcurls.spunkyshairflair.model.User

/**
 * DATA LAYER CONTRACT. Screens only talk to these interfaces; the implementations
 * (ApiRepositories.kt) call the REST API, which stores everything in the database.
 */
interface UserRepository {
    /** Creates the account and signs the user in. */
    suspend fun register(name: String, email: String, phone: String, password: String): ApiResult<User>

    /** Signs the user in. Fails with code 401 when the email or password is wrong. */
    suspend fun login(email: String, password: String): ApiResult<User>

    /** The signed-in user from the saved session (no network needed), or null. */
    fun getCurrentUser(): User?

    suspend fun updateProfile(name: String, phone: String): ApiResult<User>
    suspend fun changePassword(current: String, new: String): ApiResult<Unit>
    suspend fun logout()
}

interface CatalogRepository {
    suspend fun services(): ApiResult<List<Service>>
    suspend fun stylists(): ApiResult<List<Stylist>>
    suspend fun availability(stylistId: Int, date: String): ApiResult<List<SlotAvailability>>
}

interface BookingRepository {
    suspend fun getMine(): ApiResult<List<Booking>>
    suspend fun add(serviceId: Int, stylistId: Int, date: String, time: String): ApiResult<Booking>
    suspend fun cancel(bookingId: Long): ApiResult<Unit>
}

object Repos {
    private class Instances(
        val users: UserRepository,
        val bookings: BookingRepository,
        val catalog: CatalogRepository
    )

    private var instances: Instances? = null

    @Synchronized
    private fun get(context: Context): Instances {
        instances?.let { return it }
        val session = SharedPrefsSession(context)
        val api = ApiClient(BuildConfig.API_BASE_URL, UrlConnectionTransport(), session)
        val created = Instances(
            ApiUserRepository(api, session),
            ApiBookingRepository(api),
            ApiCatalogRepository(api)
        )
        instances = created
        return created
    }

    fun users(context: Context): UserRepository = get(context).users
    fun bookings(context: Context): BookingRepository = get(context).bookings
    fun catalog(context: Context): CatalogRepository = get(context).catalog
}
