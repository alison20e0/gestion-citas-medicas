package com.citasmedicas.auditoria.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.citasmedicas.auditoria.domain.TipoAccionAuditoria;

public record AuditarCitaRequest(
        @NotNull TipoAccionAuditoria accion,
        @Size(max = 1000) String detalle,
        @Size(max = 120) String usuario) {
}