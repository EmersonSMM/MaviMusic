# Android: estabilización y continuidad

Fecha: 29 de octubre de 2026.

## Base y alcance

- Base local: `3a66667e`.
- Rama: `codex/android-stability-stage-1`.
- Al comenzar, `main` estaba 12 commits por delante y 2 por detrás de la referencia local `origin/main`. No se hizo fetch, integración ni push.
- El usuario autorizó avanzar por etapas hacia una experiencia coherente con escritorio, visualizadores básicos y una base interna más fiable.
- La etapa 1 se limitó a reproducción/navegación; las etapas siguientes, registradas al final, amplían el diseño, visualizadores y base interna.

## Cambios

- `androidApp/.../audio/AndroidQueueOrder.kt`: recorrido aleatorio sin repetir pistas dentro de un ciclo; anterior recorre el mismo orden; repetir todo inicia otro ciclo; la radio puede añadir pistas al recorrido.
- `androidApp/.../audio/AndroidPlayerEngine.kt`: conecta ese recorrido a la cola; vuelve a resolver/preparar la canción restaurada al pulsar Play si Media3 no tiene un elemento; conserva la intención de pausa durante la resolución; un evento de fin consumido por el temporizador no avanza ni repite.
- `androidApp/.../audio/SleepTimer.kt`: controlador con reloj de coroutines inyectable. El temporizador por minutos permite avanzar canciones; el de fin de canción consume el evento antes de cancelar su estado.
- `androidApp/.../ui/MobileSearchState.kt` y `MainActivity.kt`: consulta, filtro, resultados, desplazamiento y peticiones viven en el shell. Abrir el reproductor o cambiar de pestaña no los descarta. Las búsquedas obsoletas no pisan las nuevas y los fallos se distinguen de resultados vacíos.
- `MainActivity.kt`: elimina la etiqueta HI-RES incondicional y mantiene los botones de navegación disponibles con aleatorio/repetición/radio.
- `desktopUi/src/androidMain/.../StatusBarIcons.android.kt`: corrige la inversión del contrato `dark`; solo cambia Android.
- `androidApp/build.gradle.kts`, manifiesto: variante `review` con ID independiente `com.music.mavimusic.android.review`, etiqueta «MaviMusic · pruebas» y datos aislados; dependencias JUnit/coroutines-test.
- `androidApp/src/test/`: 14 casos para recorrido, temporizador, cancelación y búsquedas fuera de orden.
- `docs/ANDROID_ROADMAP.md`: aclara límites de persistencia, radio y sincronización.

## Compilación y pruebas automáticas

JDK 21 y Android SDK local. No se usan preferencias, biblioteca, credenciales ni llavero del usuario.

```bash
./gradlew :androidApp:testDebugUnitTest :androidApp:assembleReview
./gradlew :shared:jvmTest :desktopUi:jvmTest
```

- 14 pruebas Android aprobadas: 5 de cola, 4 de temporizador y 5 de búsqueda.
- `assembleReview` aprobado, con avisos de deprecación existentes. Los chequeos AAR siguen desactivados como estaban antes; esta etapa no valida ni corrige esa compatibilidad.
- `shared:jvmTest` y `desktopUi:jvmTest`: resultado satisfactorio, tareas reutilizadas (`UP-TO-DATE`). La ejecución real previa de esas suites fue de 103 y 42 casos respectivamente.
- No se ejecutó Windows nativo ni se modificaron backend Go, empaquetado de escritorio o workflows.

## Validación en dispositivo

Samsung Galaxy Tab SM-X110, horizontal 1340 × 800, mediante ADB. Se instaló únicamente la variante `review`, sin copiar datos de la aplicación habitual.

