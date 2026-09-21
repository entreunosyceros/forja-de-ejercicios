// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite simple en memoria para login y corrección (aula compartida / abuso accidental).
 * No sustituye un WAF; evita ráfagas locales.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class FiltroLimitePeticiones extends OncePerRequestFilter {

    private static final int MAX_LOGIN = 20;
    private static final int MAX_EVALUAR = 60;
    private static final long VENTANA_MS = 60_000L;

    private final Map<String, Deque<Long>> ventanas = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String metodo = request.getMethod();
        int max = 0;
        if ("POST".equalsIgnoreCase(metodo) && path != null) {
            if (path.equals("/login") || path.endsWith("/login")) {
                max = MAX_LOGIN;
            } else if (path.contains("/ejercicio/")
                    && (path.endsWith("/evaluar") || path.endsWith("/evaluar-ajax"))) {
                max = MAX_EVALUAR;
            }
        }
        if (max > 0) {
            String clave = max + "|" + clientKey(request) + "|" + path;
            if (!permitir(clave, max)) {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Demasiadas peticiones; espera un minuto e inténtalo de nuevo.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "local";
    }

    private boolean permitir(String clave, int max) {
        long ahora = System.currentTimeMillis();
        Deque<Long> cola = ventanas.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            while (!cola.isEmpty() && ahora - cola.peekFirst() > VENTANA_MS) {
                cola.removeFirst();
            }
            if (cola.size() >= max) {
                return false;
            }
            cola.addLast(ahora);
            return true;
        }
    }
}
