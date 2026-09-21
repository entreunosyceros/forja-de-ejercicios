#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Textos legibles para criterios de corrección (evita mostrar regex al alumno)."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

RAIZ = Path(__file__).resolve().parent
RUTA_REGLAS_REGEX = RAIZ / "reglas_retroalimentacion_regex.json"

_PALABRAS_SQL = frozenset({
    "SELECT", "FROM", "WHERE", "JOIN", "INDEX", "CREATE", "EXPLAIN",
    "UPDATE", "INSERT", "DELETE", "PRIMARY", "FOREIGN", "KEY", "REFERENCES",
    "COMMIT", "ROLLBACK", "TRANSACTION", "BEGIN", "OUTER", "LEFT", "INNER",
})

_PALABRAS_DOCKER = frozenset({"DOCKER", "COMPOSE", "CONTAINER", "LOGS", "INSPECT"})

_PALABRAS_GIT = frozenset({"GIT", "STATUS", "MERGE", "COMMIT", "ADD", "ABORT"})

_CACHE_REGLAS: list[dict[str, Any]] | None = None


def _flags_re(flags: str | None) -> int:
    f = 0
    for c in (flags or ""):
        if c in "iI":
            f |= re.IGNORECASE
        elif c in "mM":
            f |= re.MULTILINE
        elif c in "sS":
            f |= re.DOTALL
    return f


def _cargar_reglas_regex() -> list[dict[str, Any]]:
    """Carga la tabla de reglas JSON (caché en proceso)."""
    global _CACHE_REGLAS
    if _CACHE_REGLAS is not None:
        return _CACHE_REGLAS
    if not RUTA_REGLAS_REGEX.is_file():
        _CACHE_REGLAS = []
        return _CACHE_REGLAS
    try:
        datos = json.loads(RUTA_REGLAS_REGEX.read_text(encoding="utf-8"))
        reglas = datos.get("reglas") if isinstance(datos, dict) else None
        _CACHE_REGLAS = list(reglas) if isinstance(reglas, list) else []
    except (json.JSONDecodeError, OSError):
        _CACHE_REGLAS = []
    return _CACHE_REGLAS


def _regla_coincide(regla: dict[str, Any], patron: str) -> bool:
    """True si el patrón del criterio dispara esta regla."""
    flags = _flags_re(str(regla.get("flags") or "i"))
    expr = regla.get("patron") or ""
    if expr and re.search(expr, patron, flags):
        return True
    contiene = regla.get("contiene_todas") or []
    if contiene and all(str(t).upper() in patron.upper() for t in contiene):
        return True
    subcadenas = regla.get("contiene_subcadena") or []
    p_low = patron.lower()
    if subcadenas and any(str(s).lower() in p_low for s in subcadenas):
        return True
    return False


def _aplicar_placeholders(texto: str, nombres: str) -> str:
    ref = f" sobre {nombres}" if nombres else ""
    nombres_ayuda = (
        f" Usa la tabla o columna del enunciado ({nombres})." if nombres else ""
    )
    return (
        texto.replace("{nombres}", nombres)
        .replace("{ref}", ref)
        .replace("{nombres_ayuda}", nombres_ayuda)
    )


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
    """Devuelve (descripcion, esperado, pista) según la tabla JSON de reglas."""
    tokens = _tokens_legibles(patron)
    nombres = ", ".join(f"«{t}»" for t in tokens[:4])

    for regla in _cargar_reglas_regex():
        if not isinstance(regla, dict):
            continue
        if not _regla_coincide(regla, patron):
            continue
        desc = _aplicar_placeholders(str(regla.get("descripcion") or ""), nombres)
        esperado = _aplicar_placeholders(str(regla.get("esperado") or ""), nombres)
        pista = _aplicar_placeholders(str(regla.get("pista") or ""), nombres)
        return (desc, esperado, pista)

    if tokens:
        return (
            "Completar el apartado del enunciado",
            "Tu respuesta debe cubrir lo que pide este apartado del enunciado.",
            "Revisa el enunciado: falta un paso o concepto clave. "
            "No copies patrones técnicos; escribe la solución completa.",
        )

    simpl = _simplificar_patron(patron)
    return (
        simpl or "Criterio técnico",
        "Tu respuesta debe cubrir lo que pide este apartado del enunciado.",
        "Vuelve a leer el enunciado: falta un paso o comando clave.",
    )


def _esperado_pista_contiene_todos(terminos: list[str]) -> tuple[str, str, str]:
    n = len(terminos)
    return (
        f"Cubrir {n} conceptos del enunciado",
        f"Debes cubrir {n} conceptos de este apartado (puntúa de forma proporcional).",
        "Revisa el enunciado y completa los conceptos que faltan en tu respuesta.",
    )


def _esperado_pista_contiene_alguno(terminos: list[str]) -> tuple[str, str, str]:
    return (
        "Incluir al menos una forma válida",
        "Debes incluir al menos una de las formas válidas del concepto pedido.",
        "Falta el concepto principal de este apartado. Revisa el enunciado.",
    )


def _esperado_pista_no_contiene(terminos: list[str]) -> tuple[str, str, str]:
    return (
        "Evitar elementos no deseados",
        "Tu respuesta no debe incluir prácticas o elementos prohibidos en el enunciado.",
        "Has incluido algo que el enunciado pide evitar. Revisa y corrige esa parte.",
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
    if tipo == "no_contiene":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_no_contiene(terminos)[0] if terminos else "Evitar elementos no deseados"
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
    if tipo == "no_contiene":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_no_contiene(terminos)[1] if terminos else "Criterio mal configurado"
    _, esperado, _ = _esperado_pista_regex(criterio.get("patron", ""))
    return esperado


def pista_desde_criterio(criterio: dict[str, Any]) -> str:
    # Preferir siempre la pista escrita en el criterio (profesor / Gemini).
    if criterio.get("pista") and not parece_patron_regex(str(criterio["pista"])):
        return str(criterio["pista"]).strip()
    tipo = criterio.get("tipo", "regex")
    if tipo == "contiene_todos":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_todos(terminos)[2] if terminos else "Revisa el enunciado."
    if tipo == "contiene_alguno":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_contiene_alguno(terminos)[2] if terminos else "Revisa el enunciado."
    if tipo == "no_contiene":
        terminos = criterio.get("terminos") or []
        return _esperado_pista_no_contiene(terminos)[2] if terminos else "Revisa el enunciado."
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
