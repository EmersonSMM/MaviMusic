# DECISIONS.md — Diagnóstico de Causa Raíz y Decisiones de Diseño

## 1. Diagnóstico de la Regresión del Automix y Fallo tras Seek (Punto A.2)

### 1.1 Historial de cambios revisados (`git log` / `git diff` y bitácora `~/.local/state/mavimusic/desktop.log`)
Se auditaron los archivos tocados en las últimas iteraciones:
- `DesktopAudioEngine.kt` (`prepareNext`, `resolveUpcoming`, `performSeek`, `maybeStartCrossfade`, `armIncomingBlend`, `promoteUpcoming`, `endBlend`)
- `DesktopApp.kt` (`LaunchedEffect` de sincronización de cola y `loadAutoplaySongs`)
- `DesktopQueue.kt` (`nextDistinctIndex`, `followingFor`, `withoutAutoplayDuplicates`)
- `QueueBuilder.kt` (`isSameRecording`, `isSameTrackOrVariant`)
- `DesktopDjState.kt`

### 1.2 Causas Raíz Confirmadas

#### Causa Raíz A (Fallo intermitente / Regresión del Automix): Condición de carrera por cancelación de corrutina al refrescar la cola de Autoplay
1. Cuando comienza una pista, `DesktopApp.kt` ejecuta `LaunchedEffect(..., selectedSong?.videoId, queue, liveQueue.index, ...)` y llama a `playbackEngine.prepareNext(next)`.
2. `prepareNext(next)` asigna `nextSong = song` y lanza `nextResolveJob = scope.launch { ... }` en `resolveUpcoming(song)`.
3. Aproximadamente 1 segundo después, `loadAutoplaySongs()` termina de anexar pistas sugeridas al final de `liveQueue` (`autoplay: queued N after '...'`).
4. Esa mutación de `liveQueue` vuelve a disparar el `LaunchedEffect` de `DesktopApp.kt`, el cual invoca `playbackEngine.prepareNext(next)` **por segunda vez con exactamente la misma canción siguiente (`next`)**.
5. En la implementación previa, `prepareNext` ejecutaba incondicionalmente `nextResolveJob?.cancel()` y `clearUpcoming()` sin verificar si `song.videoId` ya se estaba precargando o ya estaba lista.
6. El primer `nextResolveJob` cancelado capturaba `CancellationException` dentro de `DesktopMusicSources.resolve(...)` (`runCatching`), entraba en `onFailure` (`DesktopAudioEngine.kt:597-600`) y ejecutaba:
   ```kotlin
   if (nextSong?.videoId == song.videoId) nextSong = null
   ```
   Como la segunda corrutina estaba resolviendo el **mismo `song.videoId`**, la primera corrutina cancelada borraba `nextSong = null` (confirmado más de 13 veces en `~/.local/state/mavimusic/desktop.log` con líneas `could not resolve incoming '...': StandaloneCoroutine was cancelled`).
7. Cuando la segunda corrutina terminaba de abrir el decodificador (`DesktopAudioEngine.kt:578`), comprobaba `if (nextSong?.videoId != song.videoId)`, veía `nextSong == null`, **cerraba el decodificador recién abierto y descartaba silenciosamente `upcoming`**. Resultado: tanto `upcoming` como `nextSong` quedaban en `null` y el Automix no se ejecutaba al final de la canción.

#### Causa Raíz B (Fallo del Automix tras hacer Seek manual): Destrucción de `upcoming`, orden invertido de `displaySwitched` y bloqueo de 12 s
1. **Destrucción de `upcoming` y código inalcanzable en `performSeek`:**
   En `DesktopAudioEngine.kt:1532-1542`, el bloque `if (fadeRemaining > 0 || blendActive)` cerraba `upcoming`, ponía `upcoming = null` y llamaba a `endBlend()` (que ponía `displaySwitched = false`) **antes** de comprobar `if (displaySwitched && upcoming != null) promoteUpcoming()`. Además, nunca volvía a preparar `nextSong` tras el seek, dejando `upcoming = null` permanentemente si el usuario hacía seek una vez iniciada la ventana de mezcla.
2. **Supresión pos-seek de 12 segundos:**
   En `maybeStartCrossfade` (`DesktopAudioEngine.kt:1708-1711`), la guarda `if (timeSinceManualSeek < 12_000L && lastManualSeekPositionMs >= duration - 35_000L)` bloqueaba el inicio de cualquier crossfade durante 12 segundos tras hacer seek en los últimos 35 segundos del tema, impidiendo que la transición se ejecutara antes de que terminara la pista.
