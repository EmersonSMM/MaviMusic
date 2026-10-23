# Migración de MaviMusic

## Actualizaciones firmadas — 1.8.36

Se incorpora Sparkle en el bundle macOS. La aplicación instalada ofrece búsqueda de versiones, novedades y actualización integrada. La comprobación periódica está habilitada; la descarga/instalación en segundo plano es opcional. Las cuentas y archivos locales conservan su formato y ubicación.

El canal remoto necesita el secreto de firma de GitHub Actions y una primera release publicada con sus feeds. La [guía de actualizaciones](UPDATES.md) detalla esa activación y las credenciales opcionales de Apple. La clave privada local queda fuera del repositorio; la app incluye únicamente la clave pública y no accede al llavero para comprobar actualizaciones.

## Reproductor local — 1.8.35

MaviMusic se registra en macOS como lector alternativo de MP3 y otros formatos de audio. Instala la app en Aplicaciones; en Finder puedes usar «Abrir con → MaviMusic». Para elegirla como reproductor predeterminado de los MP3: selecciona un MP3, abre «Obtener información», elige MaviMusic en «Abrir con» y pulsa «Cambiar todo…». La aplicación no cambia por sí sola las preferencias del sistema.

En Música local puedes abrir uno o varios archivos, añadir carpetas o arrastrarlos a la ventana. ⌘O (Ctrl+O fuera de macOS) abre el selector. Los originales permanecen en su ubicación; se guardan referencias, sin copiar, convertir ni reescribir las etiquetas. Las carpetas añadidas se incluyen en futuras exploraciones y en la vigilancia de cambios.

El lector usa FFmpeg incluido para obtener título, artista, álbum, número de pista, duración y carátula incrustada. Los archivos sin etiquetas conservan el nombre de archivo como alternativa. Las carátulas extraídas pertenecen a la caché y pueden regenerarse desde el original. Al pulsar una pista se reproduce el contexto visible de canciones, artista o álbum; los álbumes siguen sus números de pista. Se pueden crear playlists locales incluso con una cuenta conectada. Las pistas con identidad local no solicitan reproducción automática remota ni sustituyen un archivo ausente por una búsqueda en Internet.

## Proyecto de escritorio — 1.8.34

Se retiran los módulos `app`, `tv`, `wearos` y `sharedUi`, los plugins y targets de compilación móvil, la configuración de firma móvil, sus workflows y el puente FFmpeg exclusivo de ese destino. Gradle incluye únicamente `shared`, `desktopUi` y `desktopApp`; no depende de detectar un SDK instalado.

Los dos modelos ONNX se trasladan sin modificar a `desktopApp/src/main/resources/models/` y los tres puentes JNI a `native/analyzer/jni/`. Se conservan sus avisos de autoría y se actualizan las rutas de Clang y CMake. El espacio de recursos de la interfaz pasa a `com.music.mavimusic.desktopui.resources`.

El cliente web y el servidor se mantienen. Los accesos de instalación apuntan ahora al escritorio; las invitaciones utilizan el enlace de escritorio y el contador del servidor solo admite la plataforma `pc`. La integración Discord utiliza una identidad de escritorio coherente con el sistema operativo, sin los valores predeterminados móviles anteriores.

Se retiran el adaptador de letras exclusivo de TV, la reflexión del reloj móvil, la descarga a MediaStore no utilizada por el escritorio y las entradas de catálogo sin referencias en el código actual. Se conservan todos los catálogos de idioma y las funciones de reproducción y exportación de escritorio.

Los identificadores de cliente `ANDROID_*` y `TVHTML5_*` del protocolo de YouTube, los espacios de nombres AndroidX de Compose y los metadatos de plataforma de dependencias externas no representan aplicaciones móviles del proyecto y se conservan cuando el escritorio los necesita.

## Identidad

La identidad de paquetes, recursos, JNI, ejecutables, extensiones y distribución es `MaviMusic` / `com.music.mavimusic`. El remoto de distribución es `EmersonSMM/MaviMusic`. La migración cambia el árbol actual; el historial de Git conserva la procedencia del código y los avisos de autoría siguen vigentes.

Las preferencias del escritorio anterior se identifican por su esquema y se copian al nuevo espacio sin sobrescribir valores existentes. Las rutas locales de canciones guardadas se conservan. El espacio anterior solo se retira tras importar las sesiones locales. La aplicación instalada realiza esta migración al iniciarse; las pruebas usan preferencias aisladas.

## Sesiones en macOS

