package com.codingandcurls.spunkyshairflair

import com.codingandcurls.spunkyshairflair.data.ApiBookingRepository
import com.codingandcurls.spunkyshairflair.data.ApiCatalogRepository
import com.codingandcurls.spunkyshairflair.data.ApiClient
import com.codingandcurls.spunkyshairflair.data.ApiResult
import com.codingandcurls.spunkyshairflair.data.ApiUserRepository
import com.codingandcurls.spunkyshairflair.data.HttpResponse
import com.codingandcurls.spunkyshairflair.data.HttpTransport
import com.codingandcurls.spunkyshairflair.data.SessionStorage
import com.codingandcurls.spunkyshairflair.model.User
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class InMemorySession : SessionStorage {
    private var token: String? = null
    private var user: User? = null
    override fun token(): String? = token
    override fun user(): User? = if (token == null) null else user
    override fun save(token: String, user: User) {
        this.token = token
        this.user = user
    }
    override fun saveUser(user: User) {
        this.user = user
    }
    override fun clear() {
        token = null
        user = null
    }
}

class RecordedRequest(val method: String, val url: String, val headers: Map<String, String>, val body: String?)

/** Stands in for the network: records requests and replays a canned response. */
class FakeTransport(private val respond: (RecordedRequest) -> HttpResponse) : HttpTransport {
    val requests = mutableListOf<RecordedRequest>()
    override fun execute(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse {
        val request = RecordedRequest(method, url, headers, body)
        requests.add(request)
        return respond(request)
    }
}

class ApiClientTests {
    private val base = "http://api.test/api/"
    private val userJson = """{"id":7,"name":"Lerato Dlamini","email":"lerato@example.com","phone":"0821234501"}"""

    private fun client(session: SessionStorage, transport: HttpTransport) = ApiClient(base, transport, session)

    @Test fun loginStoresTheTokenAndUserAndSendsNoAuthHeader() = runBlocking<Unit> {
        val session = InMemorySession()
        val transport = FakeTransport { HttpResponse(200, """{"token":"abc123","user":$userJson}""") }
        val users = ApiUserRepository(client(session, transport), session)

        val result = users.login(" lerato@example.com ", "Curls2026")

        assertTrue(result is ApiResult.Success)
        assertEquals("abc123", session.token())
        assertEquals("Lerato Dlamini", users.getCurrentUser()?.name)
        val request = transport.requests.single()
        assertEquals("POST", request.method)
        assertEquals("http://api.test/api/auth/login", request.url)
        assertFalse(request.headers.containsKey("Authorization"))
        assertEquals("lerato@example.com", JSONObject(request.body!!).getString("email"))
    }

    @Test fun wrongPasswordIsAFailureWithCode401AndNoSession() = runBlocking<Unit> {
        val session = InMemorySession()
        val transport = FakeTransport { HttpResponse(401, """{"error":"Incorrect email or password"}""") }
        val users = ApiUserRepository(client(session, transport), session)

        val result = users.login("lerato@example.com", "Wrong1234")

        val failure = result as ApiResult.Failure
        assertEquals(401, failure.code)
        assertEquals("Incorrect email or password", failure.message)
        assertNull(session.token())
    }

    @Test fun duplicateEmailComesBackAsAFieldError() = runBlocking<Unit> {
        val session = InMemorySession()
        val transport = FakeTransport {
            HttpResponse(409, """{"error":"exists","fields":{"email":"An account with this email already exists"}}""")
        }
        val users = ApiUserRepository(client(session, transport), session)

        val failure = users.register("Lerato Dlamini", "lerato@example.com", "082 123 4501", "Curls2026") as ApiResult.Failure

        assertEquals(409, failure.code)
        assertEquals("An account with this email already exists", failure.fields["email"])
        assertEquals("0821234501", JSONObject(transport.requests.single().body!!).getString("phone"))
    }

    @Test fun unreachableServerGivesCode0() = runBlocking<Unit> {
        val session = InMemorySession()
        val transport = FakeTransport { throw IOException("no route to host") }
        val users = ApiUserRepository(client(session, transport), session)

        val failure = users.login("lerato@example.com", "Curls2026") as ApiResult.Failure

        assertEquals(0, failure.code)
        assertEquals(ApiClient.NETWORK_ERROR, failure.message)
    }

    @Test fun loggedInRequestsSendTheBearerToken() = runBlocking<Unit> {
        val session = InMemorySession()
        session.save("tok-999", User(7, "Lerato Dlamini", "lerato@example.com", "0821234501"))
        val transport = FakeTransport { HttpResponse(200, "[]") }

        val result = ApiBookingRepository(client(session, transport)).getMine()

        assertEquals(emptyList<Any>(), (result as ApiResult.Success).value)
        assertEquals("Bearer tok-999", transport.requests.single().headers["Authorization"])
    }

    @Test fun anExpiredSessionIsForgottenOn401() = runBlocking<Unit> {
        val session = InMemorySession()
        session.save("old-token", User(7, "Lerato Dlamini", "lerato@example.com", "0821234501"))
        val transport = FakeTransport { HttpResponse(401, """{"error":"Session expired, please log in again"}""") }

        val failure = ApiBookingRepository(client(session, transport)).getMine() as ApiResult.Failure

        assertEquals(401, failure.code)
        assertNull(session.token())
        assertNull(session.user())
    }

