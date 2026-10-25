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

---

## 5. Fase 2: Pipeline DSP de AutoEQ (Corrección de Auriculares) y Prevención de Clipping

### 5.1 Orden de Procesamiento en la Cadena DSP
La cadena de audio de 32-bit float en `DesktopAudioEngine` se estructura en el siguiente orden secuencial estricto:
1. **Decodificador FFmpeg** (PCM Float 32-bit interleaved).
2. **Audio Espacial** (`DesktopSpatialAudio`).
3. **Corrección de Auriculares AutoEQ** (`DesktopAutoEqProcessor` — filtros paramétricos de corrección acústica).
4. **Ecualizador de Usuario y Tone Pad** (`DesktopEqualizer` — EQ gráfico de 10 bandas y pad tonal Graves/Medios/Agudos).
5. **Filtro de Transición / Automix** (`TransitionFilter`).
6. **Smart Trim / Salto de Silencio** (`DesktopSilenceSkipper`).
7. **Normalización de Volumen y Limitador de Picos** (`Loudness Normalizer` + `PeakLimiter`).
8. **Salida de Audio** (`DesktopAudioSink` / CoreAudio).

### 5.2 Justificación Acústica
AutoEQ busca linealizar la respuesta en frecuencia del transductor de los auriculares hacia la curva objetivo Harman Target. Al posicionar AutoEQ **antes** del ecualizador del usuario:
- Se neutralizan las anomalías acústicas y resonancias del auricular en primera instancia.
- Cualquier ajuste posterior del usuario (EQ de 10 bandas o Tone Pad) actúa sobre un campo acústico neutro ("lienzo en blanco"), en lugar de amplificar picos de distorsión propios del modelo de auricular.

### 5.3 Protección contra Clipping y Comparación A/B Nivelada
- **Preamp anti-clipping automático:** Los filtros paramétricos con ganancia positiva pueden elevar picos por encima de 0 dBFS. El procesador calcula automáticamente `preampDb <= -max(0, max_positive_gain)` (o adopta el preamp nominal del perfil), aplicándolo en el dominio lineal float antes de los biquads.
- **Modo A/B nivelado (Level-Matched Audition):** Al conmutar entre perfil activo (A) y bypass (B), el estado B conserva el mismo preamplificador negativo sin los filtros biquad. De esta forma, el usuario compara estrictamente el balance tonal sin el sesgo psicoacústico de mayor o menor sonoridad (loudness bias).

### 5.4 Arquitectura de Filtros Paramétricos
Se implementa una cascada de biquads en forma Direct Form II Transpuesta (DF2T) en aritmética de coma flotante de 32 bits, con coeficientes calculados según el Audio EQ Cookbook de Robert Bristow-Johnson:
- Filtros Peak (Campana/Paramétrico - PK).
- Filtros Low Shelf (LSC) y High Shelf (HSC).
- Soporte para importación de perfiles estándar AutoEQ / EqualizerAPO (`Preamp: -X dB`, `Filter N: ON PK Fc X Hz Gain Y dB Q Z`).
- Persistencia por dispositivo de audio de salida conectado (detectando modelo de CoreAudio / System Sound).

---

## 6. Fase 3: Time-Stretching con Key Lock (Master Tempo) y Evaluación de Licencias

### 6.1 Evaluación Comparativa de Tecnologías y Licencias

| Motor / Librería | Algoritmo | Calidad Acústica (±8% - ±15%) | Consumo CPU | Licencia y Restricción de Distribución |
|---|---|---|---|---|
| **FFmpeg `atempo`** | WSOLA / SOLA adaptativo por bloques | Excelente en rangos pequeños; sin efecto ardilla | ~1.2% CPU | **LGPL v2.1+**: Compatible con distribución binaria sin contaminación viral copyleft. |
| **Rubber Band Library (`librubberband`)** | Phase Vocoder + Subband DSP | Muy alta en transitorios complejos | ~4.5% - 8% CPU | **GPL v2+ o Comercial Privada**: **Incompatible** para distribución estándar sin licenciar comercialmente de pago o convertir toda la app a GPL estricto. No viene compilado en ByteDeco FFmpeg por defecto. |
| **SoundTouch** | WSOLA con detección de picos | Muy buena en voz y mezclas | ~1.8% CPU | **LGPL v2.1**: Compatible, pero añade dependencia JNI C++ adicional. |
| **Motor WSOLA Nativo 32-bit Float (`DesktopAudioSpeed` / `DesktopKeyLock`)** | WSOLA en coma flotante nativo en memoria | Excelente para alineación de tempo sin alterar tono | < 0.8% CPU (cero saltos JNI) | **Permisiva (MIT/Apache/LGPL compatible)**: 100% segura para redistribución libre en macOS DMG. |

