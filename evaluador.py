#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Evaluador automático de respuestas para La máquina de ejercicios."""

import argparse
import json
import operator
import re
import sys
from functools import reduce
from pathlib import Path

from alias_comandos import contiene_termino_flexible, variantes_termino
from retroalimentacion_criterios import enriquecer_detalle

# Letras sueltas en "banderas"/"flags" (p. ej. "im"). "multiline" NO activa IGNORECASE.
_BANDERAS = {
    "i": re.IGNORECASE,
    "m": re.MULTILINE,
    "s": re.DOTALL,
}

# Si un criterio obligatorio falla, la nota no puede superar este tope.
TOPE_NOTA_OBLIGATORIO_FALLIDO = 4.0


def banderas_regex(criterio: dict) -> int:
    """Interpreta banderas de forma explícita (no basta con que el texto contenga una «i»)."""
    texto = str(criterio.get("banderas") or criterio.get("flags") or "").lower().strip()
    if not texto:
        return 0
    if re.fullmatch(r"[ims]+", texto):
        return reduce(operator.or_, (_BANDERAS[c] for c in set(texto)), 0)
    tokens = set(re.findall(r"[a-z]+", texto))
    valor = 0
    if tokens & {"i", "ignorecase"}:
        valor |= re.IGNORECASE
    if tokens & {"m", "multiline"}:
        valor |= re.MULTILINE
    if tokens & {"s", "dotall"}:
        valor |= re.DOTALL
    return valor


def peso_criterio(criterio: dict) -> int:
    """Peso entero ≥ 1; valores inválidos o negativos se tratan como 1."""
    bruto = criterio.get("peso", 1)
    try:
        peso = int(bruto)
    except (TypeError, ValueError):
        return 1
    return max(1, peso)


def es_obligatorio(criterio: dict) -> bool:
    return bool(criterio.get("obligatorio"))


def _contiene_termino(respuesta: str, termino: str) -> bool:
    return contiene_termino_flexible(respuesta, termino)


def _detalle_base(criterio: dict, *, cumplido: bool, tipo: str, **extra) -> dict:
    peso = peso_criterio(criterio)
    detalle = {
        "cumplido": cumplido,
        "tipo": tipo,
        "peso": peso,
        "peso_parcial": float(peso if cumplido else 0),
        "obligatorio": es_obligatorio(criterio),
        **extra,
    }
    return enriquecer_detalle(criterio, detalle)


def evaluar_criterio_regex(respuesta: str, criterio: dict) -> dict:
    patron = criterio.get("patron", "")
    banderas = banderas_regex(criterio)
    peso = peso_criterio(criterio)
    try:
        cumplido = bool(re.search(patron, respuesta, banderas))
    except re.error as error:
        return {
            "cumplido": False,
            "patron": patron,
            "error": str(error),
            "peso": peso,
            "peso_parcial": 0.0,
            "tipo": "regex",
            "obligatorio": es_obligatorio(criterio),
        }
    esperado = criterio.get(
        "esperado",
        "Tu respuesta debe cubrir lo que pide el enunciado en este apartado.",
    )
    detalle = {
        "cumplido": cumplido,
        "patron": patron,
        "tipo": "regex",
        "peso": peso,
        "peso_parcial": float(peso if cumplido else 0),
        "obligatorio": es_obligatorio(criterio),
        "descripcion": criterio.get("descripcion", "Criterio del enunciado"),
        "esperado": esperado,
    }
    if not cumplido:
        detalle["pista"] = criterio.get(
            "pista",
            "Revisa el enunciado: falta un paso o concepto clave.",
        )
    return enriquecer_detalle(criterio, detalle)


