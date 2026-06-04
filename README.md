# Forja de ejercicios — *luego es tarde... para estudiar*

<img width="768" height="419" alt="forja-de-examenes" src="https://github.com/user-attachments/assets/7afefe21-33a0-4796-8376-8ce24c1ba2b2" />

Genera ejercicios prácticos al azar, permite practicar en Docker y corrige la respuesta del alumno con criterios verificables. La interfaz es **Spring Boot**; la generación y corrección las hace **Python** (`generador.py`, `evaluador.py`, `modelo_ejercicio.py`).

**Guía en la web (sin login):** con la aplicación arrancada, abre **http://localhost:8080/como-funciona** — también enlazada desde la pantalla de login.

---

## Arranque rápido

### Requisitos

| Herramienta | Versión | Obligatorio |
|-------------|---------|-------------|
| JDK | 21 | Sí |
| Maven | 3.8+ | Sí |
| Python | 3.10+ | Sí (al crear/corregir ejercicios) |
| pip + `requirements-docs.txt` | — | Solo si usas PDF + Gemini |

```bash
java -version
mvn -version
python3 --version
```

### Poner la web en marcha

```bash
cd examenforge
chmod +x arrancar-web.sh    # solo la primera vez
./arrancar-web.sh
```

Espera `Started AplicacionForjaExamenes` y abre **http://localhost:8080**.

- **Login:** `alumno` / `practica` o `demo` / `demo`
- **Ayuda:** http://localhost:8080/como-funciona

> El `pom.xml` está en **`web/`**. Usa `./arrancar-web.sh` o `cd web && mvn spring-boot:run`.

**Detener:** `Ctrl+C` en la misma terminal.

### Contenedor de práctica (Docker)

La aplicación web se ejecuta en el host (apartado anterior). Docker sirve solo para el **entorno shell** de práctica:

```bash
cd examenforge
docker compose up -d --build practica
docker exec -it forjaexamenes-practica bash
```

- Usuario del contenedor: `alumno` / `practica`
- Carpeta en el host: `datos-practica/` → `/home/alumno/practica` dentro del contenedor

> El servicio `web` del `docker-compose.yml` no monta Python ni el proyecto completo; no uses `docker compose up web` como sustituto de `./arrancar-web.sh`.

---

## Datos locales (no en Git)

Estos ficheros se generan al usar la app y están en `.gitignore` (no deben subirse a GitHub):

| Ruta | Contenido |
|------|-----------|
| `datos/estadisticas/*.json` | Estadísticas por usuario (intentos, notas, racha…) |
| `datos/usuarios.json` | Perfiles y contraseñas si se editan desde la web |
| `datos-practica/` | Archivos del alumno en el contenedor de práctica |
| `indice/*.json` | Índice de PDFs indexados |
| `banco/pendientes/` | Propuestas Gemini pendientes de revisión |
| `.env` | Clave de Gemini y secretos |

Solo se versionan los `.gitkeep` de las carpetas vacías.

---

## Qué hace la aplicación (resumen)

1. El alumno elige un módulo en la portada (POO, SQL, Docker, apuntes PDF, banco verificado…).
2. La web ejecuta `generador.py` y muestra un enunciado (con nombre y fecha del alumno).
3. El alumno escribe la respuesta; `evaluador.py` aplica criterios y calcula la nota (0–10, aprueba ≥ 5).
4. Opcional: contenedor Docker para practicar comandos reales.
5. Estadísticas de uso y progreso local (historial, medallas) en la portada.

Los módulos clásicos (`poo`, `bd_sql`, `docker`, etc.) **no necesitan** Gemini.

---

## Cuenta y perfil

| Acción | Dónde |
|--------|--------|
| Entrar | `/login` |
| Cambiar nombre, usuario o contraseña | `/perfil` (enlace **Perfil** en la cabecera) |
| Guía técnica (Gemini, PDF, nuevos módulos) | `/perfil` (sección inferior) o `/como-funciona` |
| Revisar propuestas IA (profesor) | Entrar como `profesor` / `profesor` → `/profesor/revisar` |
| Vista rápida del banco | `/profesor/banco` (misma cuenta profesor) |

Usuarios por defecto: `alumno`, `demo`, `profesor` (ver `application.properties` o `FORJAEXAMENES_USUARIOS`). Los logins con rol profesor se configuran en `forjaexamenes.login.profesores` / `FORJAEXAMENES_PROFESORES`. Los datos viven en `datos/usuarios.json` (no se sube a Git).

### Revisión del banco (profesor)

1. Activa `forjaexamenes.gemini-guardar-pendientes=true` para acumular propuestas en `banco/pendientes/`.
2. Entra con la cuenta **profesor** (`profesor` / `profesor` por defecto).
3. Abre **Revisar propuestas** (`/profesor/revisar/{id}`): tabla comparativa Gemini vs criterios, pruebas automáticas y alias de comandos.
4. Descarga el **paquete ZIP** o aprueba/rechaza desde la misma pantalla.

CLI equivalente:

```bash
python3 herramientas/revision_profesor.py --id <id_pendiente>
python3 herramientas/paquete_entrega.py <id_pendiente>
```

Los sinónimos de comandos (`docker run` ↔ `docker container run`, etc.) están en `vocabulario_claves.json` → sección `alias_comandos`.

---

## Módulos disponibles

| Módulo | Contenido |
|--------|-----------|
| `poo` | POO Java (certificado) |
| `bd_sql`, `bd_modelo`, `bd_transacciones`, `bd_jdbc`, `bd` | Bases de datos |
| `redes`, `sistemas`, `docker`, `git` | Infraestructura |
| `docs_*` | Apuntes indexados + Gemini (tras indexar PDFs) |
| `banco_*` | Ejercicios JSON en `banco/aprobados/` |