- Búsqueda real de Coldplay y selección de una canción: reproducción y letras visibles.
- Tras desplazar resultados, abrir/cerrar el reproductor conserva consulta y posición de lista.
- Iconos de estado claros tanto en reproductor como al regresar a Buscar; mini reproductor sin etiqueta HI-RES fija.
- Después de actualizar la variante review (proceso nuevo), Play vuelve a cargar la canción guardada; MediaSession confirma `PLAYING` y avance de posición. Se reanuda desde el inicio: la posición todavía no se persiste.
- Las pruebas automáticas del temporizador usan tiempo virtual; no se declara validación física completa de todos sus plazos ni de cada modo aleatorio.
- Capturas de esta sesión en `/tmp/mavimusic-stage1/` (temporales; no son artefactos versionados).

## Pendientes y siguientes etapas

1. Integrar la cola nativa de Media3 con la cola completa de la app; actualmente MediaSession publica un solo elemento.
2. Validar archivos locales con permiso concedido y biblioteca de prueba; revisar teléfonos pequeños, teclado y escalado de fuentes.
3. Persistir/reanudar descargas mediante un servicio o WorkManager si Android destruye el proceso. Las tareas actuales viven en el proceso; las descargas antiguas sin tamaño registrado conservan compatibilidad, pero no se certifica su integridad retroactivamente.
4. Medir fluidez, temperatura, batería y sincronía acústica de los visualizadores. Portar shaders más complejos solo después de medir.
5. Sincronización nativa de favoritos/playlists entre equipos y descubrimiento LAN. El acceso al control remoto existente debe distinguirse de esa sincronización.
6. Ejecutar el nuevo trabajo Android de CI al integrar la rama y resolver la compatibilidad AGP/SDK sin suprimir las comprobaciones AAR.

No se crearon tags ni publicaciones. Antes de continuar, comprobar rama, commit y cambios de otros agentes. La app habitual y la variante review son paquetes distintos; usar review para las verificaciones automáticas futuras.

## Etapa 2: identidad visual y navegación

Continúa sobre `c4db913a`, en la misma rama.

- `ui/AndroidAppearance.kt`: paleta neutra de escritorio, contraste blanco/negro y superficies semánticas. Se mantiene la fuente del sistema Android.
- `ui/MobileSidebar.kt`: iconos compartidos con escritorio, marca naranja, sidebar con texto en tablet ancha y formato compacto en anchuras intermedias. Utilidades con objetivos táctiles de al menos 48 dp.
- `ui/MobileHomeView.kt`: portada de altura acotada a 144 dp, carrusel y lista; cabecera y acciones útiles también cuando faltan recomendaciones. Resultados y desplazamiento de Inicio viven en el shell; radio basada en la canción actual como alternativa cuando quick picks no devuelve contenido.
- `MainActivity.kt`, `ui/AppModals.kt`: mini reproductor teal de la misma familia que PC, controles y filtros con contraste sobre blanco, eliminación de estilos magenta inconsistentes.
- Validación: `assembleReview` y 14 pruebas Android aprobadas. Inspección visual en la SM-X110 horizontal de sidebar, Inicio y mini reproductor; faltan otros tamaños y orientación vertical.
- Solo se actualiza el paquete review. No se modifica el perfil de la app habitual.

## Etapa 3: visualizadores básicos con señal de audio

Continúa sobre `23fc89fa`.

