package com.citasmedicas.auditoria;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.citasmedicas.auditoria.domain.AuditoriaCita;
import com.citasmedicas.auditoria.domain.AuditoriaCitaRepository;
import com.citasmedicas.auditoria.domain.TipoAccionAuditoria;
import com.citasmedicas.auditoria.dto.AuditoriaCitaResponse;
import com.citasmedicas.cita.domain.Cita;

@Service
public class AuditoriaService {

    private static final int LIMITE_POR_DEFECTO = 100;

    private final AuditoriaCitaRepository repository;

    public AuditoriaService(AuditoriaCitaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditoriaCita registrar(Cita cita, TipoAccionAuditoria accion, String estadoAnterior, String estadoNuevo,
            String detalle, String usuario) {
        AuditoriaCita auditoria = AuditoriaCita.registrar(cita, accion, estadoAnterior, estadoNuevo, detalle,
                usuario, direccionIpActual(), Instant.now());
        return repository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaCitaResponse> listarPorCita(UUID citaId, int limite) {
        int max = limite <= 0 ? LIMITE_POR_DEFECTO : Math.min(limite, LIMITE_POR_DEFECTO);
        return repository.findByCitaIdOrderByFechaRegistroDesc(citaId, PageRequest.of(0, max)).stream()
                .map(AuditoriaCitaResponse::from)
                .toList();
    }

    private String direccionIpActual() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}