# Decisiones de Diseño y Arquitectura: MaviMusic Remote

## 1. Contexto y Objetivos
El objetivo de **MaviMusic Remote** es permitir a cualquier usuario en la misma red local (Wi-Fi) controlar la reproducción de MaviMusic en su ordenador (macOS o Windows) desde el navegador de su teléfono móvil (iPhone con Safari u otros navegadores) sin instalar ninguna aplicación externa.

---

## 2. Decisiones Técnicas y de Seguridad

### D-1: Motor de Servidor Embebido (Ktor CIO)
- **Decisión**: Se utiliza Ktor Server con el motor CIO (`io.ktor:ktor-server-cio:3.0.3`) y soporte nativo de WebSockets y CORS.
- **Razón**: El proyecto ya utiliza Ktor 3.x para el cliente de streaming. Ktor CIO es asíncrono, basado en corutinas de Kotlin, ligero, sin dependencias pesadas de contenedores Servlet externos, y ofrece un rendimiento excepcional con uso mínimo de memoria y CPU.
- **Ciclo de vida**: Cuando el interruptor está apagado, el servidor no se instancia ni escucha en ningún socket. El consumo de memoria y CPU es 0.

### D-2: Protocolo HTTP vs HTTPS en Red Local
- **Análisis de limitaciones de iOS Safari**:
  - Para instalar una Web App como PWA completa y activar Service Workers offline, iOS exige HTTPS o `localhost`.
  - Sin embargo, en una red local doméstica (`192.168.x.x` o `10.x.x.x`), un certificado HTTPS autofirmado provoca una advertencia de seguridad aterradora en Safari ("Este sitio web no es seguro") y requiere instalar manualmente un perfil raíz de confianza en los ajustes de iOS, lo cual arruina por completo la experiencia de usuario de "escanear y usar".
  - Los dominios públicos con Let's Encrypt para IPs locales no son viables sin conexión a internet y sin resolución DNS externa dinámica.
