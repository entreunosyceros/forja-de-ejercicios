#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Regresión ReDoS y rechazo de patrones anidados."""

from __future__ import annotations

import sys
from pathlib import Path

import pytest

RAIZ = Path(__file__).resolve().parents[1]
if str(RAIZ) not in sys.path:
    sys.path.insert(0, str(RAIZ))

import criterios
import evaluador


def probar_patron_redos_rechazado_en_validacion():
    with pytest.raises(ValueError, match="ReDoS"):
        criterios.desde_json({"tipo": "regex", "patron": "(a+)+$", "peso": 1}).validar()


def probar_evaluar_patron_costoso_no_cuelga():
    # Entrada que no casa: no debe colgar ni marcar cumplido.
    malo = {"tipo": "regex", "patron": "(a+)+b", "peso": 1}
    det = criterios.desde_json(malo).evaluar("a" * 28)
    assert det["cumplido"] is False
    assert det.get("peso_parcial", 0) == 0


def probar_banco_ejemplo_no_acepta_comentario_basura():
    """Comentario negando el comando no debe bastar si hay criterios estrictos."""
    esc = {
        "id": "docker-test",
        "modulo": "docker",
        "criterios": [
            {"tipo": "regex", "patron": r"docker\s+logs", "peso": 3, "banderas": "i"},
            {"tipo": "regex", "patron": r"compose\s+up", "peso": 3, "banderas": "i"},
        ],
        "solucion_referencia": "docker logs x\ndocker compose up -d",
    }
    basura = "docker run -p 8080:80 nginx  # NO, en realidad no hay que hacer esto"
    res = evaluador.evaluar(esc, basura)
    assert res["nota"] < 5
