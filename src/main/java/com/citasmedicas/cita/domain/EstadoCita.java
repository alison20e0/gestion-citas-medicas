package com.citasmedicas.cita.domain;

public enum EstadoCita {
    CONFIRMADA,
    CANCELADA,
    FINALIZADA,
    NO_ASISTIO;

    public boolean esTerminal() {
        return this != CONFIRMADA;
    }
}