    @Test fun bookingIsParsedAndSlotClashIsReportedAs409() = runBlocking<Unit> {
        val session = InMemorySession()
        session.save("tok", User(7, "Lerato Dlamini", "lerato@example.com", "0821234501"))
        var created = true
        val transport = FakeTransport {
            if (created) {
                created = false
                HttpResponse(201, """{"id":42,"userId":7,"serviceId":3,"service":"Box Braids","price":450,
                    "stylistId":1,"stylistName":"Asenathi Valashiya","date":"2026-11-10","time":"10:00 AM",
                    "deposit":225,"status":"confirmed"}""")
            } else {
                HttpResponse(409, """{"error":"That time was just taken - please choose another"}""")
            }
        }
        val bookings = ApiBookingRepository(client(session, transport))

        val ok = bookings.add(3, 1, "2026-11-10", "10:00 AM") as ApiResult.Success
        assertEquals(42L, ok.value.id)
        assertEquals(225, ok.value.deposit)
        assertEquals("Asenathi Valashiya", ok.value.stylistName)

        val clash = bookings.add(3, 1, "2026-11-10", "10:00 AM") as ApiResult.Failure
        assertEquals(409, clash.code)
        val sent = JSONObject(transport.requests.first().body!!)
        assertEquals(3, sent.getInt("serviceId"))
        assertEquals("10:00 AM", sent.getString("time"))
    }

    @Test fun cancelUsesDeleteOnTheBookingUrl() = runBlocking<Unit> {
        val session = InMemorySession()
        session.save("tok", User(7, "Lerato Dlamini", "lerato@example.com", "0821234501"))
        val transport = FakeTransport { HttpResponse(204, "") }

        val result = ApiBookingRepository(client(session, transport)).cancel(42)

        assertTrue(result is ApiResult.Success)
        assertEquals("DELETE", transport.requests.single().method)
        assertEquals("http://api.test/api/bookings/42", transport.requests.single().url)
    }

    @Test fun catalogIsParsedAndCachedAfterTheFirstLoad() = runBlocking<Unit> {
        val session = InMemorySession()
        val transport = FakeTransport {
            HttpResponse(200, """[{"id":1,"name":"Wash & Blow","price":150,"duration":"45 min"}]""")
        }
        val catalog = ApiCatalogRepository(client(session, transport))

        val first = catalog.services() as ApiResult.Success
        catalog.services()

        assertEquals("Wash & Blow", first.value.single().name)
        assertEquals(1, transport.requests.size)
    }

    @Test fun availabilityParsesNullReasonAsEmpty() = runBlocking<Unit> {
        val session = InMemorySession()
        val transport = FakeTransport {
            HttpResponse(200, """{"stylistId":1,"date":"2026-11-10","slots":[
                {"time":"9:00 AM","available":true,"reason":null},
                {"time":"10:00 AM","available":false,"reason":"booked"}]}""")
        }

        val result = ApiCatalogRepository(client(session, transport)).availability(1, "2026-11-10") as ApiResult.Success

        assertEquals("http://api.test/api/availability?stylistId=1&date=2026-11-10", transport.requests.single().url)
        assertTrue(result.value[0].available)
        assertEquals("", result.value[0].reason)
        assertEquals("booked", result.value[1].reason)
    }

    @Test fun deletingTheAccountClearsTheSession() = runBlocking<Unit> {
        val session = InMemorySession()
        session.save("tok", User(7, "Lerato Dlamini", "lerato@example.com", "0821234501"))
        val transport = FakeTransport { HttpResponse(204, "") }
        val users = ApiUserRepository(client(session, transport), session)

        val result = users.deleteAccount("Curls2026")

        assertTrue(result is ApiResult.Success)
        assertEquals("http://api.test/api/me/delete", transport.requests.single().url)
        assertEquals("Curls2026", JSONObject(transport.requests.single().body!!).getString("password"))
        assertNull(session.token())
    }

    @Test fun aWrongPasswordKeepsTheAccountAndSession() = runBlocking<Unit> {
        val session = InMemorySession()
        session.save("tok", User(7, "Lerato Dlamini", "lerato@example.com", "0821234501"))
        val transport = FakeTransport {
            HttpResponse(400, """{"error":"Password is incorrect","fields":{"password":"Password is incorrect"}}""")
        }
        val users = ApiUserRepository(client(session, transport), session)

        val failure = users.deleteAccount("wrong") as ApiResult.Failure

        assertEquals(400, failure.code)
        assertEquals("tok", session.token())
    }

    @Test fun nonJsonResponseDoesNotCrash() = runBlocking<Unit> {
        val session = InMemorySession()
        val transport = FakeTransport { HttpResponse(200, "<html>proxy login page</html>") }

        val failure = ApiCatalogRepository(client(session, transport)).stylists() as ApiResult.Failure

        assertEquals("Unexpected response from the server", failure.message)
    }
}
