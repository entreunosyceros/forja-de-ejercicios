/**
 * Pantalla de carga mientras Gemini genera ejercicios desde apuntes (módulos docs_*).
 */
const ForjaCarga = (function () {
    const TITULO_DEFECTO = "Generando ejercicios...";
    const SUB_DEFECTO =
        "La IA está creando tu ejercicio a partir de los apuntes. Puede tardar unos segundos.";

    let overlay = null;

    function obtenerOverlay() {
        if (!overlay) {
            overlay = document.getElementById("overlay-carga-ia");
        }
        return overlay;
    }

    function mostrar(titulo, submensaje) {
        const el = obtenerOverlay();
        if (!el) return;

        const tituloEl = el.querySelector(".overlay-carga-ia-titulo");
        const subEl = el.querySelector(".overlay-carga-ia-sub");
        if (tituloEl) tituloEl.textContent = titulo || TITULO_DEFECTO;
        if (subEl) subEl.textContent = submensaje || SUB_DEFECTO;

        el.hidden = false;
        document.body.classList.add("carga-ia-activa");
    }

    function ocultar() {
        const el = obtenerOverlay();
        if (!el) return;
        el.hidden = true;
        document.body.classList.remove("carga-ia-activa");
    }

    function esModuloGemini(modulo) {
        return typeof modulo === "string" && modulo.startsWith("docs_");
    }

    function esEnlaceGemini(href) {
        if (!href) return false;
        try {
            const url = new URL(href, window.location.origin);
            return esModuloGemini(url.searchParams.get("modulo"));
        } catch (e) {
            return href.includes("modulo=docs_");
        }
    }

    function enlazarEnlacesGemini() {
        document.querySelectorAll('a[href*="/ejercicio/nuevo"]').forEach(function (enlace) {
            if (!esEnlaceGemini(enlace.getAttribute("href"))) return;
            if (enlace.dataset.cargaIaEnlazado) return;
            enlace.dataset.cargaIaEnlazado = "true";
            enlace.addEventListener("click", function () {
                mostrar();
            });
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        ocultar();
        enlazarEnlacesGemini();
    });

    window.addEventListener("pageshow", ocultar);

    return {
        mostrar: mostrar,
        ocultar: ocultar,
        esModuloGemini: esModuloGemini,
    };
})();
