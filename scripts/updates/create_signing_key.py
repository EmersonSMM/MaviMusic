#!/usr/bin/env python3
"""Create a release signing seed in a private file, without accessing Keychain."""
import argparse
import base64
import json
import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_KEY = Path.home() / ".config/mavimusic/release-signing/ed25519.key"


def public_key(seed):
    if len(seed) != 32:
        raise ValueError("Expected a base64-encoded 32-byte Ed25519 seed")
    # RFC 8410 PKCS#8: wrap the seed for OpenSSL. No key material enters command arguments.
    private_der = bytes.fromhex("302e020100300506032b657004220420") + seed
    openssl_bin = next((p for p in ["/opt/homebrew/bin/openssl", "/usr/local/bin/openssl", "openssl"]
                        if shutil.which(p)), "openssl")
    result = subprocess.run([openssl_bin, "pkey", "-inform", "DER", "-pubout", "-outform", "DER"],
                            input=private_der, capture_output=True, check=True)
    return base64.b64encode(result.stdout[-32:]).decode("ascii")


def create(path, configuration):
    path = Path(path).expanduser().absolute()
    configuration = Path(configuration)
    if ROOT == path or ROOT in path.parents:
        raise ValueError("The private key must be outside the repository")
    if path.exists():
        seed = base64.b64decode(path.read_text().strip(), validate=True)
    else:
        if configuration.exists():
            raise ValueError("Public key already configured. Recover the original private key; do not rotate it silently.")
        path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
        os.chmod(path.parent, 0o700)
        seed = os.urandom(32)
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as output:
            output.write(base64.b64encode(seed).decode("ascii") + "\n")
    os.chmod(path, 0o600)
    public = public_key(seed)
    if configuration.exists() and json.loads(configuration.read_text())["publicKey"] != public:
        raise ValueError("Signing key does not match the configured public key")
    configuration.parent.mkdir(parents=True, exist_ok=True)
    configuration.write_text(json.dumps({"repository": "EmersonSMM/MaviMusic", "publicKey": public}, indent=2) + "\n")
    print(f"Private signing key: {path} (permissions 0600; never commit this file)")
    print(f"Public configuration: {configuration}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--key-file", type=Path, default=DEFAULT_KEY)
    parser.add_argument("--configuration", type=Path, default=ROOT / "desktopApp/packaging/updates/channel.json")
    args = parser.parse_args()
    create(args.key_file, args.configuration)
