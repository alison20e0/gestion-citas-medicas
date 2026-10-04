package com.citasmedicas.cita.evento;

import java.util.UUID;

public record CitaCanceladaEvent(UUID citaId, String motivo) {
}