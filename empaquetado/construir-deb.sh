#!/usr/bin/env bash
# Construye el paquete .deb de Forja de ejercicios.
# Uso: ./empaquetado/construir-deb.sh
# Salida: dist/forjaexamenes_VERSION_all.deb
# Desarrollado por entreunosyceros - 2026

set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${FORJA_DEB_VERSION:-1.0.0}"
PKG_NAME="forjaexamenes"
ARCH="all"
OUT_DIR="$RAIZ/dist"
STAGE="$OUT_DIR/deb-stage"
DEB_ROOT="$STAGE/$PKG_NAME"
JAR_SRC="$RAIZ/web/target/forjaexamenes-web-${VERSION}.jar"
ICON_SRC="$RAIZ/web/src/img/forja-de-examenes.png"

info() { printf '→ %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

command -v dpkg-deb >/dev/null || die "Falta dpkg-deb (paquete dpkg-dev)"
command -v convert >/dev/null || die "Falta convert (ImageMagick) para generar iconos"

if [[ ! -f "$JAR_SRC" ]]; then
  info "No hay JAR; compilando con Maven…"
  (cd "$RAIZ/web" && mvn -q -DskipTests package)
fi
[[ -f "$JAR_SRC" ]] || die "No se encontró $JAR_SRC"
[[ -f "$ICON_SRC" ]] || die "No se encontró el logo $ICON_SRC"

rm -rf "$DEB_ROOT"
mkdir -p "$DEB_ROOT"/{DEBIAN,usr/bin,usr/share/forjaexamenes/plantillas}
mkdir -p "$DEB_ROOT"/usr/share/applications
mkdir -p "$DEB_ROOT"/usr/share/doc/forjaexamenes
mkdir -p "$DEB_ROOT"/usr/share/icons/hicolor/{16x16,32x32,48x48,64x64,128x128,256x256}/apps

info "Copiando aplicación a /usr/share/forjaexamenes…"
cp -a "$JAR_SRC" "$DEB_ROOT/usr/share/forjaexamenes/forjaexamenes-web.jar"

PY_FILES=(
  alias_comandos.py banco_loader.py comun.py corrector_ia.py criterios.py
  evaluador.py generador.py generador_docs.py generador_gemini.py
  indexador_docs.py modelo_ejercicio.py motor_plantillas.py
  retroalimentacion_criterios.py tipo_materia.py util_texto.py
)
for f in "${PY_FILES[@]}"; do
  cp -a "$RAIZ/$f" "$DEB_ROOT/usr/share/forjaexamenes/"
done

JSON_FILES=(
  esquema_escenario.json vocabulario_claves.json reglas_retroalimentacion_regex.json
)
for f in "${JSON_FILES[@]}"; do
  cp -a "$RAIZ/$f" "$DEB_ROOT/usr/share/forjaexamenes/"
done

cp -a "$RAIZ/plantillas/"*.json "$DEB_ROOT/usr/share/forjaexamenes/plantillas/"
cp -a "$RAIZ/requirements-docs.txt" "$DEB_ROOT/usr/share/forjaexamenes/"
cp -a "$RAIZ/.env.example" "$DEB_ROOT/usr/share/forjaexamenes/" 2>/dev/null || true
install -m 644 "$ICON_SRC" "$DEB_ROOT/usr/share/forjaexamenes/forja-de-examenes.png"
printf '%s\n' "$VERSION" > "$DEB_ROOT/usr/share/forjaexamenes/VERSION"

# Esqueleto de carpetas de datos (solo .gitkeep / README)
mkdir -p "$DEB_ROOT/usr/share/forjaexamenes/skel"/{datos/tmp,datos/estadisticas,datos/historial,datos/entregas,banco/aprobados,banco/pendientes,indice,documentacion,examenes/paquetes,datos-practica}
if [[ -f "$RAIZ/banco/aprobados/docker/ejemplo.json" ]]; then
  mkdir -p "$DEB_ROOT/usr/share/forjaexamenes/skel/banco/aprobados/docker"
  cp -a "$RAIZ/banco/aprobados/docker/ejemplo.json" \
    "$DEB_ROOT/usr/share/forjaexamenes/skel/banco/aprobados/docker/"
fi
if [[ -f "$RAIZ/banco/README.md" ]]; then
  cp -a "$RAIZ/banco/README.md" "$DEB_ROOT/usr/share/forjaexamenes/skel/banco/"
fi

info "Generando iconos desde el logo…"
for size in 16 32 48 64 128 256; do
  convert "$ICON_SRC" -resize "${size}x${size}" -background none -gravity center \
    -extent "${size}x${size}" \
    "$DEB_ROOT/usr/share/icons/hicolor/${size}x${size}/apps/forjaexamenes.png"
done
# Escalable: no meter PNG raster aquí (lintian); el .desktop usa Icon=forjaexamenes
mkdir -p "$DEB_ROOT/usr/share/pixmaps"
convert "$ICON_SRC" -resize 128x128 -background none -gravity center -extent 128x128 \
  "$DEB_ROOT/usr/share/pixmaps/forjaexamenes.png"

info "Instalando launcher y escritorio…"
install -m 755 "$RAIZ/empaquetado/forjaexamenes-launcher.sh" \
  "$DEB_ROOT/usr/bin/forjaexamenes"
install -m 644 "$RAIZ/empaquetado/forjaexamenes.desktop" \
  "$DEB_ROOT/usr/share/applications/forjaexamenes.desktop"

cat > "$DEB_ROOT/usr/share/doc/forjaexamenes/copyright" <<'EOF'
Format: https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
Upstream-Name: Forja de ejercicios
Source: https://github.com/entreunosyceros/forja-de-ejercicios

Files: *
Copyright: 2026 entreunosyceros
License: GPL-3.0-or-later

License: GPL-3.0-or-later
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
EOF

{
  echo "forjaexamenes ($VERSION) unstable; urgency=medium"
  echo
  echo "  * Empaquetado inicial .deb con icono de escritorio."
  echo
  echo " -- entreunosyceros <entreunosyceros@users.noreply.github.com>  $(date -R)"
} | gzip -9n > "$DEB_ROOT/usr/share/doc/forjaexamenes/changelog.gz"

cat > "$DEB_ROOT/usr/share/doc/forjaexamenes/README.Debian" <<'EOF'
Forja de ejercicios
===================

Arranque:
  forjaexamenes
  # o desde el menú de aplicaciones: «Forja de ejercicios»

Datos de usuario (banco, estadísticas, PDFs…):
  ~/.local/share/forjaexamenes/

Parar el servidor:
  forjaexamenes --stop

PDF + Gemini (opcional):
  python3 -m venv ~/.local/share/forjaexamenes/.venv
  ~/.local/share/forjaexamenes/.venv/bin/pip install -r /usr/share/forjaexamenes/requirements-docs.txt

URL: http://127.0.0.1:8080
Usuarios por defecto: alumno/practica · profesor/profesor
EOF
gzip -9n -c "$DEB_ROOT/usr/share/doc/forjaexamenes/README.Debian" \
  > "$DEB_ROOT/usr/share/doc/forjaexamenes/README.Debian.gz" || true

# Tamaño instalado aproximado (kB)
INSTALLED_SIZE="$(du -sk "$DEB_ROOT" | awk '{print $1}')"

cat > "$DEB_ROOT/DEBIAN/control" <<EOF
Package: ${PKG_NAME}
Version: ${VERSION}
Section: education
Priority: optional
Architecture: ${ARCH}
Installed-Size: ${INSTALLED_SIZE}
Depends: python3 (>= 3.10), openjdk-21-jre | openjdk-21-jre-headless | java21-runtime
Recommends: python3-pip, python3-venv
Suggests: docker.io | docker-ce
Maintainer: entreunosyceros <entreunosyceros@users.noreply.github.com>
Homepage: https://github.com/entreunosyceros/forja-de-ejercicios
Description: Generador y corrector de ejercicios prácticos (Forja)
 Forja de ejercicios genera problemas al azar (POO, SQL, Docker, apuntes PDF…),
 permite practicar y corrige con criterios verificables.
 .
 Interfaz web Spring Boot + motor Python. Tras instalar, ejecuta
 «forjaexamenes» o usa el icono del menú. Abre http://127.0.0.1:8080
EOF

cat > "$DEB_ROOT/DEBIAN/postinst" <<'EOF'
#!/bin/sh
set -e
if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database -q /usr/share/applications || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
  gtk-update-icon-cache -q /usr/share/icons/hicolor 2>/dev/null || true
fi
echo "Forja de ejercicios instalada. Arranca con: forjaexamenes"
exit 0
EOF
chmod 755 "$DEB_ROOT/DEBIAN/postinst"

cat > "$DEB_ROOT/DEBIAN/postrm" <<'EOF'
#!/bin/sh
set -e
if [ "$1" = remove ] || [ "$1" = purge ]; then
  if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database -q /usr/share/applications || true
  fi
fi
exit 0
EOF
chmod 755 "$DEB_ROOT/DEBIAN/postrm"

# Permisos de ficheros empaquetados
find "$DEB_ROOT" -type d -exec chmod 755 {} +
find "$DEB_ROOT" -type f -exec chmod 644 {} +
find "$DEB_ROOT/usr/share/forjaexamenes" -type f -name '*.py' -exec chmod 644 {} +
chmod 755 "$DEB_ROOT/usr/bin/forjaexamenes"
chmod 755 "$DEB_ROOT/DEBIAN/postinst" "$DEB_ROOT/DEBIAN/postrm"

DEB_FILE="$OUT_DIR/${PKG_NAME}_${VERSION}_${ARCH}.deb"
info "Construyendo $DEB_FILE …"
fakeroot dpkg-deb --build --root-owner-group "$DEB_ROOT" "$DEB_FILE"

info "Comprobando paquete…"
dpkg-deb -I "$DEB_FILE"
echo
set +o pipefail
dpkg-deb -c "$DEB_FILE" | head -40
set -o pipefail
echo "…"
ls -lh "$DEB_FILE"
info "Listo: $DEB_FILE"
info "Instalar: sudo apt install ./dist/${PKG_NAME}_${VERSION}_${ARCH}.deb"
