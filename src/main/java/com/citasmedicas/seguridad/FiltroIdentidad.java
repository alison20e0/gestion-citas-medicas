package com.citasmedicas.seguridad;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee la identidad simulada de los encabezados X-Usuario-Id y X-Rol.
 * Solo analiza peticiones de /api/**; actuator queda abierto para el healthcheck.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroIdentidad extends OncePerRequestFilter {

    public static final String ENCABEZADO_USUARIO = "X-Usuario-Id";
    public static final String ENCABEZADO_ROL = "X-Rol";

    private static final String PREFIJO_API = "/api/";

    private final ContextoUsuario contexto;

    public FiltroIdentidad(ContextoUsuario contexto) {
        this.contexto = contexto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (request.getRequestURI().startsWith(PREFIJO_API)) {
                interpretarEncabezados(request);
            }
            chain.doFilter(request, response);
        } finally {
            contexto.limpiar();
        }
    }

    private void interpretarEncabezados(HttpServletRequest request) {
        String usuario = texto(request.getHeader(ENCABEZADO_USUARIO));
        String rol = texto(request.getHeader(ENCABEZADO_ROL));
        if (usuario.isEmpty() && rol.isEmpty()) {
            return;
        }
        if (usuario.isEmpty() || rol.isEmpty()) {
            contexto.rechazar("Se requieren los encabezados " + ENCABEZADO_USUARIO + " y " + ENCABEZADO_ROL);
            return;
        }
        UUID usuarioId;
        try {
            usuarioId = UUID.fromString(usuario);
        } catch (IllegalArgumentException ex) {
            contexto.rechazar("El encabezado " + ENCABEZADO_USUARIO + " debe ser un UUID valido");
            return;
        }
        try {
            contexto.establecer(UsuarioActual.de(usuarioId, Rol.valueOf(rol.toUpperCase(Locale.ROOT))));
        } catch (IllegalArgumentException ex) {
            contexto.rechazar("El encabezado " + ENCABEZADO_ROL + " debe ser uno de PACIENTE, RECEPCION, MEDICO o ADMIN");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}