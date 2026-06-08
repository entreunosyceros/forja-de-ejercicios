#!/usr/bin/env python3
"""Genera un ejercicio JSON a partir de un único fragmento de texto (Gemini)."""

from __future__ import annotations

import json
import os
import re
import sys
from pathlib import Path
from typing import Any

from util_texto import sin_markdown

import modelo_ejercicio

MODELO_POR_DEFECTO = os.environ.get("FORJAEXAMENES_GEMINI_MODEL", "gemini-2.5-flash")
_PLACEHOLDERS_INVALIDOS = frozenset({
    "",
    "tu_clave_de_google_ai_studio",
    "tu-clave",
    "changeme",
    "xxx",
})


_GEMINI_VARS = frozenset({
    "GEMINI_API_KEY",
    "FORJAEXAMENES_GEMINI_API_KEY",
    "FORJAEXAMENES_GEMINI_MODEL",
})


def _cargar_env_desde_fichero() -> None:
    """Lee examenforge/.env. Las claves Gemini del fichero tienen prioridad sobre el entorno heredado."""
    ruta_env = Path(__file__).resolve().parent / ".env"
    if not ruta_env.is_file():
        return
    for linea in ruta_env.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if not linea or linea.startswith("#") or "=" not in linea:
            continue
        clave, _, valor = linea.partition("=")
        clave = clave.strip()
        valor = valor.strip().strip('"').strip("'")
        if not clave or not valor:
            continue
        if clave in _GEMINI_VARS or clave not in os.environ:
            os.environ[clave] = valor


def _obtener_api_key() -> str:
    _cargar_env_desde_fichero()
    clave = (
        os.environ.get("FORJAEXAMENES_GEMINI_API_KEY")
        or os.environ.get("GEMINI_API_KEY")
        or ""
    ).strip()
    if not clave or clave.lower() in _PLACEHOLDERS_INVALIDOS:
        raise RuntimeError(
            "Falta la API de Gemini. Crea examenforge/.env con:\n"
            "  GEMINI_API_KEY=AIza...tu_clave_real\n"
            "Obtén la clave en https://aistudio.google.com/apikey\n"
            "(No uses el texto de ejemplo de .env.example; debe ser una clave real.)"
        )
    if clave.startswith("tu_") or "ejemplo" in clave.lower():
        raise RuntimeError(
            "GEMINI_API_KEY parece un valor de ejemplo, no una clave real. "
            "Edita examenforge/.env con la clave de Google AI Studio."
        )
    return clave


def _mensaje_error_gemini(exc: Exception) -> str:
    texto = str(exc)
    if "API_KEY_INVALID" in texto or "API key not valid" in texto:
        return (
            "La clave GEMINI_API_KEY no es válida. Comprueba examenforge/.env:\n"
            "  1. Crea la clave en https://aistudio.google.com/apikey\n"
            "  2. Pégala en .env: GEMINI_API_KEY=AIzaSy...\n"
            "  3. Sin comillas ni espacios. Reinicia la aplicación tras guardar.\n"
            "Los módulos sin apuntes (redes, poo, docker…) no necesitan Gemini."
        )
    if "429" in texto or "RESOURCE_EXHAUSTED" in texto:
        modelo = MODELO_POR_DEFECTO
        if "model:" in texto:
            m = re.search(r"model:\s*([^\s\\n]+)", texto)
            if m:
                modelo = m.group(1)
        return (
            f"Cuota gratuita agotada para el modelo «{modelo}» (la API key sí es válida).\n"
            "  • Añade en examenforge/.env: FORJAEXAMENES_GEMINI_MODEL=gemini-2.5-flash\n"
            "  • O espera ~1 minuto y vuelve a intentar.\n"
            "  • Revisa límites: https://ai.google.dev/gemini-api/docs/rate-limits"
        )
    return f"Error al llamar a Gemini: {texto[:500]}"


