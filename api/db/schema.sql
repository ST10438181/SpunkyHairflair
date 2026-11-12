-- Spunky's Hairflair booking system - relational schema (SQLite dialect, standard SQL types).
-- Applied by `python -m spunkys_api.init_db`. All money values are whole South African rand.

PRAGMA foreign_keys = ON;

-- Customers. Passwords are never stored in plain text: password_hash holds
-- "pbkdf2_sha256$<iterations>$<salt_b64>$<hash_b64>".
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL CHECK (length(name) BETWEEN 2 AND 50),
    email         TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    phone         TEXT    NOT NULL,
    password_hash TEXT    NOT NULL,
    created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- Salon services (catalogue shown on the Services and Book screens).
CREATE TABLE IF NOT EXISTS services (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT    NOT NULL UNIQUE,
    price            INTEGER NOT NULL CHECK (price > 0),
    duration_label   TEXT    NOT NULL,
    active           INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

-- Stylists / barbers.
CREATE TABLE IF NOT EXISTS stylists (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT    NOT NULL UNIQUE,
    specialty TEXT    NOT NULL,
    rating    REAL    NOT NULL CHECK (rating BETWEEN 0 AND 5),
    reviews   INTEGER NOT NULL DEFAULT 0 CHECK (reviews >= 0)
);

-- Appointments. price and deposit are copied from the service at booking time so
-- later price changes do not rewrite history. Cancelling is a soft delete
-- (status = 'cancelled') so the record is kept.
CREATE TABLE IF NOT EXISTS bookings (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    service_id   INTEGER NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    stylist_id   INTEGER NOT NULL REFERENCES stylists(id) ON DELETE RESTRICT,
    booking_date TEXT    NOT NULL CHECK (booking_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    time_slot    TEXT    NOT NULL,
    price        INTEGER NOT NULL CHECK (price > 0),
    deposit      INTEGER NOT NULL CHECK (deposit > 0 AND deposit <= price),
    status       TEXT    NOT NULL DEFAULT 'confirmed' CHECK (status IN ('confirmed', 'cancelled')),
    created_at   TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- Double-booking protection enforced by the database itself: a stylist can have
-- only one active booking per date + time slot. Cancelled rows free the slot.
CREATE UNIQUE INDEX IF NOT EXISTS ux_bookings_active_slot
    ON bookings (stylist_id, booking_date, time_slot)
    WHERE status <> 'cancelled';

CREATE INDEX IF NOT EXISTS ix_bookings_user ON bookings (user_id, booking_date);

-- Login sessions. Only the SHA-256 of the bearer token is stored.
CREATE TABLE IF NOT EXISTS auth_tokens (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT    NOT NULL UNIQUE,
    created_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    expires_at TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_tokens_user ON auth_tokens (user_id);
