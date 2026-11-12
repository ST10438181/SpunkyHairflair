"""Password hashing and bearer-token helpers (standard library only)."""
import base64
import hashlib
import hmac
import secrets

ALGORITHM = "pbkdf2_sha256"


def hash_password(password: str, iterations: int) -> str:
    """Return 'pbkdf2_sha256$iterations$salt_b64$hash_b64' using a fresh random salt."""
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, iterations)
    return "$".join([
        ALGORITHM,
        str(iterations),
        base64.b64encode(salt).decode("ascii"),
        base64.b64encode(digest).decode("ascii"),
    ])


def verify_password(password: str, stored: str) -> bool:
    try:
        algorithm, iterations, salt_b64, hash_b64 = stored.split("$")
        if algorithm != ALGORITHM:
            return False
        salt = base64.b64decode(salt_b64)
        expected = base64.b64decode(hash_b64)
        actual = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, int(iterations))
    except (ValueError, TypeError):
        return False
    return hmac.compare_digest(actual, expected)


def new_token() -> str:
    return secrets.token_urlsafe(32)


def token_digest(token: str) -> str:
    """Only this SHA-256 digest is stored, never the token itself."""
    return hashlib.sha256(token.encode("utf-8")).hexdigest()
