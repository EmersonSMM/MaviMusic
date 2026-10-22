# 🎵 MaviMusic — Estado del Proyecto y Fases de Modernización

Este documento consolida el estado actual de las fases de modernización y optimización de **MaviMusic** en macOS.

---

## 📊 Resumen Ejecutivo del Progreso

```mermaid
flowchart LR
    F1["✅ Fase 1\nExperiencia Mac"] --> F2["✅ Fase 2\nProductividad & Descargas"]
    F2 --> F3["✅ Fase 3\nVisual & Audio Premium"]
    F3 --> F4["⏳ Fase 4\nModularización & Rendimiento"]
    style F1 fill:#22c55e,stroke:#16a34a,color:#ffffff
    style F2 fill:#22c55e,stroke:#16a34a,color:#ffffff
    style F3 fill:#22c55e,stroke:#16a34a,color:#ffffff
    style F4 fill:#eab308,stroke:#ca8a04,color:#000000
```

| Fase | Enfoque Principal | Estado | Commits Clave |
|---|---|---|---|
| **Fase 1** | Integración nativa con macOS (Dock, Notificaciones con carátula, Temporizador de apagado) | **Completada** | `fa92a7ac`, `ce4c15bd` |
| **Fase 2** | Paleta de comandos (`Cmd+K`) y Descarga masiva de álbumes/playlists a MP3 320kbps | **Completada** | `56bed5e4` |
| **Fase 3** | Rediseño panel de letras, Modo Inmersivo Apple Music cinético y Normalización LUFS | **Completada** | `67ac8a01` |
| **Fase 4** | **Salud del código, Modularización de `DesktopApp.kt` y Optimización de Rendimiento** | **PENDIENTE** | *Por ejecutar* |

---

## ✅ Fases Completadas al 100%

### Fase 1: Experiencia Mac & Utilidades Inmediatas
- **Menú contextual nativo en el Dock de macOS**: Control de reproducción con clic derecho en el ícono del Dock (*Reproducir/Pausar*, *Siguiente*, *Anterior*, *Me gusta*, estado de pista actual) mediante `NSMenu` / `NSApp.dockTile`.
- **Notificaciones nativas con carátula en segundo plano**: Soporte mediante `NSUserNotificationCenter` y `MPNowPlayingInfoCenter` / `MPMediaItemArtwork`, mostrando el banner con título, artista y arte descargado/en caché.
- **Temporizador de apagado (Sleep Timer)**: Diálogo flotante integrado con presets de 15, 30, 45, 60 minutos o al finalizar la canción actual, con cuenta regresiva en vivo.

### Fase 2: Productividad y Descargas en Masa
- **Paleta de Comandos Universal (`Cmd + K`)**: Buscador emergente ultra-rápido estilo Spotlight / Raycast para navegar a cualquier pantalla (Inicio, Explorar, Biblioteca, Ajustes), controlar la reproducción, activar el temporizador o disparar acciones.
- **Descargas masivas a MP3 320 kbps**: Botón de descarga en lote para álbumes y playlists con cola concurrente en segundo plano, tags ID3v2 completos, carátula embebida y carpeta de salida configurable (`~/Downloads/MaviMusic`).

### Fase 3: Visual & Audio Premium
- **Suavizado del panel de letras lateral**: Tarjeta flotante con bordes redondeados (`18.dp`), sombra difusa, borde sutil con gradiente tipo cristal y eliminación total de bordes cortantes y tonalidades verdosas.
- **Modo Inmersivo de Letras estilo Apple Music**:
  - Pantalla completa interactiva con fondo cinético vivo (`MeshGradientBackground`) calculado a partir de los colores de la carátula.
  - Carátula de 310 dp con sombra ambiental y controles de transporte dedicados (scrubber, play/pause circular, saltos, volumen).
  - Letras cinéticas sincronizadas con salto instantáneo al tocar estrofas (*tap-to-seek*), romanización y traducción.
- **Normalización de Volumen Automática (LUFS -14 dB & PeakLimiter)**:
  - Nivelación suave y continua de sonoridad entre pistas usando la metadata de YouTube.
  - Protección de picos mediante limitador en tiempo real para evitar cualquier distorsión o *clipping*.
  - Configurable desde Ajustes → Reproducción y conmutable mediante `Cmd + K`.

---

## ⏳ La Fase Pendiente: Fase 4

### **Fase 4: Salud del Código, Modularización de `DesktopApp.kt` y Rendimiento**

#### ¿Por qué es necesaria esta fase?
El archivo central `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt` tiene actualmente **más de 8.300 líneas de código**. En él conviven de manera entrelazada:
- La instanciación y gestión de decenas de estados globales (`remember`, `mutableStateOf`).
- Los oyentes de eventos y efectos colaterales de Compose (`LaunchedEffect`, `DisposableEffect`).
- El renderizado directo de múltiples pantallas completas (Home, Search, Explore, Library, Settings, Artist, Album, Playlist).
- Múltiples diálogos modales y popovers.
- Lógica de persistencia y cableado de atajos de teclado.

#### Objetivos Clave de la Fase 4:

1. **Desacoplamiento y extracción modular de pantallas (`screens/`)**:
   - Extraer pantallas independientes a sus propios archivos limpios:
     - `DesktopHomeScreen.kt`
     - `DesktopLibraryScreen.kt`
     - `DesktopSettingsScreen.kt`
     - `DesktopAlbumDetailScreen.kt`
     - `DesktopPlaylistDetailScreen.kt`
   - Reducir el tamaño de `DesktopApp.kt` de ~8.300 líneas a menos de 1.500 líneas (enfocado exclusivamente en el andamiaje principal de ventana y navegación).

2. **Aislamiento de Diálogos Modales (`dialogs/`)**:
   - Extraer diálogos como `SleepTimerDialog`, `AddPlaylistDialog`, `CreateFolderDialog`, etc. a componentes autónomos y reutilizables.

3. **Optimización de Recomposiciones y Rendimiento de Compose**:
   - Marcar estados estables y utilizar lambdas no capturantes en listas largas para evitar recomposiciones innecesarias de la interfaz gráfica.
   - Perfilado del consumo de memoria en el renderizado de listas extensas de pistas y carátulas.

4. **Tiempos de Compilación Graduales Reducidos**:
   - Al separar en módulos y archivos desacoplados, los cambios futuros en una pantalla solo recompilan esa unidad específica en lugar de volver a procesar el monolito entero.

---

## 💡 Mejoras Futuras Sugeridas (Backlog Opcional post-Fase 4)

Una vez completada la salud estructural del código en la Fase 4, se podrían considerar mejoras adicionales como:
- **Reproducción Crossfade / Gapless Avanzada**: Transición suave y solapada configurable de 1 a 12 segundos entre el final de una canción y el inicio de la siguiente.
- **Ecualizador Paramétrico / Gráfico de 10 Bandas**: Con presets auditivos (Rock, Pop, Bass Boost, Vocal, Jazz, Flat).
- **Integración con widgets de macOS Sonoma / Sequoia**: Widget nativo para el Centro de Notificaciones o Escritorio.
