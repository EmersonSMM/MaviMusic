#!/usr/bin/env python3
"""Activate signed releases using GitHub's official browser authorization flow."""
import argparse
import base64
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import time
from urllib.parse import quote
import xml.etree.ElementTree as ET

from create_signing_key import DEFAULT_KEY, ROOT, public_key
from publish_appcast import NS, release_notes
from publish_release import find_release

WORKFLOW = ".github/workflows/release.yml"
EXCLUDED = ("*.dmg", "*.pkg", "*.zip", "*.p12", "*.p8", "*.pem", "*.key", "*.jks", "*.keystore", "local.properties", ".env", ".env.local", ".env.production")


def command(args, *, env=None, data=None, live=False, check=True, cwd=None, timeout=None):
    result = subprocess.run([str(arg) for arg in args], cwd=cwd or ROOT, env=env, input=data,
                            stdout=None if live else subprocess.PIPE, stderr=None if live else subprocess.PIPE,
                            timeout=timeout)
    if check and result.returncode:
        # Never include stdin or environment: they may contain a signing key or OAuth credential.
        detail = (result.stderr or b"").decode(errors="replace").strip()
        raise RuntimeError(detail or f"Falló {Path(str(args[0])).name} (código {result.returncode}).")
    return result


def git(*args, env=None, data=None):
    return command(["git", *args], env=env, data=data).stdout.decode().strip()


def version_number(value):
    value = value.removeprefix("v")
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", value):
        raise ValueError("La versión debe tener el formato 1.8.36, sin etiquetas beta.")
    return value


def default_version():
    text = (ROOT / "desktopApp/build.gradle.kts").read_text()
    match = re.search(r'val appVersion:.*?\?: "([0-9]+\.[0-9]+\.[0-9]+)"', text, re.S)
    if not match:
        raise ValueError("No se encontró la versión del proyecto; usa --version.")
    return match.group(1)


def preflight(version, key_file):
    configuration = json.loads((ROOT / "desktopApp/packaging/updates/channel.json").read_text())
    repo = configuration["repository"]
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        raise ValueError("El repositorio configurado no es válido.")
    key_file = key_file.expanduser().resolve()
    if ROOT == key_file or ROOT in key_file.parents:
        raise ValueError("La clave de firma debe estar fuera del repositorio.")
    seed = base64.b64decode(key_file.read_text().strip(), validate=True)
    if public_key(seed) != configuration["publicKey"]:
        raise ValueError("La clave privada no corresponde a la clave pública de la aplicación.")
    key_file.chmod(0o600)
    release_notes(ROOT / "CHANGELOG.md", version)
    for file in (WORKFLOW, "scripts/updates/publish_appcast.py", "gradlew"):
        if not (ROOT / file).is_file():
            raise ValueError(f"Falta un archivo necesario: {file}")
    return repo, key_file


