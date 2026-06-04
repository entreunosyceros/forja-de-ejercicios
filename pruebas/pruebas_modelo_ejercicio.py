#!/usr/bin/env python3
"""Pruebas del modelo de ejercicio verificable y construcción desde Gemini."""

import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import evaluador
import modelo_ejercicio


def probar_construccion_desde_propuesta():
    fragmento = (
        "docker run -d -p 8080:80 nginx es el comando para levantar el contenedor "
        "nginx mapeando puertos"
    )
    propuesta = {
        "tema": "docker",
        "pregunta": "Levanta un contenedor nginx mapeando el puerto 8080 al 80 del host.",
        "palabras_clave": ["docker run", "-p", "nginx"],
        "solucion_modelo": "docker run -d -p 8080:80 nginx",
    }
    escenario = modelo_ejercicio.construir_escenario_desde_propuesta(
        propuesta,
        "docs_docker",
        titulo_coleccion="Docker",
        texto_fragmento=fragmento,
    )
    assert escenario["modulo"] == "docs_docker"
    assert len(escenario["criterios"]) == 3
    assert escenario["criterios"][0]["tipo"] in ("contiene_todos", "contiene_alguno")

    resultado = evaluador.evaluar(escenario, "docker run -d -p 8080:80 nginx")
    assert resultado["aprobado"]
    assert resultado["nota"] >= 7


def probar_rechaza_clave_vaga():
    fragmento = "docker run nginx -p 8080:80"
    try:
        modelo_ejercicio.validar_propuesta_gemini(
            {
                "tema": "docker",
                "pregunta": "Pregunta suficientemente larga para validar el enunciado.",
                "palabras_clave": ["configurar", "docker run"],
            },
            modulo="docs_docker",
            texto_fragmento=fragmento,
        )
        assert False, "Debía rechazar palabra genérica"
    except ValueError as e:
        assert "genérica" in str(e).lower() or "configurar" in str(e)


def probar_rechaza_clave_fuera_fragmento():
    fragmento = "solo aparece docker run aqui"
    try:
        modelo_ejercicio.validar_propuesta_gemini(
            {
                "tema": "docker",
                "pregunta": "Pregunta suficientemente larga para validar el enunciado.",
                "palabras_clave": ["docker run", "kubernetes"],
            },
            modulo="docs_docker",
            texto_fragmento=fragmento,
        )
        assert False, "Debía rechazar clave ausente del fragmento"
    except ValueError as e:
        assert "fragmento" in str(e).lower()


def probar_variantes_contiene_alguno():
    propuesta = {
        "tema": "docker",
        "pregunta": "Levanta nginx con docker run y publica el puerto 8080 al 80.",
        "palabras_clave": ["docker run", "nginx"],
        "variantes": {"nginx": ["nginx", "nginx:latest"]},
    }
    fragmento = "docker run nginx nginx:latest -p 8080:80"
    escenario = modelo_ejercicio.construir_escenario_desde_propuesta(
        propuesta, "docs_docker", texto_fragmento=fragmento
    )
    criterio_nginx = next(c for c in escenario["criterios"] if "nginx" in str(c.get("terminos", "")))
    assert criterio_nginx["tipo"] == "contiene_alguno"
    r = evaluador.evaluar(escenario, "docker run -d nginx:latest -p 8080:80")
    assert r["detalles"][1]["cumplido"] or r["nota"] >= 5


if __name__ == "__main__":
    probar_construccion_desde_propuesta()
    probar_rechaza_clave_vaga()
    probar_rechaza_clave_fuera_fragmento()
    probar_variantes_contiene_alguno()
    print("OK modelo_ejercicio")