- `audio/AudioSpectrumAnalyzer.kt`: FFT de 1024 muestras, ventana Hann, 32 bandas logarítmicas, onda y RMS. No genera movimiento aleatorio para simular sonido.
- `audio/PcmVisualizationSink.kt`, `service/PlaybackService.kt`: TeeAudioProcessor observa el primer canal PCM sin modificar el buffer de entrada. FFT fuera del hilo de audio, canal conflado, límite de publicación de aproximadamente 30 Hz (10 Hz con movimiento reducido). Sin captura de micrófono ni permiso RECORD_AUDIO.
- `ui/MobileVisualizerScreen.kt`: modos Espectro, Onda y Ambiente; colores de la carátula, pausa, anterior/siguiente y búsqueda de posición. Preferencias de modo/movimiento en `AndroidPreferences.kt`. Entrada desde el icono de espectro del reproductor, retorno con Atrás.
- El análisis solo se habilita en la vista visible, con lifecycle RESUMED y reproducción activa; sale de ese estado al cerrar, pausar o pasar a segundo plano. El servicio cancela su worker al destruirse.
- `MainActivity.kt` conecta el nuevo modo; `ui/AndroidAppearance.kt` completa colores secundarios neutros.
- 21 pruebas Android aprobadas: las 14 anteriores, 5 de FFT/amplitud/silencio y 2 del observador PCM (buffer intacto, estéreo en contrafase, suspensión al estar oculto). `assembleReview` aprobado.
- Validación visual real en SM-X110: espectro y onda muestran señal durante reproducción; Ambiente cambia de modo y muestra su composición; controles accesibles en horizontal y vertical. La captura vertical restauró los ajustes de rotación originales. Pausa accesible desde el visualizador.
- Capturas temporales en `/tmp/mavimusic-stage3/`. No se midieron FPS, consumo, temperatura ni sincronía de la visualización con la salida acústica; no se garantiza 30 FPS por limitar el análisis a 30 Hz. Falta un teléfono de anchura pequeña.

## Etapa 4: descargas, permisos y sesión

Continúa sobre `5502546e`.

- `data/VerifiedAudioDownloader.kt`: valida HTTP 206, coordenadas/total de Content-Range y longitud de cada bloque; si la sonda devuelve HTTP 200, usa ese cuerpo completo una sola vez. Rechaza respuestas HTML/JSON/XML, archivos vacíos y bloques cortos o excedidos. Cierra recursos y propaga cancelación al socket HTTP. Mantiene tres bloques simultáneos por canción.
- `data/OfflineDownloadManager.kt`: evita duplicados en curso, limita a dos canciones simultáneas, añade progreso/error/cancelación y limpieza de temporales. Solo publica audio después de verificarlo y promover metadatos; guarda tamaño esperado y lo comprueba al abrir. Borrado/limpieza cancelan y esperan trabajos antes de retirar archivos.
- `MainActivity.kt`, `data/LocalAudioScanner.kt`: permiso solicitado bajo acción del usuario; denegación y fallo de lectura diferenciados de biblioteca vacía. Usa content URI de MediaStore en lugar de rutas DATA. Progreso y error de descarga visibles; cancelación disponible en la fila.
- `audio/AndroidPlayerEngine.kt`, `data/AndroidPreferences.kt`: posición guardada cada cinco segundos y al pausar/buscar; restaura posición, modos y cola completa sin el límite anterior de 50. Reconcilia la canción restaurada con la cola, cancela letras obsoletas y evita añadir una respuesta de radio a una cola sustituida. Errores de reproducción permiten reintentar desde un snackbar.
- `ui/AppModals.kt`, `host/AndroidPlayerHost.kt`: elimina etiquetas de calidad/formato fijas y la comprobación de actualización que simulaba éxito tras un delay. Ajustes muestra la versión real del paquete y el canal manual de pruebas. Ahorro de datos persiste y conecta un techo preferido de 128 kbps al resolver; no garantiza disponibilidad de ese bitrate ni convierte descargas existentes.
- Único cambio compartido: `StreamResolver.invalidateResolvedStreams()`, invocado desde Android al cambiar calidad. No cambia las llamadas del escritorio.
- 28 pruebas Android aprobadas: añade 7 casos HTTP locales para rangos correctos/incorrectos, servidor sin rangos, bloque corto, contenido HTML y cancelación. Suite de escritorio/compartidos ejecutada: 103 + 42 + 504 = 649 casos, 647 aprobados y 2 omitidos; sin acceso opt-in al llavero ni Windows nativo.
- SM-X110 con Android 16: solicitud de permiso y denegación verificadas sin conceder acceso a la biblioteca real. Descarga de prueba guardada en review (3.999.755 bytes) y reproducida desde archivo. Tras pausar, detener únicamente el paquete review y abrirlo de nuevo, la posición guardada se restaura y Play avanza desde aproximadamente 2:06.
- Capturas temporales en `/tmp/mavimusic-stage4/`. La reproducción local se verificó con WiFi activo; no se cortó la conectividad global de la tablet para probar modo avión.

