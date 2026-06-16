# Apuntes del profesor (PDF)

> **Nota:** esta carpeta (`documentacion/`) es donde van los **PDF de apuntes**. La documentación del proyecto está en [`doc_app/`](../doc_app/README.md).

## Flujo

```
PDFs del profesor  →  indexador_docs.py  →  fragmentos (JSON)  →  Gemini  →  ejercicio
```

**Sin tocar código:** cada carpeta aquí genera un módulo automático.

| Carpeta | Módulo en la app |
|---------|------------------|
| `documentacion/ansible/` | `docs_ansible` |
| `documentacion/terraform/` | `docs_terraform` |
| `documentacion/kubernetes/` | `docs_kubernetes` |
| `documentacion/docker/volumenes/` | `docs_docker` (capítulo *volumenes*) |

## Estructura recomendada

```
documentacion/
├── ansible/
│   └── apuntes.pdf
├── terraform/
├── kubernetes/
└── docker/
    ├── redes/
    │   └── tema-redes.pdf
    ├── volumenes/
    │   └── persistentes.pdf
    └── compose/
        └── stacks.pdf
```

- **Carpeta de primer nivel** = tema (`docker` → `docs_docker`).
- **Subcarpetas** = capítulos (Volúmenes, Redes, Compose…).
- **Títulos dentro del PDF** = sección (se detectan al indexar).

## Tipo de materia (cualquier asignatura)

Por defecto, cualquier carpeta nueva se trata como materia **general** (historia, economía, etc.).
Algunas carpetas se reconocen automáticamente:

| Tipo | Carpetas reconocidas (ejemplos) | Ejercicios generados |
|------|----------------------------------|----------------------|
| `informatica` | `docker`, `git`, `linux`, `forense`… | Comandos, código, pasos técnicos |
| `idiomas` | `ingles`, `english`, `frances`… | Traducción, gramática, vocabulario |
| `general` | cualquier otra (`historia`, `arte`…) | Respuesta escrita basada en el fragmento |

Para fijar el tipo manualmente, crea en la carpeta del tema:

```
documentacion/ingles/.forja-tipo
```

Contenido del fichero (una línea): `idiomas`, `informatica` o `general`.

También puedes añadir reglas en `vocabulario_claves.json` (`tipo_materia` por módulo).

## Indexar

**Automático:** al arrancar la aplicación web (si hay PDFs aquí).

**Manual** (o tras añadir carpetas sin reiniciar):

```bash
cd examenforge
pip install -r requirements-docs.txt
python3 indexador_docs.py
```

En la portada: botón **«Actualizar apuntes»**.

Genera `indice/docs_<tema>.json` con metadatos por fragmento:

- `tema`, `capitulo`, `seccion`, `pagina`, `fragmento_id`, `fragmento` (texto)

## Generar ejercicio

```bash
export GEMINI_API_KEY="..."
# Aleatorio en todo el tema
python3 generador.py -m docs_docker --formateado
# Solo capítulo Volúmenes
python3 generador.py -m docs_docker -c volumenes --formateado
```

En la web: portada → **Desde tus apuntes** → elige tema o capítulo.

Si el alumno **suspende**, verá de qué página y sección salió la pregunta (repaso inmediato).
