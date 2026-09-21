#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
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


def probar_banderas_multiline_no_activa_ignorecase():
    """«multiline» contiene la letra i pero no debe activar IGNORECASE."""
    escenario = {
        "id": "t4",
        "modulo": "test",
        "criterios": [
            {
                "tipo": "regex",
                "patron": r"SELECT",
                "peso": 10,
                "banderas": "multiline",
            },
        ],
    }
    r = evaluador.evaluar(escenario, "select * from x")
    assert not r["detalles"][0]["cumplido"], "multiline no debe ignorar mayúsculas"
    r2 = evaluador.evaluar(escenario, "SELECT * from x")
    assert r2["detalles"][0]["cumplido"]


def probar_banderas_i_sigue_funcionando():
    escenario = {
        "id": "t5",
        "modulo": "test",
        "criterios": [
            {"tipo": "regex", "patron": r"SELECT", "peso": 10, "banderas": "i"},
        ],
    }
    r = evaluador.evaluar(escenario, "select * from x")
    assert r["detalles"][0]["cumplido"]


def probar_flags_legacy_se_normaliza_a_banderas():
    from criterios import normalizar_banderas_en_criterio

    c = {"tipo": "regex", "patron": r"x", "peso": 1, "flags": "i"}
    normalizar_banderas_en_criterio(c)
    assert c.get("banderas") == "i"
    assert "flags" not in c
    escenario = {
        "id": "legacy",
        "modulo": "test",
        "criterios": [
            {"tipo": "regex", "patron": r"HELLO", "peso": 10, "flags": "i"},
        ],
    }
    assert evaluador.evaluar(escenario, "hello")["detalles"][0]["cumplido"]
    assert escenario["criterios"][0].get("banderas") == "i"
    assert "flags" not in escenario["criterios"][0]


def probar_peso_texto_no_rompe():
    escenario = {
        "id": "t6",
        "modulo": "test",
        "criterios": [
            {"tipo": "contiene_todos", "terminos": ["docker"], "peso": "2"},
            {"tipo": "contiene_todos", "terminos": ["nginx"], "peso": "2"},
        ],
    }
    r = evaluador.evaluar(escenario, "docker run nginx")
    assert r["nota"] == 10.0
    assert r["peso_total"] == 4


def probar_peso_negativo_no_nota_disparatada():
    escenario = {
        "id": "t7",
        "modulo": "test",
        "criterios": [
            {"tipo": "contiene_todos", "terminos": ["docker"], "peso": 5},
            {"tipo": "contiene_todos", "terminos": ["faltante"], "peso": -4},
        ],
    }
    r = evaluador.evaluar(escenario, "docker run")
    assert 0.0 <= r["nota"] <= 10.0
    assert r["nota"] != 50.0


def probar_nota_siempre_entre_0_y_10():
    escenario = {
        "id": "t8",
        "modulo": "test",
        "criterios": [
            {"tipo": "contiene_todos", "terminos": ["a"], "peso": "no-numero"},
            {"tipo": "contiene_todos", "terminos": ["b"], "peso": 0},
        ],
    }
    r = evaluador.evaluar(escenario, "a b")
    assert 0.0 <= r["nota"] <= 10.0


if __name__ == "__main__":
    probar_contiene_todos_orden_irrelevante()
    probar_contiene_alguno_sinonimos()
    probar_regex_sigue_funcionando()
    probar_banderas_multiline_no_activa_ignorecase()
    probar_banderas_i_sigue_funcionando()
    probar_flags_legacy_se_normaliza_a_banderas()
    probar_peso_texto_no_rompe()
    probar_peso_negativo_no_nota_disparatada()
    probar_nota_siempre_entre_0_y_10()
    print("OK evaluador tipos")