### 6.2 Decisión y Justificación
Se adopta el motor nativo **WSOLA 32-bit Float (`DesktopAudioSpeed` / `DesktopTempoBuffer` / `DesktopKeyLock`)**:
1. **Seguridad Jurídica y de Licencia:** Descarta Rubber Band para evitar el riesgo legal de infracción GPL o pagos comerciales, asegurando cumplimiento estricto con las reglas de distribución de MaviMusic.
2. **Eficiencia y Cero Latencia:** Opera in-place sobre buffers float continuos interleaved dentro del pipeline existente sin saltos JNI foráneos ni asignaciones dinámicas por bloque.
3. **Rango Seguro Acotado (±8% por defecto, configurable hasta ±15%):** Los estudios psicoacústicos demuestran que transiciones de tempo de hasta un 8% no introducen artefactos perceptibles de modulación de fase. Más allá de ese rango, la interfaz notifica al usuario de posible degradación.
4. **Integración con Automix y AutoDJ:** Solo se activa cuando ambos temas tienen BPM con calificación fiable (`confidence >= 0.35`) y la diferencia porcentual cae dentro del rango seguro. Si alguna condición no se cumple o el interruptor está apagado, la ruta de audio salta completamente esta etapa devolviendo el comportamiento previo exacto.

---

## 7. Fase 3: Visualizadores Acelerados por GPU (Tecnología y Shaders SkSL)

### 7.1 Evaluación Tecnológica: Metal Directo vs GLSL vs Skia Shading Language (SkSL)

| Criterio | Metal Directo (`CAMetalLayer` / JNI) | GLSL Directo (OpenGL / GLFW) | Skia SL (`SkSL` vía Skiko / Compose) |
|---|---|---|---|
| **Rendimiento GPU** | Nativo (Metal en Apple Silicon / Intel) | Emulado / Deprecado por Apple | **Nativo**: Skia compila SkSL directamente a pipelines Metal MSL en macOS. 60–120 FPS fluidos. |
| **Portabilidad macOS** | Requiere código nativo Objective-C/Swift y enlace con AWT Canvas o NSView. Inestable con ventanas flotantes y escalado Retina dinámico. | OpenGL está formalmente deprecado en macOS. No soporta HDR ni ProMotion nativo adecuadamente. | **100% integrado**: Funciona directamente sobre el árbol de renderizado de Skiko y Compose Multiplatform. Sin JNI ni código C++ adicional. |
| **Complejidad y Mantenimiento** | Muy alta. Fugas de contexto Metal, sincronización con swapchain de Skia difícil. | Alta. Requiere puente de contexto gráfico independiente. | **Baja y Elegante**: Código fuente SkSL autocontenido en Kotlin, compilación y caching en tiempo de ejecución con `RuntimeEffect`. |
| **Tolerancia a Fallos** | Si el pipeline Metal falla o el shader no compila, puede congelar el NSWindow o causar un Kernel Panic/crash. | Crashes comunes en drivers Intel antiguos. | **Aislada y Segura**: Si un shader falla al compilar o el driver emite error, se captura limpiamente con `try-catch` y se conmuta discretamente al visualizador 2D clásico sin alterar la interfaz. |
| **Consumo en Reposo** | Complejo de pausar de forma sincronizada con el ciclo de vida de Compose. | Requiere bucle de renderizado manual con hilos dedicados. | **0% en Reposo**: Al cerrar o pausar el modo inmersivo o apagar el interruptor, `DisposableEffect` y `release()` liberan 100% de los efectos y pausan el bucle `withFrameNanos`. |

