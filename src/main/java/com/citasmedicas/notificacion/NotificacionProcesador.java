package com.citasmedicas.notificacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.citasmedicas.auditoria.AuditoriaService;
import com.citasmedicas.auditoria.domain.TipoAccionAuditoria;
import com.citasmedicas.cita.domain.Cita;
import com.citasmedicas.cita.domain.CitaRepository;
import com.citasmedicas.config.CitasProperties;
import com.citasmedicas.notificacion.domain.EstadoEnvio;
import com.citasmedicas.notificacion.domain.NotificacionEnvio;
import com.citasmedicas.notificacion.domain.NotificacionEnvioRepository;
import com.citasmedicas.notificacion.dto.NotificacionEnvioResponse;
import com.citasmedicas.shared.error.ConflictException;
import com.citasmedicas.shared.error.NotFoundException;

@Service
public class NotificacionProcesador {

    private static final Logger log = LoggerFactory.getLogger(NotificacionProcesador.class);

    private final NotificacionEnvioRepository envioRepository;
    private final CitaRepository citaRepository;
    private final CanalesNotificacion canales;
    private final AuditoriaService auditoriaService;
    private final CitasProperties properties;

    public NotificacionProcesador(NotificacionEnvioRepository envioRepository, CitaRepository citaRepository,
            CanalesNotificacion canales, AuditoriaService auditoriaService, CitasProperties properties) {
        this.envioRepository = envioRepository;
        this.citaRepository = citaRepository;
        this.canales = canales;
        this.auditoriaService = auditoriaService;
        this.properties = properties;
    }

    public int procesarPendientes() {
        if (!properties.notificaciones().habilitadas()) {
            return 0;
        }
        List<NotificacionEnvio> listos = envioRepository.findListosParaEnvio(Instant.now(),
                properties.notificaciones().maxIntentos(),
                PageRequest.of(0, properties.notificaciones().loteMaximo()));
        int enviados = 0;
        for (NotificacionEnvio envio : listos) {
            if (enviarUno(envio.getId())) {
                enviados++;
            }
        }
        return enviados;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean enviarUno(UUID envioId) {
        NotificacionEnvio envio = envioRepository.findById(envioId).orElse(null);
        if (envio == null || envio.getEstado().esTerminal()) {
            return false;
        }
        if (envio.getIntentos() >= properties.notificaciones().maxIntentos()) {
            envio.descartar("Se agotaron los intentos permitidos. Ultimo error: " + envio.getUltimoError());
            envioRepository.save(envio);
            return false;
        }
        Cita cita = citaRepository.findById(envio.getCitaId()).orElse(null);
        if (cita == null) {
            envio.descartar("La cita asociada ya no existe");
            envioRepository.save(envio);
            return false;
        }
        try {
            canales.obtener(envio.getTipo()).enviar(new NotificacionMensaje(envio.getDestinatario(),
                    envio.getAsunto(), envio.getCuerpo()));
            envio.registrarEnvioExitoso(Instant.now());
            envioRepository.save(envio);
            auditoriaService.registrar(cita, TipoAccionAuditoria.NOTIFICACION_ENVIADA, null, null,
                    "Recordatorio entregado por " + envio.getTipo() + " a " + envio.getDestinatario(), "sistema");
            return true;
        } catch (NotificacionFallidaException ex) {
            registrarFallo(envio, cita, envio.getTipo() + ": " + ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            registrarFallo(envio, cita, "Error inesperado: " + ex.getMessage());
            log.error("Error inesperado enviando la notificacion {}", envioId, ex);
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificacionEnvioResponse reintentar(UUID envioId) {
        NotificacionEnvio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new NotFoundException("Notificacion", envioId));
        if (envio.getEstado() == EstadoEnvio.ENVIADO) {
            throw new ConflictException("NOTIFICACION_Y_ENVIADA", "La notificacion ya fue entregada");
        }
        if (envio.getEstado() == EstadoEnvio.DESCARTADO) {
            envio.reabrir();
        }
        envio.registrarIntentoFallido("Reintento solicitado manualmente", Instant.now());
        envioRepository.save(envio);
        enviarUno(envioId);
        return envioRepository.findById(envioId)
                .map(NotificacionEnvioResponse::from)
                .orElseThrow(() -> new NotFoundException("Notificacion", envioId));
    }

    private void registrarFallo(NotificacionEnvio envio, Cita cita, String mensaje) {
        envio.registrarIntentoFallido(mensaje, Instant.now());
        if (envio.getIntentos() >= properties.notificaciones().maxIntentos()) {
            envio.descartar("Se agotaron los intentos permitidos. Ultimo error: " + mensaje);
        }
        envioRepository.save(envio);
        auditoriaService.registrar(cita, TipoAccionAuditoria.NOTIFICACION_FALLIDA, null, null,
                "Fallo el envio de la notificacion, " + mensaje, "sistema");
        log.warn("Notificacion {} fallo en el intento {}, estado {}",
                envio.getId(), envio.getIntentos(), envio.getEstado());
    }
}