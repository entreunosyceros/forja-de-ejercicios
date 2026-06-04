#!/usr/bin/env python3
"""Pruebas de conversión markdown → texto plano."""

import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

from util_texto import sin_markdown  # noqa: E402


def test_bloque_codigo():
    entrada = "Solución:\n```bash\ndocker ps\ndocker logs web\n```"
    salida = sin_markdown(entrada)
    assert "docker ps" in salida
    assert "```" not in salida


def test_negrita_y_lista():
    entrada = "**Paso 1**\n- `kubectl get pods`\n- `kubectl describe pod x`"
    salida = sin_markdown(entrada)
    assert "Paso 1" in salida
    assert "kubectl get pods" in salida
    assert "**" not in salida
    assert "`" not in salida


def main() -> int:
    test_bloque_codigo()
    test_negrita_y_lista()
    print("OK pruebas_texto_plano")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
