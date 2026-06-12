#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Generador de escenarios aleatorios para La máquina de ejercicios."""

import argparse
import json
import random
import re
import sys
from datetime import datetime, timezone

import comun

# MODULOS se completa al final del archivo (incluye docs_* si hay indice/)
MODULOS: tuple[str, ...] = ()


def _generar_identificador() -> str:
    return comun.generar_identificador()


def generar_redes() -> dict:
    v1, v2 = random.sample([10, 20, 30, 40, 50, 100], 2)
    puerto = random.randint(8000, 9999)
    return {
        "id": _generar_identificador(),
        "modulo": "redes",
        "titulo": f"VLAN {v1} no comunica con VLAN {v2}",
        "enunciado": (
            f"En el switch del aula, la VLAN {v1} (192.168.{v1}.0/24) no puede "
            f"comunicar con la VLAN {v2} (192.168.{v2}.0/24). El servicio en "
            f"puerto {puerto} tampoco responde entre subredes.\n\n"
            "Indica los comandos que ejecutarías para diagnosticar y habilitar "
            "el enrutamiento entre VLANs (ip route, vlan, sysctl, etc.)."
        ),
        "parametros": {"vlan_a": v1, "vlan_b": v2, "puerto": puerto},
        "criterios": [
            {"tipo": "regex", "patron": r"(vlan|ip\s+link)", "peso": 1},
            {"tipo": "regex", "patron": rf"vlan.*{v1}|{v1}.*vlan", "peso": 2},
            {"tipo": "regex", "patron": rf"vlan.*{v2}|{v2}.*vlan", "peso": 2},
            {"tipo": "regex", "patron": r"ip\s+route|ip\s+routing|sysctl.*ip_forward", "peso": 3},
        ],
        "solucion_referencia": (
            f"ip link add link eth0 name eth0.{v1} type vlan id {v1}\n"
            f"ip link add link eth0 name eth0.{v2} type vlan id {v2}\n"
            f"ip addr add 192.168.{v1}.1/24 dev eth0.{v1}\n"
            f"ip addr add 192.168.{v2}.1/24 dev eth0.{v2}\n"
            "sysctl -w net.ipv4.ip_forward=1\n"
            f"ip route add 192.168.{v2}.0/24 via 192.168.{v1}.1"
        ),
    }


def generar_sistemas() -> dict:
    usuario = random.choice(["alumno", "estudiante", "user01", "dev"])
    ruta = random.choice(["/var/www/html", "/var/www", "/srv/app", "/opt/web"])
    perm = random.choice(["775", "2775", "664"])
    return {
        "id": _generar_identificador(),
        "modulo": "sistemas",
        "titulo": f"El usuario {usuario} no puede escribir en {ruta}",
        "enunciado": (
            f"El usuario '{usuario}' debe poder crear y modificar archivos en "
            f"'{ruta}' pero recibe 'Permission denied'. El directorio pertenece "
            f"a root:www-data con permisos 755.\n\n"
            "Escribe los comandos para corregir permisos y propiedad "
            f"(chmod, chown, setfacl) dejando permisos {perm}."
        ),
        "parametros": {"usuario": usuario, "ruta": ruta, "permiso": perm},
        "criterios": [
            {"tipo": "regex", "patron": r"chmod\s+" + perm.replace("2", "") + r"|chmod\s+" + perm, "peso": 3},
            {"tipo": "regex", "patron": rf"chown.*{usuario}|chown.*www-data", "peso": 2},
            {"tipo": "regex", "patron": r"setfacl|usermod.*-aG", "peso": 2},
            {"tipo": "regex", "patron": ruta.replace("/", r"\/"), "peso": 1},
        ],
        "solucion_referencia": (
            f"sudo chown -R {usuario}:www-data {ruta}\n"
            f"sudo chmod -R {perm} {ruta}\n"
            f"sudo setfacl -R -m u:{usuario}:rwx {ruta}"
        ),
    }


