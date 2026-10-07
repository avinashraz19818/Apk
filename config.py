"""Environment-based bot configuration."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


@dataclass(frozen=True)
class Settings:
    bot_token: str
    allowed_user_ids: frozenset[int]
    allow_all_users: bool
    max_upload_bytes: int
    max_uncompressed_bytes: int
    max_archive_entries: int
    temp_dir: Path


def _parse_bool(value: str | None, *, default: bool = False) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


def _parse_id_list(value: str | None) -> frozenset[int]:
    if not value or not value.strip():
        return frozenset()
    ids: set[int] = set()
    for part in value.split(","):
        part = part.strip()
        if not part:
            continue
        try:
            user_id = int(part)
        except ValueError as exc:
            raise ValueError(
                "ALLOWED_USER_IDS must be comma-separated Telegram numeric IDs."
            ) from exc
        if user_id <= 0:
            raise ValueError("Telegram user IDs must be positive integers.")
        ids.add(user_id)
    return frozenset(ids)


def load_settings() -> Settings:
    """Load and validate configuration. Never log the bot token."""
    load_dotenv()
    token = os.getenv("BOT_TOKEN", "").strip()
    if not token:
        raise ValueError(
            "BOT_TOKEN is missing. Copy .env.example to .env and set your BotFather token."
        )

    max_upload_mb = _positive_int("MAX_UPLOAD_MB", 19)
    # Telegram's hosted Bot API currently limits downloads through getFile to 20 MB.
    if max_upload_mb > 20:
        raise ValueError(
            "MAX_UPLOAD_MB cannot exceed 20 when using Telegram's hosted Bot API."
        )
    max_uncompressed_mb = _positive_int("MAX_UNCOMPRESSED_MB", 256)
    max_archive_entries = _positive_int("MAX_ARCHIVE_ENTRIES", 50000)

    return Settings(
        bot_token=token,
        allowed_user_ids=_parse_id_list(os.getenv("ALLOWED_USER_IDS")),
        allow_all_users=_parse_bool(os.getenv("ALLOW_ALL_USERS"), default=False),
        max_upload_bytes=max_upload_mb * 1024 * 1024,
        max_uncompressed_bytes=max_uncompressed_mb * 1024 * 1024,
        max_archive_entries=max_archive_entries,
        temp_dir=Path(os.getenv("TEMP_DIR", "/tmp/apk-telegram-bot")).expanduser(),
    )


def _positive_int(name: str, default: int) -> int:
    raw = os.getenv(name, str(default)).strip()
    try:
        value = int(raw)
    except ValueError as exc:
        raise ValueError(f"{name} must be a positive integer.") from exc
    if value <= 0:
        raise ValueError(f"{name} must be a positive integer.")
    return value
