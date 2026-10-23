<div align="center">
<img src="Logo.png" alt="MaviMusic" width="160" />

# MaviMusic

Reproductor de música con integración nativa en macOS.

[Descargas](https://github.com/EmersonSMM/MaviMusic/releases) · [Repositorio](https://github.com/EmersonSMM/MaviMusic) · [Licencia](LICENSE)
</div>

## Funciones

- Búsqueda y reproducción de YouTube Music, biblioteca local y fuentes configurables.
- Archivos MP3 desde Finder («Abrir con»), selección múltiple con ⌘O/Ctrl+O o arrastre a la ventana; carpetas locales, etiquetas, carátulas, álbumes en orden de pistas y playlists locales.
- Sesiones locales de Google y Spotify, sin solicitudes de llavero al iniciar. Recuperación manual opcional de las sesiones migradas por la versión anterior.
- Descargas a MP3 de 320 kbps con FFmpeg incluido; preferencia por la versión canción, Google/YouTube como origen directo opcional, bloques paralelos y reanudación.
- Carpetas de artistas, álbumes y playlists; etapas, velocidad, tiempo restante y comprobación de carátula y etiquetas.
- Registro de descargas sin el límite anterior de 200 pistas, guardado en segundo plano y actualizado al reparar metadatos.
- Cola y Automix evitan repetir una grabación como video musical; se respetan los temas añadidos manualmente.
- Crossfade, reproducción continua, ecualizador, normalización y Automix con análisis nativo y modelos ONNX incluidos.
- Letras sincronizadas, traducción, romanización, modo inmersivo y visualizadores.
- Paleta de comandos, Spotlight, controles multimedia, menú del Dock y temporizador de apagado.
- Actualizador integrado de macOS con novedades y paquetes firmados desde GitHub; consulta [la configuración de publicación](docs/UPDATES.md).
- Playlists, cola, historial, descargas en lote e integraciones de scrobbling.

Convertir a MP3 no aumenta la calidad del audio original. Las pistas de proveedores específicos conservan su fuente.

## Desarrollo en macOS

Se requiere JDK 21 y las herramientas de línea de comandos de Xcode. Automix se compila con Clang; CMake no es necesario en macOS.

```sh
./gradlew :desktopApp:run
./gradlew :shared:jvmTest :desktopUi:jvmTest :desktopApp:test
./gradlew :desktopApp:packageDmg
```

Consulta [DESKTOP.md](DESKTOP.md) y [docs/MIGRATION.md](docs/MIGRATION.md) para configuración, arquitectura y verificación.

## Componentes adicionales

El repositorio contiene la aplicación de escritorio (`desktopApp`), su interfaz (`desktopUi`), el núcleo JVM (`shared`), el cliente web con extensión (`web`) y el servidor de escucha sincronizada (`backend`). Los modelos de Automix pertenecen a `desktopApp/src/main/resources/models`; el DSP y sus puentes JNI están en `native/analyzer`.

Los servicios alojados deben configurarse con dominios propios antes de desplegarlos. No se utiliza el servidor del proyecto anterior. Los dominios `.invalid` de ejemplos y pruebas son marcadores reservados, no servicios publicados.

## Licencia y atribución

Este proyecto se distribuye bajo la licencia de [LICENSE](LICENSE). Se conservan los avisos de copyright y las licencias de los autores y componentes heredados dentro de sus archivos. Los componentes derivados de Orchard mantienen sus avisos AGPL; los modelos conservan sus licencias respectivas. MaviMusic es independiente de Google, YouTube y Spotify.
