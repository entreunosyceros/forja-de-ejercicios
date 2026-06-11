(function () {
    const CLAVE = "forjaexamenes-tema";
    const raiz = document.documentElement;
    const botones = document.querySelectorAll(".btn-tema");

    function aplicar(tema) {
        const elegido = tema === "oscuro" ? "oscuro" : "claro";
        raiz.setAttribute("data-theme", elegido);
        raiz.style.colorScheme = elegido;
        localStorage.setItem(CLAVE, elegido);

        botones.forEach(function (boton) {
            const activo = boton.getAttribute("data-tema") === elegido;
            boton.classList.toggle("activo", activo);
            boton.setAttribute("aria-pressed", activo ? "true" : "false");
        });
    }

    const guardado = localStorage.getItem(CLAVE);
    const prefiereOscuro = window.matchMedia("(prefers-color-scheme: dark)").matches;
    aplicar(guardado || (prefiereOscuro ? "oscuro" : "claro"));

    botones.forEach(function (boton) {
        boton.addEventListener("click", function () {
            aplicar(boton.getAttribute("data-tema"));
        });
    });

    window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function (evento) {
        if (!localStorage.getItem(CLAVE)) {
            aplicar(evento.matches ? "oscuro" : "claro");
        }
    });
})();
