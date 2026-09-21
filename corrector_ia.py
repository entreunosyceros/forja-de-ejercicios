#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Segundo corrector con Gemini: ¿la respuesta hace lo que pide el enunciado?

La corrección elástica (regex / términos) pone el suelo. Esta capa opcional
juzga la adecuación semántica frente al enunciado y la solución de referencia.
Se activa con FORJAEXAMENES_CORRECTOR_IA=true (y clave Gemini configurada).
"""

from __future__ import annotations

import json
import os
from typing import Any

# Reutiliza carga de clave / cliente del generador (mismo .env y modelo).
import generador_gemini as gemini


def corrector_ia_activo() -> bool:
    valor = (os.environ.get("FORJAEXAMENES_CORRECTOR_IA") or "").strip().lower()
    return valor in {"1", "true", "yes", "si", "sí", "on"}


def _extraer_json(texto: str) -> dict[str, Any]:
    return gemini._extraer_json(texto)


def _prompt(escenario: dict, respuesta: str, nota_elementos: float) -> str:
    enunciado = (escenario.get("enunciado") or "").strip()
    solucion = (escenario.get("solucion_referencia") or "").strip()
    titulo = (escenario.get("titulo") or "").strip()
    modulo = (escenario.get("modulo") or "").strip()
    return f"""Eres un profesor que corrige una respuesta libre de un alumno.
NO reescribes la respuesta. SOLO juzgas si cumple lo que pide el enunciado.

Módulo: {modulo}
Título: {titulo}
Nota automática por elementos detectados (regex/términos): {nota_elementos}/10
(Esa nota solo mide si aparecen piezas; tú juzgas si la respuesta RESUELVE el problema.)

--- ENUNCIADO ---
{enunciado}

--- SOLUCIÓN DE REFERENCIA (orientativa; no exijas identidad literal) ---
{solucion}

--- RESPUESTA DEL ALUMNO ---
{respuesta}

Responde SOLO con JSON válido (sin markdown):
{{
  "nota": 0.0,
  "cumple_enunciado": false,
  "comentario": "1-3 frases en español para el alumno"
}}

Reglas:
- "nota" entre 0 y 10 (un decimal).
- Sé justo: acepta soluciones equivalentes aunque usen otros comandos o redacciones.
- Si la respuesta es vacía, irrelevante o no aborda el enunciado → nota baja.
- Si resuelve el problema de fondo aunque falte un detalle menor → nota alta.
- "comentario" no debe revelar la solución completa; orienta brevemente.
"""


def juzgar(
    escenario: dict,
    respuesta: str,
    *,
    nota_elementos: float,
    modelo: str | None = None,
) -> dict[str, Any]:
    """Devuelve dict con nota, cumple_enunciado, comentario y metadatos."""
    if not corrector_ia_activo():
        return {"usado": False, "omitido": "desactivado"}

    respuesta = respuesta or ""
    try:
        cliente = gemini._crear_cliente()
        modelo_efectivo = gemini._normalizar_modelo(
            modelo or gemini.MODELO_POR_DEFECTO
        )
        prompt = _prompt(escenario, respuesta, nota_elementos)
        raw = cliente.models.generate_content(
            model=modelo_efectivo,
            contents=prompt,
        )
        texto = getattr(raw, "text", None) or str(raw)
        datos = _extraer_json(texto)
        nota = float(datos.get("nota", 0))
        nota = round(min(10.0, max(0.0, nota)), 1)
        comentario = str(datos.get("comentario") or "").strip()
        cumple = bool(datos.get("cumple_enunciado"))
        return {
            "usado": True,
            "nota": nota,
            "cumple_enunciado": cumple,
            "comentario": comentario,
            "modelo": modelo_efectivo,
        }
    except Exception as exc:  # noqa: BLE001 — degradar a solo elementos
        mensaje = str(exc)
        try:
            mensaje = gemini._mensaje_error_gemini(exc)
        except Exception:
            mensaje = f"Corrector IA no disponible: {mensaje[:240]}"
        return {
            "usado": False,
            "error": mensaje,
        }


def combinar_notas(nota_elementos: float, juicio: dict[str, Any]) -> dict[str, Any]:
    """Nota final = mínimo de elementos e IA cuando la IA aportó nota."""
    modo = (os.environ.get("FORJAEXAMENES_CORRECTOR_IA_MODO") or "min").strip().lower()
    if not juicio.get("usado") or "nota" not in juicio:
        return {
            "nota": nota_elementos,
            "modo_combinacion": "solo_elementos",
        }
    nota_ia = float(juicio["nota"])
    if modo in {"media", "avg", "ponderada", "weighted"}:
        # 60 % elementos (suelo comprobable) + 40 % juicio semántico
        nota = round(0.6 * nota_elementos + 0.4 * nota_ia, 1)
        modo_out = "ponderada_60_40"
    else:
        nota = round(min(nota_elementos, nota_ia), 1)
        modo_out = "minimo"
    return {"nota": nota, "modo_combinacion": modo_out, "nota_ia": nota_ia}