def generar_bd() -> dict:
    tabla = random.choice(["pedidos", "clientes", "facturas", "logs"])
    columna = random.choice(["fecha", "email", "estado", "codigo"])
    segundos = random.choice([8, 10, 12, 15])
    return {
        "id": _generar_identificador(),
        "modulo": "bd",
        "titulo": f"La consulta sobre '{tabla}' tarda {segundos} segundos",
        "enunciado": (
            f"La consulta `SELECT * FROM {tabla} WHERE {columna} = ?` tarda "
            f"{segundos} segundos con 2 millones de filas. El DBA sospecha "
            "falta de índice.\n\n"
            "Escribe los comandos SQL para analizar (EXPLAIN) y optimizar "
            "(CREATE INDEX) la consulta."
        ),
        "parametros": {"tabla": tabla, "columna": columna, "segundos": segundos},
        "criterios": [
            {"tipo": "regex", "patron": r"EXPLAIN", "peso": 3, "flags": "i"},
            {"tipo": "regex", "patron": rf"CREATE\s+INDEX.*{tabla}|INDEX.*{columna}", "peso": 4, "flags": "i"},
            {"tipo": "regex", "patron": tabla, "peso": 1, "flags": "i"},
        ],
        "solucion_referencia": (
            f"EXPLAIN SELECT * FROM {tabla} WHERE {columna} = 'valor';\n"
            f"CREATE INDEX idx_{tabla}_{columna} ON {tabla}({columna});"
        ),
    }


def generar_docker() -> dict:
    contenedor = random.choice(["api-web", "backend", "nginx-app", "worker"])
    puerto = random.randint(3000, 9000)
    return {
        "id": _generar_identificador(),
        "modulo": "docker",
        "titulo": f"El contenedor '{contenedor}' no arranca",
        "enunciado": (
            f"El contenedor '{contenedor}' (puerto {puerto}) sale con estado "
            "Exited (1). docker-compose.yml está en /home/alumno/practica.\n\n"
            "Indica la secuencia de comandos para diagnosticar logs y "
            "reconstruir/levantar el servicio."
        ),
        "parametros": {"contenedor": contenedor, "puerto": puerto},
        "criterios": [
            {"tipo": "regex", "patron": r"docker\s+logs", "peso": 3, "flags": "i"},
            {"tipo": "regex", "patron": r"docker(-compose)?\s+(ps|inspect)", "peso": 2, "flags": "i"},
            {"tipo": "regex", "patron": r"docker-compose\s+up.*--build|docker\s+compose\s+up.*--build", "peso": 4, "flags": "i"},
        ],
        "solucion_referencia": (
            f"docker logs {contenedor}\n"
            f"docker-compose ps\n"
            "docker-compose up -d --build"
        ),
    }


def generar_poo_herencia() -> dict:
    clase_base = random.choice(["Empleado", "Vehiculo", "Cuenta", "Producto"])
    clase_hija = random.choice(["Gerente", "Coche", "CuentaAhorro", "ProductoDigital"])
    metodo = random.choice(["calcularNomina", "arrancar", "aplicarInteres", "descargar"])
    return {
        "id": _generar_identificador(),
        "modulo": "poo",
        "titulo": f"Polimorfismo: {clase_hija} extiende {clase_base}",
        "enunciado": (
            f"Debes modelar en Java una jerarquía donde `{clase_hija}` hereda de "
            f"`{clase_base}` y sobrescribe `{metodo}()`. Un listado "
            f"`List<{clase_base}>` debe ejecutar la versión correcta de cada objeto.\n\n"
            "Escribe el esqueleto de clases (extends, @Override, método en la superclase) "
            "y un ejemplo de bucle polimórfico."
        ),
        "parametros": {"base": clase_base, "hija": clase_hija, "metodo": metodo},
        "criterios": [
            {"tipo": "regex", "patron": r"extends\s+" + clase_base, "peso": 3},
            {"tipo": "regex", "patron": r"@Override", "peso": 2},
            {"tipo": "regex", "patron": metodo, "peso": 2},
            {"tipo": "regex", "patron": r"List<" + clase_base + r">|for\s*\(", "peso": 2},
        ],
        "solucion_referencia": (
            f"public class {clase_base} {{\n"
            f"  public void {metodo}() {{ /* implementación base */ }}\n"
            f"}}\n"
            f"public class {clase_hija} extends {clase_base} {{\n"
            f"  @Override public void {metodo}() {{ /* especialización */ }}\n"
            f"}}\n"
            f"List<{clase_base}> elementos = List.of(new {clase_hija}());\n"
            f"for ({clase_base} e : elementos) e.{metodo}();"
        ),
    }


