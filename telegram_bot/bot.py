import asyncio, hashlib, logging, os, tempfile, zipfile
from pathlib import Path
from telegram import Update
from telegram.ext import Application, CommandHandler, MessageHandler, ContextTypes, filters

logging.basicConfig(level=logging.INFO)
TOKEN = os.environ.get('BOT_TOKEN')
MAX_MB = int(os.environ.get('MAX_APK_MB', '50'))

HELP = '''Send me an APK and I will inspect it safely (size, SHA-256, package files, permissions and certificate files).\n\nCommands:\n/start - start the bot\n/help - show help\n/health - check bot status\n\nNo APK is executed or installed on the server.'''

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text('👋 Zayro APK Inspector ready.\n\n' + HELP)

async def help_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(HELP)

async def health(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text('✅ Bot is online and APK execution is disabled.')

def inspect_apk(path: Path):
    size = path.stat().st_size
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        dex = sorted(n for n in names if n.endswith('.dex'))
        native = sorted(n for n in names if n.startswith('lib/') and n.endswith(('.so', '.so.gz')))
        perms = [n for n in names if n.startswith('META-INF/') and n.endswith(('.RSA', '.DSA', '.EC'))]
        manifest = 'AndroidManifest.xml' in names
        return size, digest, names, dex, native, perms, manifest

async def apk_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    doc = update.message.document
    if not doc or not (doc.file_name or '').lower().endswith('.apk'):
        await update.message.reply_text('Please send an .apk file as a document.')
        return
    if doc.file_size and doc.file_size > MAX_MB * 1024 * 1024:
        await update.message.reply_text(f'File is too large. Maximum allowed size is {MAX_MB} MB.')
        return
    msg = await update.message.reply_text('⏳ Downloading and inspecting…')
    try:
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / 'upload.apk'
            tg_file = await doc.get_file()
            await tg_file.download_to_drive(path)
            size, digest, names, dex, native, certs, manifest = inspect_apk(path)
        report = (f'✅ APK inspection complete\n\n'
                  f'File: {doc.file_name}\nSize: {size / 1024 / 1024:.2f} MB\n'
                  f'SHA-256: {digest}\n\n'
                  f'AndroidManifest.xml: {"yes" if manifest else "no"}\n'
                  f'DEX files: {len(dex)} ({", ".join(dex[:5]) or "none"})\n'
                  f'Native libraries: {len(native)}\n'
                  f'Signing entries: {len(certs)}\nZIP entries: {len(names)}\n\n'
                  'The APK was never executed or installed.')
        await msg.edit_text(report)
    except zipfile.BadZipFile:
        await msg.edit_text('❌ This file is not a valid APK/ZIP archive.')
    except Exception:
        logging.exception('inspection failed')
        await msg.edit_text('❌ Could not inspect this file.')

def main():
    if not TOKEN:
        raise SystemExit('BOT_TOKEN is required. Copy .env.example to .env and set it.')
    app = Application.builder().token(TOKEN).build()
    app.add_handler(CommandHandler('start', start))
    app.add_handler(CommandHandler('help', help_cmd))
    app.add_handler(CommandHandler('health', health))
    app.add_handler(MessageHandler(filters.Document.ALL, apk_handler))
    app.run_polling(allowed_updates=Update.ALL_TYPES)

if __name__ == '__main__':
    main()
