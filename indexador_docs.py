#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""
Indexa PDFs de documentacion/ en fragmentos JSON en indice/.

Cada subcarpeta de primer nivel (ansible, terraform, kubernetes…) genera
automáticamente un módulo docs_<nombre> sin tocar el código de la aplicación.

Estructura recomendada:
  documentacion/docker/volumenes/apuntes.pdf  → tema docker, capítulo volumenes
  documentacion/ansible/                      → docs_ansible
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

import comun
from tipo_materia import leer_tipo_desde_carpeta, resolver_tipo_materia

RAIZ = Path(__file__).resolve().parent
CARPETA_DOCUMENTACION = RAIZ / "documentacion"
CARPETA_INDICE = RAIZ / "indice"

TAMANO_FRAGMENTO = 1800
SOLAPAMIENTO = 200
MIN_CHARS_FRAGMENTO = 80

# Títulos tipo "1.2 Volúmenes", "Capítulo 3", "## Redes"
_PATRON_CAPITULO = re.compile(
    r"^(?:capítulo|capitulo|chapter|tema|unidad|parte)\s*[\d.:]+\s*[:\-]?\s*(.+)$",
    re.IGNORECASE,
)
_PATRON_NUMERADO = re.compile(r"^(\d+(?:\.\d+)*)\s+(.{4,80})$")
_PATRON_MAYUSCULAS = re.compile(r"^[A-ZÁÉÍÓÚÑ0-9][A-ZÁÉÍÓÚÑ0-9\s\-]{3,60}$")


def _limpiar_texto(texto: str) -> str:
    texto = texto.replace("\x00", "")
    texto = re.sub(r"[ \t]+\n", "\n", texto)
    texto = re.sub(r"\n{3,}", "\n\n", texto)
    return texto.strip()


def _slug(texto: str) -> str:
    return comun.slug(texto, "general")


def _titulo_legible(slug: str) -> str:
    return slug.replace("_", " ").replace("-", " ").strip().title() or "General"


def _ruta_relativa_segura(ruta: Path, base: Path) -> str:
    """Ruta relativa portable (evita ValueError en Windows con unidades distintas)."""
    try:
        return str(ruta.resolve().relative_to(base.resolve()))
    except ValueError:
        return str(ruta.resolve())


def _configurar_salida_consola() -> None:
    """Evita UnicodeEncodeError en consola Windows (cp1252) al imprimir mensajes."""
    for stream in (sys.stdout, sys.stderr):
        reconfigurar = getattr(stream, "reconfigure", None)
        if callable(reconfigurar):
            try:
                reconfigurar(encoding="utf-8", errors="replace")
            except (OSError, ValueError):
                pass


def extraer_texto_pdf(ruta_pdf: Path) -> list[tuple[int, str]]:
    try:
        from pypdf import PdfReader
    except ImportError as exc:
        raise RuntimeError(
            "Falta pypdf. Instala: pip install -r requirements-docs.txt"
        ) from exc
    try:
        lector = PdfReader(str(ruta_pdf))
    except Exception as error:
        mensaje = str(error).lower()
        if "encrypted" in mensaje or "password" in mensaje:
            raise RuntimeError(
                f"PDF protegido con contraseña: {ruta_pdf.name}. "
                "Exporta una copia sin protección."
            ) from error
        raise RuntimeError(f"PDF corrupto o ilegible: {ruta_pdf.name} ({error})") from error
    if getattr(lector, "is_encrypted", False):
        raise RuntimeError(
            f"PDF protegido con contraseña: {ruta_pdf.name}. "
            "Exporta una copia sin protección."
        )
    paginas: list[tuple[int, str]] = []
    for numero, pagina in enumerate(lector.pages, start=1):
        bruto = pagina.extract_text() or ""
        limpio = _limpiar_texto(bruto)
        if limpio:
            paginas.append((numero, limpio))
    return paginas


def fragmentar_texto(
    texto: str,
    tamano: int = TAMANO_FRAGMENTO,
    solapamiento: int = SOLAPAMIENTO,
) -> list[str]:
    if len(texto) <= tamano:
        return [texto] if texto else []
    fragmentos: list[str] = []
    inicio = 0
    while inicio < len(texto):
        fin = min(inicio + tamano, len(texto))
        trozo = texto[inicio:fin].strip()
        if trozo:
            fragmentos.append(trozo)
        if fin >= len(texto):
            break
        inicio = max(fin - solapamiento, inicio + 1)
    return fragmentos


