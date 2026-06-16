# Guía de contribución

¡Gracias por interesarte en **Forja de ejercicios** ([forja-de-ejercicios](https://github.com/entreunosyceros/forja-de-ejercicios))! Este proyecto es software libre (GPL-3.0): genera ejercicios prácticos, permite practicar en Docker y corrige respuestas con criterios verificables. Cualquier mejora bien planteada es bienvenida.

## Antes de empezar

- Lee el [README](README.md) y la [documentación ampliada](doc_app/README.md).
- Guías por rol: [alumno.txt](alumno.txt) y [profesor.txt](profesor.txt).
- Revisa las [issues abiertas](https://github.com/entreunosyceros/forja-de-ejercicios/issues) por si alguien ya trabaja en lo mismo.
- Consulta el [Código de conducta](CODE_OF_CONDUCT.md).
- Para vulnerabilidades, sigue [SECURITY.md](SECURITY.md) (no abras issues públicas con detalles de explotación).

## Cómo puedes ayudar

- **Reportar errores** en instalación, generación, corrección, Gemini, banco o zona profesor.
- **Proponer mejoras** explicando el caso de uso (alumno, profesor o aula con PCs independientes).
- **Enviar pull requests** acotados y probados.
- **Mejorar documentación** (README, `doc_app/`, ayuda web en `/como-funciona`).
- **Añadir o afinar módulos** en `generador.py`, criterios en `evaluador.py` o alias en `vocabulario_claves.json`.
- **Corregir prompts o validación** de ejercicios `docs_*` (PDF + Gemini).

## Entorno de desarrollo

Requisitos: **JDK 21**, **Maven 3.8+**, **Python 3.10+**.

```bash
git clone https://github.com/entreunosyceros/forja-de-ejercicios.git
cd forja-de-ejercicios
chmod +x install.sh iniciar-forja.sh
./install.sh
./iniciar-forja.sh
```

En Windows: `install.bat` y luego `iniciar-forja.bat`.

La web queda en **http://localhost:8080** (usuarios por defecto: `alumno`/`practica`, `profesor`/`profesor`).

### Arranque manual (desarrollo)

```bash
mvn -f web/pom.xml spring-boot:run
```

Opcional para PDF + Gemini:

```bash
pip install -r requirements-docs.txt
cp .env.example .env   # GEMINI_API_KEY si pruebas docs_*
```

### Pruebas

```bash
python3 pruebas/pruebas_generador_evaluador.py
# … resto de suites en pruebas/
cd web && mvn test
```

Detalle en [doc_app/desarrollo.md](doc_app/desarrollo.md).

## Áreas del código

| Área | Ubicación habitual |
|------|-------------------|
| Web Spring Boot | `web/src/main/java/`, `web/src/main/resources/templates/` |
| Generación plantillas | `generador.py` |
| Corrección | `evaluador.py`, `retroalimentacion_criterios.py`, `alias_comandos.py` |
| PDF + Gemini | `indexador_docs.py`, `generador_docs.py`, `generador_gemini.py`, `modelo_ejercicio.py` |
| Tipos de materia | `tipo_materia.py`, `vocabulario_claves.json` |
| Banco portable | `banco_loader.py`, `ServicioBancoPortable.java` |
| Revisión profesor (CLI) | `herramientas/revisar_banco.py`, `revision_profesor.py` |
| Instaladores | `install.sh`, `install.ps1`, `install.bat` |

## Estilo de código

- Sigue el estilo del código existente (nombres, imports, nivel de comentarios).
- Cambios **mínimos y enfocados**: no mezcles varias funcionalidades en un mismo PR.
- Los textos visibles para el usuario van en **español**.
- No incluyas secretos (`.env`, `datos/gemini.json`), contraseñas ni datos reales de alumnos en commits o issues.
- Los PDF de apuntes del profesor van en `documentacion/` (carpeta de entrada); no subas material con copyright sin permiso.

## Pull requests

1. Crea una rama descriptiva desde `main` (por ejemplo `fix/csrf-guardar-banco` o `feat/modulo-nuevo`).
2. Describe **qué** cambias y **por qué**.
3. Indica cómo lo has probado (módulo, pasos manuales, tests).
4. Actualiza README o `doc_app/` solo si el cambio lo requiere.

Usa la [plantilla de pull request](.github/pull_request_template.md).

## Reportar problemas

- **Bugs y mejoras:** [plantillas de Issues](https://github.com/entreunosyceros/forja-de-ejercicios/issues/new/choose).
- **Preguntas de uso:** plantilla «Pregunta o ayuda».
- **Seguridad:** [SECURITY.md](SECURITY.md).

## Licencia

Al contribuir, aceptas que tu aportación se publique bajo la misma licencia del proyecto: [GPL-3.0](LICENSE).
