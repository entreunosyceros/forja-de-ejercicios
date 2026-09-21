#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Pruebas del segundo corrector IA (sin llamar a la API)."""

from __future__ import annotations

import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
if str(RAIZ) not in sys.path:
    sys.path.insert(0, str(RAIZ))

import corrector_ia
import evaluador


ESCENARIO = {
    "id": "test-ia-01",
    "modulo": "docker",
    "titulo": "Logs del contenedor",
    "enunciado": "Muestra los logs del contenedor api-web y levántalo de nuevo.",
    "solucion_referencia": "docker logs api-web\ndocker compose up -d --build",
    "criterios": [
        {"tipo": "regex", "patron": r"docker\s+logs", "peso": 3, "banderas": "i"},
        {"tipo": "regex", "patron": r"compose\s+up|docker-compose\s+up", "peso": 3, "banderas": "i"},
        {"tipo": "regex", "patron": r"api-web", "peso": 1, "banderas": "i"},
    ],
}


def test_sin_ia_etiqueta_honestidad(monkeypatch):
    monkeypatch.delenv("FORJAEXAMENES_CORRECTOR_IA", raising=False)
    res = evaluador.evaluar(ESCENARIO, ESCENARIO["solucion_referencia"])
    assert res["nota"] >= 9
    assert "elementos que sé comprobar" in res["etiqueta_nota"]
    assert res["medicion"] == "elementos"
    assert res["corrector_ia"]["usado"] is False


def test_combinar_minimo():
    juicio = {"usado": True, "nota": 4.0}
    out = corrector_ia.combinar_notas(8.0, juicio)
    assert out["nota"] == 4.0
    assert out["modo_combinacion"] == "minimo"


def test_combinar_ponderada(monkeypatch):
    monkeypatch.setenv("FORJAEXAMENES_CORRECTOR_IA_MODO", "ponderada")
    juicio = {"usado": True, "nota": 5.0}
    out = corrector_ia.combinar_notas(10.0, juicio)
    assert out["nota"] == 8.0
    assert out["modo_combinacion"] == "ponderada_60_40"


def test_con_ia_mock(monkeypatch):
    monkeypatch.setenv("FORJAEXAMENES_CORRECTOR_IA", "true")

    def fake_juzgar(escenario, respuesta, *, nota_elementos, modelo=None):
        return {
            "usado": True,
            "nota": 3.0,
            "cumple_enunciado": False,
            "comentario": "No resuelve el reinicio del servicio.",
            "modelo": "mock",
        }

    monkeypatch.setattr(corrector_ia, "juzgar", fake_juzgar)
    res = evaluador.evaluar(ESCENARIO, ESCENARIO["solucion_referencia"])
    assert res["nota_elementos"] >= 9
    assert res["nota"] == 3.0
    assert res["medicion"] == "elementos_y_ia"
    assert "juicio IA" in res["etiqueta_nota"]
    assert "No resuelve" in res["retroalimentacion"]
