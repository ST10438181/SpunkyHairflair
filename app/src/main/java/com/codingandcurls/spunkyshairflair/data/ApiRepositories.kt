package com.codingandcurls.spunkyshairflair.data

import com.codingandcurls.spunkyshairflair.model.Booking
import com.codingandcurls.spunkyshairflair.model.Service
import com.codingandcurls.spunkyshairflair.model.SlotAvailability
import com.codingandcurls.spunkyshairflair.model.Stylist
import com.codingandcurls.spunkyshairflair.model.User
import org.json.JSONArray
import org.json.JSONObject

class ApiUserRepository(
    private val api: ApiClient,
    private val session: SessionStorage
) : UserRepository {

    override suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String
    ): ApiResult<User> {
        val body = JSONObject()
            .put("name", name.trim())
            .put("email", email.trim())
            .put("phone", phone.replace(" ", ""))
            .put("password", password)
        return authenticate("auth/register", body)
    }

    override suspend fun login(email: String, password: String): ApiResult<User> {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
        return authenticate("auth/login", body)
    }

    private suspend fun authenticate(path: String, body: JSONObject): ApiResult<User> {
        val result = api.execute("POST", path, body, auth = false) { text ->
            val json = JSONObject(text)
            Pair(json.getString("token"), ApiJson.user(json.getJSONObject("user")))
        }
        return when (result) {
            is ApiResult.Success -> {
                session.save(result.value.first, result.value.second)
                ApiResult.Success(result.value.second)
            }
            is ApiResult.Failure -> result
        }
    }

    override fun getCurrentUser(): User? = session.user()

    override suspend fun updateProfile(name: String, phone: String): ApiResult<User> {
        val body = JSONObject()
            .put("name", name.trim())
            .put("phone", phone.replace(" ", ""))
        val result = api.execute("PUT", "me", body) { text -> ApiJson.user(JSONObject(text)) }
        if (result is ApiResult.Success) session.saveUser(result.value)
        return result
    }

    override suspend fun changePassword(current: String, new: String): ApiResult<Unit> {
        val body = JSONObject()
            .put("currentPassword", current)
            .put("newPassword", new)
        return api.execute("PUT", "me/password", body) { }
    }

    override suspend fun logout() {
        try {
            api.execute("POST", "auth/logout") { }
        } finally {
            // Always forget the session locally, even if the server could not be reached.
            session.clear()
        }
    }
}

class ApiCatalogRepository(private val api: ApiClient) : CatalogRepository {
    private var cachedServices: List<Service>? = null
    private var cachedStylists: List<Stylist>? = null

    override suspend fun services(): ApiResult<List<Service>> {
        cachedServices?.let { return ApiResult.Success(it) }
        val result = api.execute("GET", "services", auth = false) { text ->
            ApiJson.list(JSONArray(text)) { ApiJson.service(it) }
        }
        if (result is ApiResult.Success) cachedServices = result.value
        return result
    }

    override suspend fun stylists(): ApiResult<List<Stylist>> {
        cachedStylists?.let { return ApiResult.Success(it) }
        val result = api.execute("GET", "stylists", auth = false) { text ->
            ApiJson.list(JSONArray(text)) { ApiJson.stylist(it) }
        }
        if (result is ApiResult.Success) cachedStylists = result.value
        return result
    }

    override suspend fun availability(stylistId: Int, date: String): ApiResult<List<SlotAvailability>> =
        api.execute("GET", "availability?stylistId=$stylistId&date=$date", auth = false) { text ->
            ApiJson.list(JSONObject(text).getJSONArray("slots")) { ApiJson.slot(it) }
        }
}

class ApiBookingRepository(private val api: ApiClient) : BookingRepository {
    override suspend fun getMine(): ApiResult<List<Booking>> =
        api.execute("GET", "bookings") { text ->
            ApiJson.list(JSONArray(text)) { ApiJson.booking(it) }
        }

    override suspend fun add(
        serviceId: Int,
        stylistId: Int,
        date: String,
        time: String
    ): ApiResult<Booking> {
        val body = JSONObject()
            .put("serviceId", serviceId)
            .put("stylistId", stylistId)
            .put("date", date)
            .put("time", time)
        return api.execute("POST", "bookings", body) { text -> ApiJson.booking(JSONObject(text)) }
    }

    override suspend fun cancel(bookingId: Long): ApiResult<Unit> =
        api.execute("DELETE", "bookings/$bookingId") { }
}