def detectar_seccion_en_texto(texto: str) -> str | None:
    """Intenta detectar un título de sección al inicio del texto de página."""
    lineas = texto.split("\n")[:8]
    for linea in lineas:
        linea = linea.strip()
        if len(linea) < 4 or len(linea) > 100:
            continue
        match = _PATRON_CAPITULO.match(linea)
        if match:
            return match.group(1).strip()
        match = _PATRON_NUMERADO.match(linea)
        if match and not match.group(2)[0].isdigit():
            return match.group(2).strip()
        if _PATRON_MAYUSCULAS.match(linea) and len(linea.split()) <= 8:
            return linea.title()
    return None


def nombre_modulo_desde_carpeta(nombre_carpeta: str) -> str:
    slug = _slug(nombre_carpeta)
    return f"docs_{slug}" if slug else "docs_general"


def metadatos_desde_ruta(ruta_pdf: Path, carpeta_tema: Path) -> dict[str, str]:
    """
    documentacion/docker/volumenes/persistencia.pdf
      → tema docker, capitulo volumenes, seccion persistencia
    documentacion/ansible/intro.pdf
      → tema ansible, capitulo intro
    """
    tema = carpeta_tema.name
    try:
        rel = ruta_pdf.relative_to(carpeta_tema)
    except ValueError:
        rel = ruta_pdf.name

    partes = list(rel.parts)
    archivo = partes[-1] if partes else ruta_pdf.name
    carpetas = partes[:-1] if len(partes) > 1 else []

    if carpetas:
        capitulo = carpetas[0]
        seccion_ruta = "/".join(carpetas[1:]) if len(carpetas) > 1 else ""
        if not seccion_ruta:
            seccion_ruta = Path(archivo).stem
    else:
        capitulo = Path(archivo).stem
        seccion_ruta = ""

    return {
        "tema": tema,
        "capitulo": capitulo,
        "capitulo_slug": _slug(capitulo),
        "seccion_ruta": seccion_ruta,
        "archivo": archivo,
        "fuente": _ruta_relativa_segura(ruta_pdf, RAIZ),
    }


def _construir_catalogo_secciones(chunks: list[dict]) -> list[dict]:
    """Agrupa fragmentos por capítulo para la UI (Docker → Volúmenes, Redes…)."""
    conteo: dict[str, dict] = {}
    for chunk in chunks:
        cap_slug = chunk.get("capitulo_slug") or _slug(chunk.get("capitulo", "general"))
        if cap_slug not in conteo:
            titulo = chunk.get("capitulo_titulo") or _titulo_legible(
                chunk.get("capitulo", cap_slug)
            )
            conteo[cap_slug] = {
                "capitulo": cap_slug,
                "titulo": titulo,
                "fragmentos": 0,
            }
        conteo[cap_slug]["fragmentos"] += 1
    return sorted(conteo.values(), key=lambda s: s["titulo"])


def indexar_coleccion(
    carpeta: Path,
    carpeta_indice: Path,
    *,
    solo_pdfs_directos: bool = False,
    nombre_modulo: str | None = None,
) -> dict | None:
    pdfs = sorted(carpeta.glob("*.pdf") if solo_pdfs_directos else carpeta.rglob("*.pdf"))
    if not pdfs:
        return None

    modulo = nombre_modulo or nombre_modulo_desde_carpeta(carpeta.name)
    tema_slug = carpeta.name if modulo != "docs_general" else "general"
    tipo_materia = leer_tipo_desde_carpeta(carpeta) or resolver_tipo_materia(
        modulo, tema_slug=tema_slug
    )
    chunks: list[dict] = []
    contador_fragmento = 0

    for ruta_pdf in pdfs:
        try:
            paginas = extraer_texto_pdf(ruta_pdf)
        except Exception as error:
            print(f"  [!] No se pudo leer {ruta_pdf}: {error}", file=sys.stderr)
            continue

        if not paginas:
            print(
                f"  [!] Sin texto extraible en {ruta_pdf.name} (escaneado o vacio?).",
                file=sys.stderr,
            )
            continue

        meta_ruta = metadatos_desde_ruta(ruta_pdf, carpeta)
        seccion_actual = meta_ruta.get("seccion_ruta") or ""

        for numero_pagina, texto_pagina in paginas:
            detectada = detectar_seccion_en_texto(texto_pagina)
            if detectada:
                seccion_actual = detectada

            partes = fragmentar_texto(texto_pagina)
            for parte_local, texto_fragmento in enumerate(partes):
                if len(texto_fragmento) < MIN_CHARS_FRAGMENTO:
                    continue
                contador_fragmento += 1
                cap_slug = meta_ruta["capitulo_slug"]
                sec_slug = _slug(seccion_actual) if seccion_actual else cap_slug

                chunks.append({
                    "fragmento_id": contador_fragmento,
                    "id": f"{modulo}-f{contador_fragmento}",
                    "tema": tema_slug,
                    "modulo": modulo,
                    "tipo_materia": tipo_materia,
                    "capitulo": meta_ruta["capitulo"],
                    "capitulo_slug": cap_slug,
                    "capitulo_titulo": _titulo_legible(meta_ruta["capitulo"]),
                    "seccion": seccion_actual or _titulo_legible(meta_ruta["capitulo"]),
                    "seccion_slug": sec_slug,
                    "pagina": numero_pagina,
                    "pagina_fin": numero_pagina,
                    "archivo": meta_ruta["archivo"],
                    "fuente": meta_ruta["fuente"],
                    "parte": parte_local + 1,
                    "fragmento": texto_fragmento,
                    "texto": texto_fragmento,
                })

    if not chunks:
        return None

    titulo_visible = (
        "Apuntes generales" if modulo == "docs_general"
        else _titulo_legible(carpeta.name)
    )
    indice = {
        "modulo": modulo,
        "tema": tema_slug,
        "tipo_materia": tipo_materia,
        "titulo_visible": titulo_visible,
        "carpeta": _ruta_relativa_segura(carpeta, RAIZ),
        "chunks": chunks,
        "secciones": _construir_catalogo_secciones(chunks),
        "total_fragmentos": len(chunks),
        "indexado_en": datetime.now(timezone.utc).isoformat(),
    }
    carpeta_indice.mkdir(parents=True, exist_ok=True)
    ruta_json = carpeta_indice / f"{modulo}.json"
    with ruta_json.open("w", encoding="utf-8") as archivo:
        json.dump(indice, archivo, ensure_ascii=False, indent=2)
    return indice