def _extraer_json(texto: str) -> dict[str, Any]:
    texto = texto.strip()
    bloque = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", texto)
    if bloque:
        texto = bloque.group(1).strip()
    inicio = texto.find("{")
    fin = texto.rfind("}")
    if inicio < 0 or fin <= inicio:
        raise ValueError("La respuesta de Gemini no contiene JSON")
    return json.loads(texto[inicio : fin + 1])


def _validar_escenario(datos: dict[str, Any], modulo: str) -> dict[str, Any]:
    """Compatibilidad: valida escenario completo ya construido (banco / tests)."""
    modelo_ejercicio.validar_escenario_verificable(datos)
    return {
        "modulo": modulo,
        "titulo": sin_markdown(str(datos["titulo"]))[:120],
        "enunciado": sin_markdown(str(datos["enunciado"])),
        "criterios": datos["criterios"],
        "solucion_referencia": sin_markdown(str(datos["solucion_referencia"])),
        "parametros": datos.get("parametros") or {},
    }


def _instrucciones_nivel(nivel: int) -> str:
    if nivel <= 1:
        return (
            "Para nivel 1: pregunta concreta y acotada, enunciado breve, "
            "2-3 palabras_clave esenciales del fragmento, sin requisitos ocultos."
        )
    if nivel >= 3:
        return (
            "Para nivel 3: ejercicio más completo (varios pasos o conceptos del fragmento), "
            "4-6 palabras_clave técnicas, sin simplificar el enunciado."
        )
    return "Para nivel 2: equilibrio entre claridad y exigencia técnica."


def _construir_prompt(fragmento: dict, nivel: int, titulo_coleccion: str) -> str:
    nivel = max(1, min(3, nivel))
    pagina = fragmento.get("pagina", "")
    pagina_fin = fragmento.get("pagina_fin", pagina)
    rango_pag = str(pagina)
    if pagina_fin and pagina_fin != pagina:
        rango_pag = f"{pagina}–{pagina_fin}"
    return f"""Eres un profesor de informática. A partir ÚNICAMENTE del fragmento de apuntes siguientes,
crea UN ejercicio práctico para que el alumno escriba comandos o código en un cuadro de texto.

Colección: {titulo_coleccion}
Tema: {fragmento.get("tema", "")}
Capítulo: {fragmento.get("capitulo_titulo", fragmento.get("capitulo", ""))}
Sección: {fragmento.get("seccion", "")}
Página(s): {rango_pag}
Archivo: {fragmento.get("archivo", fragmento.get("fuente", ""))}
Nivel de dificultad pedido: {nivel} (1=fácil y guiado, 2=intermedio, 3=exigente)
{_instrucciones_nivel(nivel)}

--- FRAGMENTO (no uses conocimiento fuera de este texto) ---
{fragmento.get("texto", "")}
--- FIN FRAGMENTO ---

Responde SOLO con un objeto JSON válido (sin markdown ni comentarios), con esta forma exacta:
{{
  "tema": "tema corto del fragmento (ej. docker, sql, redes)",
  "pregunta": "enunciado claro para el alumno, en español, práctico",
  "palabras_clave": [
    "comando o concepto literal que debe aparecer en la respuesta",
    "otro término obligatorio"
  ],
  "solucion_modelo": "respuesta modelo en texto plano (comandos o código, una línea por paso)",
  "variantes": {{
    "nginx": ["nginx", "nginx:latest"]
  }}
}}

Reglas:
- Entre 2 y 8 palabras_clave: fragmentos literales del material (comandos, flags, nombres)
- Cada palabra_clave debe aparecer literalmente en el fragmento de apuntes
- Evita términos genéricos (configurar, servidor, usar, sistema…)
- variantes es opcional: sinónimos válidos por clave
- NO inventes expresiones regulares ni criterios de corrección; solo texto que el alumno debe incluir
- Las palabras_clave deben poder comprobarse buscando ese texto en la respuesta del alumno
- No inventes temas que no aparezcan en el fragmento
- La pregunta debe pedir hacer algo (comandos, código), no solo definir
- solucion_modelo y pregunta en TEXTO PLANO: sin markdown, sin ```, sin **, sin #
"""


