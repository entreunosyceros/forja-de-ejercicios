#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Utilidades compartidas por los scripts de la forja (slug, identificadores).

Centraliza pequeñas funciones que estaban repetidas en varios módulos para
evitar divergencias de comportamiento entre capas.
"""

from __future__ import annotations

import re
import uuid


def slug(texto: str, defecto: str = "") -> str:
    """Convierte un texto en un identificador seguro (minúsculas, guiones bajos).

    ``defecto`` se devuelve cuando el resultado queda vacío (p. ej. ``"general"``).
    """
    base = re.sub(r"[^a-z0-9_]+", "_", (texto or "").lower()).strip("_")
    return base or defecto


def generar_identificador(longitud: int = 8) -> str:
    """Identificador corto y único basado en UUID4."""
    return str(uuid.uuid4())[:longitud]
