"""Telegram front-end for read-only Android APK metadata reports."""

from __future__ import annotations

import asyncio
import io
import logging
import re
import tempfile
import traceback
from pathlib import Path

from telegram import InputFile, Message, Update
from telegram.constants import ChatType
from telegram.error import InvalidToken, TelegramError
from telegram.ext import (
    Application,
    ApplicationBuilder,
    CommandHandler,
    ContextTypes,
    MessageHandler,
    filters,
)

from apk_inspector import ApkInspectionError, inspect_apk
from config import Settings, load_settings

LOGGER = logging.getLogger("apk_telegram_bot")
BOT_TOKEN_PATTERN = re.compile(
    r"(?<![A-Za-z0-9_-])\d{6,}:[A-Za-z0-9_-]{20,}(?![A-Za-z0-9_-])"
)
BOT_TOKEN_REDACTION = "<BOT_TOKEN_REDACTED>"


class BotTokenRedactionFilter(logging.Filter):
    """Prevent Telegram API tokens from appearing in log lines or tracebacks."""

    @staticmethod
    def _redact(text: str) -> str:
        return BOT_TOKEN_PATTERN.sub(BOT_TOKEN_REDACTION, text)

    def filter(self, record: logging.LogRecord) -> bool:
        record.msg = self._redact(record.getMessage())
        record.args = ()
        if record.exc_info:
            exception_text = "".join(traceback.format_exception(*record.exc_info))
            record.exc_text = self._redact(exception_text)
            record.exc_info = None
        elif record.exc_text:
            record.exc_text = self._redact(record.exc_text)
        if record.stack_info:
            record.stack_info = self._redact(record.stack_info)
        return True


START_MESSAGE = (
    "Hi! I can create a read-only static metadata report for an Android APK.\n\n"
    "Send me an .apk file to receive package/version details, declared permissions, "
    "DEX and native ABI lists, manifest flags, and signing certificate fingerprints.\n\n"
    "I do not modify, sign, run, or return the uploaded APK. The report is not a malware verdict.\n\n"
    "Commands: /help, /myid, /privacy"
)


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    message = update.effective_message
    if message is None:
        return

    settings: Settings = context.application.bot_data["settings"]
    if not _is_private(update):
        await message.reply_text("For privacy, please use this bot in a private chat.")
        return

    if not _is_authorized(update, settings):
        user_id = update.effective_user.id if update.effective_user else "unknown"
        await message.reply_text(
            "This bot is private by default.\n"
            f"Your Telegram user ID is: {user_id}\n\n"
            "Add that number to ALLOWED_USER_IDS in your .env file and restart the bot, "
            "or explicitly set ALLOW_ALL_USERS=true if you want a public bot."
        )
        return

    await message.reply_text(START_MESSAGE)


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    await start(update, context)


