# Revisión del banco (profesor)

[← Índice](README.md) · [README principal](../README.md)

---

Flujo recomendado para validar ejercicios generados desde apuntes **en el equipo del profesor** y luego compartirlos con la clase:

1. Activa la cola de revisión en **`/profesor/revisar`** o **`/profesor/banco`** (casilla «Guardar propuestas de IA en banco/pendientes/») → cada ejercicio `docs_*` se guarda en `banco/pendientes/`.
2. Entra como **profesor** / **profesor**.
3. Abre **Perfil → Revisar propuestas** o ve a `/profesor/revisar`.
4. Pulsa **Revisar** en un pendiente. Verás:
   - **Comparativa** propuesta Gemini vs criterios del escenario (palabras clave, enunciado).
   - **Tabla de criterios** con pruebas automáticas: solución de referencia (debe pasar), variante con sinónimo (p. ej. `docker container run`) e respuesta insuficiente (debe fallar).
   - **Alias** aplicados a cada término (desde `vocabulario_claves.json`).
5. **Aprobar y publicar**, **Rechazar** o **Descargar paquete ZIP** desde la misma pantalla.
6. **Exportar** el banco o ejercicios sueltos a la carpeta compartida para que los alumnos los importen (ver [Compartir ejercicios del banco](aula-compartida.md#compartir-ejercicios-del-banco-carpeta-compartida)).

> «Aprobar y publicar» solo actualiza el banco **local** del profesor. Los alumnos en otros PCs necesitan el paso de exportar/importar.

## Herramientas CLI (equivalente a la web)

```bash
# Datos de revisión en JSON (tabla + casos de prueba)
python3 herramientas/revision_profesor.py --id <id_pendiente>

# Carpeta con revision.html, revision.json, enunciado y solución
python3 herramientas/paquete_entrega.py <id_pendiente>

# Aprobar / rechazar / listar
python3 herramientas/revisar_banco.py listar
python3 herramientas/revisar_banco.py aprobar <id>
python3 herramientas/revisar_banco.py rechazar <id>
python3 herramientas/revisar_banco.py reindexar
```

---

[← Índice de documentación](README.md)
