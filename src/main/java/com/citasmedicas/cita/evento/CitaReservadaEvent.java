package com.citasmedicas.cita.evento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CitaReservadaEvent(UUID citaId, UUID pacienteId, UUID medicoId, OffsetDateTime fechaHora) {
}