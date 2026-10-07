from __future__ import annotations

import sys
import tempfile
import types
import unittest
import warnings
import zipfile
from pathlib import Path
from unittest.mock import patch

from apk_inspector import ApkInspectionError, inspect_apk

MANIFEST = b"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.example.sample" android:versionCode="12" android:versionName="2.3">
  <uses-sdk android:minSdkVersion="23" android:targetSdkVersion="35" />
  <uses-permission android:name="android.permission.INTERNET" />
  <application android:label="Sample App" android:debuggable="true"
      android:allowBackup="false" android:usesCleartextTraffic="true">
    <activity android:name=".MainActivity" />
    <service android:name=".SyncService" />
    <receiver android:name=".BootReceiver" />
    <provider android:name=".DataProvider" />
  </application>
</manifest>
"""


class ApkInspectorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.root = Path(self.temp_dir.name)

    def make_apk(self, name: str = "sample.apk", *, manifest: bytes = MANIFEST) -> Path:
        path = self.root / name
        with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            if manifest is not None:
                archive.writestr("AndroidManifest.xml", manifest)
            archive.writestr("classes.dex", b"dex\n035\x00" + b"a" * 50)
            archive.writestr("classes2.dex", b"dex\n035\x00" + b"b" * 25)
            archive.writestr("lib/arm64-v8a/libsample.so", b"ELF")
            archive.writestr("lib/armeabi-v7a/libsample.so", b"ELF")
        return path

    def test_reads_manifest_and_apk_inventory(self) -> None:
        path = self.make_apk()
        report = inspect_apk(path)

        self.assertEqual(report.package_name, "org.example.sample")
        self.assertEqual(report.app_name, "Sample App")
        self.assertEqual(report.version_name, "2.3")
        self.assertEqual(report.version_code, "12")
        self.assertEqual(report.min_sdk, "23")
        self.assertEqual(report.target_sdk, "35")
        self.assertTrue(report.debuggable)
        self.assertFalse(report.allow_backup)
        self.assertTrue(report.cleartext_traffic)
        self.assertEqual(report.permissions, ("android.permission.INTERNET",))
        self.assertIn(".MainActivity", report.activities)
        self.assertIn(".SyncService", report.services)
        self.assertIn(".BootReceiver", report.receivers)
        self.assertIn(".DataProvider", report.providers)
        self.assertEqual(
            [name for name, _ in report.dex_files], ["classes.dex", "classes2.dex"]
        )
        self.assertEqual(report.native_abis, ("arm64-v8a", "armeabi-v7a"))
        self.assertEqual(report.signature_schemes, ())
        self.assertEqual(len(report.sha256), 64)

        text = report.to_text()
        self.assertIn("APK STATIC METADATA REPORT", text)
        self.assertIn("org.example.sample", text)
        self.assertIn("not a malware verdict", text)
        self.assertIn("manifest enables debuggable mode", text)

    def test_binary_manifest_parser_and_signing_metadata(self) -> None:
        class FakeCertificate:
            sha256 = bytes.fromhex("ab" * 32)

            class subject:
                human_friendly = "CN=Sample Signer"

        class FakeAPK:
            def __init__(self, filename: str, **kwargs: object) -> None:
                self.filename = filename

            def get_package(self) -> str:
                return "org.example.binary"

            def get_app_name(self) -> str:
                return "Binary Manifest App"

            def get_androidversion_name(self) -> str:
                return "1.0"

            def get_androidversion_code(self) -> str:
                return "1"

            def get_min_sdk_version(self) -> str:
                return "21"

            def get_target_sdk_version(self) -> str:
                return "34"

            def get_attribute_value(self, tag: str, name: str) -> str | None:
                return {"debuggable": "false", "allowBackup": "true"}.get(name)

            def get_permissions(self) -> list[str]:
                return ["android.permission.INTERNET"]

            def get_activities(self) -> list[str]:
                return ["org.example.binary.MainActivity"]

            def get_services(self) -> list[str]:
                return []

            def get_receivers(self) -> list[str]:
                return []

            def get_providers(self) -> list[str]:
                return []

            def is_signed_v1(self) -> bool:
                return True

            def is_signed_v2(self) -> bool:
                return False

            def is_signed_v3(self) -> bool:
                return True

            def get_certificates(self) -> list[FakeCertificate]:
                return [FakeCertificate()]

        apk_path = self.make_apk(manifest=bytes.fromhex("03000800") + b"binary-axml")
        fake_parser = types.ModuleType("pyaxmlparser")
        fake_parser.APK = FakeAPK

        with patch.dict(sys.modules, {"pyaxmlparser": fake_parser}):
            report = inspect_apk(apk_path)

        self.assertEqual(report.package_name, "org.example.binary")
        self.assertFalse(report.debuggable)
        self.assertTrue(report.allow_backup)
        self.assertEqual(report.signature_schemes, ("v1/JAR", "v3"))
        self.assertEqual(report.certificates[0].sha256, "AB" * 32)
        self.assertEqual(report.certificates[0].subject, "CN=Sample Signer")

    def test_rejects_oversized_android_resource_table(self) -> None:
        path = self.make_apk("large-resource-table.apk")
        with zipfile.ZipFile(path, "a", compression=zipfile.ZIP_DEFLATED) as archive:
            archive.writestr("resources.arsc", b"0" * (32 * 1024 * 1024 + 1))

        with self.assertRaisesRegex(ApkInspectionError, "resource table"):
            inspect_apk(path, max_uncompressed_bytes=40 * 1024 * 1024)

    def test_rejects_archive_without_manifest(self) -> None:
        path = self.root / "missing-manifest.apk"
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("classes.dex", b"dex")

        with self.assertRaisesRegex(
            ApkInspectionError, "exactly one AndroidManifest.xml"
        ):
            inspect_apk(path)

    def test_rejects_non_zip_file(self) -> None:
        path = self.root / "not-an-apk.apk"
        path.write_bytes(b"not a zip")

        with self.assertRaisesRegex(ApkInspectionError, "not a readable APK/ZIP"):
            inspect_apk(path)

    def test_enforces_upload_size_limit(self) -> None:
        path = self.make_apk()
        with self.assertRaisesRegex(ApkInspectionError, "configured upload limit"):
            inspect_apk(path, max_file_bytes=10)

    def test_enforces_uncompressed_size_limit(self) -> None:
        path = self.make_apk()
        with self.assertRaisesRegex(ApkInspectionError, "expanded size"):
            inspect_apk(path, max_uncompressed_bytes=50)

    def test_rejects_duplicate_manifests(self) -> None:
        path = self.root / "duplicate-manifest.apk"
        with (
            warnings.catch_warnings(),
            zipfile.ZipFile(path, "w") as archive,
        ):
            warnings.simplefilter("ignore", UserWarning)
            archive.writestr("AndroidManifest.xml", MANIFEST)
            archive.writestr("AndroidManifest.xml", MANIFEST)

        with self.assertRaisesRegex(
            ApkInspectionError, "exactly one AndroidManifest.xml"
        ):
            inspect_apk(path)


if __name__ == "__main__":
    unittest.main()
