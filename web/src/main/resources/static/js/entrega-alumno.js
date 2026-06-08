/**
 * Exporta estadísticas del servidor + progreso local para la carpeta compartida con el profesor.
 */
(function () {
    function exportarProgresoLocal() {
        if (typeof Progreso === "undefined") {
            return null;
        }
        if (typeof Progreso.exportarParaEntrega === "function") {
            return Progreso.exportarParaEntrega();
        }
        return {
            historial: Progreso.obtenerHistorial ? Progreso.obtenerHistorial() : [],
            stats: {},
            medallas: Progreso.obtenerMedallas ? Progreso.obtenerMedallas() : [],
        };
    }

    function descargarBlob(nombre, contenido) {
        const blob = new Blob([contenido], { type: "application/json;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement("a");
        enlace.href = url;
        enlace.download = nombre;
        enlace.click();
        URL.revokeObjectURL(url);
    }

    async function exportarEntrega() {
        const btn = document.getElementById("btn-exportar-entrega");
        if (btn) {
            btn.disabled = true;
            btn.textContent = "Generando entrega…";
        }
        try {
            const respuesta = await fetch("/entrega/exportar.json");
            if (!respuesta.ok) {
                throw new Error("No se pudo generar la entrega (" + respuesta.status + ")");
            }
            const datos = await respuesta.json();
            const progresoLocal = exportarProgresoLocal();
            if (progresoLocal) {
                datos.progresoLocal = progresoLocal;
            }
            const login = datos?.alumno?.login || "alumno";
            const fecha = new Date().toISOString().slice(0, 10);
            const nombre = "entrega-" + login + "-" + fecha + ".json";
            descargarBlob(nombre, JSON.stringify(datos, null, 2));
        } catch (err) {
            console.error("Forja: error exportando entrega", err);
            alert("No se pudo generar la entrega. ¿Sigues con la sesión iniciada?");
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.textContent = "Descargar entrega para el profesor";
            }
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        const btn = document.getElementById("btn-exportar-entrega");
        if (btn) {
            btn.addEventListener("click", exportarEntrega);
        }
    });
})();
