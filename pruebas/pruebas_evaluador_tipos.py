#!/usr/bin/env python3
"""Pruebas de tipos de criterio en evaluador.py."""

import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import evaluador


def probar_contiene_todos_orden_irrelevante():
    escenario = {
        "id": "t1",
        "modulo": "test",
        "criterios": [
            {"tipo": "contiene_todos", "terminos": ["docker", "run"], "peso": 5},
            {"tipo": "contiene_todos", "terminos": ["nginx"], "peso": 5},
        ],
    }
    r = evaluador.evaluar(escenario, "run docker con nginx")
    assert r["nota"] == 10.0


def probar_contiene_alguno_sinonimos():
    escenario = {
        "id": "t2",
        "modulo": "test",
        "criterios": [
            {
                "tipo": "contiene_alguno",
                "terminos": ["nginx:latest", "nginx"],
                "peso": 10,
            },
        ],
    }
    r = evaluador.evaluar(escenario, "docker run nginx:latest")
    assert r["detalles"][0]["cumplido"]


def probar_regex_sigue_funcionando():
    escenario = {
        "id": "t3",
        "modulo": "docker",
        "criterios": [
            {"tipo": "regex", "patron": r"docker\s+logs", "peso": 10, "banderas": "i"},
        ],
    }
    r = evaluador.evaluar(escenario, "docker logs mi_contenedor")
    assert r["aprobado"]


if __name__ == "__main__":
    probar_contiene_todos_orden_irrelevante()
    probar_contiene_alguno_sinonimos()
    probar_regex_sigue_funcionando()
    print("OK evaluador tipos")