## Etapa 5: acceso a escritorio y CI

Continúa sobre `59b3a594`.

- `data/DesktopRemoteAddress.kt`, `ui/AppModals.kt`, `MainActivity.kt`: sustituye el modal que simulaba conexión por acceso mediante navegador al control remoto web existente del PC/Mac. Puerto por defecto 8765, igual que `DesktopRemoteDiscovery`; permite puertos explícitos e IPv6. El PIN se introduce en la web del equipo; la app no simula emparejamiento ni sincronización de biblioteca. Recuerda la dirección abierta, comprueba su formato y maneja ausencia de navegador.
- `data/AndroidPreferences.kt`: guarda la dirección del control remoto.
- `.github/workflows/check.yml`: trabajo Android en Ubuntu 24.04, JDK 21, pruebas unitarias y APK review; solo sube informes si falla. No publica APK ni activa Release. El [inventario oficial del runner](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md) confirma SDK Android 36 y ANDROID_HOME. YAML validado localmente con Ruby/Psych; ejecución remota pendiente de integrar la rama.
- `scripts/deploy_android.sh`: instala review por defecto para aislar los datos. Sintaxis Bash comprobada; no se ejecutó el script completo sobre perfiles habituales.
- `README.md`, `docs/ANDROID_ROADMAP.md`: documentan comandos y distinguen control web de sincronización nativa pendiente.
- 32 pruebas Android aprobadas: añade 4 casos de direcciones (IP/nombre local, puerto, IPv6 y entradas inválidas). `assembleReview` aprobado. Suite JVM de escritorio/compartidos del bloque anterior: 647 aprobadas, 2 omitidas.
- Validación en SM-X110: modal remoto y rechazo de dirección vacía sin abrir el navegador. No se conectó a la sesión real del PC ni se verificó emparejamiento extremo a extremo. Ajustes conserva Ahorro de datos al cerrar/abrir; se dejó Automática al terminar.
- La última compilación quedó instalada únicamente como «MaviMusic · pruebas». La aplicación habitual no se reinstaló. No hubo push, tags ni publicaciones; no se modificó `.github/workflows/release.yml`.

## Ajustes puntuales: playlists y experiencia

Sobre `01ecb59d`: búsqueda añade Playlists y conserva resultados de álbum/playlist; `MobileCollections.kt` los abre por páginas con reproducción y carga manual de más canciones. Explorar conserva los elementos browse de cada género y muestra sus playlists. Se evita reiniciar las peticiones de carátulas cada vez que llega una imagen.

Inicio usa las dos búsquedas recientes guardadas (máximo 12 temas por consulta), sin quick picks ni radio de respaldo. Se distingue el historial realmente consultado de los ejemplos iniciales y se invalida la selección al cambiar de búsquedas.

Controles superiores y del visualizador más separados; el icono de personas se oculta en Android mediante una capacidad del host que permanece habilitada por defecto en escritorio. Sin nuevas dependencias.

Validación: 35 pruebas Android y 42 de desktopUi aprobadas; compilación desktopApp comprobada. En la tablet se verificaron resultados de playlists para Bad Bunny, apertura de una lista con canciones y playlists de la sección Calma. Se actualiza únicamente review.

## Bloques 1, 2 y 3 (v2 sobre `codex/android-v2`)

Fecha: 24 de noviembre de 2026.
Rama: `codex/android-v2` (base `2be51492`). `main` permanece limpio en `v1.8.54` (`954a3cc8`) sin tags ni publicaciones nuevas.

