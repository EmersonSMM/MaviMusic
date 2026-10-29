# Android: estabilización y continuidad

Fecha: 29 de octubre de 2026.

## Base y alcance

- Base local: `3a66667e`.
- Rama: `codex/android-stability-stage-1`.
- Al comenzar, `main` estaba 12 commits por delante y 2 por detrás de la referencia local `origin/main`. No se hizo fetch, integración ni push.
- El usuario autorizó avanzar por etapas hacia una experiencia coherente con escritorio, visualizadores básicos y una base interna más fiable.
- Esta primera etapa se limita a reproducción/navegación y correcciones de presentación verificables. No incluye el rediseño ni visualizadores.

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

1. Persistencia de posición y preferencias de reproducción; coherencia de colas largas (actualmente se guardan como máximo 50 canciones).
2. Permisos de archivos locales y mensajes de denegación; completar la integración de la cola de Media3 con la cola de la app.
3. Descargas verificadas, cancelables y sin duplicados; errores de reproducción visibles y recuperación de streams.
4. Diseño compartido con PC: colores/tipografía/superficies, Inicio más compacto y paneles adaptativos. Verificar también teléfono, vertical y teclado.
5. Visualizadores básicos alimentados por datos reales de audio: espectro, onda y ambiente; medir fluidez, temperatura y consumo. No presentar animaciones decorativas como análisis de audio.
6. Conexión real con escritorio; calidad de audio conectada al resolver; CI Android. La opción «Lossless» continúa sin efecto real y requiere corrección en su etapa.

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