def snapshot(env, directory, version, login):
    """Use a temporary index: preserve the listener's working tree and staging area."""
    index_env = dict(env, GIT_INDEX_FILE=str(directory / "release.index"))
    git("read-tree", "HEAD", env=index_env)
    exclusions = [f":(exclude){pattern}" for pattern in EXCLUDED]
    if git("ls-files", env=index_env):
        git("add", "--update", "--", ".", *exclusions, env=index_env)
    source_names = (".github", "desktopApp", "desktopUi", "shared", "native", "gradle", "scripts", "docs", "web", "backend",
                    "Activar actualizaciones.command", "CHANGELOG.md", "README.md", "LICENSE", "COPYING", "Logo.png",
                    ".gitignore", ".gitattributes", ".editorconfig", "gradlew", "gradlew.bat", "gradle.properties",
                    "build.gradle.kts", "settings.gradle.kts", "CONTRIBUTING.md", "DESKTOP.md")
    sources = [name for name in source_names if (ROOT / name).exists()]
    if sources:
        git("add", "--all", "--", *sources, *exclusions, env=index_env)
    indexed = command(["git", "ls-files", "-z"], env=index_env).stdout.decode().split("\0")
    for name in filter(None, indexed):
        path = Path(name)
        if path.suffix.lower() in (".dmg", ".pkg", ".zip", ".apk", ".aab"):
            git("update-index", "--force-remove", "--", name, env=index_env)
            continue
        forbidden = path.name in ("local.properties", ".env") or path.suffix.lower() in (".p12", ".p8", ".pem", ".key", ".jks", ".keystore")
        forbidden |= path.name.startswith(".env.") and not path.name.endswith((".example", ".sample", ".template"))
        if forbidden:
            raise ValueError(f"La instantánea contiene un archivo privado: {name}. Retíralo del control de versiones.")
        absolute = ROOT / path
        if absolute.is_symlink() and not absolute.resolve().is_relative_to(ROOT):
            raise ValueError(f"El enlace {name} apunta fuera del proyecto.")
        if absolute.is_file() and absolute.stat().st_size > 95 * 1024 * 1024:
            raise ValueError(f"El archivo {name} supera el límite previsto para publicar código fuente.")
    tree = git("write-tree", env=index_env)
    # A source snapshot is a root commit, so publishing it does not upload unrelated local history.
    commit = git("-c", f"user.name={login}", "-c", f"user.email={login}@users.noreply.github.com",
                 "commit-tree", tree, env=index_env, data=f"MaviMusic {version}: código para publicación\n".encode())
    return tree, commit, f"mavimusic-release/{version}-{tree[:12]}"


class GitHub:
    def __init__(self, executable, env, repo):
        self.executable, self.env, self.repo = executable, env, repo

    def run(self, *args, data=None, live=False, timeout=None):
        return command([self.executable, *args], env=self.env, data=data, live=live, timeout=timeout)

    def api(self, endpoint, payload=None, missing=False):
        args = [self.executable, "api", "--hostname", "github.com", endpoint]
        if payload is not None:
            args += ["--method", "PUT", "--input", "-"]
        result = command(args, env=self.env, data=None if payload is None else json.dumps(payload).encode(), check=False)
        if result.returncode:
            error = result.stderr.decode(errors="replace")
            if missing and "HTTP 404" in error:
                return None
            raise RuntimeError(error.strip() or "GitHub rechazó la operación.")
        return json.loads(result.stdout)

    def release(self, tag):
        class Listing:
            def api(_self, endpoint):
                return self.repo_api(endpoint)
        return find_release(Listing(), tag)

    def repo_api(self, suffix, **kwargs):
        return self.api(f"repos/{self.repo}/{suffix}", **kwargs)


def prepare_source(hub, directory, version, login, initial_branch=None):
    tree, commit, branch = snapshot(hub.env, directory, version, login)
    if initial_branch:
        branch = initial_branch
        existing = None
    else:
        existing = hub.repo_api(f"git/ref/heads/{quote(branch, safe='/')}", missing=True)
    if existing:
        commit = existing["object"]["sha"]
        remote = hub.repo_api(f"git/commits/{commit}")
        if remote["tree"]["sha"] != tree:
            raise ValueError("La rama de publicación existente contiene otro código; usa una nueva versión.")
    else:
        helper = f"!{shlex.quote(hub.executable)} auth git-credential"
        command(["git", "-c", "credential.helper=", "-c", f"credential.helper={helper}",
                 "push", f"https://github.com/{hub.repo}.git", f"{commit}:refs/heads/{branch}"], env=hub.env, live=True)
    return commit, branch


