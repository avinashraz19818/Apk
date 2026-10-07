"""Safe, read-only APK metadata inspection.

The inspector never runs code from an APK and never extracts archive paths to disk.
"""

from __future__ import annotations

import hashlib
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
DEX_RE = re.compile(r"^classes(?:(\d+))?\.dex$", re.IGNORECASE)


class ApkInspectionError(ValueError):
    """A user-safe error raised when an uploaded file is not a supported APK."""


@dataclass(frozen=True)
class CertificateInfo:
    sha256: str
    subject: str


@dataclass(frozen=True)
class ApkReport:
    sha256: str
    file_size: int
    package_name: str
    app_name: str
    version_name: str
    version_code: str
    min_sdk: str
    target_sdk: str
    debuggable: bool | None
    allow_backup: bool | None
    cleartext_traffic: bool | None
    permissions: tuple[str, ...]
    activities: tuple[str, ...]
    services: tuple[str, ...]
    receivers: tuple[str, ...]
    providers: tuple[str, ...]
    dex_files: tuple[tuple[str, int], ...]
    native_abis: tuple[str, ...]
    zip_entries: int
    uncompressed_size: int
    signature_schemes: tuple[str, ...]
    certificates: tuple[CertificateInfo, ...]
    notes: tuple[str, ...]

    def to_text(self) -> str:
        """Render a plain-text report suitable for a Telegram document."""
        lines = [
            "APK STATIC METADATA REPORT",
            "=" * 56,
            "This is a read-only metadata report, not a malware verdict.",
            "",
            "FILE",
            f"  Size: {_format_bytes(self.file_size)}",
            f"  SHA-256: {self.sha256}",
            "",
            "APPLICATION",
            f"  Package: {_line(self.package_name)}",
            f"  Name: {_line(self.app_name)}",
            f"  Version name: {_line(self.version_name)}",
            f"  Version code: {_line(self.version_code)}",
            f"  Minimum SDK: {_line(self.min_sdk)}",
            f"  Target SDK: {_line(self.target_sdk)}",
            "",
            "MANIFEST FLAGS",
            f"  Debuggable: {_format_bool(self.debuggable)}",
            f"  Allow backup: {_format_bool(self.allow_backup)}",
            f"  Cleartext traffic allowed: {_format_bool(self.cleartext_traffic)}",
            "",
            f"DEX FILES ({len(self.dex_files)})",
        ]
        if self.dex_files:
            lines.extend(
                f"  - {name} ({_format_bytes(size)})" for name, size in self.dex_files
            )
        else:
            lines.append("  - None found")

        _append_list_section(lines, "NATIVE ABIs", self.native_abis, "None found")
        _append_list_section(
            lines, "DECLARED PERMISSIONS", self.permissions, "None declared"
        )

        for title, values in (
            ("ACTIVITIES", self.activities),
            ("SERVICES", self.services),
            ("RECEIVERS", self.receivers),
            ("CONTENT PROVIDERS", self.providers),
        ):
            _append_list_section(lines, title, values, "None found")

        lines.extend(["", "SIGNING INFORMATION"])
        if self.signature_schemes:
            lines.append(
                "  Signature scheme entries detected: "
                + ", ".join(self.signature_schemes)
            )
            lines.append(
                "  Note: scheme presence is not a full cryptographic signature verification."
            )
        else:
            lines.append("  No supported signature scheme detected.")
        if self.certificates:
            for index, certificate in enumerate(self.certificates, start=1):
                lines.append(
                    f"  Signer certificate {index} subject: {_line(certificate.subject)}"
                )
                lines.append(
                    f"  Signer certificate {index} SHA-256: {certificate.sha256}"
                )
        else:
            lines.append("  No signer certificate could be read.")

        lines.extend(
            [
                "",
                "ARCHIVE",
                f"  ZIP entries: {self.zip_entries}",
                f"  Total uncompressed size: {_format_bytes(self.uncompressed_size)}",
            ]
        )
        if self.notes:
            lines.extend(["", "REVIEW NOTES"])
            lines.extend(f"  - {_line(note)}" for note in self.notes)

        lines.extend(
            [
                "",
                "LIMITATIONS",
                "  This report reads APK metadata only. It does not execute or modify the APK,",
                "  prove that an app is safe, or validate the APK's complete signature integrity.",
            ]
        )
        return "\n".join(lines) + "\n"


