"""All REST endpoints. See docs/API.md for the full contract."""
import sqlite3
from datetime import date, datetime, timedelta, timezone
from functools import wraps

from flask import Blueprint, current_app, g, jsonify, request

from . import validators
from .booking_rules import TIME_SLOTS, deposit, is_open_day, is_past, now_sast
from .db import get_db
from .security import hash_password, new_token, token_digest, verify_password

api = Blueprint("api", __name__)


# ----------------------------------------------------------------------------- helpers
def error(message, status, fields=None):
    body = {"error": message}
    if fields:
        body["fields"] = fields
    return jsonify(body), status


def json_body():
    data = request.get_json(silent=True)
    return data if isinstance(data, dict) else {}


def user_json(row):
    return {"id": row["id"], "name": row["name"], "email": row["email"], "phone": row["phone"]}


def booking_json(row):
    return {
        "id": row["id"],
        "userId": row["user_id"],
        "serviceId": row["service_id"],
        "service": row["service_name"],
        "price": row["price"],
        "stylistId": row["stylist_id"],
        "stylistName": row["stylist_name"],
        "date": row["booking_date"],
        "time": row["time_slot"],
        "deposit": row["deposit"],
        "status": row["status"],
    }


_BOOKING_SELECT = (
    "SELECT b.*, s.name AS service_name, st.name AS stylist_name "
    "FROM bookings b "
    "JOIN services s ON s.id = b.service_id "
    "JOIN stylists st ON st.id = b.stylist_id "
)


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _iso(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def issue_token(user_id: int) -> str:
    token = new_token()
    expires = _utc_now() + timedelta(days=current_app.config["TOKEN_TTL_DAYS"])
    db = get_db()
    db.execute(
        "INSERT INTO auth_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
        (user_id, token_digest(token), _iso(expires)),
    )
    db.commit()
    return token


def login_required(view):
    @wraps(view)
    def wrapper(*args, **kwargs):
        header = request.headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            return error("Please log in", 401)
        digest = token_digest(header[len("Bearer "):].strip())
        row = get_db().execute(
            "SELECT u.* FROM auth_tokens t JOIN users u ON u.id = t.user_id "
            "WHERE t.token_hash = ? AND t.expires_at > ?",
            (digest, _iso(_utc_now())),
        ).fetchone()
        if row is None:
            return error("Session expired, please log in again", 401)
        g.user = row
        g.token_digest = digest
        return view(*args, **kwargs)
    return wrapper


def _parse_date(text):
    try:
        return date.fromisoformat(text)
    except (TypeError, ValueError):
        return None


def _now():
    # Tests can inject a fixed clock through app.config["NOW"].
    fixed = current_app.config.get("NOW")
    return fixed() if fixed else now_sast()


# ----------------------------------------------------------------------------- public
@api.get("/health")
def health():
    get_db().execute("SELECT 1")
    return jsonify(status="ok")


@api.get("/services")
def list_services():
    rows = get_db().execute(
        "SELECT id, name, price, duration_label FROM services WHERE active = 1 ORDER BY id"
    ).fetchall()
    return jsonify([
        {"id": r["id"], "name": r["name"], "price": r["price"], "duration": r["duration_label"]}
        for r in rows
    ])


@api.get("/stylists")
def list_stylists():
    rows = get_db().execute(
        "SELECT id, name, specialty, rating, reviews FROM stylists ORDER BY id"
    ).fetchall()
    return jsonify([dict(r) for r in rows])


@api.get("/availability")
def availability():
    stylist_id = request.args.get("stylistId", type=int)
    day = _parse_date(request.args.get("date"))
    if stylist_id is None or day is None:
        return error("stylistId and date (yyyy-MM-dd) are required", 400)
    if not is_open_day(day):
        return error("The salon is closed on Sundays", 400)
    db = get_db()
    if db.execute("SELECT 1 FROM stylists WHERE id = ?", (stylist_id,)).fetchone() is None:
        return error("Stylist not found", 404)

    taken = {
        r["time_slot"] for r in db.execute(
            "SELECT time_slot FROM bookings WHERE stylist_id = ? AND booking_date = ? "
            "AND status <> 'cancelled'", (stylist_id, day.isoformat())
        )
    }
    now = _now()
    slots = []
    for slot in TIME_SLOTS:
        if is_past(day, slot, now):
            slots.append({"time": slot, "available": False, "reason": "past"})
        elif slot in taken:
            slots.append({"time": slot, "available": False, "reason": "booked"})
        else:
            slots.append({"time": slot, "available": True, "reason": None})
    return jsonify(stylistId=stylist_id, date=day.isoformat(), slots=slots)


# ----------------------------------------------------------------------------- auth
@api.post("/auth/register")
def register():
    data = json_body()
    name = str(data.get("name", ""))
    email = str(data.get("email", ""))
    phone = str(data.get("phone", ""))
    password = str(data.get("password", ""))

    fields = {k: v for k, v in {
        "name": validators.name_error(name),
        "email": validators.email_error(email),
        "phone": validators.phone_error(phone),
        "password": validators.password_error(password),
    }.items() if v}
    if fields:
        return error("Please correct the highlighted fields", 400, fields)

    email = email.strip().lower()
    db = get_db()
    if db.execute("SELECT 1 FROM users WHERE email = ?", (email,)).fetchone():
        return error("An account with this email already exists", 409, {"email": "An account with this email already exists"})

    cur = db.execute(
        "INSERT INTO users (name, email, phone, password_hash) VALUES (?, ?, ?, ?)",
        (name.strip(), email, phone.replace(" ", ""),
         hash_password(password, current_app.config["PBKDF2_ITERATIONS"])),
    )
    db.commit()
    user = db.execute("SELECT * FROM users WHERE id = ?", (cur.lastrowid,)).fetchone()
    return jsonify(token=issue_token(user["id"]), user=user_json(user)), 201


@api.post("/auth/login")
def login():
    data = json_body()
    email = str(data.get("email", "")).strip().lower()
    password = str(data.get("password", ""))
    user = get_db().execute("SELECT * FROM users WHERE email = ?", (email,)).fetchone()
    # Same message for unknown email and wrong password so accounts cannot be probed.
    if user is None or not verify_password(password, user["password_hash"]):
        return error("Incorrect email or password", 401)
    return jsonify(token=issue_token(user["id"]), user=user_json(user))


@api.post("/auth/logout")
@login_required
def logout():
    db = get_db()
    db.execute("DELETE FROM auth_tokens WHERE token_hash = ?", (g.token_digest,))
    db.commit()
    return "", 204


# ----------------------------------------------------------------------------- profile
@api.get("/me")
@login_required
def me():
    return jsonify(user_json(g.user))


@api.put("/me")
@login_required
def update_me():
    data = json_body()
    name = str(data.get("name", ""))
    phone = str(data.get("phone", ""))
    fields = {k: v for k, v in {
        "name": validators.name_error(name),
        "phone": validators.phone_error(phone),
    }.items() if v}
    if fields:
        return error("Please correct the highlighted fields", 400, fields)
    db = get_db()
    db.execute("UPDATE users SET name = ?, phone = ? WHERE id = ?",
               (name.strip(), phone.replace(" ", ""), g.user["id"]))
    db.commit()
    return jsonify(user_json(db.execute("SELECT * FROM users WHERE id = ?", (g.user["id"],)).fetchone()))


@api.put("/me/password")
@login_required
def change_password():
    data = json_body()
    current = str(data.get("currentPassword", ""))
    new = str(data.get("newPassword", ""))
    if not verify_password(current, g.user["password_hash"]):
        return error("Current password is incorrect", 400, {"currentPassword": "Current password is incorrect"})
    problem = validators.password_error(new)
    if problem:
        return error(problem, 400, {"newPassword": problem})
    db = get_db()
    db.execute("UPDATE users SET password_hash = ? WHERE id = ?",
               (hash_password(new, current_app.config["PBKDF2_ITERATIONS"]), g.user["id"]))
    # Sign out every other device; keep the session that made the change.
    db.execute("DELETE FROM auth_tokens WHERE user_id = ? AND token_hash <> ?",
               (g.user["id"], g.token_digest))
    db.commit()
    return "", 204


# ----------------------------------------------------------------------------- bookings
@api.get("/bookings")
@login_required
def list_bookings():
    rows = get_db().execute(
        _BOOKING_SELECT + "WHERE b.user_id = ? AND b.status <> 'cancelled'", (g.user["id"],)
    ).fetchall()
    rows = sorted(rows, key=lambda r: (r["booking_date"], TIME_SLOTS.index(r["time_slot"])))
    return jsonify([booking_json(r) for r in rows])


@api.post("/bookings")
@login_required
def create_booking():
    data = json_body()
    service_id = data.get("serviceId")
    stylist_id = data.get("stylistId")
    day = _parse_date(data.get("date"))
    slot = data.get("time")

    fields = {}
    if not isinstance(service_id, int) or isinstance(service_id, bool):
        fields["serviceId"] = "Choose a service"
    if not isinstance(stylist_id, int) or isinstance(stylist_id, bool):
        fields["stylistId"] = "Choose a stylist"
    if day is None:
        fields["date"] = "Choose a valid date"
    elif not is_open_day(day):
        fields["date"] = "The salon is closed on Sundays"
    if slot not in TIME_SLOTS:
        fields["time"] = "Choose a valid time"
    if not fields and is_past(day, slot, _now()):
        fields["time"] = "That time has already passed"
    if fields:
        return error("Please correct the booking details", 400, fields)

    db = get_db()
    service = db.execute("SELECT * FROM services WHERE id = ? AND active = 1", (service_id,)).fetchone()
    if service is None:
        return error("Service not found", 400, {"serviceId": "Service not found"})
    if db.execute("SELECT 1 FROM stylists WHERE id = ?", (stylist_id,)).fetchone() is None:
        return error("Stylist not found", 400, {"stylistId": "Stylist not found"})

    try:
        cur = db.execute(
            "INSERT INTO bookings (user_id, service_id, stylist_id, booking_date, time_slot,"
            " price, deposit) VALUES (?, ?, ?, ?, ?, ?, ?)",
            (g.user["id"], service_id, stylist_id, day.isoformat(), slot,
             service["price"], deposit(service["price"])),
        )
        db.commit()
    except sqlite3.IntegrityError:
        # The partial unique index ux_bookings_active_slot rejected a double booking.
        return error("That time was just taken - please choose another", 409)
    row = db.execute(_BOOKING_SELECT + "WHERE b.id = ?", (cur.lastrowid,)).fetchone()
    return jsonify(booking_json(row)), 201


@api.delete("/bookings/<int:booking_id>")
@login_required
def cancel_booking(booking_id):
    db = get_db()
    cur = db.execute(
        "UPDATE bookings SET status = 'cancelled' "
        "WHERE id = ? AND user_id = ? AND status <> 'cancelled'",
        (booking_id, g.user["id"]),
    )
    db.commit()
    if cur.rowcount == 0:
        return error("Booking not found", 404)
    return "", 204
