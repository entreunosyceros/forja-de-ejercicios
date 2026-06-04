#!/usr/bin/env python3
"""Genera datos de revisión (tabla comparativa + casos de prueba) para el profesor."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import banco_loader  # noqa: E402
import evaluador  # noqa: E402
from alias_comandos import variantes_termino  # noqa: E402


def _texto_criterio(criterio: dict[str, Any]) -> str:
    tipo = criterio.get("tipo", "regex")
    if tipo == "regex":
        return f"Regex: {criterio.get('patron', '')}"
    terminos = criterio.get("terminos") or []
    return f"{tipo}: {', '.join(terminos)}"


def _aliases_texto(terminos: list[str]) -> str:
    partes = []
    for t in terminos:
        vars_ = variantes_termino(t)
        if len(vars_) > 1:
            partes.append(f"{t} → {', '.join(vars_)}")
    return "; ".join(partes)


def _respuesta_prueba_fallo(criterio: dict[str, Any], escenario: dict[str, Any]) -> str:
    tipo = criterio.get("tipo", "regex")
    if tipo == "regex":
        return "respuesta vacia o incompleta"
    terminos = criterio.get("terminos") or []
    if not terminos:
        return "sin contenido relevante"
    return f"texto sin {terminos[0]}"


def _respuesta_prueba_sinonimo(criterio: dict[str, Any], escenario: dict[str, Any]) -> str | None:
    tipo = criterio.get("tipo", "")
    if tipo not in ("contiene_todos", "contiene_alguno"):
        return None
    terminos = criterio.get("terminos") or []
    if not terminos:
        return None
    vars_ = variantes_termino(terminos[0])
    if len(vars_) < 2:
        return None
    alternativa = vars_[1]
    sol = escenario.get("solucion_referencia") or ""
    if terminos[0].lower() in sol.lower():
        return sol.lower().replace(terminos[0].lower(), alternativa.lower(), 1)
    return alternativa + " " + (sol or "nginx")


def construir_revision(eid: str) -> dict[str, Any]:
    datos = banco_loader.cargar_pendiente(eid)
    escenario = datos.get("escenario_preview") or {}
    propuesta = datos.get("propuesta") or {}
    solucion = escenario.get("solucion_referencia") or ""

    filas: list[dict[str, Any]] = []
    for i, crit in enumerate(escenario.get("criterios") or [], start=1):
        resp_ok = solucion or _respuesta_prueba_fallo(crit, escenario)
        ev_ok = evaluador.evaluar_criterio(resp_ok, crit)
        resp_syn = _respuesta_prueba_sinonimo(crit, escenario)
        ev_syn = None
        if resp_syn:
            ev_syn = evaluador.evaluar_criterio(resp_syn, crit)
        resp_fail = _respuesta_prueba_fallo(crit, escenario)
        ev_fail = evaluador.evaluar_criterio(resp_fail, crit)

        fila = {
            "indice": i,
            "tipo": crit.get("tipo", ""),
            "exigencia": _texto_criterio(crit),
            "peso": crit.get("peso", 1),
            "esperado": crit.get("esperado", ""),
            "aliases": _aliases_texto(crit.get("terminos") or []),
            "prueba_referencia": {
                "respuesta": resp_ok[:500],
                "cumplido": ev_ok.get("cumplido"),
            },
            "prueba_alternativa": {
                "respuesta": (resp_syn or "")[:500],
                "cumplido": ev_syn.get("cumplido") if ev_syn else None,
                "aplica": resp_syn is not None,
            },
            "prueba_insuficiente": {
                "respuesta": resp_fail[:200],
                "cumplido": ev_fail.get("cumplido"),
            },
        }
        filas.append(fila)

    ev_ref = evaluador.evaluar(escenario, solucion) if solucion else None
    palabras_prop = propuesta.get("palabras_clave") or []
    palabras_esc = []
    for c in escenario.get("criterios") or []:
        if c.get("tipo") in ("contiene_todos", "contiene_alguno"):
            palabras_esc.extend(c.get("terminos") or [])

    return {
        "id": eid,
        "modulo": datos.get("modulo") or escenario.get("modulo", ""),
        "creado": datos.get("creado", ""),
        "propuesta_gemini": {
            "pregunta": propuesta.get("pregunta", ""),
            "palabras_clave": palabras_prop,
            "solucion_modelo": propuesta.get("solucion_modelo", ""),
        },
        "escenario": {
            "titulo": escenario.get("titulo", ""),
            "enunciado": escenario.get("enunciado", ""),
            "solucion_referencia": solucion,
            "palabras_en_criterios": palabras_esc,
        },
        "comparativa_propuesta": {
            "pregunta_coincide": (
                propuesta.get("pregunta", "").strip()[:80]
                in (escenario.get("enunciado") or "")
                or not propuesta.get("pregunta")
            ),
            "palabras_gemini": ", ".join(palabras_prop) if palabras_prop else "—",
            "palabras_criterios": ", ".join(palabras_esc) if palabras_esc else "—",
        },
        "filas_criterios": filas,
        "evaluacion_referencia": ev_ref,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Revisión profesor (JSON)")
    parser.add_argument("--id", required=True, help="ID del pendiente en banco/pendientes/")
    args = parser.parse_args()
    try:
        salida = construir_revision(args.id)
    except FileNotFoundError as e:
        print(json.dumps({"error": str(e)}, ensure_ascii=False), file=sys.stderr)
        return 1
    print(json.dumps(salida, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
