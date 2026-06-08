document.addEventListener("DOMContentLoaded", function () {
    if (typeof hljs === "undefined") {
        return;
    }
    document.querySelectorAll(".code-block-resaltado code").forEach(function (bloque) {
        hljs.highlightElement(bloque);
    });
});
