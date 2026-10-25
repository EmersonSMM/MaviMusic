# Relevo de trabajo: Windows, base 1.8.48

Fecha: 25 de octubre de 2026.

## Base y alcance

- Repositorio: https://github.com/EmersonSMM/MaviMusic
- Tag base: `v1.8.48`.
- Commit base: `41d66348f688b21ee37bb6ce0b090d9755b0547d`.
- Objetivo: corregir adaptación a pantalla, DPI, controles ocultos por la barra de tareas y rendimiento interactivo de Windows, conservando funciones y comportamiento de Mac.
- Estado: correcciones implementadas en la rama `codex/windows-display-1.8.48`, versión propuesta 1.8.49. CI y validación nativa pendientes.
- El usuario trabaja con otros agentes. Identificar rama, commit y archivos en curso antes de integrar cambios.

El checkout anterior del Mac conserva numerosos cambios sin consolidar y parte de una base antigua. Se creó otro worktree desde el tag publicado; no se reemplazó el checkout anterior. La rama de trabajo parte del commit indicado; no mover ni reutilizar el tag de release.

## Evidencia disponible

- Release publicada con EXE, MSI y ZIP portable de Windows; paquetes de Linux y Mac.
- Sparkle para macOS tiene feeds publicados para arm64 y x64. No se verificó una actualización instalada de extremo a extremo durante este barrido.
- Release: https://github.com/EmersonSMM/MaviMusic/actions/runs/40210242991 (success).
- Check del mismo commit: https://github.com/EmersonSMM/MaviMusic/actions/runs/40210240416 (Linux falló en Tests; Windows, Mac y backend pasaron).
- Check Linux usa Gradle directamente; Release Linux usa `xvfb-run`. La diferencia es relevante, pero falta confirmar la causa concreta con registros.
- Release permite publicar con Mac y Windows correctos aunque falle Linux. Evitar interpretar publicación como validación completa de todas las plataformas.

## Problemas que deben investigarse

1. `Main.kt`: mínimo de 1024 × 768, multiplicación manual por `defaultTransform` y listener que fuerza ese mínimo. No se limita al área útil. Comprobar las unidades lógicas de AWT frente a píxeles físicos antes de corregir DPI. Incluso al 100 %, una altura de 768 no deja espacio a bordes y barra de tareas en una pantalla de 768 píxeles.
2. `DesktopPlatform.kt`: `drawsOwnWindowFrame = false` desactiva el puente de marco Windows.
3. `DesktopWindowsFrame.kt` y `DesktopWindowMode.kt`: el fullscreen nativo depende de ese puente; cuando no está instalado se usa `WindowPlacement.Fullscreen`. Los comentarios históricos advierten de problemas de esa ruta. Es una hipótesis fundada para el síntoma de controles ocultos, pendiente de reproducción real.
4. Al entrar y salir de fullscreen deben conservarse monitor, posición, tamaño y estado maximizado previos. Distinguir área útil de maximización y límites completos del monitor en fullscreen.
5. `DesktopImmersiveLyricsScreen.kt`: el bucle del visualizador sigue animando pausado; revisar suspensión efectiva al minimizar/ocultar y medir carga real. Configurar Direct3D no demuestra 60–120 FPS.
6. Revisar diseños compactos: controles esenciales visibles antes de exigir ventanas grandes. Comprobar barra inferior, búsqueda, sidebar, cola y modo inmersivo.

## Estrategia entre Mac y Windows

- Mac: implementación, pruebas de cálculo de geometría, revisión de regresiones y preparación de rama/PR.
- GitHub Actions: compilación y pruebas JVM en Windows. Una PR activa Check sin publicar una release.
- Windows real: ejecutar la distribución y validar geometría, foco, tarea de pantalla completa y fluidez. Para estos síntomas usar la app Windows nativa; WSL ejercita otro entorno de ventanas.
- Si hay otro PC, clonar desde GitHub y comprobar la misma rama/commit. No copiar `build/`, `.gradle/`, credenciales, cookies ni claves de firma desde el Mac.
- Este documento y `AGENTS.md` son inicialmente archivos locales del nuevo worktree. Para que lleguen mediante `git clone`, deben incorporarse a la rama compartida; clonar hoy el tag original no los incluye.

