# Empaquetado Debian (`.deb`)

Genera un paquete instalable en Ubuntu/Debian con icono de escritorio (logo `forja-de-examenes.png`).

Documentación de instalación para el usuario: [doc_app/instalacion.md](../doc_app/instalacion.md#paquete-debian--ubuntu-deb).

## Requisitos de construcción

- `dpkg-deb`, `fakeroot`
- ImageMagick (`convert`) para redimensionar el logo
- Maven + JDK 21 (para compilar el JAR si no existe)

```bash
sudo apt install dpkg-dev fakeroot imagemagick
```

## Construir

Desde la raíz de `examenforge`:

```bash
chmod +x empaquetado/construir-deb.sh empaquetado/forjaexamenes-launcher.sh
./empaquetado/construir-deb.sh
```

Salida: `dist/forjaexamenes_1.0.0_all.deb` (~27 MB; arquitectura `all`).

## Qué incluye el paquete

| Ruta en el sistema | Contenido |
|--------------------|-----------|
| `/usr/bin/forjaexamenes` | Launcher (`--daemon`, `--stop`, `--status`) |
| `/usr/share/forjaexamenes/` | JAR, scripts Python, plantillas, `requirements-docs.txt` |
| `/usr/share/applications/forjaexamenes.desktop` | Entrada del menú |
| `/usr/share/icons/hicolor/*/apps/forjaexamenes.png` | Iconos generados desde el logo |
| `/usr/share/pixmaps/forjaexamenes.png` | Icono de compatibilidad |

Al arrancar, el launcher prepara los datos del usuario en `~/.local/share/forjaexamenes/`
(copia el código Python ahí para que `banco/`, `datos/` y `plantillas/` coincidan con la raíz).

## Instalar

```bash
sudo apt install ./dist/forjaexamenes_1.0.0_all.deb
# o: sudo dpkg -i ./dist/forjaexamenes_1.0.0_all.deb && sudo apt -f install
```

| Comando | Efecto |
|---------|--------|
| `forjaexamenes` | Arranca en primer plano y abre el navegador |
| `forjaexamenes --daemon` | Segundo plano (igual que el icono del menú) |
| `forjaexamenes --stop` | Detiene el servidor en segundo plano |
| `forjaexamenes --status` | Indica si está en marcha |

URL: http://127.0.0.1:8080  
Usuarios por defecto: `alumno`/`practica` · `profesor`/`profesor`

### PDF + Gemini (opcional)

```bash
python3 -m venv ~/.local/share/forjaexamenes/.venv
~/.local/share/forjaexamenes/.venv/bin/pip install -r /usr/share/forjaexamenes/requirements-docs.txt
```

## Desinstalar

```bash
sudo apt remove forjaexamenes
# Los datos en ~/.local/share/forjaexamenes/ se conservan
```

## Dependencias del paquete

- `Depends`: `python3` (≥ 3.10), `openjdk-21-jre` \| `openjdk-21-jre-headless` \| `java21-runtime`
- `Recommends`: `python3-pip`, `python3-venv`
- `Suggests`: Docker (solo entorno de práctica)
