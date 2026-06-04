/**
 * Progreso local (localStorage): historial, medallas, nivel, ranking personal, gráfico.
 */
const Progreso = (function () {
    const PREFIJO = "forjaexamenes";
    let sufijoUsuario = "";

    function clave(base) {
        return sufijoUsuario ? `${PREFIJO}-${base}-${sufijoUsuario}` : `${PREFIJO}-${base}`;
    }

    const CLAVE_HISTORIAL = () => clave("historial");
    const CLAVE_STATS = () => clave("stats");
    const CLAVE_MEDALLAS = () => clave("medallas");
    const MAX_HISTORIAL = 5;

    const MODULOS_BD = ["bd", "bd_sql", "bd_modelo", "bd_transacciones", "bd_jdbc"];
    const MODULOS_SISTEMAS = ["redes", "sistemas", "docker"];

    const MEDALLAS = [
        { id: "novato", icono: "🎓", nombre: "Novato", prueba: (s) => s.totalAprobados >= 1 },
        { id: "forjador", icono: "⚙️", nombre: "Forjador", prueba: (s) => s.totalAprobados >= 5 },
        { id: "maestro", icono: "🔥", nombre: "Maestro forjador", prueba: (s) => s.totalAprobados >= 10 }, // 10 ejercicios aprobados
        { id: "admin", icono: "🐧", nombre: "Administrador", prueba: (s) => MODULOS_SISTEMAS.every((m) => s.modulosAprobados[m]) },
        { id: "dba", icono: "💾", nombre: "DBA", prueba: (s) => ["bd_sql", "bd_modelo", "bd_jdbc"].every((m) => s.modulosAprobados[m]) },
        { id: "dockerizado", icono: "🐳", nombre: "Dockerizado", prueba: (s) => s.modulosAprobados.docker },
        { id: "completista", icono: "🏆", nombre: "Completista", prueba: (s) => s.modulosDistintos >= 10 },
    ];

    function leerJson(clave, defecto) {
        try {
            const raw = localStorage.getItem(clave);
            return raw ? JSON.parse(raw) : defecto;
        } catch (e) {
            return defecto;
        }
    }

    function guardarJson(clave, valor) {
        localStorage.setItem(clave, JSON.stringify(valor));
    }

    function obtenerStats() {
        return leerJson(CLAVE_STATS(), {
            porModulo: {},
            totalAprobados: 0,
            rachaActual: 0,
            modulosAprobados: {},
            modulosDistintos: 0,
        });
    }

    function calcularNivel(modulo) {
        const stats = obtenerStats();
        const pm = stats.porModulo[modulo] || { aprobados: 0, intentos: 0, mejorNota: 0 };
        if (pm.aprobados < 2) return 1;
        if (pm.aprobados < 5) return 2;
        return 3;
    }

    function registrarResultado(datos) {
        const { modulo, titulo, enunciado, nota, aprobado, tiempoSegundos, ejercicioId } = datos;
        const entrada = {
            modulo,
            titulo,
            enunciado: (enunciado || "").slice(0, 200),
            nota,
            aprobado,
            tiempoSegundos: tiempoSegundos || 0,
            ejercicioId,
            fecha: new Date().toISOString(),
        };

        let historial = leerJson(CLAVE_HISTORIAL(), []);
        historial.unshift(entrada);
        historial = historial.slice(0, MAX_HISTORIAL);
        guardarJson(CLAVE_HISTORIAL(), historial);

        const stats = obtenerStats();
        if (!stats.porModulo[modulo]) {
            stats.porModulo[modulo] = { intentos: 0, aprobados: 0, mejorNota: 0, sumaNotas: 0 };
        }
        const pm = stats.porModulo[modulo];
        pm.intentos += 1;
        pm.sumaNotas = (pm.sumaNotas || 0) + nota;
        pm.mediaNota = Math.round((pm.sumaNotas / pm.intentos) * 10) / 10;
        if (nota > pm.mejorNota) pm.mejorNota = nota;
        if (aprobado) {
            pm.aprobados += 1;
            stats.totalAprobados += 1;
            stats.rachaActual += 1;
            stats.modulosAprobados[modulo] = true;
        } else {
            stats.rachaActual = 0;
        }
        stats.modulosDistintos = Object.keys(stats.porModulo).length;
        guardarJson(CLAVE_STATS(), stats);

        actualizarMedallas(stats);
        return { entrada, stats, mensaje: mensajeMotivacion(modulo, nota, aprobado, pm) };
    }

    function actualizarMedallas(stats) {
        const obtenidas = leerJson(CLAVE_MEDALLAS(), []);
        const ids = new Set(obtenidas.map((m) => m.id));
        MEDALLAS.forEach((def) => {
            if (!ids.has(def.id) && def.prueba(stats)) {
                obtenidas.push({ id: def.id, icono: def.icono, nombre: def.nombre, fecha: new Date().toISOString() });
            }
        });
        guardarJson(CLAVE_MEDALLAS(), obtenidas);
        return obtenidas;
    }

    function mensajeMotivacion(modulo, nota, aprobado, pm) {
        const stats = obtenerStats();
        if (pm.intentos === 1) return "¡Primer ejercicio! El viaje empieza aquí.";
        if (nota > pm.mejorNota || (nota === pm.mejorNota && pm.intentos > 1 && nota === stats.porModulo[modulo].mejorNota)) {
            if (nota >= (pm.mejorNota || 0)) return "🔥 ¡Récord personal! Sigue así.";
        }
        if (!aprobado && pm.mejorNota > nota) return "Ánimo. Un mal día no define tu camino.";
        if (stats.rachaActual >= 3) return "🏅 ¡Racha de " + stats.rachaActual + "! Eres un máquina.";
        if (stats.totalAprobados >= 10) return "🎓 ¡Maestro forjador! Te has ganado el respeto.";
        const objetivo = objetivoDelDia();
        if (objetivo) return "🎯 Objetivo del día: " + objetivo;
        return aprobado ? "Bien hecho. ¿Otro ejercicio del mismo módulo?" : "Repasa los criterios marcados con ❌.";
    }

    function objetivoDelDia() {
        const stats = obtenerStats();
        let peor = null;
        Object.entries(stats.porModulo).forEach(([mod, pm]) => {
            if (!peor || pm.mediaNota < peor.media) peor = { modulo: mod, media: pm.mediaNota };
        });
        if (peor && peor.media < 7) return "Mejora tu nota en " + peor.modulo + " (media " + peor.media + ")";
        return null;
    }

    function obtenerHistorial() {
        return leerJson(CLAVE_HISTORIAL(), []);
    }

    function obtenerMedallas() {
        return leerJson(CLAVE_MEDALLAS(), []);
    }

    function rankingLocal() {
        const stats = obtenerStats();
        return Object.entries(stats.porModulo)
            .map(([modulo, pm]) => ({
                modulo,
                media: pm.mediaNota || 0,
                mejor: pm.mejorNota || 0,
                intentos: pm.intentos || 0,
            }))
            .sort((a, b) => b.media - a.media);
    }

    function datosGrafico(moduloFiltro) {
        const historial = obtenerHistorial().slice().reverse();
        const filtrado = moduloFiltro
            ? historial.filter((h) => h.modulo === moduloFiltro)
            : historial;
        return {
            labels: filtrado.map((h) => new Date(h.fecha).toLocaleDateString("es-ES", { day: "2-digit", month: "short" })),
            notas: filtrado.map((h) => h.nota),
        };
    }

    function renderizarPanelInicio() {
        const histUl = document.getElementById("lista-historial");
        const rankUl = document.getElementById("lista-ranking");
        const medDiv = document.getElementById("lista-medallas");
        if (!histUl) return;

        const historial = obtenerHistorial();
        histUl.innerHTML = historial.length
            ? historial.map((h) =>
                `<li><strong>${h.modulo}</strong> — ${h.nota}/10 ${h.aprobado ? "✓" : "✗"}<br><small>${h.titulo}</small></li>`
            ).join("")
            : "<li>Aún no hay ejercicios. ¡Empieza uno!</li>";

        if (rankUl) {
            const rank = rankingLocal();
            rankUl.innerHTML = rank.length
                ? rank.map((r) =>
                    `<li><strong>${r.modulo}</strong>: media ${r.media}/10 (mejor ${r.mejor}) — ${r.intentos} intentos</li>`
                ).join("")
                : "<li>Tu ranking aparecerá tras varios ejercicios.</li>";
        }

        if (medDiv) {
            const meds = obtenerMedallas();
            medDiv.innerHTML = MEDALLAS.map((def) => {
                const t = meds.find((m) => m.id === def.id);
                return `<span class="medalla ${t ? "medalla-ok" : "medalla-bloq"}" title="${def.nombre}">${def.icono}</span>`;
            }).join("");
        }

        dibujarGrafico(document.getElementById("grafico-progreso"), null);
    }

    let chartInstancia = null;

    function dibujarGrafico(canvas, modulo) {
        if (!canvas || typeof Chart === "undefined") return;
        const datos = datosGrafico(modulo);
        if (chartInstancia) chartInstancia.destroy();
        chartInstancia = new Chart(canvas, {
            type: "line",
            data: {
                labels: datos.labels,
                datasets: [{
                    label: "Nota",
                    data: datos.notas,
                    borderColor: "#4f46e5",
                    backgroundColor: "rgba(79, 70, 229, 0.15)",
                    tension: 0.3,
                    fill: true,
                }],
            },
            options: {
                responsive: true,
                scales: { y: { min: 0, max: 10 } },
                plugins: { legend: { display: false } },
            },
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        const pagina = document.querySelector(".pagina-inicio");
        if (pagina && pagina.dataset.loginUsuario) {
            sufijoUsuario = pagina.dataset.loginUsuario;
        }
        renderizarPanelInicio();
        const sel = document.getElementById("filtro-grafico-modulo");
        if (sel) {
            sel.addEventListener("change", function () {
                dibujarGrafico(document.getElementById("grafico-progreso"), sel.value || null);
            });
        }
    });

    return {
        calcularNivel,
        registrarResultado,
        obtenerHistorial,
        obtenerMedallas,
        rankingLocal,
        renderizarPanelInicio,
        mensajeMotivacion,
    };
})();
