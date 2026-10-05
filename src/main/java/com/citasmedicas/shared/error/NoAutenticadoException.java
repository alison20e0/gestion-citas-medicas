package com.citasmedicas.shared.error;

/**
 * No se pudo determinar la identidad del usuario (encabezados ausentes o invalidos).
 * Se responde 401.
 */
public class NoAutenticadoException extends DomainException {

    public NoAutenticadoException(String mensaje) {
        super("NO_AUTENTICADO", mensaje);
    }
}