def generar_poo_interfaz() -> dict:
    interfaz = random.choice(["Exportable", "Persistible", "Validable", "Imprimible"])
    clase = random.choice(["Factura", "Usuario", "Pedido", "Informe"])
    return {
        "id": _generar_identificador(),
        "modulo": "poo",
        "titulo": f"La clase {clase} debe cumplir el contrato {interfaz}",
        "enunciado": (
            f"`{clase}` debe poder exportarse a CSV mediante la interfaz `{interfaz}` "
            f"con el método `exportar()`. Otras clases del proyecto no implementan esa interfaz.\n\n"
            "Indica la declaración `implements`, la firma del método y un ejemplo de uso "
            "con referencia de tipo interfaz."
        ),
        "parametros": {"interfaz": interfaz, "clase": clase},
        "criterios": [
            {"tipo": "regex", "patron": rf"implements\s+{interfaz}", "peso": 3},
            {"tipo": "regex", "patron": r"exportar\s*\(", "peso": 2},
            {"tipo": "regex", "patron": rf"{interfaz}\s+\w+\s*=", "peso": 2},
            {"tipo": "regex", "patron": r"interface\s+" + interfaz, "peso": 2},
        ],
        "solucion_referencia": (
            f"public interface {interfaz} {{\n"
            f"  String exportar();\n"
            f"}}\n"
            f"public class {clase} implements {interfaz} {{\n"
            f"  @Override public String exportar() {{ return \"...\"; }}\n"
            f"}}\n"
            f"{interfaz} doc = new {clase}();\n"
            f"doc.exportar();"
        ),
    }


def generar_poo_excepciones() -> dict:
    excepcion = random.choice(["IOException", "SQLException", "NumberFormatException", "InputMismatchException"])
    recurso = random.choice(["BufferedReader", "Connection", "Scanner", "FileInputStream"])
    return {
        "id": _generar_identificador(),
        "modulo": "poo",
        "titulo": f"Manejo de {excepcion} al usar {recurso}",
        "enunciado": (
            f"Un método que utiliza `{recurso}` puede lanzar `{excepcion}`. "
            "Debe liberar recursos y propagar un mensaje claro al llamador.\n\n"
            "Escribe un método con try-catch-finally (o try-with-resources) y "
            "`throws` donde corresponda según buenas prácticas Java."
        ),
        "parametros": {"excepcion": excepcion, "recurso": recurso},
        "criterios": [
            {"tipo": "regex", "patron": r"try\s*\(", "peso": 2},
            {"tipo": "regex", "patron": r"catch\s*\(\s*" + excepcion, "peso": 3},
            {"tipo": "regex", "patron": r"finally|try-with-resources", "peso": 2},
            {"tipo": "regex", "patron": r"throws\s+" + excepcion, "peso": 2},
        ],
        "solucion_referencia": (
            f"public void leer() throws {excepcion} {{\n"
            f"  try ({recurso} in = ...) {{\n"
            f"    // uso del recurso\n"
            f"  }} catch ({excepcion} e) {{\n"
            f"    throw new {excepcion}(\"Error al leer: \" + e.getMessage());\n"
            f"  }}\n"
            f"}}"
        ),
    }


def generar_poo_colecciones() -> dict:
    coleccion = random.choice(["ArrayList", "HashMap", "HashSet", "TreeMap"])
    tipo_clave = random.choice(["Integer", "String", "Long"])
    tipo_valor = random.choice(["String", "Producto", "Double"])
    return {
        "id": _generar_identificador(),
        "modulo": "poo",
        "titulo": f"Uso de {coleccion} para agrupar datos",
        "enunciado": (
            f"Necesitas almacenar pares clave-valor de tipo `{tipo_clave}` / `{tipo_valor}` "
            f"sin duplicados de clave, usando `{coleccion}`.\n\n"
            "Escribe la declaración genérica, la inserción de al menos dos elementos "
            "y la iteración para listarlos."
        ),
        "parametros": {"coleccion": coleccion, "clave": tipo_clave, "valor": tipo_valor},
        "criterios": [
            {"tipo": "regex", "patron": coleccion, "peso": 2},
            {"tipo": "regex", "patron": rf"{coleccion}<", "peso": 2},
            {"tipo": "regex", "patron": r"put\(|add\(", "peso": 2},
            {"tipo": "regex", "patron": r"for\s*\(|entrySet|keySet", "peso": 2},
        ],
        "solucion_referencia": (
            f"{coleccion}<{tipo_valor}> datos = new {coleccion}<>();\n"
            f"datos.add(new {tipo_valor}());\n"
            f"for ({tipo_valor} item : datos) {{ System.out.println(item); }}"
        ),
    }