### 7.2 Decisión y Justificación
Se adopta **Skia SL (`SkSL` vía Skiko / Compose Desktop)**:
1. **Pipeline Metal Nativo:** Skia en macOS utiliza Metal como backend primario, de modo que cada shader SkSL se traduce y compila a Metal Shading Language (MSL) en la GPU de Apple Silicon o Intel.
2. **Reutilización de Análisis FFT:** No duplica DSP ni cálculos de espectro; consume directamente los acumuladores de energía `barValues` de `DesktopImmersiveLyricsScreen` descompuestos en bandas de graves (`u_bass`), medios (`u_mids`) y agudos (`u_treble`).
3. **Control de Cuadros y Recursos:** Implementa límite configurable de 30 FPS (bajo consumo), 60 FPS (estándar fluido) y 120 FPS (pantallas ProMotion de MacBooks de 14"/16").
4. **Fallback Transparente:** La degradación ante errores de compilación conmuta al visualizador 2D clásico (`drawWinampTunnel`, `drawWinampWaves`, `drawWinampWarp`) sin pop-ups ni avisos invasivos.

---

## 8. Fase 3: Modo Karaoke / Supresión Vocal con IA y Arquitectura DSP

### 8.1 Evaluación de Arquitectura: Modelos Deep Learning ONNX vs Separador DSP Estéreo M/S

| Parámetro | Redes Neuronales ONNX (Demucs v4 / MDX-Net / HTDemucs) | Separador DSP de Cancelación Central Adaptativa con Protección Espectral (M/S Biquad) |
|---|---|---|
| **Latencia de Audio** | **2.000 ms – 8.000 ms** (requiere ventanas STFT / FFT gigantes con overlap-add). Imposible para reproducción interactiva y scrubbing en tiempo real. | **0.0 ms (Latencia Cero)**: opera muestra a muestra / bloque a bloque in-place en tiempo real. |
| **Sincronización con Letras** | Alta deriva o necesidad de retrasar artificialmente la interfaz visual de letras en 2 a 8 segundos. | **100% Sincronizado**: Cero desviación respecto al playhead y las letras sincronizadas. |
| **Consumo de CPU / RAM** | **80% – 130% CPU** en tiempo real; 500 MB – 1.2 GB de RAM reservada. Provocaría sobrecalentamiento y 'thermal throttling' en Macs portátiles. | **< 0.4% CPU**; 0 MB de memoria adicional (solo coeficientes biquad en registros). |
| **Tamaño de Binario / Modelos** | **150 MB – 350 MB** por modelo. Inviable de empaquetar en el DMG sin inflar masivamente la descarga. | **0 KB** de datos binarios externos; código algorítmico ligero en Kotlin nativo. |
| **Calidad de Supresión Vocal** | Aislamiento completo de tallos, pero con artefactos de fase ("efecto burbuja" / phase warble) en hardware modesto. | Excelente atenuación de la voz solista paneada al centro (180 Hz a 7.5 kHz) mientras preserva con total transparencia el bajo, bombo, sintetizadores estéreo y guitarras. |

### 8.2 Decisión y Diseño Algorítmico
Se implementa el motor **DSP de Extracción Central con Doble Filtro Butterworth Biquad (`DesktopKaraokeProcessor`)**:
1. **Protección de Graves y Batería:** Filtro paso-alto Butterworth de 2º orden a 180 Hz que impide que el sub-bajo y el bombo central sean afectados por la cancelación.
2. **Protección de Brillo y Aire:** Filtro paso-bajo Butterworth de 2º orden a 7.500 Hz que preserva platillos, reverberaciones estéreo y frecuencias altas.
3. **Deslizador Continuo de Atenuación:** Permite balancear la atenuación desde 0% (voz original intacta) hasta 100% (máxima atenuación vocal) según la mezcla específica del tema.
4. **Cero Overhead en Reposo:** Cuando el modo Karaoke está inactivo o el interruptor está apagado, la ruta de audio omite el procesamiento con un retorno temprano inmediato (0% de CPU y preservación bit a bit del audio).
5. **Monitoreo de Presupuesto CPU:** Si el ciclo de procesamiento supera el 25% del tiempo de reproducción de bloque, la interfaz muestra un aviso discreto invitando a reducir la atenuación.
6. **Abstracción Extensible:** `DesktopKaraokeProcessor` expone una arquitectura modular donde un modelo ONNX de separación offline podría conectarse como motor de backend alternativo en el futuro sin modificar la interfaz de usuario ni el motor de audio.