CLI:

```bash
python3 generador.py -m poo --formateado
python3 evaluador.py -e escenario.json -r "respuesta del alumno"
```

---

## Apuntes en PDF + Gemini (opcional)

```bash
cd examenforge
cp .env.example .env          # GEMINI_API_KEY real desde https://aistudio.google.com/apikey
pip install -r requirements-docs.txt
```

Coloca PDFs en `documentacion/<tema>/` → módulo `docs_<tema>` (p. ej. `documentacion/docker/` → `docs_docker`).

**Desde la web:** en la portada, *Apuntes del profesor* → **Subir e indexar** (alumno o profesor). Tras subir, se indexa automáticamente.

Indexación manual: al arrancar la web, botón **Actualizar apuntes**, o `python3 indexador_docs.py`.

Detalle de carpetas: **[documentacion/README.md](documentacion/README.md)**.

---

## Arquitectura de ejercicios

| Tipo | Origen | Corrección |
|------|--------|------------|
| **A — plantilla** | `generador.py` | `regex` definidos en código |
| **B — apuntes + IA** | PDF → Gemini → `modelo_ejercicio.py` | `contiene_todos` / `contiene_alguno` |
| **Banco** | `banco/aprobados/*.json` | Tipos definidos en el JSON |

Gemini solo propone `tema`, `pregunta`, `palabras_clave` (y opcional `variantes`). El sistema valida con [vocabulario_claves.json](vocabulario_claves.json) y exige que cada clave aparezca en el fragmento del PDF.

### Calidad de `palabras_clave`

| Regla | Detalle |
|-------|---------|
| Mínimo | 2 claves distintas |
| Técnica | Al menos una con comando/flag (≥6 caracteres, `-`, `/`, etc.) |
| Prohibidas | Lista negra global + por módulo |
| Fragmento | Cada clave debe estar en el texto indexado |

### Banco y aprobación

```
banco/aprobados/     → publicados (portada + generador)
banco/pendientes/    → propuestas Gemini en espera
banco/catalogo.json  → índice (se regenera al arrancar)
```

| Propiedad / variable | Efecto |
|----------------------|--------|
| `forjaexamenes.gemini-guardar-pendientes=true` | Guarda cada ejercicio `docs_*` en pendientes |
| `forjaexamenes.gemini-solo-aprobados=true` | Solo ejercicios del banco (sin Gemini en vivo) |
| `FORJAEXAMENES_MODO_PROFESOR=true` | `/profesor/banco` + solución visible |

CLI del banco:

```bash
python3 herramientas/revisar_banco.py listar
python3 herramientas/revisar_banco.py aprobar <id>
python3 herramientas/revisar_banco.py rechazar <id>
python3 herramientas/revisar_banco.py reindexar
```

---

## Estructura del proyecto

```
examenforge/
├── arrancar-web.sh
├── generador.py / evaluador.py
├── generador_gemini.py / generador_docs.py
├── modelo_ejercicio.py / banco_loader.py
├── vocabulario_claves.json
├── documentacion/          # PDFs de entrada
├── indice/                 # JSON indexado
├── banco/
│   ├── aprobados/
│   ├── pendientes/
│   └── catalogo.json
├── herramientas/revisar_banco.py
├── datos/                  # usuarios, estadísticas
└── web/                    # Spring Boot
    └── src/main/resources/templates/
        ├── login.html
        ├── como-funciona.html   # guía pública
        └── fragments/
```

---

## Configuración útil (`application.properties` / entorno)

| Clave | Descripción |
|-------|-------------|
| `forjaexamenes.raiz` | Raíz del proyecto (por defecto `../` desde `web/`) |
| `forjaexamenes.modo-profesor` | `FORJAEXAMENES_MODO_PROFESOR` |
| `forjaexamenes.gemini-guardar-pendientes` | Cola de revisión |
| `forjaexamenes.gemini-solo-aprobados` | Sin generación Gemini en vivo |
| `forjaexamenes.subida-pdf-max-mb` | Máximo MB por PDF subido (default 30) |
| `forjaexamenes.login.usuarios` | `FORJAEXAMENES_USUARIOS` |

---

## Problemas frecuentes

| Síntoma | Qué hacer |
|---------|-----------|
| `no POM in this directory` | `./arrancar-web.sh` o `cd web` antes de `mvn` |
| Puerto 8080 ocupado | `fuser -k 8080/tcp` o cambiar `server.port` |
| `API_KEY_INVALID` | Clave real en `.env`; reinicia; `unset GEMINI_API_KEY` en el shell si molesta |
| Cuota Gemini `429` | `FORJAEXAMENES_GEMINI_MODEL=gemini-2.5-flash` en `.env` |
| Error tras «Actualizar apuntes» | Reinicia la app (sesión solo por cookie) |
| `generador.py falló` | `python3 generador.py -m poo` desde `examenforge/` |

---

## Ampliar el proyecto

| Objetivo | Dónde |
|----------|--------|
| Texto de la guía pública | `templates/fragments/guia-funcionamiento.html` |
| Botón en portada | `templates/inicio.html` |
| Nuevo módulo plantilla | `generador.py` + `pruebas/pruebas_generador_evaluador.py` |
| Nuevo tema PDF | `documentacion/<tema>/` + indexar |
| Ejercicio verificado manual | `banco/aprobados/<tema>/<id>.json` + `reindexar` |

---

## Pruebas

```bash
python3 pruebas/pruebas_generador_evaluador.py
python3 pruebas/pruebas_modelo_ejercicio.py
python3 pruebas/pruebas_evaluador_tipos.py
python3 pruebas/pruebas_banco_loader.py
python3 pruebas/pruebas_indexador_docs.py
cd web && mvn test
```

---

## Licencia

Ver [LICENSE](LICENSE).
