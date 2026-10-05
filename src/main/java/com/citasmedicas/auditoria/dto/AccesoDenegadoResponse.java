package com.citasmedicas.auditoria.dto;

import java.time.Instant;
import java.util.UUID;

import com.citasmedicas.auditoria.domain.AuditoriaAccesoDenegado;
import com.citasmedicas.seguridad.Rol;

public record AccesoDenegadoResponse(
        UUID id,
        UUID usuarioId,
        Rol rol,
        String metodo,
        String ruta,
        String motivo,
        String direccionIp,
        Instant fechaRegistro) {

    public static AccesoDenegadoResponse from(AuditoriaAccesoDenegado auditoria) {
        return new AccesoDenegadoResponse(
                auditoria.getId(),
                auditoria.getUsuarioId(),
                auditoria.getRol(),
                auditoria.getMetodo(),
                auditoria.getRuta(),
                auditoria.getMotivo(),
                auditoria.getDireccionIp(),
                auditoria.getFechaRegistro());
    }
}