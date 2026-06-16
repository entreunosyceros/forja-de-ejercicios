# Apuntes PDF y Gemini

[← Índice](README.md) · [README principal](../README.md)

---

```bash
cd examenforge
pip install -r requirements-docs.txt
```

**Clave Gemini:** en la web → **Perfil → Clave API de Gemini** (se guarda en `datos/gemini.json` y `.env`), o manualmente en `.env`:

```bash
cp .env.example .env   # GEMINI_API_KEY desde https://aistudio.google.com/apikey
```

El instalador (`install.sh` / `install.bat`) también puede guardar la clave en ambos sitios.

Coloca PDFs en `documentacion/<tema>/` → módulo `docs_<tema>` (p. ej. `documentacion/docker/` → `docs_docker`, `documentacion/ingles/` → `docs_ingles`).

**Desde la web:** portada → *Apuntes del profesor* → **Subir e indexar**, o botón **Actualizar apuntes** tras copiar PDFs.

**Indexación manual:** `python3 indexador_docs.py` (también al arrancar la web si hay PDFs nuevos).

Estructura de carpetas y subida: **[documentacion/README.md](../documentacion/README.md)** (carpeta de PDFs, distinta de `doc_app/`).

## Tipos de materia (apuntes PDF)

Cada carpeta `documentacion/<tema>/` genera un módulo `docs_<tema>`. El sistema detecta el **tipo de materia** y adapta el prompt de Gemini, la validación de palabras clave y el texto del enunciado.

| Tipo | Carpetas reconocidas (ejemplos) | Qué pide el ejercicio |
|------|----------------------------------|------------------------|
| `informatica` | `docker`, `git`, `linux`, `forense`, `sql`… | Comandos, código o pasos técnicos |
| `idiomas` | `ingles`, `english`, `frances`, `aleman`… | Traducción, gramática, vocabulario del fragmento |
| `general` | cualquier otra (`historia`, `arte`, `economia`…) | Respuesta escrita basada en el fragmento |

**Prioridad de detección:**

1. Fichero `documentacion/<tema>/.forja-tipo` (o `tipo_materia.txt`) con una línea: `informatica`, `idiomas` o `general`.
2. Reglas en [`vocabulario_claves.json`](../vocabulario_claves.json) → sección `tipos_materia` o `tipo_materia` por módulo.
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

[← Índice de documentación](README.md)
