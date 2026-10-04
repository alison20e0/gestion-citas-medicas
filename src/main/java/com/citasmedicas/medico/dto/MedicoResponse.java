package com.citasmedicas.medico.dto;

import java.util.List;
import java.util.UUID;

import com.citasmedicas.medico.domain.Especialidad;
import com.citasmedicas.medico.domain.HorarioAtencion;
import com.citasmedicas.medico.domain.Medico;

public record MedicoResponse(
        UUID id,
        String documento,
        String nombres,
        String apellidos,
        Especialidad especialidad,
        String telefono,
        String email,
        boolean activo,
        List<HorarioResponse> horarios) {

    public static MedicoResponse from(Medico medico) {
        List<HorarioResponse> horarios = medico.getHorarios().stream()
                .sorted((a, b) -> {
                    int porDia = a.getDiaSemana().compareTo(b.getDiaSemana());
                    return porDia != 0 ? porDia : a.getHoraInicio().compareTo(b.getHoraInicio());
                })
                .map(HorarioResponse::from)
                .toList();
        return new MedicoResponse(medico.getId(), medico.getDocumento(), medico.getNombres(), medico.getApellidos(),
                medico.getEspecialidad(), medico.getTelefono(), medico.getEmail(), medico.isActivo(), horarios);
    }

    public record HorarioResponse(
            String diaSemana,
            String horaInicio,
            String horaFin,
            int duracionMinutos) {

        public static HorarioResponse from(HorarioAtencion horario) {
            return new HorarioResponse(horario.getDiaSemana().name(), horario.getHoraInicio().toString(),
                    horario.getHoraFin().toString(), horario.getDuracionMinutos());
        }
    }
}