# Arquitectura

[← Índice](README.md) · [README principal](../README.md)

---

## Arquitectura de ejercicios

| Tipo | Origen | Corrección |
|------|--------|------------|
| **A — plantilla** | `generador.py` | `regex` definidos en código |
| **B — apuntes + IA** | PDF → Gemini → `modelo_ejercicio.py` | `contiene_todos` / `contiene_alguno` + alias |
| **Banco** | `banco/aprobados/*.json` | Tipos definidos en el JSON + alias |

Gemini solo propone `tema`, `pregunta`, `palabras_clave` (y opcional `variantes`). El sistema valida con [`tipo_materia.py`](../tipo_materia.py) y [`vocabulario_claves.json`](../vocabulario_claves.json) (prohibidas/permitidas por tipo y módulo) y exige que cada clave aparezca en el fragmento del PDF.

### Dónde se construye y envía el prompt a la IA

El **único fichero** que habla con la API de Gemini es [`generador_gemini.py`](../generador_gemini.py). La corrección (`evaluador.py`) **no** usa IA.

| Paso | Fichero | Función / detalle |
|------|---------|-------------------|
| 1. Indexar PDFs | [`indexador_docs.py`](../indexador_docs.py) | Fragmentos en `indice/docs_*.json` con `tipo_materia` |
| 2. Elegir fragmento | [`generador_docs.py`](../generador_docs.py) | `generar_ejercicio_documentacion()` |
| 3. Resolver tipo | [`tipo_materia.py`](../tipo_materia.py) | `informatica` / `idiomas` / `general` |
| 4. Construir prompt | [`generador_gemini.py`](../generador_gemini.py) | `_construir_prompt()` — tipo, nivel (1–3), fragmento |
| 5. Llamar a Gemini | [`generador_gemini.py`](../generador_gemini.py) | `generate_content` → JSON intermedio |
| 6. Validar y corregir | [`modelo_ejercicio.py`](../modelo_ejercicio.py) + [`evaluador.py`](../evaluador.py) | Criterios `contiene_todos` / `contiene_alguno` |
| 7. Web | `ServicioGenerador.java` | Ejecuta `generador.py` como subproceso |

**Flujo resumido:**

```
PDF  →  indexador_docs.py  →  indice/docs_*.json (tipo_materia)
Portada docs_*  →  generador_docs  →  tipo_materia  →  generador_gemini  →  modelo_ejercicio  →  evaluador
```

- **Nivel (1–3):** Perfil del alumno o **[dificultad adaptativa](guia-usuario.md#dificultad-adaptativa)** (5 aprobados seguidos ↑, 3 suspensos ↓, para todos los módulos) → `--nivel` en Python (`_instrucciones_nivel()`).
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

## Estructura del proyecto

```
examenforge/
├── alumno.txt / profesor.txt    # Guías por rol (instalaciones independientes)
├── doc_app/                     # Documentación del proyecto (esta carpeta)
├── install.sh / install.bat / install.ps1   # Instalación guiada (Linux/macOS / Windows)
├── iniciar-forja.sh / .bat      # Arranque tras instalar
├── arrancar-web.sh
├── generador.py / evaluador.py
├── generador_gemini.py / generador_docs.py / indexador_docs.py
├── modelo_ejercicio.py / tipo_materia.py / banco_loader.py
├── alias_comandos.py
├── comun.py                     # utilidades compartidas (slug, identificadores)
├── vocabulario_claves.json      # tipos_materia, prohibidas, alias_comandos
├── documentacion/               # PDFs de entrada (apuntes del profesor)
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

[← Índice de documentación](README.md)
