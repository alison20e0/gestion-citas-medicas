package com.citasmedicas.notificacion.domain;

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
import jakarta.persistence.Version;

@Entity
@Table(name = "notificacion_envio", indexes = {
        @Index(name = "idx_notificacion_cita", columnList = "cita_id"),
        @Index(name = "idx_notificacion_reintento", columnList = "estado, proximo_intento")
})
public class NotificacionEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cita_id", nullable = false)
    private UUID citaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 16)
    private NotificacionTipo tipo;

    @Column(name = "referencia", nullable = false, length = 32)
    private String referencia;

    @Column(name = "destinatario", nullable = false, length = 160)
    private String destinatario;

    @Column(name = "asunto", length = 160)
    private String asunto;

    @Column(name = "cuerpo", nullable = false, length = 1000)
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private EstadoEnvio estado;

    @Column(name = "intentos", nullable = false)
    private Integer intentos;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "proximo_intento", nullable = false)
    private Instant proximoIntento;

    @Column(name = "enviado_en")
    private Instant enviadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected NotificacionEnvio() {
    }

    public static NotificacionEnvio programar(UUID citaId, NotificacionTipo tipo, String referencia, String destinatario,
            String asunto, String cuerpo, Instant ahora) {
        NotificacionEnvio envio = new NotificacionEnvio();
        envio.citaId = citaId;
        envio.tipo = tipo;
        envio.referencia = referencia;
        envio.destinatario = destinatario;
        envio.asunto = asunto;
        envio.cuerpo = cuerpo;
        envio.estado = EstadoEnvio.PENDIENTE;
        envio.intentos = 0;
        envio.proximoIntento = ahora;
        envio.creadoEn = ahora;
        return envio;
    }

    public void registrarIntentoFallido(String error, Instant proximo) {
        this.intentos = this.intentos + 1;
        this.ultimoError = error;
        this.proximoIntento = proximo;
        this.estado = EstadoEnvio.FALLIDO;
    }

    public void registrarEnvioExitoso(Instant momento) {
        this.intentos = this.intentos + 1;
        this.estado = EstadoEnvio.ENVIADO;
        this.enviadoEn = momento;
        this.ultimoError = null;
    }

    public void descartar(String motivo) {
        this.estado = EstadoEnvio.DESCARTADO;
        this.ultimoError = motivo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCitaId() {
        return citaId;
    }

    public NotificacionTipo getTipo() {
        return tipo;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public EstadoEnvio getEstado() {
        return estado;
    }

    public Integer getIntentos() {
        return intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public Instant getProximoIntento() {
        return proximoIntento;
    }

    public Instant getEnviadoEn() {
        return enviadoEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Long getVersion() {
        return version;
    }
}