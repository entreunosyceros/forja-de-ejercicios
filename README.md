# Forja de ejercicios — *luego es tarde... para estudiar*

<img width="768" height="419" alt="forja-de-examenes" src="https://github.com/user-attachments/assets/7afefe21-33a0-4796-8376-8ce24c1ba2b2" />

**La máquina de ejercicios**: genera ejercicios prácticos aleatorios, permite practicar en Docker y corrige automáticamente. Se pueden añadir más ejercicios al gusto del consumidor.

## Estructura del proyecto

```
examenforge/
├── docker-compose.yml
├── Dockerfile.practica
├── generador.py              # Escenarios y criterios de corrección
├── evaluador.py              # Comprueba respuestas del alumno
├── web/                      # Panel Spring Boot + Thymeleaf
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── templates/inicio.html   # Secciones y botones de la portada
│   └── src/img/              # Logo (forja-de-examenes.png)
├── examenes/                 # PDFs generados
├── datos-practica/
└── pruebas/
```

## Módulos de ejercicio disponibles

| Módulo (`-m`) | Área | Contenido |
|---------------|------|-----------|
| `poo` | Certificado POO Java | Herencia, interfaces, excepciones, colecciones, encapsulamiento (aleatorio) |
| `bd_sql` | Certificado BD | Consultas `JOIN` |
| `bd_modelo` | Certificado BD | Normalización, PK/FK, 3FN |
| `bd_transacciones` | Certificado BD | `COMMIT` / `ROLLBACK` |
| `bd_jdbc` | Certificado BD + POO | `PreparedStatement` |
| `bd` | Certificado BD | `EXPLAIN`, índices |
| `redes`, `sistemas`, `docker`, `git` | Infraestructura | Administración de sistemas |

---

## Arranque de la aplicación web

### Sin Docker (desarrollo)

```bash
cd examenforge/web
mvn clean spring-boot:run
```

Abre **http://localhost:8080** (puerto por defecto).

> Ejecuta siempre desde `examenforge/web`. Así `forjaexamenes.raiz=../` apunta a `examenforge/` y encuentra `generador.py` y `evaluador.py`.

### Con Docker

```bash
cd examenforge
docker compose up -d --build
```

- Web: http://localhost:8080  
- Contenedor de práctica: `docker exec -it forjaexamenes-practica bash` (usuario `alumno` / contraseña `practica`)

### Cambiar el puerto

**Opción A — Fichero de configuración (recomendado)**

Edita `web/src/main/resources/application.properties`:

```properties
server.port=9090
```

**Opción B — Solo para una ejecución**

```bash
cd examenforge/web
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

O con el JAR:

```bash
java -jar target/forjaexamenes-web-1.0.0.jar --server.port=9090
```

**Opción C — Docker**

En `docker-compose.yml`, servicio `web`:

```yaml
ports:
  - "9090:8080"   # host:contenedor
```

El contenedor sigue escuchando en 8080 por dentro; desde el navegador usas http://localhost:9090.

---

## Solución de problemas al arrancar

| Síntoma | Causa habitual | Qué hacer |
|---------|----------------|-----------|
| `BindException: La dirección ya se está usando` | El puerto 8080 (u otro) está ocupado | Ver qué proceso lo usa: `ss -tlnp \| grep 8080` o `fuser 8080/tcp`. Detenerlo: `fuser -k 8080/tcp`, o cambiar `server.port` (ver arriba) |
| `No se encuentra generador.py` | Arranque desde carpeta incorrecta | Usar `cd examenforge/web` antes de `mvn spring-boot:run`, o definir `FORJAEXAMENES_RAIZ=/ruta/a/examenforge` |
| `generador.py falló` / `evaluador.py falló` | Python no instalado o error en el script | Comprobar: `python3 --version`. Probar: `python3 ../generador.py -m poo` desde `web/` |
| La web arranca pero al crear un ejercicio sale error | Misma causa que arriba | Revisar logs de la consola; validar rutas en `application.properties` |
| Cambios en HTML/CSS no se ven | Caché del navegador o `target/` antiguo | `mvn clean spring-boot:run` y recargar con **Ctrl+F5** |
| Logo o estilos viejos (p. ej. 280×280) | `target/classes` desactualizado | `cd web && mvn clean process-resources` y reiniciar la app |
| `mvn: command not found` | Maven no instalado | Instalar Maven 3.8+ y JDK 21 |
| Docker: web no responde | Contenedor no levantado o puerto distinto | `docker compose ps` y `docker compose logs web` |

### Comprobar que todo está bien

```bash
# Desde examenforge/
python3 generador.py -m poo --formateado | head