## Matriz de aceptación pendiente

- 1366 × 768 y 1920 × 1080, más 4K si está disponible.
- Escalado 100 %, 125 %, 150 % y 200 %.
- Ventana normal, maximizada, fullscreen y restauración; repetir entradas/salidas.
- Barra de tareas visible y autooculta; foco/Alt-Tab y monitor secundario.
- Mover entre monitores con distinto DPI y desconectar el monitor externo.
- Controles de reproducción accesibles; interfaz sin cortes ni solapamientos.
- CPU/GPU y tiempos de fotograma: reproducción normal, pausa, visualizador y minimización. Registrar equipo, controlador, resolución y escala.
- Reproducción, seek, Automix, cola sin duplicados automáticos, descarga MP3 con carátula/etiquetas y archivos locales siguen funcionando.

## Comandos de desarrollo

JDK 21. Windows PowerShell:

```powershell
.\gradlew.bat :shared:jvmTest :desktopUi:jvmTest :desktopApp:test
.\gradlew.bat :desktopApp:createDistributable
```

La ejecución interactiva debe usar un perfil de prueba aislado. No asumir que `java.util.prefs.userRoot` basta para aislar preferencias en todos los sistemas. Las pruebas JVM ya configuran `MemoryPreferencesFactory`.

Para producir instaladores Windows seguir `DESKTOP.md` (WiX y herramientas nativas). No hacen falta credenciales de firma de Mac para probar la interfaz de Windows.

## Próximo informe de agente

Registrar: commit/branch base, cambios y archivos, pruebas ejecutadas y resultados, evidencias visuales si existen, limitaciones y siguiente paso. Separar observaciones de hipótesis. No declarar el problema visual resuelto solo por pasar CI.

Prompt de continuidad:

> Continúa la adaptación de MaviMusic para Windows sobre la base 1.8.48. Lee AGENTS.md y docs/WINDOWS_HANDOFF.md. Confirma rama y commit, conserva todas las funciones y coordina los cambios con los otros agentes. Prioriza DPI, área útil, controles y fullscreen. Indica qué has probado en Windows real y qué queda pendiente.

## Cambios de esta rama

- `DesktopWindowsWindowLayout.kt` calcula límites en coordenadas lógicas AWT, limita tamaño y posición al área útil y reevalúa cambios de monitor mediante eventos con debounce. Solo se instala en Windows.
- El puente Win32 admite el marco estándar, no añade overscan y restaura `WINDOWPLACEMENT`; mantiene la ventana sobre la barra solo mientras está activa.
- `DesktopWindowMode` conserva el estado previo y utiliza maximización como fallback seguro si falta el puente en Windows.
- El visualizador Windows suspende su bucle al pausar, minimizar y ocultar. No se modifican las condiciones de animación de Mac.
- `windowsWindowSmoke` ejecuta una JVM Windows separada con un JFrame de prueba: caption nativa, tres ciclos fullscreen, límites/insets y restauración de maximizado. No inicia MaviMusic ni su perfil real.
- Check Linux instala dependencias de audio/display y usa Xvfb como Release. El fallo anterior estaba en la carga nativa de FFmpeg.
- Validación local Mac: 627 pruebas JVM aprobadas, 2 omitidas (llavero y prueba HWND Windows opt-in). Sin validación visual en hardware Windows.

- Barra inferior Windows compacta (menos de 1800 dp): altura de 110 dp y controles auxiliares con FlowRow; conserva todas las acciones y el slider de volumen. Mac conserva su disposición anterior.
