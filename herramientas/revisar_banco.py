#!/usr/bin/env python3
"""CLI: revisar propuestas en banco/pendientes/."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import banco_loader


def cmd_listar(_: argparse.Namespace) -> int:
    rutas = banco_loader.listar_pendientes()
    if not rutas:
        print("No hay propuestas pendientes.")
        return 0
    for ruta in rutas:
        datos = json.loads(ruta.read_text(encoding="utf-8"))
        prev = datos.get("escenario_preview") or {}
        print(f"{datos.get('id', ruta.stem)}\t{datos.get('modulo', prev.get('modulo', '?'))}\t{prev.get('titulo', '')[:50]}")
    return 0


def cmd_mostrar(args: argparse.Namespace) -> int:
    datos = banco_loader.cargar_pendiente(args.id)
    print(json.dumps(datos, ensure_ascii=False, indent=2))
    return 0


def cmd_aprobar(args: argparse.Namespace) -> int:
    destino = banco_loader.aprobar_pendiente(args.id, subcarpeta=args.carpeta)
    print(f"Aprobado → {destino}")
    return 0


def cmd_rechazar(args: argparse.Namespace) -> int:
    banco_loader.rechazar_pendiente(args.id)
    print(f"Rechazado: {args.id}")
    return 0


def cmd_reindexar(_: argparse.Namespace) -> int:
    cat = banco_loader.reconstruir_catalogo()
    print(f"Catálogo actualizado: {len(cat.get('ejercicios', []))} ejercicio(s)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Revisar banco de ejercicios")
    sub = parser.add_subparsers(dest="comando", required=True)

    p = sub.add_parser("listar", help="Listar pendientes")
    p.set_defaults(func=cmd_listar)

    p = sub.add_parser("mostrar", help="Ver JSON de un pendiente")
    p.add_argument("id")
    p.set_defaults(func=cmd_mostrar)

    p = sub.add_parser("aprobar", help="Mover pendiente a aprobados")
    p.add_argument("id")
    p.add_argument("--carpeta", help="Subcarpeta en banco/aprobados/")
    p.set_defaults(func=cmd_aprobar)

    p = sub.add_parser("rechazar", help="Eliminar pendiente")
    p.add_argument("id")
    p.set_defaults(func=cmd_rechazar)

    p = sub.add_parser("reindexar", help="Regenerar catalogo.json")
    p.set_defaults(func=cmd_reindexar)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
