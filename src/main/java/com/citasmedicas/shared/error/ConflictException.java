package com.citasmedicas.shared.error;

public class ConflictException extends DomainException {

    public ConflictException(String mensaje) {
        super("CONFLICTO", mensaje);
    }

    public ConflictException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    public ConflictException(String codigo, String mensaje, Throwable causa) {
        super(codigo, mensaje, causa);
    }
}