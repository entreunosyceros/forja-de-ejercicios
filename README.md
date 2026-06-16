# Forja de ejercicios — *luego es tarde... para estudiar*

<img width="768" height="419" alt="forja-de-examenes" src="https://github.com/user-attachments/assets/7afefe21-33a0-4796-8376-8ce24c1ba2b2" />

[![Wiki](https://img.shields.io/badge/Wiki-DeepWiki-blue?style=for-the-badge&logo=wikipedia)](https://deepwiki.com/entreunosyceros/forja-de-ejercicios/)
![Version: 1.0.0](https://img.shields.io/badge/version-1.0.0-brightgreen)

Genera ejercicios prácticos al azar (informática, idiomas u otras materias desde PDF), permite practicar en Docker y corrige la respuesta del alumno con criterios verificables. La interfaz es **Spring Boot**; la generación y corrección las hace **Python** (`generador.py`, `evaluador.py`, `modelo_ejercicio.py`, `tipo_materia.py`).

> 📘 **Documentación ampliada:** [doc_app/README.md](doc_app/README.md) · guías por rol ([alumno.txt](alumno.txt) · [profesor.txt](profesor.txt)) · ayuda en la web (**/como-funciona**). [DeepWiki](https://deepwiki.com/entreunosyceros/forja-de-ejercicios/) es complementaria.

---

## Inicio rápido

### Instalar

| Sistema | Comando |
|---------|---------|
| Linux / macOS | `chmod +x install.sh iniciar-forja.sh && ./install.sh` |
| Windows | `install.bat` (doble clic o desde cmd/PowerShell) |

Detalle (Windows, Docker, arranque manual): [doc_app/instalacion.md](doc_app/instalacion.md).

### Arrancar

| Sistema | Comando |
|---------|---------|
| Linux / macOS | `./iniciar-forja.sh` |
| Windows | `.\iniciar-forja.bat` |

Abre **http://localhost:8080** cuando veas `Started AplicacionForjaExamenes`.

### Acceso

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Alumno | `alumno` | `practica` |
| Alumno (demo) | `demo` | `demo` |
| Profesor | `profesor` | `profesor` |

---

## Prueba rápida (2 minutos)

1. Inicia sesión como **alumno** (`alumno` / `practica`).
2. En la portada, pulsa el módulo **POO** (o **«Empezar ahora»**).
3. Escribe tu solución (con **resaltado de sintaxis**) y pulsa **Enviar**.
4. Verás la nota (0–10), la retroalimentación y tu respuesta corregida.
5. En la portada aparece tu progreso y estadísticas.

---

## Documentación

| Tema | Enlace |
|------|--------|
| **Índice completo** | [doc_app/README.md](doc_app/README.md) |
| Instalación y arranque | [instalacion.md](doc_app/instalacion.md) |
| Uso (alumno / profesor) | [guia-usuario.md](doc_app/guia-usuario.md) |
| Aula con PCs independientes | [aula-compartida.md](doc_app/aula-compartida.md) |
| Cuenta y perfil | [cuenta-perfil.md](doc_app/cuenta-perfil.md) |
| Revisión del banco (IA) | [revision-banco.md](doc_app/revision-banco.md) |
| Módulos y corrección | [modulos-y-correccion.md](doc_app/modulos-y-correccion.md) |
| Apuntes PDF + Gemini | [apuntes-gemini.md](doc_app/apuntes-gemini.md) |
| Arquitectura | [arquitectura.md](doc_app/arquitectura.md) |
| Configuración | [configuracion.md](doc_app/configuracion.md) |
| Problemas frecuentes | [problemas-frecuentes.md](doc_app/problemas-frecuentes.md) |
| Desarrollo y pruebas | [desarrollo.md](doc_app/desarrollo.md) |
| PDFs de apuntes (carpeta) | [documentacion/README.md](documentacion/README.md) |

---

## Resumen

- **Alumno:** elige módulo, practica, recibe corrección automática; opcionalmente importa el banco del profesor y exporta su entrega.
- **Profesor:** practica igual, revisa propuestas de IA, aprueba ejercicios, exporta el banco y hace seguimiento de la clase.
- **Sin Gemini:** los módulos clásicos (`poo`, `bd_sql`, `docker`…) funcionan sin clave API.
- **Con Gemini:** sube PDFs a `documentacion/` para módulos `docs_*` (ver [apuntes-gemini.md](doc_app/apuntes-gemini.md)).

En el aula cada PC suele tener su propia instalación; el intercambio es por **carpeta compartida** (banco y entregas). Ver [aula-compartida.md](doc_app/aula-compartida.md).

---

## Licencia

Creado por entreunosyceros para el ICFT0112. Ver [LICENSE](LICENSE) y [desarrollo.md](doc_app/desarrollo.md#licencia-y-agradecimientos).

## Comunidad

- [Contribuir](CONTRIBUTING.md) · [Seguridad](SECURITY.md) · [Código de conducta](CODE_OF_CONDUCT.md)
- Repositorio: [github.com/entreunosyceros/forja-de-ejercicios](https://github.com/entreunosyceros/forja-de-ejercicios)
