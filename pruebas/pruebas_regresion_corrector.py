#!/usr/bin/env python3
# Desarrollado por entreunosyceros - 2026
"""Regresión del corrector: límites de palabra, nota parcial, no_contiene, obligatorio."""

import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(RAIZ))

import evaluador
from alias_comandos import contiene_termino_flexible


def probar_limites_palabra_falsos_positivos():
    casos = [
        ("No se puede concatenar la salida", "cat"),
        ("Es false, no funciona", "ls"),
        ("El formulario esta mal", "rm"),
        ("Usa el metodo digital", "git"),
        ("Hice un commitment con el equipo", "commit"),
    ]
    for respuesta, termino in casos:
        assert not contiene_termino_flexible(respuesta, termino), (
            f"falso positivo: «{termino}» en «{respuesta}»"
        )
    # Negar el comando sigue encontrando la palabra (límites de palabra no bastan);
    # para eso existe el criterio no_contiene / obligatorio.
    assert contiene_termino_flexible("No uses chmod nunca jamas", "chmod")


def probar_limites_palabra_positivos():
    assert contiene_termino_flexible("usa cat archivo.txt", "cat")
    assert contiene_termino_flexible("git commit -m msg", "commit")
    assert contiene_termino_flexible("docker container run -d nginx", "docker run")


def probar_nota_parcial_contiene_todos():
    escenario = {
        "id": "p1",
        "modulo": "test",
        "criterios": [
            {"tipo": "contiene_todos", "terminos": ["a", "b", "c", "d", "e"], "peso": 10},
        ],
    }
    r = evaluador.evaluar(escenario, "a b c d")  # 4 de 5
    assert abs(r["nota"] - 8.0) < 0.05, r
    assert not r["detalles"][0]["cumplido"]
    assert r["detalles"][0]["encontrados"] == 4


def probar_no_contiene():
    escenario = {
        "id": "p2",
        "modulo": "test",
        "criterios": [
            {"tipo": "contiene_todos", "terminos": ["PreparedStatement"], "peso": 5},
            {
                "tipo": "no_contiene",
                "terminos": ["Statement"],
                "peso": 5,
                "descripcion": "No concatenar SQL con Statement",
            },
        ],
    }
    # "Statement" aparece como subcadena de PreparedStatement — con límites de palabra
    # "Statement" solo no debe coincidir dentro de PreparedStatement... 
    # Wait: PreparedStatement contains "Statement" as suffix with word boundary?
    # (?<!\w)statement(?!\w) against "preparedstatement" - before 's' is 'd' which is \w, so NO match.
    # Good.
    r_ok = evaluador.evaluar(escenario, "PreparedStatement ps = conn.prepareStatement(sql);")
    assert r_ok["detalles"][1]["cumplido"], r_ok
    assert r_ok["nota"] == 10.0

    r_bad = evaluador.evaluar(escenario, "Statement st = conn.createStatement();")
    assert not r_bad["detalles"][0]["cumplido"]
    assert not r_bad["detalles"][1]["cumplido"]


def probar_obligatorio_tope():
    escenario = {
        "id": "p3",
        "modulo": "test",
        "criterios": [
            {
                "tipo": "contiene_todos",
                "terminos": ["PreparedStatement"],
                "peso": 6,
                "obligatorio": True,
            },
            {"tipo": "contiene_todos", "terminos": ["Connection"], "peso": 4},
        ],
    }
    r = evaluador.evaluar(escenario, "Connection c = DriverManager.getConnection(url);")
    assert r["obligatorio_fallido"]
    assert r["nota"] <= 4.0
    assert not r["aprobado"]


def probar_pista_regex_no_filtra_tokens_de_clase():
    import re
    from retroalimentacion_criterios import _tokens_legibles, pista_desde_criterio

    patron = r"getPassword|get[A-Z]\w*"
    tokens = _tokens_legibles(patron)
    assert "A-Z" not in tokens
    assert not any(re.fullmatch(r"[A-Za-z]-[A-Za-z0-9]+", t) for t in tokens)
    pista = pista_desde_criterio({"tipo": "regex", "patron": patron})
    assert "A-Z" not in pista
    assert "getPassword" not in pista


def probar_criterios_no_colador_update_y_for():
    """Alternativas flojas (UPDATE solo, for ( solo) ya no puntúan solos."""
    esc_update = {
        "id": "u1",
        "modulo": "bd_transacciones",
        "criterios": [
            {
                "tipo": "regex",
                "patron": r"UPDATE\s+\w+\s+SET\s+[\s\S]{0,80}\bsaldo\b",
                "peso": 10,
                "flags": "i",
            },
        ],
    }
    assert evaluador.evaluar(esc_update, "UPDATE cuentas SET x = 1")["nota"] < 5
    assert evaluador.evaluar(esc_update, "UPDATE")["nota"] == 0
    ok = evaluador.evaluar(
        esc_update, "UPDATE cuentas SET saldo = saldo - 10 WHERE id = 1"
    )
    assert ok["nota"] == 10.0

    esc_for = {
        "id": "f1",
        "modulo": "poo",
        "criterios": [
            {
                "tipo": "regex",
                "patron": r"List<Empleado>|for\s*\(\s*Empleado\b",
                "peso": 10,
            },
        ],
    }
    assert evaluador.evaluar(esc_for, "for (int i = 0; i < 10; i++) {}")["nota"] == 0
    assert evaluador.evaluar(esc_for, "List<Empleado> xs = List.of();")["nota"] == 10.0


def probar_banco_docker_no_acepta_palabras_sueltas():
    import json

    ruta = RAIZ / "banco" / "aprobados" / "docker" / "ejemplo.json"
    if not ruta.is_file():
        return
    esc = json.loads(ruta.read_text(encoding="utf-8"))
    basura = evaluador.evaluar(esc, "nginx docker -p 8080:80")
    assert not basura["aprobado"], basura
    assert basura["nota"] < 6
    buena = evaluador.evaluar(esc, esc["solucion_referencia"])
    assert buena["aprobado"] and buena["nota"] == 10.0


def probar_banco_aprobados_solucion_pasa():
    """Cada ejercicio aprobado debe sacar 10 con su propia solución de referencia."""
    banco = RAIZ / "banco" / "aprobados"
    if not banco.is_dir():
        return
    import json

    fallos = []
    for ruta in banco.rglob("*.json"):
        try:
            datos = json.loads(ruta.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            continue
        if not datos.get("criterios"):
            continue
        sol = datos.get("solucion_referencia") or ""
        r = evaluador.evaluar(datos, sol)
        if not all(d.get("cumplido") for d in r.get("detalles") or []):
            fallos.append(f"{ruta.name}: nota={r.get('nota')}")
    assert not fallos, "Soluciones de referencia que no pasan:\n" + "\n".join(fallos)


if __name__ == "__main__":
    probar_limites_palabra_falsos_positivos()
    probar_limites_palabra_positivos()
    probar_nota_parcial_contiene_todos()
    probar_no_contiene()
    probar_obligatorio_tope()
    probar_pista_regex_no_filtra_tokens_de_clase()
    probar_criterios_no_colador_update_y_for()
    probar_banco_docker_no_acepta_palabras_sueltas()
    probar_banco_aprobados_solucion_pasa()
    print("OK regresion corrector")
