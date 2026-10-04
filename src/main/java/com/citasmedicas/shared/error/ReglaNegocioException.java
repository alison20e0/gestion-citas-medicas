package com.citasmedicas.shared.error;

public class ReglaNegocioException extends DomainException {

    public ReglaNegocioException(String mensaje) {
        super("REGLA_NEGOCIO", mensaje);
    }
}