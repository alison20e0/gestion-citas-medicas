package com.citasmedicas.cita;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.citasmedicas.auditoria.AuditoriaService;
import com.citasmedicas.auditoria.domain.TipoAccionAuditoria;
import com.citasmedicas.auditoria.dto.AuditarCitaRequest;
import com.citasmedicas.auditoria.dto.AuditoriaCitaResponse;
import com.citasmedicas.cita.domain.Cita;
import com.citasmedicas.cita.domain.CitaRepository;
import com.citasmedicas.cita.domain.EstadoCita;
import com.citasmedicas.cita.dto.CancelarCitaRequest;
import com.citasmedicas.cita.dto.CitaResponse;
import com.citasmedicas.cita.dto.DisponibilidadResponse;
import com.citasmedicas.cita.dto.DisponibilidadResponse.SlotResponse;
import com.citasmedicas.cita.dto.ReservarCitaRequest;
import com.citasmedicas.cita.evento.CitaCanceladaEvent;
import com.citasmedicas.cita.evento.CitaReservadaEvent;
import com.citasmedicas.config.CitasProperties;
import com.citasmedicas.medico.domain.HorarioAtencion;
import com.citasmedicas.medico.domain.Medico;
import com.citasmedicas.medico.domain.MedicoRepository;
import com.citasmedicas.paciente.domain.Paciente;
import com.citasmedicas.paciente.domain.PacienteRepository;
import com.citasmedicas.seguridad.Alcances;
import com.citasmedicas.seguridad.UsuarioActual;
import com.citasmedicas.shared.error.ConflictException;
import com.citasmedicas.shared.error.NotFoundException;
import com.citasmedicas.shared.error.ReglaNegocioException;

@Service
public class CitaService {

    private final CitaRepository citaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final AuditoriaService auditoriaService;
    private final ApplicationEventPublisher eventPublisher;
    private final ZoneId zona;

    public CitaService(CitaRepository citaRepository, MedicoRepository medicoRepository,
            PacienteRepository pacienteRepository, AuditoriaService auditoriaService,
            ApplicationEventPublisher eventPublisher, CitasProperties properties) {
        this.citaRepository = citaRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.auditoriaService = auditoriaService;
        this.eventPublisher = eventPublisher;
        this.zona = properties.zoneId();
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(UsuarioActual usuario, UUID medicoId, LocalDate fecha) {
        Alcances.exigirDisponibilidad(usuario, medicoId);
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new NotFoundException("Medico", medicoId));
        LocalDate fechaConsultada = fecha == null ? LocalDate.now(zona).plusDays(1) : fecha;
        if (!medico.atiendeElDia(fechaConsultada.getDayOfWeek())) {
            throw new ReglaNegocioException("El medico no atiende el dia " + fechaConsultada.getDayOfWeek());
        }

        OffsetDateTime inicioDia = inicioDelDiaUtc(fechaConsultada);
        OffsetDateTime finDia = inicioDelDiaUtc(fechaConsultada.plusDays(1));
        Map<OffsetDateTime, UUID> ocupadas = new HashMap<>();
        for (Cita cita : citaRepository.findReservadasEnRango(medicoId, inicioDia, finDia)) {
            ocupadas.put(cita.getFechaHora(), cita.getId());
        }

        Instant ahora = Instant.now();
        List<SlotResponse> slots = new ArrayList<>();
        for (LocalTime hora : medico.slotsDelDia(fechaConsultada.getDayOfWeek())) {
            ZonedDateTime inicioZona = ZonedDateTime.of(fechaConsultada, hora, zona);
            OffsetDateTime inicio = inicioZona.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
            int duracion = duracionDelSlot(medico, fechaConsultada.getDayOfWeek(), hora);
            UUID citaExistente = ocupadas.get(inicio);
            boolean disponible = citaExistente == null && inicio.toInstant().isAfter(ahora);
            slots.add(new SlotResponse(inicio, inicio.plusMinutes(duracion), duracion, disponible, citaExistente));
        }

        int libres = (int) slots.stream().filter(SlotResponse::disponible).count();
        return new DisponibilidadResponse(medico.getId(), medico.nombreCompleto(), medico.getEspecialidad(),
                fechaConsultada, slots.size(), libres, slots);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CitaResponse reservar(ReservarCitaRequest request) {
        Medico medico = medicoRepository.findByIdBloqueado(request.medicoId())
                .orElseThrow(() -> new NotFoundException("Medico", request.medicoId()));
        if (!medico.isActivo()) {
            throw new ReglaNegocioException("El medico no esta activo");
        }
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new NotFoundException("Paciente", request.pacienteId()));
        if (!paciente.isActivo()) {
            throw new ReglaNegocioException("El paciente no esta activo");
        }

        OffsetDateTime inicio = normalizarAUtc(request.fechaHora());
        int duracion = validarSlot(medico, inicio);

        List<Cita> bloqueadas = citaRepository.findBloqueadasPorMedicoYHora(medico.getId(), inicio);
        if (bloqueadas.stream().anyMatch(cita -> cita.getEstado() != EstadoCita.CANCELADA)) {
            throw new ConflictException("HORARIO_OCUPADO",
                    "El medico ya tiene una cita reservada para el " + inicio);
        }

        Cita cita;
        try {
            cita = citaRepository.saveAndFlush(
                    Cita.reservar(paciente, medico, inicio, duracion, request.motivo()));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("HORARIO_OCUPADO",
                    "Otra transaccion reservo el mismo horario para este medico", ex);
        }

        auditoriaService.registrar(cita, TipoAccionAuditoria.RESERVA, null, EstadoCita.CONFIRMADA.name(),
                "Cita reservada para el paciente " + paciente.nombreCompleto(), null);
        eventPublisher.publishEvent(
                new CitaReservadaEvent(cita.getId(), paciente.getId(), medico.getId(), cita.getFechaHora()));
        return CitaResponse.from(cita);
    }

