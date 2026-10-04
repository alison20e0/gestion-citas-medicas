package com.citasmedicas.cita.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservarCitaRequest(
        @NotNull UUID pacienteId,
        @NotNull UUID medicoId,
        @NotNull @Future OffsetDateTime fechaHora,
        @Size(max = 500) String motivo) {
}