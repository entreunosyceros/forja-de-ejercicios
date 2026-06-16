# Aula con PCs independientes

[← Índice](README.md) · [README principal](../README.md)

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

## Entrega al profesor (carpeta compartida)

La app **no envía ficheros**; el intercambio es mediante una **carpeta compartida** de red o del aula.

| Paso | Quién | Acción |
|------|-------|--------|
| 1 | Alumno | Portada → **Tus estadísticas de uso** → «Descargar entrega para el profesor» (solo cuentas alumno; el profesor no ve este bloque) |
| 2 | Alumno | Copia el `.json` en la carpeta compartida (opcional: PDFs de ejercicios desde la pantalla de resultado) |
| 3 | Profesor | **Perfil → Seguimiento de alumnos** o `/profesor/alumnos` → elegir fichero y **nombre personalizado** del alumno |
| 4 | Profesor | Tabla comparativa y gráficas de la clase; «Detalle» para ver cada módulo |

El fichero incluye estadísticas del **servidor**, el **historial de ejercicios** (enunciado, respuesta y nota de cada intento) y progreso **local** del navegador (ranking, medallas).

Si varios alumnos practican en el **mismo equipo**, el profesor también puede verlos en «Alumnos en este servidor» sin importar ficheros.

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

[← Índice de documentación](README.md)
