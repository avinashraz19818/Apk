from __future__ import annotations

import asyncio
import logging
import sys
import unittest
from pathlib import Path

from bot import BotTokenRedactionFilter, build_application
from config import Settings


class BotApplicationTests(unittest.TestCase):
    def test_application_builds_without_starting_network_polling(self) -> None:
        settings = Settings(
            bot_token="123456:dummy-token-for-tests",
            allowed_user_ids=frozenset({123}),
            allow_all_users=False,
            max_upload_bytes=19 * 1024 * 1024,
            max_uncompressed_bytes=256 * 1024 * 1024,
            max_archive_entries=50_000,
            temp_dir=Path("/tmp/apk-bot-test"),
        )

        application = build_application(settings)
        self.assertEqual(sum(len(group) for group in application.handlers.values()), 7)
        self.assertIs(application.bot_data["settings"], settings)
        self.assertIsInstance(
            application.bot_data["analysis_semaphore"], asyncio.Semaphore
        )

    def test_token_filter_redacts_messages_and_tracebacks(self) -> None:
        token = "123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijk"
        try:
            raise RuntimeError(f"Telegram rejected {token}")
        except RuntimeError:
            record = logging.LogRecord(
                name="test",
                level=logging.ERROR,
                pathname=__file__,
                lineno=1,
                msg="Request used token %s",
                args=(token,),
                exc_info=sys.exc_info(),
            )

        BotTokenRedactionFilter().filter(record)
        rendered = logging.Formatter("%(message)s").format(record)
        self.assertNotIn(token, rendered)
        self.assertGreaterEqual(rendered.count("<BOT_TOKEN_REDACTED>"), 2)


if __name__ == "__main__":
    unittest.main()
