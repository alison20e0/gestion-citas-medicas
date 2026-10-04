package com.citasmedicas.cita.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.citasmedicas.cita.domain.Cita;
import com.citasmedicas.cita.domain.EstadoCita;

public record CitaResponse(
        UUID id,
        UUID pacienteId,
        String paciente,
        UUID medicoId,
        String medico,
        String especialidad,
        OffsetDateTime fechaHora,
        OffsetDateTime fin,
        int duracionMinutos,
        EstadoCita estado,
        String motivo,
        String motivoCancelacion,
        Instant canceladaEn,
        long version,
        Instant creadaEn) {

    public static CitaResponse from(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getPaciente().getId(),
                cita.getPaciente().nombreCompleto(),
                cita.getMedico().getId(),
                cita.getMedico().nombreCompleto(),
                cita.getMedico().getEspecialidad().name(),
                cita.getFechaHora(),
                cita.getFechaHora().plusMinutes(cita.getDuracionMinutos()),
                cita.getDuracionMinutos(),
                cita.getEstado(),
                cita.getMotivo(),
                cita.getMotivoCancelacion(),
                cita.getCanceladaEn(),
                cita.getVersion() == null ? 0L : cita.getVersion(),
                cita.getCreatedAt());
    }
}