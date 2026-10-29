#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

export JAVA_HOME="$HOME/.jdks/temurin-21/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

echo "=== Comprobando dispositivos Android conectados ==="
adb devices -l

DEVICES=$(adb devices | grep -v "List" | grep "device$" || true)

if [ -z "$DEVICES" ]; then
    echo ""
    echo "⚠️  No se detectó ningún dispositivo Android autorizado por USB."
    echo "Asegúrate de que en el teléfono:"
    echo "1. Estén activadas las 'Opciones de desarrollador'."
    echo "2. Esté activada la 'Depuración por USB'."
    echo "3. Hayas aceptado el diálogo '¿Permitir depuración por USB desde esta computadora?'"
    exit 1
fi

echo ""
echo "Instalando MaviMusic · pruebas con datos aislados en el dispositivo..."
./gradlew :androidApp:installReview

echo "✅ App instalada exitosamente en el dispositivo."
