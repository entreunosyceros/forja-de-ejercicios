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
            if (typeof ForjaEditorCodigo !== "undefined") {
                ForjaEditorCodigo.inicializar(obtenerZona());
            }
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
        if (typeof Progreso === "undefined" || !zona) return;
        const res = Progreso.registrarDesdeZona(zona);
        if (!res) return;
        const msgEl = document.getElementById("mensaje-motivacion");
        if (msgEl && res.mensaje) {
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
        let mensajeDificultad = null;
        if (typeof Progreso !== "undefined") {
            const resCab = Progreso.registrarDesdeCabecera(resp);
            if (resCab?.mensaje) mensajeDificultad = resCab.mensaje;
        }
        const html = await resp.text();
        const zona = obtenerZona();
        if (zona) {
            zona.outerHTML = html;
            const nueva = obtenerZona();
            if (nueva) {
                registrarDesdeResultado(nueva);
                if (mensajeDificultad) {
                    const msgEl = document.getElementById("mensaje-motivacion");
                    if (msgEl) {
                        msgEl.textContent = mensajeDificultad;
                        msgEl.hidden = false;
                        msgEl.classList.add("card");
                    }
                }
            }
            enlazarEventos();
            if (typeof ForjaEditorCodigo !== "undefined") {
                ForjaEditorCodigo.inicializar(nueva);
            }
        }
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function obtenerTokenCsrf() {
        const input = document.querySelector('input[name="_csrf"]');
        return input ? { nombre: input.name, valor: input.value } : null;
    }

    function mostrarMensajeSolucionProfesor(texto, esError) {
        const msg = document.getElementById("mensaje-solucion-profesor");
        if (!msg) return;
        msg.textContent = texto;
        msg.hidden = !texto;
        msg.classList.remove("ok", "error");
        msg.classList.add(esError ? "error" : "ok");
    }

    async function guardarSolucionProfesor() {
        const card = document.querySelector(".solucion-profesor-card");
        const textarea = document.getElementById("textarea-solucion-profesor");
        if (!card || !textarea) return;
        const action = card.getAttribute("data-action-guardar-solucion");
        if (!action) return;
        const csrf = obtenerTokenCsrf();
        const fd = new FormData();
        fd.set("solucionReferencia", textarea.value);
        if (csrf) fd.set(csrf.nombre, csrf.valor);
        const resp = await fetch(action, {
            method: "POST",
            body: fd,
            headers: { "X-Requested-With": "XMLHttpRequest" },
        });
        if (!resp.ok) {
            const detalle = await resp.text();
            throw new Error(detalle || "No se pudo guardar la solución");
        }
        const datos = await resp.json();
        mostrarMensajeSolucionProfesor(datos.mensaje || "Solución guardada.", false);
    }

    function copiarSolucionARespuesta() {
        const origen = document.getElementById("textarea-solucion-profesor");
        const destino = document.getElementById("textarea-respuesta");
        if (!origen || !destino) return;
        destino.value = origen.value;
        if (typeof ForjaEditorCodigo !== "undefined") {
            ForjaEditorCodigo.refrescarEditor(destino);
        }
        destino.focus();
        mostrarMensajeSolucionProfesor("Copiado a «Tu solución». Pulsa Enviar y corregir para probar.", false);
    }

    function enlazarEventosProfesor() {
        document.querySelectorAll(".btn-guardar-solucion-profesor").forEach(function (btn) {
            if (btn.dataset.enlazado) return;
            btn.dataset.enlazado = "true";
            btn.addEventListener("click", function () {
                guardarSolucionProfesor().catch(function (err) {
                    mostrarMensajeSolucionProfesor(err.message || String(err), true);
                });
            });
        });
        document.querySelectorAll(".btn-copiar-solucion-respuesta").forEach(function (btn) {
            if (btn.dataset.enlazado) return;
            btn.dataset.enlazado = "true";
            btn.addEventListener("click", copiarSolucionARespuesta);
        });
    }

    function enlazarEventos() {
        enlazarEventosProfesor();
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

        if (typeof ForjaEditorCodigo !== "undefined") {
            ForjaEditorCodigo.inicializar(obtenerZona());
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
