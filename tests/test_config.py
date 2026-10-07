from __future__ import annotations

import os
import unittest
from unittest.mock import patch

from config import load_settings


class SettingsTests(unittest.TestCase):
    def test_loads_private_default_and_allowlist(self) -> None:
        env = {
            "BOT_TOKEN": "123456:dummy-token-for-tests",
            "ALLOWED_USER_IDS": "123, 456",
            "MAX_UPLOAD_MB": "18",
            "MAX_UNCOMPRESSED_MB": "100",
            "MAX_ARCHIVE_ENTRIES": "9000",
        }
        with patch.dict(os.environ, env, clear=True), patch("config.load_dotenv"):
            settings = load_settings()

        self.assertEqual(settings.allowed_user_ids, frozenset({123, 456}))
        self.assertFalse(settings.allow_all_users)
        self.assertEqual(settings.max_upload_bytes, 18 * 1024 * 1024)
        self.assertEqual(settings.max_uncompressed_bytes, 100 * 1024 * 1024)
        self.assertEqual(settings.max_archive_entries, 9000)

    def test_rejects_upload_limit_over_hosted_bot_api_limit(self) -> None:
        env = {"BOT_TOKEN": "123456:dummy-token-for-tests", "MAX_UPLOAD_MB": "21"}
        with (
            patch.dict(os.environ, env, clear=True),
            patch("config.load_dotenv"),
            self.assertRaisesRegex(ValueError, "cannot exceed 20"),
        ):
            load_settings()

    def test_rejects_malformed_allowlist(self) -> None:
        env = {
            "BOT_TOKEN": "123456:dummy-token-for-tests",
            "ALLOWED_USER_IDS": "123,nope",
        }
        with (
            patch.dict(os.environ, env, clear=True),
            patch("config.load_dotenv"),
            self.assertRaisesRegex(ValueError, "comma-separated Telegram numeric IDs"),
        ):
            load_settings()


if __name__ == "__main__":
    unittest.main()
