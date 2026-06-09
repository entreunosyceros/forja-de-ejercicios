#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Textos legibles para criterios de corrección (evita mostrar regex al alumno)."""

from __future__ import annotations

import re
from typing import Any

_PALABRAS_SQL = frozenset({
    "SELECT", "FROM", "WHERE", "JOIN", "INDEX", "CREATE", "EXPLAIN",
    "UPDATE", "INSERT", "DELETE", "PRIMARY", "FOREIGN", "KEY", "REFERENCES",
    "COMMIT", "ROLLBACK", "TRANSACTION", "BEGIN", "OUTER", "LEFT", "INNER",
})

_PALABRAS_DOCKER = frozenset({"DOCKER", "COMPOSE", "CONTAINER", "LOGS", "INSPECT"})

_PALABRAS_GIT = frozenset({"GIT", "STATUS", "MERGE", "COMMIT", "ADD", "ABORT"})


def parece_patron_regex(texto: str) -> bool:
    """True si el texto parece una expresión regular, no una frase para el alumno."""
    if not texto or not texto.strip():
        return False
    t = texto.strip()
    if re.search(r"\\[sdwSDWnrt.]|\\s\+|\.\*|\(\?:|\[\^?", t):
        return True
    if "|" in t and re.search(r"[\\.*+?\[\]]", t):
        return True
    return bool(re.search(r"\(\?i\)|\(\?=", t))


def _tokens_legibles(patron: str) -> list[str]:
    """Extrae palabras concretas (tablas, columnas, comandos) de un patrón regex."""
    simpl = patron
    simpl = re.sub(r"\\s\+?", " ", simpl)
    simpl = re.sub(r"\\.", "", simpl)
    simpl = re.sub(r"\.\*", " ", simpl)
    simpl = re.sub(r"[|()?[\]{}^$+*]", " ", simpl)
    vistos: set[str] = set()
    tokens: list[str] = []
    for coincidencia in re.finditer(r"[a-zA-Z_][a-zA-Z0-9_-]*", simpl):
        palabra = coincidencia.group(0)
        clave = palabra.lower()
        if clave in vistos or len(palabra) < 2:
            continue
        if palabra.isupper() and palabra in _PALABRAS_SQL | _PALABRAS_DOCKER | _PALABRAS_GIT:
            continue
        vistos.add(clave)
        tokens.append(palabra)
    return tokens


def _simplificar_patron(patron: str) -> str:
    texto = patron
    texto = re.sub(r"\\s\+?", " ", texto)
    texto = re.sub(r"\\.", "", texto)
    texto = re.sub(r"\.\*", "…", texto)
    texto = re.sub(r"\|", " o ", texto)
    texto = re.sub(r"[()?\[\]{}^$+*]", "", texto)
    return re.sub(r"\s+", " ", texto).strip()[:80]