def install_dispatcher(hub, default_branch):
    """Register the workflow on GitHub's default branch; execute it against the source snapshot."""
    current = hub.repo_api(f"contents/{WORKFLOW}?ref={quote(default_branch, safe='')}", missing=True)
    content = (ROOT / WORKFLOW).read_bytes()
    if current and base64.b64decode(current.get("content", "")) == content:
        return
    payload = {"message": "Configurar publicación firmada de MaviMusic", "branch": default_branch,
               "content": base64.b64encode(content).decode()}
    if current:
        payload["sha"] = current["sha"]
    hub.repo_api(f"contents/{WORKFLOW}", payload=payload)


def find_run(hub, branch, commit, previous_ids, attempts=60):
    endpoint = f"actions/workflows/release.yml/runs?event=workflow_dispatch&branch={quote(branch, safe='')}&per_page=30"
    for _ in range(attempts):
        runs = hub.repo_api(endpoint)["workflow_runs"]
        matches = [run for run in runs if run["id"] not in previous_ids and run["head_sha"] == commit]
        if len(matches) == 1:
            return matches[0]
        if len(matches) > 1:
            raise RuntimeError("Hay varias ejecuciones simultáneas; revísalas en Actions antes de publicar.")
        time.sleep(3)
    raise RuntimeError("GitHub no inició la compilación a tiempo. Revisa Actions; no se publicó la release.")


def verify_release(hub, directory, version, commit, key_file, allow_published=False):
    release = hub.release(f"v{version}")
    if release is None:
        raise ValueError("No existe una release para verificar.")
    if (not allow_published and not release["draft"]) or release.get("prerelease"):
        raise ValueError("El resultado debe ser un borrador estable antes de publicarlo.")
    if release.get("target_commitish") != commit:
        raise ValueError("La release no apunta a la instantánea que se compiló.")
    assets = {asset["name"]: asset for asset in release["assets"] if asset.get("state") == "uploaded" and asset.get("size", 0) > 0}
    for arch in ("arm64", "x64"):
        required = (f"appcast-{arch}.xml", f"MaviMusic-{version}-macos-{arch}.dmg")
        if not all(name in assets for name in required):
            raise ValueError(f"Faltan archivos de actualización para {arch}; se conserva el borrador.")
    hub.run("release", "download", f"v{version}", "--repo", hub.repo, "--pattern", "appcast-*.xml", "--dir", str(directory))
    signer = ROOT / "desktopApp/build/sparkle/bin/sign_update"
    if not signer.is_file():
        command(["python3", ROOT / "scripts/updates/prepare_sparkle.py", "--output", ROOT / "desktopApp/build/sparkle"], live=True)
    for arch in ("arm64", "x64"):
        feed = directory / f"appcast-{arch}.xml"
        command([signer, "--ed-key-file", key_file, "--verify", feed])
        item = ET.parse(feed).getroot().find("channel/item")
        expected = f"MaviMusic-{version}-macos-{arch}.dmg"
        enclosure = item.find("enclosure")
        if item.findtext(f"{{{NS}}}version") != version or enclosure.get("url") != f"https://github.com/{hub.repo}/releases/download/v{version}/{expected}":
            raise ValueError("El feed firmado contiene una versión o URL inesperada.")
        if int(enclosure.get("length", "0")) != assets[expected]["size"]:
            raise ValueError("El tamaño del paquete no coincide con el feed firmado.")
    return release


def save_failure_logs(hub, run_id):
    destination = ROOT / "build/github-diagnostics"
    destination.mkdir(parents=True, exist_ok=True)
    log = destination / f"release-{run_id}.log"
    result = hub.run("run", "view", str(run_id), "--repo", hub.repo, "--log-failed")
    log.write_bytes(result.stdout)
    log.chmod(0o600)
    print(f"Diagnóstico guardado: {log}", flush=True)
    return log


