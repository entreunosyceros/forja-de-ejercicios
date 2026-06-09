#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""
Banco de ejercicios verificables — formato único para todos los orígenes.

Orígenes soportados (misma salida):
  • Plantillas en generador.py (Tipo A, determinista)
  • Propuesta estructurada de Gemini → construcción local (Tipo B)
  • JSON importado en banco/aprobados/
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

from alias_comandos import variantes_termino
from tipo_materia import (
    es_clave_valida,
    min_longitud_clave,
    prohibidas_extra_tipo,
    resolver_tipo_materia,
    texto_enunciado_alumno,
)
from util_texto import sin_markdown

RAIZ = Path(__file__).resolve().parent
RUTA_VOCABULARIO = RAIZ / "vocabulario_claves.json"

CAMPOS_OBLIGATORIOS = ("titulo", "enunciado", "criterios", "solucion_referencia")
MIN_PALABRAS_CLAVE = 2
MAX_PALABRAS_CLAVE = 8
MIN_LONGITUD_CLAVE = 2

PROHIBIDAS_GLOBAL = frozenset({
    "configurar", "configuracion", "servidor", "sistema", "usar", "hacer",
    "importante", "necesario", "correcto", "apropiado", "ejemplo", "proceso",
    "informacion", "datos", "archivo", "programa", "aplicacion", "usuario",
})

TIPOS_CRITERIO = frozenset({"regex", "contiene_todos", "contiene_alguno"})


