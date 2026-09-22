#!/usr/bin/env bash
# Launcher de Forja de ejercicios (paquete .deb).
# Desarrollado por entreunosyceros - 2026

set -euo pipefail

SHARE="/usr/share/forjaexamenes"
JAR="$SHARE/forjaexamenes-web.jar"
DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}/forjaexamenes"
PIDFILE="$DATA_HOME/forjaexamenes.pid"
LOGFILE="$DATA_HOME/forjaexamenes.log"
URL="http://127.0.0.1:8080"

uso() {
  cat <<EOF
Uso: forjaexamenes [opciones]

  (sin args)     Arranca el servidor (primer plano) y abre el navegador
  --daemon       Arranca en segundo plano
  --stop         Detiene el servidor en segundo plano
  --status       Muestra si está en marcha
  --no-browser   No abre el navegador
  --help         Esta ayuda

Datos: $DATA_HOME
EOF
}

preparar_datos() {
  mkdir -p "$DATA_HOME"/{datos/tmp,datos/estadisticas,datos/historial,datos/entregas}
  mkdir -p "$DATA_HOME"/{banco/aprobados,banco/pendientes,indice,documentacion}
  mkdir -p "$DATA_HOME"/{examenes/paquetes,datos-practica,plantillas}

  # Copiar código a la raíz de datos (no symlinks: Path(__file__).resolve()
  # debe coincidir con FORJAEXAMENES_RAIZ para banco/, datos/, plantillas/).
  local pkg_ver="" local_ver=""
  [[ -f "$SHARE/VERSION" ]] && pkg_ver="$(tr -d '[:space:]' <"$SHARE/VERSION")"
  [[ -f "$DATA_HOME/.package-version" ]] && local_ver="$(tr -d '[:space:]' <"$DATA_HOME/.package-version")"

  if [[ "$pkg_ver" != "$local_ver" || ! -f "$DATA_HOME/generador.py" ]]; then
    local f
    for f in "$SHARE"/*.py "$SHARE"/*.json; do
      [[ -e "$f" ]] || continue
      cp -a "$f" "$DATA_HOME/"
    done
    mkdir -p "$DATA_HOME/plantillas"
    cp -a "$SHARE/plantillas/"*.json "$DATA_HOME/plantillas/" 2>/dev/null || true
    cp -a "$SHARE/requirements-docs.txt" "$DATA_HOME/" 2>/dev/null || true
    printf '%s\n' "${pkg_ver:-unknown}" >"$DATA_HOME/.package-version"
  fi

  # Ejemplo de banco la primera vez
  if [[ ! -e "$DATA_HOME/banco/aprobados/docker/ejemplo.json" \
        && -f "$SHARE/skel/banco/aprobados/docker/ejemplo.json" ]]; then
    mkdir -p "$DATA_HOME/banco/aprobados/docker"
    cp -a "$SHARE/skel/banco/aprobados/docker/ejemplo.json" \
      "$DATA_HOME/banco/aprobados/docker/"
  fi
  if [[ ! -f "$DATA_HOME/.env" && -f "$SHARE/.env.example" ]]; then
    cp -a "$SHARE/.env.example" "$DATA_HOME/.env.example"
  fi
}

java_cmd() {
  if command -v java >/dev/null 2>&1; then
    command -v java
    return
  fi
  echo "No se encontró 'java'. Instala openjdk-21-jre." >&2
  exit 1
}

python_cmd() {
  if [[ -x "$DATA_HOME/.venv/bin/python" ]]; then
    echo "$DATA_HOME/.venv/bin/python"
  elif command -v python3 >/dev/null 2>&1; then
    command -v python3
  else
    echo "No se encontró python3." >&2
    exit 1
  fi
}

esta_vivo() {
  if [[ -f "$PIDFILE" ]]; then
    local pid
    pid="$(cat "$PIDFILE" 2>/dev/null || true)"
    if [[ -n "${pid:-}" ]] && kill -0 "$pid" 2>/dev/null; then
      return 0
    fi
  fi
  return 1
}

parar() {
  if ! esta_vivo; then
    echo "Forja no está en marcha."
    rm -f "$PIDFILE"
    exit 0
  fi
  local pid
  pid="$(cat "$PIDFILE")"
  echo "→ Deteniendo Forja (PID $pid)…"
  kill "$pid" 2>/dev/null || true
  for _ in $(seq 1 20); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 0.25
  done
  kill -9 "$pid" 2>/dev/null || true
  rm -f "$PIDFILE"
  echo "Detenida."
}

estado() {
  if esta_vivo; then
    echo "En marcha (PID $(cat "$PIDFILE")) → $URL"
  else
    echo "Parada."
    exit 1
  fi
}

abrir_navegador() {
  sleep 3
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$URL" >/dev/null 2>&1 || true
  fi
}

arrancar() {
  local daemon=0 browser=1
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --daemon) daemon=1 ;;
      --no-browser) browser=0 ;;
      *) echo "Opción desconocida: $1" >&2; uso; exit 2 ;;
    esac
    shift
  done

  if [[ ! -f "$JAR" ]]; then
    echo "Falta $JAR (¿paquete incompleto?)." >&2
    exit 1
  fi

  preparar_datos

  if esta_vivo; then
    echo "Ya está en marcha (PID $(cat "$PIDFILE")) → $URL"
    if [[ "$browser" -eq 1 ]]; then
      xdg-open "$URL" >/dev/null 2>&1 || true
    fi
    exit 0
  fi

  export FORJAEXAMENES_RAIZ="$DATA_HOME"
  export FORJAEXAMENES_PYTHON_INTERPRETE
  FORJAEXAMENES_PYTHON_INTERPRETE="$(python_cmd)"
  export PYTHONUTF8=1
  export PYTHONIOENCODING=utf-8
  export TMPDIR="${TMPDIR:-$DATA_HOME/datos/tmp}"
  mkdir -p "$TMPDIR"

  local java_bin
  java_bin="$(java_cmd)"

  echo "→ Forja de ejercicios"
  echo "  Datos: $DATA_HOME"
  echo "  URL:   $URL"
  echo "  Parar: forjaexamenes --stop  (si usaste --daemon)"

  if [[ "$browser" -eq 1 ]]; then
    abrir_navegador &
  fi

  if [[ "$daemon" -eq 1 ]]; then
    nohup "$java_bin" -jar "$JAR" >>"$LOGFILE" 2>&1 &
    echo $! >"$PIDFILE"
    echo "Arrancada en segundo plano (PID $(cat "$PIDFILE")). Log: $LOGFILE"
    exit 0
  fi

  # Primer plano: Ctrl+C detiene
  exec "$java_bin" -jar "$JAR"
}

case "${1:-}" in
  --help|-h) uso ;;
  --stop) preparar_datos; parar ;;
  --status) preparar_datos; estado ;;
  --daemon)
    shift
    arrancar --daemon "$@"
    ;;
  --no-browser)
    shift
    arrancar --no-browser "$@"
    ;;
  "") arrancar ;;
  *)
    # Permite: forjaexamenes --daemon --no-browser
    arrancar "$@"
    ;;
esac
