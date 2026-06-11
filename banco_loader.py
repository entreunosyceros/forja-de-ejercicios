#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Carga ejercicios del banco verificable (aprobados / pendientes) y catálogo."""

from __future__ import annotations

import json
import random
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable

import comun
import modelo_ejercicio

RAIZ = Path(__file__).resolve().parent
CARPETA_BANCO = RAIZ / "banco"
APROBADOS = CARPETA_BANCO / "aprobados"
PENDIENTES = CARPETA_BANCO / "pendientes"
CATALOGO = CARPETA_BANCO / "catalogo.json"

MODULOS_BANCO: tuple[str, ...] = ()

_SUBCARPETAS_INVALIDAS = frozenset({"aprobados", "pendientes"})


def _resolver_modulo_banco(datos: dict[str, Any], ruta: Path) -> str:
    """Nombre de módulo estable para catálogo y generador (prefijo banco_*)."""
    crudo = (datos.get("modulo") or "").strip()
    if crudo in ("", "aprobados", "banco_aprobados"):
        crudo = ""
    elif crudo.startswith("docs_"):
        return crudo
    elif crudo.startswith("banco_"):
        return crudo
    else:
        return f"banco_{crudo}"

    try:
        rel = ruta.relative_to(APROBADOS)
        if len(rel.parts) >= 2:
            sub = rel.parts[0]
            if sub and sub not in _SUBCARPETAS_INVALIDAS:
                return sub if sub.startswith("banco_") else f"banco_{sub}"
    except ValueError:
        pass
    return "banco_general"


def _generar_id() -> str:
    return comun.generar_identificador()


def _ahora_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def asegurar_estructura() -> None:
    APROBADOS.mkdir(parents=True, exist_ok=True)
    PENDIENTES.mkdir(parents=True, exist_ok=True)


def listar_json_aprobados() -> list[Path]:
    if not APROBADOS.is_dir():
        return []
    return sorted(APROBADOS.rglob("*.json"))


def listar_pendientes() -> list[Path]:
    if not PENDIENTES.is_dir():
        return []
    return sorted(PENDIENTES.glob("*.json"))


