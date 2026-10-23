import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import activate_github as activation


class ActivationTest(unittest.TestCase):
    def test_version_is_stable_and_cannot_inject_arguments(self):
        self.assertEqual("1.8.36", activation.version_number("v1.8.36"))
        for value in ("1.8.36;evil", "../1.8.36", "1.8.36-beta", "-x", "1.8"):
            with self.assertRaises(ValueError):
                activation.version_number(value)

    def test_snapshot_preserves_original_index_and_has_only_current_source(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            subprocess.run(["git", "init", "-q", str(root)], check=True)
            def git(*args):
                return subprocess.check_output(["git", "-C", str(root), *args])
            git("config", "user.name", "Test")
            git("config", "user.email", "test@example.invalid")
            (root / "desktopApp/src").mkdir(parents=True)
            (root / "desktopApp/src/source.kt").write_text("old")
            (root / "desktopApp/src/removed.kt").write_text("gone")
            git("add", ".")
            git("commit", "-qm", "base")
            index = (root / ".git/index").read_bytes()
            old_head = git("rev-parse", "HEAD")
            (root / "desktopApp/src/source.kt").write_text("updated")
            (root / "desktopApp/src/removed.kt").unlink()
            (root / "desktopApp/src/new.kt").write_text("new source")
            (root / "private.key").write_text("must not upload")
            (root / "installer.dmg").write_text("must not upload")
            (root / "personal-notes.txt").write_text("not source code")
            with tempfile.TemporaryDirectory() as index_directory, patch.object(activation, "ROOT", root):
                tree, commit, branch = activation.snapshot(dict(os.environ), Path(index_directory), "1.8.36", "test")
            self.assertEqual(index, (root / ".git/index").read_bytes())
            self.assertEqual(old_head, git("rev-parse", "HEAD"))
            self.assertEqual("mavimusic-release/1.8.36-" + tree[:12], branch)
            self.assertEqual(b"desktopApp/src/new.kt\ndesktopApp/src/source.kt\n", git("ls-tree", "-r", "--name-only", commit))
            self.assertEqual(b"updated", git("show", f"{commit}:desktopApp/src/source.kt"))
            self.assertEqual(commit.encode(), git("rev-list", "--parents", "-n", "1", commit).strip())

    def test_unrecognized_env_secret_prevents_source_publication(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            subprocess.run(["git", "init", "-q", str(root)], check=True)
            subprocess.run(["git", "-C", str(root), "-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "--allow-empty", "-qm", "base"], check=True)
            (root / "desktopApp").mkdir()
            (root / "desktopApp/.env.staging").write_text("PRIVATE=value")
            with tempfile.TemporaryDirectory() as index_directory, patch.object(activation, "ROOT", root):
                with self.assertRaisesRegex(ValueError, "archivo privado"):
                    activation.snapshot(dict(os.environ), Path(index_directory), "1.8.36", "test")

    def test_api_404_is_distinguished_from_missing_permissions(self):
        hub = activation.GitHub("gh", {}, "owner/repo")
        with patch.object(activation, "command", return_value=subprocess.CompletedProcess([], 1, b"", b"HTTP 404")):
            self.assertIsNone(hub.api("missing", missing=True))
        with patch.object(activation, "command", return_value=subprocess.CompletedProcess([], 1, b"", b"HTTP 403 Forbidden")):
            with self.assertRaises(RuntimeError):
                hub.api("forbidden", missing=True)

    def test_failed_or_incomplete_release_stays_unpublished(self):
        class Hub:
            def repo_api(self, _):
                return {"draft": True, "target_commitish": "commit", "assets": []}
        with self.assertRaisesRegex(ValueError, "Faltan archivos"):
            activation.verify_release(Hub(), Path("unused"), "1.8.36", "commit", Path("unused"))

    def test_observes_only_the_new_dispatch_for_the_exact_commit(self):
        class Hub:
            def repo_api(self, _):
                return {"workflow_runs": [
                    {"id": 1, "head_sha": "wanted"},
                    {"id": 2, "head_sha": "other"},
                    {"id": 3, "head_sha": "wanted"},
                ]}
        self.assertEqual(3, activation.find_run(Hub(), "branch", "wanted", {1}, attempts=1)["id"])

    def test_publication_requires_successful_ci_and_valid_signatures(self):
        class Hub:
            repo = "owner/repo"
            def __init__(self, conclusion="success"):
                self.conclusion, self.commands = conclusion, []
            def run(self, *args, **kwargs):
                self.commands.append(args)
            def repo_api(self, endpoint, **kwargs):
                if endpoint.startswith("actions/"):
                    return {"conclusion": self.conclusion, "head_sha": "commit"}
                if not any(cmd[:2] == ("release", "edit") for cmd in self.commands):
                    return None
                return {"tag_name": "v1.8.36", "draft": False}
        hub = Hub("failure")
        with patch.object(activation, "verify_release") as verify:
            with self.assertRaises(ValueError):
                activation.finish_release(hub, 1, Path("unused"), "1.8.36", "commit", Path("unused"), 1)
            verify.assert_not_called()
            self.assertFalse(any(cmd[:2] == ("release", "edit") for cmd in hub.commands))
        hub = Hub()
        with patch.object(activation, "verify_release", side_effect=ValueError("invalid signature")):
            with self.assertRaises(ValueError):
                activation.finish_release(hub, 1, Path("unused"), "1.8.36", "commit", Path("unused"), 1)
            self.assertFalse(any(cmd[:2] == ("release", "edit") for cmd in hub.commands))
        hub = Hub()
        with patch.object(activation, "verify_release") as verify:
            activation.finish_release(hub, 1, Path("unused"), "1.8.36", "commit", Path("unused"), 1)
            verify.assert_called_once()
            self.assertEqual(1, sum(cmd[:2] == ("release", "edit") for cmd in hub.commands))

    def test_plan_does_not_authenticate_install_or_publish(self):
        import argparse
        args = argparse.Namespace(version="1.8.36", key_file=Path("unused"), plan=True)
        with patch.object(activation, "preflight", return_value=("owner/repo", Path("unused"))), patch.object(activation, "command") as runner:
            activation.activate(args)
            runner.assert_not_called()


if __name__ == "__main__":
    unittest.main()
