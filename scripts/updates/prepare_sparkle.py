#!/usr/bin/env python3
"""Download a pinned upstream Sparkle distribution and verify it before extraction."""
import argparse
import hashlib
from pathlib import Path
import shutil
import tarfile
import urllib.request

VERSION = "2.10.0"
SHA256 = "c2bf58aa8387266ac179357b1415d6f2635f044da8be41042af32425dae6da0c"
URL = f"https://github.com/sparkle-project/Sparkle/releases/download/{VERSION}/Sparkle-{VERSION}.tar.xz"


def prepare(output, archive=None):
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    cached = Path(archive) if archive else output.parent / f"Sparkle-{VERSION}.tar.xz"
    if not cached.exists():
        partial = cached.with_suffix(".download")
        try:
            with urllib.request.urlopen(URL, timeout=60) as source, partial.open("wb") as target:
                shutil.copyfileobj(source, target)
            partial.replace(cached)
        finally:
            partial.unlink(missing_ok=True)
    hasher = hashlib.sha256()
    with cached.open("rb") as stream:
        for chunk in iter(lambda: stream.read(65536), b""):
            hasher.update(chunk)
    actual = hasher.hexdigest()
    if actual != SHA256:
        raise ValueError("Sparkle archive checksum mismatch; refusing to use it")
    # The hash identifies the trusted upstream tarball, including framework symlinks.
    with tarfile.open(cached) as archive_file:
        try:
            archive_file.extractall(output, filter="fully_trusted")
        except TypeError:
            archive_file.extractall(output)
    (output / "verified-sha256.txt").write_text(SHA256 + "\n")
    return output


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True)
    parser.add_argument("--archive")
    args = parser.parse_args()
    prepare(args.output, args.archive)
