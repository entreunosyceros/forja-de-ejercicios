#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Tipo de materia para apuntes PDF (informática, idiomas, general)."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

RAIZ = Path(__file__).resolve().parent
RUTA_VOCABULARIO = RAIZ / "vocabulario_claves.json"
FICHEROS_TIPO = (".forja-tipo", "tipo_materia.txt")

TIPOS_VALIDOS = frozenset({"informatica", "idiomas", "general"})
TIPO_POR_DEFECTO = "general"

_CACHE_VOCAB: dict[str, Any] | None = None


def _cargar_vocabulario() -> dict[str, Any]:
    global _CACHE_VOCAB
    if _CACHE_VOCAB is not None:
        return _CACHE_VOCAB
    if not RUTA_VOCABULARIO.is_file():
        _CACHE_VOCAB = {}
        return _CACHE_VOCAB
    try:
        _CACHE_VOCAB = json.loads(RUTA_VOCABULARIO.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        _CACHE_VOCAB = {}
    return _CACHE_VOCAB


def _slug(texto: str) -> str:
    return re.sub(r"[^a-z0-9_]+", "_", (texto or "").lower()).strip("_")


def _config_tipo(tipo: str) -> dict[str, Any]:
    vocab = _cargar_vocabulario()
    tipos = vocab.get("tipos_materia") or {}
    cfg = tipos.get(tipo) or {}
    if tipo == TIPO_POR_DEFECTO and not cfg:
        return {"default": True, "requiere_clave_tecnica": False, "min_longitud_clave": 4}
    return cfg if isinstance(cfg, dict) else {}


def leer_tipo_desde_carpeta(carpeta: Path | str) -> str | None:
    """Lee documentacion/<tema>/.forja-tipo o tipo_materia.txt (informatica|idiomas|general)."""
    base = Path(carpeta)
    if not base.is_dir():
        return None
    for nombre in FICHEROS_TIPO:
        fichero = base / nombre
        if not fichero.is_file():
            continue
        texto = fichero.read_text(encoding="utf-8").strip().lower()
        if texto in TIPOS_VALIDOS:
            return texto
    return None


def resolver_tipo_materia(
    modulo: str | None,
    *,
    tema_slug: str | None = None,
    tipo_explicito: str | None = None,
    carpeta_documentacion: Path | str | None = None,
) -> str:
    """Determina el tipo de materia para un módulo docs_*."""
    if tipo_explicito:
        t = tipo_explicito.strip().lower()
        if t in TIPOS_VALIDOS:
            return t

    if carpeta_documentacion:
        desde_fichero = leer_tipo_desde_carpeta(carpeta_documentacion)
        if desde_fichero:
            return desde_fichero

    vocab = _cargar_vocabulario()
    if modulo and modulo in vocab:
        cfg_mod = vocab.get(modulo) or {}
        if isinstance(cfg_mod, dict):
            t = str(cfg_mod.get("tipo_materia", "")).strip().lower()
            if t in TIPOS_VALIDOS:
                return t

    slug = tema_slug or ""
    if not slug and modulo and modulo.startswith("docs_"):
        slug = modulo[5:]

    tipos = vocab.get("tipos_materia") or {}
    if isinstance(tipos, dict):
        for nombre_tipo, cfg in tipos.items():
            if nombre_tipo not in TIPOS_VALIDOS or not isinstance(cfg, dict):
                continue
            modulos = {str(m).lower() for m in (cfg.get("modulos") or [])}
            if modulo and modulo.lower() in modulos:
                return nombre_tipo
            carpetas = {_slug(str(c)) for c in (cfg.get("carpetas") or [])}
            if slug and _slug(slug) in carpetas:
                return nombre_tipo

    return TIPO_POR_DEFECTO


def requiere_clave_tecnica(tipo: str) -> bool:
    cfg = _config_tipo(tipo)
    return bool(cfg.get("requiere_clave_tecnica", tipo == "informatica"))


def min_longitud_clave(tipo: str) -> int:
    cfg = _config_tipo(tipo)
    try:
        return max(2, int(cfg.get("min_longitud_clave", 2)))
    except (TypeError, ValueError):
        return 2


def prohibidas_extra_tipo(tipo: str) -> frozenset[str]:
    cfg = _config_tipo(tipo)
    items = cfg.get("prohibidas") or cfg.get("prohibidas_extra") or []
    return frozenset(str(x).lower() for x in items if str(x).strip())


def es_clave_valida(palabra: str, tipo: str, *, en_fragmento: bool = False) -> bool:
    """Comprueba si una palabra clave es aceptable según el tipo de materia."""
    p = palabra.strip()
    if not p:
        return False
    min_len = min_longitud_clave(tipo)
    if len(p) < min_len:
        return False

    if requiere_clave_tecnica(tipo):
        if len(p) >= 6:
            return True
        if re.search(r"[-/.#\\]|^\d|:\d", p):
            return True
        if " " in p and len(p) >= 4:
            return True
        # Nombres cortos de herramientas del fragmento (nginx, git, sql…)
        return en_fragmento and len(p) >= 3

    if tipo == "idiomas":
        if len(p) >= 4 or " " in p:
            return True
        return en_fragmento and len(p) >= min_len

    # general
    if len(p) >= 5 or " " in p:
        return True
    return en_fragmento and len(p) >= min_len


def instrucciones_prompt_tipo(tipo: str) -> str:
    if tipo == "informatica":
        return (
            "Tipo de materia: INFORMÁTICA / sistemas.\n"
            "El alumno debe escribir comandos de terminal, código o pasos técnicos verificables.\n"
            "Las palabras_clave deben ser comandos, flags, nombres de herramientas o sintaxis del fragmento."
        )
    if tipo == "idiomas":
        return (
            "Tipo de materia: IDIOMAS.\n"
            "El alumno practica vocabulario, gramática o expresiones del fragmento.\n"
            "Pide traducir, completar frases, aplicar una regla o usar expresiones del texto.\n"
            "Las palabras_clave deben ser términos, estructuras o expresiones del fragmento "
            "(en el idioma del material o en español si el apunte mezcla idiomas)."
        )
    return (
        "Tipo de materia: GENERAL (cualquier asignatura).\n"
        "El alumno responde con texto basado solo en el fragmento: definiciones, pasos, causas, "
        "consecuencias, ejemplos o resumen aplicado.\n"
        "Las palabras_clave deben ser conceptos concretos del fragmento que deban reflejarse en la respuesta."
    )


def formato_respuesta_prompt(tipo: str) -> str:
    if tipo == "informatica":
        return "comandos o código en un cuadro de texto"
    if tipo == "idiomas":
        return "frases, traducciones o explicaciones breves en el cuadro de texto"
    return "una respuesta escrita en el cuadro de texto"


def texto_enunciado_alumno(tipo: str, claves: list[str]) -> str:
    lista = ", ".join(claves[:6])
    if tipo == "informatica":
        return (
            f"Escribe los comandos o el código en el cuadro de respuesta. "
            f"Se comprobarán conceptos del material: {lista}."
        )
    if tipo == "idiomas":
        return (
            f"Escribe tu respuesta en el cuadro de texto (frases, traducción o explicación según el enunciado). "
            f"Debe reflejar el material: {lista}."
        )
    return (
        f"Escribe tu respuesta en el cuadro de texto basándote en el fragmento de apuntes. "
        f"Se tendrán en cuenta estos conceptos: {lista}."
    )
