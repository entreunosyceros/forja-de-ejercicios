#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import evaluador  # noqa: E402
from alias_comandos import contiene_termino_flexible, variantes_termino  # noqa: E402


def probar_variantes_docker_run():
    vars_ = variantes_termino("docker run")
    assert "docker container run" in [v.lower() for v in vars_]
    assert contiene_termino_flexible("docker container run -d nginx", "docker run")
    print("OK variantes docker run")


def probar_evaluador_acepta_sinonimo():
    escenario = {
        "id": "t1",
        "modulo": "banco_docker",
        "criterios": [
            {"tipo": "contiene_todos", "terminos": ["docker run"], "peso": 10},
        ],
        "solucion_referencia": "docker run nginx",
    }
    r = evaluador.evaluar(escenario, "docker container run -d nginx")
    assert r["detalles"][0]["cumplido"], r
    print("OK evaluador sinonimo docker container run")


if __name__ == "__main__":
    probar_variantes_docker_run()
    probar_evaluador_acepta_sinonimo()
    print("Todas las pruebas alias OK")
