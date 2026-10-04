package com.citasmedicas.auditoria.domain;

import java.time.Instant;
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

import com.citasmedicas.cita.domain.Cita;

@Entity
@Table(name = "auditoria_cita", indexes = {
        @Index(name = "idx_auditoria_cita_fecha", columnList = "cita_id, fecha_registro"),
        @Index(name = "idx_auditoria_cita_accion", columnList = "cita_id, accion")
})
public class AuditoriaCita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 32)
    private TipoAccionAuditoria accion;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 20)
    private String estadoNuevo;

    @Column(name = "detalle", length = 1000)
    private String detalle;

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "direccion_ip", length = 64)
    private String direccionIp;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    protected AuditoriaCita() {
    }

    public static AuditoriaCita registrar(Cita cita, TipoAccionAuditoria accion, String estadoAnterior,
            String estadoNuevo, String detalle, String usuario, String direccionIp, Instant fechaRegistro) {
        AuditoriaCita auditoria = new AuditoriaCita();
        auditoria.cita = cita;
        auditoria.accion = accion;
        auditoria.estadoAnterior = estadoAnterior;
        auditoria.estadoNuevo = estadoNuevo;
        auditoria.detalle = detalle;
        auditoria.usuario = usuario == null || usuario.isBlank() ? "sistema" : usuario;
        auditoria.direccionIp = direccionIp;
        auditoria.fechaRegistro = fechaRegistro;
        return auditoria;
    }

    public UUID getId() {
        return id;
    }

    public Cita getCita() {
        return cita;
    }

    public TipoAccionAuditoria getAccion() {
        return accion;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }
}