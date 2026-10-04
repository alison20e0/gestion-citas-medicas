package com.citasmedicas.notificacion;

public class NotificacionFallidaException extends RuntimeException {

    private final String codigo;

    public NotificacionFallidaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}