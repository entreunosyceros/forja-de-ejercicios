/**
 * Formulario de subida de PDF en la portada (tema nuevo + overlay de carga).
 */
(function () {
    const form = document.getElementById("form-subida-pdf");
    const selectTema = document.getElementById("tema-select");
    const campoNuevo = document.getElementById("campo-tema-nuevo");
    const inputNuevo = document.getElementById("tema-nuevo");

    if (selectTema && campoNuevo) {
        function actualizarTemaNuevo() {
            const esNuevo = selectTema.value === "__nuevo__";
            campoNuevo.hidden = !esNuevo;
            if (inputNuevo) {
                inputNuevo.required = esNuevo;
            }
        }
        selectTema.addEventListener("change", actualizarTemaNuevo);
        actualizarTemaNuevo();
    }

    if (form) {
        form.addEventListener("submit", function () {
            const btn = document.getElementById("btn-subir-pdf");
            if (btn) {
                btn.disabled = true;
                btn.textContent = "Subiendo e indexando…";
            }
            if (typeof ForjaCarga !== "undefined") {
                ForjaCarga.mostrar(
                    "Indexando apuntes…",
                    "Guardando PDF y actualizando el índice. Puede tardar unos segundos."
                );
            }
        });
    }
})();
