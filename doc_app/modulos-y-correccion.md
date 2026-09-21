# Módulos y corrección

[← Índice](README.md) · [README principal](../README.md)

---

## Módulos disponibles

| Módulo | Contenido |
|--------|-----------|
| `poo` | POO Java (funciones en `generador.py`) |
| `bd_sql`, `bd_modelo`, `bd_transacciones`, `bd_jdbc` | Bases de datos (funciones en `generador.py`) |
| `bd`, `redes`, `docker`, `git` | Plantillas JSON en `plantillas/` |
| `sistemas` | Infraestructura (función en `generador.py`) |
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

Tipos de criterio soportados: `regex`, `contiene_todos`, `contiene_alguno`, `no_contiene` (y flag `obligatorio`).

## Cómo se puntúa (reglas del corrector)

| Regla | Detalle |
|-------|---------|
| Límites de palabra | `cat` no coincide dentro de `concatenar`; `ls` no dentro de `false`. Negar un comando (`no uses chmod`) **sí** encuentra la palabra: usa `no_contiene` u `obligatorio`. |
| `contiene_todos` | Nota **parcial** (4 de 5 términos → 80 % de ese peso). |
| `obligatorio` | Si falla, la nota queda tope 4 (suspenso). |
| Pesos | Texto, negativo o 0 se normalizan a entero ≥ 1; la nota final siempre queda en 0–10. |
| `banderas` | Campo canónico de flags regex (`i`, `m`, `s` o `ignorecase` / `multiline` / `dotall`). El legado `flags` solo se lee. `"multiline"` **no** activa IGNORECASE. |
| Interpolación | En plantillas y generador, las variables (ruta, archivo, VLAN, tabla…) se insertan con `re.escape` para que un punto en `Main.java` no sea «cualquier carácter». |
| ReDoS | Al validar el banco se rechazan cuantificadores anidados (`(a+)+`). En evaluación hay tope de longitud y timeout si está `regex`. |
| Pistas | Texto claro desde [`reglas_retroalimentacion_regex.json`](../reglas_retroalimentacion_regex.json); no se publican tokens de clase (`A-Z`). |

Un test recorre **todo** `banco/aprobados/**/*.json` y exige que cada `solucion_referencia` saque un 10.

## Qué mide la nota (honestidad)

La nota automática mide **elementos comprobables** (términos, regex). En pantalla aparece como
«X sobre los elementos que sé comprobar». No pretende ser un corrector humano.

### Segundo corrector con IA (opcional)

Con `FORJAEXAMENES_CORRECTOR_IA=true` (y clave Gemini), tras la nota por elementos se pide a Gemini
si la respuesta **resuelve el enunciado**. La nota final es, por defecto, el **mínimo** de ambas
(`FORJAEXAMENES_CORRECTOR_IA_MODO=ponderada` usa 60 % elementos + 40 % IA). Si la API falla, se
conserva solo la nota por elementos.

### Retroalimentación al corregir

Tras enviar una respuesta, la pantalla de resultado muestra **criterios en lenguaje claro** (qué se esperaba y una **pista** distinta si fallas), no expresiones regulares crudas. La lógica está en [`retroalimentacion_criterios.py`](../retroalimentacion_criterios.py) (tabla editable [`reglas_retroalimentacion_regex.json`](../reglas_retroalimentacion_regex.json)) y se aplica al generar (`generador.py`) y al evaluar (`evaluador.py`). En **modo profesor** puedes ver el patrón técnico del criterio.

---

[← Índice de documentación](README.md)
