# Forja de ejercicios — *luego es tarde... para estudiar*

<img width="768" height="419" alt="forja-de-examenes" src="https://github.com/user-attachments/assets/7afefe21-33a0-4796-8376-8ce24c1ba2b2" />

[![Wiki](https://img.shields.io/badge/Wiki-DeepWiki-blue?style=for-the-badge&logo=wikipedia)](https://deepwiki.com/entreunosyceros/forja-de-ejercicios/)

Genera ejercicios prácticos al azar (informática, idiomas u otras materias desde PDF), permite practicar en Docker y corrige la respuesta del alumno con criterios verificables. La interfaz es **Spring Boot**; la generación y corrección las hace **Python** (`generador.py`, `evaluador.py`, `modelo_ejercicio.py`, `tipo_materia.py`).

> 📘 **Documentación de referencia:** este `README`, las guías por rol ([alumno.txt](alumno.txt) · [profesor.txt](profesor.txt)) y la ayuda integrada en **`/como-funciona`** (menú **Ayuda**). La vista generada de la arquitectura en [DeepWiki](https://deepwiki.com/entreunosyceros/forja-de-ejercicios/) es complementaria; si no carga, usa las guías locales.

---

## Contenido

- [Instalación y arranque](#instalación-y-arranque)
- [Prueba rápida (2 minutos)](#prueba-rápida-2-minutos)
- [Qué hace la aplicación](#qué-hace-la-aplicación-resumen) — [Alumno](#alumno) · [Profesor](#profesor)
- [Dificultad adaptativa](#dificultad-adaptativa)
- [Instalaciones independientes (aula real)](#instalaciones-independientes-aula-real)
- [Progreso y estadísticas](#progreso-y-estadísticas-portada)
- [Compartir ejercicios del banco](#compartir-ejercicios-del-banco-carpeta-compartida)
- [Seguimiento de alumnos (profesor)](#seguimiento-de-alumnos-profesor)
- [Apuntes en PDF + Gemini](#apuntes-en-pdf--gemini-opcional)
- [Arquitectura de ejercicios](#arquitectura-de-ejercicios)
- [Cuenta y perfil](#cuenta-y-perfil) — [Cambiar contraseñas por defecto](#cambiar-contraseñas-por-defecto)
- [Configuración útil](#configuración-útil)
- [Problemas frecuentes](#problemas-frecuentes)
- [Pruebas](#pruebas)
- [Licencia](#licencia)

---

## Instalación y arranque

### Requisitos

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

### Instalación guiada (recomendado)

Los scripts comprueban requisitos, crean carpetas locales, compilan el JAR, ofrecen instalar dependencias Python y guían la configuración opcional de Gemini y Docker.

| Sistema | Instalación | Arranque posterior |
|---------|-------------|--------------------|
| Linux / macOS | `chmod +x install.sh iniciar-forja.sh && ./install.sh` | `./iniciar-forja.sh` |
| Windows | `.\install.bat` (doble clic o desde cmd/PowerShell) | `.\iniciar-forja.bat` |

En Windows, en la carpeta `examenforge`:

```bat
install.bat
```

`install.bat` lanza PowerShell con `-ExecutionPolicy Bypass` solo para esa ejecución; no hace falta `Set-ExecutionPolicy` manual. Si prefieres PowerShell directamente: `powershell -NoProfile -ExecutionPolicy Bypass -File .\install.ps1`

Notas de Windows:

- Maven se descarga en `tools/apache-maven-3.9.16/` si no está instalado, porque `winget` no siempre ofrece Maven de forma fiable.
- El instalador compila usando `web\pom.xml` con ruta absoluta; no hace falta entrar en `web\`.
- La ruta de Python se guarda en `.env` con barras normales (`C:/Program Files/Python313/python.exe`) para evitar errores de escape.
- Docker es opcional. Si `winget` falla por *«El hash del instalador no coincide»*, instala Docker Desktop manualmente o ejecuta `winget install Docker.DockerDesktop --ignore-security-hash` en PowerShell normal, no como administrador.

### Arrancar la web

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

### Arranque manual sin instalador

```bash
cd examenforge
chmod +x arrancar-web.sh    # solo la primera vez
./arrancar-web.sh
```

Este script arranca la web en Linux/macOS sin crear un JAR previo.

### Acceso inicial

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Alumno | `alumno` | `practica` |
| Alumno (demo) | `demo` | `demo` |
| **Profesor** | `profesor` | `profesor` |

Para **cambiar estas contraseñas antes del primer arranque**, edita
[`web/src/main/resources/application.properties`](web/src/main/resources/application.properties)
(o `.env` con `FORJAEXAMENES_USUARIOS`). Detalle en [Cambiar contraseñas por defecto](#cambiar-contraseñas-por-defecto).

- **Ayuda:** http://localhost:8080/como-funciona

### Contenedor de práctica

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

## Prueba rápida (2 minutos)

Con la web arrancada en **http://localhost:8080**:

1. Inicia sesión como **alumno** (`alumno` / `practica`).
2. En la portada, pulsa el módulo **POO** (o **«Empezar ahora»** para un ejercicio aleatorio).
3. Lee el enunciado y escribe tu respuesta en **«Tu solución»** (con **resaltado de sintaxis** mientras escribes). Por ejemplo, para una clase con un mensaje:

   ```java
   System.out.println("Hola");
   ```

4. Pulsa **Enviar**. Verás la **nota (0–10)**, la **retroalimentación** con los criterios cumplidos y las pistas de los que falten, y tu respuesta mostrada con el mismo resaltado.
5. Vuelve a la portada: tu intento aparece en **«Tu progreso»** y en **«Tus estadísticas de uso»**.

> Cada criterio se muestra en lenguaje claro (qué se esperaba y una pista), no como expresión regular. Repite varios ejercicios y la [dificultad se ajustará sola](#dificultad-adaptativa).

---

## Datos locales (no en Git)

Estos ficheros se generan al usar la app y están en `.gitignore` (no deben subirse a GitHub):

| Ruta | Contenido |
|------|-----------|
| `datos/estadisticas/*.json` | Estadísticas por usuario en el **servidor** (totales acumulados). Se borran con «Limpiar estadísticas» en la portada |
| `datos/entregas/<profesor>/` | Entregas JSON importadas por el profesor (carpeta compartida → panel) |
| `datos/usuarios.json` | Perfiles, contraseñas y rol (`alumno` / `profesor`) |
| `datos/gemini.json` | Clave API de Gemini guardada desde Perfil |
| `datos/preferencias-profesor.json` | Preferencias del profesor (p. ej. cola `banco/pendientes/`) |
| `datos-practica/` | Archivos del alumno en el contenedor de práctica |
| `indice/*.json` | Índice de PDFs indexados |
| `banco/pendientes/` | Propuestas Gemini pendientes de revisión |
| `examenes/paquetes/` | Paquetes ZIP generados para el profesor |
| `.env` | Clave de Gemini y secretos |

Solo se versionan los `.gitkeep` de las carpetas vacías.

---

## Qué hace la aplicación (resumen)

### Alumno

1. Elige un módulo en la portada (POO, SQL, Docker, apuntes PDF…) o **importa el banco** que el profesor dejó en la carpeta compartida.
2. La web ejecuta `generador.py` y muestra un enunciado (con nombre y fecha del alumno).
3. Escribe la respuesta en un cuadro con **resaltado de sintaxis** (Java, SQL, bash… según el módulo); `evaluador.py` aplica criterios y calcula la nota (0–10, aprueba ≥ 5). La dificultad se [adapta sola](#dificultad-adaptativa) según tus rachas.
4. Opcional: contenedor Docker para practicar comandos reales.
5. En la portada: progreso local y estadísticas del servidor (ver [Progreso y estadísticas](#progreso-y-estadísticas-portada)).
6. **Entrega al profesor:** descarga un `.json` con su avance y lo deja en la carpeta compartida.

### Profesor

1. Entra con la cuenta **profesor** (o cualquier login configurado con rol profesor).
2. **Practica igual que un alumno** desde la portada (mismos módulos, corrección y estadísticas propias). No verá «Descargar entrega para el profesor» (solo alumnos).
3. **Seguimiento de alumnos** en **`/profesor/alumnos`**: importa entregas JSON de la carpeta compartida (con nombre personalizado por alumno), tabla comparativa, gráficas y nota media por módulo.
4. Revisa propuestas de la IA en **`/profesor/revisar`**: tabla comparativa, casos de prueba y paquete ZIP.
5. Aprueba o rechaza ejercicios en **su** instalación; para que los alumnos los tengan debe **exportar** el banco (`banco-forja.json`) a la carpeta compartida.
6. Puede subir PDFs, ver y editar la **solución de referencia** (con resaltado de sintaxis) y exportar ejercicios para compartir.

> 💡 **TIP para el profesor:** prueba tú mismo unos ejercicios para ver la [dificultad adaptativa](#dificultad-adaptativa) en acción y, cuando tengas alguno validado, **exporta tu primer banco** (`/profesor/banco` → «Descargar banco completo») y déjalo en la carpeta compartida para que la clase lo importe.

Cada alumno y el profesor suelen tener **instalaciones independientes** (distinto PC): el intercambio es manual por carpeta compartida. Ver [Instalaciones independientes (aula real)](#instalaciones-independientes-aula-real).

Los módulos clásicos (`poo`, `bd_sql`, `docker`, etc.) **no necesitan** Gemini.

---

## Dificultad adaptativa

El nivel de dificultad (1–3) de cada alumno **se ajusta solo** según sus resultados, sin tener que tocar el perfil:

| Racha consecutiva | Efecto |
|-------------------|--------|
| **5 aprobados seguidos** | Sube un nivel (máximo 3) |
| **3 suspensos seguidos** | Baja un nivel (mínimo 1) |

- Aplica a **todos los módulos** (clásicos y `docs_*`), no solo a los de IA.
- Solo afecta a **cuentas de alumno**; el profesor mantiene el nivel que elija manualmente.
- Tras un ajuste, la racha correspondiente se reinicia (no encadena varias subidas/bajadas seguidas) y el alumno ve un aviso en pantalla.
- El nivel se guarda por usuario (`datos/usuarios.json`, campo `nivelGemini`) y también puede fijarse a mano en **Perfil → «Dificultad de ejercicios»**.

Lógica en `ServicioDificultadAdaptativa.java` (umbrales 5/3) apoyada en las rachas de `EstadisticasUsuario` (`rachaActual` / `rachaSuspensos`).

---

## Instalaciones independientes (aula real)

En clase, **cada alumno y el profesor suelen tener la app en su propio PC**. No hay servidor central que sincronice datos: lo que el profesor aprueba en su banco **no aparece solo** en los equipos de los alumnos.

El intercambio es manual mediante una **carpeta compartida** (red del aula, Google Drive con Drive para escritorio, USB, etc.). La app **no envía correos ni sube a la nube** por sí sola.

> ℹ️ **La «carpeta compartida» no forma parte del programa.** Es una carpeta cualquiera que **tú creas en el sistema operativo** (un recurso de red SMB/NFS, una carpeta de Google Drive/OneDrive sincronizada, una memoria USB…). La Forja solo **exporta** ficheros `.json` (que guardas ahí) e **importa** ficheros que tú seleccionas desde ahí; no crea, monta ni gestiona esa carpeta. En todo el documento «carpeta compartida» se refiere a ese sitio externo de tu elección.

| Qué se comparte | Sentido | Fichero típico | Quién exporta | Quién importa |
|-----------------|---------|----------------|---------------|---------------|
| **Ejercicios verificados** | Profesor → alumnos | `banco-forja.json` o un `.json` suelto | Profesor | Alumno (portada) |
| **Progreso del alumno** | Alumno → profesor | `entrega-<usuario>-<fecha>.json` | Alumno | Profesor (`/profesor/alumnos`) |

### Flujo completo recomendado

```
Profesor                          Carpeta compartida                 Alumno
────────                          ──────────────────                 ──────
Prueba ejercicios
Aprueba en su banco local
Exporta banco-forja.json      →   banco-forja.json            →    Importa banco (portada)
                                                                  Practica ejercicios
                                                                  Exporta entrega .json   →
Importa entregas + nombre       ←   entrega-*.json              ←   (deja en carpeta)
Consulta tabla y gráficas
```

Detalle de cada intercambio:

- [Compartir ejercicios del banco](#compartir-ejercicios-del-banco-carpeta-compartida)
- [Entrega al profesor](#entrega-al-profesor-carpeta-compartida) y [Seguimiento de alumnos](#seguimiento-de-alumnos-profesor)

---

## Progreso y estadísticas (portada)

En la **portada** (`/`) hay dos paneles que **no comparten datos**: uno vive en el navegador y otro en el servidor.

| Panel | Dónde se guarda | Qué muestra | Cómo vaciarlo |
|-------|-----------------|-------------|---------------|
| **Tu progreso (local, sin red)** | `localStorage` del navegador (por usuario de sesión) | Últimos **5** ejercicios, gráfico, ranking por módulo y medallas | Botón **«Limpiar progreso local»** (solo este navegador) |
| **Tus estadísticas de uso (servidor)** | `datos/estadisticas/<usuario>.json` en el equipo | Totales acumulados: intentos, aprobados, nota media, tiempo, desglose por módulo | Botón **«Limpiar estadísticas»** (pide confirmación) |

### Detalles importantes

- **Local:** si cambias de navegador o borras datos del sitio, el progreso local desaparece; el del servidor no.
- **Servidor:** cuenta todos los ejercicios que has **corregido** (enviado la respuesta), desde que usas la app en ese equipo.
- Limpiar uno **no** borra el otro: puedes resetear medallas en el navegador y conservar el historial del servidor, o al revés.
- Tras limpiar estadísticas del servidor, el fichero `datos/estadisticas/<usuario>.json` se elimina; al hacer el siguiente ejercicio se vuelve a crear.

### API (servidor)

| Método | Ruta | Efecto |
|--------|------|--------|
| `POST` | `/estadisticas/limpiar` | Borra las estadísticas de uso del usuario con sesión iniciada y redirige a la portada |

Requiere estar logueado y token CSRF (el formulario de la portada lo incluye).

### Entrega al profesor (carpeta compartida)

La app **no envía ficheros**; el intercambio es mediante una **carpeta compartida** de red o del aula.

| Paso | Quién | Acción |
|------|-------|--------|
| 1 | Alumno | Portada → **Tus estadísticas de uso** → «Descargar entrega para el profesor» (solo cuentas alumno; el profesor no ve este bloque) |
| 2 | Alumno | Copia el `.json` en la carpeta compartida (opcional: PDFs de ejercicios desde la pantalla de resultado) |
| 3 | Profesor | **Perfil → Seguimiento de alumnos** o `/profesor/alumnos` → elegir fichero y **nombre personalizado** del alumno |
| 4 | Profesor | Tabla comparativa y gráficas de la clase; «Detalle» para ver cada módulo |

El fichero incluye estadísticas del **servidor**, el **historial de ejercicios** (enunciado, respuesta y nota de cada intento) y progreso **local** del navegador (ranking, medallas).

Si varios alumnos practican en el **mismo equipo**, el profesor también puede verlos en «Alumnos en este servidor» sin importar ficheros.

---

## Historial de ejercicios y exportar resultados (profesor)

Cada vez que un alumno envía una respuesta y se corrige, el servidor guarda en disco un registro con **ejercicio, respuesta y nota** (`datos/historial/<login>.json`). Así el profesor puede revisarlos después, aunque el alumno no haya exportado su entrega.

Ruta: **`/profesor/resultados`** (requiere rol profesor).

| Bloque | Origen | Contenido |
|--------|--------|-----------|
| **Resultados en este servidor** | `datos/historial/*.json` | Intentos de alumnos que practican en la misma instalación |
| **Resultados de entregas importadas** | Campo `historialIntentos` de cada entrega | Mismo detalle cuando el alumno exportó su `.json` y el profesor lo importó |

En cada fila: alumno, fecha, módulo, título, nota, tiempo y enlace **Ver** (enunciado y respuesta completos, con resaltado de sintaxis).

### Exportar CSV (Excel)

Desde `/profesor/resultados` puedes descargar:

| Botón | Fichero | Columnas |
|-------|---------|----------|
| **Exportar CSV (servidor)** | `resultados-servidor.csv` | Alumno, Login, Fecha, Módulo, Título, Nota, Aprobado, Tiempo |
| **Exportar CSV (importados)** | `resultados-importados.csv` | Igual, solo entregas importadas |

Formato: UTF-8 con BOM, separador `;` (abre bien en Excel en español).

### API (resultados)

| Método | Ruta | Efecto |
|--------|------|--------|
| `GET` | `/profesor/resultados` | Lista resultados (servidor + importados) |
| `GET` | `/profesor/resultados/exportar.csv` | CSV de alumnos en este servidor |
| `GET` | `/profesor/resultados/importados/exportar.csv` | CSV de entregas importadas |
| `GET` | `/profesor/resultados/{login}/{intentoId}` | Detalle: enunciado + respuesta del intento |

---

## Compartir ejercicios del banco (carpeta compartida)

La app **no sincroniza** instalaciones entre profesor y alumnos. El banco local (`banco/aprobados/`) solo existe en cada equipo.

| Paso | Quién | Acción |
|------|-------|--------|
| 1 | Profesor | Prueba ejercicios, aprueba en `/profesor/revisar` o `/profesor/banco` |
| 2 | Profesor | **Un ejercicio** → «Guardar en banco» (escribe en `banco/aprobados/` o `pendientes/`). **Todo el banco** → `/profesor/banco` → «Descargar banco completo» (`banco-forja.json`) para la carpeta compartida |
| 3 | Profesor | Deja el `.json` en la carpeta compartida |
| 4 | Alumno | Portada → «Ejercicios del banco (del profesor)» → «Importar al banco local» |
| 5 | Alumno | Practica con los botones del banco que aparecen tras importar |

Formato del paquete: `forja-banco-ejercicios` (también se acepta un ejercicio suelto con `criterios` y `enunciado`).

| Ruta | Efecto |
|------|--------|
| `GET /profesor/banco/exportar.json` | Descarga todo el banco aprobado (solo profesor) |
| `POST /ejercicio/{id}/guardar-banco` | Profesor: guarda en `banco/aprobados/` o `pendientes/` con `nombreArchivo` descriptivo (p. ej. `docker-nginx-8080.json`) |
| `GET /ejercicio/{id}/exportar-banco.json` | Profesor: descarga portable para otra instalación (carpeta compartida) |
| `POST /banco/importar` | Alumno o profesor: sube paquete o ejercicio a `banco/aprobados/` |
| `POST /profesor/banco/abrir-carpeta` | Abre `banco/aprobados/` o `banco/pendientes/` en el explorador del SO (solo en equipos con escritorio) |

Guías de rol: [alumno.txt](alumno.txt) y [profesor.txt](profesor.txt).

---

## Seguimiento de alumnos (profesor)

Ruta: **`/profesor/alumnos`** (requiere rol profesor).

| Bloque | Origen | Uso |
|--------|--------|-----|
| **Importar desde carpeta compartida** | Fichero `.json` + nombre personalizado | Identifica a cada alumno al importar |
| **Avance de la clase** | Entregas importadas | Tabla comparativa + gráficas (resumen y evolución) |
| **Entregas guardadas** | `datos/entregas/<profesor>/` | Historial de ficheros importados en este equipo |
| **Alumnos en este servidor** | `datos/estadisticas/*.json` | Alumnos que han practicado en la misma instalación |

En el detalle de cada alumno verás totales, tabla **por módulo/categoría** (`poo`, `docker`, `docs_forense`, etc.) y los últimos ejercicios registrados.

### Panel «Avance de la clase»

Tras importar varias entregas:

- **Tabla comparativa** con nombre personalizado, ejercicios, aprobados, media, módulos y última actividad (editable el nombre en cada fila).
- **Tabla por módulo/categoría** con la nota media de cada alumno en cada tema.
- **Gráficas:** resumen por alumno (métrica elegible) y evolución de las últimas notas; filtro por alumno o «toda la clase».

### API (entregas y banco)

| Método | Ruta | Quién | Efecto |
|--------|------|-------|--------|
| `GET` | `/entrega/exportar.json` | Alumno | Descarga entrega (estadísticas servidor; el navegador añade progreso local al guardar) |
| `POST` | `/profesor/alumnos/importar` | Profesor | Importa entrega de un alumno (fichero + nombre personalizado) |
| `POST` | `/profesor/alumnos/importada/{id}/renombrar` | Profesor | Cambia el nombre personalizado de una entrega |
| `POST` | `/profesor/alumnos/importada/{id}/eliminar` | Profesor | Elimina una entrega importada |
| `GET` | `/profesor/banco/exportar.json` | Profesor | Descarga paquete `forja-banco-ejercicios` con todo el banco aprobado |
| `GET` | `/ejercicio/{id}/exportar-banco.json` | Profesor | Un ejercicio probado, listo para importar en otro equipo |
| `POST` | `/banco/importar` | Todos | Importa paquete o ejercicio suelto a `banco/aprobados/` |
| `GET` | `/profesor/resultados` | Profesor | Historial de intentos (servidor + importados) |
| `GET` | `/profesor/resultados/exportar.csv` | Profesor | CSV de resultados en este servidor |
| `GET` | `/profesor/resultados/importados/exportar.csv` | Profesor | CSV de entregas importadas |
| `GET` | `/profesor/resultados/{login}/{intentoId}` | Profesor | Detalle de un intento (enunciado + respuesta) |

Formato entrega alumno: `forja-entrega-alumno`. Formato banco: `forja-banco-ejercicios`.

---

## Cuenta y perfil

| Acción | Dónde |
|--------|--------|
| Entrar | `/login` |
| Cambiar nombre, usuario o contraseña | `/perfil` (enlace **Perfil** en la cabecera) |
| Guía técnica (Gemini, PDF, nuevos módulos) | `/perfil` (sección inferior) o `/como-funciona` |
| Clave API de Gemini | `/perfil` → requiere **contraseña de acceso** (no la clave API) |
| Modelo de Gemini | `/perfil` → **Guardar modelo** (p. ej. `gemini-2.5-flash`; no pide contraseña) |
| Dificultad de ejercicios (1–3) | `/perfil` → **«Dificultad de ejercicios»** (manual); también se ajusta sola, ver [Dificultad adaptativa](#dificultad-adaptativa) |
| **Seguimiento de alumnos** (profesor) | `/profesor/alumnos` → importar entrega o ver alumnos del servidor |
| **Resultados y CSV** (profesor) | `/profesor/resultados` → revisar intentos y exportar CSV |
| Descargar entrega para el profesor (**solo alumno**) | Portada → «Descargar entrega para el profesor» |
| Importar banco del profesor (alumno) | Portada → «Ejercicios del banco» → «Importar al banco local» |
| Exportar banco completo (profesor) | `/profesor/banco` → «Descargar banco completo» |
| Guardar ejercicio en el banco (profesor) | Ejercicio o resultado → nombre del fichero + «Guardar en banco» (plantilla o IA) |
| **Revisar propuestas IA** (profesor) | `/profesor/revisar` → detalle en `/profesor/revisar/{id}` |
| Vista rápida del banco (profesor) | `/profesor/banco` |
| Descargar paquete de revisión (profesor) | `/profesor/revisar/{id}/paquete` (ZIP) |
| Limpiar progreso local (navegador) | Portada → **Tu progreso (local, sin red)** → «Limpiar progreso local» |
| Limpiar estadísticas de uso (servidor) | Portada → **Tus estadísticas de uso (servidor)** → «Limpiar estadísticas» |

### Usuarios y roles

- Los logins listados en `forjaexamenes.login.profesores` (o `FORJAEXAMENES_PROFESORES`) tienen rol **PROFESOR** y acceden a `/profesor/**`.
- Los demás son **ALUMNO**.
- Tras el primer arranque, las contraseñas quedan guardadas (cifradas) en `datos/usuarios.json`. Cambiar `application.properties` **no** actualiza cuentas que ya existen en ese fichero.

`FORJAEXAMENES_MODO_PROFESOR=true` sigue siendo útil para **mostrar la solución** antes de enviar en cualquier cuenta; la **zona de revisión** (`/profesor/*`) requiere rol profesor.

### Cambiar contraseñas por defecto

| Situación | Qué archivo tocar | Qué hacer |
|-----------|-------------------|-----------|
| **Instalación nueva** (aún no existe `datos/usuarios.json`) | [`web/src/main/resources/application.properties`](web/src/main/resources/application.properties) | Edita `forjaexamenes.login.usuarios` y, si hace falta, `forjaexamenes.login.profesores`. Formato: `usuario:contraseña` separados por coma. |
| **Misma máquina, sin tocar el JAR** | [`examenforge/.env`](.env) (créalo si no está) | Añade `FORJAEXAMENES_USUARIOS=alumno:tu_clave,demo:demo,profesor:clave_profesor` y opcionalmente `FORJAEXAMENES_PROFESORES=profesor`. Reinicia la app. Solo aplica si `datos/usuarios.json` **no** existe todavía. |
| **La app ya se ha usado** | — (no edites `usuarios.json` a mano) | Entra en **`/perfil`** y cambia la contraseña con la actual. Es la forma habitual en producción. |
| **Resetear todas las cuentas** | Borra `datos/usuarios.json` | Para la app, ajusta `application.properties` o `.env` como arriba y vuelve a arrancar: se regeneran los usuarios con las claves nuevas. Pierdes perfiles y estadísticas de esos logins. |

Ejemplo en `application.properties` (líneas ~53–55):

```properties
forjaexamenes.login.usuarios=${FORJAEXAMENES_USUARIOS:alumno:practica,demo:demo,profesor:profesor}
forjaexamenes.login.profesores=${FORJAEXAMENES_PROFESORES:profesor}
```

Ejemplo en `.env` en la raíz del proyecto (`examenforge/.env`):

```bash
FORJAEXAMENES_USUARIOS=alumno:mi_clase_2026,demo:demo,profesor:clave_segura
FORJAEXAMENES_PROFESORES=profesor
```

> **Nota:** el contenedor Docker de práctica (`docker compose … practica`) usa su propio usuario `alumno`/`practica`; es independiente del login web. Para cambiarlo, edita [`docker-compose.yml`](docker-compose.yml) o la imagen del servicio `practica`.

---

## Revisión del banco (profesor)

Flujo recomendado para validar ejercicios generados desde apuntes **en el equipo del profesor** y luego compartirlos con la clase:

1. Activa la cola de revisión en **`/profesor/revisar`** o **`/profesor/banco`** (casilla «Guardar propuestas de IA en banco/pendientes/») → cada ejercicio `docs_*` se guarda en `banco/pendientes/`.
2. Entra como **profesor** / **profesor**.
3. Abre **Perfil → Revisar propuestas** o ve a `/profesor/revisar`.
4. Pulsa **Revisar** en un pendiente. Verás:
   - **Comparativa** propuesta Gemini vs criterios del escenario (palabras clave, enunciado).
   - **Tabla de criterios** con pruebas automáticas: solución de referencia (debe pasar), variante con sinónimo (p. ej. `docker container run`) e respuesta insuficiente (debe fallar).
   - **Alias** aplicados a cada término (desde `vocabulario_claves.json`).
5. **Aprobar y publicar**, **Rechazar** o **Descargar paquete ZIP** desde la misma pantalla.
6. **Exportar** el banco o ejercicios sueltos a la carpeta compartida para que los alumnos los importen (ver [Compartir ejercicios del banco](#compartir-ejercicios-del-banco-carpeta-compartida)).

> «Aprobar y publicar» solo actualiza el banco **local** del profesor. Los alumnos en otros PCs necesitan el paso de exportar/importar.

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

### Retroalimentación al corregir

Tras enviar una respuesta, la pantalla de resultado muestra **criterios en lenguaje claro** (qué se esperaba y una **pista** distinta si fallas), no expresiones regulares crudas. La lógica está en [`retroalimentacion_criterios.py`](retroalimentacion_criterios.py) y se aplica al generar (`generador.py`) y al evaluar (`evaluador.py`). En **modo profesor** puedes ver el patrón técnico del criterio.

---

## Módulos disponibles

| Módulo | Contenido |
|--------|-----------|
| `poo` | POO Java (certificado) |
| `bd_sql`, `bd_modelo`, `bd_transacciones`, `bd_jdbc`, `bd` | Bases de datos |
| `redes`, `sistemas`, `docker`, `git` | Infraestructura |
| `docs_*` | Apuntes PDF indexados + Gemini (cualquier asignatura; ver [tipos de materia](#tipos-de-materia-apuntes-pdf)) |
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

Coloca PDFs en `documentacion/<tema>/` → módulo `docs_<tema>` (p. ej. `documentacion/docker/` → `docs_docker`, `documentacion/ingles/` → `docs_ingles`).

**Desde la web:** portada → *Apuntes del profesor* → **Subir e indexar**, o botón **Actualizar apuntes** tras copiar PDFs.

**Indexación manual:** `python3 indexador_docs.py` (también al arrancar la web si hay PDFs nuevos).

Estructura de carpetas y subida: **[documentacion/README.md](documentacion/README.md)**.

### Tipos de materia (apuntes PDF)

Cada carpeta `documentacion/<tema>/` genera un módulo `docs_<tema>`. El sistema detecta el **tipo de materia** y adapta el prompt de Gemini, la validación de palabras clave y el texto del enunciado.

| Tipo | Carpetas reconocidas (ejemplos) | Qué pide el ejercicio |
|------|----------------------------------|------------------------|
| `informatica` | `docker`, `git`, `linux`, `forense`, `sql`… | Comandos, código o pasos técnicos |
| `idiomas` | `ingles`, `english`, `frances`, `aleman`… | Traducción, gramática, vocabulario del fragmento |
| `general` | cualquier otra (`historia`, `arte`, `economia`…) | Respuesta escrita basada en el fragmento |

**Prioridad de detección:**

1. Fichero `documentacion/<tema>/.forja-tipo` (o `tipo_materia.txt`) con una línea: `informatica`, `idiomas` o `general`.
2. Reglas en [`vocabulario_claves.json`](vocabulario_claves.json) → sección `tipos_materia` o `tipo_materia` por módulo.
3. Por defecto: `general` (materias no listadas explícitamente como informática o idiomas).

Ejemplo para un curso de inglés:

```
documentacion/ingles/
├── .forja-tipo          # opcional; contenido: idiomas
└── unit1-grammar.pdf
```

Tras indexar, en la portada aparece **docs_ingles**. Los ejercicios pedirán frases o traducciones (no comandos de terminal). La corrección comprueba que la respuesta incluya términos del fragmento.

> Si añades PDFs o cambias `.forja-tipo`, vuelve a indexar (**Actualizar apuntes** o `python3 indexador_docs.py`) para que `indice/docs_*.json` incluya `tipo_materia`.

---

## Arquitectura de ejercicios

| Tipo | Origen | Corrección |
|------|--------|------------|
| **A — plantilla** | `generador.py` | `regex` definidos en código |
| **B — apuntes + IA** | PDF → Gemini → `modelo_ejercicio.py` | `contiene_todos` / `contiene_alguno` + alias |
| **Banco** | `banco/aprobados/*.json` | Tipos definidos en el JSON + alias |

Gemini solo propone `tema`, `pregunta`, `palabras_clave` (y opcional `variantes`). El sistema valida con [`tipo_materia.py`](tipo_materia.py) y [`vocabulario_claves.json`](vocabulario_claves.json) (prohibidas/permitidas por tipo y módulo) y exige que cada clave aparezca en el fragmento del PDF.

### Dónde se construye y envía el prompt a la IA

El **único fichero** que habla con la API de Gemini es [`generador_gemini.py`](generador_gemini.py). La corrección (`evaluador.py`) **no** usa IA.

| Paso | Fichero | Función / detalle |
|------|---------|-------------------|
| 1. Indexar PDFs | [`indexador_docs.py`](indexador_docs.py) | Fragmentos en `indice/docs_*.json` con `tipo_materia` |
| 2. Elegir fragmento | [`generador_docs.py`](generador_docs.py) | `generar_ejercicio_documentacion()` |
| 3. Resolver tipo | [`tipo_materia.py`](tipo_materia.py) | `informatica` / `idiomas` / `general` |
| 4. Construir prompt | [`generador_gemini.py`](generador_gemini.py) | `_construir_prompt()` — tipo, nivel (1–3), fragmento |
| 5. Llamar a Gemini | [`generador_gemini.py`](generador_gemini.py) | `generate_content` → JSON intermedio |
| 6. Validar y corregir | [`modelo_ejercicio.py`](modelo_ejercicio.py) + [`evaluador.py`](evaluador.py) | Criterios `contiene_todos` / `contiene_alguno` |
| 7. Web | `ServicioGenerador.java` | Ejecuta `generador.py` como subproceso |

**Flujo resumido:**

```
PDF  →  indexador_docs.py  →  indice/docs_*.json (tipo_materia)
Portada docs_*  →  generador_docs  →  tipo_materia  →  generador_gemini  →  modelo_ejercicio  →  evaluador
```

- **Nivel (1–3):** Perfil del alumno o **[dificultad adaptativa](#dificultad-adaptativa)** (5 aprobados seguidos ↑, 3 suspensos ↓, para todos los módulos) → `--nivel` en Python (`_instrucciones_nivel()`).
- **Tipo de materia:** carpeta, `.forja-tipo` o `vocabulario_claves.json` → prompt y validación distintos.
- **Clave API / modelo:** `datos/gemini.json` o `.env` (`GEMINI_API_KEY`, `FORJAEXAMENES_GEMINI_MODEL`).
- **Pre-generación:** `forjaexamenes.precarga-ejercicios-activa` — pool de ejercicios `docs_*` listos.

### Calidad de `palabras_clave`

| Regla | Detalle |
|-------|---------|
| Mínimo | 2 claves distintas del fragmento |
| Tipo `informatica` | Al menos una clave técnica (comando, flag, nombre de herramienta…) |
| Tipo `idiomas` / `general` | Claves concretas del texto; longitud mínima según tipo |
| Prohibidas | Lista global + por tipo (`tipos_materia`) + por módulo (`docs_*`) |
| Fragmento | Cada clave debe aparecer en el texto indexado (salvo `permitidas_extra`) |

### Banco y aprobación

```
banco/aprobados/     → ejercicios verificados en ESTE equipo (portada + generador)
banco/pendientes/    → propuestas Gemini en espera de revisión (solo profesor)
banco/catalogo.json  → índice (se regenera al importar, aprobar o arrancar)
```

Para repartir ejercicios entre equipos: exportar `forja-banco-ejercicios` (`GET /profesor/banco/exportar.json`) e importar en cada alumno (`POST /banco/importar`). Tras importar, los módulos del banco aparecen en la portada como «Ejercicios del banco (del profesor)».

| Propiedad / variable | Efecto |
|----------------------|--------|
| Cola de revisión IA (`banco/pendientes/`) | Actívala en **`/profesor/revisar`** o **`/profesor/banco`** (preferencia guardada en `datos/preferencias-profesor.json`). También vía `forjaexamenes.gemini-guardar-pendientes=true` en properties |
| `forjaexamenes.gemini-solo-aprobados=true` | Solo ejercicios del banco (sin Gemini en vivo) |
| `forjaexamenes.modo-profesor` / `FORJAEXAMENES_MODO_PROFESOR` | Solución visible antes de enviar |
| `forjaexamenes.login.profesores` / `FORJAEXAMENES_PROFESORES` | Logins con acceso a `/profesor/**` |

---

## Estructura del proyecto

```
examenforge/
├── alumno.txt / profesor.txt    # Guías por rol (instalaciones independientes)
├── install.sh / install.bat / install.ps1   # Instalación guiada (Linux/macOS / Windows)
├── iniciar-forja.sh / .bat      # Arranque tras instalar
├── arrancar-web.sh
├── generador.py / evaluador.py
├── generador_gemini.py / generador_docs.py / indexador_docs.py
├── modelo_ejercicio.py / tipo_materia.py / banco_loader.py
├── alias_comandos.py
├── comun.py                     # utilidades compartidas (slug, identificadores)
├── vocabulario_claves.json      # tipos_materia, prohibidas, alias_comandos
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
├── datos/
│   ├── estadisticas/            # por usuario en este equipo
│   └── entregas/<profesor>/    # entregas de alumnos importadas
└── web/                         # Spring Boot
    └── src/main/resources/
        ├── static/js/editor-codigo.js   # resaltado en «Tu solución» y solución de referencia
        └── templates/
            ├── login.html
            ├── inicio.html            # importar banco + entrega alumno
            ├── como-funciona.html
            ├── profesor-alumnos-lista.html
            ├── profesor-alumnos-detalle.html
            ├── profesor-revisar-lista.html
            ├── profesor-revisar-detalle.html
            ├── profesor-banco.html    # exportar banco completo
            └── fragments/
```

---

## Configuración útil

Claves de `application.properties` (o variables de entorno equivalentes):

| Clave | Descripción |
|-------|-------------|
| `forjaexamenes.raiz` | Raíz del proyecto (por defecto `../` desde `web/`) |
| `forjaexamenes.python-interprete` | `FORJAEXAMENES_PYTHON_INTERPRETE` — `python3` o `python` (el instalador lo fija en `.env`) |
| `forjaexamenes.login.usuarios` | `FORJAEXAMENES_USUARIOS` — `usuario:clave` separados por coma; ver [Cambiar contraseñas por defecto](#cambiar-contraseñas-por-defecto) |
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
| **Dificultad adaptativa** | El nivel del alumno sube tras 5 aprobados seguidos y baja tras 3 suspensos seguidos (`ServicioDificultadAdaptativa.java`). Ver [Dificultad adaptativa](#dificultad-adaptativa). |
| **Progreso local** | Últimos 5 ejercicios en `localStorage` (`progreso.js`). Botón «Limpiar progreso local» en la portada. |
| **Estadísticas de uso** | Totales en `datos/estadisticas/<usuario>.json`. Botón «Limpiar estadísticas» → `POST /estadisticas/limpiar`. |
| **Entrega al profesor** | **Alumno:** portada → «Descargar entrega para el profesor» → `GET /entrega/exportar.json` + progreso local. **Profesor:** no exporta entrega; importa en `/profesor/alumnos` con nombre personalizado. |
| **Banco portable** | Profesor exporta `banco-forja.json`; alumno importa en portada → `banco/aprobados/` + `catalogo.json`. |
| **Seguimiento clase** | Tabla comparativa, notas por módulo y gráficas en `/profesor/alumnos` tras importar entregas. |
| **Historial de intentos** | Tras cada corrección: `datos/historial/<login>.json` (enunciado, respuesta, nota). Panel y CSV en `/profesor/resultados`. |
| **Revisión profesor** | Badges **PASSED** / **FAILED** y resumen «listo para aprobar» en `/profesor/revisar/{id}`. |
| **Resaltado de sintaxis** | En ejercicios y resultados: «Tu solución» y «Solución de referencia» (profesor) con coloreado en vivo; «Tu respuesta» tras corregir; detalle en `/profesor/resultados/{login}/{intentoId}`. Lenguaje según módulo (`poo`→Java, `bd_*`→SQL, `docker`/`git`/`redes`/`sistemas`→bash). Implementado con highlight.js (`editor-codigo.js`, `LenguajeResaltado.java`). |
| **PDFs problemáticos** | `indexador_docs.py` avisa si un PDF está corrupto o protegido con contraseña (se omite y continúa con el resto). |

---

## Problemas frecuentes

| Síntoma | Qué hacer |
|---------|-----------|
| Maven no encuentra `pom.xml` | El proyecto Maven está en `web/`. Usa `mvn -f web/pom.xml ...` (`web\pom.xml` en Windows) o entra antes en `cd web`. |
| Windows: *«No se esperaba ... en este momento»* al arrancar | Actualiza `iniciar-forja.bat` o arranca directo con `java -jar web\target\forjaexamenes-web-1.0.0.jar`. |
| Windows: Python aparece como `C:Program FilesPython...` | En `examenforge\.env`, usa barras normales: `FORJAEXAMENES_PYTHON_INTERPRETE=C:/Program Files/Python313/python.exe`; reinicia la app. |
| Windows: Docker falla por hash en `winget` | Instala desde Docker Desktop o ejecuta `winget install Docker.DockerDesktop --ignore-security-hash` en PowerShell normal. |
| Logo no aparece en la portada | Actualiza el repo, confirma que existe `web/src/img/forja-de-examenes.png` y recompila el JAR. |
| Puerto 8080 ocupado | `fuser -k 8080/tcp` o cambiar `server.port` |
| `API_KEY_INVALID` | Clave real en `.env`; reinicia; `unset GEMINI_API_KEY` en el shell si molesta |
| Cuota Gemini `429` | `FORJAEXAMENES_GEMINI_MODEL=gemini-2.5-flash` en `.env` |
| Gemini `503 UNAVAILABLE` / «high demand» | Saturación temporal del servidor de Google, no de tu configuración. La app reintenta sola con espera; si persiste, aguarda unos minutos, usa módulos sin IA (`poo`, `bd_sql`, `docker`…) o prueba otro modelo en Perfil |
| Gemini `404 NOT_FOUND` (modelo) | Usa `gemini-2.5-flash` con **guiones** (no `gemini-2.5.flash`). En **Mi perfil** → **Guardar modelo** (no pide contraseña) |
| Perfil: contraseña incorrecta al guardar clave API | Usa la contraseña **de acceso** del usuario con el que entraste (no la clave API). Por defecto: `alumno`/`practica`; si la cambiaste antes, la nueva |
| Indexación lenta | Es normal en PDFs grandes; espera el aviso «en segundo plano» en la portada |
| Windows: `indexador_docs.py falló (código 1)` al arrancar | Actualiza `indexador_docs.py` e `iniciar-forja.bat`; comprueba `pip install -r requirements-docs.txt`; prueba `python indexador_docs.py` en la carpeta del proyecto |
| Ejercicios PDF piden comandos en materia no técnica | Crea `documentacion/<tema>/.forja-tipo` con `general` o `idiomas` y reindexa |
| Palabra clave rechazada en apuntes PDF | Revisa `vocabulario_claves.json` (prohibidas del módulo o del tipo) |
| `generador.py falló` | `python3 generador.py -m poo` desde `examenforge/` |
| `403` en `/profesor/revisar` | Entra con cuenta profesor (`profesor` / `profesor`) |
| `403` al enviar ejercicio | Recarga la página (token CSRF en el formulario) |
| Sinónimo no aceptado | Añádelo en `vocabulario_claves.json` → `alias_comandos` |
| El panel local muestra datos viejos o de otro sitio | «Limpiar progreso local»; no confundir con «Limpiar estadísticas» del servidor |
| Quiero empezar de cero en totales e historial del servidor | «Limpiar estadísticas» en la portada (no afecta al progreso del navegador) |
| El profesor no ve el progreso de un alumno en otro PC | El alumno deja el JSON en la carpeta compartida; el profesor lo importa en `/profesor/alumnos` con un nombre personalizado |
| `403` en `/profesor/alumnos` | Solo cuentas con rol profesor; el alumno exporta su entrega, no accede a este panel |
| Aprobé ejercicios pero el alumno no los ve | Normal en PCs distintos: el profesor debe **exportar** el banco y el alumno **importar** en la portada |
| Tras importar banco no hay botones | Comprueba que el JSON sea `forja-banco-ejercicios` o un ejercicio con `criterios` y `enunciado`; recarga la portada |
| Google Drive como carpeta compartida | Válido: sube/descarga los `.json` manualmente o usa Drive para escritorio y el selector de archivos al importar |

---

## Ampliar el proyecto

| Objetivo | Dónde |
|----------|--------|
| **Prompt enviado a Gemini** (enunciado, reglas, JSON) | `generador_gemini.py` → `_construir_prompt()` |
| **Tipo de materia** (informática / idiomas / general) | `tipo_materia.py`, `vocabulario_claves.json` → `tipos_materia`, `.forja-tipo` en carpeta |
| Parámetros de la llamada API (modelo, temperatura, timeout) | `generador_gemini.py` → `_generar_desde_fragmento_intento()`; timeout también en `application.properties` (`forjaexamenes.gemini-timeout-ms`) |
| Texto de la guía pública | `templates/fragments/guia-funcionamiento.html` |
| Vista de revisión profesor | `profesor-revisar-detalle.html`, `herramientas/revision_profesor.py` |
| Nuevos sinónimos de comandos | `vocabulario_claves.json` → `alias_comandos` |
| Textos de corrección (esperado / pista) | `retroalimentacion_criterios.py`, `evaluador.py` |
| Botón en portada | `templates/inicio.html` |
| Nuevo módulo plantilla | `generador.py` + `pruebas/pruebas_generador_evaluador.py` |
| Nuevo tema PDF | `documentacion/<tema>/` + indexar |
| Ejercicio verificado manual | `banco/aprobados/<tema>/<id>.json` + `reindexar` |
| Exportar/importar banco entre PCs | `ServicioBancoPortable.java`, `ControladorBanco.java` |
| Entregas y seguimiento alumnos | `ServicioEntregasAlumno.java`, `ControladorProfesorAlumnos.java` |
| Dificultad adaptativa (umbrales) | `ServicioDificultadAdaptativa.java` (constantes `APROBADOS_PARA_SUBIR` / `SUSPENSOS_PARA_BAJAR`) |
| Lenguaje del resaltado de sintaxis por módulo | `util/LenguajeResaltado.java` y `static/js/editor-codigo.js` → `resolverLenguaje()` |
| Utilidades compartidas (evitar duplicar) | Java: `util/` (`MapeadorJson`, `FechasForja`, `RutasUsuario`) · Python: `comun.py` (`slug`, `generar_identificador`) |

---

## Pruebas

```bash
python3 pruebas/pruebas_generador_evaluador.py
python3 pruebas/pruebas_modelo_ejercicio.py
python3 pruebas/pruebas_evaluador_tipos.py
python3 pruebas/pruebas_alias_comandos.py
python3 pruebas/pruebas_banco_loader.py
python3 pruebas/pruebas_indexador_docs.py
python3 pruebas/pruebas_tipo_materia.py
python3 pruebas/pruebas_retroalimentacion_criterios.py
cd web && mvn test
```

---

## Licencia

Creado por entreunosyceros con ☕ y 🚬 para el ICFT0112

Ver [LICENSE](LICENSE).
