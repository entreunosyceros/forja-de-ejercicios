# Política de seguridad

## Versiones con soporte

| Versión | Soportada |
| ------- | --------- |
| 1.0.x   | ✅        |
| < 1.0   | ❌        |

## Alcance

**Forja de ejercicios** es una aplicación web (Spring Boot + Python) pensada principalmente para uso en el aula o en equipos locales. En seguridad nos interesa especialmente:

- **Autenticación y autorización:** acceso indebido a rutas `/profesor/**`, elevación de privilegios o bypass de roles.
- **CSRF / sesión:** envío de formularios sin token válido o fijación de sesión.
- **Inyección y ejecución de código:** subida de ficheros maliciosos (PDF, JSON de banco/entrega), inyección en plantillas o ejecución no controlada de subprocesos Python.
- **Exposición de secretos:** claves `GEMINI_API_KEY` en `.env`, `datos/gemini.json`, logs o repositorio Git.
- **Datos de alumnos:** fugas en entregas JSON, historial (`datos/historial/`) o estadísticas exportadas.
- **Dependencias** con vulnerabilidades conocidas (Maven, pip).
- **Contenedor Docker de práctica:** escape o montajes inseguros en `docker-compose.yml` / `Dockerfile.practica`.

**Fuera de alcance habitual:** disponibilidad de la API de Gemini, cuotas o cambios de modelos de Google; fallos por PDF corruptos; uso de contraseñas por defecto en instalaciones de demostración sin exposición a Internet.

## Cómo reportar una vulnerabilidad

1. **No** abras un issue público con detalles del fallo de seguridad.
2. Usa [GitHub Security Advisories](https://github.com/entreunosyceros/forja-de-ejercicios/security/advisories/new) (**Report a vulnerability**) si la opción está habilitada.
3. Si no puedes usar Advisories, abre un issue con el título `SECURITY (sin detalles)` y solicita un canal privado; no incluyas pasos de explotación en el tablón público.

Incluye, en la medida de lo posible:

- Descripción del problema y componente afectado (Java, Python, plantilla, Docker…).
- Pasos para reproducir el fallo.
- Impacto estimado (local, red del aula, exposición en Internet).
- Versión o commit afectado.
- Sugerencia de mitigación, si dispones de ella.

## Qué esperar

- **Acuse de recibo:** evaluación inicial en un plazo razonable de pocos días.
- **Resolución:** parche o mitigación en una versión posterior si procede.
- **Créditos:** reconocimiento al informante en las notas de la release, salvo anonimato expreso.

## Buenas prácticas para usuarios

- **Contraseñas:** cambia las cuentas por defecto (`alumno`/`practica`, `profesor`/`profesor`) antes de exponer la app en red; ver [doc_app/cuenta-perfil.md](doc_app/cuenta-perfil.md).
- **Gemini:** no subas `.env` ni `datos/gemini.json` a GitHub; usa `.env.example` como plantilla.
- **Exposición en red:** la app no está diseñada como servicio multi-tenant en Internet; si la publicas, limita el acceso (firewall, VPN del centro).
- **Entregas y banco:** los JSON exportados pueden contener respuestas de alumnos; trátalos como datos personales según la normativa de tu centro.
- **Actualizaciones:** mantén JDK, Maven, Python y dependencias actualizadas; recompila el JAR tras actualizar el código.

Repositorio oficial: [https://github.com/entreunosyceros/forja-de-ejercicios](https://github.com/entreunosyceros/forja-de-ejercicios)
