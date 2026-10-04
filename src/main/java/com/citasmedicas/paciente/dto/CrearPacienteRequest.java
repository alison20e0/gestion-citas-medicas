package com.citasmedicas.paciente.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearPacienteRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9-]+") String documento,
        @NotBlank @Size(max = 80) String nombres,
        @NotBlank @Size(max = 80) String apellidos,
        @Past LocalDate fechaNacimiento,
        @Size(max = 32) String telefono,
        @Email @Size(max = 160) String email) {
}