### Bloque 1: Motor de audio, MediaSession, cola editable y resiliencia
- `audio/AndroidPlaybackRecovery.kt`: recuperación acotada ante fallos de red (2 reintentos con espera de 1 s y 3 s, rearmada tras 30 s de progreso estable; omitida en archivos locales) y normalización de volumen hacia `-14 LUFS` (`AndroidLoudnessNormalizer`, atenuación lineal en ExoPlayer y `LoudnessEnhancer` acotado a `+9 dB`).
- `audio/AndroidQueueOrder.kt`: `hasNext`, `hasPrevious`, `insertAfterCurrent`, `removeAt` y `syncAfterMove` coherentes en orden lineal y aleatorio.
- `audio/AndroidPlayerEngine.kt`: `playNext`, `addToQueue`, `removeFromQueue`, `moveInQueue`, `clearQueue`, `setVolumeNormalization`, `selectLyricsProvider`, estados por proveedor de letras (`lyricsProviderStates`), lectura de formato de audio real (`audioFormat`), historial local (`playHistory`) y aplicación de recuperación/normalización.
- `service/PlaybackService.kt`: envuelve `ExoPlayer` en `ForwardingPlayer` para que `MediaSession` (pantalla de bloqueo, Bluetooth, Android Auto y notificación) exponga y ejecute `seekToNext`, `seekToPrevious`, `play` y `pause` delegando a `AndroidPlayerEngine`.
- `MainActivity.kt`, `ui/AppModals.kt`: cableado completo de `NowPlayingScreen` (`onRemoveFromQueue`, `onMoveInQueue`, `onClearQueue`, `onOpenAlbum`, `onOpenArtist`, `lyricsProviderStates`, `onSelectLyricsProvider`, `signedIn`/`accountName`, desfase de letra persistido), botones superiores ocultos cuando Queue/Lyrics están activos para evitar solapamientos, acciones de cola en `TrackActionsModal` y menú desplegable por fila en `SongRowItem`, además de interruptor de normalización en `SettingsModal`.

### Bloque 2: Sesión YouTube Music, Inicio real y paridad de hosts
- `data/AndroidYouTubeAuth.kt` y `ui/YouTubeSignInModal.kt`: inicio de sesión opcional con YouTube Music mediante `WebView` + `CookieManager`, validando cookies de firma (`SAPISID` / `__Secure-3PAPISID` / `__Secure-1PAPISID`), aplicando `Innertube.cookie` + `StreamResolver.onSessionChanged()`, mostrando el nombre de cuenta y permitiendo cerrar sesión limpiando cookies.
- `ui/MobileHomeView.kt` y `ui/MobileCollections.kt`: Inicio fija `Recientes` arriba (combinando historial local y `YtMusicRepository.homeRecentlyPlayed()`), añade tarjetas rápidas de `Iniciar radio`, estanterías reales de `YtMusicRepository.home()` con apertura de colecciones (`ALBUM`, `PLAYLIST`, `ARTIST` vía `artistPage`) y banner opcional para iniciar sesión. `MobileHomeState` mantiene retrocompatibilidad con sus constructores previos en pruebas.
- `host/AndroidAppHost.kt`: `downloadedIds()` reactivo sobre `OfflineDownloadManager.downloadedSongs`.
- `host/AndroidPlayerHost.kt`: traducción y romanización real mediante `LyricsTranslation` (con `cacheDir` inicializado en `MainActivity.onCreate`), detección de dispositivos de salida reales mediante `AudioManager.getDevices(GET_DEVICES_OUTPUTS)`, `outputFormat` conectado al formato real de ExoPlayer y persistencia de `lyricsOffsetMs`.

