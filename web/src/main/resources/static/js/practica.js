/**
 * Práctica infinita (AJAX), temporizador y registro de progreso local.
 */
(function () {
    let segundosTranscurridos = 0;
    let intervaloTemporizador = null;

    function formatearTiempo(seg) {
        const m = Math.floor(seg / 60);
        const s = seg % 60;
        return String(m).padStart(2, "0") + ":" + String(s).padStart(2, "0");
    }

    function iniciarTemporizador() {
        detenerTemporizador();
        segundosTranscurridos = 0;
        actualizarDisplay();
        intervaloTemporizador = setInterval(function () {
            segundosTranscurridos += 1;
            actualizarDisplay();
        }, 1000);
    }

    function detenerTemporizador() {
        if (intervaloTemporizador) {
            clearInterval(intervaloTemporizador);
            intervaloTemporizador = null;
        }
    }

    function actualizarDisplay() {
        const el = document.getElementById("temporizador");
        const input = document.getElementById("input-tiempo-segundos");
        if (el) el.textContent = "⏱ " + formatearTiempo(segundosTranscurridos);
        if (input) input.value = String(segundosTranscurridos);
    }

    function obtenerZona() {
        return document.getElementById("zona-practica");
    }

    async function cargarFragmento(url) {
        const resp = await fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } });
        if (!resp.ok) throw new Error("Error al cargar ejercicio: " + resp.status);
        return resp.text();
    }

    async function otroEjercicio(modulo, sorpresa) {
        const zonaActual = obtenerZona();
        const capitulo = zonaActual ? zonaActual.getAttribute("data-capitulo") : "";
        const seccion = zonaActual ? zonaActual.getAttribute("data-seccion") : "";
        const usaIa = typeof ForjaCarga !== "undefined" && ForjaCarga.esModuloGemini(modulo);
        let nivel = 2;
        if (!usaIa && typeof Progreso !== "undefined") {
            nivel = Progreso.calcularNivel(modulo);
        }
        let url = usaIa ? "/ejercicio/fragment/nuevo?" : "/ejercicio/fragment/nuevo?nivel=" + nivel;
        if (sorpresa) url += "&sorpresa=true";
        else if (modulo) url += "&modulo=" + encodeURIComponent(modulo);
        if (capitulo) url += "&capitulo=" + encodeURIComponent(capitulo);
        if (seccion) url += "&seccion=" + encodeURIComponent(seccion);

        if (usaIa) ForjaCarga.mostrar();

        try {
            const html = await cargarFragmento(url);
            const zona = obtenerZona();
            if (!zona) {
                window.location.href = sorpresa ? "/ejercicio/nuevo?sorpresa=true" : "/ejercicio/nuevo?modulo=" + modulo;
                return;
            }
            zona.outerHTML = html;
            enlazarEventos();
            iniciarTemporizador();
            window.scrollTo({ top: 0, behavior: "smooth" });
        } finally {
            if (usaIa) ForjaCarga.ocultar();
        }
    }

    function extraerTexto(selector, raiz) {
        const el = (raiz || document).querySelector(selector);
        return el ? el.textContent.trim() : "";
    }

    function registrarDesdeResultado(zona) {
        if (typeof Progreso === "undefined") return;
        const card = zona.querySelector(".score-card");
        if (!card) return;
        const nota = parseFloat(card.getAttribute("data-nota") || "0");
        const aprobado = card.getAttribute("data-aprobado") === "true";
        const modulo = zona.getAttribute("data-modulo");
        const titulo = extraerTexto("h1", zona);
        const enunciado = extraerTexto(".enunciado", zona) || titulo;
        const tiempo = parseInt(document.getElementById("input-tiempo-segundos")?.value || "0", 10);
        const res = Progreso.registrarResultado({
            modulo,
            titulo,
            enunciado,
            nota,
            aprobado,
            tiempoSegundos: tiempo,
            ejercicioId: zona.getAttribute("data-ejercicio-id"),
        });
        const msgEl = document.getElementById("mensaje-motivacion");
        if (msgEl) {
            msgEl.textContent = res.mensaje;
            msgEl.hidden = false;
            msgEl.classList.add("card");
        }
        if (document.getElementById("lista-historial")) {
            Progreso.renderizarPanelInicio();
        }
    }

    async function enviarEvaluacion(form) {
        detenerTemporizador();
        const action = form.getAttribute("data-action-evaluar");
        const fd = new FormData(form);
        fd.set("tiempoSegundos", String(segundosTranscurridos));

        const resp = await fetch(action, {
            method: "POST",
            body: fd,
            headers: { "X-Requested-With": "XMLHttpRequest" },
        });
        if (!resp.ok) throw new Error("Error al evaluar: " + resp.status);
        const html = await resp.text();
        const zona = obtenerZona();
        if (zona) {
            zona.outerHTML = html;
            const nueva = obtenerZona();
            if (nueva) registrarDesdeResultado(nueva);
            enlazarEventos();
        }
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function enlazarEventos() {
        const form = document.getElementById("formulario-ejercicio");
        if (form && !form.dataset.enlazado) {
            form.dataset.enlazado = "true";
            form.addEventListener("submit", function (ev) {
                ev.preventDefault();
                enviarEvaluacion(form).catch(function (err) {
                    alert(err.message || err);
                    iniciarTemporizador();
                });
            });
        }

        document.querySelectorAll(".btn-otro-ejercicio").forEach(function (btn) {
            btn.replaceWith(btn.cloneNode(true));
        });
        document.querySelectorAll(".btn-otro-ejercicio").forEach(function (btn) {
            btn.addEventListener("click", function () {
                const modulo = btn.getAttribute("data-modulo");
                otroEjercicio(modulo, false).catch(function (err) {
                    alert(err.message || err);
                });
            });
        });

        if (document.getElementById("temporizador") && !intervaloTemporizador) {
            iniciarTemporizador();
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        enlazarEventos();
        if (document.getElementById("temporizador")) {
            iniciarTemporizador();
        }
        const zonaRes = obtenerZona();
        if (zonaRes && zonaRes.querySelector(".score-card")) {
            registrarDesdeResultado(zonaRes);
        }
        const btnSorpresa = document.getElementById("btn-ejercicio-sorpresa");
        if (btnSorpresa) {
            btnSorpresa.addEventListener("click", function (ev) {
                ev.preventDefault();
                window.location.href = "/ejercicio/nuevo?sorpresa=true";
            });
        }
    });

    window.ForjaPractica = { otroEjercicio, iniciarTemporizador, detenerTemporizador };
})();
