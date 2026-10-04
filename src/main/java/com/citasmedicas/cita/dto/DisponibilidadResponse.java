package com.citasmedicas.cita.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.citasmedicas.medico.domain.Especialidad;

public record DisponibilidadResponse(
        UUID medicoId,
        String medico,
        Especialidad especialidad,
        LocalDate fecha,
        int totalSlots,
        int slotsDisponibles,
        List<SlotResponse> slots) {

    public record SlotResponse(
            OffsetDateTime inicio,
            OffsetDateTime fin,
            int duracionMinutos,
            boolean disponible,
            UUID citaId) {
    }
}