def generar_poo_encapsulamiento() -> dict:
    atributo = random.choice(["saldo", "dni", "stock", "password"])
    clase = random.choice(["CuentaBancaria", "Cliente", "Almacen", "Usuario"])
    return {
        "id": _generar_identificador(),
        "modulo": "poo",
        "titulo": f"Encapsular el atributo {atributo} en {clase}",
        "enunciado": (
            f"El atributo `{atributo}` de `{clase}` no debe ser público. "
            "El acceso externo solo puede hacerse mediante getters/setters con validación.\n\n"
            "Escribe el campo privado, los métodos de acceso y un ejemplo que rechace valores inválidos."
        ),
        "parametros": {"atributo": atributo, "clase": clase},
        "criterios": [
            {"tipo": "regex", "patron": r"private\s+\w+\s+" + atributo, "peso": 3},
            {"tipo": "regex", "patron": rf"get{atributo.capitalize()}|get[A-Z]\w*", "peso": 2},
            {"tipo": "regex", "patron": rf"set{atributo.capitalize()}|set[A-Z]\w*", "peso": 2},
            {"tipo": "regex", "patron": r"if\s*\(|throw new", "peso": 2},
        ],
        "solucion_referencia": (
            f"public class {clase} {{\n"
            f"  private double {atributo};\n"
            f"  public double get{atributo.capitalize()}() {{ return {atributo}; }}\n"
            f"  public void set{atributo.capitalize()}(double v) {{\n"
            f"    if (v < 0) throw new IllegalArgumentException();\n"
            f"    this.{atributo} = v;\n"
            f"  }}\n"
            f"}}"
        ),
    }


def generar_poo() -> dict:
    return random.choice([
        generar_poo_herencia,
        generar_poo_interfaz,
        generar_poo_excepciones,
        generar_poo_colecciones,
        generar_poo_encapsulamiento,
    ])()


def generar_bd_sql() -> dict:
    tabla_a = random.choice(["clientes", "pedidos", "alumnos", "empleados"])
    tabla_b = random.choice(["facturas", "lineas_pedido", "matriculas", "departamentos"])
    columna = random.choice(["id_cliente", "id_pedido", "id_alumno", "id_empleado"])
    return {
        "id": _generar_identificador(),
        "modulo": "bd_sql",
        "titulo": f"Listar {tabla_a} con sus {tabla_b} (JOIN)",
        "enunciado": (
            f"Hay tablas `{tabla_a}` y `{tabla_b}` relacionadas por `{columna}`. "
            f"Se necesita un listado de todos los registros de `{tabla_a}` aunque no tengan "
            f"filas en `{tabla_b}`.\n\n"
            "Escribe la consulta SQL (JOIN adecuado, alias y al menos tres columnas en el SELECT)."
        ),
        "parametros": {"tabla_a": tabla_a, "tabla_b": tabla_b, "columna": columna},
        "criterios": [
            {"tipo": "regex", "patron": r"SELECT", "peso": 2, "flags": "i"},
            {"tipo": "regex", "patron": r"LEFT\s+JOIN|LEFT\s+OUTER\s+JOIN", "peso": 4, "flags": "i"},
            {"tipo": "regex", "patron": tabla_a, "peso": 1, "flags": "i"},
            {"tipo": "regex", "patron": tabla_b, "peso": 1, "flags": "i"},
            {"tipo": "regex", "patron": columna, "peso": 2, "flags": "i"},
        ],
        "solucion_referencia": (
            f"SELECT a.*, b.*\n"
            f"FROM {tabla_a} a\n"
            f"LEFT JOIN {tabla_b} b ON a.id = b.{columna};"
        ),
    }


