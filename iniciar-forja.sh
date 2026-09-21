#!/usr/bin/env bash
# Arranca Forja de ejercicios (JAR compilado o Maven en desarrollo).
# Desarrollado por entreunosyceros - 2026

set -euo pipefail

RAIZ="$(cd "$(dirname "$0")" && pwd)"
export FORJAEXAMENES_RAIZ="$RAIZ"
# Misma política UTF-8 que iniciar-forja.bat: evita problemas de codificación
# al lanzar los scripts Python (acentos y símbolos en la salida).
export PYTHONUTF8=1
export PYTHONIOENCODING=utf-8
mkdir -p "$RAIZ/datos/tmp"
export TMPDIR="${TMPDIR:-$RAIZ/datos/tmp}"

# Si existe el venv del instalador y no hay intérprete forzado, úsalo.
if [[ -z "${FORJAEXAMENES_PYTHON_INTERPRETE:-}" && -x "$RAIZ/.venv/bin/python" ]]; then
    export FORJAEXAMENES_PYTHON_INTERPRETE="$RAIZ/.venv/bin/python"
fi

JAR="$RAIZ/web/target/forjaexamenes-web-1.0.0.jar"

if [[ -f "$JAR" ]]; then
    echo "→ Arrancando Forja de ejercicios (JAR)…"
    echo "  http://localhost:8080  (solo localhost por defecto)"
    echo "  Red/aula: FORJAEXAMENES_SERVER_ADDRESS=0.0.0.0 ./iniciar-forja.sh"
    echo "  Detener: Ctrl+C"
    exec java -jar "$JAR"
fi

echo "→ No hay JAR compilado; usando Maven (ejecuta ./install.sh para compilar)."
cd "$RAIZ/web"
exec mvn spring-boot:run