def _cargar_vocabulario() -> dict[str, Any]:
    if not RUTA_VOCABULARIO.is_file():
        return {}
    try:
        return json.loads(RUTA_VOCABULARIO.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return {}


def _reglas_vocabulario(modulo: str | None) -> tuple[frozenset[str], frozenset[str]]:
    vocab = _cargar_vocabulario()
    prohibidas = set(PROHIBIDAS_GLOBAL)
    permitidas_extra: set[str] = set()

    global_cfg = vocab.get("global") or {}
    prohibidas.update(x.lower() for x in global_cfg.get("prohibidas") or [])
    permitidas_extra.update(x.lower() for x in global_cfg.get("permitidas_extra") or [])

    if modulo:
        if modulo in vocab:
            cfg = vocab[modulo]
            prohibidas.update(x.lower() for x in cfg.get("prohibidas") or [])
            permitidas_extra.update(x.lower() for x in cfg.get("permitidas_extra") or [])
        wildcard = vocab.get("docs_*")
        if wildcard and modulo.startswith("docs_"):
            prohibidas.update(x.lower() for x in wildcard.get("prohibidas") or [])
            permitidas_extra.update(x.lower() for x in wildcard.get("permitidas_extra") or [])

    return frozenset(prohibidas), frozenset(permitidas_extra)


def es_clave_tecnica(palabra: str) -> bool:
    """Al menos un indicador de término técnico (comando, flag, etc.)."""
    p = palabra.strip()
    if len(p) >= 6:
        return True
    if re.search(r"[-/.#\\]|^\d|:\d", p):
        return True
    if " " in p and len(p) >= 4:
        return True
    return False


def _clave_en_fragmento(clave: str, texto_fragmento: str, permitidas_extra: frozenset[str]) -> bool:
    if clave.lower() in permitidas_extra:
        return True
    if not texto_fragmento:
        return True
    return clave.lower() in texto_fragmento.lower()


def validar_escenario_verificable(datos: dict[str, Any]) -> None:
    """Comprueba que un escenario es evaluable por evaluador.py."""
    for campo in CAMPOS_OBLIGATORIOS:
        if campo not in datos or not datos[campo]:
            raise ValueError(f"Falta el campo obligatorio: {campo}")

    criterios = datos["criterios"]
    if not isinstance(criterios, list) or len(criterios) < 2:
        raise ValueError("Se requieren al menos 2 criterios")

    for criterio in criterios:
        tipo = criterio.get("tipo", "regex")
        if tipo not in TIPOS_CRITERIO:
            raise ValueError(f"Tipo de criterio no soportado: {tipo}")
        if tipo == "regex":
            patron = criterio.get("patron", "")
            if not patron:
                raise ValueError("Criterio regex sin patrón")
            re.compile(patron, _banderas_regex(criterio))
        elif tipo == "contiene_todos":
            terminos = criterio.get("terminos") or []
            if not terminos:
                raise ValueError("contiene_todos requiere lista terminos")
        elif tipo == "contiene_alguno":
            terminos = criterio.get("terminos") or []
            if not terminos:
                raise ValueError("contiene_alguno requiere lista terminos")


def validar_propuesta_gemini(
    datos: dict[str, Any],
    *,
    modulo: str | None = None,
    texto_fragmento: str | None = None,
    tipo_materia: str | None = None,
) -> dict[str, Any]:
    """Valida el JSON intermedio que devuelve Gemini (sin regex de corrección)."""
    for campo in ("tema", "pregunta", "palabras_clave"):
        if campo not in datos or not datos[campo]:
            raise ValueError(f"Gemini no devolvió el campo obligatorio: {campo}")

    tema = sin_markdown(str(datos["tema"])).strip()[:80]
    pregunta = sin_markdown(str(datos["pregunta"])).strip()
    if len(pregunta) < 20:
        raise ValueError("La pregunta es demasiado corta")

    claves = datos["palabras_clave"]
    if not isinstance(claves, list):
        raise ValueError("palabras_clave debe ser una lista")

    tipo = resolver_tipo_materia(modulo, tipo_explicito=tipo_materia)
    min_len = min_longitud_clave(tipo)
    prohibidas, permitidas_extra = _reglas_vocabulario(modulo)
    prohibidas = set(prohibidas) | set(prohibidas_extra_tipo(tipo))
    fragmento_lower = (texto_fragmento or "").lower()

    normalizadas: list[str] = []
    vistos: set[str] = set()
    for item in claves:
        texto = sin_markdown(str(item)).strip()
        if len(texto) < min_len or len(texto) > 120:
            continue
        clave_lower = texto.lower()
        if clave_lower in vistos:
            continue
        if clave_lower in prohibidas:
            raise ValueError(f"Palabra clave demasiado genérica: «{texto}»")
        en_fragmento = bool(fragmento_lower and clave_lower in fragmento_lower)
        if fragmento_lower and not _clave_en_fragmento(texto, fragmento_lower, permitidas_extra):
            raise ValueError(
                f"Palabra clave «{texto}» no aparece en el fragmento de apuntes"
            )
        if not es_clave_valida(texto, tipo, en_fragmento=en_fragmento):
            continue
        vistos.add(clave_lower)
        normalizadas.append(texto)

    if len(normalizadas) < MIN_PALABRAS_CLAVE:
        raise ValueError(
            f"Se requieren al menos {MIN_PALABRAS_CLAVE} palabras_clave distintas y adecuadas al material"
        )
    if len(normalizadas) > MAX_PALABRAS_CLAVE:
        normalizadas = normalizadas[:MAX_PALABRAS_CLAVE]

    if not any(
        es_clave_valida(p, tipo, en_fragmento=bool(fragmento_lower and p.lower() in fragmento_lower))
        for p in normalizadas
    ):
        raise ValueError(
            "Al menos una palabra_clave debe ser concreta y adecuada al tipo de materia"
        )

    variantes_raw = datos.get("variantes") or {}
    variantes: dict[str, list[str]] = {}
    if isinstance(variantes_raw, dict):
        for clave, vals in variantes_raw.items():
            if isinstance(vals, list):
                variantes[str(clave).lower()] = [
                    sin_markdown(str(v)).strip() for v in vals if str(v).strip()
                ]

    solucion = sin_markdown(str(datos.get("solucion_modelo", "") or "")).strip()

    return {
        "tema": tema,
        "pregunta": pregunta,
        "palabras_clave": normalizadas,
        "solucion_modelo": solucion,
        "variantes": variantes,
        "tipo_materia": tipo,
    }


def criterio_desde_palabra_clave(
    palabra: str,
    peso: int,
    variantes: dict[str, list[str]] | None = None,
) -> dict[str, Any]:
    """Criterio verificable: contiene_todos (o alguno si hay sinónimos en variantes)."""
    palabra = palabra.strip()
    extras = (variantes or {}).get(palabra.lower(), [])
    terminos = list(variantes_termino(palabra))
    for v in extras:
        if v and v.lower() not in {t.lower() for t in terminos}:
            terminos.append(v)

    if len(terminos) > 1:
        lista = ", ".join(f"«{t}»" for t in terminos)
        return {
            "tipo": "contiene_alguno",
            "terminos": terminos,
            "peso": peso,
            "descripcion": f"Uno de: {lista}",
            "esperado": f"Debe incluir al menos uno de: {lista}.",
            "pista": (
                f"Válido cualquiera de estas formas: {lista}. "
                "El concepto del enunciado debe quedar reflejado con alguna de ellas."
            ),
        }
    return {
        "tipo": "contiene_todos",
        "terminos": [palabra],
        "peso": peso,
        "descripcion": f"Incluir: «{palabra}»",
        "esperado": f"Debe aparecer en tu respuesta: «{palabra}».",
        "pista": (
            f"El ejercicio se basa en tus apuntes: debías mencionar o aplicar "
            f"«{palabra}». Revisa el enunciado y el fragmento usado."
        ),
    }


def construir_criterios_desde_palabras_clave(
    palabras: list[str],
    variantes: dict[str, list[str]] | None = None,
) -> list[dict[str, Any]]:
    if len(palabras) < MIN_PALABRAS_CLAVE:
        raise ValueError("Palabras clave insuficientes para corrección automática")
    pesos = _distribuir_pesos(len(palabras))
    return [
        criterio_desde_palabra_clave(palabra, peso, variantes)
        for palabra, peso in zip(palabras, pesos)
    ]


def construir_escenario_desde_propuesta(
    propuesta: dict[str, Any],
    modulo: str,
    *,
    titulo_coleccion: str = "",
    metadatos: dict[str, Any] | None = None,
    texto_fragmento: str | None = None,
    tipo_materia: str | None = None,
) -> dict[str, Any]:
    """Convierte la propuesta de Gemini en un escenario verificable."""
    validada = validar_propuesta_gemini(
        propuesta,
        modulo=modulo,
        texto_fragmento=texto_fragmento,
        tipo_materia=tipo_materia,
    )
    criterios = construir_criterios_desde_palabras_clave(
        validada["palabras_clave"],
        validada.get("variantes"),
    )

    tipo = validada.get("tipo_materia") or resolver_tipo_materia(
        modulo, tipo_explicito=tipo_materia
    )
    titulo = _titulo_desde_propuesta(validada, titulo_coleccion)
    enunciado = _enunciado_desde_propuesta(validada, tipo)
    solucion = validada["solucion_modelo"] or _solucion_desde_claves(validada["palabras_clave"])

    escenario: dict[str, Any] = {
        "modulo": modulo,
        "titulo": titulo,
        "enunciado": enunciado,
        "criterios": criterios,
        "solucion_referencia": solucion,
        "parametros": {
            "propuesta_gemini": {
                k: v for k, v in validada.items() if k != "variantes"
            },
            "tipo_materia": tipo,
            "generado_con": "gemini+modelo_ejercicio",
            **(metadatos or {}),
        },
    }
    validar_escenario_verificable(escenario)
    return escenario


def cargar_desde_json(ruta: Path | str) -> dict[str, Any]:
    """Carga un ejercicio del banco."""
    path = Path(ruta)
    datos = json.loads(path.read_text(encoding="utf-8"))
    validar_escenario_verificable(datos)
    return datos


def _titulo_desde_propuesta(propuesta: dict[str, str], coleccion: str) -> str:
    tema = propuesta["tema"]
    if coleccion and coleccion.lower() not in tema.lower():
        texto = f"{coleccion}: {tema}"
    else:
        texto = tema
    return texto[:120]


def _enunciado_desde_propuesta(propuesta: dict[str, str], tipo: str = "informatica") -> str:
    pregunta = propuesta["pregunta"]
    claves = propuesta["palabras_clave"][:6]
    return f"{pregunta}\n\n{texto_enunciado_alumno(tipo, claves)}"


def _solucion_desde_claves(palabras: list[str]) -> str:
    return "\n".join(f"# {p}" for p in palabras)


def _distribuir_pesos(cantidad: int) -> list[int]:
    if cantidad <= 2:
        return [3, 2][:cantidad]
    pesos = [2] * cantidad
    pesos[0] = 3
    if cantidad > 3:
        pesos[1] = 3
    return pesos


def _banderas_regex(criterio: dict[str, Any]) -> int:
    banderas_texto = criterio.get("banderas", criterio.get("flags", ""))
    return re.IGNORECASE if str(banderas_texto).lower().find("i") >= 0 else 0
