#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Motor de plantillas de ejercicio (JSON → escenario).

Las plantillas viven en ``plantillas/*.json``. Un profesor puede añadir módulos
sin tocar Python: solo JSON con variables y textos.
"""

from __future__ import annotations

import copy
import json
import random
import re
from pathlib import Path
from typing import Any

import comun

RAIZ = Path(__file__).resolve().parent
CARPETA_PLANTILLAS = RAIZ / "plantillas"

_CACHE: dict[str, dict[str, Any]] | None = None
_PLACEHOLDER = re.compile(r"\{([a-zA-Z_][a-zA-Z0-9_]*)\}")


def _cargar_plantillas() -> dict[str, dict[str, Any]]:
    global _CACHE
    if _CACHE is not None:
        return _CACHE
    cargadas: dict[str, dict[str, Any]] = {}
    if CARPETA_PLANTILLAS.is_dir():
        for ruta in sorted(CARPETA_PLANTILLAS.glob("*.json")):
            try:
                datos = json.loads(ruta.read_text(encoding="utf-8"))
            except (json.JSONDecodeError, OSError):
                continue
            modulo = str(datos.get("modulo") or ruta.stem).strip()
            if modulo:
                cargadas[modulo] = datos
    _CACHE = cargadas
    return cargadas


def modulos_plantilla() -> tuple[str, ...]:
    return tuple(sorted(_cargar_plantillas().keys()))


def _elegir_variables(plantilla: dict[str, Any], rng: random.Random) -> dict[str, Any]:
    variables_raw = plantilla.get("variables") or {}
    elegidas: dict[str, Any] = {}
    for nombre, valor in variables_raw.items():
        if isinstance(valor, list) and valor:
            elegidas[nombre] = rng.choice(valor)
        else:
            elegidas[nombre] = valor
    # Evitar VLANs idénticas en plantilla redes
    if elegidas.get("v1") == elegidas.get("v2") and isinstance(variables_raw.get("v2"), list):
        opciones = [x for x in variables_raw["v2"] if x != elegidas.get("v1")]
        if opciones:
            elegidas["v2"] = rng.choice(opciones)
    return elegidas


def _sustituir(texto: str, variables: dict[str, Any], *, escapar_regex: bool = False) -> str:
    def repl(m: re.Match[str]) -> str:
        clave = m.group(1)
        if clave not in variables:
            return m.group(0)
        valor = str(variables[clave])
        return re.escape(valor) if escapar_regex else valor

    return _PLACEHOLDER.sub(repl, texto)


def _sustituir_estructura(nodo: Any, variables: dict[str, Any], *, en_patron: bool = False) -> Any:
    if isinstance(nodo, str):
        return _sustituir(nodo, variables, escapar_regex=en_patron)
    if isinstance(nodo, list):
        return [_sustituir_estructura(x, variables, en_patron=en_patron) for x in nodo]
    if isinstance(nodo, dict):
        out: dict[str, Any] = {}
        for k, v in nodo.items():
            if k == "patron":
                out[k] = _sustituir_estructura(v, variables, en_patron=True)
            else:
                out[k] = _sustituir_estructura(v, variables, en_patron=en_patron)
        return out
    return nodo


def generar_desde_plantilla(
    modulo: str,
    *,
    rng: random.Random | None = None,
) -> dict[str, Any]:
    plantillas = _cargar_plantillas()
    if modulo not in plantillas:
        raise ValueError(f"No hay plantilla para el módulo: {modulo}")
    plantilla = plantillas[modulo]
    rng = rng or random.Random()
    variables = _elegir_variables(plantilla, rng)

    escenario = {
        "version": 1,
        "id": comun.generar_identificador(),
        "modulo": modulo,
        "titulo": _sustituir(str(plantilla.get("titulo", modulo)), variables),
        "enunciado": _sustituir(str(plantilla.get("enunciado", "")), variables),
        "parametros": dict(variables),
        "criterios": _sustituir_estructura(
            copy.deepcopy(plantilla.get("criterios") or []),
            variables,
        ),
        "solucion_referencia": _sustituir(
            str(plantilla.get("solucion_referencia", "")),
            variables,
        ),
    }
    return escenario


def registrar_en_generadores(
    generadores: dict[str, Any],
    pistas_modulo: dict[str, str],
) -> None:
    """Registra cada plantilla JSON como generador callable."""
    for modulo, plantilla in _cargar_plantillas().items():
        if modulo in generadores:
            continue  # preferir función Python explícita si existe
        pista = str(plantilla.get("pista_modulo") or "").strip()
        if pista:
            pistas_modulo.setdefault(modulo, pista)

        def _factory(m: str = modulo) -> dict[str, Any]:
            return generar_desde_plantilla(m)

        generadores[modulo] = _factory
