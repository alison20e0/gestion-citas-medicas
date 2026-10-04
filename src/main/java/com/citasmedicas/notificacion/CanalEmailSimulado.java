package com.citasmedicas.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.citasmedicas.config.CitasProperties;
import com.citasmedicas.notificacion.domain.NotificacionTipo;

@Component
public class CanalEmailSimulado implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(CanalEmailSimulado.class);

    private final CitasProperties properties;

    public CanalEmailSimulado(CitasProperties properties) {
        this.properties = properties;
    }

    @Override
    public NotificacionTipo tipo() {
        return NotificacionTipo.EMAIL;
    }

    @Override
    @Retryable(retryFor = NotificacionFallidaException.class, maxAttempts = 4,
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 5000))
    public void enviar(NotificacionMensaje mensaje) {
        simularLatencia();
        if (mensaje.destinatario() == null || !mensaje.destinatario().contains("@")) {
            throw new NotificacionFallidaException("EMAIL_INVALIDO",
                    "No se puede enviar el correo porque el paciente no tiene un email valido");
        }
        if (properties.notificaciones().debeFallar(mensaje.cuerpo() + "|" + mensaje.destinatario())) {
            throw new NotificacionFallidaException("EMAIL_RECHAZADO_POR_SERVIDOR",
                    "El servidor simulado de correo rechazo el mensaje para " + mensaje.destinatario());
        }
        log.info("[EMAIL SIMULADO] enviado a={} | asunto={} | cuerpo={}", mensaje.destinatario(), mensaje.asunto(),
                mensaje.cuerpo());
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
            throw new NotificacionFallidaException("EMAIL_INTERRUMPIDO", "El envio del correo fue interrumpido");
        }
    }
}