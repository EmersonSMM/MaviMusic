#!/usr/bin/env python3
"""Import Developer ID only on a disposable GitHub macOS runner, never on a user's machine."""
import base64
import json
import os
from pathlib import Path
import secrets
import shlex
import subprocess
import sys


def run(*args):
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout


def main(mode):
    if os.environ.get("GITHUB_ACTIONS") != "true":
        raise ValueError("This helper is restricted to disposable GitHub Actions runners")
    directory = Path(os.environ["RUNNER_TEMP"]) / "mavimusic-signing"
    state = directory / "state.json"
    if mode == "cleanup":
        if state.exists():
            saved = json.loads(state.read_text())
            run("security", "list-keychains", "-d", "user", "-s", *saved["previous"])
            run("security", "delete-keychain", saved["keychain"])
        import shutil
        shutil.rmtree(directory, ignore_errors=True)
        return
    certificate = os.environ.get("CERTIFICATE")
    identity = os.environ.get("MACOS_SIGN_IDENTITY")
    if not certificate and not identity:
        print("No Developer ID configured: build uses ad-hoc signing and signed Sparkle updates.")
        return
    if not certificate or not identity:
        raise ValueError("Configure both MACOS_CERTIFICATE_P12 and MACOS_SIGN_IDENTITY")
    directory.mkdir(mode=0o700, exist_ok=True)
    p12 = directory / "certificate.p12"
    p12.write_bytes(base64.b64decode(certificate, validate=True))
    p12.chmod(0o600)
    keychain = str(directory / "build.keychain-db")
    password = secrets.token_urlsafe(32)
    previous = shlex.split(run("security", "list-keychains", "-d", "user"))
    state.write_text(json.dumps({"previous": previous, "keychain": keychain}))
    run("security", "create-keychain", "-p", password, keychain)
    run("security", "set-keychain-settings", "-lut", "21600", keychain)
    run("security", "unlock-keychain", "-p", password, keychain)
    run("security", "import", str(p12), "-P", os.environ.get("CERTIFICATE_PASSWORD", ""), "-k", keychain, "-T", "/usr/bin/codesign")
    run("security", "set-key-partition-list", "-S", "apple-tool:,apple:", "-s", "-k", password, keychain)
    run("security", "list-keychains", "-d", "user", "-s", keychain, *previous)
    p12.unlink()
    print("Developer ID imported into temporary CI keychain")


if __name__ == "__main__":
    try:
        main(sys.argv[1])
    except subprocess.CalledProcessError:
        # subprocess arguments contain the disposable keychain password; never print them.
        raise SystemExit("Apple signing credential setup failed") from None
