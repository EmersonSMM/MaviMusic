import base64
import hashlib
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import publish_release as release


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.version = '1.8.50'
        self.commit = 'a' * 40
        for name in release.required_assets(self.version):
            (self.root / name).write_bytes(b'build bytes')
        for arch in ('arm64', 'x64'):
            signature = base64.b64encode(bytes(64)).decode()
            archive = f'MaviMusic-{self.version}-macos-{arch}.zip'
            (self.root / f'appcast-{arch}.xml').write_text(f'<rss xmlns:s="{release.NS}"><channel><item><s:version>{self.version}</s:version><enclosure url="https://github.com/owner/repo/releases/download/v{self.version}/{archive}" length="11" s:edSignature="{signature}" /></item></channel></rss>')
        self.manifest()

    def tearDown(self):
        self.tmp.cleanup()

    def manifest(self):
        (self.root / 'SHA256SUMS').write_text(''.join(f'{release.sha256(p)}  {p.name}\n' for p in sorted(self.root.iterdir()) if p.name != 'SHA256SUMS'))

    def test_all_platforms_and_manifest_are_required(self):
        self.assertEqual(15, len(release.inspect_assets(self.root, self.version, 'owner/repo')))
        (self.root / f'MaviMusic-{self.version}-windows-x64.msi').unlink()
        with self.assertRaisesRegex(ValueError, 'asset set'):
            release.inspect_assets(self.root, self.version, 'owner/repo')

    def test_corrupt_or_extra_files_cannot_be_published(self):
        (self.root / f'MaviMusic-{self.version}-windows-x64.msi').write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError, 'manifest'):
            release.inspect_assets(self.root, self.version, 'owner/repo')
        self.manifest()
        (self.root / 'private.key').write_bytes(b'must not upload')
        with self.assertRaisesRegex(ValueError, 'asset set'):
            release.inspect_assets(self.root, self.version, 'owner/repo')

    def test_feed_must_reference_this_repository_version_and_zip_size(self):
        feed = self.root / 'appcast-arm64.xml'
        feed.write_text(feed.read_text().replace('owner/repo', 'other/repo'))
        self.manifest()
        with self.assertRaisesRegex(ValueError, 'feed URL'):
            release.inspect_assets(self.root, self.version, 'owner/repo')

    def hub(self, corrupt=False, public=False, competing=False):
        test = self
        class Hub:
            repo = 'owner/repo'
            def __init__(self):
                self.commands = []
                self.draft = not public
            def run(self, *args):
                self.commands.append(args)
                if args[:2] == ('release', 'edit'):
                    self.draft = False
            def api(self, endpoint, missing=False):
                if endpoint == 'releases/latest':
                    if competing and any(c[:2] == ('release', 'upload') for c in self.commands):
                        return {'tag_name': 'v1.8.51'}
                    return None
                if endpoint.startswith('git/ref'):
                    return {'object': {'type': 'commit', 'sha': test.commit}}
                if endpoint.startswith('releases/tags/'):
                    raise AssertionError('Drafts cannot be queried by tag')
                if endpoint.startswith('releases?') or endpoint == 'releases/42':
                    if not public and not any(c[:2] == ('release', 'create') for c in self.commands):
                        return [] if endpoint.startswith('releases?') else None
                    assets = [{'name': p.name, 'size': p.stat().st_size, 'state': 'uploaded', 'digest': 'sha256:' + release.sha256(p)} for p in test.root.iterdir()]
                    if corrupt: assets[0]['digest'] = 'sha256:incorrect'
                    value = {'id': 42, 'tag_name': 'v' + test.version, 'draft': self.draft, 'target_commitish': test.commit, 'assets': assets, 'html_url': 'https://example.invalid/release'}
                    return [value] if endpoint.startswith('releases?') else value
        return Hub()

    def test_draft_is_published_only_after_every_uploaded_digest_matches(self):
        hub = self.hub()
        release.publish(hub, self.root, self.version, self.commit, Path('notes'))
        self.assertEqual(['create', 'upload', 'edit'], [c[1] for c in hub.commands])
        self.assertIn('--draft', hub.commands[0])

    def test_upload_mismatch_leaves_draft_unpublished(self):
        hub = self.hub(corrupt=True)
        with self.assertRaisesRegex(ValueError, 'differs'):
            release.publish(hub, self.root, self.version, self.commit, Path('notes'))
        self.assertFalse(any(c[:2] == ('release', 'edit') for c in hub.commands))
        self.assertTrue(hub.draft)

    def test_existing_public_release_is_never_overwritten(self):
        hub = self.hub(public=True)
        with self.assertRaisesRegex(ValueError, 'overwrite'):
            release.publish(hub, self.root, self.version, self.commit, Path('notes'))
        self.assertEqual([], hub.commands)

    def test_newer_release_published_during_upload_prevents_downgrade(self):
        hub = self.hub(competing=True)
        with self.assertRaisesRegex(ValueError, 'newer'):
            release.publish(hub, self.root, self.version, self.commit, Path('notes'))
        self.assertFalse(any(c[:2] == ('release', 'edit') for c in hub.commands))

    def test_authenticated_listing_finds_draft_on_later_page(self):
        class Hub:
            def api(self, endpoint):
                if endpoint.endswith('page=1'):
                    return [{'tag_name': f'v0.0.{i}'} for i in range(100)]
                return [{'id': 42, 'tag_name': 'v1.8.50', 'draft': True}]
        self.assertEqual(42, release.find_release(Hub(), 'v1.8.50')['id'])

    def test_wrong_tag_commit_blocks_uploads(self):
        hub = self.hub()
        with self.assertRaisesRegex(ValueError, 'different commit'):
            release.publish(hub, self.root, self.version, 'b' * 40, Path('notes'))
        self.assertEqual([], hub.commands)


if __name__ == '__main__':
    unittest.main()
