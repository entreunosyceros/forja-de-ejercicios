#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Pruebas del indexador de documentación (sin Gemini ni pypdf obligatorio)."""

import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

from indexador_docs import (  # noqa: E402
    fragmentar_texto,
    metadatos_desde_ruta,
    nombre_modulo_desde_carpeta,
)
from generador_docs import seleccionar_fragmento, construir_origen_apuntes  # noqa: E402


def prueba_fragmentar():
    texto = "a" * 5000
    partes = fragmentar_texto(texto, tamano=1000, solapamiento=100)
    assert len(partes) >= 4
    assert all(len(p) <= 1000 for p in partes)
    print("OK fragmentar_texto")


def prueba_nombre_modulo():
    assert nombre_modulo_desde_carpeta("kubernetes") == "docs_kubernetes"
    assert nombre_modulo_desde_carpeta("Java POO") == "docs_java_poo"
    print("OK nombre_modulo_desde_carpeta")


def prueba_metadatos_ruta():
    raiz = RAIZ / "documentacion" / "docker"
    pdf = raiz / "volumenes" / "persistencia.pdf"
    meta = metadatos_desde_ruta(pdf, raiz)
    assert meta["tema"] == "docker"
    assert meta["capitulo"] == "volumenes"
    print("OK metadatos_desde_ruta")


def prueba_seleccion_fragmento():
    chunks = [
        {"id": "a", "capitulo_slug": "redes", "seccion_slug": "bridge", "texto": "x" * 100},
        {"id": "b", "capitulo_slug": "volumenes", "seccion_slug": "bind", "texto": "y" * 100},
    ]
    f = seleccionar_fragmento(chunks, capitulo="volumenes")
    assert f["capitulo_slug"] == "volumenes"
    print("OK seleccionar_fragmento")


def prueba_origen_apuntes():
    frag = {
        "modulo": "docs_docker",
        "tema": "docker",
        "capitulo": "volumenes",
        "capitulo_slug": "volumenes",
        "capitulo_titulo": "Volumenes",
        "seccion": "Volúmenes persistentes",
        "pagina": 37,
        "archivo": "apuntes.pdf",
        "fuente": "documentacion/docker/volumenes/apuntes.pdf",
        "fragmento_id": 142,
        "id": "docs_docker-f142",
    }
    origen = construir_origen_apuntes(frag, {"titulo_visible": "Docker", "modulo": "docs_docker"})
    assert origen["pagina"] == 37
    assert origen["fragmento_id"] == 142
    print("OK construir_origen_apuntes")


def main() -> int:
    prueba_fragmentar()
    prueba_nombre_modulo()
    prueba_metadatos_ruta()
    prueba_seleccion_fragmento()
    prueba_origen_apuntes()
    print("Todas las pruebas del indexador pasaron.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