def finish_release(hub, run_id, directory, version, commit, key_file, timeout):
    try:
        hub.run("run", "watch", str(run_id), "--repo", hub.repo, "--exit-status", "--interval", "10", live=True, timeout=timeout * 60)
    except RuntimeError as error:
        try:
            log = save_failure_logs(hub, run_id)
        except (RuntimeError, OSError) as diagnostic_error:
            raise RuntimeError(f"La compilación falló. No se publicó la release. No se pudieron recuperar los registros: {diagnostic_error}") from error
        raise RuntimeError(f"La compilación falló. No se publicó la release. Revisa {log}") from error
    completed = hub.repo_api(f"actions/runs/{run_id}")
    if completed["conclusion"] != "success" or completed["head_sha"] != commit:
        raise ValueError("La compilación no terminó correctamente; no se publicará la release.")
    verified = verify_release(hub, directory, version, commit, key_file, allow_published=True)
    latest = hub.repo_api("releases/latest", missing=True)
    if not verified["draft"]:
        if latest and latest["tag_name"] == f"v{version}" and not latest["draft"]:
            return latest
        raise ValueError("La versión está publicada pero otra release ocupa el canal; no se modificará.")
    if latest and tuple(map(int, version_number(latest["tag_name"]).split('.'))) >= tuple(map(int, version.split('.'))):
        raise ValueError("Se publicó una versión igual o superior mientras se compilaba; se conserva este borrador.")
    hub.run("release", "edit", f"v{version}", "--repo", hub.repo, "--draft=false", "--latest")
    published = hub.repo_api("releases/latest")
    if published["tag_name"] != f"v{version}" or published["draft"]:
        raise RuntimeError("No se pudo confirmar la activación del canal de actualizaciones.")
    return published


