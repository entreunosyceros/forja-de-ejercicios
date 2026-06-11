/**
 * Resaltado de sintaxis en soluciones (textarea editable + bloques estáticos).
 */
(function () {
    const SELECTOR_EDITOR = "textarea.editor-codigo-input";
    const SELECTOR_BLOQUE = ".code-block-resaltado code";

    function resolverLenguaje(modulo) {
        if (!modulo) return "plaintext";
        const mod = modulo.trim().toLowerCase();
        if (mod.startsWith("bd") || mod.includes("sql")) return "sql";
        if (mod === "poo" || mod.includes("java")) return "java";
        if (mod === "docker" || mod === "git" || mod === "redes" || mod === "sistemas") return "bash";
        return "plaintext";
    }

    function obtenerModulo(raiz) {
        const zona = (raiz || document).querySelector("#zona-practica");
        return zona ? zona.getAttribute("data-modulo") : null;
    }

    function resaltarCodigo(codeEl, lenguaje) {
        if (!codeEl) return;
        codeEl.className = "language-" + lenguaje;
        codeEl.removeAttribute("data-highlighted");
        if (lenguaje === "plaintext" || typeof hljs === "undefined") return;
        hljs.highlightElement(codeEl);
    }

    function ajustarAltura(textarea, pre) {
        // El textarea crece con el contenido para no tener scroll interno:
        // así la capa de resaltado (pre con inset:0) queda siempre alineada.
        textarea.style.height = "auto";
        const altura = Math.max(textarea.scrollHeight, 0);
        textarea.style.height = altura + "px";
        if (pre) pre.style.height = altura + "px";
    }

    function actualizarEditor(textarea, codeEl, lenguaje) {
        const valor = textarea.value;
        // El "\n" final evita que la última línea quede recortada en el overlay.
        codeEl.textContent = valor.length ? valor + "\n" : "";
        resaltarCodigo(codeEl, lenguaje);
        const pre = codeEl.closest(".editor-codigo-highlight");
        ajustarAltura(textarea, pre);
    }

    function envolverTextarea(textarea, lenguaje) {
        if (textarea.dataset.editorCodigo === "true") {
            return textarea.closest(".editor-codigo");
        }
        const wrapper = document.createElement("div");
        wrapper.className = "editor-codigo";
        const pre = document.createElement("pre");
        pre.className = "editor-codigo-highlight";
        pre.setAttribute("aria-hidden", "true");
        const code = document.createElement("code");
        code.className = "language-" + lenguaje;
        pre.appendChild(code);
        textarea.parentNode.insertBefore(wrapper, textarea);
        wrapper.appendChild(pre);
        wrapper.appendChild(textarea);
        textarea.classList.add("editor-codigo-input");
        textarea.dataset.editorCodigo = "true";

        let timer = null;
        function programarActualizacion() {
            if (timer) clearTimeout(timer);
            timer = setTimeout(function () {
                actualizarEditor(textarea, code, lenguaje);
            }, 60);
        }

        textarea.addEventListener("input", programarActualizacion);
        window.addEventListener("resize", programarActualizacion);

        actualizarEditor(textarea, code, lenguaje);
        return wrapper;
    }

    function inicializarEditores(raiz) {
        if (typeof hljs === "undefined") return;
        const modulo = obtenerModulo(raiz);
        const lenguaje = resolverLenguaje(modulo);
        const scope = raiz || document;
        scope.querySelectorAll("#textarea-respuesta, #textarea-solucion-profesor").forEach(function (ta) {
            if (ta.dataset.editorCodigo === "true") return;
            envolverTextarea(ta, lenguaje);
        });
    }

    function refrescarEditor(textarea) {
        if (!textarea || textarea.dataset.editorCodigo !== "true") return;
        const wrapper = textarea.closest(".editor-codigo");
        const code = wrapper ? wrapper.querySelector("code") : null;
        if (!code) return;
        const lenguaje = resolverLenguaje(obtenerModulo(document));
        actualizarEditor(textarea, code, lenguaje);
    }

    function inicializarBloques(raiz) {
        if (typeof hljs === "undefined") return;
        const scope = raiz || document;
        scope.querySelectorAll(SELECTOR_BLOQUE).forEach(function (bloque) {
            if (bloque.dataset.resaltado === "true") return;
            bloque.dataset.resaltado = "true";
            const clase = Array.from(bloque.classList).find(function (c) {
                return c.startsWith("language-");
            });
            const lenguaje = clase ? clase.slice("language-".length) : "plaintext";
            resaltarCodigo(bloque, lenguaje);
        });
    }

    function inicializar(raiz) {
        inicializarEditores(raiz);
        inicializarBloques(raiz);
    }

    window.ForjaEditorCodigo = {
        inicializar,
        refrescarEditor,
        resolverLenguaje,
    };
})();
