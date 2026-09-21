#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Property-based: la nota del evaluador siempre queda en [0, 10].

Opcional: se omite si `hypothesis` no está instalado
(`pip install hypothesis` o CI con deps de desarrollo).
"""

from __future__ import annotations

import sys
from pathlib import Path

import pytest

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

pytest.importorskip("hypothesis")

from hypothesis import given, settings  # noqa: E402
from hypothesis import strategies as st  # noqa: E402

import evaluador  # noqa: E402

peso_raro = st.one_of(
    st.integers(-50, 200),
    st.floats(allow_nan=False, allow_infinity=False, width=32),
    st.text(max_size=12),
    st.none(),
    st.just("no-numero"),
    st.just(""),
)


@given(
    pesos=st.lists(peso_raro, min_size=1, max_size=10),
    respuesta=st.text(
        alphabet=st.characters(blacklist_categories=("Cs",)),
        max_size=400,
    ),
)
@settings(max_examples=80, deadline=None)
def probar_nota_siempre_entre_0_y_10_property(pesos, respuesta):
    escenario = {
        "id": "hyp-nota",
        "modulo": "test",
        "criterios": [
            {
                "tipo": "contiene_todos",
                "terminos": ["docker"] if i % 2 == 0 else ["nginx"],
                "peso": peso,
            }
            for i, peso in enumerate(pesos)
        ],
    }
    r = evaluador.evaluar(escenario, respuesta)
    assert 0.0 <= r["nota"] <= 10.0
    assert 0.0 <= r["nota_elementos"] <= 10.0
    assert r["peso_total"] >= 1
