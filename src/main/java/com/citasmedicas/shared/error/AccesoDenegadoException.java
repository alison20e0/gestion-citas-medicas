package com.citasmedicas.shared.error;

/**
 * El rol autenticado no tiene permiso sobre el recurso solicitado.
 * Se responde 403 y el intento queda registrado en la auditoria.
 */
public class AccesoDenegadoException extends DomainException {

    public AccesoDenegadoException(String mensaje) {
        super("ACCESO_DENEGADO", mensaje);
    }
}