def evaluar_criterio_contiene_todos(respuesta: str, criterio: dict) -> dict:
    terminos = criterio.get("terminos") or []
    peso = peso_criterio(criterio)
    if not terminos:
        return {
            "cumplido": False,
            "tipo": "contiene_todos",
            "terminos": terminos,
            "peso": peso,
            "peso_parcial": 0.0,
            "obligatorio": es_obligatorio(criterio),
            "esperado": criterio.get("esperado", "Criterio mal configurado"),
            "descripcion": criterio.get("descripcion", "Sin términos"),
            "error": "contiene_todos sin terminos",
            "pista": criterio.get("pista", "Revisa la configuración del ejercicio"),
        }
    faltan = [t for t in terminos if not _contiene_termino(respuesta, t)]
    encontrados = len(terminos) - len(faltan)
    fraccion = encontrados / len(terminos)
    cumplido = len(faltan) == 0
    peso_parcial = round(peso * fraccion, 4)
    esperado = criterio.get(
        "esperado",
        f"Debes cubrir {len(terminos)} conceptos del enunciado "
        "(se puntúa de forma proporcional si aciertas solo algunos).",
    )
    detalle = {
        "cumplido": cumplido,
        "tipo": "contiene_todos",
        "terminos": terminos,
        "peso": peso,
        "peso_parcial": peso_parcial,
        "encontrados": encontrados,
        "total_terminos": len(terminos),
        "obligatorio": es_obligatorio(criterio),
        "esperado": esperado,
        "descripcion": criterio.get("descripcion", esperado[:60]),
    }
    aliases: dict[str, list[str]] = {}
    for t in terminos:
        vars_t = variantes_termino(t)
        if len(vars_t) > 1:
            aliases[t] = vars_t
    if aliases:
        detalle["aliases"] = aliases
    if not cumplido:
        detalle["faltan"] = faltan
        n = len(faltan)
        detalle["pista"] = criterio.get(
            "pista",
            f"Te faltan {n} concepto(s) de este apartado. Revisa el enunciado y completa la respuesta.",
        )
    return enriquecer_detalle(criterio, detalle)


def evaluar_criterio_contiene_alguno(respuesta: str, criterio: dict) -> dict:
    terminos = criterio.get("terminos") or []
    peso = peso_criterio(criterio)
    if not terminos:
        return {
            "cumplido": False,
            "tipo": "contiene_alguno",
            "terminos": terminos,
            "peso": peso,
            "peso_parcial": 0.0,
            "obligatorio": es_obligatorio(criterio),
            "esperado": criterio.get("esperado", "Criterio mal configurado"),
            "descripcion": criterio.get("descripcion", "Sin términos"),
            "error": "contiene_alguno sin terminos",
            "pista": criterio.get("pista", "Revisa la configuración del ejercicio"),
        }
    cumplido = any(_contiene_termino(respuesta, t) for t in terminos)
    esperado = criterio.get(
        "esperado",
        "Debes incluir al menos una de las formas válidas del concepto pedido.",
    )
    detalle = {
        "cumplido": cumplido,
        "tipo": "contiene_alguno",
        "terminos": terminos,
        "peso": peso,
        "peso_parcial": float(peso if cumplido else 0),
        "obligatorio": es_obligatorio(criterio),
        "esperado": esperado,
        "descripcion": criterio.get("descripcion", esperado[:60]),
    }
    if not cumplido:
        detalle["pista"] = criterio.get(
            "pista",
            "Falta el concepto principal de este apartado. Revisa el enunciado.",
        )
    return enriquecer_detalle(criterio, detalle)


def evaluar_criterio_no_contiene(respuesta: str, criterio: dict) -> dict:
    terminos = criterio.get("terminos") or []
    peso = peso_criterio(criterio)
    if not terminos:
        return {
            "cumplido": False,
            "tipo": "no_contiene",
            "terminos": terminos,
            "peso": peso,
            "peso_parcial": 0.0,
            "obligatorio": es_obligatorio(criterio),
            "error": "no_contiene sin terminos",
            "esperado": criterio.get("esperado", "Criterio mal configurado"),
            "descripcion": criterio.get("descripcion", "Sin términos"),
            "pista": criterio.get("pista", "Revisa la configuración del ejercicio"),
        }
    presentes = [t for t in terminos if _contiene_termino(respuesta, t)]
    cumplido = len(presentes) == 0
    esperado = criterio.get(
        "esperado",
        "Tu respuesta no debe incluir prácticas o elementos prohibidos en el enunciado.",
    )
    detalle = {
        "cumplido": cumplido,
        "tipo": "no_contiene",
        "terminos": terminos,
        "peso": peso,
        "peso_parcial": float(peso if cumplido else 0),
        "obligatorio": es_obligatorio(criterio),
        "esperado": esperado,
        "descripcion": criterio.get(
            "descripcion",
            "Evitar elementos no deseados",
        ),
    }
    if not cumplido:
        detalle["presentes"] = presentes
        detalle["pista"] = criterio.get(
            "pista",
            "Has incluido algo que el enunciado pide evitar. Revisa y corrige esa parte.",
        )
    return enriquecer_detalle(criterio, detalle)


