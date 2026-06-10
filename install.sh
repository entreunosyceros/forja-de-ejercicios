#!/usr/bin/env bash
# Instalador de Forja de ejercicios (Linux / macOS).
# Comprueba requisitos, compila la web, opcionalmente configura Gemini y Docker.
# Desarrollado por entreunosyceros - 2026

set -euo pipefail

RAIZ="$(cd "$(dirname "$0")" && pwd)"
cd "$RAIZ"

VERDE='\033[0;32m'
AMARILLO='\033[1;33m'
AZUL='\033[0;34m'
ROJO='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${AZUL}→${NC} $*"; }
ok()    { echo -e "${VERDE}✓${NC} $*"; }
aviso() { echo -e "${AMARILLO}!${NC} $*"; }
error() { echo -e "${ROJO}✗${NC} $*" >&2; }

preguntar_si() {
    local texto="$1"
    local respuesta
    read -r -p "$texto [s/N] " respuesta
    [[ "${respuesta,,}" == "s" || "${respuesta,,}" == "si" || "${respuesta,,}" == "sí" ]]
}

actualizar_env() {
    local clave="$1"
    local valor="$2"
    local env_file="$RAIZ/.env"
    touch "$env_file"
    if grep -q "^${clave}=" "$env_file" 2>/dev/null; then
        if [[ "$(uname -s)" == "Darwin" ]]; then
            sed -i '' "s|^${clave}=.*|${clave}=${valor}|" "$env_file"
        else
            sed -i "s|^${clave}=.*|${clave}=${valor}|" "$env_file"
        fi
    else
        echo "${clave}=${valor}" >> "$env_file"
    fi
}

echo ""
echo "══════════════════════════════════════════════════════════"
echo "  Forja de ejercicios — instalación (Linux / macOS)"
echo "══════════════════════════════════════════════════════════"
echo ""

# ── 1. Requisitos ─────────────────────────────────────────────
info "Comprobando requisitos…"

if ! command -v java &>/dev/null; then
    error "No se encontró Java. Instala JDK 21 (Temurin, OpenJDK…)."
    exit 1
fi
JAVA_VER="$(java -version 2>&1 | head -1)"
if ! java -version 2>&1 | grep -qE 'version "21'; then
    aviso "Se recomienda JDK 21. Detectado: $JAVA_VER"
else
    ok "Java: $JAVA_VER"
fi

if ! command -v mvn &>/dev/null; then
    error "No se encontró Maven 3.8+. Instálalo y vuelve a ejecutar este script."
    exit 1
fi
ok "Maven: $(mvn -version 2>&1 | head -1)"

PYTHON=""
if command -v python3 &>/dev/null; then
    PYTHON="python3"
elif command -v python &>/dev/null; then
    PYTHON="python"
fi
if [[ -z "$PYTHON" ]]; then
    error "No se encontró Python 3.10+. Instálalo y vuelve a ejecutar."
    exit 1
fi
PY_VER="$("$PYTHON" --version 2>&1)"
ok "Python: $PY_VER ($PYTHON)"
actualizar_env "FORJAEXAMENES_PYTHON_INTERPRETE" "$PYTHON"

# ── 2. Carpetas locales ───────────────────────────────────────
info "Preparando carpetas de datos…"
mkdir -p datos datos/estadisticas datos-practica indice banco/aprobados banco/pendientes examenes/paquetes documentacion
ok "Carpetas listas (datos/, indice/, banco/, datos-practica/)"

# ── 3. Dependencias Python (PDF + Gemini) ─────────────────────
echo ""
info "Dependencias Python para apuntes PDF y Gemini (requirements-docs.txt)"
if preguntar_si "¿Instalar dependencias Python ahora?"; then
    "$PYTHON" -m pip install -r requirements-docs.txt
    ok "Dependencias Python instaladas"
else
    aviso "Omitido. Instálalas más tarde: $PYTHON -m pip install -r requirements-docs.txt"
fi

# ── 4. Compilar aplicación web ────────────────────────────────
echo ""
info "Compilando la aplicación web (mvn package, puede tardar unos minutos)…"
(cd web && mvn -q -DskipTests package)
ok "JAR generado: web/target/forjaexamenes-web-1.0.0.jar"

# ── 5. Gemini (opcional) ──────────────────────────────────────
echo ""
echo "── Clave API de Gemini (opcional) ──"
echo "  Sirve para ejercicios desde apuntes PDF (módulos docs_*)."
echo "  Los módulos clásicos (POO, SQL, Docker…) funcionan sin ella."
echo "  Crea una clave en: https://aistudio.google.com/apikey"
echo "  También puedes configurarla después en la web: Perfil → Clave API de Gemini"
echo ""

