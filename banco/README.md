# Banco de ejercicios verificables

## Estructura

```
banco/
  aprobados/       Ejercicios listos (portada + generador)
  pendientes/      Propuestas Gemini en espera de revisión
  catalogo.json    Índice regenerado al arrancar la app
```

## Formato JSON (aprobados)

Mismo esquema que `generador.py`. Tipos de criterio: `regex`, `contiene_todos`, `contiene_alguno`.

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
