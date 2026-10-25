#!/usr/bin/env python3
"""Upload into a draft, verify the complete release, then activate the update channel."""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

NS = 'http://www.andymatuschak.org/xml-namespaces/sparkle'


def version_tuple(version):
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', version):
        raise ValueError('Use a stable MAJOR.MINOR.PATCH version')
    return tuple(map(int, version.split('.')))


def required_assets(version):
    return {'SHA256SUMS', f'MaviMusic-{version}-windows-x64-setup.exe',
            f'MaviMusic-{version}-windows-x64.msi', f'MaviMusic-{version}-windows-x64-portable.zip',
            f'MaviMusic-{version}-linux-x86_64.AppImage', f'MaviMusic-{version}-linux-amd64.deb',
            f'MaviMusic-{version}-linux-x86_64.rpm'} | {
        name for arch in ('arm64', 'x64') for name in
        (f'appcast-{arch}.xml', *(f'MaviMusic-{version}-macos-{arch}.{ext}' for ext in ('zip', 'dmg', 'pkg')))}


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def inspect_assets(directory, version, repo):
    version_tuple(version)
    paths = {p.name: p for p in directory.iterdir()}
    expected = required_assets(version)
    if set(paths) != expected:
        raise ValueError(f'Unexpected asset set; missing={sorted(expected-set(paths))}, extra={sorted(set(paths)-expected)}')
    for name, path in paths.items():
        if path.is_symlink() or not path.is_file() or path.stat().st_size == 0:
            raise ValueError(f'Empty or unsafe asset: {name}')
    hashes = {name: sha256(path) for name, path in paths.items()}
    manifest = {}
    for line in paths['SHA256SUMS'].read_text().splitlines():
        match = re.fullmatch(r'([a-f0-9]{64})  (.+)', line)
        if not match or match[2] in manifest:
            raise ValueError('Invalid or duplicate checksum entry')
        manifest[match[2]] = match[1]
    if manifest != {name: digest for name, digest in hashes.items() if name != 'SHA256SUMS'}:
        raise ValueError('Checksum manifest does not match release files')
    for arch in ('arm64', 'x64'):
        item = ET.parse(paths[f'appcast-{arch}.xml']).getroot().find('channel/item')
        if item is None or item.findtext(f'{{{NS}}}version') != version:
            raise ValueError(f'Unexpected feed version: {arch}')
        enclosure = item.find('enclosure')
        archive = f'MaviMusic-{version}-macos-{arch}.zip'
        if enclosure is None or enclosure.get('url') != f'https://github.com/{repo}/releases/download/v{version}/{archive}':
            raise ValueError(f'Unexpected feed URL: {arch}')
        if int(enclosure.get('length', '0')) != paths[archive].stat().st_size:
            raise ValueError(f'Unexpected archive size: {arch}')
        signature = base64.b64decode(enclosure.get(f'{{{NS}}}edSignature', ''), validate=True)
        if len(signature) != 64:
            raise ValueError(f'Missing archive signature: {arch}')
    # Both Mac jobs cryptographically verify feed/ZIP signatures with Sparkle before uploading.
    # This job preserves those exact bytes and verifies GitHub's SHA-256 digest for every file.
    return {name: {'size': path.stat().st_size, 'digest': f'sha256:{hashes[name]}'} for name, path in paths.items()}


class GitHub:
    def __init__(self, repo):
        if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repo):
            raise ValueError('Invalid GitHub repository')
        self.repo = repo

    def run(self, *args):
        return subprocess.run(['gh', *args], check=True, capture_output=True).stdout

    def api(self, endpoint, missing=False):
        result = subprocess.run(['gh', 'api', f'repos/{self.repo}/{endpoint}'], capture_output=True)
        if result.returncode:
            error = result.stderr.decode(errors='replace')
            if missing and 'HTTP 404' in error:
                return None
            raise RuntimeError(error.strip())
        return json.loads(result.stdout)


def check_tag(hub, version, commit, missing=False):
    ref = hub.api(f'git/ref/tags/v{version}', missing=missing)
    if ref is None:
        return
    obj = ref['object']
    for _ in range(8):
        if obj['type'] != 'tag':
            break
        obj = hub.api(f'git/tags/{obj["sha"]}')['object']
    if obj['type'] != 'commit' or obj['sha'] != commit:
        raise ValueError('Release tag points to a different commit')


def check_latest(hub, version):
    latest = hub.api('releases/latest', missing=True)
    if latest and version_tuple(latest['tag_name'].removeprefix('v')) >= version_tuple(version):
        raise ValueError('An equal or newer release is already public')


def verify_uploaded(release, expected, commit):
    if not release['draft'] or release.get('prerelease') or release['target_commitish'] != commit:
        raise ValueError('Release must remain a stable draft for this exact commit')
    assets = release['assets']
    if len(assets) != len(expected) or {a['name'] for a in assets} != set(expected):
        raise ValueError('Incomplete or unexpected uploaded asset set')
    for asset in assets:
        wanted = expected[asset['name']]
        if asset['state'] != 'uploaded' or asset['size'] != wanted['size'] or asset.get('digest') != wanted['digest']:
            raise ValueError(f'Uploaded file differs from verified build: {asset["name"]}')


def publish(hub, directory, version, commit, notes):
    if not re.fullmatch(r'[a-f0-9]{40}', commit):
        raise ValueError('Use the exact build commit SHA')
    expected = inspect_assets(directory, version, hub.repo)
    check_latest(hub, version)
    check_tag(hub, version, commit, missing=True)
    tag = f'v{version}'
    existing = hub.api(f'releases/tags/{tag}', missing=True)
    if existing and (not existing['draft'] or existing['target_commitish'] != commit):
        raise ValueError('Refusing to overwrite an existing release from another build')
    if not existing:
        hub.run('release', 'create', tag, '--repo', hub.repo, '--draft', '--target', commit,
                '--title', f'MaviMusic {version}', '--notes-file', str(notes))
    check_tag(hub, version, commit)
    hub.run('release', 'upload', tag, '--repo', hub.repo, '--clobber',
            *(str(directory / name) for name in sorted(expected)))
    verify_uploaded(hub.api(f'releases/tags/{tag}'), expected, commit)
    # Recheck after uploads to avoid replacing a newer channel published in the meantime.
    check_latest(hub, version)
    hub.run('release', 'edit', tag, '--repo', hub.repo, '--draft=false', '--latest')
    released = hub.api(f'releases/tags/{tag}')
    if released['draft']:
        raise RuntimeError('GitHub did not publish the verified draft')
    print(f'Published complete release: {released["html_url"]}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--repository', required=True)
    parser.add_argument('--notes', type=Path, required=True)
    args = parser.parse_args()
    publish(GitHub(args.repository), args.directory, args.version, args.commit, args.notes)
