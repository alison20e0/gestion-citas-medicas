package com.citasmedicas.auditoria.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.citasmedicas.seguridad.Rol;

/**
 * Intento de acceso que el sistema rechazo con 403. No pertenece a una cita
 * concreta porque el rechazo puede ocurrir sobre catalogos o colecciones.
 */
@Entity
@Table(name = "auditoria_acceso_denegado", indexes = {
        @Index(name = "idx_acceso_denegado_fecha", columnList = "fecha_registro"),
        @Index(name = "idx_acceso_denegado_usuario", columnList = "usuario_id, fecha_registro")
})
public class AuditoriaAccesoDenegado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "metodo", nullable = false, length = 10)
    private String metodo;

    @Column(name = "ruta", nullable = false, length = 300)
    private String ruta;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "direccion_ip", length = 64)
    private String direccionIp;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    protected AuditoriaAccesoDenegado() {
    }

    public static AuditoriaAccesoDenegado registrar(UUID usuarioId, Rol rol, String metodo, String ruta, String motivo,
            String direccionIp, Instant fechaRegistro) {
        AuditoriaAccesoDenegado auditoria = new AuditoriaAccesoDenegado();
        auditoria.usuarioId = usuarioId;
        auditoria.rol = rol;
        auditoria.metodo = metodo;
        auditoria.ruta = ruta;
        auditoria.motivo = motivo;
        auditoria.direccionIp = direccionIp;
        auditoria.fechaRegistro = fechaRegistro;
        return auditoria;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Rol getRol() {
        return rol;
    }

    public String getMetodo() {
        return metodo;
    }

    public String getRuta() {
        return ruta;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }
}