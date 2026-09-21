#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Pruebas del motor de plantillas JSON y del registro Strategy de criterios."""

from __future__ import annotations

import random
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
if str(RAIZ) not in sys.path:
    sys.path.insert(0, str(RAIZ))

import criterios
import evaluador
import motor_plantillas


def test_plantillas_generan_escenario_valido():
    for modulo in ("bd", "docker", "git", "redes"):
        esc = motor_plantillas.generar_desde_plantilla(modulo, rng=random.Random(42))
        assert esc["modulo"] == modulo
        assert esc.get("version") == 1
        assert esc["titulo"] and esc["enunciado"]
        assert len(esc["criterios"]) >= 2
        assert "{tabla}" not in esc["enunciado"]
        assert "{v1}" not in esc["enunciado"]
        resultado = evaluador.evaluar(esc, esc["solucion_referencia"])
        assert resultado["nota"] >= 5.0, (modulo, resultado)


def test_redes_vlans_distintas():
    for semilla in range(30):
        esc = motor_plantillas.generar_desde_plantilla("redes", rng=random.Random(semilla))
        assert esc["parametros"]["v1"] != esc["parametros"]["v2"]


def test_criterios_registro_strategy():
    assert "regex" in criterios.tipos_soportados()
    assert "no_contiene" in criterios.tipos_soportados()
    c = criterios.desde_json({"tipo": "contiene_todos", "terminos": ["git"], "peso": 1})
    c.validar()
    det = c.evaluar("git status")
    assert det["cumplido"] is True
