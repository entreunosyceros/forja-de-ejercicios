# Módulos y corrección

[← Índice](README.md) · [README principal](../README.md)

---

## Módulos disponibles

| Módulo | Contenido |
|--------|-----------|
| `poo` | POO Java (certificado) |
| `bd_sql`, `bd_modelo`, `bd_transacciones`, `bd_jdbc`, `bd` | Bases de datos |
| `redes`, `sistemas`, `docker`, `git` | Infraestructura |
| `docs_*` | Apuntes PDF indexados + Gemini (cualquier asignatura; ver [Apuntes PDF y Gemini](apuntes-gemini.md#tipos-de-materia-apuntes-pdf)) |
| `banco_*` | Ejercicios JSON en `banco/aprobados/` |

CLI:

```bash
python3 generador.py -m poo --formateado
python3 evaluador.py -e escenario.json -r "respuesta del alumno"
```

## Corrección elástica (alias de comandos)

Los criterios `contiene_todos` y `contiene_alguno` aceptan **sinónimos técnicos** definidos en [vocabulario_claves.json](../vocabulario_claves.json) → sección `alias_comandos`.

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

Tras enviar una respuesta, la pantalla de resultado muestra **criterios en lenguaje claro** (qué se esperaba y una **pista** distinta si fallas), no expresiones regulares crudas. La lógica está en [`retroalimentacion_criterios.py`](../retroalimentacion_criterios.py) y se aplica al generar (`generador.py`) y al evaluar (`evaluador.py`). En **modo profesor** puedes ver el patrón técnico del criterio.

---

[← Índice de documentación](README.md)