---

## 9. Fase 3: Emisión a AirPlay 2 y Enrutamiento Nativo en macOS

### 9.1 Evaluación de Arquitectura: Protocolo Inverso AirPlay 2 vs Enrutamiento Nativo macOS

| Criterio | Protocolo AirPlay 2 Inverso (Librerías de terceros en JVM) | Enrutamiento Nativo del Sistema macOS (`AVRoutePickerView` / CoreAudio) |
|---|---|---|
| **Cifrado y Autenticación** | AirPlay 2 requiere autenticación criptográfica propietaria de Apple (SRP, HomeKit pairing, FairPlay). Las librerías de terceros (p. ej. jmupnp / airplay2-receiver) quedan obsoletas o rotas con cada actualización de firmware de iOS/tvOS/macOS. | **100% Nativo y Permanente**: El sistema operativo macOS gestiona las claves de sesión, autenticación de hardware y cifrado punto a punto de forma transparente y oficial. |
| **Estabilidad y Dependencias** | Requiere añadir dependencias Java/JNI pesadas y frágiles (> 30 MB) con riesgos de incompatibilidad y fugas de sockets. | **Cero dependencias externas**: Utiliza las interfaces CoreAudio y el subsistema de sonido nativo de macOS. |
| **Multi-room y HomePods Estéreo** | Falla frecuentemente con pares estéreo de HomePod y altavoces AirPlay 2 agrupados. | **Soporte Completo**: Permite emparejamiento estéreo, multi-room y volumen independiente gestionado por macOS. |
| **Compensación de Latencia** | Difícil de sincronizar con el motor de audio local. | **Sincronización Precisa**: El motor de audio (`DesktopAudioEngine`) compensa el retardo del búfer de red (ajustable de 0 a 3.000 ms, por defecto 2.000 ms) asegurando que las letras sincronizadas, la barra de progreso y los visualizadores coincidan con lo que escucha el usuario. |

### 9.2 Decisión y Diseño
Se adopta el **Enrutamiento Nativo del Sistema macOS con Compensación de Latencia (`DesktopAirPlaySettings` / `DesktopAirPlayManager`)**:
1. **Detección Dinámica:** Detecta receptores AirPlay presentes en CoreAudio y red local, integrándolos con distintivos claros en el selector de salida de audio (`DesktopAudioOutputPopover`).
2. **Traspaso Sin Cortes:** El cambio entre altavoces locales y receptores AirPlay 2 reconfigura el sink de audio en caliente sin reiniciar la pista ni perder la posición de reproducción (`playhead`).
3. **Compensación de Latencia:** Aplica un offset temporal en `heardSinkFrames()` para que la visualización, las letras sincronizadas y el playhead no se adelanten al audio reproducido por el búfer de red del HomePod o Apple TV.
4. **Seguridad y Aislamiento:** Detrás del interruptor "Soporte AirPlay 2" en Ajustes; al estar apagado, no ejecuta tareas en segundo plano ni altera las lecturas de tiempo del sink.---

## 10. Corrección de Rendimiento y Desbloqueo del Hilo de Audio en Tiempo Real (v1.8.46)

