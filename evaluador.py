#!/usr/bin/env python3
"""Evaluador automático de respuestas para La máquina de ejercicios."""

import argparse
import json
import re
import sys
from pathlib import Path

# Evaluar un criterio
def evaluar_criterio(respuesta: str, criterio: dict) -> dict:
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
        }
    esperado = criterio.get("esperado", f"La respuesta debe incluir algo como: {patron[:50]}")
    detalle = {
        "cumplido": cumplido,
        "patron": patron,
        "tipo": criterio.get("tipo", "regex"),
        "peso": criterio.get("peso", 1),
        "descripcion": criterio.get("descripcion", patron[:60]),
        "esperado": esperado,
    }
    if not cumplido:
        detalle["pista"] = criterio.get("pista", esperado)
    return detalle

# Evaluar la respuesta del alumno
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

    # Devolver el resultado de la evaluación
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
