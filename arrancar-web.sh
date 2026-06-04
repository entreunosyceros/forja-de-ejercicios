#!/usr/bin/env bash
# Arranca Spring Boot desde la raíz de examenforge (el pom.xml está en web/).
# Sin argumentos: mvn clean spring-boot:run
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")" && pwd)"
cd "$RAIZ/web"

if [ $# -eq 0 ]; then
  echo "→ mvn clean spring-boot:run  (pasa otros goals como argumentos si quieres)"
  set -- clean spring-boot:run
fi

exec mvn "$@"
