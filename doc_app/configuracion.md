# Configuración

[← Índice](README.md) · [README principal](../README.md)

---

## Propiedades útiles

Claves de `application.properties` (o variables de entorno equivalentes):

| Clave | Descripción |
|-------|-------------|
| `forjaexamenes.raiz` | Raíz del proyecto (por defecto `../` desde `web/`) |
| `server.address` | `FORJAEXAMENES_SERVER_ADDRESS` — por defecto `127.0.0.1` (solo este PC). En aula/LAN o Docker: `0.0.0.0` o perfil `red` |
| `forjaexamenes.python-interprete` | `FORJAEXAMENES_PYTHON_INTERPRETE` — `python3`, `python` o la ruta de `.venv` (el instalador lo fija en `.env`) |
| `forjaexamenes.login.usuarios` | `FORJAEXAMENES_USUARIOS` — `usuario:clave` separados por coma; ver [Cambiar contraseñas](cuenta-perfil.md#cambiar-contraseñas-por-defecto) |
| `forjaexamenes.login.profesores` | `FORJAEXAMENES_PROFESORES` — logins con rol profesor |
| `forjaexamenes.modo-profesor` | `FORJAEXAMENES_MODO_PROFESOR` — solución visible |
| `forjaexamenes.gemini-guardar-pendientes` | Cola de revisión en `banco/pendientes/` |
| `forjaexamenes.gemini-solo-aprobados` | Sin generación Gemini en vivo |
| `forjaexamenes.corrector-ia` | `FORJAEXAMENES_CORRECTOR_IA` — segundo corrector semántico con Gemini tras la nota por elementos (default `false`; consume API) |
| `forjaexamenes.corrector-ia-modo` | `FORJAEXAMENES_CORRECTOR_IA_MODO` — `min` (nota = mínimo elementos/IA) o `ponderada` (60 % / 40 %) |
| `forjaexamenes.gemini-timeout-ms` | Timeout (ms) de la llamada HTTP a Gemini (default 60000) |
| `forjaexamenes.precarga-ejercicios-activa` | Pre-genera ejercicios `docs_*` en segundo plano |
| `forjaexamenes.precarga-por-clave` | Cuántos ejercicios listos por módulo+nivel (default 1) |
| `forjaexamenes.timeout-generador-segundos` | Tiempo máximo del subproceso `generador.py` (default 120) |
| `forjaexamenes.subida-pdf-max-mb` | Máximo MB por PDF subido (default 30) |
| `forjaexamenes.limpiar-practica-al-nuevo-ejercicio` | Vacía `datos-practica/` al empezar ejercicio shell |

## Sincronización y pulido

| Comportamiento | Detalle |
|----------------|---------|
| **Catálogo del banco** | Al cargar la portada, si `banco/aprobados/` cambió (CLI o web), se regenera `catalogo.json` automáticamente. Tras aprobar, se publica un evento interno. |
| **Índice de apuntes** | Caché en memoria invalidada por fecha de `indice/docs_*.json` y tras cada indexación. |
| **Actualizar apuntes** | Indexación **en segundo plano** (`@Async`): no bloquea la sesión; la portada hace polling y se recarga al terminar. |
| **Entorno Docker** | Botón «Limpiar entorno de práctica» en la portada; auto-limpieza de `datos-practica/` al iniciar ejercicios `docker`, `redes`, `sistemas`, `git` (`forjaexamenes.limpiar-practica-al-nuevo-ejercicio`, default `true`). |
| **Dificultad adaptativa** | El nivel del alumno sube tras 5 aprobados seguidos y baja tras 3 suspensos seguidos (`ServicioDificultadAdaptativa.java`). Ver [Dificultad adaptativa](guia-usuario.md#dificultad-adaptativa). |
| **Progreso (portada)** | Snapshot del servidor (`/estadisticas/progreso.json`); `localStorage` solo como caché. Botón «Recargar desde el servidor». |
| **Estadísticas de uso** | Totales en `datos/estadisticas/<usuario>.json`. Botón «Limpiar estadísticas» → `POST /estadisticas/limpiar`. |
| **Entrega al profesor** | **Alumno:** portada → «Descargar entrega» → estadísticas servidor (+ caché de progreso del navegador). **Profesor:** importa en `/profesor/alumnos`. |
| **Banco portable** | Profesor exporta `banco-forja.json`; alumno importa en portada → `banco/aprobados/` + `catalogo.json`. |
| **Seguimiento clase** | Tabla comparativa, notas por módulo y gráficas en `/profesor/alumnos` tras importar entregas. |
| **Historial de intentos** | Tras cada corrección: `datos/historial/<login>.json` (enunciado, respuesta, nota). Panel y CSV en `/profesor/resultados`. |
| **Revisión profesor** | Badges **PASSED** / **FAILED** y resumen «listo para aprobar» en `/profesor/revisar/{id}`. |
| **Resaltado de sintaxis** | En ejercicios y resultados: «Tu solución» y «Solución de referencia» (profesor) con coloreado en vivo; «Tu respuesta» tras corregir; detalle en `/profesor/resultados/{login}/{intentoId}`. Lenguaje según módulo (`poo`→Java, `bd_*`→SQL, `docker`/`git`/`redes`/`sistemas`→bash). Implementado con highlight.js (`editor-codigo.js`, `LenguajeResaltado.java`). |
| **PDFs problemáticos** | `indexador_docs.py` avisa si un PDF está corrupto o protegido con contraseña (se omite y continúa con el resto). |

---

[← Índice de documentación](README.md)
