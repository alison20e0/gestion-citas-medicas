package com.citasmedicas.medico.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.citasmedicas.shared.domain.BaseEntity;

@Entity
@Table(name = "medico", uniqueConstraints = {
        @UniqueConstraint(name = "uk_medico_documento", columnNames = "documento")
})
public class Medico extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "documento", nullable = false, length = 32)
    private String documento;

    @Column(name = "nombres", nullable = false, length = 80)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "especialidad", nullable = false, length = 32)
    private Especialidad especialidad;

    @Column(name = "telefono", length = 32)
    private String telefono;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medico_horario",
            joinColumns = @JoinColumn(name = "medico_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_medico_horario",
                    columnNames = {"medico_id", "dia_semana", "hora_inicio"}))
    @OrderBy("diaSemana ASC, horaInicio ASC")
    private Set<HorarioAtencion> horarios = new LinkedHashSet<>();

    protected Medico() {
    }

    public static Medico crear(String documento, String nombres, String apellidos, Especialidad especialidad,
            String telefono, String email, Set<HorarioAtencion> horarios) {
        Medico medico = new Medico();
        medico.documento = documento;
        medico.nombres = nombres;
        medico.apellidos = apellidos;
        medico.especialidad = especialidad;
        medico.telefono = telefono;
        medico.email = email;
        medico.activo = true;
        if (horarios != null) {
            medico.horarios.addAll(horarios);
        }
        return medico;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public List<HorarioAtencion> horariosDelDia(DayOfWeek dia) {
        return horarios.stream()
                .filter(horario -> horario.getDiaSemana() == dia)
                .sorted(Comparator.comparing(HorarioAtencion::getHoraInicio))
                .toList();
    }

    public boolean atiendeElDia(DayOfWeek dia) {
        return horarios.stream().anyMatch(horario -> horario.getDiaSemana() == dia);
    }

    public Optional<HorarioAtencion> horarioQueContiene(DayOfWeek dia, LocalTime hora) {
        return horariosDelDia(dia).stream().filter(horario -> horario.contiene(hora)).findFirst();
    }

    public List<LocalTime> slotsDelDia(DayOfWeek dia) {
        List<LocalTime> slots = new ArrayList<>();
        for (HorarioAtencion horario : horariosDelDia(dia)) {
            LocalTime cursor = horario.getHoraInicio();
            while (!cursor.plusMinutes(horario.getDuracionMinutos()).isAfter(horario.getHoraFin())) {
                slots.add(cursor);
                cursor = cursor.plusMinutes(horario.getDuracionMinutos());
            }
        }
        return slots;
    }

    public UUID getId() {
        return id;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActivo() {
        return activo;
    }

    public Set<HorarioAtencion> getHorarios() {
        return Set.copyOf(horarios);
    }

    public void desactivar() {
        this.activo = false;
    }
}