3. **Falta de acortamiento dinámico cuando el tiempo restante es menor al crossfade:**
   Si un seek caía más allá de `plan.transitionStart` o cuando faltaba menos tiempo que `plan.fadeSeconds`, el planificador no ajustaba la duración de la transición al tiempo restante real.

### 1.3 Solución Implementada para el Automix
- **Idempotencia y control generacional (`nextResolveGeneration` + `resolvingUpcomingId`):** `prepareNext(song)` no cancela ni cierra `upcoming` si el mismo `videoId` ya está precargado, en curso de precarga o esperando armado de addon. Las corrutinas canceladas (`CancellationException`) o de generaciones anteriores nunca mutan `nextSong`.
- **Máquina de estados explícita (`AutomixState`):** `INACTIVE → ARMED → PRELOADING → TRANSITIONING → COMPLETED` centralizada en `DesktopDjState` con registro de diagnóstico local (`[Automix] state=... | current='...' | next='...' | transitionAt=... | fade=... | reason=...`).
- **Recálculo pos-seek y casos límite (`performSeek` + `recalculateTransitionAfterSeek` + `adjustPlanForRemainingTime`):**
  - Seek tras el punto medio de una transición (`displaySwitched == true`): promueve primero la pista entrante (`promoteUpcoming()`) y aplica el seek sobre ella.
  - Seek hacia atrás después de iniciada la transición (antes del punto medio): aborta la mezcla activa, rebobina `upcoming` a `0s` sin cerrar su decodificador y la mantiene en estado `ARMED`.
  - Seek dentro de la ventana de transición o cuando falta menos tiempo que la duración nominal del crossfade: acorta dinámicamente `fadeSeconds` al tiempo restante (`remainingSec.coerceIn(0.8, plannedFade)`) en lugar de cancelar la transición.
- **Fallback limpio:** Si todas las fuentes de red fallan al precargar `upcoming`, se registra el motivo en el log de diagnóstico y `finishTrack` realiza un corte limpio (`onEnded()`) hacia la siguiente pista disponible sin detener la reproducción.

---

## 2. Diagnóstico del Seek Impreciso en la Barra de Progreso (Punto A.1)

### 2.1 Causa Raíz Confirmada
En `DesktopThinSlider.kt` (`line 75`), el modificador `.pointerInput(Unit)` capturaba las lambdas `onValueChange` y `onValueChangeFinished` de la **primera composición** sin usar `rememberUpdatedState`.
- En `DesktopBottomPlayerBar.kt` y `DesktopImmersiveLyricsScreen.kt`, `durationMs: Long` se pasaba por valor dentro de `onValueChangeFinished = { onSeek((scrubFraction * durationMs).toLong()) }`.
- Como `.pointerInput(Unit)` nunca se reinicia al cambiar de canción, `onValueChangeFinished` seguía multiplicando `scrubFraction` por el `durationMs` de la **primera canción reproducida en la sesión**.
- Ejemplo real: Canción 1 dura `4:00` (`240_000 ms`). Canción 2 dura `3:00` (`180_000 ms`). Al hacer clic o arrastrar al minuto `2:00` en la Canción 2 (`scrubFraction = 0.6667`), la etiqueta visual mostraba `2:00`, pero al soltar el ratón la clausura obsoleta calculaba `0.6667 * 240_000 = 160_000 ms` (**`2:40`**). Si el usuario hacía clic cerca del final de una canción más corta, multiplicaba por la duración mayor, saltando más allá del final de la canción y cambiando de pista inmediatamente.

### 2.2 Solución Implementada
- Se incorporó `rememberUpdatedState(onValueChange)`, `rememberUpdatedState(onValueChangeFinished)` y `rememberUpdatedState(tooltipText)` dentro de `DesktopThinSlider.kt`.
- Se añadió la función pura `computeSeekTargetMs(xPx, barWidthPx, durationMs)` con precisión de milisegundos sin redondeos a segmentos.
- Se añadió un tooltip flotante en tiempo real sobre la posición del cursor durante el arrastre (`Popup` con el tiempo objetivo exacto) y retención temporal (`pendingSeekMs`) de 650 ms para evitar cualquier parpadeo visual mientras el hilo de audio reposiciona el decodificador.

---

## 3. Detector Multicapa de Similitud de Canciones y Versiones (Punto A.3)

