#!/usr/bin/env python3
"""Produce signed, architecture-specific Sparkle feeds from a verified release DMG (or historical ZIP)."""
import argparse
import base64
from datetime import datetime, timezone
from email.utils import format_datetime
import json
import os
from pathlib import Path
import plistlib
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile
from create_signing_key import DEFAULT_KEY, ROOT, public_key

NS = "http://www.andymatuschak.org/xml-namespaces/sparkle"
ET.register_namespace("sparkle", NS)


def release_notes(changelog, version):
    text = Path(changelog).read_text()
    match = re.search(r"^## \[?" + re.escape(version) + r"\]?(?:\s[^\n]*)?\n(.*?)(?=^## |\Z)", text, re.M | re.S)
    if not match or not match.group(1).strip():
        raise ValueError(f"Missing changelog section for {version}")
    return match.group(1).strip()


def inspect_archive(archive, version, arch, configured_key):
    if Path(archive).suffix.lower() == ".dmg":
        # Inspect the actual signed delivery image, never a separate unverified staging bundle.
        with tempfile.TemporaryDirectory(prefix="mavimusic-dmg-inspect-") as temporary:
            mount = Path(temporary) / "volume"
            mount.mkdir()
            attached = False
            try:
                subprocess.run(["hdiutil", "attach", str(Path(archive).resolve()), "-readonly", "-nobrowse",
                                "-noautoopen", "-mountpoint", str(mount)],
                               stdin=subprocess.DEVNULL, capture_output=True, check=True, timeout=120)
                attached = True
                bundle = mount / "MaviMusic.app" / "Contents"
                info = plistlib.loads((bundle / "Info.plist").read_bytes())
                with (bundle / "MacOS/MaviMusic").open("rb") as binary:
                    launcher = binary.read(8)
            finally:
                if attached or mount.is_mount():
                    subprocess.run(["hdiutil", "detach", str(mount)], capture_output=True, check=True, timeout=30)
    elif Path(archive).suffix.lower() == ".zip":
        # Compatibility for validating previously published versions; new builds produce DMG only.
        with zipfile.ZipFile(archive) as zipped:
            info = plistlib.loads(zipped.read("MaviMusic.app/Contents/Info.plist"))
            with zipped.open("MaviMusic.app/Contents/MacOS/MaviMusic") as binary:
                launcher = binary.read(8)
    else:
        raise ValueError("Expected an installer DMG or historical update ZIP")
    if info.get("CFBundleIdentifier") != "com.music.mavimusic":
        raise ValueError("Update bundle identifier does not match MaviMusic")
    if info.get("CFBundleVersion") != version or info.get("CFBundleShortVersionString") != version:
        raise ValueError("Update bundle version does not match the release tag")
    if info.get("SUPublicEDKey") != configured_key or info.get("SURequireSignedFeed") is not True:
        raise ValueError("Update bundle does not carry the trusted signing configuration")
    cpu = int.from_bytes(launcher[4:8], "little")
    expected = {"arm64": 0x0100000C, "x64": 0x01000007}[arch]
    if launcher[:4] != b"\xcf\xfa\xed\xfe" or cpu != expected:
        raise ValueError("Installer architecture does not match its update channel")
    return info


def create_feed(archive, version, arch, notes, output, signer, key_file=None, configuration=None):
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
        raise ValueError("Only stable MAJOR.MINOR.PATCH releases enter the stable feed")
    config = json.loads(Path(configuration or ROOT / "desktopApp/packaging/updates/channel.json").read_text())
    info = inspect_archive(archive, version, arch, config["publicKey"])
    secret = Path(key_file).read_text().strip() if key_file else os.environ.get("SPARKLE_PRIVATE_KEY")
    if not secret:
        if os.environ.get("CI") and key_file is None:
            raise ValueError("Configure the SPARKLE_PRIVATE_KEY GitHub Actions secret before releasing")
        secret = Path(key_file or DEFAULT_KEY).expanduser().read_text().strip()
    seed = base64.b64decode(secret.strip(), validate=True)
    if public_key(seed) != config["publicKey"]:
        raise ValueError("Release signing key does not match the app's trusted public key")
    # sign_update reads only this explicit file. It never consults Keychain.
    with tempfile.TemporaryDirectory(prefix="mavimusic-feed-sign-") as temporary:
        key = Path(temporary) / "key"
        key.write_text(secret.strip() + "\n")
        key.chmod(0o600)
        def sign(path, plain=False):
            command = [str(signer), "--ed-key-file", str(key)]
            if plain:
                command.append("-p")
            result = subprocess.run(command + [str(path)], capture_output=True, check=True, text=True)
            return result.stdout.strip()
        signature = sign(archive, True)
        root = ET.Element("rss", version="2.0")
        channel = ET.SubElement(root, "channel")
        ET.SubElement(channel, "title").text = f"MaviMusic · macOS {arch}"
        release_url = f"https://github.com/{config['repository']}/releases/tag/v{version}"
        ET.SubElement(channel, "link").text = release_url
        ET.SubElement(channel, "language").text = "es"
        item = ET.SubElement(channel, "item")
        ET.SubElement(item, "title").text = f"MaviMusic {version}"
        ET.SubElement(item, "link").text = release_url
        ET.SubElement(item, "pubDate").text = format_datetime(datetime.now(timezone.utc))
        ET.SubElement(item, f"{{{NS}}}version").text = info["CFBundleVersion"]
        ET.SubElement(item, f"{{{NS}}}shortVersionString").text = version
        ET.SubElement(item, f"{{{NS}}}minimumSystemVersion").text = "12.0.0"
        if arch == "arm64":
            ET.SubElement(item, f"{{{NS}}}hardwareRequirements").text = "arm64"
        ET.SubElement(item, "description", {f"{{{NS}}}format": "markdown"}).text = notes
        ET.SubElement(item, "enclosure", {
            "url": f"https://github.com/{config['repository']}/releases/download/v{version}/{Path(archive).name}",
            "length": str(Path(archive).stat().st_size), "type": "application/octet-stream",
            f"{{{NS}}}edSignature": signature, f"{{{NS}}}os": "macos",
        })
        output = Path(output)
        output.parent.mkdir(parents=True, exist_ok=True)
        ET.indent(root)
        ET.ElementTree(root).write(output, encoding="utf-8", xml_declaration=True)
        sign(output)
        # Verify the generated signatures immediately, including the signed changelog/feed.
        subprocess.run([str(signer), "--ed-key-file", str(key), "--verify", str(archive), signature],
                       check=True, capture_output=True)
        subprocess.run([str(signer), "--ed-key-file", str(key), "--verify", str(output)],
                       check=True, capture_output=True)
    print(f"Signed {output.name} for MaviMusic {version} ({arch})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", required=True, type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--arch", choices=["arm64", "x64"], required=True)
    parser.add_argument("--changelog", type=Path, default=ROOT / "CHANGELOG.md")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--signer", type=Path, required=True)
    parser.add_argument("--key-file", type=Path)
    args = parser.parse_args()
    create_feed(args.archive, args.version, args.arch, release_notes(args.changelog, args.version), args.output, args.signer, args.key_file)