La versión 1.8.30 revierte el acceso automático a Keychain. Las sesiones nuevas se guardan en las preferencias locales, como antes de la migración; no tienen la protección de cifrado de Keychain. Abrir la app, leer una sesión, iniciar sesión o cerrar sesión no consulta, escribe ni elimina elementos del llavero en macOS.

Ajustes → Cuenta → Recuperar sesiones anteriores abre una explicación y pide confirmación dentro de la app. Solo después de pulsar «Recuperar mis sesiones» se consultan las claves conocidas de Google y Spotify de esta app bajo `com.music.mavimusic.sessions`. macOS puede pedir permiso durante esa recuperación. Las sesiones recuperadas se guardan localmente; los elementos del llavero se conservan y no se usan automáticamente después. Una sesión local nueva nunca se sobrescribe con la recuperada. También se puede iniciar sesión de nuevo sin recuperar nada.

La versión 1.8.31 observa los cambios de sesión en la interfaz: menú, avatar y biblioteca distinguen un perfil guardado de una sesión activa. Durante la validación se muestra «Comprobando sesión» y una sesión caducada se explica. La recuperación manual actualiza la sesión y las vistas sin reiniciar.

En 1.8.32 las validaciones y renovaciones tardías se descartan si el usuario cambió de cuenta o sesión mientras esperaban. Una ruta de perfil del navegador no acredita que siga abierta la misma cuenta de Google: la renovación debe verificar su identidad. Quitar una cuenta borra todos los fragmentos de su cookie local; quitar una cuenta inactiva conserva la cuenta y el canal seleccionados. La recuperación manual no puede sobrescribir una sesión nueva ni restaurar la cookie de una cuenta eliminada mientras se mostraba el permiso.

## Descargas y MP3

Ajustes → Descargas permite elegir Google/YouTube o las fuentes configuradas. Google se utiliza para pistas originales de YouTube y respeta que la fuente esté habilitada. Las pistas de proveedores específicos y las políticas de descarga siguen usando el registro de fuentes existente.

Al descargar un video musical se busca primero la versión canción mediante el catálogo de canciones y el comparador de artista, título y versión existente. La búsqueda tiene un límite de 15 segundos; si no encuentra una coincidencia, conserva el audio solicitado y lo explica en el resultado de la descarga. Las filas de otros proveedores conservan su origen.

Los archivos nuevos se organizan como `Artistas/<artista>/<álbum o Canciones>/`, `Álbumes/<artista de la colección>/<álbum>/` o `Playlists/<playlist>/`. El contexto se captura al encolar; cambiar de página no cambia el destino. Los álbumes incluyen número de pista cuando se conoce. Un sufijo de identidad evita sobrescribir grabaciones distintas con nombres iguales. Los componentes se sanean y se limitan por bytes UTF-8. Los archivos anteriores se conservan en su lugar; exportar una descarga ya registrada a otra colección reutiliza su audio y conserva el archivo anterior.

El gestor muestra resolución de canción, metadatos, fuente, transferencia, carátula, conversión y verificación. Durante la transferencia calcula velocidad y tiempo restante con bytes medidos. Los resultados de esta sesión indican qué etiquetas y carátula se verificaron y qué información falta, sin inventarla. Se registran tiempos por etapa. Los temporales quedan en `.mavimusic-staging` y permiten reanudar o reparar sin volver a transferir el audio. Cancelar no se presenta como una descarga completada.

Los streams que requieren rangos se leen en bloques de 512 KiB con hasta tres solicitudes simultáneas por pista. La cola conserva cuatro pistas simultáneas. Los rangos y longitudes se verifican; los bloques fallidos se reintentan y los archivos parciales se reanudan. Una descarga incompleta no se promociona como archivo terminado.

La conversión utiliza FFmpeg incluido con sus bibliotecas, salida MP3 explícita, publicación atómica y cancelación del proceso. La carátula se obtiene independientemente de la caché de la interfaz y se acepta WebP. Se preservan los tags del archivo original y se añaden título, artista, álbum y pista cuando están disponibles en el catálogo o la colección. Una comprobación con FFprobe verifica el audio, la duración, las etiquetas esperadas y la presencia de carátula antes de publicar el archivo. Un error de conversión deja el original recuperable y se muestra como descarga fallida. No se presenta un WebM como un MP3 terminado. La transcodificación a 320 kbps no recupera calidad que no existe en el origen. Ajustes → Descargas → Reparar carátulas y etiquetas actualiza las descargas registradas: los MP3 se remultiplexan copiando sus paquetes de audio, sin recomprimirlos; los WebM anteriores se convierten. Los errores conservan el original.

