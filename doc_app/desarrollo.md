# Desarrollo

[← Índice](README.md) · [README principal](../README.md)

---

## Ampliar el proyecto

| Objetivo | Dónde |
|----------|--------|
| **Prompt enviado a Gemini** (enunciado, reglas, JSON) | `generador_gemini.py` → `_construir_prompt()` |
| **Tipo de materia** (informática / idiomas / general) | `tipo_materia.py`, `vocabulario_claves.json` → `tipos_materia`, `.forja-tipo` en carpeta |
| Parámetros de la llamada API (modelo, temperatura, timeout) | `generador_gemini.py` → `_generar_desde_fragmento_intento()`; timeout también en `application.properties` (`forjaexamenes.gemini-timeout-ms`) |
| Texto de la guía pública | `templates/fragments/guia-funcionamiento.html` |
| Vista de revisión profesor | `profesor-revisar-detalle.html`, `herramientas/revision_profesor.py` |
| Nuevos sinónimos de comandos | `vocabulario_claves.json` → `alias_comandos` |
| Textos de corrección (esperado / pista) | `retroalimentacion_criterios.py` + tabla `reglas_retroalimentacion_regex.json`; `evaluador.py` |
| Botón en portada | `templates/inicio.html` |
| Nuevo módulo plantilla | `plantillas/<modulo>.json` (+ opcional entrada en `generador.py`) |
| Nuevo tipo de criterio | Clase en `criterios.py` con `@registrar` |
| Flags de regex en criterios | Campo canónico `banderas` (legacy `flags` solo se lee). Migración banco: `python3 herramientas/migrar_banderas_banco.py` |
| Nuevo tema PDF | `documentacion/<tema>/` + indexar |
| Ejercicio verificado manual | `banco/aprobados/<tema>/<id>.json` + `reindexar` |
| Exportar/importar banco entre PCs | `ServicioBancoPortable.java`, `ControladorBanco.java` |
| Entregas y seguimiento alumnos | `ServicioEntregasAlumno.java`, `ControladorProfesorAlumnos.java` |
| Dificultad adaptativa (umbrales) | `ServicioDificultadAdaptativa.java` (constantes `APROBADOS_PARA_SUBIR` / `SUSPENSOS_PARA_BAJAR`) |
| Lenguaje del resaltado de sintaxis por módulo | `util/LenguajeResaltado.java` y `static/js/editor-codigo.js` → `resolverLenguaje()` |
| Utilidades compartidas (evitar duplicar) | Java: `util/` (`MapeadorJson`, `FechasForja`, `RutasUsuario`) · Python: `comun.py` (`slug`, `generar_identificador`) |

## Pruebas

```bash
# Deps de prueba (pytest, regex, hypothesis)
pip install -r requirements-dev.txt

# Todas las suites Python (recomendado)
pytest

# Integración Java (requiere Python; en CI va en job aparte)
mvn -f web/pom.xml test -Dgroups=integracion

# O una a una:
python3 pruebas/pruebas_generador_evaluador.py
python3 pruebas/pruebas_modelo_ejercicio.py
python3 pruebas/pruebas_evaluador_tipos.py
python3 pruebas/pruebas_alias_comandos.py
python3 pruebas/pruebas_banco_loader.py
python3 pruebas/pruebas_indexador_docs.py
python3 pruebas/pruebas_tipo_materia.py
python3 pruebas/pruebas_texto_plano.py
python3 pruebas/pruebas_plantillas_criterios.py
python3 pruebas/pruebas_redos_y_basura.py
python3 pruebas/pruebas_retroalimentacion_criterios.py
python3 pruebas/pruebas_regresion_corrector.py
# Opcional (hypothesis): nota siempre en [0, 10]
pytest pruebas/pruebas_hypothesis_nota.py
cd web && mvn test
```

Cobertura clave del corrector: falsos positivos de límites de palabra, pesos/banderas inválidos, ReDoS, y que **toda** `solucion_referencia` de `banco/aprobados/**/*.json` saque un 10.

Hay integración continua en `.github/workflows/ci.yml` (`pytest` + `mvn test` con JDK 21).

## Licencia y agradecimientos

Creado por entreunosyceros con ☕ y 🚬 para el ICFT0112.

Este proyecto no habría sido posible sin el increíble trabajo de las comunidades de código abierto que mantienen las herramientas y bibliotecas que utiliza:

- [Spring Boot](https://spring.io/projects/spring-boot) — framework web.
- [Thymeleaf](https://www.thymeleaf.org/) — motor de plantillas.
- [Python](https://www.python.org/) y sus bibliotecas: `pypdf`, `google-genai`, `rapidfuzz`, etc.
- [SQLite](https://www.sqlite.org/) — base de datos ligera.
- [Docker](https://www.docker.com/) — contenedor de práctica.
- [Highlight.js](https://highlightjs.org/) — resaltado de sintaxis en el navegador.

Ver [LICENSE](../LICENSE).

---

[← Índice de documentación](README.md)