def generar_bd_modelo() -> dict:
    entidad = random.choice(["CLIENTE", "PEDIDO", "PRODUCTO", "EMPLEADO"])
    dependencia = random.choice(["PEDIDO", "LINEA_PEDIDO", "CATEGORIA", "CONTRATO"])
    return {
        "id": _generar_identificador(),
        "modulo": "bd_modelo",
        "titulo": f"Normalizar {entidad} y {dependencia} hasta 3FN",
        "enunciado": (
            f"En un modelo inicial, `{entidad}` incluye datos de `{dependencia}` repetidos "
            "(dependencia transitiva). Debes separar tablas para cumplir 3FN.\n\n"
            "Indica las tablas resultantes, claves primarias y foráneas (notación SQL o lista estructurada)."
        ),
        "parametros": {"entidad": entidad, "dependencia": dependencia},
        "criterios": [
            {"tipo": "regex", "patron": r"PRIMARY\s+KEY|PK", "peso": 2, "flags": "i"},
            {"tipo": "regex", "patron": r"FOREIGN\s+KEY|REFERENCES", "peso": 3, "flags": "i"},
            {"tipo": "regex", "patron": entidad[:4], "peso": 1, "flags": "i"},
            {"tipo": "regex", "patron": r"3FN|tercera\s+forma|normaliz", "peso": 2, "flags": "i"},
        ],
        "solucion_referencia": (
            f"CREATE TABLE {entidad} (id INT PRIMARY KEY, ...);\n"
            f"CREATE TABLE {dependencia} (id INT PRIMARY KEY, id_{entidad.lower()} INT,\n"
            f"  FOREIGN KEY (id_{entidad.lower()}) REFERENCES {entidad}(id));"
        ),
    }


def generar_bd_transacciones() -> dict:
    cuenta_origen = random.randint(1000, 9999)
    cuenta_destino = random.randint(1000, 9999)
    importe = random.choice([50, 120, 300, 750])
    return {
        "id": _generar_identificador(),
        "modulo": "bd_transacciones",
        "titulo": f"Transferencia {importe}€ entre cuentas {cuenta_origen} y {cuenta_destino}",
        "enunciado": (
            f"Debes transferir {importe}€ de la cuenta {cuenta_origen} a la {cuenta_destino}. "
            "Si el saldo es insuficiente, no debe quedar ningún cambio parcial.\n\n"
            "Escribe el bloque SQL con BEGIN/COMMIT/ROLLBACK (o START TRANSACTION) "
            "y las dos actualizaciones de saldo."
        ),
        "parametros": {"cuenta_origen": cuenta_origen, "destino": cuenta_destino, "importe": importe},
        "criterios": [
            {"tipo": "regex", "patron": r"START\s+TRANSACTION|BEGIN", "peso": 2, "flags": "i"},
            {"tipo": "regex", "patron": r"UPDATE.*saldo|UPDATE", "peso": 3, "flags": "i"},
            {"tipo": "regex", "patron": r"COMMIT", "peso": 2, "flags": "i"},
            {"tipo": "regex", "patron": r"ROLLBACK", "peso": 2, "flags": "i"},
            {"tipo": "regex", "patron": str(importe), "peso": 1},
        ],
        "solucion_referencia": (
            "START TRANSACTION;\n"
            f"UPDATE cuentas SET saldo = saldo - {importe} WHERE id = {cuenta_origen};\n"
            f"UPDATE cuentas SET saldo = saldo + {importe} WHERE id = {cuenta_destino};\n"
            "-- si saldo < 0 entonces ROLLBACK; si no COMMIT;\n"
            "COMMIT;"
        ),
    }


