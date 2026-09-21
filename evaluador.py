#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Evaluador automático de respuestas para La máquina de ejercicios."""

import argparse
import json
import sys
from pathlib import Path

import criterios
from criterios import (  # noqa: F401 — API pública usada por otros módulos
    banderas_regex,
    es_obligatorio,
    peso_criterio,
)

TOPE_NOTA_OBLIGATORIO_FALLIDO = 4.0


def evaluar_criterio(respuesta: str, criterio: dict) -> dict:
    return criterios.desde_json(criterio).evaluar(respuesta)


def evaluar(escenario: dict, respuesta: str) -> dict:
    lista = escenario.get("criterios", [])
    detalles = [evaluar_criterio(respuesta, c) for c in lista]
    peso_total = sum(peso_criterio(c) for c in lista) or 1
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
