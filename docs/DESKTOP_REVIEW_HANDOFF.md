# Prueba local de escritorio: Inicio, cola y videoclip

Base: `8d4929d6`, rama `codex/android-stability-stage-1`. Cambios locales preparados para revisión del usuario; sin commit, push, tags ni publicación. Android fuera del alcance.

## Cambios

- `DesktopApp.kt`: las colecciones conservan su orden original antes de activar aleatorio. Radios con consulta correcta por artista, aviso de carga/error, cancelación y comprobación de intención antes de reemplazar la cola. Inicio cancela cargas antiguas y calcula una sola vez las estadísticas fuera del hilo de interfaz.
- `DesktopHomeRecommendations.kt`: «Música sugerida» intercala playlists y álbumes de hasta tres artistas, ponderando historial reciente, artistas frecuentes y favoritos disponibles. Máximo seis búsquedas y doce tarjetas, caché de sesión de treinta minutos. Se conservan las tarjetas existentes y «Álbumes para ti» utiliza los mismos resultados.
- `DesktopQueue.kt` / `DesktopTrackSimilarity.kt`: el contexto de una colección conserva su orden y sus remixes, pero elimina repeticiones de una grabación como audio/videoclip. Las entradas pedidas expresamente por el usuario se conservan. Se evita repetir también cuando ambas representaciones terminan resolviéndose al mismo identificador.
- `DesktopMusicVideo.kt`: selección de videoclip con comprobación de artista/grabación, exclusión de letras/visualizadores y versiones distintas, imagen sincronizada con el reloj de audio, transporte y volumen. Los cambios de versión conservan identidad y procedencia de la entrada de cola.
- `DesktopImmersiveLyricsScreen.kt`: opción persistente «Vídeo automático». Al entrar se utiliza el videoclip disponible; si no hay coincidencia o falla se conserva/recupera el visualizador. Al salir se recupera la canción original en la posición aproximada, sin cambiar la preferencia global de audio.
- `StreamResolver.kt`: nueva extracción de flujos de imagen y audio del mismo vídeo, limitada a 720p. No modifica la resolución de audio existente ni añade dependencias.
- `DesktopCanvasDecoder.kt` / `DesktopAudioEngine.kt`: seek y timestamps para vídeos largos sin bucle; limpieza de recursos nativos; audio directo de la misma versión, sin sustitución por otra fuente. Durante vídeo se suspenden temporalmente Automix, crossfade, salto de silencios y smart trim, conservando las preferencias. El modo automático no cambia la reproducción de Listen Together.

## Validación

Las pruebas JVM utilizan preferencias en memoria. No se abrió la aplicación sobre una sesión, biblioteca o llavero reales.

- Nuevas pruebas de recomendaciones: historial vacío, mezcla, límites, caché y ponderación.
- Nuevas pruebas de cola/versiones: audio/vídeo en colección, conversión al mismo ID, remixes, petición manual, identidad y restauración del orden.
- Vídeo sintético propio de tres segundos en `desktopApp/src/test/resources/video/sync-test.mp4`: decodificación, timestamps, seek, EOF sin bucle y cierre repetido. Generado con FFmpeg `testsrc2`, 96×64, 10 fps, MPEG-4; no contiene contenido de terceros.
- Prueba de red opt-in `DesktopMusicVideoNetworkTest`: videoclip público, decodificación real de imagen, seek y audio sin altavoces ni ventana. Pasó localmente. Activación: `MAVIMUSIC_TEST_VIDEO_NETWORK=1 JAVA_HOME=/Users/emersonsmm/.jdks/temurin-21/Contents/Home ./gradlew :desktopApp:test --tests '*DesktopMusicVideoNetworkTest'`. Se omite en la batería normal.
- Batería completa: 103 pruebas shared, 42 desktopUi y 516 desktopApp; 658 aprobadas, 3 omitidas (llavero, ventana Windows y red opt-in), sin fallos. La prueba de red se ejecutó por separado y pasó.
- Informe final de pruebas y artefacto en `output/desktop-review/` (carpeta ignorada por Git).

## Prueba manual pendiente

