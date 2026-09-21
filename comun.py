#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Utilidades compartidas por los scripts de la forja (slug, identificadores).

Centraliza pequeñas funciones que estaban repetidas en varios módulos para
evitar divergencias de comportamiento entre capas.
"""

from __future__ import annotations

import json
import os
import re
import tempfile
import uuid
from pathlib import Path


def slug(texto: str, defecto: str = "") -> str:
    """Convierte un texto en un identificador seguro (minúsculas, guiones bajos).

    ``defecto`` se devuelve cuando el resultado queda vacío (p. ej. ``"general"``).
    """
    base = re.sub(r"[^a-z0-9_]+", "_", (texto or "").lower()).strip("_")
    return base or defecto


def generar_identificador(longitud: int | None = None) -> str:
    """Identificador único basado en UUID4 (completo por defecto).

    ``longitud`` solo se usa si se pide un prefijo corto de forma explícita
    (p. ej. nombres temporales); el banco y los escenarios usan UUID entero.
    """
    completo = str(uuid.uuid4())
    if longitud is None:
        return completo
    return completo[: max(8, longitud)]


def escribir_json_atomico(ruta: Path | str, datos: object, *, indent: int = 2) -> None:
    """Escribe JSON vía fichero temporal + ``os.replace`` (evita truncados).

    Los temporales se crean junto al destino; si existe ``datos/tmp`` (o ``TMPDIR``),
    se usa esa carpeta cuando el destino no es escribible de forma segura.
    """
    destino = Path(ruta)
    destino.parent.mkdir(parents=True, exist_ok=True)
    texto = json.dumps(datos, ensure_ascii=False, indent=indent) + "\n"
    dir_tmp = _directorio_temporales(destino.parent)
    fd, tmp_nombre = tempfile.mkstemp(
        prefix=".tmp-",
        suffix=".json",
        dir=str(dir_tmp),
    )
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as tmp:
            tmp.write(texto)
            tmp.flush()
            os.fsync(tmp.fileno())
        os.replace(tmp_nombre, destino)
    except Exception:
        try:
            os.unlink(tmp_nombre)
        except OSError:
            pass
        raise


def _directorio_temporales(preferido: Path) -> Path:
    """Prefiere ``TMPDIR`` / ``datos/tmp`` del proyecto; cae al directorio del destino."""
    candidatos: list[Path] = []
    env_tmp = os.environ.get("TMPDIR") or os.environ.get("FORJAEXAMENES_TMP")
    if env_tmp:
        candidatos.append(Path(env_tmp))
    raiz = Path(__file__).resolve().parent
    candidatos.append(raiz / "datos" / "tmp")
    candidatos.append(preferido)
    for cand in candidatos:
        try:
            cand.mkdir(parents=True, exist_ok=True)
            if os.access(cand, os.W_OK):
                return cand
        except OSError:
            continue
    return preferido