# Desde examenforge/web/ (con la app parada o en marcha)
mvn -q test
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/
```

Un `200` en el último comando indica que la portada responde.

---

## Cómo añadir una nueva sección en la portada

Una **sección** es el bloque visual de la página de inicio (tarjeta con título y botones). No genera ejercicios por sí sola: solo enlaza a módulos ya registrados en `generador.py`.

Archivo: **`web/src/main/resources/templates/inicio.html`**

### Añadir botones a una sección existente

Dentro del `<div class="card card-wide">` correspondiente, añade un enlace:

```html
<a class="btn btn-secondary" th:href="@{/ejercicio/nuevo(modulo='mi_modulo')}">Texto del botón</a>
```

`mi_modulo` debe existir en `generador.py` (tupla `MODULOS` y diccionario `GENERADORES`).

### Crear una sección nueva

Copia un bloque `card-wide` completo y adapta título y botones:

```html
<div class="card card-wide">
    <h2>Mi nueva sección</h2>
    <p class="hint">Descripción breve para el alumno.</p>
    <div class="module-buttons">
        <a class="btn btn-secondary" th:href="@{/ejercicio/nuevo(modulo='mi_modulo')}">Ejercicio 1</a>
        <a class="btn btn-secondary" th:href="@{/ejercicio/nuevo(modulo='otro_modulo')}">Ejercicio 2</a>
    </div>
</div>
```

No hace falta tocar Java: `ControladorEjercicio` ya acepta cualquier `?modulo=` válido.

Tras editar plantillas, reinicia la app o usa `mvn spring-boot:run` (con `spring.thymeleaf.cache=false` los cambios se ven al recargar).

---

## Cómo añadir un nuevo ejercicio (módulo)

Un **ejercicio** (módulo) es un tipo de problema que genera `generador.py` y corrige `evaluador.py` mediante criterios regex.

### 1. Crear la función generadora

En **`generador.py`**, añade una función que devuelva este diccionario:

```python
def generar_mi_tema() -> dict:
    parametro = random.choice(["valor1", "valor2"])
    return {
        "id": _generar_identificador(),
        "modulo": "mi_tema",              # identificador único, sin espacios
        "titulo": "Título corto del ejercicio",
        "enunciado": "Enunciado largo para el alumno...",
        "parametros": {"clave": parametro},   # opcional (auditoría / depuración)
        "criterios": [
            {"tipo": "regex", "patron": r"palabra_clave", "peso": 3},
            {"tipo": "regex", "patron": r"otro", "peso": 2, "flags": "i"},
        ],
        "solucion_referencia": "Respuesta modelo visible tras corregir",
    }
```

| Campo | Uso |
|-------|-----|
| `criterios` | Cada `patron` es una expresión regular sobre la respuesta del alumno |
| `peso` | Ponderación; nota = `10 × (peso cumplido / peso total)` |
| `flags: "i"` | Regex sin distinguir mayúsculas/minúsculas |
| Aleatoriedad | Usa `random.choice`, `randint`, etc. para variantes por alumno |

### 2. Registrar el módulo

Al inicio de `generador.py`:

```python
MODULOS = (
    # ... existentes ...
    "mi_tema",
)

GENERADORES = {
    # ... existentes ...
    "mi_tema": generar_mi_tema,
}
```

### 3. Varias preguntas en un solo módulo (patrón `poo`)

Varias funciones y una despachadora:

```python
def generar_poo() -> dict:
    return random.choice([
        generar_poo_herencia,
        generar_poo_interfaz,
        # ...
    ])()
