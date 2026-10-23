# Actualizaciones de MaviMusic para macOS

La versión 1.8.36 incorpora Sparkle 2.10.0. La ventana nativa presenta las novedades, descarga la app, verifica firmas e instala al reiniciar o salir. En Ajustes → Actualizaciones se controla la búsqueda diaria y la descarga automática. Cerrar la ventana con la opción de bandeja activa no equivale a salir de la aplicación.

## Activación automatizada desde el navegador

En macOS, abre con doble clic **`Activar actualizaciones.command`** en la carpeta del proyecto. También puedes ejecutarlo en Terminal:

```sh
./"Activar actualizaciones.command"
```

El asistente instala GitHub CLI mediante Homebrew si hace falta y abre la autorización oficial en tu navegador habitual. Aprovecha la sesión de GitHub que ya tengas iniciada: pega el código de un solo uso que GitHub CLI copia al portapapeles y aprueba la autorización. Se solicitan los permisos para administrar este repositorio y sus workflows. El script no lee perfiles ni cookies del navegador.

Después realiza automáticamente estos pasos:

1. Comprueba la clave de firma, la versión y sus novedades.
2. Prepara el código actual con un índice temporal de Git y sube una instantánea a una rama `mavimusic-release/…`. Conserva el índice y los archivos de tu checkout. Excluye los instaladores y archivos privados conocidos de la instantánea; los demás archivos de código y recursos dentro de los directorios del proyecto se publicarán.
3. Registra el workflow de release en la rama predeterminada de GitHub, actualizando únicamente `.github/workflows/release.yml` allí. En un repositorio vacío, crea primero su rama inicial con el código.
4. Configura `SPARKLE_PRIVATE_KEY`, ejecuta el workflow sobre la instantánea y muestra el progreso de las compilaciones.
5. Si las pruebas pasan, verifica las firmas de los feeds descargados, sus versiones, enlaces y tamaños de paquetes. Publica el borrador y confirma que queda como la última release.

La autorización se guarda en una carpeta temporal privada que se elimina al terminar. GitHub CLI se configura con `--insecure-storage` para guardar esta autorización en el archivo temporal, en lugar de almacenarla en el llavero. Sus configuraciones globales y las de Git se aíslan durante esta ejecución. La clave de firma solo se transmite a **Actions Secrets del repositorio indicado en `channel.json`**. La autorización OAuth concedida a GitHub CLI puede administrarse después desde GitHub Settings → Applications.

Si GitHub exige verificación de identidad, la completas en su navegador. Si la rama predeterminada está protegida o faltan permisos, se detiene con el error de GitHub; no desactiva esas protecciones. Una compilación fallida conserva la release sin publicar. No reemplaza una versión que ya esté publicada.

Para comprobar solo la configuración local, sin instalar herramientas ni modificar GitHub:

```sh
python3 scripts/updates/activate_github.py --plan
```

Para la próxima versión, añade su sección al changelog y ejecuta:

```sh
./"Activar actualizaciones.command" --version 1.8.37
```

La compilación de las plataformas puede tardar varios minutos. Si detienes el asistente, una ejecución de GitHub Actions ya iniciada puede seguir generando su borrador; el asistente no publicará ese borrador mientras esté detenido.

## Primera configuración en GitHub

El repositorio público es `EmersonSMM/MaviMusic`. Las distribuciones están en GitHub Releases; no se necesita un servidor adicional ni tokens en la app.

1. En **Settings → Secrets and variables → Actions**, crea el secreto `SPARKLE_PRIVATE_KEY` con el contenido del archivo privado local `~/.config/mavimusic/release-signing/ed25519.key`. El archivo está fuera del repositorio, tiene permisos 0600 y contiene una semilla Ed25519 codificada en base64. Cópialo mediante un gestor de secretos, conserva una copia segura y no lo incluyas en commits, issues ni mensajes.
2. La clave pública correspondiente ya está en `desktopApp/packaging/updates/channel.json` y en el Info.plist generado. No generes otra clave para cada versión: los clientes instalados confían en esta clave pública.
3. Instala manualmente una versión que incluya el actualizador (1.8.36 o posterior). Las versiones anteriores no pueden actualizarse solas.
4. Publica la primera versión con los feeds firmados para activar el canal. Mientras no exista una release pública, GitHub devuelve 404 al consultar `releases/latest`.

No se ha cargado ningún secreto ni publicado ninguna release desde el entorno de desarrollo. La instalación y la comprobación automáticas se validan de extremo a extremo al publicar una segunda versión de prueba en un repositorio/canal aislado.

