# Novedades de MaviMusic

## 1.8.52

- Actualización Windows por MSI verificado, auxiliar independiente e instalación silenciosa por usuario; recuperación transaccional y registro local.
- Aviso discreto de actualización, comprobaciones cada ocho horas y texto del botón con contraste correcto.
- Búsqueda de Ajustes sin contenedores vacíos; Escape y cierre accesible en letras.
- Menús glass compartidos y logo estático con brillo suave al pasar el cursor.
- Spotlight global configurable en Windows (Ctrl+Alt+K por defecto) y barra nativa con colores DWM.
- Exportación de diagnóstico sin datos privados y publicación que verifica todos los artefactos antes de hacer pública la release.
- Validación real de actualización/rollback y revisión visual/rendimiento pendientes; ver docs/WINDOWS_POLISH_STATUS.md.

## 1.8.51

- **Efectos GPU Activos por Defecto (macOS & Windows)**:
  - Los visualizadores acelerados por hardware GPU (Túnel Neón, Superficie Líquida y Partículas Cósmicas) impulsados por Shaders SkSL y compilados nativamente a Apple Metal (macOS) y Direct3D (Windows) ahora están activados por defecto.
- **Rediseño Glass del Menú de Opciones en el Mini Reproductor (Widget macOS)**:
  - Eliminación total del popup rectangular blanco que desentonaba con el widget.
  - Nuevo diseño de menú flotante en cristal oscuro translúcido (`liquid obsidian glass`), bordes finos iluminados (`0.5.dp`), esquinas redondeadas (`14.dp`) e iconografía vectorial en alta definición.
- **Reproductor de Música Local Estilo Winamp en Ajustes (macOS & Windows)**:
  - Nueva sección interactiva en Ajustes para reproducir música local descargada.
  - Selección de carpeta con conteo instantáneo de canciones detectadas (MP3, FLAC, WAV, M4A, AAC, OGG).
  - Botones directos para **«▶ Reproducir carpeta ahora»** y **«🔀 Reproducir en aleatorio (Shuffle)»** al más puro estilo Winamp, además de acceso rápido a la biblioteca local completa.

## 1.8.50

- **Actualizaciones Automáticas en Windows**:
  - Nuevo módulo nativo de actualización para Windows (`DesktopWindowsUpdater`) con comprobación periódica de versiones en GitHub Releases.
  - Soporte de comprobación automática, descarga en segundo plano con barra de progreso interactiva e instalación/reinicio automático en un clic.
  - Integración completa en el panel de Ajustes tanto en Windows como en macOS.
- **Corrección de Audio Post-Actualización en macOS**:
  - Cierre ordenado y liberación completa de la línea de salida CoreAudio (`SourceDataLine`) y servicios multimedia antes del reinicio de instalación de Sparkle.
  - Mecanismo de reintento progresivo y recuperación automática en caliente del canal de audio: si la línea de salida del sistema no está abierta al reanudar la reproducción o al pulsar reproducir, el motor reactiva automáticamente la salida sin requerir reiniciar la aplicación.
- **Rediseño Estético Glass Translúcido & Localización en Español**:
  - Panel de Ajustes rediseñado: secciones agrupadas en elegantes tarjetas de cristal oscuro translúcido (`liquid obsidian glass`) con sutiles bordes iluminados y tipografía ordenada.
  - Menús contextuales (clic derecho) y desplegables modernizados con estética translúcida, bordes refinados y traducción al 100% en español.
  - Menú de bandeja del sistema / barra de tareas y widget mini reproductor con diseño de cristal, logo oficial de MaviMusic y controles intuitivos en español.

## 1.8.49

- Windows: tamaño inicial y mínimo ajustados al área útil, sin aplicar el escalado DPI dos veces. Reajuste al cambiar de monitor.
- Pantalla completa nativa compatible con el marco estándar de Windows, límites exactos del monitor y restauración de la ventana anterior.
- Barra inferior adaptativa en Windows: controles secundarios distribuidos en dos filas cuando falta ancho, conservando todas las funciones.
- El visualizador deja de animarse en Windows al pausar, minimizar u ocultar la ventana.
- Pruebas automáticas de geometría y una prueba nativa de ventana Windows para comprobar pantalla completa y restauración.
- Entorno de pruebas Linux alineado con las dependencias nativas y el display virtual usados en Release.

La validación automática no sustituye la comprobación visual de DPI, barra de tareas y rendimiento en distintos equipos Windows.

## 1.8.48

- **Experiencia Nativa y Rendimiento en Windows**:
  - Ventana 100% nativa con aceleración gráfica por hardware Direct3D (`skiko.renderApi=DIRECT3D`), eliminando retrasos y alcanzando 60-120 FPS fluidos.
  - Eliminación de botones simulados de macOS en Windows: integración con los botones estándar y marco nativo del sistema operativo (minimizar, maximizar y cerrar a la derecha).
  - Corrección de escalado High-DPI y pantalla completa: la barra superior (búsqueda, botón Azar y perfil) se adapta sin solapamientos ni desbordes.
  - Icono y widget en barra de tareas / bandeja del sistema: carga del logo oficial PNG de MaviMusic en la bandeja de Windows y apertura del Mini Reproductor Glass flotante posicionado sobre la barra de tareas.
- **Calidad de Estudio en Modo DJ (Automix)**:
  - Eliminación del filtrado comb / flanger artificial ("sonido metálico") y bitcrusher de 3 bits ("chicharreo") en transiciones.
  - Supresión del bucle repetidor de micro-segmentos (beat-roll) y paso directo transparente cuando Key Lock no está activo.
  - Transiciones impecables de alta fidelidad acústica mediante crossfades de volumen limpios, isolators de ecualización DJ de 3 bandas y reverberación Schroeder suave, óptimos para audífonos de alta definición como AirPods Pro 2.

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
