package com.citasmedicas.notificacion;

import com.citasmedicas.notificacion.domain.NotificacionTipo;

public interface CanalNotificacion {

    NotificacionTipo tipo();

    void enviar(NotificacionMensaje mensaje);
}