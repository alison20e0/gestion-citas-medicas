package com.citasmedicas.seguridad;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.citasmedicas.shared.error.NoAutenticadoException;

/**
 * Identidad del usuario que atiende la peticion. Se arma en el filtro de
 * identidad a partir de los encabezados X-Usuario-Id y X-Rol.
 */
@Component
public class ContextoUsuario {

    private static final ThreadLocal<UsuarioActual> ACTUAL = new ThreadLocal<>();
    private static final ThreadLocal<String> MOTIVO_RECHAZO = new ThreadLocal<>();

    public void establecer(UsuarioActual usuario) {
        ACTUAL.set(usuario);
        MOTIVO_RECHAZO.remove();
    }

    public void rechazar(String motivo) {
        ACTUAL.remove();
        MOTIVO_RECHAZO.set(motivo);
    }

    public void limpiar() {
        ACTUAL.remove();
        MOTIVO_RECHAZO.remove();
    }

    public Optional<UsuarioActual> opcional() {
        return Optional.ofNullable(ACTUAL.get());
    }

    public Optional<String> motivoRechazo() {
        return Optional.ofNullable(MOTIVO_RECHAZO.get());
    }

    public UsuarioActual exigir() {
        UsuarioActual usuario = ACTUAL.get();
        if (usuario != null) {
            return usuario;
        }
        String motivo = MOTIVO_RECHAZO.get();
        throw new NoAutenticadoException(motivo == null
                ? "Faltan los encabezados X-Usuario-Id y X-Rol"
                : motivo);
    }

    public static UsuarioActual de(UUID id, Rol rol) {
        return UsuarioActual.de(id, rol);
    }
}