package com.citasmedicas.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final NotificacionService notificacionService;
    private final NotificacionProcesador procesador;

    public RecordatorioScheduler(NotificacionService notificacionService, NotificacionProcesador procesador) {
        this.notificacionService = notificacionService;
        this.procesador = procesador;
    }

    @Scheduled(initialDelayString = "${citas.notificaciones.intervalo-barrido-milis:60000}",
            fixedDelayString = "${citas.notificaciones.intervalo-barrido-milis:60000}")
    public void barrerRecordatorios() {
        try {
            int programados = notificacionService.programarRecordatorios();
            int enviados = procesador.procesarPendientes();
            if (programados > 0 || enviados > 0) {
                log.info("Barrido de notificaciones: {} programados, {} enviados", programados, enviados);
            }
        } catch (RuntimeException ex) {
            log.error("Fallo el barrido de notificaciones", ex);
        }
    }
}