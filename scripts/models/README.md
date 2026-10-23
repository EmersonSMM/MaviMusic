# Compatibilidad de los modelos de Automix

`beat_this_int8.onnx` conserva el modelo y sus escalas de cuantización. Sus cuatro nodos
`ConvInteger` usan pesos y puntos cero `uint8`, compatibles con el kernel u8/u8 de
ONNX Runtime 1.23.2 en macOS Intel. Para cada peso, `(w + 128) - (z + 128)` es
exactamente `w - z`; las multiplicaciones enteras y la precisión se conservan.
Los pesos de `MatMulInteger` no se modifican.

La transformación reproducible está en `normalize_integer_convolutions.py`:

```sh
python3 -m venv /tmp/mavimusic-model-tools
/tmp/mavimusic-model-tools/bin/pip install onnx==1.20.1
/tmp/mavimusic-model-tools/bin/python scripts/models/normalize_integer_convolutions.py entrada.onnx salida.onnx
```

El script valida el grafo con ONNX y comprueba la reversibilidad de cada tensor.
Sobre un modelo ya normalizado no modifica las convoluciones. No forma parte del
arranque ni de las dependencias de la aplicación.

Validación: inferencia real del modelo de beats en JVM Intel (Rosetta, ORT 1.23.2),
y comparación del modelo original y normalizado en Apple Silicon (ORT 1.29.0):
con entradas deterministas de 200 y 501 frames, ambas salidas tuvieron error
absoluto máximo 0.0. Las pruebas JVM de Automix también ejecutan el modelo incluido.
