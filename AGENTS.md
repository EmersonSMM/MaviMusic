# MaviMusic: continuidad entre equipos

- Antes de trabajar en la adaptación a Windows, lee `docs/WINDOWS_HANDOFF.md` y comprueba el commit y el estado del checkout.
- Conserva reproducción, Automix, cola, descargas con metadatos/carátula, archivos locales y actualizaciones de macOS.
- Mantén las correcciones de ventana específicas de Windows cuando corresponda. No cambies el comportamiento de macOS para simular pruebas de Windows.
- Las pruebas deben usar preferencias aisladas; no inicies la app sobre la biblioteca o sesión real para verificar cambios automáticamente. El acceso al llavero de macOS continúa siendo manual y opcional.
- Distingue pruebas automáticas, inspección de código y validación visual real. Compilar en Windows no acredita DPI, barra de tareas ni rendimiento interactivo.
- Coordina cambios mediante ramas y commits identificados. Actualiza el relevo con archivos modificados, pruebas y pendientes. No sobrescribas cambios de otros agentes.

## Fase de pruebas internas (política vigente)

- No crear tags de release ni lanzar publicaciones públicas sin una petición explícita del usuario para esa publicación. Un commit o una prueba no autoriza publicar.
- Mantener `Release` deshabilitado en GitHub entre sesiones durante esta pausa. Habilitarlo solo ante una solicitud de compilación/publicación, usando el workflow actual de main, y pausarlo al terminar.
- `Release` solo se lanza manualmente; `publish=false` por defecto. Para pruebas internas mantenerlo así; Intel es opcional. La publicación explícita exige ambas arquitecturas Mac.
- Pruebas internas rápidas: MSI Windows y DMG macOS. En publicación pública conservar también el EXE instalador para los clientes Windows antiguos (1.8.51); nunca portables. Linux de escritorio y portables quedan temporalmente fuera de CI/distribución, conservando el código Linux.
- Ver `docs/DESKTOP_RELEASES.md` antes de modificar empaquetado o actualizaciones. No recuperar el flujo antiguo de tags ni el asistente sin `--publish`.

## Documentación de mantenimiento

- Mantener manualmente la información de mantenimiento en MAINTAINERS.md. No regenerar listas de colaboradores ni paneles de avatares desde el historial de GitHub.
- Conservar los avisos de copyright, licencias y atribuciones obligatorias de los componentes heredados y de terceros.