```

En la web un solo botón: `modulo='poo'`.

### 4. Probar el ejercicio

```bash
cd examenforge
python3 generador.py -m mi_tema --formateado
python3 generador.py -m mi_tema -s /tmp/escenario.json
python3 evaluador.py -e /tmp/escenario.json -r "$(python3 -c "import json; print(json.load(open('/tmp/escenario.json'))['solucion_referencia'])")"
```

La última línea debería devolver `"nota": 10.0` si la solución de referencia cumple todos los criterios.

### 5. Mostrarlo en la portada

Añade el botón en `inicio.html` (ver sección anterior).

### 6. Añadir prueba automática

En **`pruebas/pruebas_generador_evaluador.py`**, incluye `"mi_tema"` en la tupla `MODULOS_PRUEBA`:

```bash
python3 pruebas/pruebas_generador_evaluador.py
```

### Consejos (certificado POO y BD)

- **POO**: pide código Java (`class`, `extends`, `implements`, `@Override`, `try-catch`).
- **BD**: pide SQL ejecutable (`SELECT`, `JOIN`, `CREATE TABLE`, `START TRANSACTION`).
- Evita patrones demasiado amplios (`.*`) o frases literales completas.
- Comprueba que `solucion_referencia` cumple **todos** los criterios antes de usarlo en clase.

> **`evaluador.py`** no suele necesitar cambios: lee el JSON del escenario y aplica los `criterios`. Solo modifícalo si cambias la estructura del JSON.

---

## Uso por línea de comandos

```bash
cd examenforge
python3 generador.py -m bd_sql --formateado
python3 evaluador.py -e escenario.json -r "respuesta del alumno"
```

## Pruebas

```bash
python3 pruebas/pruebas_generador_evaluador.py
cd web && mvn test
```

## Funciones para el alumno (sin red)

| Función | Descripción |
|---------|-------------|
| **Práctica infinita** | En resultado, «Otro ejercicio» recarga solo el contenido (AJAX), mismo módulo |
| **Ejercicio sorpresa** | `/ejercicio/nuevo?sorpresa=true` — módulo aleatorio |
| **Historial** | Últimos 5 ejercicios en `localStorage` (portada) |
| **Ranking personal** | Media de notas por módulo (solo tú, en tu navegador) |
| **Medallas** | Novato, Forjador, DBA, Completista, etc. (`progreso.js`) |
| **Gráfico** | Chart.js local en `static/vendor/` |
| **Temporizador** | Orientativo, sin penalización; se guarda en el historial |
| **Dificultad progresiva** | Nivel 1→3 según aprobados (`generador.py --nivel` + `Progreso.calcularNivel`) |
| **PDF detallado** | ✅/❌, esperado y pista por criterio fallido |
| **Exportar JSON** | `/ejercicio/{id}/json` — enunciado, parámetros, criterios, solución |
| **Modo profesor** | `FORJAEXAMENES_MODO_PROFESOR=true` — muestra la solución antes de enviar |

Scripts: `web/src/main/resources/static/js/progreso.js`, `practica.js`.

## Logo y tema

- Logo: `web/src/img/forja-de-examenes.png` → URL `/img/forja-de-examenes.png`
- Portada: banner 640×320 px centrado
- Tema **Claro** / **Oscuro** en la cabecera (se guarda en `localStorage`)

## Configuración (`application.properties`)

| Propiedad | Descripción |
|-----------|-------------|
| `server.port` | Puerto HTTP (por defecto `8080`) |
| `forjaexamenes.raiz` | Carpeta `examenforge/` (`FORJAEXAMENES_RAIZ`) |
| `forjaexamenes.directorio-examenes` | Carpeta de PDFs (`examenes/`) |
| `forjaexamenes.script-generador` | Ruta a `generador.py` |
| `forjaexamenes.script-evaluador` | Ruta a `evaluador.py` |
| `spring.thymeleaf.cache` | `false` en desarrollo (recarga plantillas al refrescar) |
