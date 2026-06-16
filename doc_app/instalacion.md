# Instalación y arranque

[← Índice](README.md) · [README principal](../README.md)

---

## Requisitos

| Herramienta | Versión | Obligatorio |
|-------------|---------|-------------|
| JDK | 21 | Sí |
| Maven | 3.8+ | Sí |
| Python | 3.10+ | Sí (al crear/corregir ejercicios) |
| pip + `requirements-docs.txt` | — | Solo si usas PDF + Gemini |
| Docker Desktop / Docker Engine | — | No; solo para el contenedor de práctica |

```bash
java -version
mvn -version
python3 --version
```

## Instalación guiada (recomendado)

Los scripts comprueban requisitos, crean carpetas locales, compilan el JAR, ofrecen instalar dependencias Python y guían la configuración opcional de Gemini y Docker.

| Sistema | Instalación | Arranque posterior |
|---------|-------------|--------------------|
| Linux / macOS | `chmod +x install.sh iniciar-forja.sh && ./install.sh` | `./iniciar-forja.sh` |
| Windows | `.\install.bat` (doble clic o desde cmd/PowerShell) | `.\iniciar-forja.bat` |

### Windows: cómo lanzar el instalador

En la carpeta `examenforge` tienes dos formas (elige una):

**Opción A — recomendada (sin tocar nada):** doble clic en `install.bat`, o desde cmd/PowerShell:

```bat
install.bat
```

`install.bat` arranca PowerShell con `-ExecutionPolicy Bypass` **solo para esa ejecución**, así que **no** necesitas escribir `Set-ExecutionPolicy`.

**Opción B — ejecutar `install.ps1` directamente:** Windows bloquea los scripts `.ps1` por defecto, así que **antes** hay que habilitar la ejecución en esa sesión de PowerShell:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\install.ps1
```

> `-Scope Process` solo afecta a la ventana de PowerShell actual; no cambia la configuración del sistema. Equivale a la línea única `powershell -NoProfile -ExecutionPolicy Bypass -File .\install.ps1`.

En resumen: con `install.bat` no hace falta `Set-ExecutionPolicy`; si ejecutas `install.ps1` a mano, sí.

Notas de Windows:

- Maven se descarga en `tools/apache-maven-3.9.16/` si no está instalado, porque `winget` no siempre ofrece Maven de forma fiable.
- El instalador compila usando `web\pom.xml` con ruta absoluta; no hace falta entrar en `web\`.
- La ruta de Python se guarda en `.env` con barras normales (`C:/Program Files/Python313/python.exe`) para evitar errores de escape.
- Docker es opcional. Si `winget` falla por *«El hash del instalador no coincide»*, instala Docker Desktop manualmente o ejecuta `winget install Docker.DockerDesktop --ignore-security-hash` en PowerShell normal, no como administrador.

## Arrancar la web

Tras instalar:

| Sistema | Comando recomendado | Alternativa |
|---------|---------------------|-------------|
| Linux / macOS | `./iniciar-forja.sh` | `cd web && mvn spring-boot:run` |
| Windows | `.\iniciar-forja.bat` | `java -jar web\target\forjaexamenes-web-1.0.0.jar` |

También puedes arrancar con Maven desde la raíz del proyecto:

```bash
mvn -f web/pom.xml spring-boot:run
```

En Windows:

```powershell
mvn -f web\pom.xml spring-boot:run
```

Espera `Started AplicacionForjaExamenes` y abre **http://localhost:8080**. **Detener:** `Ctrl+C` en la misma terminal.

> El `pom.xml` está en **`web/`**, no en la raíz. Si Maven muestra *«no POM in this directory»*, usa `-f web/pom.xml` o entra antes en `cd web`.

## Arranque manual sin instalador

```bash
cd examenforge
chmod +x arrancar-web.sh    # solo la primera vez
./arrancar-web.sh
```

Este script arranca la web en Linux/macOS sin crear un JAR previo.

## Acceso inicial

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Alumno | `alumno` | `practica` |
| Alumno (demo) | `demo` | `demo` |
| **Profesor** | `profesor` | `profesor` |

Para **cambiar estas contraseñas antes del primer arranque**, edita
[`web/src/main/resources/application.properties`](../web/src/main/resources/application.properties)
(o `.env` con `FORJAEXAMENES_USUARIOS`). Detalle en [Cuenta y perfil → Cambiar contraseñas](cuenta-perfil.md#cambiar-contraseñas-por-defecto).

- **Ayuda:** http://localhost:8080/como-funciona

## Contenedor de práctica

La web se ejecuta en el host. Docker sirve solo para el **entorno shell** de práctica (`docker`, `redes`, `sistemas`, `git`):

```bash
cd examenforge
docker compose up -d --build practica
docker exec -it forjaexamenes-practica bash
```

- Usuario del contenedor: `alumno` / `practica`
- Carpeta en el host: `datos-practica/` → `/home/alumno/practica` dentro del contenedor

> El servicio `web` del `docker-compose.yml` no monta Python ni el proyecto completo; no uses `docker compose up web` como sustituto de `./arrancar-web.sh`.

---

[← Índice de documentación](README.md)