def indexar_todo(
    carpeta_documentacion: Path = CARPETA_DOCUMENTACION,
    carpeta_indice: Path = CARPETA_INDICE,
) -> list[dict]:
    if not carpeta_documentacion.is_dir():
        carpeta_documentacion.mkdir(parents=True, exist_ok=True)
        print(f"Creada carpeta vacía: {carpeta_documentacion}")
        return []

    resultados: list[dict] = []
    subcarpetas = sorted(
        p for p in carpeta_documentacion.iterdir()
        if p.is_dir() and not p.name.startswith(".")
    )

    pdfs_raiz = list(carpeta_documentacion.glob("*.pdf"))
    if pdfs_raiz:
        print("Indexando PDFs en la raiz de documentacion/...")
        indice = indexar_coleccion(
            carpeta_documentacion,
            carpeta_indice,
            solo_pdfs_directos=True,
            nombre_modulo="docs_general",
        )
        if indice:
            n = indice["total_fragmentos"]
            s = len(indice.get("secciones", []))
            print(f"  -> {indice['modulo']}: {n} fragmentos, {s} capitulos")
            resultados.append(indice)

    for carpeta in subcarpetas:
        modulo_esperado = nombre_modulo_desde_carpeta(carpeta.name)
        print(f"Indexando {carpeta.name}/ -> {modulo_esperado}...")
        indice = indexar_coleccion(carpeta, carpeta_indice)
        if indice:
            n = indice["total_fragmentos"]
            s = len(indice.get("secciones", []))
            print(f"  -> {indice['modulo']}: {n} fragmentos, {s} capitulos/secciones")
            resultados.append(indice)
        else:
            print(f"  -> sin PDFs legibles en {carpeta.name}")

    return resultados


def principal() -> int:
    _configurar_salida_consola()

    try:
        import pypdf  # noqa: F401
    except ImportError:
        print("Falta pypdf. Instala: pip install -r requirements-docs.txt", file=sys.stderr)
        return 1

    analizador = argparse.ArgumentParser(
        description="Indexar PDFs: cada carpeta en documentacion/ -> docs_<tema>.json"
    )
    analizador.add_argument("--documentacion", type=Path, default=CARPETA_DOCUMENTACION)
    analizador.add_argument("--indice", type=Path, default=CARPETA_INDICE)
    argumentos = analizador.parse_args()

    try:
        colecciones = indexar_todo(argumentos.documentacion, argumentos.indice)
    except Exception as error:
        print(f"Error al indexar: {error}", file=sys.stderr)
        return 1

    if not colecciones:
        print(
            "No se indexo nada. Anade PDFs en documentacion/<tema>/ y vuelve a ejecutar.",
            file=sys.stderr,
        )
        return 1
    print(f"\nListo: {len(colecciones)} modulo(s) en {argumentos.indice}")
    for ind in colecciones:
        print(f"  - {ind['modulo']}: {ind['titulo_visible']}")
    return 0


if __name__ == "__main__":
    sys.exit(principal())
