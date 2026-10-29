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