### Developer ID y notarización de Apple

La firma Ed25519 de Sparkle se configura siempre y es obligatoria. La firma Developer ID y la notarización se activan en CI al configurar:

| Secreto | Contenido |
| --- | --- |
| `MACOS_SIGN_IDENTITY` | Identidad completa `Developer ID Application: … (TEAMID)` |
| `MACOS_CERTIFICATE_P12` | Certificado y clave privada exportados a P12 y codificados en base64 |
| `MACOS_CERTIFICATE_PASSWORD` | Contraseña del P12 |
| `APPLE_API_KEY_P8` | Contenido de una clave API de App Store Connect autorizada para notarización |
| `APPLE_API_KEY_ID` | Identificador de esa clave |
| `APPLE_API_ISSUER` | Identificador del emisor |

CI importa el certificado solo en un llavero temporal del runner y lo elimina al terminar. La app de los usuarios no consulta el llavero para verificar actualizaciones. Sin esos secretos de Apple se genera una distribución con firma ad hoc y firmas Ed25519 de actualización; no se presenta como una app notarizada.

## Publicar una versión

1. Añade una sección `## 1.8.37` (por ejemplo) a `CHANGELOG.md` con sus novedades.
2. Actualiza la versión predeterminada del código y crea el tag `v1.8.37`, o ejecuta **Actions → Release → Run workflow** indicando `1.8.37`. El commit de publicación debe contener todos los cambios de la versión.
3. El workflow compila y prueba las plataformas. En macOS incluye Sparkle, firma, notariza si hay credenciales y produce los ZIP/DMG y los feeds firmados.
4. GitHub crea un **borrador** de release con las novedades y archivos. Revisa y publica el borrador. Esto activa conjuntamente los archivos y el feed de la nueva versión; no se actualiza un feed público antes de que exista su paquete.

Los tags y versiones del canal estable usan `MAJOR.MINOR.PATCH`; no se publican betas en este canal. `CFBundleVersion` coincide con la versión del tag: no depende de un contador manual que pudiera quedarse atrás.

## Archivos y garantías

- `appcast-arm64.xml`: Apple Silicon; `appcast-x64.xml`: Intel. Cada app consulta solo su canal mediante `releases/latest/download/…`.
- El feed contiene novedades Markdown, versión, requisitos de macOS, enlace al ZIP y firma del ZIP. El feed completo está firmado: las novedades y los enlaces quedan autenticados.
- Se comprueban la identidad de la app, ambas versiones del bundle, la arquitectura real del ejecutable, la clave pública y las firmas antes de publicar. Una clave privada incorrecta hace fallar la compilación de la release.
- Los paquetes se sirven desde URLs de versión inmutables. No se deben reemplazar los archivos de una versión publicada; publica una versión superior.
- Sparkle verifica antes de extraer. El framework se descarga con SHA-256 fijado, se conserva con sus enlaces simbólicos y se firma el bundle después de incluirlo.
- Al salir y antes del reinicio solicitado por Sparkle se guardan cola y posición. La canción se restaura pausada para no comenzar a sonar de improviso.
- No se cambia el formato de cuentas ni se eliminan preferencias, descargas o archivos locales al actualizar.

## Verificación local

```sh
./gradlew :shared:jvmTest :desktopUi:jvmTest :desktopApp:test :desktopApp:packageDmg
SPARKLE_SIGNER="$PWD/desktopApp/build/sparkle/bin/sign_update" \
  python3 -m unittest discover -s scripts/updates -p 'test_*.py' -v
```

Para crear manualmente un feed firmado de una compilación local:

```sh
ditto -c -k --sequesterRsrc --keepParent \
  desktopApp/build/compose/binaries/main/app/MaviMusic.app \
  /tmp/MaviMusic-1.8.36-macos-arm64.zip
python3 scripts/updates/publish_appcast.py \
  --archive /tmp/MaviMusic-1.8.36-macos-arm64.zip --version 1.8.36 --arch arm64 \
  --output /tmp/appcast-arm64.xml --signer desktopApp/build/sparkle/bin/sign_update
```

Se usa el archivo privado local explícito, nunca el llavero. Los tests de firma modifican el archivo/feed y comprueban que la verificación los rechaza. No se prueba una actualización sobre la instalación o las preferencias reales del usuario.

Referencias: [Sparkle](https://sparkle-project.org/documentation/), [publicación y firmas](https://sparkle-project.org/documentation/publishing/), [GitHub Releases](https://docs.github.com/en/rest/releases/releases).
