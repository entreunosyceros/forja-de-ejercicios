#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Pruebas de banco_loader y cola de aprobación."""

import json
import sys
import tempfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import banco_loader
import evaluador
import modelo_ejercicio


def probar_ciclo_pendiente_aprobado():
    with tempfile.TemporaryDirectory() as tmp:
        banco = Path(tmp) / "banco"
        aprob = banco / "aprobados"
        pend = banco / "pendientes"
        aprob.mkdir(parents=True)
        pend.mkdir(parents=True)

        orig_banco = banco_loader.CARPETA_BANCO
        orig_ap = banco_loader.APROBADOS
        orig_pe = banco_loader.PENDIENTES
        orig_cat = banco_loader.CATALOGO
        try:
            banco_loader.CARPETA_BANCO = banco
            banco_loader.APROBADOS = aprob
            banco_loader.PENDIENTES = pend
            banco_loader.CATALOGO = banco / "catalogo.json"

            escenario = {
                "id": "test99",
                "modulo": "docs_test",
                "titulo": "Test",
                "enunciado": "Haz docker run",
                "criterios": modelo_ejercicio.construir_criterios_desde_palabras_clave(
                    ["docker run", "nginx"]
                ),
                "solucion_referencia": "docker run nginx",
            }
            banco_loader.guardar_pendiente(escenario)
            assert (pend / "test99.json").is_file()

            dest = banco_loader.aprobar_pendiente("test99")
            assert dest.is_file()
            assert not (pend / "test99.json").exists()

            cargado = banco_loader.cargar_ejercicio_aprobado(dest)
            r = evaluador.evaluar(cargado, "docker run nginx")
            assert r["aprobado"]
        finally:
            banco_loader.CARPETA_BANCO = orig_banco
            banco_loader.APROBADOS = orig_ap
            banco_loader.PENDIENTES = orig_pe
            banco_loader.CATALOGO = orig_cat


def probar_modulo_sin_campo_json():
    """Ejercicios importados sin 'modulo' o con banco_aprobados deben registrarse bien."""
    with tempfile.TemporaryDirectory() as tmp:
        banco = Path(tmp) / "banco"
        aprob = banco / "aprobados" / "docker"
        aprob.mkdir(parents=True)

        orig_banco = banco_loader.CARPETA_BANCO
        orig_ap = banco_loader.APROBADOS
        orig_pe = banco_loader.PENDIENTES
        orig_cat = banco_loader.CATALOGO
        try:
            banco_loader.CARPETA_BANCO = banco
            banco_loader.APROBADOS = banco / "aprobados"
            banco_loader.PENDIENTES = banco / "pendientes"
            banco_loader.CATALOGO = banco / "catalogo.json"

            ruta = aprob / "sin-modulo.json"
            ruta.write_text(
                json.dumps({
                    "id": "sin-modulo",
                    "modulo": "banco_aprobados",
                    "titulo": "Docker sin modulo",
                    "enunciado": "docker ps",
                    "criterios": modelo_ejercicio.construir_criterios_desde_palabras_clave(
                        ["docker ps", "nginx"]
                    ),
                    "solucion_referencia": "docker ps",
                }),
                encoding="utf-8",
            )

            cat = banco_loader.reconstruir_catalogo()
            modulo_cat = cat["ejercicios"][0]["modulo"]
            assert modulo_cat == "banco_docker", modulo_cat

            gens: dict = {}
            pistas: dict = {}
            mods = banco_loader.registrar_en_generadores(gens, pistas)
            assert "banco_docker" in mods
            assert "banco_aprobados" not in mods
            escenario = gens["banco_docker"]()
            assert escenario["id"] == "sin-modulo"
        finally:
            banco_loader.CARPETA_BANCO = orig_banco
            banco_loader.APROBADOS = orig_ap
            banco_loader.PENDIENTES = orig_pe
            banco_loader.CATALOGO = orig_cat


def probar_registro_generador():
    ejemplo = RAIZ / "banco" / "aprobados" / "docker" / "ejemplo.json"
    if not ejemplo.is_file():
        return
    gens = {}
    pistas = {}
    mods = banco_loader.registrar_en_generadores(gens, pistas)
    assert "banco_docker" in mods or "banco_docker" in gens


if __name__ == "__main__":
    probar_ciclo_pendiente_aprobado()
    probar_modulo_sin_campo_json()
    probar_registro_generador()
    print("OK banco_loader")
