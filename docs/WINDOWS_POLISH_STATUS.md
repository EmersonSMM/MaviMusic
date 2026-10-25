# Estado de la pasada Windows / macOS — 1.8.52

Base: main `683a2617` (1.8.51). Rama: `codex/windows-polish-1.8.52`.
Los nueve puntos se pueden revertir individualmente. No se modifican motor de audio, Automix ni seek.
El commit previo `88bf2183` incorpora diagnóstico privado y publicación verificada en borrador.

## Validación

- Antes: suites `:shared:jvmTest :desktopUi:jvmTest :desktopApp:test` completas, BUILD SUCCESSFUL; herramientas Python: 23 pruebas correctas.
- Después: mismas suites completas, 638 pruebas declaradas, cero errores/fallos y dos omisiones condicionadas al sistema (636 ejecutadas). Python: 23 correctas.
- Gradle usa PreferencesFactory en memoria en pruebas; no se abrió la app con el perfil personal.
- Puente nativo Sparkle compilado localmente con clang (buildUpdateBridge).
- Workflows revisados con actionlint; Windows CI prueba además registro/liberación real del atajo y aceptación de los atributos de color DWM; construye un MSI y verifica su secuencia de rollback, analiza PowerShell y ejecuta una prueba HWND de fullscreen/restauración. Estos controles no equivalen a actualizar una instalación real del usuario.
- No se midieron todavía CPU/RAM en reposo ni tiempo de arranque con instalaciones empaquetadas. Por tanto, el criterio de no regresión de rendimiento queda sin acreditar, aunque los nuevos listeners son bloqueantes y las comprobaciones están suspendidas ocho horas.

## Pendientes explícitos y límites de aceptación

1. Actualización real Windows entre dos MSI por usuario, incluyendo archivos bloqueados, fallo inyectado, reinicio visible/oculto, instalación en ruta personalizada y preservación comprobada de datos. La secuencia transaccional se corrige en el MSI nuevo; no modifica instaladores antiguos ya publicados. Si MSI excede diez minutos, se conserva su transacción y registro y se requiere recuperación manual. Portable e instalación por máquina conservan actualización manual.
2. Actualización real Sparkle entre dos versiones macOS firmadas y comprobación del aviso suave. Se conserva su mecanismo nativo.
3. Revisión visual física de estados del botón, resultados parciales en bloques personalizados de Ajustes, orden Escape con distintos popups, y menús sobre fondos distintos. La app existente usa tema oscuro global; no se añade un tema claro global. Los componentes compartidos usan colores semánticos y contraste.
4. Blur entre ventanas Popup depende del backend; si no captura el fondo se conserva tinte legible. No se afirma blur nativo donde Haze no lo pueda ofrecer.
5. Atajo global: probar conflictos y apertura externa en un escritorio Windows real. macOS mantiene Carbon con ⌘K/⌘⇧K; falta configuración y aviso de conflicto de Carbon.
6. Windows 10 antiguos que no admiten atributos DWM: falta una barra propia con equivalencia completa de Snap/resize/menú del sistema. Se conservan los controles nativos para evitar regresiones.
7. Cambiar «reducir movimiento» con la vista ya compuesta no actualiza inmediatamente el hover del logo; se lee al crear la composición.
8. Medición comparativa de arranque y CPU/RAM en reposo, y aceptación física de pantalla completa/escalado de 100–200 % en Windows.

No se publica una release estable desde esta rama mientras la validación de instalación permanezca pendiente. La PR permite revisar y probar sin reemplazar la versión distribuida.

## Resumen por punto

### fix(updates): verify MSI and hand off silent per-user updates [1]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `.github/workflows/check.yml`
- `.github/workflows/release.yml`
- `desktopApp/build.gradle.kts`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopWindowVisibility.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopWindowsUpdater.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/Main.kt`
- `desktopApp/src/main/resources/updates/windows-update.ps1`
- `desktopApp/src/test/kotlin/com/music/mavimusic/desktop/DesktopWindowsUpdateIntegrityTest.kt`
- `scripts/updates/activate_github.py`
- `scripts/updates/prepare_windows_msi.ps1`
- `scripts/updates/test_activation.py`

### fix(ui): restore update button contrast in every state [2]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSettingsScreen.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopUpdateAction.kt`
- `desktopApp/src/test/kotlin/com/music/mavimusic/desktop/DesktopUpdateActionTest.kt`

### feat(updates): add quiet periodic checks and availability pill [3]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `desktopApp/build.gradle.kts`
- `desktopApp/native/mac/updates.m`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopMacUpdater.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopNavigationChrome.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSettingsScreen.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopUpdateNotice.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopWindowsUpdater.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/Main.kt`

### fix(settings): remove empty filtered cards and highlight matches [4]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSettingsScreen.kt`

### fix(lyrics): prioritize Escape and enlarge close targets [5]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopApp.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopImmersiveLyricsScreen.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopLyricsClose.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSidePanel.kt`

### style(menus): unify rounded glass context and dropdown surfaces [6]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopCollectionPage.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopGlassMenu.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopNavigationChrome.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSettingsScreen.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSharedPages.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSongMenuUi.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopTopMiniPlayer.kt`

### style(brand): replace fire loop with cached static vector wordmark [7]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopMotionPreference.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopNavigationChrome.kt`

### feat(windows): register configurable global Spotlight shortcut [8]

Commit identificado por el número al final del título. Archivos (más DECISIONS.md):

- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopSettingsScreen.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopShortcutSettings.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopWindowsHotkey.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/Main.kt`
- `desktopApp/src/test/kotlin/com/music/mavimusic/desktop/DesktopHotkeyTest.kt`
- `desktopApp/src/test/kotlin/com/music/mavimusic/desktop/DesktopWindowsWindowIntegrationTest.kt`

### fix(windows): theme native title bar with DWM while preserving Snap [9]

Commit: último commit de esta PR. Archivos (más DECISIONS.md):

- `CHANGELOG.md`
- `desktopApp/build.gradle.kts`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopApp.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopWindowsCaption.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/DesktopWindowsFrame.kt`
- `desktopApp/src/main/kotlin/com/music/mavimusic/desktop/Main.kt`
- `desktopApp/src/test/kotlin/com/music/mavimusic/desktop/DesktopCaptionTest.kt`
- `desktopApp/src/test/kotlin/com/music/mavimusic/desktop/DesktopWindowsWindowIntegrationTest.kt`
- `docs/WINDOWS_POLISH_STATUS.md`