def reconstruir_catalogo() -> dict[str, Any]:
    asegurar_estructura()
    entradas: list[dict[str, Any]] = []
    for ruta in listar_json_aprobados():
        try:
            datos = json.loads(ruta.read_text(encoding="utf-8"))
            modulo = _resolver_modulo_banco(datos, ruta)
            try:
                ruta_rel = str(ruta.relative_to(RAIZ))
            except ValueError:
                ruta_rel = str(ruta)
            entradas.append({
                "id": datos.get("id") or ruta.stem,
                "modulo": modulo,
                "titulo": datos.get("titulo", ruta.stem),
                "ruta": ruta_rel,
                "origen": datos.get("parametros", {}).get("generado_con", "banco"),
            })
        except (json.JSONDecodeError, OSError, ValueError):
            continue
    catalogo = {"actualizado": _ahora_iso(), "ejercicios": entradas}
    CATALOGO.write_text(
        json.dumps(catalogo, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return catalogo


def cargar_catalogo() -> dict[str, Any]:
    if CATALOGO.is_file():
        try:
            return json.loads(CATALOGO.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            pass
    return reconstruir_catalogo()


def _preparar_escenario(datos: dict[str, Any], ruta: Path | None = None) -> dict[str, Any]:
    escenario = dict(datos)
    modelo_ejercicio.validar_escenario_verificable(escenario)
    if not escenario.get("id"):
        escenario["id"] = _generar_id()
    params = dict(escenario.get("parametros") or {})
    if ruta:
        try:
            params["banco_ruta"] = str(ruta.relative_to(RAIZ))
        except ValueError:
            params["banco_ruta"] = str(ruta)
    params["generado_con"] = params.get("generado_con", "banco")
    escenario["parametros"] = params
    return escenario


def cargar_ejercicio_aprobado(ruta: Path) -> dict[str, Any]:
    datos = json.loads(ruta.read_text(encoding="utf-8"))
    return _preparar_escenario(datos, ruta)


def ejercicios_aprobados_por_modulo(modulo: str) -> list[Path]:
    rutas = []
    for ruta in listar_json_aprobados():
        try:
            datos = json.loads(ruta.read_text(encoding="utf-8"))
            if _resolver_modulo_banco(datos, ruta) == modulo:
                rutas.append(ruta)
        except (json.JSONDecodeError, OSError):
            continue
    return rutas


def generar_desde_banco_aprobado(modulo: str) -> dict[str, Any]:
    rutas = ejercicios_aprobados_por_modulo(modulo)
    if not rutas:
        raise RuntimeError(
            f"No hay ejercicios aprobados en banco para '{modulo}'. "
            "Añade JSON en banco/aprobados/ o desactiva gemini-solo-aprobados."
        )
    return cargar_ejercicio_aprobado(random.choice(rutas))


def guardar_pendiente(
    escenario: dict[str, Any],
    propuesta: dict[str, Any] | None = None,
) -> Path:
    asegurar_estructura()
    eid = escenario.get("id") or _generar_id()
    payload = {
        "id": eid,
        "creado": _ahora_iso(),
        "modulo": escenario.get("modulo"),
        "propuesta": propuesta,
        "escenario_preview": escenario,
    }
    ruta = PENDIENTES / f"{eid}.json"
    ruta.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return ruta


def cargar_pendiente(eid: str) -> dict[str, Any]:
    ruta = PENDIENTES / f"{eid}.json"
    if not ruta.is_file():
        raise FileNotFoundError(f"Pendiente no encontrado: {eid}")
    return json.loads(ruta.read_text(encoding="utf-8"))


def aprobar_pendiente(eid: str, subcarpeta: str | None = None) -> Path:
    import evaluador
    import modelo_ejercicio

    datos = cargar_pendiente(eid)
    escenario = datos.get("escenario_preview") or {}
    if not escenario.get("criterios"):
        raise ValueError("El pendiente no tiene escenario válido")
    if not modelo_ejercicio.solucion_referencia_completa(escenario):
        resultado = evaluador.evaluar(escenario, escenario.get("solucion_referencia") or "")
        raise ValueError(
            "La solución de referencia no cumple todos los criterios "
            f"(nota {resultado.get('nota', 0)}/10). Corrígela antes de aprobar."
        )
    modulo = escenario.get("modulo", "general")
    carpeta = APROBADOS / (subcarpeta or modulo.replace("docs_", ""))
    carpeta.mkdir(parents=True, exist_ok=True)
    destino = carpeta / f"{eid}.json"
    escenario["id"] = eid
    params = dict(escenario.get("parametros") or {})
    params["aprobado_en"] = _ahora_iso()
    params["generado_con"] = params.get("generado_con", "gemini+modelo_ejercicio")
    escenario["parametros"] = params
    destino.write_text(json.dumps(escenario, ensure_ascii=False, indent=2), encoding="utf-8")
    (PENDIENTES / f"{eid}.json").unlink(missing_ok=True)
    reconstruir_catalogo()
    return destino


def rechazar_pendiente(eid: str) -> None:
    ruta = PENDIENTES / f"{eid}.json"
    if ruta.is_file():
        ruta.unlink()


def _fabrica_banco(ruta: Path) -> Callable[[], dict]:
    def generar() -> dict:
        return cargar_ejercicio_aprobado(ruta)

    return generar


def registrar_en_generadores(
    generadores: dict[str, Callable[[], dict]],
    pistas_modulo: dict[str, str],
) -> tuple[str, ...]:
    global MODULOS_BANCO
    asegurar_estructura()
    reconstruir_catalogo()
    por_modulo: dict[str, list[Path]] = {}
    for ruta in listar_json_aprobados():
        try:
            datos = json.loads(ruta.read_text(encoding="utf-8"))
            modulo = _resolver_modulo_banco(datos, ruta)
            por_modulo.setdefault(modulo, []).append(ruta)
        except (json.JSONDecodeError, OSError):
            continue

    modulos: set[str] = set()
    for modulo, rutas in por_modulo.items():
        def _fabrica(rs: list[Path] = rutas) -> dict:
            return cargar_ejercicio_aprobado(random.choice(rs))

        generadores[modulo] = _fabrica
        pistas_modulo[modulo] = (
            "Ejercicio del banco verificado del profesor. "
            "Incluye todos los comandos o conceptos del enunciado."
        )
        modulos.add(modulo)

    MODULOS_BANCO = tuple(sorted(modulos))
    return MODULOS_BANCO


def listar_entradas_portada() -> list[dict[str, str]]:
    catalogo = cargar_catalogo()
    vistos: set[str] = set()
    salida: list[dict[str, str]] = []
    for ej in catalogo.get("ejercicios") or []:
        modulo = ej.get("modulo", "")
        if modulo in vistos:
            continue
        vistos.add(modulo)
        salida.append({
            "modulo": modulo,
            "titulo": ej.get("titulo", modulo),
        })
    return salida