def _esperado_pista_regex(patron: str) -> tuple[str, str, str]:
    """Devuelve (descripcion, esperado, pista) para un patrón regex."""
    p = patron
    p_low = p.lower()
    tokens = _tokens_legibles(p)
    nombres = ", ".join(f"«{t}»" for t in tokens[:4])

    if re.search(r"\bexplain\b", p, re.I):
        return (
            "Analizar la consulta con EXPLAIN",
            "Incluir el comando EXPLAIN sobre la consulta del enunciado.",
            "Antes de crear un índice, usa EXPLAIN para ver si la consulta hace un "
            "recorrido completo de la tabla (full scan) o usa un índice.",
        )

    if ("CREATE" in p.upper() and "INDEX" in p.upper()) or re.search(
        r"create\\s\+index", p, re.I
    ):
        ref = f" sobre {nombres}" if nombres else ""
        return (
            "Crear un índice (CREATE INDEX)",
            f"Incluir un comando CREATE INDEX{ref}.",
            "La consulta del enunciado es lenta porque falta un índice en la columna "
            "del WHERE. Escribe CREATE INDEX nombre ON tabla(columna); "
            + (f"Usa la tabla o columna del enunciado ({nombres})." if nombres else ""),
        )

    if re.search(r"docker\s*\\s*\+?\s*logs|docker\s+logs", p, re.I):
        return (
            "Consultar logs del contenedor",
            "Incluir docker logs (o equivalente) para ver el error.",
            "Los logs del contenedor muestran por qué falla el servicio antes de "
            "reconstruir la imagen.",
        )

    if re.search(r"docker(-compose)?\s*\\s*\+?\s*(ps|inspect)", p, re.I) or "inspect" in p_low:
        return (
            "Inspeccionar contenedores",
            "Incluir docker ps o docker inspect para revisar el estado.",
            "Comprueba que el contenedor está en ejecución y revisa su configuración "
            "antes de cambiar el compose.",
        )

    if re.search(r"docker-compose\s*\\s*\+?\s*up|compose\s+up", p, re.I):
        return (
            "Reconstruir y levantar servicios",
            "Incluir docker compose up con --build (o docker-compose up --build).",
            "Tras corregir el Dockerfile o el compose, vuelve a construir la imagen "
            "con --build para aplicar los cambios.",
        )

    if re.search(r"git\s*\\s*\+?\s*status", p, re.I):
        return (
            "Comprobar el estado del repositorio",
            "Incluir git status.",
            "Siempre empieza viendo qué archivos están en conflicto o sin commitear.",
        )

    if re.search(r"git\s*\\s*\+?\s*merge", p, re.I):
        return (
            "Resolver el merge o abortarlo",
            "Incluir git merge --abort, git add o git commit según el caso.",
            "Tras un conflicto, o abortas el merge o resuelves los archivos y haces commit.",
        )

    if re.search(r"vlan|ip\s*\\s*\+?\s*link", p, re.I):
        return (
            "Revisar interfaces y VLANs",
            "Mencionar VLAN o ip link para comprobar interfaces.",
            "Comprueba que las VLAN existen y las interfaces están activas antes "
            "de configurar rutas.",
        )

    if re.search(r"ip\s*\\s*\+?\s*route|ip_forward|routing", p, re.I):
        return (
            "Habilitar enrutamiento entre subredes",
            "Incluir ip route, ip routing o sysctl ip_forward.",
            "Para que dos VLANs se comuniquen hace falta reenvío IP y rutas entre subredes.",
        )

    if re.search(r"chmod", p, re.I):
        return (
            "Ajustar permisos (chmod)",
            "Incluir chmod con los permisos pedidos en el enunciado.",
            "Los permisos de lectura/escritura/ejecución deben coincidir con lo que pide el caso.",
        )

    if re.search(r"chown", p, re.I):
        return (
            "Cambiar propietario (chown)",
            "Incluir chown para asignar usuario o grupo al archivo o carpeta.",
            "El propietario y el grupo determinan quién puede acceder al recurso.",
        )

    if re.search(r"setfacl|usermod", p, re.I):
        return (
            "Permisos avanzados (ACL o grupos)",
            "Incluir setfacl o usermod -aG según el enunciado.",
            "A veces chmod no basta: ACL o grupos secundarios dan acceso fino.",
        )

    if re.search(r"preparedstatement|preparestatement", p, re.I):
        return (
            "Usar PreparedStatement",
            "Incluir PreparedStatement y prepareStatement en el código Java.",
            "Nunca concatenes SQL con el input del usuario: usa ? y PreparedStatement.",
        )

    if re.search(r"setstring|setint|setobject", p, re.I):
        return (
            "Enlazar parámetros con setXxx",
            "Incluir setString, setInt o setObject para los placeholders ?.",
            "Cada ? del SQL se rellena con setString/setInt antes de ejecutar.",
        )

    if re.search(r"left\s*\\s*\+?\s*join", p, re.I):
        return (
            "Unir tablas con LEFT JOIN",
            "Incluir LEFT JOIN (o LEFT OUTER JOIN) entre las tablas del enunciado.",
            "LEFT JOIN devuelve todas las filas de la tabla izquierda aunque no haya "
            "coincidencia en la derecha.",
        )

    if re.search(r"\bselect\b", p, re.I):
        return (
            "Consulta SELECT",
            "Incluir una sentencia SELECT que use las tablas del enunciado.",
            "La consulta debe recuperar los datos pedidos con las tablas y columnas correctas.",
        )

    if re.search(r"primary\s*\\s*\+?\s*key|foreign\s*\\s*\+?\s*key|references", p, re.I):
        return (
            "Claves en el modelo relacional",
            "Incluir PRIMARY KEY y/o FOREIGN KEY según el enunciado.",
            "Separa entidades en tablas y enlázalas con claves foráneas para evitar "
            "datos repetidos.",
        )

    if re.search(r"start\s*\\s*\+?\s*transaction|begin|commit|rollback", p, re.I):
        return (
            "Transacción SQL",
            "Incluir BEGIN/START TRANSACTION y COMMIT o ROLLBACK.",
            "Agrupa los cambios en una transacción para que todo se aplique o nada.",
        )

    if re.search(r"@override|extends|implements", p, re.I):
        return (
            "Herencia o polimorfismo en Java",
            "Incluir extends, implements o @Override según el enunciado.",
            "La subclase debe reutilizar o redefinir el comportamiento de la superclase.",
        )

    if tokens:
        return (
            f"Incluir: {', '.join(tokens[:3])}",
            f"Tu respuesta debe mencionar o usar {nombres}.",
            f"Revisa el enunciado: se esperaba que apareciera algo relacionado con "
            f"{nombres}. Comprueba que no omitiste ese paso o concepto.",
        )

    simpl = _simplificar_patron(patron)
    return (
        simpl or "Criterio técnico",
        f"Tu respuesta debe incluir elementos como: {simpl}." if simpl else "Completar el enunciado.",
        "Vuelve a leer el enunciado y la solución de referencia: falta un paso o comando clave.",
    )