async def my_id(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    message = update.effective_message
    if message is None:
        return
    if not _is_private(update):
        await message.reply_text(
            "For privacy, use /myid in a private chat with this bot."
        )
        return
    user = update.effective_user
    if user is None:
        await message.reply_text("Telegram user ID is unavailable for this message.")
        return
    await message.reply_text(
        f"Your Telegram user ID is {user.id}.\n"
        "To restrict the bot to you, set ALLOWED_USER_IDS to this number in .env."
    )


async def privacy(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    message = update.effective_message
    if message is None:
        return
    await message.reply_text(
        "Privacy: the bot downloads an APK only to a temporary local directory to read its metadata. "
        "That temporary directory is removed after processing; the text report is held in memory only "
        "while it is sent. There is no database or permanent file store. Telegram itself handles delivery of the file "
        "between you and the bot. Do not send APKs you are not allowed to share."
    )


async def analyze_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    message = update.effective_message
    if message is None:
        return
    settings: Settings = context.application.bot_data["settings"]
    if not _is_private(update):
        await message.reply_text("For privacy, please use this bot in a private chat.")
        return
    if not _is_authorized(update, settings):
        await _not_authorized(message, update)
        return
    await message.reply_text(
        "Send the APK as a document (.apk). I will return a metadata report only."
    )


async def handle_document(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    message = update.effective_message
    document = message.document if message else None
    if message is None or document is None:
        return

    settings: Settings = context.application.bot_data["settings"]
    if not _is_private(update):
        await message.reply_text(
            "For privacy, please send APK files in a private chat with this bot."
        )
        return
    if not _is_authorized(update, settings):
        await _not_authorized(message, update)
        return

    name = document.file_name or ""
    if not name.lower().endswith(".apk"):
        await message.reply_text("Please send an Android .apk file as a document.")
        return

    advertised_size = document.file_size
    if advertised_size is not None and advertised_size > settings.max_upload_bytes:
        await message.reply_text(
            "That APK is over the configured limit of "
            f"{settings.max_upload_bytes // (1024 * 1024)} MB."
        )
        return

    status = await message.reply_text("Checking APK structure and reading metadata…")
    semaphore: asyncio.Semaphore = context.application.bot_data["analysis_semaphore"]
    try:
        async with semaphore:
            report_text, file_name = await _analyze_document(
                document.file_id, settings, context
            )
            document_bytes = io.BytesIO(report_text.encode("utf-8"))
            await message.reply_document(
                document=InputFile(document_bytes, filename=file_name),
                caption="Static report ready. The APK was not modified or executed.",
            )
    except ApkInspectionError as exc:
        await message.reply_text(str(exc))
    except TelegramError:
        LOGGER.warning("Telegram file transfer failed")
        await message.reply_text(
            "Telegram could not download or return the file. Check the size limit and try again."
        )
    except Exception:
        LOGGER.exception("Unexpected APK report error")
        await message.reply_text(
            "The APK could not be inspected. No output file was kept."
        )
    finally:
        try:
            await status.delete()
        except TelegramError:
            pass


async def _analyze_document(
    file_id: str,
    settings: Settings,
    context: ContextTypes.DEFAULT_TYPE,
) -> tuple[str, str]:
    settings.temp_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    with tempfile.TemporaryDirectory(
        prefix="apk-report-", dir=settings.temp_dir
    ) as work_dir:
        apk_path = Path(work_dir) / "upload.apk"
        telegram_file = await context.bot.get_file(file_id)
        await telegram_file.download_to_drive(custom_path=apk_path)

        actual_size = apk_path.stat().st_size
        if actual_size > settings.max_upload_bytes:
            raise ApkInspectionError(
                "The downloaded APK exceeds the configured upload limit "
                f"({settings.max_upload_bytes // (1024 * 1024)} MB)."
            )

        report = await asyncio.to_thread(
            inspect_apk,
            apk_path,
            max_file_bytes=settings.max_upload_bytes,
            max_uncompressed_bytes=settings.max_uncompressed_bytes,
            max_archive_entries=settings.max_archive_entries,
        )
        return report.to_text(), f"apk-report-{report.sha256[:12].lower()}.txt"


async def handle_text(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    message = update.effective_message
    if message is None or not message.text:
        return
    settings: Settings = context.application.bot_data["settings"]
    if not _is_private(update):
        await message.reply_text("For privacy, please use this bot in a private chat.")
        return
    if not _is_authorized(update, settings):
        await _not_authorized(message, update)
        return
    await message.reply_text(
        "Send an .apk file as a document, or use /help for instructions."
    )


async def error_handler(update: object, context: ContextTypes.DEFAULT_TYPE) -> None:
    LOGGER.error("Unhandled Telegram update error", exc_info=context.error)
    if isinstance(update, Update) and update.effective_message:
        try:
            await update.effective_message.reply_text(
                "Something went wrong. Please try again later."
            )
        except TelegramError:
            pass


def _is_private(update: Update) -> bool:
    chat = update.effective_chat
    return bool(chat and chat.type == ChatType.PRIVATE)


def _is_authorized(update: Update, settings: Settings) -> bool:
    if settings.allow_all_users:
        return True
    user = update.effective_user
    return bool(user and user.id in settings.allowed_user_ids)


async def _not_authorized(message: Message, update: Update) -> None:
    user = update.effective_user
    user_id = user.id if user else "unknown"
    await message.reply_text(
        "This bot is private by default. "
        f"Your Telegram user ID is {user_id}; ask the bot owner to add it to ALLOWED_USER_IDS."
    )


def build_application(settings: Settings) -> Application:
    application = (
        ApplicationBuilder()
        .token(settings.bot_token)
        .concurrent_updates(4)
        .read_timeout(60)
        .write_timeout(60)
        .connect_timeout(20)
        .build()
    )
    application.bot_data["settings"] = settings
    # APK parsing is CPU/memory intensive; queue analysis jobs instead of parsing
    # several user-provided archives at once.
    application.bot_data["analysis_semaphore"] = asyncio.Semaphore(1)

    application.add_handler(CommandHandler("start", start))
    application.add_handler(CommandHandler("help", help_command))
    application.add_handler(CommandHandler("myid", my_id))
    application.add_handler(CommandHandler("privacy", privacy))
    application.add_handler(CommandHandler("analyze", analyze_command))
    application.add_handler(MessageHandler(filters.Document.ALL, handle_document))
    application.add_handler(
        MessageHandler(filters.TEXT & ~filters.COMMAND, handle_text)
    )
    application.add_error_handler(error_handler)
    return application


def configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )
    token_filter = BotTokenRedactionFilter()
    for handler in logging.getLogger().handlers:
        handler.addFilter(token_filter)
    # httpx logs request URLs at INFO, and Telegram embeds the bot token in them.
    logging.getLogger("httpx").setLevel(logging.WARNING)


def main() -> None:
    configure_logging()
    try:
        settings = load_settings()
    except ValueError as exc:
        raise SystemExit(str(exc)) from exc

    settings.temp_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    LOGGER.info(
        "Starting APK report bot (allow_all_users=%s, allowlisted_users=%d)",
        settings.allow_all_users,
        len(settings.allowed_user_ids),
    )
    application = build_application(settings)
    try:
        application.run_polling(drop_pending_updates=True)
    except InvalidToken:
        LOGGER.error(
            "Telegram rejected BOT_TOKEN. Replace it with a fresh token from @BotFather."
        )
        raise SystemExit(2) from None


if __name__ == "__main__":
    main()
