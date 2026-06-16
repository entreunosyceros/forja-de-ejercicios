# Cuenta y perfil

[← Índice](README.md) · [README principal](../README.md)

---

| Acción | Dónde |
|--------|--------|
| Entrar | `/login` |
| Cambiar nombre, usuario o contraseña | `/perfil` (enlace **Perfil** en la cabecera) |
| Guía técnica (Gemini, PDF, nuevos módulos) | `/perfil` (sección inferior) o `/como-funciona` |
| Clave API de Gemini | `/perfil` → requiere **contraseña de acceso** (no la clave API) |
| Modelo de Gemini | `/perfil` → **Guardar modelo** (p. ej. `gemini-2.5-flash`; no pide contraseña) |
| Dificultad de ejercicios (1–3) | `/perfil` → **«Dificultad de ejercicios»** (manual); también se ajusta sola, ver [Dificultad adaptativa](guia-usuario.md#dificultad-adaptativa) |
| **Seguimiento de alumnos** (profesor) | `/profesor/alumnos` → importar entrega o ver alumnos del servidor |
| **Resultados y CSV** (profesor) | `/profesor/resultados` → revisar intentos y exportar CSV |
| Descargar entrega para el profesor (**solo alumno**) | Portada → «Descargar entrega para el profesor» |
| Importar banco del profesor (alumno) | Portada → «Ejercicios del banco» → «Importar al banco local» |
| Exportar banco completo (profesor) | `/profesor/banco` → «Descargar banco completo» |
| Guardar ejercicio en el banco (profesor) | Ejercicio o resultado → nombre del fichero + «Guardar en banco» (plantilla o IA) |
| **Revisar propuestas IA** (profesor) | `/profesor/revisar` → detalle en `/profesor/revisar/{id}` |
| Vista rápida del banco (profesor) | `/profesor/banco` |
| Descargar paquete de revisión (profesor) | `/profesor/revisar/{id}/paquete` (ZIP) |
| Limpiar progreso local (navegador) | Portada → **Tu progreso (local, sin red)** → «Limpiar progreso local» |
| Limpiar estadísticas de uso (servidor) | Portada → **Tus estadísticas de uso (servidor)** → «Limpiar estadísticas» |

## Usuarios y roles

- Los logins listados en `forjaexamenes.login.profesores` (o `FORJAEXAMENES_PROFESORES`) tienen rol **PROFESOR** y acceden a `/profesor/**`.
- Los demás son **ALUMNO**.
- Tras el primer arranque, las contraseñas quedan guardadas (cifradas) en `datos/usuarios.json`. Cambiar `application.properties` **no** actualiza cuentas que ya existen en ese fichero.

`FORJAEXAMENES_MODO_PROFESOR=true` sigue siendo útil para **mostrar la solución** antes de enviar en cualquier cuenta; la **zona de revisión** (`/profesor/*`) requiere rol profesor.

## Cambiar contraseñas por defecto

| Situación | Qué archivo tocar | Qué hacer |
|-----------|-------------------|-----------|
| **Instalación nueva** (aún no existe `datos/usuarios.json`) | [`web/src/main/resources/application.properties`](../web/src/main/resources/application.properties) | Edita `forjaexamenes.login.usuarios` y, si hace falta, `forjaexamenes.login.profesores`. Formato: `usuario:contraseña` separados por coma. |
| **Misma máquina, sin tocar el JAR** | [`examenforge/.env`](../.env) (créalo si no está) | Añade `FORJAEXAMENES_USUARIOS=alumno:tu_clave,demo:demo,profesor:clave_profesor` y opcionalmente `FORJAEXAMENES_PROFESORES=profesor`. Reinicia la app. Solo aplica si `datos/usuarios.json` **no** existe todavía. |
| **La app ya se ha usado** | — (no edites `usuarios.json` a mano) | Entra en **`/perfil`** y cambia la contraseña con la actual. Es la forma habitual en producción. |
| **Resetear todas las cuentas** | Borra `datos/usuarios.json` | Para la app, ajusta `application.properties` o `.env` como arriba y vuelve a arrancar: se regeneran los usuarios con las claves nuevas. Pierdes perfiles y estadísticas de esos logins. |

Ejemplo en `application.properties` (líneas ~53–55):

```properties
forjaexamenes.login.usuarios=${FORJAEXAMENES_USUARIOS:alumno:practica,demo:demo,profesor:profesor}
forjaexamenes.login.profesores=${FORJAEXAMENES_PROFESORES:profesor}
```

Ejemplo en `.env` en la raíz del proyecto (`examenforge/.env`):

```bash
FORJAEXAMENES_USUARIOS=alumno:mi_clase_2026,demo:demo,profesor:clave_segura
FORJAEXAMENES_PROFESORES=profesor
```

> **Nota:** el contenedor Docker de práctica (`docker compose … practica`) usa su propio usuario `alumno`/`practica`; es independiente del login web. Para cambiarlo, edita [`docker-compose.yml`](../docker-compose.yml) o la imagen del servicio `practica`.

---

[← Índice de documentación](README.md)
