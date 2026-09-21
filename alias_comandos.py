#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Sinónimos de comandos técnicos para criterios contiene_todos / contiene_alguno."""

from __future__ import annotations

import json
import re
import unicodedata
from pathlib import Path
from typing import Any

RAIZ = Path(__file__).resolve().parent
RUTA_VOCABULARIO = RAIZ / "vocabulario_claves.json"

_CACHE: dict[str, list[str]] | None = None


def _cargar_vocabulario() -> dict[str, Any]:
    if not RUTA_VOCABULARIO.is_file():
        return {}
    try:
        return json.loads(RUTA_VOCABULARIO.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return {}


def _normalizar_grupos(raw: dict[str, Any]) -> dict[str, list[str]]:
    grupos: dict[str, list[str]] = {}
    for clave, valor in raw.items():
        if not isinstance(valor, list):
            continue
        vistos: list[str] = []
        for item in [clave, *valor]:
            texto = str(item).strip()
            if not texto:
                continue
            low = texto.lower()
            if low not in {v.lower() for v in vistos}:
                vistos.append(texto)
        if vistos:
            canon = vistos[0]
            grupos[canon.lower()] = vistos
    return grupos


def cargar_grupos_alias() -> dict[str, list[str]]:
    global _CACHE
    if _CACHE is not None:
        return _CACHE
    vocab = _cargar_vocabulario()
    raw = vocab.get("alias_comandos") or {}
    if not isinstance(raw, dict):
        _CACHE = {}
        return _CACHE
    _CACHE = _normalizar_grupos(raw)
    return _CACHE


def normalizar_texto(texto: str) -> str:
    """Minúsculas, sin acentos y espacios colapsados (para comparar términos)."""
    if not texto:
        return ""
    nfkd = unicodedata.normalize("NFKD", texto)
    sin_acentos = "".join(c for c in nfkd if not unicodedata.combining(c))
    return re.sub(r"\s+", " ", sin_acentos.lower()).strip()


def variantes_termino(termino: str) -> list[str]:
    """Devuelve el término y sus alias conocidos (sin duplicados, orden estable)."""
    texto = (termino or "").strip()
    if not texto:
        return []
    grupos = cargar_grupos_alias()
    low = texto.lower()
    if low in grupos:
        return list(grupos[low])
    for grupo in grupos.values():
        if low in {g.lower() for g in grupo}:
            return list(grupo)
    return [texto]


def contiene_termino_flexible(respuesta: str, termino: str) -> bool:
    """Busca el término (o alias) con límites de palabra; evita falsos positivos tipo «cat» en «concatenar»."""
    texto = normalizar_texto(respuesta or "")
    if not texto:
        return False
    for variante in variantes_termino(termino):
        patron = normalizar_texto(variante)
        if not patron:
            continue
        if re.search(rf"(?<!\w){re.escape(patron)}(?!\w)", texto):
            return True
    return False