### Bloque 3: Descargas en WorkManager, icono real, pulido móvil y remoto nativo
- `androidApp/build.gradle.kts`: `versionName = "1.8.54"`, `versionCode = 2` y dependencia `androidx.work:work-runtime-ktx:2.9.1`.
- `data/DownloadAudioWorker.kt` y `data/OfflineDownloadManager.kt`: descargas encoladas mediante trabajos únicos de `WorkManager` (`ExistingWorkPolicy.KEEP`) con notificación de progreso de baja prioridad y limpieza de `.tmp` al cancelar o fallar, conservando respaldo por coroutines cuando WorkManager no está inicializado en pruebas JVM.
- `androidApp/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher*.png` y `AndroidManifest.xml`: iconos reales de MaviMusic generados desde `desktopApp/src/main/composeResources/drawable/app_logo.png`.
- `data/DesktopRemoteClient.kt` y `ui/AppModals.kt`: cliente nativo OkHttp HTTP (`POST /api/pair`) + WebSocket (`/ws?token=...`) compatible con `DesktopRemoteServer`, con panel de control integrado (carátula, Play/Pausa, Anterior/Siguiente, barra de progreso, volumen, aleatorio, repetición, Me gusta y selector de salida) más acceso secundario al navegador.
- Pulido responsivo en `MainActivity.kt`, `MobileVisualizerScreen.kt`, `MobileHomeView.kt` y `AppModals.kt`: objetivos táctiles de 44–48 dp, `imePadding()` en búsqueda, diálogos con scroll vertical y fila de acciones compacta con `MoreVert`.

### Compilación y pruebas automáticas
```bash
JAVA_HOME=/Users/emersonsmm/.jdks/temurin-21/Contents/Home ./gradlew :androidApp:testDebugUnitTest :androidApp:assembleReview
JAVA_HOME=/Users/emersonsmm/.jdks/temurin-21/Contents/Home ./gradlew :shared:jvmTest :desktopUi:jvmTest :desktopApp:test
```
- **Android (`:androidApp:testDebugUnitTest`)**: **45 pruebas aprobadas** (incluye nuevas pruebas en `AndroidPlaybackRecoveryTest`, `AndroidYouTubeAuthTest`, `DesktopRemoteClientTest`, `AndroidQueueOrderTest` ampliado y `MobileHomeStateTest` ampliado).
- **APK (`:androidApp:assembleReview`)**: generado correctamente (`androidApp/build/outputs/apk/review/androidApp-review.apk`, versión `1.8.54-review`).
- **No-regresión Desktop/compartidos (`:shared:jvmTest :desktopUi:jvmTest :desktopApp:test`)**: **BUILD SUCCESSFUL** sin regresiones.

### Validación real en dispositivo físico (ADB)
Dispositivo: **Samsung Galaxy Tab A9 (`SM-X110`)**, Android 16 (API 36), `800×1340` (`213 dpi`), variante aislada `com.music.mavimusic.android.review` (sin tocar ninguna instalación de producción). Nota: la tablet estaba sin Wi-Fi activo (`Active default network: none`), lo cual permitió verificar el comportamiento sin conexión.
- Arranque en frío sin fallos (`TotalTime ~2.4 s`, cero excepciones en `logcat`).
- Reproducción real de las 5 pistas descargadas sin conexión desde Biblioteca (`Lovers in Japan`, `No Debiste Volver`, etc.).
- **`MediaSession` / `ForwardingPlayer`**: verificado con `dumpsys media_session` (`state=PLAYING`, `actions=7340031`) y evento de hardware `KEYCODE_MEDIA_NEXT`, que avanzó correctamente a la siguiente pista de la cola.
- **Visualizador nativo**: verificado modo Onda animándose con señal real del audio en reproducción.
- **Cola en `NowPlayingScreen`**: se detectó y corrigió en caliente el solapamiento de los botones superiores sobre la cabecera compacta de Cola/Letras; re-verificado sin solapamiento tras recompilar y reinstalar.
- **Inicio (`Recientes` e `Iniciar radio`)**, menú contextual por pista (`MoreVert`), **Ajustes** (cuenta YouTube Music, normalización `-14 LUFS`, herramientas rápidas, versión `1.8.54-review`) y **Control remoto nativo** verificados visualmente mediante capturas reales.

### Pendientes de validación con red / otro hardware
1. Probar inicio de sesión real de YouTube Music y carga en vivo de estanterías de `YtMusicRepository.home()` con Wi-Fi activo.
2. Probar emparejamiento en vivo del Control remoto nativo contra una instancia activa de MaviMusic Desktop en la misma red LAN.
3. Probar descarga real vía `WorkManager` mandando la app a segundo plano o matando el proceso con red activa.
4. Validación visual en un teléfono estrecho (`360–411 dp`) y con teclado virtual abierto en Buscar.

