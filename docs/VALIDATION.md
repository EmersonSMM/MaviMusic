# Verificación — 23 de octubre de 2026

## Actualización 1.8.36 — actualizaciones firmadas desde GitHub

- 563 pruebas de escritorio aprobadas y una integración de Keychain omitida; tres pruebas nuevas verifican el checkpoint de reinicio y la detección del bundle instalado.
- Seis pruebas de publicación aprobadas: selección exacta del changelog, identidad/versión/arquitectura/clave del bundle, protección de la clave privada y rechazo del archivo Sparkle alterado. Las pruebas criptográficas usan claves temporales y el `sign_update` real; alterar el ZIP o las novedades invalida su firma.
- Puente JNI de Sparkle compilado para ARM64 y cargado correctamente en un proceso Java aislado, sin iniciar MaviMusic ni leer las preferencias reales.
- Sparkle 2.10.0 descargado con SHA-256 fijado. Bundle con feed y clave pública, comprobación de firma del feed y verificación previa a extracción; framework con sus symlinks conservados y firma ad hoc del bundle verificada de forma estricta.
- `actionlint` 1.7.12 valida los workflows Check y Release. El workflow incorpora firma Ed25519 obligatoria y firma Developer ID/notarización opcionales, con credenciales solo en CI.
- El repositorio público responde, pero no hay una release publicada. El acceso por navegador a la configuración de GitHub fue denegado: no se subió el secreto ni se activó el canal remoto. No se prueba una instalación automática de extremo a extremo sin una release de prueba y la configuración del servidor.
- Distribución local: 1.8.36 para Mac ARM64. La distribución local usa firma ad hoc; no se afirma firma Developer ID ni notarización de Apple.

## Actualización 1.8.35 — reproductor de archivos locales

- 560 pruebas de escritorio aprobadas (101 shared, 42 desktopUi y 417 desktopApp); la integración nativa de Keychain permanece omitida. Se añaden seis pruebas del reproductor local.
- MP3 real con ruta Unicode y símbolo `%`: título, artista, álbum, número de pista, duración, carátula legible y decodificación PCM verificados. Los bytes del original permanecen idénticos. Una carátula eliminada de caché se recupera desde el archivo.
- FLAC y Opus reales: se verifican las etiquetas del contenedor y del stream. Archivos sin etiquetas usan el nombre como alternativa y una cabecera corrupta no rompe la exploración.
- Importación de carpetas y archivos: persistencia de referencias, ausencia de duplicados, orden de pistas, incorporación de archivos nuevos, exclusión de archivos eliminados y rechazo de extensiones de video. Añadir carpetas no solicita iniciar reproducción.
- Las peticiones de apertura quedan pendientes hasta que existe el consumidor de la interfaz. Se verifica que las identidades locales ausentes fallan antes de acceder a adaptadores de streaming.
- El bundle macOS 1.8.35 declara tipos de documento de audio y rol Viewer/Alternate; contiene el lector de etiquetas, el receptor de archivos y un punto de entrada que acepta argumentos.
- Se compila el instalador ARM64 sin iniciar la app sobre las preferencias reales. Las pruebas automáticas no sustituyen una comprobación interactiva de Finder, selector y arrastre en una instalación real.

## Actualización 1.8.34 — retirada de los destinos móviles

- Se retiran más de 760 archivos de los módulos móviles y su UI duplicada: unos 31,4 MiB netos, descontando los modelos y puentes JNI trasladados sin modificar. Además se limpian recursos sin referencias y documentación obsoleta.
- El grafo Gradle contiene únicamente `desktopApp`, `desktopUi` y `shared`, también con variables de SDK presentes. No quedan plugins AGP, targets móviles ni overrides R8.
- Los hashes SHA-256 de ambos modelos ONNX y de los tres archivos JNI coinciden antes y después del traslado. El analizador nativo se recompila y las pruebas de ejecución del modelo siguen pasando.
- Se limpia y recompila `shared` y `desktopUi`; el nuevo namespace de recursos pertenece al escritorio. Se mantienen los 33 archivos de catálogo y se retiran solo claves sin referencias en el código; no se detectan accesos de catálogo con claves dinámicas.
- Identidad Discord de escritorio validada para macOS, Windows y Linux. Se retiran el reloj por reflexión móvil y las rutas de descarga/adapter exclusivas de los destinos eliminados.
- Escritorio: 101 pruebas shared, 42 desktopUi y 412 desktopApp; 554 aprobadas y 1 integración nativa de Keychain omitida deliberadamente.
- Web: comprobación de tipos, 119 pruebas y compilación de la app y extensiones Chrome/Firefox aprobadas.
- Backend: 46 pruebas/subpruebas y 6 paquetes aprobados; `go vet` sin errores. Invitaciones y presencia adaptadas al escritorio.
- Distribución ARM64: `MaviMusic-1.8.34.dmg`. No se inicia sobre las preferencias reales durante la validación.

