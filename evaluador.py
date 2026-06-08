#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Evaluador automático de respuestas para La máquina de ejercicios."""

import argparse
import json
import re
import sys
from pathlib import Path

from alias_comandos import contiene_termino_flexible, variantes_termino


def _normalizar(texto: str) -> str:
    return texto.lower()


def _contiene_termino(respuesta: str, termino: str) -> bool:
    return contiene_termino_flexible(respuesta, termino)


def evaluar_criterio_regex(respuesta: str, criterio: dict) -> dict:
    patron = criterio.get("patron", "")
    banderas_texto = criterio.get("banderas", criterio.get("flags", ""))
    banderas = re.IGNORECASE if str(banderas_texto).lower().find("i") >= 0 else 0
    try:
        cumplido = bool(re.search(patron, respuesta, banderas))
    except re.error as error:
        return {
            "cumplido": False,
            "patron": patron,
            "error": str(error),
            "peso": criterio.get("peso", 1),
            "tipo": "regex",
        }
    esperado = criterio.get("esperado", f"La respuesta debe incluir algo como: {patron[:50]}")
    detalle = {
        "cumplido": cumplido,
        "patron": patron,
        "tipo": "regex",
        "peso": criterio.get("peso", 1),
        "descripcion": criterio.get("descripcion", patron[:60]),
        "esperado": esperado,
    }
    if not cumplido:
        detalle["pista"] = criterio.get("pista", esperado)
    return detalle


def evaluar_criterio_contiene_todos(respuesta: str, criterio: dict) -> dict:
    terminos = criterio.get("terminos") or []
    if not terminos:
        return {
            "cumplido": False,
            "tipo": "contiene_todos",
            "terminos": terminos,
            "peso": criterio.get("peso", 1),
            "esperado": criterio.get("esperado", "Criterio mal configurado"),
            "descripcion": criterio.get("descripcion", "Sin términos"),
            "error": "contiene_todos sin terminos",
            "pista": criterio.get("pista", "Revisa la configuración del ejercicio"),
        }
    faltan = [t for t in terminos if not _contiene_termino(respuesta, t)]
    cumplido = len(faltan) == 0
    esperado = criterio.get(
        "esperado",
        "Debe incluir: " + ", ".join(terminos),
    )
    detalle = {
        "cumplido": cumplido,
        "tipo": "contiene_todos",
        "terminos": terminos,
        "peso": criterio.get("peso", 1),
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
        detalle["pista"] = criterio.get("pista", f"Faltan: {', '.join(faltan)}")
    return detalle


def evaluar_criterio_contiene_alguno(respuesta: str, criterio: dict) -> dict:
    terminos = criterio.get("terminos") or []
    if not terminos:
        return {
            "cumplido": False,
            "tipo": "contiene_alguno",
            "terminos": terminos,
            "peso": criterio.get("peso", 1),
            "esperado": criterio.get("esperado", "Criterio mal configurado"),
            "descripcion": criterio.get("descripcion", "Sin términos"),
            "error": "contiene_alguno sin terminos",
            "pista": criterio.get("pista", "Revisa la configuración del ejercicio"),
        }
    cumplido = any(_contiene_termino(respuesta, t) for t in terminos)
    esperado = criterio.get(
        "esperado",
        "Debe incluir alguno de: " + ", ".join(terminos),
    )
    detalle = {
        "cumplido": cumplido,
        "tipo": "contiene_alguno",
        "terminos": terminos,
        "peso": criterio.get("peso", 1),
        "esperado": esperado,
        "descripcion": criterio.get("descripcion", esperado[:60]),
    }
    if not cumplido:
        detalle["pista"] = criterio.get("pista", esperado)
    return detalle


def evaluar_criterio(respuesta: str, criterio: dict) -> dict:
    tipo = criterio.get("tipo", "regex")
    if tipo == "contiene_todos":
        return evaluar_criterio_contiene_todos(respuesta, criterio)
    if tipo == "contiene_alguno":
        return evaluar_criterio_contiene_alguno(respuesta, criterio)
    return evaluar_criterio_regex(respuesta, criterio)


def evaluar(escenario: dict, respuesta: str) -> dict:
    criterios = escenario.get("criterios", [])
    detalles = [evaluar_criterio(respuesta, c) for c in criterios]
    peso_total = sum(c.get("peso", 1) for c in criterios) or 1
    peso_obtenido = sum(d["peso"] for d in detalles if d["cumplido"])
    nota = round(10 * peso_obtenido / peso_total, 1)
    aprobado = nota >= 5.0

    if nota >= 9:
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
        "peso_obtenido": peso_obtenido,
        "peso_total": peso_total,
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
