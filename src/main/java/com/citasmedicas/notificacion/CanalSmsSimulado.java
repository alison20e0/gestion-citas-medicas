package com.citasmedicas.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.citasmedicas.config.CitasProperties;
import com.citasmedicas.notificacion.domain.NotificacionTipo;

@Component
public class CanalSmsSimulado implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(CanalSmsSimulado.class);

    private final CitasProperties properties;

    public CanalSmsSimulado(CitasProperties properties) {
        this.properties = properties;
    }

    @Override
    public NotificacionTipo tipo() {
        return NotificacionTipo.SMS;
    }

    @Override
    @Retryable(retryFor = NotificacionFallidaException.class, maxAttempts = 4,
            backoff = @Backoff(delay = 300, multiplier = 2.0, maxDelay = 5000))
    public void enviar(NotificacionMensaje mensaje) {
        simularLatencia();
        if (mensaje.destinatario() == null || mensaje.destinatario().isBlank()) {
            throw new NotificacionFallidaException("SMS_SIN_DESTINATARIO",
                    "No se puede enviar el SMS porque el paciente no tiene telefono registrado");
        }
        if (properties.notificaciones().debeFallar(mensaje.cuerpo() + "|" + mensaje.destinatario())) {
            throw new NotificacionFallidaException("SMS_RECHAZADO_POR_PROVEEDOR",
                    "El proveedor simulado de SMS rechazo el mensaje para " + mensaje.destinatario());
        }
        log.info("[SMS SIMULADO] enviado a={} | cuerpo={}", mensaje.destinatario(), mensaje.cuerpo());
    }

    private void simularLatencia() {
        long milisegundos = properties.notificaciones().simulacionLatencia().toMillis();
        if (milisegundos <= 0) {
            return;
        }
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new NotificacionFallidaException("SMS_INTERRUMPIDO", "El envio del SMS fue interrumpido");
        }
    }
}