1. Inicio: conservar las tarjetas anteriores y ver playlists y álbumes en «Música sugerida» con historial disponible.
2. Álbum/playlist: activar aleatorio y apagarlo; comprobar el orden restante. Avanzar sobre una pareja audio/videoclip y un remix; comprobar que solo se evita la repetición redundante. Añadir una repetición manual y comprobar que se respeta.
3. Radio: iniciar una tarjeta y elegir otra canción mientras carga; la radio pendiente debe descartarse. Comprobar errores y radios de artistas que solo existan en estadísticas.
4. Pantalla completa: canción con videoclip, pausa/reanudar, seek, volumen, siguiente/anterior, desactivar «Vídeo automático», cerrar/reabrir y minimizar. Sin videoclip, archivos locales o fallo de red deben mantener una reproducción utilizable.
5. macOS: validación visual por el usuario. Windows: pendiente de compilación y prueba nativa; no se modificaron las reglas específicas de DPI, geometría, fullscreen ni barra de tareas.

Las versiones de álbum y videoclip pueden tener introducciones o cortes diferentes: se conserva el tiempo aproximado, no una alineación musical entre ediciones. El vídeo necesita red y una coincidencia/flujo accesible; no se descarga íntegro ni se guarda offline. No se midieron FPS o consumo en hardware Windows.


## Segunda revisión: inmersión y recuperación

Petición del usuario: pulir vídeo, switch Audio/Vídeo, sugerencias, recuperación y rendimiento. Se conserva la misma base Git y todos los cambios siguen sin commit/push.

- La cabecera inmersiva ofrece selección explícita Audio/Vídeo y muestra título/artista del videoclip. Menú de modos 3D, paletas y ajustes retro oculto durante vídeo y búsqueda; sus popups se cierran al entrar. En Audio reaparece el visualizador. Cierre blanco visible sobre el fondo oscuro, tiempos de reproducción y controles de volumen/transporte.
- Vídeo suspendido con `snapshotFlow` al pausar/minimizar: sin bucle de sondeo mientras no haya motivo para dibujar. El seek pausado solicita un solo fotograma; el estado de carga evita mostrar un fotograma del inicio mientras el audio aún está abriendo en otra posición. Los visualizadores de audio también suspenden el reloj en pausa/minimización en Mac y Windows.
- Sugerencias con botón «Actualizar sugerencias», estado de carga y actualización solo de las secciones personalizadas. Menú de tres puntos / clic derecho permite «No me interesa»; oculta la colección y reduce el peso del artista, con preferencias locales acotadas. El historial permanece intacto. La caché respeta exclusiones y el refresco rota candidatos disponibles.
- Recuperación: errores de lectura distintos de EOF se propagan desde FFmpeg y DecodeAhead. Un fin remoto prematuro se diferencia del fin normal; no se aplica esa regla al tramo de una mezcla Automix. Se conserva canción/cola/posición, se renueva solo su URL y se permiten dos reintentos automáticos (1 s y 3 s). Se evita reutilizar caché fallida, se respeta pausa/cambio de canción/Listen Together y el botón Reproducir permite un intento manual nuevo. FFmpeg limita también sus reconexiones internas. Cargas y fallos conservan la posición solicitada; la intención de pausa durante carga se respeta al arrancar.
- Pruebas nuevas: exclusiones persistentes y ponderación, refresco/caché, errores después del audio ya bufferizado, límites de recuperación/EOF, gating de vídeo y dos escenas Compose aisladas. La escena de vídeo verifica ausencia de controles de efectos y activa Audio mediante su acción semántica; la de Inicio verifica refresco y menú de colección. Capturas inspeccionadas visualmente en `output/desktop-review-v2/`.
- Medición aislada con FFmpeg: clips sintéticos de vídeo 720p/30 fps y audio estéreo, sin altavoces ni sesión. `media-profile.json` informa tiempo de CPU/hilo y pared de decodificación, más dos lecturas de heap. No equivale a consumo total de la app, RSS, GPU o FPS interactivos. Activación: `MAVIMUSIC_PROFILE_MEDIA=1`; requiere los archivos sintéticos `profile-720p.mp4` y `profile-audio.wav` en la carpeta de salida.
- `desktopUi/build.gradle.kts` respeta el destino Windows explícito para usar Compose 1.10.3 también al comprobar compilación desde Mac. Compilación de fuentes y pruebas de configuración Windows comprobada; puentes nativos Win32 y analizador no construidos en Mac por falta de toolchain. Pendientes instalación, DPI, monitores, audio/vídeo y rendimiento en Windows real.
- Prueba visual aislada activable con `MAVIMUSIC_REVIEW_VISUAL=1`; usa exclusivamente preferencias de prueba y material sintético, nunca abre la app sobre el perfil real.

