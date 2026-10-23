#!/usr/bin/env python3
"""Optionally notarize a Developer ID signed bundle using an App Store Connect API key."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile


def notarize(app):
    required = ["APPLE_API_KEY_P8", "APPLE_API_KEY_ID", "APPLE_API_ISSUER"]
    values = [os.environ.get(key) for key in required]
    if not any(values):
        print("Apple notarization not configured; this build is not notarized.")
        return
    if not all(values) or not os.environ.get("MACOS_SIGN_IDENTITY"):
        raise ValueError("Notarization requires a Developer ID signing identity and all three Apple API secrets")
    with tempfile.TemporaryDirectory(prefix="mavimusic-notary-") as temporary:
        private = Path(temporary) / "AuthKey.p8"
        private.write_text(values[0])
        private.chmod(0o600)
        archive = Path(temporary) / "MaviMusic.zip"
        subprocess.run(["ditto", "-c", "-k", "--sequesterRsrc", "--keepParent", str(app), str(archive)], check=True)
        subprocess.run(["xcrun", "notarytool", "submit", str(archive), "--key", str(private),
                        "--key-id", values[1], "--issuer", values[2], "--wait", "--timeout", "30m"], check=True)
        subprocess.run(["xcrun", "stapler", "staple", str(app)], check=True)
        subprocess.run(["xcrun", "stapler", "validate", str(app)], check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", type=Path, required=True)
    notarize(parser.parse_args().app)
