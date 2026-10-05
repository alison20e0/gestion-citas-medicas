package com.citasmedicas.seguridad;

import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.citasmedicas.shared.error.AccesoDenegadoException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Valida la identidad y el rol antes de ejecutar el endpoint.
 * Sin identidad responde 401, con rol no autorizado responde 403.
 */
@Component
public class InterceptorPermisos implements HandlerInterceptor {

    private final ContextoUsuario contexto;

    public InterceptorPermisos(ContextoUsuario contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        UsuarioActual usuario = contexto.exigir();
        Permitido permiso = metodo.getMethodAnnotation(Permitido.class);
        if (permiso == null) {
            permiso = metodo.getBeanType().getAnnotation(Permitido.class);
        }
        if (permiso == null) {
            return true;
        }
        if (!Arrays.asList(permiso.value()).contains(usuario.rol())) {
            throw new AccesoDenegadoException("El rol " + usuario.rol() + " no tiene permiso sobre "
                    + request.getMethod() + " " + request.getRequestURI());
        }
        return true;
    }
}