def _esperado_pista_contiene_todos(terminos: list[str]) -> tuple[str, str, str]:
    lista = ", ".join(f"«{t}»" for t in terminos)
    return (
        f"Incluir: {lista}",
        f"Debe aparecer en tu respuesta: {lista}.",
        f"El ejercicio pide que uses o menciones {lista}. "
        "Revisa el enunciado y comprueba que no falte ninguno de esos términos.",
    )


def _esperado_pista_contiene_alguno(terminos: list[str]) -> tuple[str, str, str]:
    lista = ", ".join(f"«{t}»" for t in terminos)
    return (
        f"Uno de: {lista}",
        f"Debe incluir al menos uno de: {lista}.",
        f"Válido cualquiera de estas formas: {lista}. "
        "El concepto del enunciado debe quedar reflejado con alguna de ellas.",
    )


def descripcion_desde_criterio(criterio: dict[str, Any]) -> str:
    tipo = criterio.get("tipo", "regex")
    if criterio.get("descripcion") and not parece_patron_regex(criterio["descripcion"]):
        return str(criterio["descripcion"]).strip()
    if tipo == "contiene_todos":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_todos(terminos)[0] if terminos else "Incluir términos clave"
    if tipo == "contiene_alguno":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_alguno(terminos)[0] if terminos else "Incluir un término válido"
    desc, _, _ = _esperado_pista_regex(criterio.get("patron", ""))
    return desc


def esperado_desde_criterio(criterio: dict[str, Any]) -> str:
    tipo = criterio.get("tipo", "regex")
    if tipo == "contiene_todos":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_todos(terminos)[1] if terminos else "Criterio mal configurado"
    if tipo == "contiene_alguno":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_alguno(terminos)[1] if terminos else "Criterio mal configurado"
    _, esperado, _ = _esperado_pista_regex(criterio.get("patron", ""))
    return esperado


def pista_desde_criterio(criterio: dict[str, Any]) -> str:
    tipo = criterio.get("tipo", "regex")
    if tipo == "contiene_todos":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_todos(terminos)[2] if terminos else "Revisa el enunciado."
    if tipo == "contiene_alguno":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_alguno(terminos)[2] if terminos else "Revisa el enunciado."
    _, _, pista = _esperado_pista_regex(criterio.get("patron", ""))
    return pista


def _texto_necesita_mejora(texto: str | None) -> bool:
    if not texto or not str(texto).strip():
        return True
    t = str(texto).strip()
    if parece_patron_regex(t):
        return True
    if t.startswith("Incluir en la respuesta:"):
        resto = t.split(":", 1)[-1].strip()
        return parece_patron_regex(resto) or "\\" in resto
    return t.startswith("Tu solución debe mencionar o usar:") and len(t) < 40


def aplicar_retroalimentacion_a_criterio(criterio: dict[str, Any]) -> None:
    """Rellena descripcion, esperado y pista legibles en un criterio del escenario."""
    criterio["descripcion"] = descripcion_desde_criterio(criterio)
    if _texto_necesita_mejora(criterio.get("esperado")):
        criterio["esperado"] = esperado_desde_criterio(criterio)
    if _texto_necesita_mejora(criterio.get("pista")) or criterio.get("pista") == criterio.get("esperado"):
        criterio["pista"] = pista_desde_criterio(criterio)


def enriquecer_detalle(criterio: dict[str, Any], detalle: dict[str, Any]) -> dict[str, Any]:
    """Ajusta esperado/pista/descripcion en el detalle devuelto al alumno."""
    detalle["descripcion"] = descripcion_desde_criterio(criterio)
    if _texto_necesita_mejora(detalle.get("esperado")):
        detalle["esperado"] = esperado_desde_criterio(criterio)
    if detalle.get("cumplido"):
        detalle.pop("pista", None)
    elif _texto_necesita_mejora(detalle.get("pista")) or detalle.get("pista") == detalle.get("esperado"):
        detalle["pista"] = pista_desde_criterio(criterio)
    return detalle
