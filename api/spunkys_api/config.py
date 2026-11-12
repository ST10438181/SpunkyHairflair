"""Configuration, overridable with environment variables."""
import os
from pathlib import Path

_DEFAULT_DB = Path(__file__).resolve().parent.parent / "data" / "spunkys.db"


class Config:
    DATABASE = os.environ.get("SPUNKYS_DB", str(_DEFAULT_DB))
    PBKDF2_ITERATIONS = int(os.environ.get("SPUNKYS_PBKDF2_ITERATIONS", "600000"))
    TOKEN_TTL_DAYS = int(os.environ.get("SPUNKYS_TOKEN_TTL_DAYS", "30"))
    JSON_SORT_KEYS = False
