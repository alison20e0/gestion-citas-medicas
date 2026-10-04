package com.citasmedicas.medico.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class HorarioAtencion {

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 12)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    protected HorarioAtencion() {
    }

    public HorarioAtencion(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin, Integer duracionMinutos) {
        if (diaSemana == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("El horario requiere dia, hora de inicio y hora de fin");
        }
        int duracion = duracionMinutos == null ? 30 : duracionMinutos;
        if (duracion < 5) {
            throw new IllegalArgumentException("La duracion minima de un turno es de 5 minutos");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionMinutos = duracion;
    }

    public boolean contiene(LocalTime hora) {
        return !hora.isBefore(horaInicio) && !hora.plusMinutes(duracionMinutos).isAfter(horaFin);
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof HorarioAtencion horario)) {
            return false;
        }
        return duracionMinutos == horario.duracionMinutos
                && diaSemana == horario.diaSemana
                && Objects.equals(horaInicio, horario.horaInicio)
                && Objects.equals(horaFin, horario.horaFin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diaSemana, horaInicio, horaFin, duracionMinutos);
    }
}