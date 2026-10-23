#!/usr/bin/env python3
"""Sign embedded native code after Sparkle is copied into the application bundle."""
import argparse
import os
from pathlib import Path
import plistlib
import subprocess
import tempfile

MAGIC = {b'\xcf\xfa\xed\xfe', b'\xce\xfa\xed\xfe', b'\xfe\xed\xfa\xcf', b'\xfe\xed\xfa\xce', b'\xca\xfe\xba\xbe', b'\xbe\xba\xfe\xca'}


def sign(app):
    app = Path(app).resolve()
    identity = os.environ.get("MACOS_SIGN_IDENTITY") or "-"
    # JIT and extracted FFmpeg/OpenJFX libraries require the same exceptions as jpackage.
    entitlements = {"com.apple.security.cs.allow-jit": True,
                    "com.apple.security.cs.allow-unsigned-executable-memory": True,
                    "com.apple.security.cs.disable-library-validation": True}
    def codesign(path, entitlements_file=None):
        args = ["codesign", "--force", "--sign", identity, "--options", "runtime"]
        if identity != "-":
            args += ["--timestamp"]
        if entitlements_file:
            args += ["--entitlements", str(entitlements_file)]
        else:
            args += ["--preserve-metadata=entitlements"]
        subprocess.run(args + [str(path)], check=True, stdout=subprocess.DEVNULL)
    with tempfile.TemporaryDirectory(prefix="mavimusic-sign-") as temporary:
        permissions = Path(temporary) / "entitlements.plist"
        permissions.write_bytes(plistlib.dumps(entitlements))
        if identity != "-":
            # Sign leaves first, then XPC/app/framework containers, then the outer app.
            for path in sorted(app.rglob("*"), key=lambda p: len(p.parts), reverse=True):
                if path.is_symlink() or not path.is_file():
                    continue
                with path.open("rb") as file:
                    magic = file.read(4)
                if magic in MAGIC:
                    codesign(path, permissions if path.name in ("java", "MaviMusic") else None)
            for path in sorted(app.rglob("*"), key=lambda p: len(p.parts), reverse=True):
                if not path.is_symlink() and path.is_dir() and path.suffix in (".framework", ".xpc", ".app", ".bundle"):
                    codesign(path)
        else:
            # Preserve upstream Sparkle signatures; local development never reads Keychain.
            codesign(app / "Contents/Frameworks/libmavimusic_updates.dylib")
        codesign(app, permissions)
    subprocess.run(["codesign", "--verify", "--deep", "--strict", str(app)], check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", type=Path, required=True)
    sign(parser.parse_args().app)
