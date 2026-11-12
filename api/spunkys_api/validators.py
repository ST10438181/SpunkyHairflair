"""Server-side input validation. Mirrors the rules used by the Android app (Validators.kt).

Every function returns an error message, or None when the value is valid.
"""
import re

_EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
_NAME = re.compile(r"[A-Za-z][A-Za-z '\-]{1,49}")
_PHONE = re.compile(r"(0|\+27)[0-9]{9}")


def name_error(value):
    v = (value or "").strip()
    if len(v) < 2:
        return "Enter your full name"
    if not _NAME.fullmatch(v):
        return "Name can only contain letters, spaces, - and '"
    return None


def email_error(value):
    v = (value or "").strip()
    if not v:
        return "Enter your email address"
    if not _EMAIL.fullmatch(v):
        return "Enter a valid email address"
    return None


def phone_error(value):
    v = (value or "").replace(" ", "")
    if not v:
        return "Enter your phone number"
    if not _PHONE.fullmatch(v):
        return "Enter a valid SA number, e.g. 0812345678"
    return None


def password_error(value):
    v = value or ""
    if len(v) < 8:
        return "Password must be at least 8 characters"
    if not any(c.isalpha() for c in v) or not any(c.isdigit() for c in v):
        return "Password needs at least one letter and one number"
    return None
