package com.citasmedicas.auditoria.dto;

import java.time.Instant;
import java.util.UUID;

import com.citasmedicas.auditoria.domain.AuditoriaCita;
import com.citasmedicas.auditoria.domain.TipoAccionAuditoria;

public record AuditoriaCitaResponse(
        UUID id,
        UUID citaId,
        TipoAccionAuditoria accion,
        String estadoAnterior,
        String estadoNuevo,
        String detalle,
        String usuario,
        String direccionIp,
        Instant fechaRegistro) {

    public static AuditoriaCitaResponse from(AuditoriaCita auditoria) {
        return new AuditoriaCitaResponse(
                auditoria.getId(),
                auditoria.getCita().getId(),
                auditoria.getAccion(),
                auditoria.getEstadoAnterior(),
                auditoria.getEstadoNuevo(),
                auditoria.getDetalle(),
                auditoria.getUsuario(),
                auditoria.getDireccionIp(),
                auditoria.getFechaRegistro());
    }
}