    @Transactional
    public CitaResponse cancelar(UsuarioActual usuario, UUID citaId, CancelarCitaRequest request) {
        Cita cita = obtenerBloqueada(citaId);
        Alcances.exigirCancelacionDeCita(usuario, cita.getPaciente().getId());
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ConflictException("CITA_Y_CANCELADA", "La cita ya se encuentra cancelada");
        }
        if (cita.getEstado().esTerminal()) {
            throw new ConflictException("CITA_NO_CANCELABLE",
                    "No se puede cancelar una cita en estado " + cita.getEstado());
        }

        EstadoCita estadoAnterior = cita.getEstado();
        cita.cancelar(request.motivo(), Instant.now());
        auditoriaService.registrar(cita, TipoAccionAuditoria.CANCELACION, estadoAnterior.name(),
                EstadoCita.CANCELADA.name(), request.motivo(), null);
        eventPublisher.publishEvent(new CitaCanceladaEvent(cita.getId(), request.motivo()));
        return CitaResponse.from(cita);
    }

    @Transactional(readOnly = true)
    public CitaResponse buscarPorId(UsuarioActual usuario, UUID citaId) {
        Cita cita = citaRepository.findDetallePorId(citaId)
                .orElseThrow(() -> new NotFoundException("Cita", citaId));
        Alcances.exigirLecturaDeCita(usuario, cita.getPaciente().getId(), cita.getMedico().getId());
        return CitaResponse.from(cita);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarPorPaciente(UsuarioActual usuario, UUID pacienteId, int limite) {
        Alcances.exigirAlcancePaciente(usuario, pacienteId);
        return citaRepository.findPorPaciente(pacienteId, page(limite)).stream()
                .map(CitaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarPorMedico(UsuarioActual usuario, UUID medicoId, int limite) {
        Alcances.exigirAlcanceMedico(usuario, medicoId);
        return citaRepository.findPorMedico(medicoId, page(limite)).stream()
                .map(CitaResponse::from)
                .toList();
    }

    @Transactional
    public AuditoriaCitaResponse registrarAuditoria(UUID citaId, AuditarCitaRequest request) {
        Cita cita = obtenerBloqueada(citaId);
        return AuditoriaCitaResponse.from(auditoriaService.registrar(cita, request.accion(),
                cita.getEstado().name(), cita.getEstado().name(), request.detalle(), request.usuario()));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaCitaResponse> listarAuditoria(UUID citaId, int limite) {
        if (!citaRepository.existsById(citaId)) {
            throw new NotFoundException("Cita", citaId);
        }
        return auditoriaService.listarPorCita(citaId, limite);
    }

    private Cita obtenerBloqueada(UUID citaId) {
        return citaRepository.findByIdBloqueado(citaId)
                .orElseThrow(() -> new NotFoundException("Cita", citaId));
    }

    private int validarSlot(Medico medico, OffsetDateTime inicioUtc) {
        ZonedDateTime inicioZona = inicioUtc.atZoneSameInstant(zona);
        if (!inicioZona.toInstant().isAfter(Instant.now())) {
            throw new ReglaNegocioException("No se pueden reservar citas en fechas pasado");
        }
        HorarioAtencion horario = medico.horarioQueContiene(inicioZona.getDayOfWeek(), inicioZona.toLocalTime())
                .orElseThrow(() -> new ReglaNegocioException(
                        "El horario " + inicioZona.toLocalTime() + " no pertenece a la agenda del medico"));
        return horario.getDuracionMinutos();
    }

    private int duracionDelSlot(Medico medico, DayOfWeek dia, LocalTime hora) {
        return medico.horarioQueContiene(dia, hora)
                .map(HorarioAtencion::getDuracionMinutos)
                .orElse(30);
    }

    private OffsetDateTime normalizarAUtc(OffsetDateTime fechaHora) {
        return fechaHora.withOffsetSameInstant(ZoneOffset.UTC);
    }

    private OffsetDateTime inicioDelDiaUtc(LocalDate fecha) {
        return ZonedDateTime.of(fecha, LocalTime.MIN, zona).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private PageRequest page(int limite) {
        int max = limite <= 0 ? 50 : Math.min(limite, 200);
        return PageRequest.of(0, max);
    }
}