def _append_list_section(
    lines: list[str], title: str, values: tuple[str, ...], empty_label: str
) -> None:
    lines.extend(["", f"{title} ({len(values)})"])
    if values:
        lines.extend(f"  - {_line(value)}" for value in values)
    else:
        lines.append(f"  - {empty_label}")


def inspect_apk(
    path: str | Path,
    *,
    max_file_bytes: int = 20 * 1024 * 1024,
    max_uncompressed_bytes: int = 256 * 1024 * 1024,
    max_archive_entries: int = 50_000,
) -> ApkReport:
    """Inspect an APK without executing it or extracting its contents.

    Limits are checked against the ZIP central directory before the manifest parser
    is invoked. Only AndroidManifest.xml is read directly; no archive member is
    written to the filesystem.
    """
    apk_path = Path(path)
    try:
        file_size = apk_path.stat().st_size
    except OSError as exc:
        raise ApkInspectionError("The uploaded file could not be read.") from exc

    if file_size <= 0:
        raise ApkInspectionError("The uploaded file is empty.")
    if file_size > max_file_bytes:
        raise ApkInspectionError(
            f"The APK is larger than the configured upload limit ({_format_bytes(max_file_bytes)})."
        )

    digest = _sha256_file(apk_path)
    try:
        with zipfile.ZipFile(apk_path, "r") as archive:
            infos = archive.infolist()
            if len(infos) > max_archive_entries:
                raise ApkInspectionError(
                    "The archive contains too many entries to inspect safely."
                )

            total_uncompressed = sum(info.file_size for info in infos)
            if total_uncompressed > max_uncompressed_bytes:
                raise ApkInspectionError(
                    "The archive's expanded size exceeds the configured safety limit "
                    f"({_format_bytes(max_uncompressed_bytes)})."
                )
            resource_tables = [
                info for info in infos if info.filename == "resources.arsc"
            ]
            if any(info.file_size > 32 * 1024 * 1024 for info in resource_tables):
                raise ApkInspectionError(
                    "The Android resource table exceeds the safe parser size limit."
                )

            manifests = [
                info for info in infos if info.filename == "AndroidManifest.xml"
            ]
            if len(manifests) != 1:
                raise ApkInspectionError(
                    "A valid APK must contain exactly one AndroidManifest.xml entry."
                )
            manifest_info = manifests[0]
            if (
                manifest_info.file_size <= 0
                or manifest_info.file_size > 16 * 1024 * 1024
            ):
                raise ApkInspectionError(
                    "AndroidManifest.xml has an invalid or excessive size."
                )

            try:
                manifest_bytes = archive.read(manifest_info)
            except (OSError, RuntimeError, zipfile.BadZipFile) as exc:
                raise ApkInspectionError(
                    "AndroidManifest.xml could not be read from this archive."
                ) from exc

            dex_files: list[tuple[str, int]] = []
            native_abis: set[str] = set()
            for info in infos:
                match = DEX_RE.fullmatch(Path(info.filename).name)
                if match and "/" not in info.filename:
                    dex_files.append((info.filename, info.file_size))
                if info.filename.startswith("lib/"):
                    pieces = info.filename.split("/")
                    if len(pieces) >= 3 and pieces[1] and pieces[-1].endswith(".so"):
                        native_abis.add(pieces[1])

            dex_files.sort(key=lambda item: _dex_sort_key(item[0]))
            zip_entry_count = len(infos)
    except ApkInspectionError:
        raise
    except (OSError, zipfile.BadZipFile, zipfile.LargeZipFile) as exc:
        raise ApkInspectionError(
            "The uploaded file is not a readable APK/ZIP archive."
        ) from exc

    try:
        metadata = _parse_manifest(apk_path, manifest_bytes)
    except ApkInspectionError:
        raise
    except (
        Exception
    ) as exc:  # parser libraries raise several format-specific exceptions
        raise ApkInspectionError(
            "AndroidManifest.xml could not be parsed. The file may not be a valid or supported APK."
        ) from exc

    package_name = _line(metadata.get("package", ""))
    if not package_name:
        raise ApkInspectionError(
            "No Android package name could be read from the manifest."
        )

    permissions = tuple(sorted(set(filter(None, metadata.get("permissions", [])))))
    activities = tuple(sorted(set(filter(None, metadata.get("activities", [])))))
    services = tuple(sorted(set(filter(None, metadata.get("services", [])))))
    receivers = tuple(sorted(set(filter(None, metadata.get("receivers", [])))))
    providers = tuple(sorted(set(filter(None, metadata.get("providers", [])))))
    schemes = tuple(metadata.get("signature_schemes", []))
    certificates = tuple(metadata.get("certificates", []))

    notes: list[str] = []
    if metadata.get("debuggable") is True:
        notes.append("The manifest enables debuggable mode.")
    if metadata.get("allow_backup") is True:
        notes.append("The manifest enables app backup.")
    if metadata.get("cleartext_traffic") is True:
        notes.append("The manifest allows cleartext network traffic.")
    if not schemes:
        notes.append(
            "No supported APK signature scheme was detected; installation may be rejected by Android."
        )
    if not dex_files:
        notes.append("No root-level DEX files were found in the archive.")
    if permissions:
        notes.append(
            "Declared permissions are listed for review; a permission alone is not evidence of malicious behavior."
        )

    return ApkReport(
        sha256=digest,
        file_size=file_size,
        package_name=package_name,
        app_name=_line(metadata.get("app_name", "")) or "(not set in manifest)",
        version_name=_line(metadata.get("version_name", "")) or "(not set)",
        version_code=_line(metadata.get("version_code", "")) or "(not set)",
        min_sdk=_line(metadata.get("min_sdk", "")) or "(not set)",
        target_sdk=_line(metadata.get("target_sdk", "")) or "(not set)",
        debuggable=metadata.get("debuggable"),
        allow_backup=metadata.get("allow_backup"),
        cleartext_traffic=metadata.get("cleartext_traffic"),
        permissions=permissions,
        activities=activities,
        services=services,
        receivers=receivers,
        providers=providers,
        dex_files=tuple(dex_files),
        native_abis=tuple(sorted(native_abis)),
        zip_entries=zip_entry_count,
        uncompressed_size=total_uncompressed,
        signature_schemes=schemes,
        certificates=certificates,
        notes=tuple(notes),
    )


