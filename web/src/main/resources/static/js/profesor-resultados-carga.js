/**
 * Paginación virtual en tablas de resultados: muestra N filas y «Cargar más» sin peticiones al servidor.
 */
(function () {
    var TAMANO_BLOQUE = 100;

    function inicializarTabla(contenedor) {
        var tabla = contenedor.querySelector("table");
        if (!tabla) {
            return;
        }
        var filas = Array.prototype.slice.call(tabla.querySelectorAll("tbody tr"));
        if (filas.length <= TAMANO_BLOQUE) {
            return;
        }

        var visibles = TAMANO_BLOQUE;
        var info = document.createElement("p");
        info.className = "hint tabla-resultados-info";
        contenedor.appendChild(info);

        var acciones = document.createElement("div");
        acciones.className = "tabla-resultados-mas form-actions";
        var boton = document.createElement("button");
        boton.type = "button";
        boton.className = "btn btn-secondary btn-sm";
        acciones.appendChild(boton);
        contenedor.appendChild(acciones);

        function actualizar() {
            filas.forEach(function (fila, indice) {
                fila.hidden = indice >= visibles;
            });
            var mostradas = Math.min(visibles, filas.length);
            info.textContent = "Mostrando " + mostradas + " de " + filas.length + " intentos (más recientes primero).";
            if (visibles >= filas.length) {
                acciones.hidden = true;
            } else {
                acciones.hidden = false;
                var restantes = Math.min(TAMANO_BLOQUE, filas.length - visibles);
                boton.textContent = "Cargar " + restantes + " más";
            }
        }

        boton.addEventListener("click", function () {
            visibles = Math.min(visibles + TAMANO_BLOQUE, filas.length);
            actualizar();
        });

        actualizar();
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".tabla-resultados-virtual").forEach(inicializarTabla);
    });
})();
