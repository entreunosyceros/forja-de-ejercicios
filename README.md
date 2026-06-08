# Forja de ejercicios — *luego es tarde... para estudiar*

<img width="768" height="419" alt="forja-de-examenes" src="https://github.com/user-attachments/assets/7afefe21-33a0-4796-8376-8ce24c1ba2b2" />

Genera ejercicios prácticos al azar, permite practicar en Docker y corrige la respuesta del alumno con criterios verificables. La interfaz es **Spring Boot**; la generación y corrección las hace **Python** (`generador.py`, `evaluador.py`, `modelo_ejercicio.py`).

**Guía en la web (sin login):** con la aplicación arrancada, abre **http://localhost:8080/como-funciona** — también enlazada desde la pantalla de login.

---

## Instalación guiada (recomendado)

Scripts que comprueban requisitos, compilan la web, instalan dependencias Python y te guían por **Gemini** y **Docker** (ambos opcionales).

### Linux / macOS

```bash
cd examenforge
chmod +x install.sh iniciar-forja.sh
./install.sh
```

### Windows (PowerShell)

```powershell
cd examenforge
Set-ExecutionPolicy -Scope Process Bypass
.\install.ps1
```

El instalador:

1. Comprueba JDK 21, Maven y Python.
2. Crea carpetas locales (`datos/`, `indice/`, `banco/`, …).
3. Ofrece instalar `requirements-docs.txt` (PDF + Gemini).
4. Compila el JAR con Maven.
5. **Gemini (opcional):** enlace a [Google AI Studio](https://aistudio.google.com/apikey) y guardado en `datos/gemini.json`, o «configurar después» en **Perfil**.
6. **Docker (opcional):** detecta si está instalado y ofrece `docker compose up -d --build practica`.
7. Muestra un resumen y puede arrancar la app al terminar.

**Arrancar tras instalar:**

| Sistema | Comando |
|---------|---------|
| Linux / macOS | `./iniciar-forja.sh` |
| Windows | `.\iniciar-forja.bat` |

---

## Arranque rápido (manual)

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

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Alumno | `alumno` | `practica` |
| Alumno (demo) | `demo` | `demo` |
| **Profesor** | `profesor` | `profesor` |

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
| `datos/usuarios.json` | Perfiles, contraseñas y rol (`alumno` / `profesor`) |
| `datos/gemini.json` | Clave API de Gemini guardada desde Perfil |
| `datos-practica/` | Archivos del alumno en el contenedor de práctica |
| `indice/*.json` | Índice de PDFs indexados |
| `banco/pendientes/` | Propuestas Gemini pendientes de revisión |
| `examenes/paquetes/` | Paquetes ZIP generados para el profesor |
| `.env` | Clave de Gemini y secretos |

Solo se versionan los `.gitkeep` de las carpetas vacías.

---

## Qué hace la aplicación (resumen)

### Alumno

1. Elige un módulo en la portada (POO, SQL, Docker, apuntes PDF, banco verificado…).
2. La web ejecuta `generador.py` y muestra un enunciado (con nombre y fecha del alumno).
3. Escribe la respuesta; `evaluador.py` aplica criterios y calcula la nota (0–10, aprueba ≥ 5).
4. Opcional: contenedor Docker para practicar comandos reales.
5. En la portada: progreso local en el navegador (historial, medallas) y estadísticas de uso en el servidor.

### Profesor

1. Entra con la cuenta **profesor** (o cualquier login configurado con rol profesor).
2. Revisa propuestas de la IA en **`/profesor/revisar`** sin leer JSON crudo: tabla comparativa, casos de prueba y descarga de paquete ZIP.
3. Aprueba o rechaza ejercicios; los aprobados pasan a `banco/aprobados/` y aparecen en la portada.
4. Puede subir PDFs desde la portada, ver la solución de referencia y descargar JSON/PDF de cada ejercicio.

Los módulos clásicos (`poo`, `bd_sql`, `docker`, etc.) **no necesitan** Gemini.

---

## Cuenta y perfil

| Acción | Dónde |
|--------|--------|
| Entrar | `/login` |
| Cambiar nombre, usuario o contraseña | `/perfil` (enlace **Perfil** en la cabecera) |
| Guía técnica (Gemini, PDF, nuevos módulos) | `/perfil` (sección inferior) o `/como-funciona` |
| **Revisar propuestas IA** (profesor) | `/profesor/revisar` → detalle en `/profesor/revisar/{id}` |
| Vista rápida del banco (profesor) | `/profesor/banco` |
| Descargar paquete de revisión (profesor) | `/profesor/revisar/{id}/paquete` (ZIP) |

### Usuarios y roles

Usuarios por defecto en `application.properties` o `FORJAEXAMENES_USUARIOS`:

```properties
forjaexamenes.login.usuarios=alumno:practica,demo:demo,profesor:profesor
forjaexamenes.login.profesores=profesor
```

- Los logins listados en `forjaexamenes.login.profesores` (o `FORJAEXAMENES_PROFESORES`) tienen rol **PROFESOR** y acceden a `/profesor/**`.
- Los demás son **ALUMNO**.
- Los datos viven en `datos/usuarios.json`. Si el fichero ya existía, al arrancar se añaden usuarios nuevos definidos en propiedades (p. ej. `profesor`).

`FORJAEXAMENES_MODO_PROFESOR=true` sigue siendo útil para **mostrar la solución** antes de enviar en cualquier cuenta; la **zona de revisión** (`/profesor/*`) requiere rol profesor.

---

## Revisión del banco (profesor)

Flujo recomendado para validar ejercicios generados desde apuntes antes de publicarlos:

1. Activa `forjaexamenes.gemini-guardar-pendientes=true` → cada ejercicio `docs_*` se guarda en `banco/pendientes/`.
2. Entra como **profesor** / **profesor**.
3. Abre **Perfil → Revisar propuestas** o ve a `/profesor/revisar`.
4. Pulsa **Revisar** en un pendiente. Verás:
   - **Comparativa** propuesta Gemini vs criterios del escenario (palabras clave, enunciado).
   - **Tabla de criterios** con pruebas automáticas: solución de referencia (debe pasar), variante con sinónimo (p. ej. `docker container run`) e respuesta insuficiente (debe fallar).
   - **Alias** aplicados a cada término (desde `vocabulario_claves.json`).
5. **Aprobar y publicar**, **Rechazar** o **Descargar paquete ZIP** desde la misma pantalla.

### Herramientas CLI (equivalente a la web)

```bash
# Datos de revisión en JSON (tabla + casos de prueba)
python3 herramientas/revision_profesor.py --id <id_pendiente>

# Carpeta con revision.html, revision.json, enunciado y solución
python3 herramientas/paquete_entrega.py <id_pendiente>

# Aprobar / rechazar / listar
python3 herramientas/revisar_banco.py listar
python3 herramientas/revisar_banco.py aprobar <id>
python3 herramientas/revisar_banco.py rechazar <id>
python3 herramientas/revisar_banco.py reindexar
```

---

## Corrección elástica (alias de comandos)

Los criterios `contiene_todos` y `contiene_alguno` aceptan **sinónimos técnicos** definidos en [vocabulario_claves.json](vocabulario_claves.json) → sección `alias_comandos`.

| Canonical | También acepta (ejemplos) |
|-----------|---------------------------|
| `docker run` | `docker container run` |
| `docker compose up` | `docker-compose up` |
| `docker ps` | `docker container ls` |
| `BEGIN` | `START TRANSACTION` |

- El evaluador (`evaluador.py` + `alias_comandos.py`) expande cada término al corregir.
- Al generar criterios desde apuntes (`modelo_ejercicio.py`), las variantes se incorporan como `contiene_alguno` cuando hay alias.
- Para añadir sinónimos: edita `alias_comandos` en el JSON (frases completas, no palabras sueltas sueltas).

Tipos de criterio soportados: `regex`, `contiene_todos`, `contiene_alguno`.

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
pip install -r requirements-docs.txt
```

**Clave Gemini:** en la web → **Perfil → Clave API de Gemini** (se guarda en `datos/gemini.json`), o manualmente en `.env`:

```bash
cp .env.example .env   # GEMINI_API_KEY desde https://aistudio.google.com/apikey
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
| **B — apuntes + IA** | PDF → Gemini → `modelo_ejercicio.py` | `contiene_todos` / `contiene_alguno` + alias |
| **Banco** | `banco/aprobados/*.json` | Tipos definidos en el JSON + alias |

Gemini solo propone `tema`, `pregunta`, `palabras_clave` (y opcional `variantes`). El sistema valida con [vocabulario_claves.json](vocabulario_claves.json) (listas prohibidas/permitidas) y exige que cada clave aparezca en el fragmento del PDF.

### Dónde se construye y envía el prompt a la IA

El **único fichero** que habla con la API de Gemini es [`generador_gemini.py`](generador_gemini.py). La corrección (`evaluador.py`) **no** usa IA.

| Paso | Fichero | Función / detalle |
|------|---------|-------------------|
| 1. Elegir fragmento PDF | [`generador_docs.py`](generador_docs.py) | `generar_ejercicio_documentacion()` — lee `indice/docs_*.json` y elige un trozo de apuntes |
| 2. **Construir el prompt** | [`generador_gemini.py`](generador_gemini.py) | `_construir_prompt()` — metadatos del fragmento, nivel (1–3), reglas y esquema JSON esperado |
| 3. **Llamar a Gemini** | [`generador_gemini.py`](generador_gemini.py) | `_generar_desde_fragmento_intento()` → `cliente.models.generate_content(model=…, contents=prompt, …)` |
| 4. Validar y convertir en ejercicio | [`modelo_ejercicio.py`](modelo_ejercicio.py) | `validar_propuesta_gemini()` + `construir_escenario_desde_propuesta()` — criterios verificables |
| 5. Orquestación CLI | [`generador.py`](generador.py) | Si el módulo empieza por `docs_`, delega en `generador_docs` |
| 6. Orquestación web | `web/.../ServicioGenerador.java` | Ejecuta `generador.py` como subproceso; inyecta clave y modelo vía entorno |

**Flujo resumido:**

```
Portada / ejercicio docs_*  →  ServicioGenerador (Java)
  →  generador.py  →  generador_docs.py  →  generador_gemini.py
       (_construir_prompt + generate_content)  →  modelo_ejercicio.py  →  evaluador.py
```

- **Nivel de dificultad (1–3):** lo fija el alumno en **Perfil** (`datos/usuarios.json`); Java lo pasa a Python con `--nivel`. El prompt incluye instrucciones distintas según el nivel (`_instrucciones_nivel()` en `generador_gemini.py`).
- **Modelo y clave API:** `datos/gemini.json` o `.env`; Java las pasa al subproceso (`FORJAEXAMENES_GEMINI_MODEL`, `GEMINI_API_KEY`).
- **Para cambiar qué se le pide a la IA** (tono, reglas, formato JSON): edita `_construir_prompt()` en `generador_gemini.py`. La temperatura y `response_mime_type: application/json` están en `_generar_desde_fragmento_intento()`.
- **Pre-generación:** si está activa (`forjaexamenes.precarga-ejercicios-activa`), `ServicioPrecargaEjercicios` genera ejercicios `docs_*` en segundo plano para servirlos al instante; el prompt es el mismo.

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
banco/pendientes/    → propuestas Gemini en espera de revisión
banco/catalogo.json  → índice (se regenera al arrancar)
```

| Propiedad / variable | Efecto |
|----------------------|--------|
| `forjaexamenes.gemini-guardar-pendientes=true` | Guarda cada ejercicio `docs_*` en pendientes |
| `forjaexamenes.gemini-solo-aprobados=true` | Solo ejercicios del banco (sin Gemini en vivo) |
| `forjaexamenes.modo-profesor` / `FORJAEXAMENES_MODO_PROFESOR` | Solución visible antes de enviar |
| `forjaexamenes.login.profesores` / `FORJAEXAMENES_PROFESORES` | Logins con acceso a `/profesor/**` |

---

## Estructura del proyecto

```
examenforge/
├── install.sh / install.ps1     # Instalación guiada
├── iniciar-forja.sh / .bat      # Arranque tras instalar
├── arrancar-web.sh
├── generador.py / evaluador.py
├── generador_gemini.py / generador_docs.py
├── modelo_ejercicio.py / banco_loader.py
├── alias_comandos.py
├── vocabulario_claves.json      # prohibidas, permitidas, alias_comandos
├── documentacion/               # PDFs de entrada
├── indice/                      # JSON indexado
├── banco/
│   ├── aprobados/
│   ├── pendientes/
│   └── catalogo.json
├── herramientas/
│   ├── revisar_banco.py
│   ├── revision_profesor.py     # JSON de revisión para la web/CLI
│   └── paquete_entrega.py       # carpeta + HTML para el profesor
├── datos/                       # usuarios, estadísticas
└── web/                         # Spring Boot
    └── src/main/resources/templates/
        ├── login.html
        ├── como-funciona.html
        ├── profesor-revisar-lista.html
        ├── profesor-revisar-detalle.html
        └── fragments/
```

---

## Configuración útil (`application.properties` / entorno)

| Clave | Descripción |
|-------|-------------|
| `forjaexamenes.raiz` | Raíz del proyecto (por defecto `../` desde `web/`) |
| `forjaexamenes.python-interprete` | `FORJAEXAMENES_PYTHON_INTERPRETE` — `python3` o `python` (el instalador lo fija en `.env`) |
| `forjaexamenes.login.usuarios` | `FORJAEXAMENES_USUARIOS` — `usuario:clave` separados por coma |
| `forjaexamenes.login.profesores` | `FORJAEXAMENES_PROFESORES` — logins con rol profesor |
| `forjaexamenes.modo-profesor` | `FORJAEXAMENES_MODO_PROFESOR` — solución visible |
| `forjaexamenes.gemini-guardar-pendientes` | Cola de revisión en `banco/pendientes/` |
| `forjaexamenes.gemini-solo-aprobados` | Sin generación Gemini en vivo |
| `forjaexamenes.gemini-timeout-ms` | Timeout (ms) de la llamada HTTP a Gemini (default 60000) |
| `forjaexamenes.precarga-ejercicios-activa` | Pre-genera ejercicios `docs_*` en segundo plano |
| `forjaexamenes.precarga-por-clave` | Cuántos ejercicios listos por módulo+nivel (default 1) |
| `forjaexamenes.timeout-generador-segundos` | Tiempo máximo del subproceso `generador.py` (default 120) |
| `forjaexamenes.subida-pdf-max-mb` | Máximo MB por PDF subido (default 30) |
| `forjaexamenes.limpiar-practica-al-nuevo-ejercicio` | Vacía `datos-practica/` al empezar ejercicio shell |

---

## Sincronización y pulido

| Comportamiento | Detalle |
|----------------|---------|
| **Catálogo del banco** | Al cargar la portada, si `banco/aprobados/` cambió (CLI o web), se regenera `catalogo.json` automáticamente. Tras aprobar, se publica un evento interno. |
| **Índice de apuntes** | Caché en memoria invalidada por fecha de `indice/docs_*.json` y tras cada indexación. |
| **Actualizar apuntes** | Indexación **en segundo plano** (`@Async`): no bloquea la sesión; la portada hace polling y se recarga al terminar. |
| **Entorno Docker** | Botón «Limpiar entorno de práctica» en la portada; auto-limpieza de `datos-practica/` al iniciar ejercicios `docker`, `redes`, `sistemas`, `git` (`forjaexamenes.limpiar-practica-al-nuevo-ejercicio`, default `true`). |
| **Revisión profesor** | Badges **PASSED** / **FAILED** y resumen «listo para aprobar» en `/profesor/revisar/{id}`. |
| **PDFs problemáticos** | `indexador_docs.py` avisa si un PDF está corrupto o protegido con contraseña (se omite y continúa con el resto). |

---

## Problemas frecuentes

| Síntoma | Qué hacer |
|---------|-----------|
| `no POM in this directory` | `./arrancar-web.sh` o `cd web` antes de `mvn` |
| Puerto 8080 ocupado | `fuser -k 8080/tcp` o cambiar `server.port` |
| `API_KEY_INVALID` | Clave real en `.env`; reinicia; `unset GEMINI_API_KEY` en el shell si molesta |
| Cuota Gemini `429` | `FORJAEXAMENES_GEMINI_MODEL=gemini-2.5-flash` en `.env` |
| Indexación lenta | Es normal en PDFs grandes; espera el aviso «en segundo plano» en la portada |
| `generador.py falló` | `python3 generador.py -m poo` desde `examenforge/` |
| `403` en `/profesor/revisar` | Entra con cuenta profesor (`profesor` / `profesor`) |
| `403` al enviar ejercicio | Recarga la página (token CSRF en el formulario) |
| Sinónimo no aceptado | Añádelo en `vocabulario_claves.json` → `alias_comandos` |

---

## Ampliar el proyecto

| Objetivo | Dónde |
|----------|--------|
| **Prompt enviado a Gemini** (enunciado, reglas, JSON) | `generador_gemini.py` → `_construir_prompt()` |
| Parámetros de la llamada API (modelo, temperatura, timeout) | `generador_gemini.py` → `_generar_desde_fragmento_intento()`; timeout también en `application.properties` (`forjaexamenes.gemini-timeout-ms`) |
| Texto de la guía pública | `templates/fragments/guia-funcionamiento.html` |
| Vista de revisión profesor | `profesor-revisar-detalle.html`, `herramientas/revision_profesor.py` |
| Nuevos sinónimos de comandos | `vocabulario_claves.json` → `alias_comandos` |
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
python3 pruebas/pruebas_alias_comandos.py
python3 pruebas/pruebas_banco_loader.py
python3 pruebas/pruebas_indexador_docs.py
cd web && mvn test
```

---

## Licencia

Ver [LICENSE](LICENSE).
