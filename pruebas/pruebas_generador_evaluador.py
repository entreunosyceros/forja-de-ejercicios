#!/usr/bin/env python3
"""Pruebas del generador y evaluador."""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent


def ejecutar(comando):
    return subprocess.run(comando, capture_output=True, text=True, cwd=RAIZ)


MODULOS_PRUEBA = (
    "redes", "sistemas", "bd", "docker", "git",
    "poo", "bd_sql", "bd_modelo", "bd_transacciones", "bd_jdbc",
)


def probar_generador_todos_modulos():
    for modulo in MODULOS_PRUEBA:
        resultado = ejecutar(["python3", "generador.py", "-m", modulo])
        assert resultado.returncode == 0, resultado.stderr
        datos = json.loads(resultado.stdout)
        assert datos["modulo"] == modulo
        assert datos["criterios"]


def probar_poo_variantes():
    modulos = set()
    for _ in range(30):
        resultado = ejecutar(["python3", "generador.py", "-m", "poo"])
        datos = json.loads(resultado.stdout)
        modulos.add(datos["titulo"])
    assert len(modulos) >= 3, "POO debería rotar entre varias variantes"


def probar_evaluador_nota_maxima():
    generacion = ejecutar(["python3", "generador.py", "-m", "bd_jdbc"])
    escenario = json.loads(generacion.stdout)
    respuesta = escenario["solucion_referencia"]

    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as archivo:
        json.dump(escenario, archivo)
        ruta_escenario = archivo.name

    evaluacion = ejecutar([
        "python3", "evaluador.py",
        "-e", ruta_escenario,
        "-r", respuesta,
    ])
    assert evaluacion.returncode == 0, evaluacion.stderr
    resultado = json.loads(evaluacion.stdout)
    assert resultado["nota"] == 10.0
    assert resultado["aprobado"] is True
    assert "retroalimentacion" in resultado
    assert resultado["detalles"][0]["cumplido"] is True


def probar_evaluador_respuesta_vacia():
    generacion = ejecutar(["python3", "generador.py", "-m", "git"])
    escenario = json.loads(generacion.stdout)
    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as archivo:
        json.dump(escenario, archivo)
        ruta_escenario = archivo.name

    evaluacion = ejecutar(["python3", "evaluador.py", "-e", ruta_escenario, "-r", ""])
    resultado = json.loads(evaluacion.stdout)
    assert resultado["nota"] < 5.0
    assert resultado["aprobado"] is False


if __name__ == "__main__":
    probar_generador_todos_modulos()
    probar_poo_variantes()
    probar_evaluador_nota_maxima()
    probar_evaluador_respuesta_vacia()
    print("OK: todas las pruebas Python pasaron")
    sys.exit(0)
