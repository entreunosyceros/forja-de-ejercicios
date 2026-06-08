# Desarrollado por entreunosyceros - 2026
"""Convierte texto con markdown a texto plano (comandos/código listos para comparar)."""

from __future__ import annotations

import re


def sin_markdown(texto: str) -> str:
    """Elimina marcado markdown; conserva el contenido útil (código, listas, párrafos)."""
    if not texto:
        return ""
    t = texto.replace("\r\n", "\n").replace("\r", "\n").strip()

    def _bloque_codigo(match: re.Match[str]) -> str:
        cuerpo = match.group(1).strip()
        return cuerpo + "\n" if cuerpo else ""

    t = re.sub(r"```[\w.-]*\n?([\s\S]*?)```", _bloque_codigo, t)
    t = re.sub(r"`([^`\n]+)`", r"\1", t)
    t = re.sub(r"^#{1,6}\s+", "", t, flags=re.MULTILINE)
    t = re.sub(r"\*\*([^*\n]+)\*\*", r"\1", t)
    t = re.sub(r"\*([^*\n]+)\*", r"\1", t)
    t = re.sub(r"__([^_\n]+)__", r"\1", t)
    t = re.sub(r"_([^_\n]+)_", r"\1", t)
    t = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", t)
    t = re.sub(r"^>\s?", "", t, flags=re.MULTILINE)
    t = re.sub(r"^[-*]{3,}\s*$", "", t, flags=re.MULTILINE)
    t = re.sub(r"^[\*\-\+]\s+", "", t, flags=re.MULTILINE)
    t = re.sub(r"^\d+\.\s+", "", t, flags=re.MULTILINE)
    # Líneas vacías repetidas
    t = re.sub(r"\n{3,}", "\n\n", t)
    return t.strip()
