package com.citasmedicas.shared.error;

public class NotFoundException extends DomainException {

    public NotFoundException(String mensaje) {
        super("RECURSO_NO_ENCONTRADO", mensaje);
    }

    public NotFoundException(String recurso, Object id) {
        super("RECURSO_NO_ENCONTRADO", recurso + " con id " + id + " no existe");
    }
}