def generar_bd_jdbc() -> dict:
    tabla = random.choice(["usuarios", "productos", "reservas", "empleados"])
    campo = random.choice(["email", "nombre", "fecha", "activo"])
    return {
        "id": _generar_identificador(),
        "modulo": "bd_jdbc",
        "titulo": f"Consulta parametrizada JDBC sobre {tabla}",
        "enunciado": (
            f"Debes buscar en `{tabla}` por `{campo}` usando JDBC sin concatenar SQL "
            "(riesgo de inyección). La conexión ya existe como `Connection conn`.\n\n"
            "Escribe el código Java: PreparedStatement, setString/setInt, executeQuery y try-with-resources."
        ),
        "parametros": {"tabla": tabla, "campo": campo},
        "criterios": [
            {"tipo": "regex", "patron": r"PreparedStatement", "peso": 3},
            {"tipo": "regex", "patron": r"prepareStatement", "peso": 2},
            {"tipo": "regex", "patron": r"setString|setInt|setObject", "peso": 2},
            {"tipo": "regex", "patron": r"executeQuery|executeUpdate", "peso": 2},
            {"tipo": "regex", "patron": r"\?\s*;|WHERE.*\?", "peso": 2},
        ],
        "solucion_referencia": (
            f'String sql = "SELECT * FROM {tabla} WHERE {campo} = ?";\n'
            f"try (PreparedStatement ps = conn.prepareStatement(sql)) {{\n"
            f"  ps.setString(1, valor);\n"
            f"  ResultSet rs = ps.executeQuery();\n"
            f"}}"
        ),
    }


def generar_git() -> dict:
    archivo = random.choice(["Main.java", "App.java", "config.yml", "index.html"])
    rama = random.choice(["feature/login", "feature/api", "hotfix/permisos"])
    return {
        "id": _generar_identificador(),
        "modulo": "git",
        "titulo": f"Conflicto de merge en {archivo}",
        "enunciado": (
            f"Tras `git merge {rama}` hay conflictos en '{archivo}'. "
            "El merge debe completarse o abortarse de forma controlada.\n\n"
            "Escribe los comandos git para revisar el estado, resolver o "
            "abortar el merge y dejar el repositorio limpio."
        ),
        "parametros": {"archivo": archivo, "rama": rama},
        "criterios": [
            {"tipo": "regex", "patron": r"git\s+status", "peso": 2, "flags": "i"},
            {"tipo": "regex", "patron": r"git\s+merge\s+--abort|git\s+add|git\s+commit", "peso": 3, "flags": "i"},
            {"tipo": "regex", "patron": archivo.replace(".", r"\."), "peso": 1, "flags": "i"},
        ],
        "solucion_referencia": (
            "git status\n"
            "# Opción A: abortar\n"
            "git merge --abort\n"
            "# Opción B: resolver\n"
            f"git add {archivo}\n"
            'git commit -m "Resuelve conflicto en merge"'
        ),
    }


GENERADORES = {
    "redes": generar_redes,
    "sistemas": generar_sistemas,
    "bd": generar_bd,
    "docker": generar_docker,
    "git": generar_git,
    "poo": generar_poo,
    "bd_sql": generar_bd_sql,
    "bd_modelo": generar_bd_modelo,
    "bd_transacciones": generar_bd_transacciones,
    "bd_jdbc": generar_bd_jdbc,
}

PISTAS_MODULO = {
    "redes": "Empieza comprobando interfaces VLAN y el reenvío IP.",
    "sistemas": "Revisa propietario, grupo y permisos del directorio.",
    "bd": "Usa EXPLAIN antes de crear un índice.",
    "docker": "Mira los logs del contenedor antes de reconstruir.",
    "git": "git status es siempre el primer paso.",
    "poo": "Piensa en la relación entre superclase, subclase o interfaz.",
    "bd_sql": "Identifica las tablas y el tipo de JOIN necesario.",
    "bd_modelo": "Separa entidades y elimina dependencias transitivas.",
    "bd_transacciones": "Agrupa los UPDATE en una transacción atómica.",
    "bd_jdbc": "Nunca concatenes SQL: usa ? y PreparedStatement.",
}

import generador_docs  # noqa: E402
import banco_loader  # noqa: E402

generador_docs.registrar_modulos_documentacion(GENERADORES, PISTAS_MODULO)
banco_loader.registrar_en_generadores(GENERADORES, PISTAS_MODULO)
MODULOS = tuple(sorted(GENERADORES.keys()))


def _enriquecer_criterios(escenario: dict) -> None:
    from retroalimentacion_criterios import aplicar_retroalimentacion_a_criterio

    for criterio in escenario.get("criterios", []):
        aplicar_retroalimentacion_a_criterio(criterio)


