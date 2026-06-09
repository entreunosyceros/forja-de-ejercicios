#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Pruebas de tipo de materia para apuntes PDF."""

import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import modelo_ejercicio
from tipo_materia import (
    es_clave_valida,
    resolver_tipo_materia,
    texto_enunciado_alumno,
)


def probar_resolver_informatica():
    assert resolver_tipo_materia("docs_docker", tema_slug="docker") == "informatica"
    assert resolver_tipo_materia("docs_git") == "informatica"
    print("OK resolver informatica")


def probar_resolver_idiomas():
    assert resolver_tipo_materia("docs_ingles", tema_slug="ingles") == "idiomas"
    assert resolver_tipo_materia("docs_english", tema_slug="english") == "idiomas"
    print("OK resolver idiomas")


def probar_resolver_general():
    assert resolver_tipo_materia("docs_historia", tema_slug="historia") == "general"
    assert resolver_tipo_materia("docs_matematicas", tema_slug="matematicas") == "general"
    print("OK resolver general")


def probar_validacion_idiomas():
    fragmento = (
        "Present perfect: use have/has + past participle. "
        "Example: I have visited London. She has finished the homework."
    )
    propuesta = modelo_ejercicio.validar_propuesta_gemini(
        {
            "tema": "Present perfect",
            "pregunta": "Escribe dos frases en present perfect usando el vocabulario del fragmento.",
            "palabras_clave": ["have visited", "has finished", "past participle"],
            "solucion_modelo": "I have visited London.\nShe has finished the homework.",
        },
        modulo="docs_ingles",
        texto_fragmento=fragmento,
        tipo_materia="idiomas",
    )
    assert propuesta["tipo_materia"] == "idiomas"
    assert len(propuesta["palabras_clave"]) >= 2
    print("OK validacion idiomas")


def probar_validacion_general():
    fragmento = (
        "La Revolución Industrial comenzó en Gran Bretaña en el siglo XVIII "
        "y transformó la producción manufacturera con la máquina de vapor."
    )
    propuesta = modelo_ejercicio.validar_propuesta_gemini(
        {
            "tema": "Revolución Industrial",
            "pregunta": "Explica dónde y cuándo comenzó la Revolución Industrial según el fragmento.",
            "palabras_clave": ["Gran Bretaña", "siglo XVIII", "máquina de vapor"],
            "solucion_modelo": "Comenzó en Gran Bretaña en el siglo XVIII con la máquina de vapor.",
        },
        modulo="docs_historia",
        texto_fragmento=fragmento,
        tipo_materia="general",
    )
    assert propuesta["tipo_materia"] == "general"
    print("OK validacion general")


def probar_enunciado_por_tipo():
    claves = ["docker run", "nginx"]
    assert "comandos" in texto_enunciado_alumno("informatica", claves)
    assert "traducción" in texto_enunciado_alumno("idiomas", claves) or "frases" in texto_enunciado_alumno("idiomas", claves)
    assert "fragmento" in texto_enunciado_alumno("general", claves)
    print("OK enunciado por tipo")


def probar_clave_idiomas_corta():
    assert es_clave_valida("have", "idiomas", en_fragmento=True)
    assert not es_clave_valida("ab", "idiomas", en_fragmento=False)
    print("OK clave idiomas")


if __name__ == "__main__":
    probar_resolver_informatica()
    probar_resolver_idiomas()
    probar_resolver_general()
    probar_validacion_idiomas()
    probar_validacion_general()
    probar_enunciado_por_tipo()
    probar_clave_idiomas_corta()
    print("Todas las pruebas tipo_materia OK")
