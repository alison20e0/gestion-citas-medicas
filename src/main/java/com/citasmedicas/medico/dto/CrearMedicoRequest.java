package com.citasmedicas.medico.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.citasmedicas.medico.domain.Especialidad;

public record CrearMedicoRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9-]+") String documento,
        @NotBlank @Size(max = 80) String nombres,
        @NotBlank @Size(max = 80) String apellidos,
        @NotNull Especialidad especialidad,
        @Size(max = 32) String telefono,
        @Size(max = 160) String email,
        @NotEmpty List<@Valid HorarioRequest> horarios) {

    public record HorarioRequest(
            @NotNull DayOfWeek diaSemana,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFin,
            @NotNull @Min(5) @Max(240) Integer duracionMinutos) {
    }
}