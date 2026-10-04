package com.citasmedicas.cita.domain;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import com.citasmedicas.medico.domain.Medico;
import com.citasmedicas.paciente.domain.Paciente;
import com.citasmedicas.shared.domain.BaseEntity;

@Entity
@Table(name = "cita",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cita_medico_horario",
                        columnNames = {"medico_id", "fecha_hora", "reserva_activa"})
        },
        indexes = {
                @Index(name = "idx_cita_medico_fecha", columnList = "medico_id, fecha_hora"),
                @Index(name = "idx_cita_paciente_fecha", columnList = "paciente_id, fecha_hora"),
                @Index(name = "idx_cita_estado_fecha", columnList = "estado, fecha_hora")
        })
public class Cita extends BaseEntity {

    public static final int SLOT_ACTIVO = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCita estado;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "reserva_activa")
    private Integer reservaActiva;

    @Column(name = "cancelada_en")
    private Instant canceladaEn;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Cita() {
    }

    public static Cita reservar(Paciente paciente, Medico medico, OffsetDateTime inicio, int duracionMinutos,
            String motivo) {
        Cita cita = new Cita();
        cita.paciente = paciente;
        cita.medico = medico;
        cita.fechaHora = inicio;
        cita.duracionMinutos = duracionMinutos;
        cita.motivo = motivo;
        cita.estado = EstadoCita.CONFIRMADA;
        cita.sincronizarReservaActiva();
        return cita;
    }

    public void cancelar(String motivo, Instant momento) {
        this.estado = EstadoCita.CANCELADA;
        this.motivoCancelacion = motivo;
        this.canceladaEn = momento;
        this.sincronizarReservaActiva();
    }

    public void finalizar() {
        this.estado = EstadoCita.FINALIZADA;
        this.sincronizarReservaActiva();
    }

    public void registrarNoAsistio() {
        this.estado = EstadoCita.NO_ASISTIO;
        this.sincronizarReservaActiva();
    }

    private void sincronizarReservaActiva() {
        this.reservaActiva = estado == EstadoCita.CANCELADA ? null : SLOT_ACTIVO;
    }

    public boolean ocupaSlot() {
        return reservaActiva != null;
    }

    public UUID getId() {
        return id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public Integer getReservaActiva() {
        return reservaActiva;
    }

    public Instant getCanceladaEn() {
        return canceladaEn;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public Long getVersion() {
        return version;
    }
}