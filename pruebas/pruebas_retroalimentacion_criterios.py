#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Pruebas de textos legibles en criterios de corrección."""

import json
import subprocess
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import evaluador
from retroalimentacion_criterios import (
    RUTA_REGLAS_REGEX,
    _cargar_reglas_regex,
    _esperado_pista_regex,
    aplicar_retroalimentacion_a_criterio,
    parece_patron_regex,
    pista_desde_criterio,
)


def probar_tabla_reglas_json():
    assert RUTA_REGLAS_REGEX.is_file(), f"Falta {RUTA_REGLAS_REGEX.name}"
    reglas = _cargar_reglas_regex()
    assert len(reglas) >= 10
    ids = {r.get("id") for r in reglas}
    assert "create_index" in ids
    assert "explain" in ids
    assert "docker_logs" in ids
    desc, esperado, pista = _esperado_pista_regex(r"CREATE\s+INDEX.*pedidos")
    assert "CREATE INDEX" in esperado
    assert "índice" in pista.lower() or "indice" in pista.lower()
    desc2, _, _ = _esperado_pista_regex(r"(?i)explain\s+select")
    assert "EXPLAIN" in desc2.upper()
    print("OK tabla reglas JSON")


def probar_create_index_bd():
    criterio = {
        "tipo": "regex",
        "patron": r"CREATE\s+INDEX.*pedidos|INDEX.*fecha",
        "peso": 4,
        "banderas": "i",
    }
    aplicar_retroalimentacion_a_criterio(criterio)
    assert not parece_patron_regex(criterio["esperado"])
    assert criterio["pista"] != criterio["esperado"]
    assert "CREATE INDEX" in criterio["esperado"]
    assert "índice" in criterio["pista"].lower() or "indice" in criterio["pista"].lower()
    print("OK create index bd")


def probar_evaluador_enriquece_al_vuelo():
    escenario = {
        "id": "x",
        "modulo": "bd",
        "criterios": [
            {
                "tipo": "regex",
                "patron": r"CREATE\s+INDEX.*pedidos|INDEX.*fecha",
                "peso": 10,
                "esperado": "Incluir en la respuesta: CREATE\\s+INDEX.*pedidos|INDEX.*fecha",
                "pista": "Incluir en la respuesta: CREATE\\s+INDEX.*pedidos|INDEX.*fecha",
            },
        ],
    }
    resultado = evaluador.evaluar(escenario, "solo explain")
    detalle = resultado["detalles"][0]
    assert not detalle["cumplido"]
    assert "\\s" not in detalle["esperado"]
    assert detalle["pista"] != detalle["esperado"]
    assert "CREATE INDEX" in detalle["esperado"] or "índice" in detalle["esperado"].lower()
    print("OK evaluador enriquece")


def probar_generador_bd_no_regex_en_pista():
    proc = subprocess.run(
        [sys.executable or "python3", "generador.py", "-m", "bd"],
        capture_output=True,
        text=True,
        cwd=RAIZ,
    )
    assert proc.returncode == 0, proc.stderr
    escenario = json.loads(proc.stdout)
    for criterio in escenario["criterios"]:
        aplicar_retroalimentacion_a_criterio(criterio)
        if criterio.get("patron"):
            assert not parece_patron_regex(criterio.get("esperado", ""))
            if criterio.get("pista"):
                assert criterio["pista"] != criterio["esperado"] or "EXPLAIN" in criterio["patron"]
    print("OK generador bd")


def probar_palabra_clave_docs():
    criterio = {
        "tipo": "contiene_todos",
        "terminos": ["docker run"],
        "peso": 5,
    }
    pista = pista_desde_criterio(criterio)
    assert "docker run" not in pista  # no filtrar el término a corregir
    assert "enunciado" in pista.lower()
    print("OK palabra clave docs")


def main() -> int:
    probar_tabla_reglas_json()
    probar_create_index_bd()
    probar_evaluador_enriquece_al_vuelo()
    probar_generador_bd_no_regex_en_pista()
    probar_palabra_clave_docs()
    print("Todas las pruebas de retroalimentación pasaron.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