### 3.1 Diseño del Algoritmo (`DesktopTrackSimilarity.kt` y `QueueBuilder.kt`)
1. **Capa 1 — Normalización agresiva de títulos (`normalizeTitle`):**
   - Descomposición Unicode NFD, eliminación de tildes/diacríticos y emojis, conversión a minúsculas.
   - Eliminación de sufijos de canal/tubería (`| ...`, `// ...`, ` l PRIMERA MUSA`, `- Topic`).
   - Eliminación de etiquetas de ruido entre paréntesis/corchetes o sueltas: `(Video Oficial)`, `(Official Video)`, `(Video Official)`, `(Official Music Video)`, `(Lyrics)`, `(Audio)`, `(Visualizer)`, `(HD)`, `(4K)`, `(Letra)`, `[Explícita]`, `(Explicit)`, `(Remastered)`, `ft./feat./con/junto a/prod.`.
   - Eliminación del nombre del artista repetido como prefijo o sufijo en el título, incluyendo apodos anidados entre paréntesis (p. ej. `OMAR COURTZ (((ousi))) - GODDESS`).
   - Colapso de letras sueltas separadas por espacios (`"G O D D E S S"` → `"goddess"`).
2. **Capa 2 — Similitud de texto y artista:**
   - Combinación ponderada de similitud **Jaro-Winkler** (`55%`) y **Levenshtein normalizado** (`45%`) sobre el título normalizado (`72%` del peso base) más coincidencia de tokens de artista (`28%` del peso base).
3. **Capa 3 — Refuerzo por duración similar:**
   - Si ambas pistas tienen duración conocida y la diferencia es `≤ 10 s`, se suma un bono de `+0.10` al puntaje combinado (tope `1.0`).
4. **Capa 4 — Coincidencia directa por metadatos:**
   - Coincidencia directa inmediata (`score = 1.0`) si `videoId` coincide.
5. **Ventana histórica y configuración:**
   - Umbral por defecto: `DEFAULT_THRESHOLD = 0.82`.
   - Ventana de comparación: últimas `N = 10` canciones reproducidas (`DEFAULT_HISTORY_WINDOW = 10`) más la canción actual y la cola activa.
   - Ajustes persistidos y configurables desde la interfaz: `"Evitar versiones repetidas"` (`avoid_duplicate_versions = true`) y `"Permitir remixes/en vivo"` (`allow_remixes_live = true`, que distingue etiquetas `remix`, `live`/`en vivo`, `acoustic`, `cover`).

---

## 4. Mejoras del Modo Inmersivo (Puntos B.4–B.7 y Criterio 11)

- **Punto B.4 y Criterio 11 (Persistencia y reemplazo de "Winamp" por "Classic"):**
  - Todos los textos visibles al usuario reemplazan `"Winamp"` por `"Classic"` (`"Ondas Classic"`, `"Espectro Classic"`, `"Verde Classic"`, `"Modo Inmersivo Classic / Ondas"`).
  - Se persisten en `DesktopPersistence` el modo visual (`immersive_visual_mode`), la paleta de color (`immersive_color_palette`), el modo de efectos clásicos (`immersive_classic_mode`) y el estado de letras (`immersive_show_lyrics`).
- **Punto B.5 (Controles de reproducción con efecto Frosted Glass estilo macOS):**
  - En `ImmersiveHeroPlayer`, la barra de progreso, tiempos, controles de transporte y volumen se agrupan dentro de una tarjeta translúcida esmerilada (`Surface` con fondo translúcido multicapa, borde con gradiente especular sutil y sombra elevada de `22.dp`).
- **Punto B.6 (Cola flotante independiente en Modo Inmersivo con atajo `Q`):**
  - Panel flotante glass independiente a la derecha, conmutable mediante botón (`Cola (Q)`) o tecla `Q` (`KeyEventDispatcher`), que comparte el estado `liveQueue` del modo normal y permite reproducir al instante, subir/bajar pistas (`onMoveQueueItem`) y eliminar pistas (`onRemoveQueueIndex`).
- **Punto B.7 (Indicador dinámico de calidad de audio real):**
  - La insignia superior derecha lee `playback.streamFormat` real medido por FFmpeg (`measuredFormat ?: stream.format`) mediante `formatDynamicStreamQuality`, mostrando códec, profundidad de bits o bitrate, frecuencia de muestreo y canales reales (p. ej. `"FLAC 24-bit • 96 kHz • STEREO"`, `"OPUS 153 kbps • 48 kHz • STEREO"`, `"AAC 256 kbps • 44.1 kHz • STEREO"`) o `"N/D"` cuando el dato no está disponible.
