#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Módulos docs_*: PDF → fragmentos → Gemini (un fragmento por ejercicio)."""

from __future__ import annotations

import json
import os
import random
from pathlib import Path
from typing import Callable

import comun
import generador_gemini

RAIZ = Path(__file__).resolve().parent
CARPETA_INDICE = RAIZ / "indice"

MODULOS_DOCS: tuple[str, ...] = ()


def _generar_identificador() -> str:
    return comun.generar_identificador()


def _slug(texto: str) -> str:
    return comun.slug(texto)


def listar_modulos_indexados(carpeta_indice: Path = CARPETA_INDICE) -> list[str]:
    if not carpeta_indice.is_dir():
        return []
    return sorted(p.stem for p in carpeta_indice.glob("docs_*.json") if p.is_file())


def cargar_indice(modulo: str, carpeta_indice: Path = CARPETA_INDICE) -> dict:
    ruta = carpeta_indice / f"{modulo}.json"
    if not ruta.is_file():
        raise FileNotFoundError(
            f"No hay índice para '{modulo}'. Coloca PDFs en documentacion/<tema>/ "
            f"y ejecuta: python3 indexador_docs.py"
        )
    with ruta.open(encoding="utf-8") as archivo:
        return json.load(archivo)


def seleccionar_fragmento(
    chunks: list[dict],
    capitulo: str | None = None,
    seccion: str | None = None,
) -> dict:
    candidatos = chunks
    if capitulo:
        cap_slug = _slug(capitulo)
        candidatos = [
            c for c in candidatos
            if _slug(c.get("capitulo_slug", c.get("capitulo", ""))) == cap_slug
            or _slug(c.get("capitulo", "")) == cap_slug
        ]
    if seccion:
        sec_slug = _slug(seccion)
        candidatos = [
            c for c in candidatos
            if _slug(c.get("seccion_slug", c.get("seccion", ""))) == sec_slug
            or sec_slug in _slug(c.get("seccion", ""))
        ]
    if not candidatos:
        raise ValueError(
            f"No hay fragmentos para capitulo={capitulo!r} seccion={seccion!r}. "
            "Reindexa o elige otra sección."
        )
    return random.choice(candidatos)


def construir_origen_apuntes(fragmento: dict, indice: dict) -> dict:
    """Metadatos de trazabilidad: repaso al fallar y export JSON."""
    return {
        "modulo": fragmento.get("modulo", indice.get("modulo")),
        "tema": fragmento.get("tema", indice.get("tema")),
        "titulo_apuntes": indice.get("titulo_visible", ""),
        "capitulo": fragmento.get("capitulo", ""),
        "capitulo_slug": fragmento.get("capitulo_slug", ""),
        "capitulo_titulo": fragmento.get("capitulo_titulo", fragmento.get("capitulo", "")),
        "seccion": fragmento.get("seccion", ""),
        "seccion_slug": fragmento.get("seccion_slug", ""),
        "pagina": fragmento.get("pagina"),
        "pagina_fin": fragmento.get("pagina_fin", fragmento.get("pagina")),
        "archivo": fragmento.get("archivo", ""),
        "fuente": fragmento.get("fuente", ""),
        "fragmento_id": fragmento.get("fragmento_id"),
        "id_fragmento": fragmento.get("id", ""),
        "url_ver_pdf": _url_ver_pdf(fragmento.get("fuente", ""), fragmento.get("pagina")),
    }


def _url_ver_pdf(fuente: str, pagina: int | None) -> str:
    if not fuente:
        return ""
    from urllib.parse import quote
    q = quote(fuente, safe="")
    url = f"/ejercicio/apuntes/ver?fuente={q}"
    if pagina is not None and pagina > 0:
        url += f"&pagina={pagina}"
    return url


def generar_ejercicio_documentacion(
    modulo: str,
    nivel: int = 2,
    capitulo: str | None = None,
    seccion: str | None = None,
) -> dict:
    solo_aprobados = os.environ.get("FORJAEXAMENES_GEMINI_SOLO_APROBADOS", "").lower() in (
        "1", "true", "yes"
    )
    if solo_aprobados:
        import banco_loader
        return banco_loader.generar_desde_banco_aprobado(modulo)

    indice = cargar_indice(modulo)
    chunks = indice.get("chunks") or []
    if not chunks:
        raise RuntimeError(f"El índice {modulo} no tiene fragmentos.")

    fragmento = seleccionar_fragmento(chunks, capitulo=capitulo, seccion=seccion)
    origen = construir_origen_apuntes(fragmento, indice)

    escenario = generador_gemini.generar_desde_fragmento(
        fragmento=fragmento,
        modulo=modulo,
        titulo_coleccion=indice.get("titulo_visible", modulo),
        nivel=nivel,
    )
    escenario["id"] = _generar_identificador()
    escenario["parametros"] = {
        **(escenario.get("parametros") or {}),
        "coleccion": indice.get("carpeta", ""),
        "titulo_coleccion": indice.get("titulo_visible", ""),
        "origen": origen,
        "generado_desde_apuntes": True,
    }

    if os.environ.get("FORJAEXAMENES_GEMINI_GUARDAR_PENDIENTES", "").lower() in (
        "1", "true", "yes"
    ):
        import banco_loader
        banco_loader.guardar_pendiente(escenario, propuesta=escenario.get("parametros", {}).get(
            "propuesta_gemini"
        ))

    return escenario


def _fabrica_generador(modulo: str) -> Callable[[], dict]:
    def generar() -> dict:
        return generar_ejercicio_documentacion(modulo)

    return generar


def registrar_modulos_documentacion(
    generadores: dict[str, Callable[[], dict]],
    pistas_modulo: dict[str, str],
    carpeta_indice: Path = CARPETA_INDICE,
) -> tuple[str, ...]:
    global MODULOS_DOCS
    modulos = listar_modulos_indexados(carpeta_indice)
    for modulo in modulos:
        generadores[modulo] = _fabrica_generador(modulo)
        pistas_modulo[modulo] = (
            "Repasa el fragmento de tus apuntes indicado al final si suspendes; "
            "solo usa lo que el profesor explicó en ese material."
        )
    MODULOS_DOCS = tuple(modulos)
    return MODULOS_DOCS
