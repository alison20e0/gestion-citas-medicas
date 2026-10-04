package com.citasmedicas.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.citasmedicas.auditoria.AuditoriaService;
import com.citasmedicas.auditoria.domain.TipoAccionAuditoria;
import com.citasmedicas.cita.domain.Cita;
import com.citasmedicas.cita.domain.CitaRepository;
import com.citasmedicas.cita.evento.CitaCanceladaEvent;
import com.citasmedicas.cita.evento.CitaReservadaEvent;
import com.citasmedicas.shared.error.NotFoundException;

@Component
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    private final NotificacionService notificacionService;
    private final NotificacionProcesador procesador;
    private final CitaRepository citaRepository;
    private final AuditoriaService auditoriaService;

    public NotificacionListener(NotificacionService notificacionService, NotificacionProcesador procesador,
            CitaRepository citaRepository, AuditoriaService auditoriaService) {
        this.notificacionService = notificacionService;
        this.procesador = procesador;
        this.citaRepository = citaRepository;
        this.auditoriaService = auditoriaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alReservarCita(CitaReservadaEvent evento) {
        Cita cita = citaRepository.findDetallePorId(evento.citaId()).orElse(null);
        if (cita == null) {
            log.warn("No se encontro la cita {} para enviar la confirmacion", evento.citaId());
            return;
        }
        notificacionService.programarConfirmacion(cita).ifPresent(envioId -> {
            log.info("Confirmacion de la cita {} encolada para {}", cita.getId(),
                    cita.getPaciente().getEmail());
            procesador.enviarUno(envioId);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCancelarCita(CitaCanceladaEvent evento) {
        Cita cita = citaRepository.findDetallePorId(evento.citaId())
                .orElseThrow(() -> new NotFoundException("Cita", evento.citaId()));
        auditoriaService.registrar(cita, TipoAccionAuditoria.OBSERVACION, "CONFIRMADA", "CANCELADA",
                "SeCanceled la cita, motivo: " + evento.motivo(), "sistema");
    }
}