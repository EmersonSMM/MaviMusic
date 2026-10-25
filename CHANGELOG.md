# Novedades de MaviMusic

## 1.8.47

- Corrección del instalador de Windows: asignación de un `UpgradeCode` único (`e428f6a4-9679-5c99-b1d0-8a044d18c068`) y selector de carpeta de destino para evitar cierres en "Getting Information" y colisiones con instalaciones previas.
- Publicación directa y canal de actualización automática Sparkle activo para macOS (Apple Silicon e Intel).
- Paquetes de instalación actualizados para Windows (.exe, .msi, .zip portable), macOS (.dmg, .pkg, .zip) y Linux (.AppImage, .deb, .rpm).

## 1.8.46

- Estabilidad y restauración del motor de audio en tiempo real: eliminación de consultas de dispositivos CoreAudio en el bucle de procesamiento de audio.
- Blindaje del procesador de Karaoke y AutoEQ contra valores infinitos o NaN, con clamping en [-1.0, 1.0].
- Compatibilidad multiplataforma en dibujado de shaders GPU entre macOS, Windows y Linux usando `nativeCanvas`.
- Validación de 100 transiciones consecutivas de Automix sin fallos ni desincronización.

## 1.8.45

- Modo Karaoke con atenuación y extracción vocal mediante cancelación de fase adaptativa, filtro paso banda (300 Hz - 3.4 kHz) y preservación de graves.
- Soporte y compensación de latencia para AirPlay 2 con amortiguamiento ajustable (0 a 3000 ms) para sincronización de audio y letras.
- Detección de dispositivos AirPlay, HomePod y Apple TV en macOS.

## 1.8.44

- Visualizadores reactivos con shaders GPU acelerados mediante SkSL (Skia Shading Language).
- Tres nuevos modos inmersivos: Túnel de Neón, Superficie de Agua Reactiva y Partículas Cósmicas Espectrales.
- Límite de fotogramas configurable (30, 60, 120 FPS) con suspensión automática al minimizar o perder visibilidad de ventana.

## 1.8.43

- Time-Stretching con Key Lock (bloqueo de tono): ajuste dinámico de tempo sin alteración de tono mediante filtro atempo de FFmpeg.
- Integración en Automix y AutoDJ con rango seguro de tempo (±8%) y adaptación gradual en transiciones.
- Control manual de velocidad y pitch lock desde el reproductor y Ajustes.

## 1.8.42

- Detector de BPM en segundo plano para pistas locales y descubiertas, con cálculo de confianza e inmunidad a doblaje o división de tempo.
- Metadatos de tempo integrados para reglas de playlists inteligentes y transiciones armónicas.

## 1.8.41

- Estadísticas locales privadas y Wrapped anual: cálculo 100% en dispositivo sin telemetría ni transmisión remota.
- Resúmenes de escucha: canciones más reproducidas, artistas destacados, géneros frecuentes y tiempo total acumulado.

## 1.8.40

- Playlists inteligentes (Smart Rules): creación de listas dinámicas con filtros combinables (Y / O) y operadores avanzados.
- Plantillas de playlists preconfiguradas: Más escuchadas, Favoritas rápidas, Descubrimientos recientes y Olvidadas.
- Actualización automática en segundo plano al reproducir, marcar favoritas o añadir temas a la biblioteca.

## 1.8.39

- AutoEQ: Corrección paramétrica para cientos de modelos de auriculares populares con ecualización de alta fidelidad.
- Protección automática contra saturación (negative preamp) para prevenir clipping en el pipeline de 32 bits en coma flotante.
- Comparación instantánea A/B y memorización del perfil activo por dispositivo de salida de audio.

## 1.8.38

- Mini-Player compacto integrado en la barra de menús de macOS con carátula y controles multimedia.
- Atajos de teclado globales configurables para reproducción, pausa, salto de pista y ajuste de volumen.

## 1.8.37

- Optimización del pipeline de audio a 32-bit float con headroom ampliado y cero saturación interna.
- Reducción del consumo de CPU y memoria en reposo a niveles mínimos en macOS y Windows.

## 1.8.36

- Nuevo actualizador integrado en macOS con ventana de novedades y descarga desde GitHub.
- Actualizaciones y changelog verificados mediante firmas Ed25519.
- Búsqueda manual y periódica de versiones, con descarga e instalación al salir opcionales.
- Conservación de la cola y del punto de reproducción antes de reiniciar para actualizar.
- Canales independientes para Apple Silicon e Intel.
- Compatibilidad de Automix con Intel y actualización de FFmpeg para mejorar su carga en Linux.

## 1.8.35

- Apertura de archivos MP3 desde Finder, el selector de archivos y el arrastre a la ventana.
- Biblioteca de carpetas locales, etiquetas, carátulas y playlists en este equipo.
- Reproducción continua de listas y álbumes en orden de pistas.

## 1.8.34

- Retirada de los módulos móviles y conservación de las funciones del escritorio.