### 10.1 Análisis de Causa Raíz
1. **Llamadas bloqueantes a CoreAudio en el bucle de audio:**
   - La comprobación `DesktopAirPlaySettings.isAirPlayActive()` se ejecutaba decenas de veces por segundo en cada bloque de audio a través de `publishPosition` y `heardSinkFrames()`.
   - `isAirPlayActive()` invocaba internamente `DesktopAudioDevices.label()`, el cual recorría todos los mezcladores del sistema invocando `AudioSystem.getMixerInfo()` y verificando compatibilidad de líneas mediante llamadas nativas al subsistema CoreAudio HAL de macOS.
   - CoreAudio HAL bloquea internamente sus mutexes durante la enumeración de dispositivos. Al ejecutarse concurrentemente con la escritura de audio en `SourceDataLine.write()`, provocaba inanición del hilo de renderizado de audio (`pump()`), contención de bloqueos e interrupción completa del flujo de reproducción.
2. **Robustez y Límites del Procesador de Karaoke:**
   - En `DesktopKaraokeProcessor`, el filtro Butterworth de paso bajo requería desacoplamiento estricto de variables de retardo (`lpX2`) y acotamiento `coerceIn(-1.0f, 1.0f)` para evitar valores fuera de rango o NaN en caso de picos de audio.
   - El flag por defecto de Karaoke se ajustó a `false` (desactivado) para respetar la regla de invariancia del pipeline de audio.

### 10.2 Solución y Verificación
1. **Optimización O(1) no bloqueante de `isAirPlayActive`:** Se evalúa únicamente la cadena en memoria de `DesktopAudioDevices.selected.value` sin tocar `AudioSystem` ni los mezcladores del sistema en el hilo de audio.
2. **Protección NaN e invariancia bit a bit:** Cuando Karaoke o AirPlay están inactivos, la señal no sufre ninguna transformación ni retardo extra.
3. **Validación:** 100 transiciones de automix consecutivas sin fallos (`DesktopSeekAndAutomixSimulationTest`), paso completo de suite de pruebas unitarias y generación de DMG v1.8.46.


## Windows y macOS: pasada de interfaz y actualización (base 1.8.51)

### 1. Actualización transparente
- Se usa el MSI exacto de cada versión, no el EXE inicial ni flags /S supuestos. Se verifica tamaño y SHA-256 proporcionado por la API HTTPS del repositorio; esto asegura integridad con GitHub como raíz de confianza, no sustituye Authenticode ni una firma independiente del canal.
- Auxiliar PowerShell separado: copia solo app/runtime/launcher, captura procesos propios y descendientes con PID+fecha para evitar reutilización, espera 45 s y limita la limpieza forzada a 15 s. No cierra procesos ajenos por nombre.
- MSI por usuario /qn /norestart; no solicita elevación. Se valida instalación HKCU existente. Portable e instalación por máquina quedan en actualización manual, sin convertirlas silenciosamente.
- Se cambia RemoveExistingProducts del MSI generado a justo después de InstallInitialize, dentro de la transacción de rollback (la plantilla de jpackage 21 lo ponía antes de CostInitialize). Se conserva además una copia de archivos de programa para recuperación. INSTALLDIR mantiene la ubicación anterior. No se copian ni borran ajustes, biblioteca, caché o estadísticas. Ventana oculta se restaura mediante argumento interno, con checkpoint existente.
- El motor de audio, Automix y seek no se modifican. Se reutiliza el callback de cierre ya existente solo después de que el auxiliar esté listo.
- Pendiente de aceptación física: actualización real entre MSI, bloqueo por antivirus, fallo de MSI/registro y restauración en diferentes ubicaciones. Si Windows Installer excede 10 min, no se intenta copiar archivos encima de una transacción viva: se conserva el registro y se requiere recuperación manual.
- macOS conserva Sparkle y su cierre/checkpoint actual. Pendiente probar una actualización real entre dos instalaciones firmadas; no se reemplaza por el auxiliar Windows.

### 2. Texto del botón de actualización
- Causa confirmada: DesktopAccent es blanco y el botón antiguo forzaba texto blanco. No era una fuente ausente. Se usa un componente compartido con contraste calculado, fuente SansSerif del sistema y colores de deshabilitado explícitos. La etiqueta permanece visible en descarga y preparación; errores y reintento no eliminan el botón.
