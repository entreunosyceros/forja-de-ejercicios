# Banco de ejercicios verificables

## Estructura

```
banco/
  aprobados/       Ejercicios listos (portada + generador)
  pendientes/      Propuestas Gemini en espera de revisión
  catalogo.json    Índice regenerado al arrancar / importar / aprobar
                   (no se versiona; está en .gitignore)
```

No uses copias bajo `web/banco/` ni `web/datos/`: el runtime lee la raíz del proyecto.

## Formato JSON (aprobados)

Mismo esquema que `esquema_escenario.json` / `generador.py`.

Tipos de criterio: `regex`, `contiene_todos`, `contiene_alguno`, `no_contiene`.
Campo de flags regex: **`banderas`** (`i`, `m`, `s`…). El legado `flags` se acepta al leer.
Opcional: `"obligatorio": true` (si falla, tope de nota 4).

La `solucion_referencia` debe cumplir **todos** los criterios (nota 10). pytest lo comprueba
en `pruebas/pruebas_regresion_corrector.py`.

Migración one-shot de `flags` → `banderas`:

```bash
python3 herramientas/migrar_banderas_banco.py
```

## CLI

```bash
python3 herramientas/revisar_banco.py listar
python3 herramientas/revisar_banco.py mostrar <id>
python3 herramientas/revisar_banco.py aprobar <id>
python3 herramientas/revisar_banco.py rechazar <id>
python3 herramientas/revisar_banco.py reindexar
```

## Web (modo profesor)

`FORJAEXAMENES_MODO_PROFESOR=true` → Perfil → «Revisar banco (IA)» o `/profesor/banco`
