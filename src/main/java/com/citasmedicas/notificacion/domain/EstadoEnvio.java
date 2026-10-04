package com.citasmedicas.notificacion.domain;

public enum EstadoEnvio {
    PENDIENTE,
    ENVIADO,
    FALLIDO,
    DESCARTADO;

    public boolean esTerminal() {
        return this == ENVIADO || this == DESCARTADO;
    }
}