def _timeout_ms() -> int:
    """Timeout (ms) de la llamada a Gemini; lo fija Java vía entorno. 0 = sin límite."""
    valor = os.environ.get("FORJAEXAMENES_GEMINI_TIMEOUT_MS", "60000").strip()
    try:
        return max(0, int(valor))
    except ValueError:
        return 60000


def _crear_cliente():
    try:
        from google import genai
    except ImportError as exc:
        raise RuntimeError(
            "Falta google-genai. Instala: pip install -r requirements-docs.txt"
        ) from exc

    api_key = _obtener_api_key()
    timeout_ms = _timeout_ms()
    if timeout_ms > 0:
        try:
            return genai.Client(api_key=api_key, http_options={"timeout": timeout_ms})
        except Exception:
            # Versiones antiguas del SDK no aceptan http_options; sin timeout entonces.
            pass
    return genai.Client(api_key=api_key)


def generar_desde_fragmento(
    fragmento: dict,
    modulo: str,
    titulo_coleccion: str = "Apuntes",
    nivel: int = 2,
    modelo: str | None = None,
) -> dict:
    texto_fragmento = fragmento.get("texto", "") or ""
    modelo_efectivo = modelo or MODELO_POR_DEFECTO
    # El cliente y el prompt se construyen una sola vez y se reutilizan en los reintentos.
    cliente = _crear_cliente()
    prompt = _construir_prompt(fragmento, nivel, titulo_coleccion)
    ultimo_error: Exception | None = None
    max_intentos = 3

    for intento in range(max_intentos):
        try:
            return _generar_desde_fragmento_intento(
                cliente, modelo_efectivo, prompt, fragmento, modulo,
                titulo_coleccion, texto_fragmento,
            )
        except ValueError as exc:
            ultimo_error = exc
            if intento >= max_intentos - 1:
                raise
    raise ultimo_error or RuntimeError("No se pudo generar ejercicio desde fragmento")


def _generar_desde_fragmento_intento(
    cliente,
    modelo_efectivo: str,
    prompt: str,
    fragmento: dict,
    modulo: str,
    titulo_coleccion: str,
    texto_fragmento: str,
) -> dict:
    try:
        respuesta = cliente.models.generate_content(
            model=modelo_efectivo,
            contents=prompt,
            config={
                "temperature": 0.4,
                "response_mime_type": "application/json",
            },
        )
    except Exception as exc:
        nombre = type(exc).__name__
        if "ClientError" in nombre or "APIError" in nombre:
            raise RuntimeError(_mensaje_error_gemini(exc)) from exc
        raise

    texto = (respuesta.text or "").strip()
    if not texto:
        raise RuntimeError("Gemini devolvió una respuesta vacía")

    datos = _extraer_json(texto)
    datos["_fuente"] = fragmento.get("fuente", "")
    datos["_fragmento_id"] = fragmento.get("id", "")
    datos["_pagina"] = fragmento.get("pagina")
    datos["_modelo"] = modelo_efectivo

    propuesta = modelo_ejercicio.validar_propuesta_gemini(
        datos,
        modulo=modulo,
        texto_fragmento=texto_fragmento,
    )
    escenario = modelo_ejercicio.construir_escenario_desde_propuesta(
        propuesta,
        modulo,
        titulo_coleccion=titulo_coleccion,
        texto_fragmento=texto_fragmento,
        metadatos={
            "fuente_fragmento": datos["_fuente"],
            "fragmento_id": datos["_fragmento_id"],
            "pagina": datos["_pagina"],
            "modelo": modelo_efectivo,
        },
    )
    escenario["parametros"]["generado_con"] = "gemini+modelo_ejercicio"
    return escenario


if __name__ == "__main__":
    print(
        "Uso: importar desde generador_docs.py o generador.py (no ejecutar directamente).",
        file=sys.stderr,
    )
    sys.exit(1)
