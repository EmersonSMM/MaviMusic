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
