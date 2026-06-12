#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Pruebas del generador y evaluador."""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
# sys.executable usa el mismo intérprete que lanza la prueba (python en Windows,
# python3 en Linux/macOS) en lugar de un nombre fijo que puede no existir.
PYTHON = sys.executable or "python3"


def ejecutar(comando):
    return subprocess.run(comando, capture_output=True, text=True, cwd=RAIZ)


MODULOS_PRUEBA = (
    "redes", "sistemas", "bd", "docker", "git",
    "poo", "bd_sql", "bd_modelo", "bd_transacciones", "bd_jdbc",
)


def probar_generador_todos_modulos():
    for modulo in MODULOS_PRUEBA:
        resultado = ejecutar([PYTHON, "generador.py", "-m", modulo])
        assert resultado.returncode == 0, resultado.stderr
        datos = json.loads(resultado.stdout)
        assert datos["modulo"] == modulo
        assert datos["criterios"]


def probar_poo_variantes():
    modulos = set()
    for _ in range(30):
        resultado = ejecutar([PYTHON, "generador.py", "-m", "poo"])
        datos = json.loads(resultado.stdout)
        modulos.add(datos["titulo"])
    assert len(modulos) >= 3, "POO debería rotar entre varias variantes"


def probar_evaluador_nota_maxima():
    generacion = ejecutar([PYTHON, "generador.py", "-m", "bd_jdbc"])
    escenario = json.loads(generacion.stdout)
    respuesta = escenario["solucion_referencia"]

    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as archivo:
        json.dump(escenario, archivo)
        ruta_escenario = archivo.name

    evaluacion = ejecutar([
        PYTHON, "evaluador.py",
        "-e", ruta_escenario,
        "-r", respuesta,
    ])
    assert evaluacion.returncode == 0, evaluacion.stderr
    resultado = json.loads(evaluacion.stdout)
    assert resultado["nota"] == 10.0
    assert resultado["aprobado"] is True
    assert "retroalimentacion" in resultado
    assert resultado["detalles"][0]["cumplido"] is True


def probar_aplicar_nivel_sin_duplicar_prefijo():
    sys.path.insert(0, str(RAIZ))
    import generador

    base = {
        "modulo": "poo",
        "titulo": "Test",
        "enunciado": "Implementa la interfaz.",
        "criterios": [{"tipo": "contiene_todos", "terminos": ["implements"], "peso": 1}],
    }
    una_vez = generador._aplicar_nivel(dict(base), 2)
    assert una_vez["enunciado"].count("[Nivel intermedio]") == 1
    dos_veces = generador._aplicar_nivel(dict(una_vez), 2)
    assert dos_veces["enunciado"].count("[Nivel intermedio]") == 1
    assert "Implementa la interfaz." in dos_veces["enunciado"]


def probar_evaluador_respuesta_vacia():
    generacion = ejecutar([PYTHON, "generador.py", "-m", "git"])
    escenario = json.loads(generacion.stdout)
    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as archivo:
        json.dump(escenario, archivo)
        ruta_escenario = archivo.name

    evaluacion = ejecutar([PYTHON, "evaluador.py", "-e", ruta_escenario, "-r", ""])
    resultado = json.loads(evaluacion.stdout)
    assert resultado["nota"] < 5.0
    assert resultado["aprobado"] is False


if __name__ == "__main__":
    probar_generador_todos_modulos()
    probar_poo_variantes()
    probar_aplicar_nivel_sin_duplicar_prefijo()
    probar_evaluador_nota_maxima()
    probar_evaluador_respuesta_vacia()
    print("OK: todas las pruebas Python pasaron")
    sys.exit(0)