La reducción corresponde al repositorio y a su configuración de compilación. La distribución de escritorio sigue necesitando su runtime, FFmpeg y los modelos de Automix.

## Actualización 1.8.33 — duplicado de «Hey Ma»

- Se reprodujo antes de corregir: 3 de 4 pruebas del caso fallaban. La conjunción «y» no separaba artistas, por lo que «Pitbull y J Balvin» y «J Balvin y Pitbull» no compartían créditos normalizados.
- Se comparan créditos localizados/reordenados y los artistas estructurados; «con…», «feat…» y «with…» entre paréntesis son créditos, mientras que idioma/remix siguen formando parte de la identidad de grabación.
- La coincidencia exacta de título/artista admite hasta 90 segundos de intro/outro cuando una entrada es video; se conserva el margen anterior para dos canciones de catálogo.
- La generación de radio siempre incluye el tema semilla entre las exclusiones. La restauración de cola, las transiciones y el añadido de sugerencias eliminan las repeticiones automáticas visibles, conservando posición actual, historial y entradas explícitas.
- Pruebas nuevas: «Hey Ma» con créditos reordenados y colaboración localizada, video sin flag de video, intro/outro, cola guardada/restaurada, cola visible, siguiente pista, repeticiones manuales y distinción español/inglés/remix.
- Suite: 97 pruebas shared, 42 desktopUi y 412 desktopApp; 550 aprobadas, 1 integración nativa de Keychain omitida deliberadamente, sin fallos.
- Distribución ARM64: `MaviMusic-1.8.33.dmg`.

## Actualización 1.8.32

- Cola de descargas extraída a un scheduler con dependencias inyectables: cancelación/reencolado inmediato, espera de limpieza, rechazo de callbacks antiguos, reintento con contexto y mantenimiento de cuatro workers.
- Catálogo central de descargas: más de 200 entradas conservadas, persistencia sin suscriptores de eventos y actualización reactiva de las reparaciones. Pruebas de verificación atrasada, eliminación y reparación concurrentes y fallos de escritura.
- Sesiones: pruebas de validación atrasada después de cambiar de cuenta, conservación de cuenta activa al borrar otra, eliminación completa de cookies fragmentadas y rechazo de renovación desde un perfil que ahora contiene otra cuenta.
- Búsqueda de canción opcional: fallback ante fallo de red con origen de video explícito; cancelación propagada.
- Las pruebas de bookkeeping de la cola utilizan transferencias suspendidas inyectadas y no inician descargas de producción.
- Suite: 93 pruebas shared, 42 desktopUi y 408 desktopApp; 542 aprobadas, 1 integración nativa de Keychain omitida deliberadamente, sin fallos.
- Distribución ARM64: `MaviMusic-1.8.32.dmg`. Se verifica el contenedor sin iniciar la app ni consultar el llavero real.

## Actualización 1.8.31

- Sesión reactiva y estados de comprobación, perfil guardado, sesión activa y sesión caducada; recuperación manual sin reiniciar ni consultas automáticas a Keychain.
- Descargas: preferencia por la versión canción cuando existe coincidencia, contexto capturado al encolar, carpetas de artista/álbum/playlist y protección frente a nombres inválidos o colisiones.
- Etapas, velocidad y tiempo restante medidos; resultados con etiquetas y carátula verificadas, información pendiente y tiempos por etapa.
- Integración con servidor HTTP local y FFmpeg/FFprobe reales: video → canción de catálogo → MP3 con álbum/pista/carátula en carpeta de playlist; fallo de carátula → reintento sin una segunda transferencia; exportación a otra carpeta reutilizando el MP3 sin borrar el anterior; cancelación propagada sin publicar un archivo terminado.
- Next y Automix comparten destino; pruebas de duplicado final, acentos, créditos, VEVO, pequeñas variaciones, títulos distintos, otras versiones y repeticiones manuales.
- Módulos de navegación y biblioteca extraídos con pruebas de Back, límite de historial, paginación y edición de playlists sin perder otros datos.
- Suite: 93 pruebas shared, 42 desktopUi y 392 desktopApp; 526 aprobadas, 1 integración nativa de Keychain omitida deliberadamente, sin fallos.
- Distribución ARM64: `MaviMusic-1.8.31.dmg`. No se arranca sobre las preferencias reales para probarla. Las pruebas utilizan `MemoryPreferencesFactory`.