def evaluar_criterio(respuesta: str, criterio: dict) -> dict:
    tipo = criterio.get("tipo", "regex")
    if tipo == "contiene_todos":
        return evaluar_criterio_contiene_todos(respuesta, criterio)
    if tipo == "contiene_alguno":
        return evaluar_criterio_contiene_alguno(respuesta, criterio)
    if tipo == "no_contiene":
        return evaluar_criterio_no_contiene(respuesta, criterio)
    return evaluar_criterio_regex(respuesta, criterio)


def evaluar(escenario: dict, respuesta: str) -> dict:
    criterios = escenario.get("criterios", [])
    detalles = [evaluar_criterio(respuesta, c) for c in criterios]
    peso_total = sum(peso_criterio(c) for c in criterios) or 1
    peso_obtenido = sum(float(d.get("peso_parcial", 0)) for d in detalles)
    nota = round(10 * peso_obtenido / peso_total, 1)
    nota = min(10.0, max(0.0, nota))

    obligatorio_fallido = any(
        d.get("obligatorio") and not d.get("cumplido") for d in detalles
    )
    if obligatorio_fallido:
        nota = min(nota, TOPE_NOTA_OBLIGATORIO_FALLIDO)

    aprobado = nota >= 5.0

    if obligatorio_fallido and nota < 5:
        retroalimentacion = (
            "Insuficiente: falta un criterio obligatorio. "
            "Aunque hayas acertado otras partes, ese punto es eliminatorio."
        )
    elif nota >= 9:
        retroalimentacion = "Excelente. Has cubierto los puntos clave de la solución."
    elif nota >= 7:
        retroalimentacion = "Bien. Revisa los criterios no cumplidos para una solución completa."
    elif nota >= 5:
        retroalimentacion = "Aprobado por los pelos. Faltan comandos o pasos importantes."
    else:
        retroalimentacion = "Insuficiente. Repasa el enunciado y la solución de referencia."

    return {
        "examen_id": escenario.get("id"),
        "modulo": escenario.get("modulo"),
        "nota": nota,
        "aprobado": aprobado,
        "peso_obtenido": round(peso_obtenido, 4),
        "peso_total": peso_total,
        "obligatorio_fallido": obligatorio_fallido,
        "retroalimentacion": retroalimentacion,
        "detalles": detalles,
        "solucion_referencia": escenario.get("solucion_referencia", ""),
    }


def principal() -> int:
    analizador = argparse.ArgumentParser(description="Evaluador — Forja de ejercicios")
    analizador.add_argument("--escenario", "-e", required=True, help="JSON del escenario")
    analizador.add_argument("--respuesta", "-r", help="Texto de la respuesta del alumno")
    analizador.add_argument("--archivo-respuesta", help="Archivo con la respuesta")
    analizador.add_argument("--salida", "-s", help="Archivo JSON de resultado")
    argumentos = analizador.parse_args()

    ruta_escenario = Path(argumentos.escenario)
    with ruta_escenario.open(encoding="utf-8") as archivo:
        escenario = json.load(archivo)

    if argumentos.archivo_respuesta:
        respuesta = Path(argumentos.archivo_respuesta).read_text(encoding="utf-8")
    elif argumentos.respuesta is not None:
        respuesta = argumentos.respuesta
    else:
        respuesta = sys.stdin.read()

    resultado = evaluar(escenario, respuesta)
    texto = json.dumps(resultado, ensure_ascii=False, indent=2)

    if argumentos.salida:
        Path(argumentos.salida).write_text(texto, encoding="utf-8")
    else:
        print(texto)
    return 0


if __name__ == "__main__":
    sys.exit(principal())
