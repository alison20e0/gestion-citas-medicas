package com.citasmedicas.notificacion;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.citasmedicas.cita.domain.Cita;
import com.citasmedicas.cita.domain.CitaRepository;
import com.citasmedicas.config.CitasProperties;
import com.citasmedicas.notificacion.domain.EstadoEnvio;
import com.citasmedicas.notificacion.domain.NotificacionEnvio;
import com.citasmedicas.notificacion.domain.NotificacionEnvioRepository;
import com.citasmedicas.notificacion.domain.NotificacionTipo;
import com.citasmedicas.notificacion.dto.NotificacionEnvioResponse;
import com.citasmedicas.seguridad.Alcances;
import com.citasmedicas.seguridad.UsuarioActual;
import com.citasmedicas.shared.error.NotFoundException;

@Service
public class NotificacionService {

    public static final String REFERENCIA_CONFIRMACION = "CONFIRMACION_RESERVA";
    public static final String REFERENCIA_RECORDATORIO = "RECORDATORIO";

    private static final int LIMITE_LISTADO = 100;

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.of("es"));

    private final NotificacionEnvioRepository envioRepository;
    private final CitaRepository citaRepository;
    private final CitasProperties properties;
    private final ZoneId zona;

    public NotificacionService(NotificacionEnvioRepository envioRepository, CitaRepository citaRepository,
            CitasProperties properties) {
        this.envioRepository = envioRepository;
        this.citaRepository = citaRepository;
        this.properties = properties;
        this.zona = properties.zoneId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> programarConfirmacion(Cita cita) {
        if (existeNotificacion(cita.getId(), REFERENCIA_CONFIRMACION)) {
            return Optional.empty();
        }
        NotificacionEnvioResponse envio = programar(cita, NotificacionTipo.EMAIL, REFERENCIA_CONFIRMACION,
                cuerpoConfirmacion(cita));
        return Optional.of(envio.id());
    }

    @Transactional
    public int programarRecordatorios() {
        if (!properties.notificaciones().habilitadas()) {
            return 0;
        }
        OffsetDateTime desde = OffsetDateTime.now(zona).withNano(0);
        OffsetDateTime hasta = desde.plusHours(properties.notificaciones().horasAnticipacion());
        int programados = 0;
        for (Cita cita : citaRepository.findConfirmadasEntre(desde, hasta)) {
            if (existeNotificacion(cita.getId(), REFERENCIA_RECORDATORIO)) {
                continue;
            }
            programar(cita, tipoPreferido(cita), REFERENCIA_RECORDATORIO, cuerpoRecordatorio(cita));
            programados++;
        }
        if (programados > 0) {
            log.info("Recordatorios de cita programados: {}", programados);
        }
        return programados;
    }

    @Transactional(readOnly = true)
    public List<NotificacionEnvioResponse> listarPorCita(UsuarioActual usuario, UUID citaId) {
        Cita cita = citaRepository.findDetallePorId(citaId)
                .orElseThrow(() -> new NotFoundException("Cita", citaId));
        Alcances.exigirLecturaDeCita(usuario, cita.getPaciente().getId(), cita.getMedico().getId());
        return envioRepository.findByCitaIdOrderByCreadoEnDesc(citaId, PageRequest.of(0, LIMITE_LISTADO)).stream()
                .map(NotificacionEnvioResponse::from)
                .toList();
    }

    private boolean existeNotificacion(UUID citaId, String referencia) {
        return envioRepository.existsByCitaIdAndReferenciaAndEstadoIn(citaId, referencia,
                List.of(EstadoEnvio.PENDIENTE, EstadoEnvio.ENVIADO, EstadoEnvio.FALLIDO, EstadoEnvio.DESCARTADO));
    }

    private NotificacionEnvioResponse programar(Cita cita, NotificacionTipo tipo, String referencia, String cuerpo) {
        String destinatario = destinatario(cita, tipo);
        NotificacionEnvio envio = NotificacionEnvio.programar(cita.getId(), tipo, referencia, destinatario,
                "Cita medica - " + referencia, cuerpo, Instant.now());
        return NotificacionEnvioResponse.from(envioRepository.save(envio));
    }

    private NotificacionTipo tipoPreferido(Cita cita) {
        String telefono = cita.getPaciente().getTelefono();
        return telefono != null && !telefono.isBlank() ? NotificacionTipo.SMS : NotificacionTipo.EMAIL;
    }

    private String destinatario(Cita cita, NotificacionTipo tipo) {
        if (tipo == NotificacionTipo.SMS) {
            return cita.getPaciente().getTelefono() == null ? "" : cita.getPaciente().getTelefono();
        }
        return cita.getPaciente().getEmail() == null ? "" : cita.getPaciente().getEmail();
    }

    private String cuerpoConfirmacion(Cita cita) {
        return "Cita confirmada con %s (%s) el %s. Motivo: %s".formatted(
                cita.getMedico().nombreCompleto(),
                cita.getMedico().getEspecialidad(),
                formatear(cita),
                cita.getMotivo() == null || cita.getMotivo().isBlank() ? "consulta general" : cita.getMotivo());
    }

    private String cuerpoRecordatorio(Cita cita) {
        return "Recordatorio: su cita con %s es el %s. Presentese 15 minutos antes. Responda SI para confirmar."
                .formatted(cita.getMedico().nombreCompleto(), formatear(cita));
    }

    private String formatear(Cita cita) {
        return ZonedDateTime.ofInstant(cita.getFechaHora().toInstant(), zona).format(FORMATO_FECHA);
    }
}