#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Tipos de criterio como Strategy con registro (Factory).

Añadir un tipo nuevo = una clase + @registrar. El evaluador y la validación
usan el mismo registro.
"""

from __future__ import annotations

import operator
import re
from abc import ABC, abstractmethod
from functools import reduce
from typing import Any, ClassVar

from alias_comandos import contiene_termino_flexible, variantes_termino
from retroalimentacion_criterios import enriquecer_detalle

try:
    import regex as _regex_mod  # type: ignore
except ImportError:  # pragma: no cover
    _regex_mod = None

_TIMEOUT_REGEX_S = 0.05
_MAX_RESPUESTA_REGEX = 50_000
# Cuantificadores anidados típicos de ReDoS: (a+)+, (a*)+, etc.
_REDOS_ANIDADOS = re.compile(
    r"\((?:[^()]*[+*][^()]*)\)[+*]|\((?:[^()]*[+*][^()]*)\)\{"
)

_BANDERAS = {
    "i": re.IGNORECASE,
    "m": re.MULTILINE,
    "s": re.DOTALL,
}

_REGISTRO: dict[str, type["Criterio"]] = {}


def registrar(cls: type["Criterio"]) -> type["Criterio"]:
    _REGISTRO[cls.tipo] = cls
    return cls


def tipos_soportados() -> frozenset[str]:
    return frozenset(_REGISTRO.keys())


def desde_json(datos: dict[str, Any]) -> "Criterio":
    tipo = datos.get("tipo", "regex")
    clase = _REGISTRO.get(tipo)
    if clase is None:
        raise ValueError(f"Tipo de criterio no soportado: {tipo}")
    return clase(datos)


def banderas_regex(criterio: dict) -> int:
    texto = str(criterio.get("banderas") or criterio.get("flags") or "").lower().strip()
    if not texto:
        return 0
    if re.fullmatch(r"[ims]+", texto):
        return reduce(operator.or_, (_BANDERAS[c] for c in set(texto)), 0)
    tokens = set(re.findall(r"[a-z]+", texto))
    valor = 0
    if tokens & {"i", "ignorecase"}:
        valor |= re.IGNORECASE
    if tokens & {"m", "multiline"}:
        valor |= re.MULTILINE
    if tokens & {"s", "dotall"}:
        valor |= re.DOTALL
    return valor


def peso_criterio(criterio: dict) -> int:
    bruto = criterio.get("peso", 1)
    try:
        peso = int(bruto)
    except (TypeError, ValueError):
        return 1
    return max(1, peso)


def es_obligatorio(criterio: dict) -> bool:
    return bool(criterio.get("obligatorio"))


def patron_sospecha_redos(patron: str) -> bool:
    """Heurística: cuantificadores anidados peligrosos."""
    return bool(_REDOS_ANIDADOS.search(patron or ""))


def _buscar_regex(patron: str, texto: str, banderas: int) -> bool:
    if len(texto) > _MAX_RESPUESTA_REGEX:
        texto = texto[:_MAX_RESPUESTA_REGEX]
    if _regex_mod is not None:
        return bool(
            _regex_mod.search(patron, texto, flags=banderas, timeout=_TIMEOUT_REGEX_S)
        )
    return bool(re.search(patron, texto, banderas))


class Criterio(ABC):
    tipo: ClassVar[str]

    def __init__(self, datos: dict[str, Any]) -> None:
        self.datos = datos
        self.peso = peso_criterio(datos)
        self.obligatorio = es_obligatorio(datos)

    @abstractmethod
    def evaluar(self, respuesta: str) -> dict[str, Any]:
        ...

    def validar(self) -> None:
        """Comprueba que el criterio está bien formado."""
        return


@registrar
class CriterioRegex(Criterio):
    tipo = "regex"

    def validar(self) -> None:
        patron = self.datos.get("patron", "")
        if not patron:
            raise ValueError("Criterio regex sin patrón")
        if patron_sospecha_redos(patron):
            raise ValueError(
                f"Patrón regex con riesgo de ReDoS (cuantificadores anidados): {patron[:80]}"
            )
        re.compile(patron, banderas_regex(self.datos))

    def evaluar(self, respuesta: str) -> dict[str, Any]:
        patron = self.datos.get("patron", "")
        try:
            cumplido = _buscar_regex(patron, respuesta or "", banderas_regex(self.datos))
        except TimeoutError:
            return {
                "cumplido": False,
                "patron": patron,
                "error": "Patrón regex demasiado costoso (timeout)",
                "peso": self.peso,
                "peso_parcial": 0.0,
                "tipo": self.tipo,
                "obligatorio": self.obligatorio,
            }
        except re.error as error:
            return {
                "cumplido": False,
                "patron": patron,
                "error": str(error),
                "peso": self.peso,
                "peso_parcial": 0.0,
                "tipo": self.tipo,
                "obligatorio": self.obligatorio,
            }
        detalle = {
            "cumplido": cumplido,
            "patron": patron,
            "tipo": self.tipo,
            "peso": self.peso,
            "peso_parcial": float(self.peso if cumplido else 0),
            "obligatorio": self.obligatorio,
            "descripcion": self.datos.get("descripcion", "Criterio del enunciado"),
            "esperado": self.datos.get(
                "esperado",
                "Tu respuesta debe cubrir lo que pide el enunciado en este apartado.",
            ),
        }
        if not cumplido:
            detalle["pista"] = self.datos.get(
                "pista",
                "Revisa el enunciado: falta un paso o concepto clave.",
            )
        return enriquecer_detalle(self.datos, detalle)


@registrar
class CriterioContieneTodos(Criterio):
    tipo = "contiene_todos"

    def validar(self) -> None:
        if not (self.datos.get("terminos") or []):
            raise ValueError("contiene_todos requiere lista terminos")

    def evaluar(self, respuesta: str) -> dict[str, Any]:
        terminos = self.datos.get("terminos") or []
        if not terminos:
            return {
                "cumplido": False,
                "tipo": self.tipo,
                "terminos": terminos,
                "peso": self.peso,
                "peso_parcial": 0.0,
                "obligatorio": self.obligatorio,
                "error": "contiene_todos sin terminos",
                "esperado": self.datos.get("esperado", "Criterio mal configurado"),
                "descripcion": self.datos.get("descripcion", "Sin términos"),
                "pista": self.datos.get("pista", "Revisa la configuración del ejercicio"),
            }
        faltan = [t for t in terminos if not contiene_termino_flexible(respuesta, t)]
        encontrados = len(terminos) - len(faltan)
        fraccion = encontrados / len(terminos)
        cumplido = len(faltan) == 0
        detalle = {
            "cumplido": cumplido,
            "tipo": self.tipo,
            "terminos": terminos,
            "peso": self.peso,
            "peso_parcial": round(self.peso * fraccion, 4),
            "encontrados": encontrados,
            "total_terminos": len(terminos),
            "obligatorio": self.obligatorio,
            "esperado": self.datos.get(
                "esperado",
                f"Debes cubrir {len(terminos)} conceptos del enunciado "
                "(se puntúa de forma proporcional si aciertas solo algunos).",
            ),
            "descripcion": self.datos.get("descripcion", "Cubrir conceptos"),
        }
        aliases: dict[str, list[str]] = {}
        for t in terminos:
            vars_t = variantes_termino(t)
            if len(vars_t) > 1:
                aliases[t] = vars_t
        if aliases:
            detalle["aliases"] = aliases
        if not cumplido:
            detalle["faltan"] = faltan
            detalle["pista"] = self.datos.get(
                "pista",
                f"Te faltan {len(faltan)} concepto(s) de este apartado. "
                "Revisa el enunciado y completa la respuesta.",
            )
        return enriquecer_detalle(self.datos, detalle)


@registrar
class CriterioContieneAlguno(Criterio):
    tipo = "contiene_alguno"

    def validar(self) -> None:
        if not (self.datos.get("terminos") or []):
            raise ValueError("contiene_alguno requiere lista terminos")

    def evaluar(self, respuesta: str) -> dict[str, Any]:
        terminos = self.datos.get("terminos") or []
        if not terminos:
            return {
                "cumplido": False,
                "tipo": self.tipo,
                "terminos": terminos,
                "peso": self.peso,
                "peso_parcial": 0.0,
                "obligatorio": self.obligatorio,
                "error": "contiene_alguno sin terminos",
                "esperado": self.datos.get("esperado", "Criterio mal configurado"),
                "descripcion": self.datos.get("descripcion", "Sin términos"),
                "pista": self.datos.get("pista", "Revisa la configuración del ejercicio"),
            }
        cumplido = any(contiene_termino_flexible(respuesta, t) for t in terminos)
        detalle = {
            "cumplido": cumplido,
            "tipo": self.tipo,
            "terminos": terminos,
            "peso": self.peso,
            "peso_parcial": float(self.peso if cumplido else 0),
            "obligatorio": self.obligatorio,
            "esperado": self.datos.get(
                "esperado",
                "Debes incluir al menos una de las formas válidas del concepto pedido.",
            ),
            "descripcion": self.datos.get("descripcion", "Incluir forma válida"),
        }
        if not cumplido:
            detalle["pista"] = self.datos.get(
                "pista",
                "Falta el concepto principal de este apartado. Revisa el enunciado.",
            )
        return enriquecer_detalle(self.datos, detalle)


@registrar
class CriterioNoContiene(Criterio):
    tipo = "no_contiene"

    def validar(self) -> None:
        if not (self.datos.get("terminos") or []):
            raise ValueError("no_contiene requiere lista terminos")

    def evaluar(self, respuesta: str) -> dict[str, Any]:
        terminos = self.datos.get("terminos") or []
        if not terminos:
            return {
                "cumplido": False,
                "tipo": self.tipo,
                "terminos": terminos,
                "peso": self.peso,
                "peso_parcial": 0.0,
                "obligatorio": self.obligatorio,
                "error": "no_contiene sin terminos",
                "esperado": self.datos.get("esperado", "Criterio mal configurado"),
                "descripcion": self.datos.get("descripcion", "Sin términos"),
                "pista": self.datos.get("pista", "Revisa la configuración del ejercicio"),
            }
        presentes = [t for t in terminos if contiene_termino_flexible(respuesta, t)]
        cumplido = len(presentes) == 0
        detalle = {
            "cumplido": cumplido,
            "tipo": self.tipo,
            "terminos": terminos,
            "peso": self.peso,
            "peso_parcial": float(self.peso if cumplido else 0),
            "obligatorio": self.obligatorio,
            "esperado": self.datos.get(
                "esperado",
                "Tu respuesta no debe incluir prácticas o elementos prohibidos en el enunciado.",
            ),
            "descripcion": self.datos.get("descripcion", "Evitar elementos no deseados"),
        }
        if not cumplido:
            detalle["presentes"] = presentes
            detalle["pista"] = self.datos.get(
                "pista",
                "Has incluido algo que el enunciado pide evitar. Revisa y corrige esa parte.",
            )
        return enriquecer_detalle(self.datos, detalle)