## Pulido v2.1: notificación multimedia Android, estética cristalizada, visualizadores 60 fps y gestor offline tipo iPod

Sobre `84539ca3` en `codex/android-v2`:

- **Controles de música en notificaciones y pantalla de bloqueo (`PlaybackService.kt`, `MainActivity.kt`, `AndroidPlayerEngine.kt`, `ic_media_notification.xml`, `strings.xml`):**
  - Se registra `DefaultMediaNotificationProvider` (`mavimusic_playback_channel`, icono monocromo vectorial `ic_media_notification`), `addSession(session)`, `WAKE_MODE_NETWORK`, metadatos `MEDIA_TYPE_MUSIC` (`displayTitle`/`subtitle`/`isPlayable`), vinculación de `MediaController` en `MainActivity.onStart/onStop` y solicitud del permiso `POST_NOTIFICATIONS` en Android 13+.
  - Verificado en el dispositivo `SM-X110` (Android 16): `PlaybackService` entra en primer plano (`isForeground=true`, `types=mediaPlayback`, `Notification$MediaStyle`) y One UI muestra la tarjeta multimedia nativa con carátula, progreso y controles `Anterior / Pausa / Siguiente`.
- **Fondo cristalizado de la carátula (`ui/CrystallizedArtworkBackdrop.kt`, `MainActivity.kt`, `ui/MobileSidebar.kt`):** escena de vidrio esmerilado con desenfoque nativo de la carátula en reproducción, paleta animada y planos prismáticos translúcidos detrás de toda la app.
- **Visualizadores estéticos a 60 fps (`ui/MobileVisualizerScreen.kt`):** interpolación exponencial continua a 60 fps sobre el análisis PCM/FFT real con 5 modos inspirados en escritorio: **Aurora** (cintas de plasma y partículas reactivas), **Espectro** (barras simétricas con reflejo en cristal y peak-hold), **Órbita** (espectro radial alrededor de la carátula circular), **Onda Neón** (osciloscopio multicapa con curvas Bézier) y **Cristal** (orbes y hexágonos prismáticos).
- **Gestor de descargas tipo iPod Music (`ui/MobileDownloadsManager.kt`):** segmentación en **Canciones**, **Artistas**, **Álbumes** y **Playlists**, filtro rápido, vistas de detalle por artista/álbum/playlist con `Reproducir` y `Aleatorio`, y tarjeta de progreso activo de descargas.
- **Nombre de app y logo animado (`androidApp/build.gradle.kts`, `ui/MaviBrandLogo.kt`, `ui/MobileHomeView.kt`, `ui/MobileSidebar.kt`):** se retiró «pruebas» de la etiqueta visible (`appLabel = "MaviMusic"`, conservando `applicationIdSuffix = ".review"` para aislamiento de datos) y se animó la marca **MaviMusic** con un barrido luminoso tipo cortina sobre las letras.
- **Pulido v2.2 sobre dispositivo:**
  - `MiniPlayerBar` (`MainActivity.kt`): adopta la paleta de la carátula en reproducción (`rememberArtworkPalette` con transición suave de fondo, borde y barra de progreso) en lugar del verde azulado fijo.
  - `MobileVisualizerScreen.kt`: modo **Pantalla Completa (FullScreen)** inmersivo que oculta barras del sistema y expande el visualizador de borde a borde, con HUD flotante al tocar y auto-ocultado tras 4 s.
  - `MobileDownloadsManager.kt` y `MobileLibraryView`: barra segmentada compacta en una sola fila con filtro plegable (icono de lupa) y botones `Reproducir`/`Aleatorio` integrados en el desplazamiento, triplicando el espacio vertical útil en horizontal.
- **Pruebas:** `46` pruebas unitarias Android aprobadas (añade `MobileDownloadsManagerTest`), `:shared:jvmTest :desktopUi:jvmTest :desktopApp:test` sin regresiones y verificación visual en `SM-X110` (Android 16) mediante ADB.
