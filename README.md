<div align="center">
<img src="Logo.png" alt="MaviMusic" width="160" />

# MaviMusic

**Reproductor de música de alta fidelidad para macOS y Windows. Distribución Linux temporalmente pausada.**

[![Última versión](https://img.shields.io/github/v/release/EmersonSMM/MaviMusic?style=for-the-badge&labelColor=0d1117&color=6366f1)](https://github.com/EmersonSMM/MaviMusic/releases)
[![Licencia](https://img.shields.io/badge/licencia-GPL--3.0-blue?style=for-the-badge&labelColor=0d1117)](LICENSE)
[![Plataformas](https://img.shields.io/badge/plataformas-macOS%20%7C%20Windows-emerald?style=for-the-badge&labelColor=0d1117)](https://github.com/EmersonSMM/MaviMusic/releases)

[Descargas](https://github.com/EmersonSMM/MaviMusic/releases) · [Novedades (Changelog)](CHANGELOG.md) · [Guía de Escritorio](DESKTOP.md) · [Contribución](CONTRIBUTING.md)
</div>

---

## Características Principales

### 🎧 Motor de Audio de Alta Fidelidad (32-bit Float)
- **Pipeline en Coma Flotante de 32 bits**: Headroom ampliado procesado mediante FFmpeg nativo, eliminando cualquier distorsión interna o clipping digital.
- **Reproducción Continua (Gapless) y Crossfade**: Transición ininterrumpida entre temas con curvas de fundido configurables.
- **Ecualizador Paramétrico y Gráfico**: Convivencia armónica de tone controls (graves, agudos) con ecualización de precisión.

### 🎚️ AutoDJ Inteligente y Key Lock
- **Key Lock (Time-Stretching)**: Modifica la velocidad o tempo de reproducción sin alterar el tono musical, utilizando el filtro de alta fidelidad `atempo` de FFmpeg.
- **Automix Adaptativo con IA**: Detección de BPM y compases en segundo plano mediante modelos ONNX integrados. Transiciones de mezcla progresiva cuando las pistas comparten tempo compatible (rango seguro de ±8%).

### 🎨 Visualizadores Reactivos con Shaders GPU (SkSL)
- **Shaders GPU Acelerados por Hardware**: Renderizados fluidos a 60 o 120 FPS mediante SkSL (*Skia Shading Language*).
- **Estilos Inmersivos**:
  - *Túnel de Neón*: Animación geométrica reactiva a frecuencias graves.
  - *Superficie de Agua*: Mallas dinámicas ondulantes impulsadas por graves, medios y agudos.
  - *Partículas Cósmicas*: Sistema de partículas espectrales con paletas de color adaptativas derivadas de la carátula.
- **Eficiencia Energética**: Pausa total y liberación instantánea de memoria gráfica al minimizar la ventana o bloquear la pantalla.

### 🎤 Modo Karaoke Instantáneo
- Supresión de voces en tiempo real mediante cancelación adaptativa de fase estéreo y filtrado paso banda (300 Hz - 3.4 kHz) con preservación del rango sub-grave.
- Activación instantánea con cero latencia añadida.

### 📡 Soporte AirPlay 2 con Compensación de Latencia
- Enrutamiento inteligente a altavoces y receptores AirPlay (HomePod, Apple TV).
- Búfer de amortiguamiento ajustable (0 a 3000 ms) para mantener la sincronización visual exacta de las letras y la barra de reproducción.

### 🎛️ AutoEQ: Corrección Paramétrica de Auriculares
- Perfiles de ecualización para cientos de modelos de auriculares populares.
- **Protección contra Saturación (Negative Preamp)**: Ajuste automático de ganancia para garantizar que ningún realce provoque saturación en la salida final.
- Comparación instantánea A/B y memorización del perfil según el dispositivo de audio conectado.

### 📋 Playlists Inteligentes (Smart Rules)
- Creación de listas automáticas basadas en reglas lógicas flexibles (Y / O): reproducciones, fecha de adición, Me gusta, género, año, BPM, artista y álbum.
- Plantillas listas para usar (*Más escuchadas del mes*, *Favoritas rápidas*, *Olvidadas*).
- Actualización dinámica en segundo plano sin ralentizar la interfaz.

### 📊 Estadísticas Locales y Wrapped Privado
- Métricas y resumen de hábitos de reproducción (canciones más escuchadas, horas acumuladas, artistas principales).
- **100 % Local y Privado**: Procesamiento exclusivo en el equipo; ningún dato de telemetría o escucha sale de tu ordenador.

### 🖥️ Integración Nativa con el Sistema Operativo
- **macOS**:
  - Mini-Player compacto integrado en la barra de menús con carátula y controles.
  - Soporte de `MPRemoteCommandCenter` para control desde teclado y auriculares (AirPods).
  - Integración con Spotlight y menú contextual en el Dock.
- **Windows**:
  - Integración nativa con SMTC (System Media Transport Controls) y teclas multimedia globales.
  - Instalador MSI por usuario; EXE de compatibilidad solo al publicar.
- **Linux**:
  - Código conservado para reactivar Linux más adelante; distribución y CI de escritorio pausados.

### ⬇️ Descargas y Metadatos de Alta Calidad
- Descarga a MP3 de 320 kbps con carátula de alta resolución incrustada y etiquetas ID3 enriquecidas.
- Letras sincronizadas en tiempo real (LRC) con traducción y romanización automática.

---

## Instalación y Descargas

Las releases existentes siguen disponibles en [Releases](https://github.com/EmersonSMM/MaviMusic/releases).
Durante el testeo interno no se publica al hacer commits ni crear tags. Las compilaciones manuales producen solo estos instaladores; [política y comandos](docs/DESKTOP_RELEASES.md).

| Plataforma | Paquete | Descripción |
|---|---|---|
| **macOS (Apple Silicon)** | `MaviMusic-<versión>-macos-arm64.dmg` | Para Mac con chips M1, M2, M3, M4 o superior. |
| **macOS (Intel)** | `MaviMusic-<versión>-macos-x64.dmg` | Para ordenadores Mac con procesadores Intel. |
| **Windows** | `MaviMusic-<versión>-windows-x64.msi` | Instalador por usuario para Windows 10 / 11; también usado por el actualizador. |
| **Windows (compatibilidad)** | `MaviMusic-<versión>-windows-x64-setup.exe` | Solo en publicaciones públicas, para clientes antiguos que descargan EXE. |

> [!NOTE]
> **macOS Gatekeeper:** Si al abrir por primera vez una versión compilada o no notarizada macOS muestra un aviso de desarrollador no identificado, retira el atributo de cuarentena ejecutando en la Terminal:
> ```bash
> xattr -cr /Applications/MaviMusic.app
> ```

---

## Compilación y Desarrollo

### Requisitos
- **JDK 21** (Temurin 21 recomendado).
- **macOS**: Herramientas de línea de comandos de Xcode (`clang++`).
- **Linux / Windows**: CMake 3.20+, Ninja y compilador C++17 (GCC/MinGW o MSVC).

### Comandos de Compilación

```bash
# Ejecutar la aplicación en modo desarrollo
./gradlew :desktopApp:run

# Ejecutar el conjunto completo de pruebas unitarias y de integración
./gradlew :shared:jvmTest :desktopUi:jvmTest :desktopApp:test

# Empaquetar el instalador para la plataforma actual
./gradlew :desktopApp:packageDmg      # En macOS
./gradlew :desktopApp:packageMsi      # En Windows
```

Para más detalles sobre la arquitectura de compilación, consulta [DESKTOP.md](DESKTOP.md).

---

## Estructura del Proyecto

- `desktopApp/`: Estado principal de la aplicación, motor de audio, integración con el SO, visualizadores SkSL y servicios nativos.
- `desktopUi/`: Componentes gráficos en Compose Multiplatform (reproductor, letras inmersivas, panel de cola, páginas).
- `shared/`: Modelos de datos, algoritmos de cola, red, clientes de metadatos y lógica multiplataforma.
- `native/analyzer/`: Núcleo DSP en C++ de análisis de audio y puentes JNI para Automix y beat detection.

---

## Licencia y Atribución

Este proyecto está bajo licencia **GNU General Public License v3.0** ([LICENSE](LICENSE)).
Los componentes y modelos de terceros conservan sus respectivos avisos de licencia y atribución de copyright en sus carpetas de origen. MaviMusic es un proyecto independiente sin afiliación oficial con Google, YouTube o Spotify.