Resultado final r2: 103 pruebas shared + 42 desktopUi + 527 desktopApp; 670 aprobadas, 2 omitidas (llavero y ventana Windows), sin fallos. En esta ejecución estaban habilitadas las pruebas de red, escenas y perfil de medios. DMG Apple Silicon y firma local verificados; validación interactiva por el usuario y Windows real pendientes.

## Tercera revisión: suspensión en macOS, orden de Inicio y vídeo (23–24 de noviembre de 2026)

Consolidado y publicado en la rama `codex/android-stability-stage-1` (sobre `8d4929d6`, commit `7eec953d` en `origin/codex/android-stability-stage-1`, sin tags ni publicación de release).

- Confirmaciones de diseño cerradas con el usuario (24 de noviembre de 2026):
  - `Música sugerida`, `Álbumes para ti` e `Iniciar radio` permanecen justo debajo de `Recents` y encima del feed general de YouTube.
  - `buildStartRadioShelf` conserva el orden estable por ranking de escucha y `FEmusic_new_releases` permanece visible en el feed.
  - `DesktopQueue.withoutDuplicates` conserva su comportamiento actual (mantiene remixes en `BROWSE` y evita repetir el mismo `videoId` o pares audio/videoclip).

- `DesktopWindowVisibility.kt` / `DesktopWindowVisibilityTest.kt`: suspensión de `_renderActive` al minimizar u ocultar también en macOS (`attachWindow` unificado para todas las plataformas, `startHidden()`, `show()`, `onCloseRequest()` y soporte `AppHiddenListener` para ocultar app con `⌘H`), deteniendo visualizadores y videoclip cuando la ventana no es visible.
- `DesktopHomeRecommendations.kt` / `DesktopApp.kt` / `DesktopHomeRecommendationsTest.kt`: `Recents` queda fijado en el índice `0` con `Música sugerida`, `Álbumes para ti` e `Iniciar radio` inmediatamente debajo; «Escuchar de nuevo» (`Listen again`) permanece en el feed inferior; las cargas prepended de `Recents` reemplazan alias previos (`Recently played`, `Recientes`); exclusión de «No me interesa» antes de `rotate().take(4)` y respaldo `localRecentsShelf()` desde historial local.
- `DesktopCanvasDecoder.kt` / `DesktopMusicVideo.kt` / `DesktopStrings.kt` / `DesktopPlaybackRecoveryTest.kt` / `DesktopMusicVideoDecoderTest.kt`: reconexión FFmpeg `reconnect*` en URLs HTTP/HTTPS, eliminación de asignación duplicada, propagación de `failure` cuando `av_read_frame < 0` distinto de `AVERROR_EOF`, prevención de re-seek repetido al alcanzar el final del vídeo (`shouldSeekVideo`) y localización de `VideoTransport` mediante `DesktopStrings` (`d_volume` añadido a `SPANISH_FALLBACKS`).
- Validación tras los últimos cambios de código:
  - `JAVA_HOME=/Users/emersonsmm/.jdks/temurin-21/Contents/Home ./gradlew :shared:jvmTest :desktopUi:jvmTest :desktopApp:test --rerun-tasks`: `103` `shared` aprobadas, `42` `desktopUi` aprobadas, `525` `desktopApp` aprobadas y `6` omitidas opt-in/plataforma (llavero macOS, `DesktopImmersivePreviewTest` ×2, `DesktopMediaProfileTest`, `DesktopMusicVideoNetworkTest`, `DesktopWindowsWindowIntegrationTest`), `0` fallos.
  - `JAVA_HOME=/Users/emersonsmm/.jdks/temurin-21/Contents/Home ./gradlew -Pmavimusic.target=windows :desktopApp:compileKotlin :desktopApp:compileTestKotlin`: compilación correcta (`BUILD SUCCESSFUL`, puentes nativos Win32/analizador omitidos en Mac por falta de toolchain).
  - `MAVIMUSIC_REVIEW_VISUAL=1 JAVA_HOME=/Users/emersonsmm/.jdks/temurin-21/Contents/Home ./gradlew :desktopApp:test --tests '*DesktopImmersivePreviewTest' --rerun-tasks`: `2` aprobadas, `0` fallos.
- Pendientes: validación visual/interactiva en macOS con sesión real (minimizar/ocultar/`⌘H`, orden de Inicio, reproducción de vídeo) y compilación nativa/aceptación visual en hardware Windows real.