def activate(args):
    if not (args.plan or getattr(args, "diagnose_run", None) or getattr(args, "publish", False)):
        raise ValueError("La publicación está pausada. Usa Actions → Release para pruebas internas; publicar requiere --publish explícito.")
    version = version_number(args.version or default_version())
    repo, key_file = preflight(version, args.key_file)
    print(f"MaviMusic {version} → https://github.com/{repo}", flush=True)
    print("Pasos: autorización en navegador → instantánea del código → secreto de firma → compilación y pruebas → publicación.", flush=True)
    if args.plan:
        print("Plan local verificado. No se abrió el navegador ni se modificó GitHub.")
        return
    if sys.platform != "darwin":
        raise ValueError("Este asistente se ejecuta desde macOS para verificar las firmas con Sparkle.")
    executable = shutil.which("gh")
    if not executable:
        brew = shutil.which("brew")
        if not brew:
            raise ValueError("Instala GitHub CLI desde https://cli.github.com y vuelve a ejecutar el asistente.")
        print("Instalando GitHub CLI con Homebrew…", flush=True)
        command([brew, "install", "gh"], live=True)
        executable = shutil.which("gh")
        if not executable:
            raise ValueError("No se encontró GitHub CLI después de instalarlo.")
    with tempfile.TemporaryDirectory(prefix="mavimusic-github-") as temporary:
        directory = Path(temporary)
        env = dict(os.environ)
        for name in ("GH_TOKEN", "GITHUB_TOKEN", "GH_ENTERPRISE_TOKEN", "GITHUB_ENTERPRISE_TOKEN", "GIT_INDEX_FILE",
                     "GIT_DIR", "GIT_WORK_TREE", "GH_DEBUG", "GIT_TRACE", "GIT_TRACE_CURL", "GIT_CURL_VERBOSE"):
            env.pop(name, None)
        config = directory / "auth"
        config.mkdir(mode=0o700)
        env.update(GH_CONFIG_DIR=str(config), GH_HOST="github.com", GH_NO_UPDATE_NOTIFIER="1", GIT_TERMINAL_PROMPT="0",
                   GIT_CONFIG_GLOBAL=str(directory / "gitconfig"))
        hub = GitHub(executable, env, repo)
        print("Autoriza GitHub CLI en tu navegador. La autorización se guarda en un archivo temporal privado que se elimina al terminar.", flush=True)
        hub.run("auth", "login", "--hostname", "github.com", "--web", "--clipboard", "--git-protocol", "https", "--skip-ssh-key",
                "--insecure-storage", "--scopes", "workflow", live=True)
        env["GH_PROMPT_DISABLED"] = "1"
        if getattr(args, "diagnose_run", None):
            save_failure_logs(hub, args.diagnose_run)
            return
        account = hub.api("user")
        repository = hub.api(f"repos/{repo}")
        if not repository.get("permissions", {}).get("admin"):
            raise ValueError(f"La cuenta {account['login']} necesita administrar {repo} para configurar las actualizaciones.")
        default_branch = repository["default_branch"]
        existing_default = hub.repo_api(f"branches/{quote(default_branch, safe='')}", missing=True)
        release = hub.release(f"v{version}")
        if release and not release["draft"]:
            raise ValueError(f"La versión {version} ya está publicada. Añade novedades y usa una versión superior.")
        latest = hub.repo_api("releases/latest", missing=True)
        if latest and tuple(map(int, version.split('.'))) <= tuple(map(int, version_number(latest['tag_name']).split('.'))):
            raise ValueError("La versión debe superar la última release publicada.")
        print(f"Cuenta: {account['login']}. Preparando código de publicación…", flush=True)
        commit, branch = prepare_source(hub, directory, version, account["login"], initial_branch=default_branch if existing_default is None else None)
        if release and release.get("target_commitish") != commit:
            raise ValueError("Ya existe un borrador de esta versión con otro código. Usa una versión nueva.")
        install_dispatcher(hub, repository["default_branch"])
        print("Configurando SPARKLE_PRIVATE_KEY…", flush=True)
        hub.run("secret", "set", "SPARKLE_PRIVATE_KEY", "--repo", repo, data=key_file.read_bytes())
        for _ in range(30):
            workflow = hub.repo_api("actions/workflows/release.yml", missing=True)
            if workflow:
                break
            time.sleep(2)
        else:
            raise RuntimeError("GitHub aún no registró el workflow. Vuelve a ejecutar el asistente.")
        if workflow["state"] != "active":
            hub.run("workflow", "enable", "release.yml", "--repo", repo)
        endpoint = f"actions/workflows/release.yml/runs?event=workflow_dispatch&branch={quote(branch, safe='')}&per_page=30"
        previous = {run["id"] for run in hub.repo_api(endpoint)["workflow_runs"]}
        hub.run("workflow", "run", "release.yml", "--repo", repo, "--ref", branch, "-f", f"version={version}", "-f", "publish=true")
        run = find_run(hub, branch, commit, previous)
        print(f"Compilación: {run['html_url']}\nPuede tardar varios minutos; esta ventana mostrará el progreso.", flush=True)
        published = finish_release(hub, run["id"], directory, version, commit, key_file, args.timeout)
        print(f"\nActualizaciones activadas: {published['html_url']}", flush=True)
        print("La autorización local temporal se elimina al cerrar este asistente.", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--publish", action="store_true", help="Autorizar explícitamente una publicación pública manual")
    parser.add_argument("--version", help="Versión estable; por defecto usa la del proyecto")
    parser.add_argument("--key-file", type=Path, default=DEFAULT_KEY)
    parser.add_argument("--plan", action="store_true", help="Validar la configuración sin abrir el navegador ni modificar GitHub")
    parser.add_argument("--diagnose-run", type=int, help="Recuperar los errores de una ejecución sin compilar ni publicar")
    parser.add_argument("--timeout", type=int, default=120, help="Máximo de minutos de espera de GitHub Actions")
    try:
        activate(parser.parse_args())
    except (ValueError, RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print(f"\nNo se completó la activación: {error}", file=sys.stderr)
        print("Puedes corregir el problema y volver a ejecutar el asistente. Las releases publicadas se conservan.", file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nAsistente detenido. Una compilación ya iniciada en GitHub puede seguir ejecutándose.", file=sys.stderr)
        sys.exit(130)