Los tiempos y la concurrencia se verifican con un servidor local; no se afirma una velocidad específica contra Google. La coincidencia de grabaciones es heurística y prefiere conservar versiones explícitas cuando hay dudas. Las funciones de Google con una cuenta real y la aplicación completa en Intel requieren validación en esos entornos.

## Actualización 1.8.30

- Se revierte el acceso automático al llavero: las sesiones vuelven al almacenamiento local anterior. La recuperación de las sesiones ya migradas requiere abrir la opción en Ajustes → Cuenta y confirmar antes de cualquier consulta nativa.
- Las pruebas normales no consultan Keychain. La integración nativa queda omitida salvo habilitación explícita.
- Se valida la exportación con una carátula WebP obtenida por HTTP sin caché, etiquetas del catálogo y datos de álbum aportados por la colección.
- Se preservan las etiquetas de origen y las imágenes ya incrustadas. La reparación de MP3 copia los paquetes de audio; una comparación SHA-256 confirma que no se recomprimen.
- FFprobe verifica audio, duración, tags esperados y carátula antes de publicar cada exportación.
- Se añade Ajustes → Descargas → Reparar carátulas y etiquetas para actualizar archivos registrados existentes.
- Suite de esa versión: 75 pruebas shared, 42 desktopUi y 376 desktopApp; 492 aprobadas, 1 integración nativa de Keychain omitida deliberadamente, sin fallos.

## Verificación anterior — 1.8.29


Entorno validado: macOS ARM64, JDK 21 y Node 24.

| Comprobación | Resultado |
|---|---|
| `:shared:jvmTest` | 75 pruebas aprobadas |
| `:desktopUi:jvmTest` | 42 pruebas aprobadas |
| `:desktopApp:test` | 370 pruebas aprobadas |
| Web: comprobación de tipos, pruebas y compilación | 119 pruebas aprobadas; aplicación y extensiones Chrome/Firefox compiladas |
| Total | 606 pruebas aprobadas |
| Distribución | `MaviMusic-1.8.29.dmg` generado |
| Arranque del paquete | Inicio correcto, ventana nativa instalada y datos de biblioteca cargados |
| Identidad | Sin referencias heredadas en fuentes, recursos ni contenido de los JAR propios |
| Bibliotecas macOS ARM64 e Intel | Analizador y puente multimedia/Keychain compilados para ambas arquitecturas |

Las pruebas nuevas cubren la escritura, lectura y eliminación de una credencial sintética en Keychain; el rechazo del fallback sin protección; la migración de preferencias y sus tipos; el aislamiento de las preferencias durante las pruebas; la conversión real WebM→MP3, audio decodificable, carátula y metadatos; la recuperación de conversiones sin descargar de nuevo; las transferencias paralelas y reanudables; y la ejecución del modelo ONNX incluido.

La prueba de arranque ejecutó la migración autorizada sobre las preferencias reales: macOS ignora `java.util.prefs.userRoot`. Tras detectarlo, se sustituyó ese mecanismo por una fábrica de preferencias en memoria para las pruebas y se añadió una prueba que verifica el aislamiento. La suite final pasó con ese aislamiento. La aplicación instalada debe actualizarse con el nuevo DMG para utilizar el nuevo espacio de preferencias.

No se midió la velocidad contra el CDN de Google en una descarga real. La mejora de transferencia se verificó con un servidor local que controla rangos y concurrencia. La compilación de bibliotecas Intel no equivale a probar la aplicación completa en un Mac Intel. Android y el backend Go no se compilaron en este entorno.

La limpieza de identidad afecta al árbol actual y la distribución. El historial de Git y los avisos de autoría se conservan.
