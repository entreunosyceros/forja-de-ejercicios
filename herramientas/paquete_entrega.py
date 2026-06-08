#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Genera un paquete de entrega (carpeta) para revisión offline por el profesor."""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
HERRAMIENTAS = Path(__file__).resolve().parent
sys.path.insert(0, str(HERRAMIENTAS))

from revision_profesor import construir_revision  # noqa: E402


def _html_revision(datos: dict) -> str:
    filas = datos.get("filas_criterios") or []
    trs = []
    for f in filas:
        alt = f.get("prueba_alternativa") or {}
        alt_txt = "—"
        if alt.get("aplica"):
            ok = "✅" if alt.get("cumplido") else "❌"
            alt_txt = f"{ok} <code>{alt.get('respuesta', '')[:120]}</code>"
        ref = f.get("prueba_referencia") or {}
        ref_ok = "✅" if ref.get("cumplido") else "❌"
        fail = f.get("prueba_insuficiente") or {}
        fail_ok = "✅" if fail.get("cumplido") else "❌"
        trs.append(
            f"<tr><td>{f.get('indice')}</td><td>{f.get('tipo')}</td>"
            f"<td>{f.get('exigencia')}</td><td>{f.get('peso')}</td>"
            f"<td>{f.get('aliases') or '—'}</td>"
            f"<td>{ref_ok}</td><td>{alt_txt}</td><td>{fail_ok}</td></tr>"
        )
    comp = datos.get("comparativa_propuesta") or {}
    prop = datos.get("propuesta_gemini") or {}
    esc = datos.get("escenario") or {}
    return f"""<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8"/>
<title>Revisión {datos.get('id', '')}</title>
<style>
body {{ font-family: system-ui, sans-serif; margin: 1.5rem; max-width: 1100px; }}
table {{ border-collapse: collapse; width: 100%; margin: 1rem 0; }}
th, td {{ border: 1px solid #ccc; padding: 0.4rem 0.6rem; vertical-align: top; }}
th {{ background: #f0f0f0; }}
pre {{ background: #f8f8f8; padding: 0.75rem; overflow-x: auto; }}
</style></head><body>
<h1>Paquete de revisión — {datos.get('id', '')}</h1>
<p><strong>Módulo:</strong> {datos.get('modulo', '')} · <strong>Creado:</strong> {datos.get('creado', '')}</p>
<h2>Propuesta Gemini vs escenario</h2>
<ul>
<li><strong>Pregunta IA:</strong> {prop.get('pregunta', '')}</li>
<li><strong>Palabras clave IA:</strong> {comp.get('palabras_gemini', '')}</li>
<li><strong>Palabras en criterios:</strong> {comp.get('palabras_criterios', '')}</li>
</ul>
<h2>Enunciado</h2>
<pre>{esc.get('enunciado', '')}</pre>
<h2>Solución de referencia</h2>
<pre>{esc.get('solucion_referencia', '')}</pre>
<h2>Tabla comparativa de criterios</h2>
<table>
<thead><tr><th>#</th><th>Tipo</th><th>Exigencia</th><th>Peso</th><th>Alias</th>
<th>Ref. OK</th><th>Alternativa</th><th>Insuf. OK?</th></tr></thead>
<tbody>{''.join(trs)}</tbody>
</table>
<p><em>Ref. OK = solución de referencia; Insuf. OK? = debe fallar (❌ esperado).</em></p>
</body></html>"""


def generar_paquete(eid: str, destino: Path) -> Path:
    datos = construir_revision(eid)
    if destino.exists():
        shutil.rmtree(destino)
    destino.mkdir(parents=True)
    (destino / "revision.json").write_text(
        json.dumps(datos, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    (destino / "revision.html").write_text(_html_revision(datos), encoding="utf-8")
    esc = datos.get("escenario") or {}
    (destino / "enunciado.txt").write_text(esc.get("enunciado", ""), encoding="utf-8")
    (destino / "solucion_referencia.txt").write_text(
        esc.get("solucion_referencia", ""),
        encoding="utf-8",
    )
    (destino / "LEEME.txt").write_text(
        f"Paquete de revisión Forja de ejercicios\n"
        f"ID: {eid}\n"
        f"Abre revision.html en el navegador o revision.json para integración.\n"
        f"Aprobar: python3 herramientas/revisar_banco.py aprobar {eid}\n",
        encoding="utf-8",
    )
    return destino


def main() -> int:
    parser = argparse.ArgumentParser(description="Paquete de entrega para profesor")
    parser.add_argument("id", help="ID del pendiente")
    parser.add_argument(
        "--salida",
        "-o",
        help="Carpeta destino (por defecto: examenes/paquetes/<id>)",
    )
    args = parser.parse_args()
    salida = Path(args.salida) if args.salida else RAIZ / "examenes" / "paquetes" / args.id
    try:
        ruta = generar_paquete(args.id, salida)
    except FileNotFoundError as e:
        print(str(e), file=sys.stderr)
        return 1
    print(ruta)
    return 0


if __name__ == "__main__":
    sys.exit(main())