_PREFIJO_NIVEL = re.compile(
    r"^\[(?:Nivel principiante[^\]]*|Nivel intermedio|Nivel avanzado[^\]]*)\]\s*\n*",
    re.IGNORECASE,
)
_PISTA_GENERAL_NIVEL = re.compile(r"\n*💡 Pista general:.*\Z", re.DOTALL)


def _quitar_decoracion_nivel(enunciado: str) -> str:
    """Quita prefijos de dificultad y pistas ya incrustados (p. ej. ejercicios del banco)."""
    texto = enunciado or ""
    while True:
        sin_prefijo = _PREFIJO_NIVEL.sub("", texto, count=1)
        if sin_prefijo == texto:
            break
        texto = sin_prefijo
    texto = _PISTA_GENERAL_NIVEL.sub("", texto)
    return texto.strip()


def _aplicar_nivel(escenario: dict, nivel: int) -> dict:
    nivel = max(1, min(3, nivel))
    escenario["dificultad"] = nivel
    _enriquecer_criterios(escenario)
    criterios = escenario.get("criterios", [])
    modulo = escenario.get("modulo", "")
    pista_general = PISTAS_MODULO.get(modulo, "Lee el enunciado e incluye los pasos básicos.")
    enunciado_base = _quitar_decoracion_nivel(str(escenario.get("enunciado", "")))

    if nivel == 1:
        escenario["enunciado"] = (
            "[Nivel principiante — menos criterios, más ayuda]\n\n"
            + enunciado_base
            + f"\n\n💡 Pista general: {pista_general}"
        )
        if len(criterios) > 2:
            escenario["criterios"] = sorted(criterios, key=lambda c: c.get("peso", 1))[:2]
    elif nivel == 3:
        escenario["enunciado"] = (
            "[Nivel avanzado — sin pistas extra]\n\n" + enunciado_base
        )
    else:
        escenario["enunciado"] = "[Nivel intermedio]\n\n" + enunciado_base

    return escenario


def generar(
    modulo: str | None = None,
    nivel: int = 2,
    capitulo: str | None = None,
    seccion: str | None = None,
) -> dict:
    if modulo and modulo not in GENERADORES:
        raise ValueError(f"Módulo desconocido: {modulo}. Válidos: {', '.join(MODULOS)}")
    if modulo and modulo.startswith("docs_"):
        escenario = generador_docs.generar_ejercicio_documentacion(
            modulo, nivel, capitulo=capitulo, seccion=seccion
        )
    elif modulo:
        escenario = GENERADORES[modulo]()
    else:
        escenario = random.choice(list(GENERADORES.values()))()
    escenario = _aplicar_nivel(escenario, nivel)
    escenario["generado_en"] = datetime.now(timezone.utc).isoformat()
    return escenario


def principal() -> int:
    analizador = argparse.ArgumentParser(description="Generador de escenarios — Forja de ejercicios")
    analizador.add_argument("--modulo", "-m", choices=MODULOS, help="Módulo específico")
    analizador.add_argument("--capitulo", "-c", help="Capítulo (módulos docs_* indexados)")
    analizador.add_argument("--seccion", help="Sección dentro del capítulo (docs_*)")
    analizador.add_argument("--nivel", "-n", type=int, default=2, choices=[1, 2, 3],
                            help="Dificultad 1=fácil, 2=medio, 3=avanzado")
    analizador.add_argument("--salida", "-s", help="Archivo JSON de salida")
    analizador.add_argument("--formateado", action="store_true", help="JSON indentado")
    argumentos = analizador.parse_args()

    escenario = generar(
        argumentos.modulo,
        argumentos.nivel,
        capitulo=argumentos.capitulo,
        seccion=argumentos.seccion,
    )
    sangria = 2 if argumentos.formateado else None
    texto = json.dumps(escenario, ensure_ascii=False, indent=sangria)

    if argumentos.salida:
        with open(argumentos.salida, "w", encoding="utf-8") as archivo:
            archivo.write(texto)
    else:
        print(texto)
    return 0


if __name__ == "__main__":
    sys.exit(principal())
