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

JAR="$RAIZ/web/target/forjaexamenes-web-1.0.0.jar"

if [[ -f "$JAR" ]]; then
    echo "→ Arrancando Forja de ejercicios (JAR)…"
    echo "  http://localhost:8080"
    echo "  Detener: Ctrl+C"
    exec java -jar "$JAR"
fi

echo "→ No hay JAR compilado; usando Maven (ejecuta ./install.sh para compilar)."
cd "$RAIZ/web"
exec mvn spring-boot:run
