#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Migra criterios del banco: flags → banderas (una sola vez).

Uso:
  python3 herramientas/migrar_banderas_banco.py
  python3 herramientas/migrar_banderas_banco.py --dry-run
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

from criterios import normalizar_banderas_en_criterio  # noqa: E402


def migrar_fichero(ruta: Path, dry_run: bool) -> bool:
    try:
        datos = json.loads(ruta.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as ex:
        print(f"  omitido {ruta.relative_to(RAIZ)}: {ex}")
        return False
    criterios = datos.get("criterios")
    if not isinstance(criterios, list):
        return False
    cambiado = False
    for c in criterios:
        if not isinstance(c, dict):
            continue
        antes = dict(c)
        normalizar_banderas_en_criterio(c)
        if c != antes:
            cambiado = True
    if not cambiado:
        return False
    if dry_run:
        print(f"  [dry-run] {ruta.relative_to(RAIZ)}")
        return True
    ruta.write_text(
        json.dumps(datos, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"  migrado {ruta.relative_to(RAIZ)}")
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    raiz_banco = RAIZ / "banco"
    if not raiz_banco.is_dir():
        print("No hay carpeta banco/")
        return 0
    total = 0
    for ruta in sorted(raiz_banco.rglob("*.json")):
        if ruta.name == "catalogo.json":
            continue
        if migrar_fichero(ruta, args.dry_run):
            total += 1
    print(f"Listo: {total} fichero(s) con flags→banderas.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
