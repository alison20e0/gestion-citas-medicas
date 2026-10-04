package com.citasmedicas.paciente.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.citasmedicas.paciente.domain.Paciente;

public record PacienteResponse(
        UUID id,
        String documento,
        String nombres,
        String apellidos,
        LocalDate fechaNacimiento,
        String telefono,
        String email,
        boolean activo) {

    public static PacienteResponse from(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getDocumento(),
                paciente.getNombres(),
                paciente.getApellidos(),
                paciente.getFechaNacimiento(),
                paciente.getTelefono(),
                paciente.getEmail(),
                paciente.isActivo());
    }
}