def _parse_manifest(apk_path: Path, manifest: bytes) -> dict[str, Any]:
    """Use PyAXMLParser for binary Android XML, with support for plain XML."""
    xml_candidate = manifest.removeprefix(b"\xef\xbb\xbf").lstrip()
    if xml_candidate.startswith(b"<"):
        return _parse_text_xml_manifest(xml_candidate)

    try:
        from pyaxmlparser import APK

        apk = APK(str(apk_path), skip_analysis=False, testzip=False)
        package = apk.get_package() or ""
        if not package:
            raise ApkInspectionError(
                "The Android package name is missing from the manifest."
            )

        def string_value(method_name: str) -> str:
            try:
                value = getattr(apk, method_name)()
            except Exception:  # noqa: BLE001 - parser exceptions vary across APK/XML formats
                return ""
            return "" if value is None else str(value)

        def bool_value(attribute: str) -> bool | None:
            try:
                return _parse_bool(apk.get_attribute_value("application", attribute))
            except Exception:  # noqa: BLE001 - parser exceptions vary across APK/XML formats
                return None

        signatures: list[str] = []
        for scheme, method_name in (
            ("v1/JAR", "is_signed_v1"),
            ("v2", "is_signed_v2"),
            ("v3", "is_signed_v3"),
        ):
            try:
                if getattr(apk, method_name)():
                    signatures.append(scheme)
            except Exception:  # noqa: BLE001, S112 - malformed signatures are just reported as undetected
                continue

        certificates: list[CertificateInfo] = []
        try:
            for cert in apk.get_certificates():
                fingerprint = getattr(cert, "sha256", b"")
                if isinstance(fingerprint, bytes):
                    fingerprint = fingerprint.hex().upper()
                else:
                    fingerprint = str(fingerprint).replace(":", "").upper()
                subject_obj = getattr(cert, "subject", "")
                subject = getattr(subject_obj, "human_friendly", None)
                if not subject:
                    subject = getattr(subject_obj, "native", subject_obj)
                if isinstance(subject, dict):
                    subject = ", ".join(
                        f"{key}={value}" for key, value in subject.items()
                    )
                if fingerprint:
                    certificates.append(
                        CertificateInfo(fingerprint, _line(str(subject)))
                    )
        except Exception:  # noqa: BLE001 - certificate parsers can raise format-specific exceptions
            # Some APKs contain malformed or unusual signing blocks. Report other
            # metadata and leave the certificate section empty instead of failing.
            certificates = []

        def get_names(method_name: str) -> list[str]:
            try:
                return list(getattr(apk, method_name)() or [])
            except Exception:  # noqa: BLE001 - a failed component lookup should not discard other metadata
                return []

        return {
            "package": package,
            "app_name": string_value("get_app_name"),
            "version_name": string_value("get_androidversion_name"),
            "version_code": string_value("get_androidversion_code"),
            "min_sdk": string_value("get_min_sdk_version"),
            "target_sdk": string_value("get_target_sdk_version"),
            "debuggable": bool_value("debuggable"),
            "allow_backup": bool_value("allowBackup"),
            "cleartext_traffic": bool_value("usesCleartextTraffic"),
            "permissions": get_names("get_permissions"),
            "activities": get_names("get_activities"),
            "services": get_names("get_services"),
            "receivers": get_names("get_receivers"),
            "providers": get_names("get_providers"),
            "signature_schemes": signatures,
            "certificates": certificates,
        }
    except ApkInspectionError:
        raise
    except ImportError as exc:
        raise ApkInspectionError(
            "The APK parser dependency is not installed. Install requirements.txt and retry."
        ) from exc