GEMINI_OK="no"
if preguntar_si "¿Quieres guardar una clave Gemini ahora?"; then
    read -r -s -p "Pega tu clave API (AIzaSy…): " GEMINI_KEY
    echo ""
    if [[ -n "$GEMINI_KEY" ]]; then
        read -r -p "Modelo [gemini-2.5-flash]: " GEMINI_MODEL
        GEMINI_MODEL="${GEMINI_MODEL:-gemini-2.5-flash}"
        FECHA="$(date -Iseconds 2>/dev/null || date '+%Y-%m-%dT%H:%M:%S')"
        "$PYTHON" - <<PY
import json
from pathlib import Path
cfg = {
    "apiKey": """${GEMINI_KEY}""",
    "model": """${GEMINI_MODEL}""",
    "actualizadoPor": "instalador",
    "actualizadoEn": """${FECHA}""",
}
Path("datos/gemini.json").write_text(json.dumps(cfg, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
PY
        ok "Clave guardada en datos/gemini.json"
        GEMINI_OK="sí"
    else
        aviso "Clave vacía; puedes configurarla en Perfil más tarde."
    fi
else
    aviso "Gemini omitido. Configúralo en Perfil cuando quieras."
fi

# ── 6. Docker (opcional) ──────────────────────────────────────
echo ""
echo "── Entorno de práctica en Docker (opcional) ──"
echo "  Contenedor shell para ejercicios docker, redes, sistemas y git."
echo "  La web corre en tu máquina; Docker solo para practicar comandos reales."
echo "  Linux: https://docs.docker.com/engine/install/"
echo "  macOS: Docker Desktop — https://www.docker.com/products/docker-desktop/"
echo ""

DOCKER_OK="no"
if command -v docker &>/dev/null; then
    ok "Docker CLI: $(docker --version 2>&1 | head -1)"
    if docker info &>/dev/null; then
        ok "Motor Docker en marcha."
    else
        aviso "Docker instalado pero el motor no responde."
        echo "  En macOS/Windows: abre Docker Desktop y espera a que esté listo."
        echo "  En Linux: sudo systemctl start docker"
        DOCKER_OK="cli (motor parado)"
    fi
    if docker compose version &>/dev/null || docker-compose --version &>/dev/null; then
        if docker info &>/dev/null && preguntar_si "¿Levantar el contenedor de práctica ahora (docker compose up)?"; then
            if docker compose version &>/dev/null; then
                docker compose up -d --build practica
            else
                docker-compose up -d --build practica
            fi
            ok "Contenedor forjaexamenes-practica en marcha"
            echo "  Entrar: docker exec -it forjaexamenes-practica bash"
            echo "  Usuario: alumno / practica"
            DOCKER_OK="sí"
        elif ! docker info &>/dev/null; then
            echo "  Cuando el motor esté en marcha: docker compose up -d --build practica"
        else
            aviso "Docker listo. Levántalo más tarde:"
            echo "    docker compose up -d --build practica"
        fi
    else
        aviso "Docker instalado pero sin 'docker compose'. Instala el plugin Compose."
    fi
else
    aviso "Docker no detectado. La web funcionará; la práctica en contenedor requerirá instalarlo."
fi

# ── 7. Lanzadores ─────────────────────────────────────────────
chmod +x arrancar-web.sh iniciar-forja.sh 2>/dev/null || true
ok "Lanzadores: ./iniciar-forja.sh (recomendado) o ./arrancar-web.sh"

# ── 8. Resumen ────────────────────────────────────────────────
echo ""
echo "══════════════════════════════════════════════════════════"
echo "  Instalación completada"
echo "══════════════════════════════════════════════════════════"
echo ""
echo "  Arrancar:     ./iniciar-forja.sh"
echo "  Abrir web:    http://localhost:8080"
echo "  Guía:         http://localhost:8080/como-funciona"
echo ""
echo "  Cuentas demo: alumno/practica · demo/demo · profesor/profesor"
echo ""
echo "  Resumen:"
echo "    · Python:  $PYTHON"
echo "    · Gemini:  $GEMINI_OK"
echo "    · Docker:  $DOCKER_OK"
echo ""
if [[ "$GEMINI_OK" == "no" ]]; then
    echo "  → Gemini: Perfil → Clave API de Gemini (o datos/gemini.json)"
fi
if [[ "$DOCKER_OK" == "no" ]]; then
    echo "  → Docker: docker compose up -d --build practica"
fi
echo ""

if preguntar_si "¿Arrancar la aplicación ahora?"; then
    exec "$RAIZ/iniciar-forja.sh"
fi
