# Guía de uso

[← Índice](README.md) · [README principal](../README.md)

---

## Qué hace la aplicación

### Alumno

1. Elige un módulo en la portada (POO, SQL, Docker, apuntes PDF…) o **importa el banco** que el profesor dejó en la carpeta compartida.
2. La web ejecuta `generador.py` y muestra un enunciado (con nombre y fecha del alumno).
3. Escribe la respuesta en un cuadro con **resaltado de sintaxis** (Java, SQL, bash… según el módulo); `evaluador.py` aplica criterios y calcula la nota (0–10, aprueba ≥ 5). La dificultad se [adapta sola](#dificultad-adaptativa) según tus rachas.
4. Opcional: contenedor Docker para practicar comandos reales.
5. En la portada: progreso y estadísticas del servidor (ver [Progreso y estadísticas](#progreso-y-estadísticas-portada)).
6. **Entrega al profesor:** descarga un `.json` con su avance y lo deja en la carpeta compartida.

### Profesor

1. Entra con la cuenta **profesor** (o cualquier login configurado con rol profesor).
2. **Practica igual que un alumno** desde la portada (mismos módulos, corrección y estadísticas propias). No verá «Descargar entrega para el profesor» (solo alumnos).
3. **Seguimiento de alumnos** en **`/profesor/alumnos`**: importa entregas JSON de la carpeta compartida (con nombre personalizado por alumno), tabla comparativa, gráficas y nota media por módulo.
4. Revisa propuestas de la IA en **`/profesor/revisar`**: tabla comparativa, casos de prueba y paquete ZIP.
5. Aprueba o rechaza ejercicios en **su** instalación; para que los alumnos los tengan debe **exportar** el banco (`banco-forja.json`) a la carpeta compartida.
6. Puede subir PDFs, ver y editar la **solución de referencia** (con resaltado de sintaxis) y exportar ejercicios para compartir.

> 💡 **TIP para el profesor:** prueba tú mismo unos ejercicios para ver la [dificultad adaptativa](#dificultad-adaptativa) en acción y, cuando tengas alguno validado, **exporta tu primer banco** (`/profesor/banco` → «Descargar banco completo») y déjalo en la carpeta compartida para que la clase lo importe.

Cada alumno y el profesor suelen tener **instalaciones independientes** (distinto PC): el intercambio es manual por carpeta compartida. Ver [Aula con PCs independientes](aula-compartida.md).

Los módulos clásicos (`poo`, `bd_sql`, `docker`, etc.) **no necesitan** Gemini.

Guías de rol: [alumno.txt](../alumno.txt) y [profesor.txt](../profesor.txt).

## Dificultad adaptativa

El nivel de dificultad (1–3) de cada alumno **se ajusta solo** según sus resultados, sin tener que tocar el perfil:

| Racha consecutiva | Efecto |
|-------------------|--------|
| **5 aprobados seguidos** en el nivel actual o superior | Sube un nivel (máximo 3) |
| **3 suspensos seguidos** en el nivel actual o inferior | Baja un nivel (mínimo 1) |

- Aplica a **todos los módulos** (clásicos y `docs_*`), no solo a los de IA.
- Solo afecta a **cuentas de alumno**; el profesor mantiene el nivel que elija manualmente.
- Un aprobado en un nivel **más fácil** que el actual **no** suma a la racha de subida (evita subir solo con ejercicios fáciles).
- Tras un ajuste, la racha correspondiente se reinicia (no encadena varias subidas/bajadas seguidas) y el alumno ve un aviso en pantalla.
- El nivel se guarda por usuario (`datos/usuarios.json`, campo `nivelGemini`) y también puede fijarse a mano en **Perfil → «Dificultad de ejercicios»**.
- Cada intento guarda su nivel en el historial; la **nota media** de estadísticas usa nota ponderada (nivel 1 ×0,7, 2 ×1,0, 3 ×1,3, tope 10).

Lógica en `ServicioDificultadAdaptativa.java` y ponderación en `ServicioEstadisticasUsuario` (umbrales 5/3).

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

## Progreso y estadísticas (portada)

En la **portada** (`/`) el panel de progreso y las estadísticas de uso **comparten la misma fuente: el servidor**. El navegador solo guarda una caché para pintar el gráfico y las medallas al instante.

| Panel | Dónde se guarda | Qué muestra | Cómo vaciarlo |
|-------|-----------------|-------------|---------------|
| **Tu progreso** | Servidor (`datos/estadisticas/…`); caché en `localStorage` | Últimos **5** ejercicios, gráfico, ranking por módulo y medallas | **«Recargar desde el servidor»** (solo refresca la caché) |
| **Tus estadísticas de uso** | `datos/estadisticas/<usuario>.json` | Totales acumulados: intentos, aprobados, nota media, tiempo, desglose por módulo | **«Limpiar estadísticas»** (borra el servidor; la caché se alinea al recargar) |

### Detalles importantes

- Si cambias de navegador, al iniciar sesión el panel se **vuelve a sincronizar** con el servidor.
- Contabiliza los ejercicios que has **corregido** (enviado la respuesta) en ese equipo.
- Limpiar la caché del navegador **no** borra el servidor; limpiar estadísticas del servidor sí reinicia totales y el panel al recargar.

### API (servidor)

| Método | Ruta | Efecto |
|--------|------|--------|
| `GET` | `/estadisticas/progreso.json` | Snapshot para sincronizar el panel (historial + stats) |
| `POST` | `/estadisticas/limpiar` | Borra las estadísticas de uso del usuario con sesión iniciada |

Requiere estar logueado; el POST necesita token CSRF (el formulario de la portada lo incluye).

---

[← Índice de documentación](README.md)
