/**
 * Tabla comparativa y gráficas de avance de alumnos importados (profesor).
 */
(function () {
    const COLORES = [
        "#4f46e5", "#0891b2", "#059669", "#d97706", "#dc2626",
        "#7c3aed", "#db2777", "#2563eb", "#65a30d", "#ea580c",
    ];

    let comparativa = { alumnos: [], modulos: [] };
    let chartResumen = null;
    let chartEvolucion = null;

    function leerComparativa() {
        const seccion = document.querySelector(".pagina-profesor-alumnos");
        const raw = seccion?.dataset?.comparativa;
        if (!raw) return;
        try {
            comparativa = JSON.parse(raw);
        } catch (e) {
            console.warn("Forja: datos comparativa no válidos", e);
        }
    }

    function idsSeleccionados() {
        const todos = document.getElementById("check-todos-alumnos");
        if (todos?.checked) {
            return new Set((comparativa.alumnos || []).map((a) => a.id));
        }
        const ids = new Set();
        document.querySelectorAll(".check-alumno-grafico:checked").forEach((cb) => {
            if (cb.value) ids.add(cb.value);
        });
        return ids;
    }

    function alumnosFiltrados() {
        const ids = idsSeleccionados();
        return (comparativa.alumnos || []).filter((a) => ids.has(a.id));
    }

    function etiquetaMetrica(metrica) {
        const mapa = {
            notaMedia: "Nota media",
            totalIntentos: "Ejercicios",
            totalAprobados: "Aprobados",
            mejorNota: "Mejor nota",
        };
        return mapa[metrica] || metrica;
    }

    function dibujarResumen() {
        const canvas = document.getElementById("grafico-resumen-clase");
        if (!canvas || typeof Chart === "undefined") return;

        const metrica = document.getElementById("grafico-metrica")?.value || "notaMedia";
        const filas = alumnosFiltrados();
        const labels = filas.map((a) => a.nombreEtiqueta);
        const datos = filas.map((a) => a[metrica] ?? 0);

        if (chartResumen) chartResumen.destroy();
        chartResumen = new Chart(canvas, {
            type: "bar",
            data: {
                labels,
                datasets: [{
                    label: etiquetaMetrica(metrica),
                    data: datos,
                    backgroundColor: COLORES.map((c) => c + "99"),
                    borderColor: COLORES,
                    borderWidth: 1,
                }],
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: metrica === "notaMedia" || metrica === "mejorNota" ? 10 : undefined,
                    },
                },
                plugins: { legend: { display: false } },
            },
        });
    }

    function dibujarEvolucion() {
        const canvas = document.getElementById("grafico-evolucion-clase");
        if (!canvas || typeof Chart === "undefined") return;

        const filas = alumnosFiltrados();
        let maxLen = 0;
        filas.forEach((a) => {
            if (a.ultimasNotas?.length > maxLen) maxLen = a.ultimasNotas.length;
        });
        if (maxLen === 0) maxLen = 1;

        const labels = Array.from({ length: maxLen }, (_, i) => "Ej. " + (i + 1));
        const datasets = filas.map((alumno, idx) => ({
            label: alumno.nombreEtiqueta,
            data: alumno.ultimasNotas || [],
            borderColor: COLORES[idx % COLORES.length],
            backgroundColor: COLORES[idx % COLORES.length] + "22",
            tension: 0.25,
            fill: false,
            spanGaps: true,
        }));

        if (chartEvolucion) chartEvolucion.destroy();
        chartEvolucion = new Chart(canvas, {
            type: "line",
            data: { labels, datasets },
            options: {
                responsive: true,
                scales: { y: { min: 0, max: 10 } },
                plugins: {
                    legend: {
                        display: filas.length <= 8,
                        position: "bottom",
                    },
                },
            },
        });
    }

    function actualizarGraficos() {
        dibujarResumen();
        dibujarEvolucion();
    }

    function sincronizarCheckTodos() {
        const todos = document.getElementById("check-todos-alumnos");
        const checks = document.querySelectorAll(".check-alumno-grafico");
        if (!todos) return;

        todos.addEventListener("change", function () {
            const marcar = todos.checked;
            checks.forEach((cb) => {
                cb.checked = marcar;
                cb.disabled = marcar;
            });
            actualizarGraficos();
        });

        checks.forEach((cb) => {
            cb.addEventListener("change", function () {
                if (todos.checked) return;
                const alguno = Array.from(checks).some((c) => c.checked);
                if (!alguno) cb.checked = true;
                actualizarGraficos();
            });
        });

        if (todos.checked) {
            checks.forEach((cb) => { cb.disabled = true; });
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        leerComparativa();
        if (!comparativa.alumnos?.length) return;

        sincronizarCheckTodos();

        const selMetrica = document.getElementById("grafico-metrica");
        if (selMetrica) {
            selMetrica.addEventListener("change", dibujarResumen);
        }

        actualizarGraficos();
    });
})();