En 1.8.32 cada intento de descarga tiene una identidad propia. Cancelar y volver a encolar espera a que el intento anterior termine de limpiar transferencias y procesos; sus callbacks no pueden modificar el progreso del nuevo intento. Los reintentos mantienen la colección de destino y no reinician tareas que ya están ejecutándose. El guardado del registro se realiza en el worker y fuera del bloqueo de la cola; un fallo de persistencia se muestra como reintentable.

`DesktopDownloadLibrary` centraliza los cambios del registro y publica un flujo observado por la interfaz. Ya no se descartan las entradas posteriores a la 200. Las finalizaciones no dependen de que haya una ventana escuchando eventos; una reparación actualiza la vista. Una verificación lenta no puede borrar descargas o reparaciones más recientes, ni una reparación terminada puede resucitar una entrada eliminada. Al borrar desde búsqueda o el reproductor se utiliza la ruta del registro guardado, y un fallo al borrar el archivo no elimina su entrada mientras el archivo siga existiendo.

Si la búsqueda opcional de la versión canción falla, se conserva el audio solicitado y su origen de video se marca para explicarlo en el resultado. La cancelación del usuario sigue cancelando la operación completa.

## Cola y Automix

Next, el crossfade y Automix consultan la misma decisión de próxima pista. Se evita una repetición inmediata aunque el último elemento sea el video de la canción actual. La comparación normaliza acentos, créditos con separador, sufijos Topic/VEVO y marcas de video/letra; pequeñas variaciones ortográficas requieren coincidencia de artista y duración. No usa prefijos amplios que confundan títulos como «Love» y «Love Me». Las variantes se evitan consecutivamente, mientras que versiones distintas siguen siendo grabaciones distintas al generar sugerencias. Las canciones añadidas expresamente a la cola y la repetición de toda la lista conservan su intención.

La versión 1.8.33 corrige específicamente créditos como «Pitbull y J Balvin» frente a «J Balvin y Pitbull», y títulos con «con…» frente a «feat…». El tema semilla se excluye siempre de las sugerencias. Las colas guardadas se limpian de duplicados automáticos al restaurarse, y también al incorporar sugerencias o avanzar: la cola visible refleja el filtro, sin limitarse a saltar la repetición cuando llega su turno. Se conservan el historial, la pista actual y las entradas explícitas; las ediciones de otro idioma y los remixes siguen siendo grabaciones distintas al generar sugerencias.

## Automix y arquitecturas

El analizador macOS se compila con Clang desde las mismas fuentes C++ y JNI. Los modelos ONNX se empaquetan desde `desktopApp/src/main/resources/models`; los puentes JNI residen en `native/analyzer/jni`. Las pruebas en macOS fallan si falta el analizador nativo. `-Pmavimusic.arch=x86_64` selecciona arquitectura Intel para las bibliotecas; la distribución completa Intel debe construirse con un JDK Intel en un host/runner Intel. ARM64 es el destino validado localmente.

## Servicios propios

- `LISTEN_TOGETHER_SERVER`: servidor del cliente nativo; sin valor se conserva la configuración manual.
- `VITE_PARTY_SERVER`: servidor por defecto del cliente web.
- `MAVIMUSIC_SITE_ORIGIN`, `MAVIMUSIC_PAGES_ORIGIN`, `MAVIMUSIC_SITE_ORIGINS`: orígenes permitidos de la extensión.
- `JAM_ALLOWED_ORIGINS`, `JAM_PUBLIC_ORIGIN`: orígenes del servidor propio.

Los servicios no tienen un dominio público propio confirmado. Los dominios reservados de ejemplo deben reemplazarse antes de publicar. No se redirige a infraestructura ajena.

## Modularización

`DesktopTheme`, `DesktopGlass` y `DesktopWindowLayout` contienen los mismos componentes extraídos de `DesktopApp`. Las transferencias, la elección de origen, la conversión y el almacenamiento de cookies tienen unidades separadas. En 1.8.31 también se separan el historial de navegación (`DesktopNavigationState`), las ediciones de biblioteca (`DesktopLibraryEdits`), el estado de sesión, las rutas y el contexto de descargas, la preferencia de canción y las mediciones/resultados. Se conservan los puntos de entrada y callbacks de reproducción; las pruebas verifican historial, paginación, playlists, cola y exportación.
