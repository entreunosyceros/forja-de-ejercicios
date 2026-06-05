/**
 * Polling del estado de indexación en segundo plano (portada).
 */
(function () {
    const panel = document.getElementById("panel-indexacion-curso");
    if (!panel) return;

    const mensajeEl = document.getElementById("indexacion-mensaje-dinamico");
    const intervalo = 2500;

    async function consultar() {
        try {
            const resp = await fetch("/documentacion/estado-indexacion", {
                headers: { "X-Requested-With": "XMLHttpRequest" },
            });
            if (!resp.ok) return;
            const datos = await resp.json();
            if (mensajeEl && datos.mensaje) {
                mensajeEl.textContent = datos.mensaje;
                mensajeEl.classList.toggle("alerta-ok", datos.exito === true);
                mensajeEl.classList.toggle("alerta-error", datos.exito !== true);
            }
            if (!datos.enCurso) {
                panel.hidden = true;
                if (datos.exito && datos.colecciones > 0) {
                    window.location.reload();
                }
                return false;
            }
            return true;
        } catch (_e) {
            return true;
        }
    }

    consultar();
    const timer = setInterval(async function () {
        const sigue = await consultar();
        if (!sigue) clearInterval(timer);
    }, intervalo);
})();
