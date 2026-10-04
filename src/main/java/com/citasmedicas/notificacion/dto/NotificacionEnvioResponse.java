package com.citasmedicas.notificacion.dto;

import java.time.Instant;
import java.util.UUID;

import com.citasmedicas.notificacion.domain.EstadoEnvio;
import com.citasmedicas.notificacion.domain.NotificacionEnvio;
import com.citasmedicas.notificacion.domain.NotificacionTipo;

public record NotificacionEnvioResponse(
        UUID id,
        UUID citaId,
        NotificacionTipo tipo,
        String referencia,
        String destinatario,
        String asunto,
        String cuerpo,
        EstadoEnvio estado,
        int intentos,
        String ultimoError,
        Instant proximoIntento,
        Instant enviadoEn,
        Instant creadoEn) {

    public static NotificacionEnvioResponse from(NotificacionEnvio envio) {
        return new NotificacionEnvioResponse(
                envio.getId(),
                envio.getCitaId(),
                envio.getTipo(),
                envio.getReferencia(),
                envio.getDestinatario(),
                envio.getAsunto(),
                envio.getCuerpo(),
                envio.getEstado(),
                envio.getIntentos(),
                envio.getUltimoError(),
                envio.getProximoIntento(),
                envio.getEnviadoEn(),
                envio.getCreadoEn());
    }
}