def _parse_text_xml_manifest(data: bytes) -> dict[str, Any]:
    """Parse the uncommon case where AndroidManifest.xml is plain XML text."""
    try:
        from defusedxml import ElementTree as ET

        root = ET.fromstring(data)
    except Exception as exc:
        raise ApkInspectionError(
            "The plain-text AndroidManifest.xml is malformed."
        ) from exc

    if _local_name(root.tag) != "manifest":
        raise ApkInspectionError(
            "AndroidManifest.xml does not have a manifest root element."
        )

    application = next(
        (child for child in root if _local_name(child.tag) == "application"), None
    )
    uses_sdk = next(
        (child for child in root if _local_name(child.tag) == "uses-sdk"), None
    )

    def attr(element: Any, name: str) -> str:
        if element is None:
            return ""
        return (
            element.get(ANDROID_NS + name)
            or element.get("android:" + name)
            or element.get(name)
            or ""
        )

    permissions: list[str] = []
    for child in root:
        if _local_name(child.tag).startswith("uses-permission"):
            name = attr(child, "name")
            if name:
                permissions.append(name)

    component_tags = {
        "activity": "activities",
        "activity-alias": "activities",
        "service": "services",
        "receiver": "receivers",
        "provider": "providers",
    }
    components: dict[str, list[str]] = {key: [] for key in component_tags.values()}
    if application is not None:
        for child in application:
            key = component_tags.get(_local_name(child.tag))
            name = attr(child, "name")
            if key and name:
                components[key].append(name)

    version_code = attr(root, "versionCode") or attr(root, "versionCodeMajor")
    return {
        "package": root.get("package", ""),
        "app_name": attr(application, "label"),
        "version_name": attr(root, "versionName"),
        "version_code": version_code,
        "min_sdk": attr(uses_sdk, "minSdkVersion"),
        "target_sdk": attr(uses_sdk, "targetSdkVersion"),
        "debuggable": _parse_bool(attr(application, "debuggable")),
        "allow_backup": _parse_bool(attr(application, "allowBackup")),
        "cleartext_traffic": _parse_bool(attr(application, "usesCleartextTraffic")),
        "permissions": permissions,
        **components,
        "signature_schemes": [],
        "certificates": [],
    }


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise ApkInspectionError("The uploaded file could not be read.") from exc
    return digest.hexdigest().upper()


def _parse_bool(value: Any) -> bool | None:
    if value is None:
        return None
    normalized = str(value).strip().lower()
    if normalized in {"true", "1", "0xffffffff"}:
        return True
    if normalized in {"false", "0", "0x0"}:
        return False
    return None


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1].split(":")[-1]


def _dex_sort_key(name: str) -> int:
    match = DEX_RE.fullmatch(name)
    if not match or not match.group(1):
        return 1
    try:
        return int(match.group(1))
    except ValueError:
        return 1


def _format_bool(value: bool | None) -> str:
    if value is None:
        return "Not specified"
    return "Yes" if value else "No"


def _format_bytes(value: int) -> str:
    size = float(value)
    for unit in ("B", "KB", "MB", "GB"):
        if size < 1024 or unit == "GB":
            return f"{size:.1f} {unit}" if unit != "B" else f"{value} B"
        size /= 1024
    return f"{value} B"


def _line(value: Any) -> str:
    """Keep untrusted manifest strings to one printable line in the report."""
    text = str(value or "")
    return " ".join("".join(ch if ch.isprintable() else " " for ch in text).split())
