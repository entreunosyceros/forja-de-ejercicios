# Problemas frecuentes

[← Índice](README.md) · [README principal](../README.md)

---

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

[← Índice de documentación](README.md)
