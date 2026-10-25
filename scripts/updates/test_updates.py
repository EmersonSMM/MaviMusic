#!/usr/bin/env python3
"""Regression checks for update metadata, signing trust, and release-note selection."""
import base64
import json
import os
from pathlib import Path
import plistlib
import tempfile
import subprocess
import sys
import unittest
from unittest.mock import patch
import zipfile
import xml.etree.ElementTree as ET

import create_signing_key as keys
import prepare_sparkle
import publish_appcast as feed


class UpdatesTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="mavimusic-update-test-")
        self.root = Path(self.directory.name)
        self.configuration = json.loads((keys.ROOT / "desktopApp/packaging/updates/channel.json").read_text())

    def tearDown(self):
        self.directory.cleanup()

    def archive(self, version="1.8.36", arch="arm64", identifier="com.music.mavimusic", public=None):
        target = self.root / f"MaviMusic-{version}-macos-{arch}.zip"
        info = {"CFBundleIdentifier": identifier, "CFBundleVersion": version,
                "CFBundleShortVersionString": version, "SUPublicEDKey": public or self.configuration["publicKey"],
                "SURequireSignedFeed": True}
        cpu = {"arm64": 0x0100000C, "x64": 0x01000007}[arch]
        with zipfile.ZipFile(target, "w") as zipped:
            zipped.writestr("MaviMusic.app/Contents/Info.plist", plistlib.dumps(info))
            zipped.writestr("MaviMusic.app/Contents/MacOS/MaviMusic", b'\xcf\xfa\xed\xfe' + cpu.to_bytes(4, 'little'))
        return target

    def test_notes_select_exact_version_and_keep_markdown(self):
        changelog = self.root / "CHANGELOG.md"
        changelog.write_text("# Changes\n\n## 1.8.36\n\n- A & B < C\n\n## 1.8.35\n- Old\n")
        self.assertEqual("- A & B < C", feed.release_notes(changelog, "1.8.36"))
        with self.assertRaises(ValueError):
            feed.release_notes(changelog, "1.8.3")

    def test_refuses_wrong_bundle_version_architecture_identity_or_signing_key(self):
        archive = self.archive()
        feed.inspect_archive(archive, "1.8.36", "arm64", self.configuration["publicKey"])
        for version, arch, public in [("1.8.37", "arm64", self.configuration["publicKey"]),
                                      ("1.8.36", "x64", self.configuration["publicKey"]),
                                      ("1.8.36", "arm64", "wrong key")]:
            with self.assertRaises(ValueError):
                feed.inspect_archive(archive, version, arch, public)
        with self.assertRaises(ValueError):
            feed.inspect_archive(self.archive(identifier="another.application"), "1.8.36", "arm64", self.configuration["publicKey"])

    def test_key_creation_is_idempotent_private_and_refuses_silent_rotation(self):
        private = self.root / "private/key"
        config = self.root / "public.json"
        keys.create(private, config)
        before = private.read_bytes()
        self.assertEqual(0o600, private.stat().st_mode & 0o777)
        keys.create(private, config)
        self.assertEqual(before, private.read_bytes())
        public = json.loads(config.read_text())["publicKey"]
        self.assertEqual(public, keys.public_key(base64.b64decode(before)))
        private.unlink()
        with self.assertRaises(ValueError):
            keys.create(private, config)
        with self.assertRaises(ValueError):
            keys.create(keys.ROOT / "forbidden-private-key", self.root / "other.json")

    def test_corrupt_sparkle_archive_is_not_extracted(self):
        archive = self.root / "bad.tar.xz"
        archive.write_bytes(b"not the pinned upstream archive")
        with self.assertRaises(ValueError):
            prepare_sparkle.prepare(self.root / "framework", archive)
        self.assertFalse((self.root / "framework/Sparkle.framework").exists())

    def test_missing_ci_key_and_prereleases_cannot_publish(self):
        archive = self.archive()
        for version in ["1.8.36-beta1", "1.8.36;bad", "../1.8.36"]:
            with self.assertRaises(ValueError):
                feed.create_feed(archive, version, "arm64", "Notes", self.root / "feed.xml", Path("unused"))
        with patch.dict(os.environ, {"CI": "true", "SPARKLE_PRIVATE_KEY": ""}):
            with self.assertRaisesRegex(ValueError, "SPARKLE_PRIVATE_KEY"):
                feed.create_feed(archive, "1.8.36", "arm64", "Notes", self.root / "feed.xml", Path("unused"))

    @unittest.skipUnless(sys.platform == "darwin" and os.environ.get("SPARKLE_SIGNER"),
                         "DMG signing requires macOS and SPARKLE_SIGNER")
    def test_real_dmg_is_inspected_and_signed_without_a_portable_zip(self):
        seed = os.urandom(32)
        public = keys.public_key(seed)
        key = self.root / "dmg-signing-key"
        key.write_text(base64.b64encode(seed).decode("ascii"))
        key.chmod(0o600)
        configuration = self.root / "dmg-channel.json"
        configuration.write_text(json.dumps({"repository": "EmersonSMM/MaviMusic", "publicKey": public}))
        zipped = self.archive(public=public)
        staging = self.root / "image-contents"
        with zipfile.ZipFile(zipped) as archive:
            archive.extractall(staging)
        zipped.unlink()
        image = self.root / "MaviMusic-1.8.36-macos-arm64.dmg"
        subprocess.run(["hdiutil", "create", "-srcfolder", str(staging), "-format", "UDZO", "-fs", "HFS+",
                        "-volname", "MaviMusicTest", str(image)], check=True, capture_output=True)
        output = self.root / "appcast-arm64.xml"
        feed.create_feed(image, "1.8.36", "arm64", "Internal installer test", output,
                         Path(os.environ["SPARKLE_SIGNER"]), key_file=key, configuration=configuration)
        enclosure = ET.parse(output).getroot().find("channel/item/enclosure")
        self.assertTrue(enclosure.get("url").endswith(image.name))
        self.assertEqual(str(image.stat().st_size), enclosure.get("length"))
        self.assertFalse(zipped.exists())
        with self.assertRaisesRegex(ValueError, "version"):
            feed.inspect_archive(image, "1.8.37", "arm64", public)

    @unittest.skipUnless(os.environ.get("SPARKLE_SIGNER"), "Set SPARKLE_SIGNER for real Sparkle cryptographic checks")
    def test_real_archive_and_feed_signatures_detect_tampering(self):
        signer = Path(os.environ["SPARKLE_SIGNER"])
        key = self.root / "temporary-signing-key"
        seed = os.urandom(32)
        key.write_text(base64.b64encode(seed).decode('ascii'))
        key.chmod(0o600)
        configuration = self.root / "channel.json"
        public = keys.public_key(seed)
        configuration.write_text(json.dumps({"repository": "EmersonSMM/MaviMusic", "publicKey": public}))
        archive = self.archive(public=public)
        output = self.root / "appcast-arm64.xml"
        feed.create_feed(archive, "1.8.36", "arm64", "- Corrección & mejora <MP3>", output, signer,
                         key_file=key, configuration=configuration)
        root = ET.parse(output).getroot()
        item = root.find("channel/item")
        self.assertEqual("- Corrección & mejora <MP3>", item.find("description").text)
        enclosure = item.find("enclosure")
        self.assertTrue(enclosure.get("url").endswith(archive.name))
        self.assertEqual(str(archive.stat().st_size), enclosure.get("length"))
        import subprocess
        command = [str(signer), "--ed-key-file", str(key), "--verify"]
        signature = enclosure.get(f"{{{feed.NS}}}edSignature")
        with archive.open("ab") as file:
            file.write(b"tampered")
        self.assertNotEqual(0, subprocess.run(command + [str(archive), signature], capture_output=True).returncode)
        output.write_text(output.read_text().replace("Corrección", "Cambio falso"))
        self.assertNotEqual(0, subprocess.run(command + [str(output)], capture_output=True).returncode)


if __name__ == "__main__":
    unittest.main()
