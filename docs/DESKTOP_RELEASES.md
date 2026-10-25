# Instaladores y pruebas internas

La publicación automática está pausada. Un push, commit, merge o tag no lanza Release.
`Check` conserva las pruebas JVM de shared, desktopUi y desktopApp en Windows/macOS,
la prueba nativa de ventana/atajo/DWM en Windows, el puente Sparkle y las herramientas de
firma en macOS, y las pruebas del backend Go. No empaqueta instaladores en cada commit.
Linux de escritorio queda temporalmente fuera de CI y distribución; su código se conserva.

## Construcción manual para probar

Release queda además deshabilitado temporalmente en GitHub para impedir ejecuciones de
workflows antiguos desde tags o ramas previas. Al pedir una compilación interna, habilitarlo
manualmente con la definición actual de main y volver a pausarlo al terminar. Check sigue activo.

En GitHub: **Actions → Release → Run workflow**. Indicar una versión con sección en
CHANGELOG.md y dejar `publish` desmarcado. Por defecto construye Windows x64 y macOS
Apple Silicon. Marcar `intel` para incluir macOS Intel. Los instaladores se descargan
como artefactos de esa ejecución: no se crea tag, borrador ni release pública y no se activa
el canal de actualización.

También se puede lanzar manualmente:

```sh
gh workflow enable release.yml
gh workflow run release.yml --ref main -f version=1.8.52 -f publish=false -f intel=false
```

Formatos actuales:

| Plataforma | Instalador |
| --- | --- |
| Windows x64 | `MaviMusic-<versión>-windows-x64.msi` |
| macOS Apple Silicon | `MaviMusic-<versión>-macos-arm64.dmg` |
| macOS Intel (opcional en pruebas internas) | `MaviMusic-<versión>-macos-x64.dmg` |

Las pruebas internas no generan EXE, PKG, portables ZIP, AppImage, DEB ni RPM.
Al publicar se incluye además `MaviMusic-<versión>-windows-x64-setup.exe`: es un instalador,
no un portable, y lo necesitan los clientes Windows 1.8.51 que solo saben descargar EXE.
El MSI conserva el asistente inicial y sirve para las actualizaciones silenciosas posteriores.
El DMG contiene la app y sirve tanto para instalar como para la actualización nativa de Sparkle.
Se mantiene el formato histórico ZIP únicamente para verificar archivos ya publicados.

## Publicación pública explícita

Solo cuando el usuario pida publicar: elegir una versión nueva MAJOR.MINOR.PATCH y añadir
sus novedades, ajustar la versión predeterminada y lanzar Release marcando `publish`.
Esa opción incluye Intel automáticamente, aunque `intel` esté desmarcado. El publicador
exige cuatro instaladores (MSI, EXE y dos DMG), dos feeds de Sparkle y SHA256SUMS: siete archivos en total.
Comprueba firmas, identidad/versión/arquitectura del bundle, hashes, commit y que no se
reemplace una versión más reciente. Sube primero a borrador y lo publica tras verificarlos.

El asistente local `Activar actualizaciones.command` requiere ahora `--publish` explícito;
sin esa opción no autentica, sube fuentes, configura secretos ni publica. `--plan` y
`--diagnose-run` siguen disponibles sin publicar.

Las versiones ya distribuidas (incluida 1.8.52) y sus archivos se conservan intactos.
No se cambian la clave pública de Sparkle, la identidad de la app, el upgradeUuid de MSI
ni los datos de los usuarios. Una futura reactivación de Linux deberá restaurar también
su CI, archivos exigidos, documentación y validaciones antes de publicarlo.

## Comprobación local

```sh
./gradlew :shared:jvmTest :desktopUi:jvmTest :desktopApp:test
./gradlew :desktopApp:packageDmg       # macOS
# gradlew.bat :desktopApp:packageMsi   # Windows, JDK 21 y WiX 3
SPARKLE_SIGNER="$PWD/desktopApp/build/sparkle/bin/sign_update" \
  python3 -m unittest discover -s scripts/updates -p 'test_*.py'
```

El test DMG monta una imagen sintética de solo lectura, verifica y firma el feed con una
clave temporal y la desmonta; no abre MaviMusic ni usa una cuenta o biblioteca real.
Compilar y pasar CI no acredita el escalado, fluidez o ciclo completo de actualización
entre instalaciones en un PC físico.