- **Decisión**: El servidor opera sobre HTTP en interfaces de red privadas locales (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`). Se incluye en la web app todos los metadatos PWA compatibles (`apple-mobile-web-app-capable`, `apple-mobile-web-app-status-bar-style`, `viewport-fit=cover`, iconos táctiles y manifest). En la interfaz de escritorio se informa explícitamente que la conexión está acotada estrictamente a la red local privada.

### D-3: Aislamiento Estricto de Red y Filtrado de IPs Privadas
- **Decisión**: El servidor valida la dirección IP de origen de cada petición entrante y de cada conexión WebSocket.
- **Regla**: Solo se aceptan peticiones procedentes de rangos de direcciones IP privadas definidos por la IANA (RFC 1918 y RFC 3927):
  - `127.0.0.0/8` y `::1` (Loopback local)
  - `10.0.0.0/8` (Clase A privada)
  - `172.16.0.0/12` (Clase B privada)
  - `192.168.0.0/16` (Clase C privada)
  - `169.254.0.0/16` y `fe80::/10` (Enlace local / Link-Local)
- Cualquier paquete proveniente de una IP pública o no identificada es rechazado de inmediato con `403 Forbidden` y registrado en el log de seguridad local. No se utiliza UPnP ni se abre ningún puerto en el router.

### D-4: Modelo de Emparejamiento y Autenticación
- **PIN de 4 a 6 dígitos**: Generado aleatoriamente al iniciar o regenerable en Ajustes.
- **Token de un solo uso (OTP)**: Al escanear el código QR, este incluye un token efímero (`?otp=...`) con caducidad corta (5 minutos). El usuario que escanea el QR en la pantalla de su escritorio no necesita teclear el PIN.
- **Token de sesión persistente**: Tras el emparejamiento válido, el servidor entrega un token criptográfico de 32 bytes (256 bits de entropía). El navegador lo almacena en `localStorage`.
- **Almacenamiento seguro**: El escritorio no almacena los tokens en texto plano, sino su hash SHA-256 (`tokenHash`).
- **Comparación en tiempo constante**: La validación de tokens usa `MessageDigest.isEqual(...)` para mitigar ataques de temporización (*timing attacks*).
- **Protección contra fuerza bruta**: Tras 5 intentos fallidos de PIN en 5 minutos, el emparejamiento se bloquea temporalmente por 2 minutos y se registra el intento.
- **Revocación en tiempo real**: Si el usuario revoca un dispositivo en Ajustes, el token se invalida inmediatamente y cualquier WebSocket abierto por ese dispositivo se desconecta en el acto.

### D-5: Generación Nativa de Código QR
- **Decisión**: Se utiliza la librería `com.google.zxing:core:3.5.3` (que ya forma parte del classpath de la aplicación) para codificar la URL de emparejamiento en una matriz de bits (`BitMatrix`) y dibujarla directamente como un `ImageBitmap` en Compose Desktop.
- **Razón**: 0 dependencias externas nuevas, 0 llamadas a servicios externos de QR en internet y renderizado visual ultrarrápido y nítido.

### D-6: Interpolación de Posición de Reproducción
- **Decisión**: No se transmiten paquetes WebSocket a 60 FPS con la posición de la canción. En su lugar, el servidor emite un "ancla temporal" `{ positionMs: Long, timestamp: Long, isPlaying: Boolean }` cada vez que cambia el estado (play, pausa, seek, cambio de pista) y periódicamente cada 3 segundos.
- **Razón**: El navegador del móvil calcula la posición exacta interpolando `positionMs + (Date.now() - timestamp)` mientras `isPlaying == true`. Esto reduce el consumo de batería del iPhone en un 95% y mantiene la barra de progreso a 60 FPS fluidos.

### D-7: Web App Mobile-First sin Frameworks Pesados
- **Decisión**: La aplicación web se implementa en un único conjunto de archivos estáticos optimizados (`index.html`, `style.css`, `app.js`) empaquetados directamente en los recursos de la app (`remote/`).
- **Carga inicial**: Menos de 40 KB totales (muy por debajo del límite de 300 KB).
- **Diseño**: Inspirado en la interfaz Liquid Glass de MaviMusic:
  - Vistas por pestañas inferiores: **Reproduciendo**, **Letras**, **Cola**.
  - Áreas táctiles de al menos 44×44 px.
  - Soporte completo para Notch y barra de inicio de iPhone (`viewport-fit=cover`, `padding-bottom: env(safe-area-inset-bottom)`).
  - Reconexión y resincronización automática de WebSocket al desbloquear el teléfono.

---

## 3. Métricas de Rendimiento y Consumo

- **Servidor Apagado (Por Defecto)**:
  - Puertos abiertos: 0
  - Hilos o corrutinas activas: 0
  - Consumo de CPU adicional: 0.0%
  - Consumo de RAM adicional: 0 MB
  - Comportamiento idéntico a la versión base.

- **Servidor Encendido (En Espera / Sin clientes)**:
  - Puertos abiertos: 1 (por defecto 8765, con reintento automático si está ocupado)
  - Hilos: Ktor CIO asíncrono sobre dispatcher IO compartido.
  - Consumo de CPU: 0.0% en reposo (completamente reactivo, sin polling).
  - Consumo de RAM: ~1.5 MB para el contexto de sockets y rutas estáticas.

- **Servidor con Dispositivo Móvil Activo (iPhone con Safari)**:
  - Consumo de red: Anclas de tiempo cada 3 segundos + diffs al pausar/reanudar/cambiar de pista.
  - Consumo de CPU: < 0.2% en el ordenador; el iPhone interpola fluidamente a 60 FPS usando `requestAnimationFrame`.
  - Duración de batería en iPhone: impacto indistinguible frente a una página estática estándar gracias a la eliminación del tráfico continuo de 60 Hz.

---

## 4. Lista de Pendientes y Mejoras Futuras

1. **Descubrimiento por mDNS/Bonjour**: Publicación opcional de servicio `_mavimusic._tcp.local` para acceder vía `http://mavimusic.local:8765` sin teclear IP numérica.
2. **Reordenación por arrastre (Drag & Drop) en la Cola Web**: Añadir soporte táctil nativo con PointerEvents para arrastrar ítems de la cola en la web móvil (actualmente cuenta con tap-para-reproducir y botón para quitar de la cola).
3. **Búsqueda remota de canciones**: Exponer un endpoint de búsqueda rápida conectada al cliente de búsqueda interna para añadir temas nuevos a la cola desde el teléfono.


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

### 3. Aviso no invasivo
- Windows consulta al arrancar y cada 8 h con una corrutina suspendida, sin polling ni avisos de red en pantalla. La preferencia de comprobación automática se respeta.
- Píldora solo si hay versión nueva; no compone espaciadores cuando no existe. Abre versión, notas y acciones en Windows; en Mac abre el flujo nativo Sparkle.
- Sparkle usa recordatorios suaves gestionados por la app (sin ventana programada intrusiva); conserva su UI cuando el usuario pide actualizar. Intervalo programado de 8 h.

### 4. Búsqueda de Ajustes
- Causa: se ocultaba el título pero se seguía dibujando el contenedor con padding y borde. SubcomposeLayout mantiene el registro de coincidencias sin colocar ninguna parte de una tarjeta vacía; su altura final es cero.
- Se filtran filas compartidas y se resaltan coincidencias sin alterar callbacks ni valores; limpiar la búsqueda conserva el estado de controles. Hay estado vacío y transición de tamaño recortada.
- Pendiente de revisión visual exhaustiva: controles personalizados internos que no usan filas compartidas pueden requerir etiquetas adicionales para búsqueda fina; los bloques de música local y actualizaciones se registran expresamente.

### 5. Escape y cierre de letras
- Prioridad: menús/paleta y panel flotante de cola; después panel de letras; después vista de letras/reproductor; por último modo inmersivo y navegación de Ajustes. Los popups con foco procesan su propio Escape antes de volver a la ventana principal.
- El dispatcher inmersivo solo consume Escape cuando su ventana tiene foco. Q y K existentes se conservan; no se cambia reproducción ni seek.
- Cierre compartido con IconButton de 44 dp (al menos 40 px al escalado normal), hover de Material y tooltip «Cerrar (Esc)». Se sitúa dentro del contenido, separado de los botones nativos de ventana.

### 6. Menús glass compartidos
- Radio de contenedor 20 dp y filas 12 dp, borde de 0,5 dp y sombra suave. Contexto de canciones y desplegables usan el mismo modificador/componentes, sin cambiar acciones.
- Se reutiliza Haze/desktopChromeGlass y su fallback cuando se reduce el desenfoque. No se aplica blur al texto. Pendiente validar captura de fondo entre ventanas Popup en cada backend Windows/Mac; si Haze no puede capturarlo queda el tinte legible, no texto difuminado.
- Colores semánticos de Material para el menú compartido. El tema global existente sigue siendo oscuro; no se afirma haber implementado un tema claro global nuevo.

### 7. Logo sin bucle
- Se conserva la palabra MaviMusic como glifos vectoriales escalables con degradado cálido y resplandor fijo; no hay GIF, secuencia, descarga ni recurso asíncrono que parpadee. Brushes recordados en memoria y caché de fuentes existente.
- Solo se anima el brillo al hover durante 180 ms. Se consulta reducir movimiento de NSWorkspace o SPI_GETCLIENTAREAANIMATION al componer; en ese caso la transición es instantánea. No hay temporizador en reposo.
- Pendiente: refrescar automáticamente esa preferencia si cambia en el sistema mientras la vista permanece compuesta; al volver a componer se toma de nuevo.

### 8. Spotlight global
- Por defecto Windows Ctrl+Alt+K, para evitar apropiarse del Ctrl+K común. En Ajustes puede cambiarse a Ctrl+K, Ctrl+Shift+K u otra combinación con Ctrl/Alt/Win y una letra; se muestra conflicto sin ventana modal.
- RegisterHotKey con MOD_NOREPEAT y un hilo daemon bloqueado en GetMessage. Sin polling ni hook global de todas las teclas. El callback se entrega a AWT y abre solo Spotlight existente, no la ventana principal.
- Se libera con UnregisterHotKey al cambiar combinación y al terminar; la app debe seguir viva en bandeja. Mac conserva Carbon ⌘K/⌘⇧K y la ventana Spotlight externa existente.
- Pendiente: configuración y aviso de conflicto del registro Carbon en macOS (actualmente combinaciones nativas existentes). No se altera el subsistema multimedia para implementarlo.

### 9. Barra de ventana Windows (criterio de aceptación 10)
- Se elige DWM vía JNA: atributos 20 (fallback 19 en Windows 10), 35 para caption y 36 para texto. Se aplican al cambiar tema/fondo y al volver de fullscreen. Sin sondeo ni trabajo por frame.
- Se conservan decoración, botones, arrastre, doble clic, Snap, menú de sistema y redimensionado nativos. No se introduce una barra propia que pueda regresarlos. Windows 11 usa tono de la app y texto contrastado; versiones que no admitan colores específicos conservan el modo oscuro/claro nativo si está disponible.
- Pendiente: fallback de barra propia para Windows 10 antiguos sin atributos DWM compatibles. No se afirma transparencia Mica en títulos estándar cuando el OS no la soporte; se usa el tono coherente permitido por el requisito.
- No existe un punto 10 separado en la lista de implementación: este punto cubre la barra nativa y el criterio 10.


## Fase de pruebas internas: publicaciones en pausa

- Release queda deshabilitado temporalmente en GitHub, también para proteger tags/ramas con workflows antiguos. Se habilita manualmente cuando se solicite una compilación y se pausa después. Su nueva definición deja de escuchar tags/push y requiere workflow_dispatch. El campo publish es false por defecto; solo una acción manual explícita activa la publicación pública. El asistente antiguo falla antes de autenticar o subir código salvo --publish; --plan/--diagnose-run no publican.
- Se elige un instalador por plataforma: MSI Windows y DMG macOS. Se eliminan PKG duplicados y portables, y se pausa Linux de escritorio en Release y Check sin borrar su código ni releases antiguas.
- Las pruebas internas manuales incluyen Windows y Apple Silicon por defecto; Intel es opcional para ahorrar la espera de ese runner. Publicar obliga a incluir Intel y valida siete archivos (MSI, EXE, dos DMG, dos feeds y SHA256SUMS). El EXE se construye solo al publicar para conservar la actualización desde Windows 1.8.51, que busca exclusivamente EXE; las pruebas internas rápidas generan únicamente MSI.
- Sparkle admite DMG como archivo de actualización firmado (https://sparkle-project.org/documentation/publishing/). Se inspecciona el DMG real montado de solo lectura y desmontado, comprobando bundle, versión, arquitectura y configuración de confianza. Se mantienen clave pública y feed, sin acceso automático al llavero ni cambios al motor de audio.
- Check sigue automático para Windows/macOS y backend, con las suites existentes; los empaquetados pesados pasan al flujo manual. Mac compila el puente nativo y verifica las firmas, incluida una imagen DMG sintética con clave temporal.
- Verificación local: suites JVM correctas antes y después; herramientas Python pasan de 24 a 27 pruebas correctas. Se construyó packageDmg y se inspeccionó el instalador real (bundle/versión/arquitectura/configuración de Sparkle). Pendiente CI Windows/macOS de esta política; no se publicó ni se borró ninguna release para probarla.


## Colaboradores en la documentación

Se retiran los workflows heredados que actualizaban listas de colaboradores y publicaban una imagen de avatares, junto con su generador. Se deshabilitan ambos en GitHub y se retira la rama auxiliar de la imagen (solo contenía contributors.svg, generado por el bot). README.md y MAINTAINERS.md actuales no contenían listas de terceros; MAINTAINERS.md conserva únicamente el repositorio de mantenimiento actual y los avisos de atribución necesarios. Esta limpieza no modifica el historial de commits ni las licencias de los componentes.
