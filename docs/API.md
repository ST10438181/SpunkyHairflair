# Spunky's Hairflair REST API

JSON over HTTP. Base URL: `http://<host>:8080/api` (the Android app reads it from `BuildConfig.API_BASE_URL`).
Source: [`api/spunkys_api/routes.py`](../api/spunkys_api/routes.py) · Tests: [`api/tests/test_api.py`](../api/tests/test_api.py)

**Authentication:** `POST /auth/login` or `/auth/register` returns a `token`. Send it on protected calls as
`Authorization: Bearer <token>`. Tokens last 30 days, are stored hashed (SHA-256) in `auth_tokens`, and are
revoked on logout or password change (other devices).

**Errors** always look like `{"error": "message", "fields": {"email": "message"}}` (`fields` only for validation errors).

| Status | Meaning |
|---|---|
| 400 | Validation failed (see `fields`) |
| 401 | Missing/expired token, or wrong email/password on login |
| 404 | Not found (or not your booking) |
| 409 | Email already registered, or the time slot was just taken |

## Public endpoints

| Method & path | Description | Success |
|---|---|---|
| `GET /health` | Liveness + database check | `200 {"status":"ok"}` |
| `GET /services` | Active services | `200 [{"id":1,"name":"Wash & Blow","price":150,"duration":"45 min"}]` |
| `GET /stylists` | Stylists | `200 [{"id":4,"name":"Luna Summerton","specialty":"Barber","rating":4.8,"reviews":22}]` |
| `GET /availability?stylistId=1&date=2026-11-10` | Slots for a stylist on a day. `reason` is `"past"`, `"booked"` or `null`. `400` on Sundays. | `200 {"stylistId":1,"date":"2026-11-10","slots":[{"time":"9:00 AM","available":true,"reason":null}, ...]}` |

## Authentication

| Method & path | Body | Success |
|---|---|---|
| `POST /auth/register` | `{"name","email","phone","password"}` | `201 {"token","user":{id,name,email,phone}}` |
| `POST /auth/login` | `{"email","password"}` | `200 {"token","user"}` |
| `POST /auth/logout` 🔒 | – | `204` |

Validation (same rules as the app): name 2–50 letters/space/-/'; valid email; SA phone `0xxxxxxxxx` or `+27xxxxxxxxx`;
password ≥ 8 chars with a letter and a number. Passwords are hashed with salted PBKDF2-HMAC-SHA256 (600 000 iterations).

## Profile (🔒 = needs token)

| Method & path | Body | Success |
|---|---|---|
| `GET /me` 🔒 | – | `200 {user}` |
| `PUT /me` 🔒 | `{"name","phone"}` | `200 {user}` |
| `PUT /me/password` 🔒 | `{"currentPassword","newPassword"}` | `204` (`400` if the current password is wrong) |
| `POST /me/delete` 🔒 | `{"password"}` | `204` – permanently deletes the account, its bookings and sessions (`400` if the password is wrong) |

## Bookings (🔒)

| Method & path | Body | Success |
|---|---|---|
| `GET /bookings` 🔒 | – | `200 [booking, ...]` – your active bookings, sorted by date then time |
| `POST /bookings` 🔒 | `{"serviceId":3,"stylistId":1,"date":"2026-11-10","time":"10:00 AM"}` | `201 booking` |
| `DELETE /bookings/{id}` 🔒 | – | `204` (soft cancel: status becomes `cancelled`, slot is freed) |

A booking:

```json
{"id":42,"userId":7,"serviceId":3,"service":"Box Braids","price":450,"stylistId":1,
 "stylistName":"Asenathi Valashiya","date":"2026-11-10","time":"10:00 AM","deposit":225,"status":"confirmed"}
```

Rules enforced by the server: Monday–Saturday only; time must be one of `9:00 AM … 4:00 PM`; slot must not be in the
past (South African time); deposit = 50 % of price rounded up; one active booking per stylist/date/time (a database
unique index guarantees this even under concurrent requests → `409`).

## Try it

```bash
curl http://localhost:8080/api/services

curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"email":"lerato.dlamini@example.com","password":"Curls2026"}'

curl http://localhost:8080/api/bookings -H "Authorization: Bearer <token from login>"
```
