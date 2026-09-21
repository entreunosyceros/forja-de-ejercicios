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


def generar_identificador(longitud: int = 8) -> str:
    """Identificador corto y único basado en UUID4."""
    return str(uuid.uuid4())[:longitud]


def escribir_json_atomico(ruta: Path | str, datos: object, *, indent: int = 2) -> None:
    """Escribe JSON vía fichero temporal + ``os.replace`` (evita truncados)."""
    destino = Path(ruta)
    destino.parent.mkdir(parents=True, exist_ok=True)
    texto = json.dumps(datos, ensure_ascii=False, indent=indent) + "\n"
    fd, tmp_nombre = tempfile.mkstemp(
        prefix=".tmp-",
        suffix=".json",
        dir=str(destino.parent),
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
