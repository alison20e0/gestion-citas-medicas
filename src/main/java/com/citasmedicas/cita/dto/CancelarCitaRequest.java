package com.citasmedicas.cita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarCitaRequest(
        @NotBlank @Size(max = 500) String motivo) {
}